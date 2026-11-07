//! M4 inteqrasiya testləri — FakeBackend (şəbəkə YOX) + axum router
//! (tower::ServiceExt::oneshot) + JWT (fixtures/test_key.pem — test-only).

use async_trait::async_trait;
use serde_json::{json, Value};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Mutex};

use qoshul_service::supabase::{RestError, SupabaseRest};
use qoshul_service::{build_router, AppState};

/// D-092: vaxt-bombası ləğvi — 24s pəncərəsi (20-28 saat, jobs.rs)
/// həmişə tutması üçün resv = AZ-indi + 24 saat. Əvvəlki HARDCODED
/// "2026-11-08 10:00" yalnız AZ 06:00-14:00 aralığında işləyirdi —
/// kənarda əbədi uğursuz (CI: 01:54 UTC 28h → keçdi, 13:44 UTC 16h → düşdü).
fn sabah_24s() -> (String, String) {
    let t = chrono::Utc::now() + chrono::Duration::hours(4) + chrono::Duration::hours(24);
    (
        t.format("%Y-%m-%d").to_string(),
        t.format("%H:%M").to_string(),
    )
}

/// Saxta backend — çağırışları QEYD edir (idempotensiya sübutları üçün).
struct FakeBackend {
    reservations: Vec<Value>,
    patch_calls: Mutex<Vec<(String, Value)>>,
    insert_calls: Mutex<Vec<(String, Value)>>,
    rpc_calls: Mutex<Vec<(String, Value)>>,
    patch_result_rows: AtomicUsize, // 0 → boş (yeni deyil), n → uğur
}

impl FakeBackend {
    fn new(reservations: Vec<Value>) -> Self {
        Self {
            reservations,
            patch_calls: Mutex::new(vec![]),
            insert_calls: Mutex::new(vec![]),
            rpc_calls: Mutex::new(vec![]),
            patch_result_rows: AtomicUsize::new(1),
        }
    }
    fn make_patch_succeed(&self, rows: usize) {
        self.patch_result_rows.store(rows, Ordering::SeqCst);
    }
}

#[async_trait]
impl SupabaseRest for FakeBackend {
    async fn select(&self, table: &str, query: &str) -> Result<Vec<Value>, RestError> {
        if table == "reservations" {
            return Ok(self.reservations.clone());
        }
        if table == "reports" && query.contains("status=eq.OPEN") {
            // 35 günlük stale report
            return Ok(vec![
                json!({"id": "rep1", "created_at": "2026-10-01T00:00:00Z"}),
            ]);
        }
        if table == "reviews" && query.contains("status=eq.PENDING") {
            return Ok(vec![
                json!({"id": "rev1", "created_at": "2026-10-31T00:00:00Z"}),
            ]);
        }
        if table == "push_tokens" {
            return Ok(vec![json!({"token": "device-token-1"})]);
        }
        Ok(vec![])
    }
    async fn patch(&self, table: &str, filter: &str, body: Value) -> Result<Vec<Value>, RestError> {
        self.patch_calls
            .lock()
            .unwrap()
            .push((format!("{table}?{filter}"), body));
        let n = self.patch_result_rows.load(Ordering::SeqCst);
        Ok(vec![json!({"id": "x"}); n])
    }
    async fn insert(&self, table: &str, body: Value) -> Result<Vec<Value>, RestError> {
        self.insert_calls.lock().unwrap().push((table.into(), body));
        Ok(vec![json!({"id": "audit"})])
    }
    async fn rpc(&self, fn_name: &str, params: Value) -> Result<Value, RestError> {
        self.rpc_calls
            .lock()
            .unwrap()
            .push((fn_name.into(), params));
        Ok(json!({"ok": true}))
    }
}

fn test_state(fake: Arc<FakeBackend>) -> Arc<AppState> {
    Arc::new(AppState {
        db: fake,
        fcm: Arc::new(qoshul_service::fcm::FcmSender::new("PLACEHOLDER")),
        admin_token: "test-admin-token".into(),
        cfg: qoshul_service::Config::from_env(),
    })
}

use tower::util::ServiceExt;

