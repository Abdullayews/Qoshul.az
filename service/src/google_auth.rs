//! Google Sign-In verifikasiya modulu (D-155, master-spec §4.1 — SSO Google).
//!
//! PROBLEM (2026-11-25): istifadəçi Google Cloud-da ANDROID tipli OAuth klient
//! yaratdı (SHA-1 ilə). Credential Manager `setServerClientId` üçün Google
//! rəsmi olaraq WEB tipli ID tələb edir ("not your Android client ID" —
//! Firebase/Android sənədləri). Android ID qoyulanda klient DEVELOPER_ERROR
//! (konfiqurasiya xətası) verir. Google Console-a giriş olmadan Web klient
//! yaratmaq mümkün deyil.
//!
//! HƏLL: Android ID `serverClientId` KİMİ İSTİFADƏ OLUNMUR — onun əvəzinə
//! Credential Manager Android ID-ni ÖZÜ tanıyır (GetGoogleIdOption Android
//! klientlər üçün də işləyir: aud = Android ID). Bizim M4 servisi bu
//! id_token-ı Google JWKS (RS256) ilə ÖZÜ doğrulayır və GoTrue admin API
//! ilə istifadəçi yaradıb sessiya JWT-si qaytarır.
//!
//! Axın:
//!   app → Credential Manager (Google-un öz UI) → id_token (aud=Android ID)
//!   app → POST /m4/auth/google/verify {id_token, nonce}
//!   m4  → JWKS imza + iss + aud + exp + nonce yoxlaması
//!   m4  → GoTrue admin: createUser (email = google email, `email_confirm`)
//!   m4  → GoTrue admin: generate_link (magiclink) → access_token çıxarılır
//!         (yeniləmə axınları olmadan birbaşa JWT qaytarmaq üçün)
//!
//! Təhlükəsizlik: nonce SHA-256 hash clientdə Google-a gedir, raw nonce
//! burada gəlir — token `nonce` claim-i raw-nonce-un SHA256-i olmalıdır.

use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::Json;
use jsonwebtoken::{Algorithm, DecodingKey, Validation};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::sync::Arc;
use std::sync::Mutex;

/// Google-ın sabit OIDC dəyərləri.
pub const GOOGLE_ISSUERS: [&str; 2] = ["https://accounts.google.com", "accounts.google.com"];
pub const JWKS_URL: &str = "https://www.googleapis.com/oauth2/v3/certs";

/// JWKS keşi — 12 saat (Google açarları nadir dəyişir; kid-miss = dərhal refetch).
struct JwksCache {
    keys: HashMap<String, Value>,
    fetched_at: chrono::DateTime<chrono::Utc>,
}

impl JwksCache {
    fn fresh(&self) -> bool {
        chrono::Utc::now() - self.fetched_at < chrono::Duration::hours(12)
    }
}

pub struct GoogleVerifier {
    http: reqwest::Client,
    cache: Mutex<Option<JwksCache>>,
    /// qəbul edilən aud dəyərləri (Android ID; gələcəkdə Web ID də əlavə olunar)
    audiences: Vec<String>,
}

#[derive(Debug)]
pub enum GoogleAuthError {
    InvalidToken(String),
    Network(String),
    GoTrue(String),
}

impl std::fmt::Display for GoogleAuthError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            GoogleAuthError::InvalidToken(m) => write!(f, "INVALID_TOKEN: {m}"),
            GoogleAuthError::Network(m) => write!(f, "NETWORK: {m}"),
            GoogleAuthError::GoTrue(m) => write!(f, "GOTRUE: {m}"),
        }
    }
}

