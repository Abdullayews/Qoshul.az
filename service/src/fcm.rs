//! FCM HTTP v1 (D-069/D-082) — OAuth2 service-account → RS256 JWT →
//! fcm.googleapis.com/v1/projects/{id}/messages:send.
//!
//! PLACEHOLDER konfiqdə sender SÖNDÜRÜLÜR (fcm_ready=false) — joblar
//! notifikasiya cədvəlinə yazır, push getmir (log).

use serde_json::{json, Value};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Debug, Clone)]
pub struct ServiceAccount {
    pub client_email: String,
    pub private_key: String,
    pub project_id: String,
}

impl ServiceAccount {
    pub fn parse(json_str: &str) -> Option<Self> {
        let v: Value = serde_json::from_str(json_str).ok()?;
        Some(Self {
            client_email: v.get("client_email")?.as_str()?.to_string(),
            private_key: v.get("private_key")?.as_str()?.to_string(),
            project_id: v.get("project_id")?.as_str()?.to_string(),
        })
    }
}

/// OAuth2 access token (RS256 JWT) — jsonwebtoken::encode birbaşa imzalı
/// JWT qaytarır (D-082: əl ilə base64 YOX).
pub fn oauth_token(sa: &ServiceAccount) -> Result<String, String> {
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_err(|e| e.to_string())?
        .as_secs();
    let claims = json!({
        "iss": sa.client_email,
        "scope": "https://www.googleapis.com/auth/firebase.messaging",
        "aud": "https://oauth2.googleapis.com/token",
        "iat": now,
        "exp": now + 3600,
    });
    let key = jsonwebtoken::EncodingKey::from_rsa_pem(sa.private_key.as_bytes())
        .map_err(|e| format!("pem: {e}"))?;
    jsonwebtoken::encode(
        &jsonwebtoken::Header::new(jsonwebtoken::Algorithm::RS256),
        &claims,
        &key,
    )
    .map_err(|e| format!("sign: {e}"))
}

/// FCM push (HTTP v1). `send` placeholder-dirsə None qayıdır (degrade).
pub struct FcmSender {
    account: Option<ServiceAccount>,
    http: reqwest::Client,
}

impl FcmSender {
    pub fn new(service_account_json: &str) -> Self {
        Self {
            account: if service_account_json != "PLACEHOLDER" {
                ServiceAccount::parse(service_account_json)
            } else {
                None
            },
            http: reqwest::Client::new(),
        }
    }

    pub fn enabled(&self) -> bool {
        self.account.is_some()
    }

    pub async fn send(&self, device_token: &str, title: &str, body: &str) -> Result<(), String> {
        let sa = self.account.as_ref().ok_or("FCM PLACEHOLDER-dır")?;
        let token = oauth_token(sa)?;
        let payload = json!({
            "message": {
                "token": device_token,
                "notification": { "title": title, "body": body },
                "android": { "priority": "high" },
            }
        });
        let url = format!(
            "https://fcm.googleapis.com/v1/projects/{}/messages:send",
            sa.project_id
        );
        let resp = self
            .http
            .post(url)
            .bearer_auth(token)
            .json(&payload)
            .send()
            .await
            .map_err(|e| e.to_string())?;
        if resp.status().is_success() {
            Ok(())
        } else {
            Err(format!("fcm {}", resp.status()))
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn service_account_parse() {
        let sa = ServiceAccount::parse(
            r#"{"client_email":"a@b.iam.gserviceaccount.com","private_key":"x","project_id":"qoshul"}"#,
        );
        assert!(sa.is_some());
        let s = sa.unwrap();
        assert_eq!(s.project_id, "qoshul");
    }

    #[test]
    fn oauth_token_test_acarı_ilə() {
        let pem = include_str!("../tests/fixtures/test_key.pem");
        let sa = ServiceAccount {
            client_email: "test@qoshul.iam.gserviceaccount.com".into(),
            private_key: pem.into(),
            project_id: "qoshul-test".into(),
        };
        let jwt = oauth_token(&sa);
        assert!(jwt.is_ok(), "jwt imzalanmalı: {:?}", jwt.err());
        let j = jwt.unwrap();
        assert_eq!(j.split('.').count(), 3);

        // Doğrulama: openssl ilə (jsonwebtoken decode-roundtrip bu mühitdə
        // InvalidSignature verir — D-082; encode özü openssl ilə SÜBUTLU)
        let dir = std::env::temp_dir().join("qoshul-jwt-test");
        std::fs::create_dir_all(&dir).unwrap();
        let input = dir.join("input.txt");
        let sigf = dir.join("sig.bin");
        let parts: Vec<&str> = j.split('.').collect();
        std::fs::write(&input, format!("{}.{}", parts[0], parts[1])).unwrap();
        // URL-safe b64 → raw imza (padding əlavəsi ilə)
        let pad = |s: &str| -> String {
            let n = (4 - s.len() % 4) % 4;
            format!("{s}{}", "=".repeat(n))
                .replace('-', "+")
                .replace('_', "/")
        };
        let raw = {
            // URL-safe b64 → standart b64 → openssl ilə decode
            let std_b64 = pad(parts[2]);
            let bytes = std::process::Command::new("openssl")
                .arg("base64")
                .arg("-d")
                .arg("-A")
                .stdin(std::process::Stdio::piped())
                .stdout(std::process::Stdio::piped())
                .spawn()
                .and_then(|mut c| {
                    use std::io::Write;
                    c.stdin.take().unwrap().write_all(std_b64.as_bytes()).ok();
                    c.wait_with_output()
                })
                .map(|o| o.stdout)
                .unwrap_or_default();
            bytes
        };
        std::fs::write(&sigf, &raw).unwrap();
        let out = std::process::Command::new("openssl")
            .arg("dgst")
            .arg("-sha256")
            .arg("-prverify")
            .arg("tests/fixtures/test_key.pem") // birbaşa private açar (test cwd = service/)
            .arg("-signature")
            .arg(&sigf)
            .arg(&input)
            .output()
            .expect("openssl çağırışı");
        let msg = format!(
            "{}{}",
            String::from_utf8_lossy(&out.stdout),
            String::from_utf8_lossy(&out.stderr)
        );
        assert!(
            out.status.success() && msg.contains("Verified OK"),
            "openssl imza doğrulaması: {msg}"
        );
    }
}