async fn get_json(
    router: axum::Router,
    path: &str,
    token: Option<&str>,
) -> (axum::http::StatusCode, Value) {
    let mut req = axum::http::Request::builder().uri(path);
    if let Some(t) = token {
        req = req.header("x-admin-token", t);
    }
    let resp = router
        .oneshot(req.body(axum::body::Body::empty()).unwrap())
        .await
        .unwrap();
    let status = resp.status();
    let bytes = axum::body::to_bytes(resp.into_body(), usize::MAX)
        .await
        .unwrap();
    let v = serde_json::from_slice(&bytes).unwrap_or(Value::Null);
    (status, v)
}

async fn post_json(
    router: axum::Router,
    path: &str,
    token: Option<&str>,
    body: Value,
) -> (axum::http::StatusCode, Value) {
    let mut req = axum::http::Request::builder()
        .method("POST")
        .uri(path)
        .header("content-type", "application/json");
    if let Some(t) = token {
        req = req.header("x-admin-token", t);
    }
    let resp = router
        .oneshot(req.body(axum::body::Body::from(body.to_string())).unwrap())
        .await
        .unwrap();
    let status = resp.status();
    let bytes = axum::body::to_bytes(resp.into_body(), usize::MAX)
        .await
        .unwrap();
    let v = serde_json::from_slice(&bytes).unwrap_or(Value::Null);
    (status, v)
}

#[tokio::test]
async fn health_placeholder_degrade() {
    let fake = Arc::new(FakeBackend::new(vec![]));
    let st = test_state(fake);
    let (status, v) = get_json(build_router(st), "/health", None).await;
    assert_eq!(status, axum::http::StatusCode::OK);
    assert_eq!(v["ok"], json!(true));
    assert_eq!(v["fcm"], json!("PLACEHOLDER"));
    assert_eq!(v["admin_api"], json!("CONFIGURED"));
}

#[tokio::test]
async fn reminders_idempotent_ikinci_tur_gormur() {
    // sabah 10:00 rezervi — run_reminders_now öz `now`-unu götürür;
    // plan_reminder-in SAF testləri jobs.rs-də (pəncərələr orada sübutlu)
    let fake = Arc::new(FakeBackend::new(vec![json!({
        "id": "r1", "profile_id": "p1",
        "resv_date": sabah_24s().0, "resv_time": sabah_24s().1,
        "reminder_24h_sent_at": null, "reminder_2h_sent_at": null,
    })]));
    let st = test_state(fake.clone());
    let router = build_router(st);
    // D-092: resv dinamikdir (AZ-indi+24s) — run_reminders_now real now
    // istifadə edir, pəncərə 20-28s həmişə tutur:
    // (plan_reminder-in saf testləri jobs.rs-də; burada HTTP təbəqəsi yoxlanılır)
    let (status, v) = post_json(router.clone(), "/jobs/reminders/run", None, json!({})).await;
    assert_eq!(status, axum::http::StatusCode::OK);
    assert!(v.get("reminders_sent").is_some());
    // İDEMPOTENSİYA: patch çağırışı və yalnız sent_at NULL şərti ilə
    let calls = fake.patch_calls.lock().unwrap();
    assert!(
        calls
            .iter()
            .any(|(f, _)| f.starts_with("reservations?id=eq.r1")
                && (f.contains("reminder_24h_sent_at.is.null")
                    || f.contains("reminder_2h_sent_at.is.null"))),
        "idempotent filter olmalı: {calls:?}"
    );
}

#[tokio::test]
async fn reminders_patch_bos_qaytarir_sa_gondermir() {
    let fake = Arc::new(FakeBackend::new(vec![json!({
        "id": "r9", "profile_id": "p1",
        "resv_date": sabah_24s().0, "resv_time": sabah_24s().1,
        "reminder_24h_sent_at": null, "reminder_2h_sent_at": null,
    })]));
    fake.make_patch_succeed(0); // «başqa tur artıq göndərib»
    let st = test_state(fake.clone());
    let (status, v) = post_json(build_router(st), "/jobs/reminders/run", None, json!({})).await;
    assert_eq!(status, axum::http::StatusCode::OK);
    assert_eq!(v["reminders_sent"], 0); // boş → sayılmır
}