impl GoogleVerifier {
    pub fn new(client_ids: &str) -> Self {
        Self {
            http: reqwest::Client::new(),
            cache: Mutex::new(None),
            // D-156: vergüllü siyahı — GOOGLE_CLIENT_IDS=web_id,android_id.
            // aud yoxlaması siyahının HAMSINA qarşı aparılır (jsonwebtoken
            // set_audience Vec qəbul edir) — hər iki tipli token keçər.
            audiences: client_ids
                .split(',')
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty() && s != "PLACEHOLDER")
                .collect(),
        }
    }

    pub fn configured(&self) -> bool {
        !self.audiences.is_empty()
    }

    async fn fetch_jwks(&self) -> Result<HashMap<String, Value>, GoogleAuthError> {
        let resp = self
            .http
            .get(JWKS_URL)
            .timeout(std::time::Duration::from_secs(10))
            .send()
            .await
            .map_err(|e| GoogleAuthError::Network(e.to_string()))?;
        let body: Value = resp
            .json()
            .await
            .map_err(|e| GoogleAuthError::Network(e.to_string()))?;
        let mut map = HashMap::new();
        if let Some(keys) = body["keys"].as_array() {
            for k in keys {
                if let Some(kid) = k["kid"].as_str() {
                    map.insert(kid.to_string(), k.clone());
                }
            }
        }
        Ok(map)
    }

    /// JWKS-i keşdən ver; yoxdursa/faydasızdırsa fetch et.
    async fn jwks(&self) -> Result<HashMap<String, Value>, GoogleAuthError> {
        {
            let guard = self.cache.lock().unwrap();
            if let Some(c) = guard.as_ref() {
                if c.fresh() {
                    return Ok(c.keys.clone());
                }
            }
        }
        let keys = self.fetch_jwks().await?;
        *self.cache.lock().unwrap() = Some(JwksCache {
            keys: keys.clone(),
            fetched_at: chrono::Utc::now(),
        });
        Ok(keys)
    }

    /// id_token-ı tam doğrula: imza (JWKS RS256) + iss + aud + exp + nonce.
    /// Ok → claim-lər (sub, email, name, picture, nonce).
    pub async fn verify(
        &self,
        id_token: &str,
        raw_nonce: Option<&str>,
    ) -> Result<Value, GoogleAuthError> {
        // ── başlıqdan kid ──
        let parts: Vec<&str> = id_token.split('.').collect();
        if parts.len() != 3 {
            return Err(GoogleAuthError::InvalidToken("malformed jwt".into()));
        }
        let header_b = b64url_decode(parts[0])
            .map_err(|_| GoogleAuthError::InvalidToken("header decode".into()))?;
        let header: Value = serde_json::from_slice(&header_b)
            .map_err(|_| GoogleAuthError::InvalidToken("header json".into()))?;
        let kid = header["kid"]
            .as_str()
            .ok_or_else(|| GoogleAuthError::InvalidToken("kid yoxdur".into()))?
            .to_string();
        let alg = header["alg"].as_str().unwrap_or("RS256").to_string();
        if alg != "RS256" {
            return Err(GoogleAuthError::InvalidToken(format!(
                "dəstəklənməyən alg: {alg}"
            )));
        }

        // ── imza + standart claim-lər (jsonwebtoken) ──
        let jwks = self.jwks().await?;
        let jwk_value = jwks
            .get(&kid)
            .ok_or_else(|| GoogleAuthError::InvalidToken(format!("naməlum kid: {kid}")))?
            .clone();
        let jwk: jsonwebtoken::jwk::Jwk = serde_json::from_value(jwk_value)
            .map_err(|e| GoogleAuthError::InvalidToken(e.to_string()))?;
        let key = DecodingKey::from_jwk(&jwk)
            .map_err(|e| GoogleAuthError::InvalidToken(e.to_string()))?;

        let mut validation = Validation::new(Algorithm::RS256);
        validation.iss = Some(GOOGLE_ISSUERS.iter().map(|s| s.to_string()).collect());
        validation.set_audience(&self.audiences);
        validation.validate_exp = true;
        validation.validate_nbf = false;
        validation.leeway = 30;

        let claims: Value = jsonwebtoken::decode(id_token, &key, &validation)
            .map_err(|e| GoogleAuthError::InvalidToken(e.to_string()))?
            .claims;

        // ── nonce (opsional — klient göndərirsə mütləq uyğun olmalıdır) ──
        if let Some(raw) = raw_nonce {
            if !raw.is_empty() {
                let token_nonce = claims["nonce"].as_str().unwrap_or("");
                let expected = sha256_hex(raw.as_bytes());
                if !constant_time_eq(token_nonce, &expected) {
                    return Err(GoogleAuthError::InvalidToken("nonce uyğunsuzluğu".into()));
                }
            }
        }
        Ok(claims)
    }
}

/// SHA-256 hex (nonce yoxlaması) — SHA2 dependency əlavə etməmək üçün
/// rust-crypto qrupundan istifadə. sha2 crate artıq jsonwebtoken-in
/// rust_crypto feature-i ilə gəlir.
fn sha256_hex(data: &[u8]) -> String {
    use sha2::{Digest, Sha256};
    let mut h = Sha256::new();
    h.update(data);
    h.finalize().iter().map(|b| format!("{b:02x}")).collect()
}

fn b64url_decode(s: &str) -> Result<Vec<u8>, base64::DecodeError> {
    use base64::Engine;
    base64::engine::general_purpose::URL_SAFE_NO_PAD.decode(s)
}

pub(crate) fn constant_time_eq(a: &str, b: &str) -> bool {
    if a.len() != b.len() {
        return false;
    }
    a.bytes()
        .zip(b.bytes())
        .fold(0u8, |acc, (x, y)| acc | (x ^ y))
        == 0
}

// ═══════════════════════════════════════════════════════════════════════════
// HTTP endpoint: POST /auth/google/verify
// ═══════════════════════════════════════════════════════════════════════════

/// GoTrue admin API klienti — yalnız Google axını üçün lazım olan hissə.
pub struct GoTrueAdmin {
    pub base: String,        // http://supabase-auth:9999
    pub service_key: String, // service_role JWT
    http: reqwest::Client,
}

#[derive(Debug, serde::Deserialize, Clone)]
pub struct GoTrueSession {
    #[serde(default)]
    pub access_token: String,
    #[serde(default)]
    pub refresh_token: String,
    #[serde(default)]
    pub expires_in: i64,
    #[serde(default)]
    pub user_id: String,
    #[serde(default)]
    pub email: String,
}

impl GoTrueAdmin {
    pub fn new(base: &str, service_key: &str) -> Self {
        // D-158: /verify 303 redirect qaytarır; onu İZLƏMƏMƏK lazımdır —
        // Location başlığındakı #access_token fragment-i client tərəfdə
        // itir. Policy::none → cavab birbaşa bizə gəlir.
        let http = reqwest::Client::builder()
            .redirect(reqwest::redirect::Policy::none())
            .build()
            .unwrap_or_else(|_| reqwest::Client::new());
        Self {
            base: base.trim_end_matches('/').to_string(),
            service_key: service_key.to_string(),
            http,
        }
    }

    fn headers(&self) -> reqwest::header::HeaderMap {
        let mut h = reqwest::header::HeaderMap::new();
        if let Ok(k) = reqwest::header::HeaderValue::from_str(&self.service_key) {
            h.insert("apikey", k.clone());
            h.insert("authorization", k);
        }
        h
    }