#[tokio::test]
async fn moderation_job_stale_report_dismissed() {
    let fake = Arc::new(FakeBackend::new(vec![]));
    let st = test_state(fake.clone());
    let (status, v) = post_json(build_router(st), "/jobs/moderation/run", None, json!({})).await;
    assert_eq!(status, axum::http::StatusCode::OK);
    assert!(v["dismissed"].as_i64().unwrap_or(0) >= 1, "{v}");
    assert!(v["approved"].as_i64().unwrap_or(0) >= 1, "{v}");
    // rpc: fn_moderate_review çağırılıb (approve)
    let rpc = fake.rpc_calls.lock().unwrap();
    assert!(rpc.iter().any(|(f, _)| f == "fn_moderate_review"));
    // patch: reports DISMISSED
    let patches = fake.patch_calls.lock().unwrap();
    assert!(patches
        .iter()
        .any(|(f, b)| f.starts_with("reports?id=eq.rep1") && b["status"] == json!("DISMISSED")));
}

#[tokio::test]
async fn admin_token_qapısı() {
    let fake = Arc::new(FakeBackend::new(vec![]));
    let st = test_state(fake.clone());
    let router = build_router(st);
    // yanlış token → 403
    let (s, _) = get_json(router.clone(), "/admin/reports", Some("wrong")).await;
    assert_eq!(s, axum::http::StatusCode::FORBIDDEN);
    // düzgün token → 200 + sıra
    let (s, v) = get_json(router.clone(), "/admin/reports", Some("test-admin-token")).await;
    assert_eq!(s, axum::http::StatusCode::OK);
    assert!(v.as_array().is_some());
    // resolve → audit insert
    let (s, v) = post_json(
        router.clone(),
        "/admin/reports/rep2",
        Some("test-admin-token"),
        json!({"action": "RESOLVED"}),
    )
    .await;
    assert_eq!(s, axum::http::StatusCode::OK, "{v}");
    let inserts = fake.insert_calls.lock().unwrap();
    assert!(inserts
        .iter()
        .any(|(t, b)| t == "admin_actions" && b["action"] == json!("REPORT_RESOLVED")));
}

#[tokio::test]
async fn admin_placeholder_qapalı() {
    // ADMIN_TOKEN PLACEHOLDER → admin 403, /health açıq
    let fake = Arc::new(FakeBackend::new(vec![]));
    let st = Arc::new(AppState {
        db: fake,
        fcm: Arc::new(qoshul_service::fcm::FcmSender::new("PLACEHOLDER")),
        admin_token: "PLACEHOLDER".into(),
        cfg: qoshul_service::Config::from_env(),
    });
    let (s, _) = get_json(
        build_router(st.clone()),
        "/admin/reports",
        Some("PLACEHOLDER"),
    )
    .await;
    assert_eq!(s, axum::http::StatusCode::FORBIDDEN);
    let (s, v) = get_json(build_router(st), "/health", None).await;
    assert_eq!(s, axum::http::StatusCode::OK);
    assert_eq!(v["admin_api"], json!("PLACEHOLDER"));
}

#[tokio::test]
async fn restrict_profile_profile_patch() {
    let fake = Arc::new(FakeBackend::new(vec![]));
    let st = test_state(fake.clone());
    let (s, _) = post_json(
        build_router(st),
        "/admin/profiles/p9",
        Some("test-admin-token"),
        json!({"days": 7, "reason": "test"}),
    )
    .await;
    assert_eq!(s, axum::http::StatusCode::OK);
    let patches = fake.patch_calls.lock().unwrap();
    assert!(patches
        .iter()
        .any(|(f, b)| f.starts_with("profiles?id=eq.p9") && b["restricted_until"].is_string()));
}

#[tokio::test]
async fn fcm_jwt_test_acarı() {
    // fcm.rs-dəki unit testlər artıq; burada yalnız enabled() degrade
    let f = qoshul_service::fcm::FcmSender::new("PLACEHOLDER");
    assert!(!f.enabled());
    let err = f.send("tok", "t", "b").await;
    assert!(err.is_err());
}