    /// Google email-i ilə istifadəçini tap/yaarat və SESSİYA qaytar.
    ///
    /// Addım 1: admin/user?email=... ilə axtar (v2.197 dəstəkləyir).
    /// Addım 2: yoxdursa — createUser (email_confirm=true, app_metadata
    ///          provider=google; mövcud email-də 422 → addım 1-ə qayıt).
    /// Addım 3: generate_link (magiclink) → action_link-dən access_token
    ///          və refresh_token çıxarılır → app importSession edir.
    pub async fn sign_in_google_user(
        &self,
        email: &str,
        name: &str,
        _picture: &str,
        google_sub: &str,
    ) -> Result<GoTrueSession, GoogleAuthError> {
        // ── Addım 1: mövcud user ──
        let url = format!("{}/admin/users?per_page=1&email={}", self.base, email);
        let resp = self
            .http
            .get(&url)
            .headers(self.headers())
            .timeout(std::time::Duration::from_secs(10))
            .send()
            .await
            .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
        let body: Value = resp
            .json()
            .await
            .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
        let mut user_id = body["users"][0]["id"]
            .as_str()
            .unwrap_or_default()
            .to_string();

        // ── Addım 2: yarat (idempotent) ──
        if user_id.is_empty() {
            let create = json!({
                "email": email,
                "email_confirm": true,
                "app_metadata": {"provider": "google", "providers": ["google"]},
                "user_metadata": {"full_name": name, "name": name,
                                   "google_sub": google_sub},
            });
            let url = format!("{}/admin/users", self.base);
            let resp = self
                .http
                .post(&url)
                .headers(self.headers())
                .timeout(std::time::Duration::from_secs(10))
                .json(&create)
                .send()
                .await
                .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
            if resp.status().is_success() {
                let body: Value = resp
                    .json()
                    .await
                    .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
                user_id = body["id"].as_str().unwrap_or_default().to_string();
            }
            // 422 (email mövcuddur) → addım 1-də tapılmamış deməkdir —
            // bu, yalnız list filter uyuşmazlığı ola bilər; bir də axtar.
            if user_id.is_empty() {
                let url = format!("{}/admin/users?per_page=1&email={}", self.base, email);
                let resp = self
                    .http
                    .get(&url)
                    .headers(self.headers())
                    .timeout(std::time::Duration::from_secs(10))
                    .send()
                    .await
                    .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
                let body: Value = resp
                    .json()
                    .await
                    .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
                user_id = body["users"][0]["id"]
                    .as_str()
                    .unwrap_or_default()
                    .to_string();
            }
        }

        if user_id.is_empty() {
            return Err(GoogleAuthError::GoTrue(
                "istifadəçi yaradıla bilmədi".into(),
            ));
        }

        // ── Addım 3: generate_link → sessiya JWT ──
        // GoTrue magiclink token-i query param kimi action_link-də gəlir.
        // Onu parçalayıb verify edirik → access+refresh JWT.
        let link_req = json!({
            "type": "magiclink",
            "email": email,
            "new_email": email, // bəzi versiyalarda tələb olunur — zərərsizdir
        });
        let url = format!("{}/admin/generate_link", self.base);
        let resp = self
            .http
            .post(&url)
            .headers(self.headers())
            .timeout(std::time::Duration::from_secs(10))
            .json(&link_req)
            .send()
            .await
            .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
        let status = resp.status();
        let body: Value = resp
            .json()
            .await
            .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
        if !status.is_success() {
            return Err(GoogleAuthError::GoTrue(format!(
                "generate_link {} → {}",
                status,
                body["msg"].as_str().unwrap_or("?")
            )));
        }

        // action_link: https://.../auth/v1/verify?token=...&redirect_to=...
        let action_link = body["action_link"].as_str().unwrap_or_default();
        let token = extract_query_param(action_link, "token")
            .ok_or_else(|| GoogleAuthError::GoTrue("action_link-də token yoxdur".into()))?;
        let verify_url = format!(
            "{}/verify?token={}&type=magiclink&redirect_to={}",
            self.base,
            token,
            urlencoding::encode("https://supabase.qoshul.com")
        );
        // D-158: GoTrue /verify YALNIZ GET qəbul edir (POST → 400 bad_json)
        // və uğurda 302 redirect qaytarır — Location başlığında (və ya
        // fallback HTML bədənində) #access_token=...&refresh_token=...
        // fragment-i olur. JSON gözləmək yanlış idi (köhnə 502-nin səbəbi).
        let resp = self
            .http
            .get(&verify_url)
            .headers(self.headers())
            .timeout(std::time::Duration::from_secs(10))
            .send()
            .await
            .map_err(|e| GoogleAuthError::GoTrue(e.to_string()))?;
        let status = resp.status();
        // Location başlığı birincil mənbədir; yoxdursa bədəndəki HTML-dən
        // href çıxarırıq (browser olmayan klient üçün fallback).
        let location = resp
            .headers()
            .get("location")
            .and_then(|v| v.to_str().ok())
            .map(|s| s.to_string());
        let html = resp.text().await.unwrap_or_default();
        if !status.is_success()
            && status != reqwest::StatusCode::FOUND
            && status != reqwest::StatusCode::SEE_OTHER
        {
            return Err(GoogleAuthError::GoTrue(format!(
                "verify {} → {}",
                status,
                extract_query_param(&html, "msg").unwrap_or_else(|| "?".into())
            )));
        }
        let redirect_url = location
            .clone()
            .or_else(|| extract_href_from_html(&html))
            .ok_or_else(|| {
                GoogleAuthError::GoTrue("verify cavabında redirect linki yoxdur".into())
            })?;
        // fragment: ...#access_token=...&refresh_token=...&expires_in=...
        let fragment = redirect_url
            .split_once('#')
            .map(|(_, f)| f.to_string())
            .unwrap_or_default();
        let params = format!("?{fragment}");
        let access_token = extract_query_param(&params, "access_token").unwrap_or_default();
        let refresh_token = extract_query_param(&params, "refresh_token").unwrap_or_default();
        if access_token.is_empty() || refresh_token.is_empty() {
            return Err(GoogleAuthError::GoTrue(
                "verify redirectində access/refresh token yoxdur".into(),
            ));
        }
        let expires_in = extract_query_param(&params, "expires_in")
            .and_then(|v| v.parse::<i64>().ok())
            .unwrap_or(3600);

        Ok(GoTrueSession {
            access_token,
            refresh_token,
            expires_in,
            user_id,
            email: email.to_string(),
        })
    }
}

/// HTML fallback: `<a href="URL#access_token=...">` içindən URL çıxarır.
fn extract_href_from_html(html: &str) -> Option<String> {
    let h = html.split_once("href=\"")?.1;
    let url = h.split_once('"')?.0;
    if url.contains("access_token=") {
        Some(url.to_string())
    } else {
        None
    }
}

fn extract_query_param(url: &str, key: &str) -> Option<String> {
    let q = url.split_once('?')?.1;
    for pair in q.split('&') {
        let (k, v) = pair.split_once('=')?;
        if k == key {
            return Some(urlencoding::decode(v).ok()?.to_string());
        }
    }
    None
}

#[derive(serde::Deserialize)]
pub struct GoogleVerifyRequest {
    pub id_token: String,
    #[serde(default)]
    pub nonce: Option<String>,
}

/// Google ID token doğrula → GoTrue istifadəçisi + sessiya JWT qaytar.
///
/// Keçiddən sonra app `SupabaseBackend.importSession(...)` edir — istifadəçi
/// normal Supabase sessiyası ilə davam edir (RLS, realtime — hamısı işləyir).
pub async fn verify_google(
    State(st): State<Arc<AppState>>,
    Json(req): Json<GoogleVerifyRequest>,
) -> axum::response::Response {
    let verifier = match st.google.as_ref() {
        Some(v) if v.configured() => v,
        _ => {
            return (
                StatusCode::SERVICE_UNAVAILABLE,
                Json(json!({"error": "GOOGLE_NOT_CONFIGURED",
                             "error_description": "GOOGLE_ANDROID_CLIENT_ID env qurulmayıb"})),
            )
                .into_response()
        }
    };

    // 1) Google tərəfindən imzalanmış token-ı doğrula
    let claims = match verifier.verify(&req.id_token, req.nonce.as_deref()).await {
        Ok(c) => c,
        Err(GoogleAuthError::InvalidToken(m)) => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(json!({
                "error": "INVALID_ID_TOKEN", "error_description": m})),
            )
                .into_response()
        }
        Err(e) => {
            return (
                StatusCode::BAD_GATEWAY,
                Json(json!({"error": e.to_string()})),
            )
                .into_response()
        }
    };

    let sub = claims["sub"].as_str().unwrap_or_default().to_string();
    let email = claims["email"].as_str().unwrap_or_default().to_string();
    let name = claims["name"].as_str().unwrap_or_default().to_string();
    let picture = claims["picture"].as_str().unwrap_or_default().to_string();
    if sub.is_empty() || email.is_empty() {
        return (
            StatusCode::UNAUTHORIZED,
            Json(json!({"error": "INVALID_ID_TOKEN",
                         "error_description": "sub/email claim-ləri yoxdur"})),
        )
            .into_response();
    }

    // 2) GoTrue admin: identikitə görə user axtar (google sub → identities)
    //    Rəsmi GoTrue admin API: list_users üçün filter yoxdur → email ilə
    //    birbaşa userId tapmaq üçün admin/user/search yoxdur; ən sadə yol:
    //    createUser idempotent şəkildə cəhd et — mövcuddursa 422 qaytarır.
    let gotrue = match st.gotrue_admin.as_ref() {
        Some(g) => g,
        None => {
            return (
                StatusCode::SERVICE_UNAVAILABLE,
                Json(json!({"error": "GOTRUE_NOT_CONFIGURED"})),
            )
                .into_response()
        }
    };

    match gotrue
        .sign_in_google_user(&email, &name, &picture, &sub)
        .await
    {
        Ok(session) => (
            StatusCode::OK,
            Json(json!({
                "access_token": session.access_token,
                "refresh_token": session.refresh_token,
                "expires_in": session.expires_in,
                "user": {"id": session.user_id, "email": session.email},
            })),
        )
            .into_response(),
        Err(e) => (
            StatusCode::BAD_GATEWAY,
            Json(json!({"error": e.to_string()})),
        )
            .into_response(),
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HTTP endpoint: GET /config (D-156)
// ═══════════════════════════════════════════════════════════════════════════

/// Klient üçün dinamik konfiqurasiya — APK yenidən buraxmadan Google Web
/// klient ID-ni dəyişmək mümkün olur (istifadəçi Console-da ID yaratdıqda
/// yalnız server env-i yenilənir).
pub async fn public_config(State(st): State<Arc<AppState>>) -> axum::response::Response {
    let web_id = st.google_web_client_id.clone().unwrap_or_default();
    (
        StatusCode::OK,
        Json(json!({
            "google_web_client_id": if web_id.is_empty() { Value::Null } else { json!(web_id) },
            "service": "qoshul-m4",
            "version": env!("CARGO_PKG_VERSION"),
        })),
    )
        .into_response()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn b64url() {
        assert_eq!(b64url_decode("aGVsbG8").unwrap(), b"hello");
        assert!(b64url_decode("!!!").is_err());
    }

    #[test]
    fn ct_eq() {
        assert!(constant_time_eq("abc", "abc"));
        assert!(!constant_time_eq("abc", "abd"));
        assert!(!constant_time_eq("abc", "abcd"));
    }

    #[test]
    fn sha256_nonce() {
        // "abc" SHA-256 — tanınmış test vektoru
        assert_eq!(
            sha256_hex(b"abc"),
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
    }

    #[test]
    fn verifier_configured() {
        // PLACEHOLDER/boş → configured=false (degrade)
        assert!(!GoogleVerifier::new("PLACEHOLDER").configured());
        assert!(!GoogleVerifier::new("").configured());
        // vergüllü siyahı — Web ID gələndə strukturu dəstəkləyir
        let v = GoogleVerifier::new("web-id-1,android-id-2");
        assert!(v.configured());
    }

    #[test]
    fn malformed_tokens_rejected() {
        let v = GoogleVerifier::new("some-id");
        let rt = tokio::runtime::Runtime::new().unwrap();
        // 2 hissəli token → malformed
        let r = rt.block_on(v.verify("a.b", None));
        assert!(matches!(r, Err(GoogleAuthError::InvalidToken(_))));
        // 3 hissəli amma base64 deyil
        let r = rt.block_on(v.verify("!!.??.??", None));
        assert!(matches!(r, Err(GoogleAuthError::InvalidToken(_))));
    }

    #[test]
    fn query_param_extraction() {
        let link = "https://x.com/auth/v1/verify?token=ABC123&redirect_to=https%3A%2F%2Fy.com";
        assert_eq!(extract_query_param(link, "token").unwrap(), "ABC123");
        assert_eq!(
            extract_query_param(link, "redirect_to").unwrap(),
            "https://y.com"
        );
        assert!(extract_query_param(link, "yoxdur").is_none());
    }

    #[test]
    fn gotrue_admin_base_trim() {
        let g = GoTrueAdmin::new("http://supabase-auth:9999/", "key");
        assert_eq!(g.base, "http://supabase-auth:9999");
    }

    #[test]
    fn href_extraction_from_verify_html() {
        // D-158: GoTrue /verify uğurda HTML redirect səhifəsi qaytarır —
        // `<a href="https://...#access_token=..&refresh_token=..">`
        let html = concat!(
            "<html><head></head><body>",
            "<a href=\"https://supabase.qoshul.com",
            "#access_token=eyJ.abc&refresh_token=rt1&expires_in=3600\">",
            "...</a></body></html>"
        );
        let url = extract_href_from_html(html).expect("href çıxmalıdır");
        assert!(url.contains("#access_token="));

        // fragment parçalanması
        let fragment = url.split_once('#').unwrap().1;
        let params = format!("?{fragment}");
        assert_eq!(
            extract_query_param(&params, "access_token").unwrap(),
            "eyJ.abc"
        );
        assert_eq!(
            extract_query_param(&params, "refresh_token").unwrap(),
            "rt1"
        );
        assert_eq!(
            extract_query_param(&params, "expires_in").unwrap(),
            "3600"
        );
    }

    #[test]
    fn href_extraction_rejects_plain_links() {
        // access_token olmayan href → None (yanlış_positive qorunması)
        assert!(extract_href_from_html("<a href=\"https://x.com/just\">t</a>").is_none());
        assert!(extract_href_from_html("href=\"").is_none());
    }
}
