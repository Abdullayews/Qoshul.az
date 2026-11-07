//! Qoshul.az M4 mikroservisi (D-082) — axum 0.8.9 (lib).
//!
//! PLACEHOLDER konfiq (D-079): /health və sxematik cavablar; DB/FCM
//! əməliyyatları degrade — crash YOX.
//!
//! Endpointlər:
//!   GET  /health
//!   POST /jobs/reminders/run     (idempotent — DB sütunları)
//!   POST /jobs/moderation/run
//!   GET  /admin/reports?status=   (X-Admin-Token: ADMIN_TOKEN env)
//!   POST /admin/reports/{id}      {action: RESOLVED|DISMISSED}
//!   POST /admin/profiles/{id}     {days, reason} | {unrestrict: true}
//!   POST /admin/reviews/{id}      {approve: bool}
//!   GET  /admin/actions

pub mod admin;
pub mod config;
pub mod fcm;
pub mod jobs;
pub mod supabase;

pub use config::Config;

use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::routing::{get, post};
use axum::{Json, Router};
use serde_json::json;
use std::sync::Arc;

pub struct AppState {
    pub db: Arc<dyn supabase::SupabaseRest>,
    pub fcm: Arc<fcm::FcmSender>,
    pub admin_token: String,
    pub cfg: Config,
}

impl AppState {
    /// ADMIN_TOKEN PLACEHOLDER-dır → panel qapalı (yalnız /health açıq).
    pub fn admin_ok(&self) -> bool {
        self.admin_token != "PLACEHOLDER" && !self.admin_token.is_empty()
    }
}

pub fn build_router(state: Arc<AppState>) -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/jobs/reminders/run", post(run_reminders_now))
        .route("/jobs/moderation/run", post(run_moderation_now))
        .route("/admin/reports", get(admin::list_reports))
        .route("/admin/reports/{id}", post(admin::resolve_report))
        .route("/admin/profiles/{id}", post(admin::restrict_profile))
        .route("/admin/reviews/{id}", post(admin::moderate_review))
        .route("/admin/actions", get(admin::list_actions))
        .with_state(state)
}

async fn health(
    axum::extract::State(st): axum::extract::State<Arc<AppState>>,
) -> impl IntoResponse {
    (
        StatusCode::OK,
        Json(json!({
            "ok": true,
            "service": "qoshul-m4",
            "version": env!("CARGO_PKG_VERSION"),
            "supabase": if st.cfg.supabase_ready() { "CONFIGURED" } else { "PLACEHOLDER" },
            "fcm": if st.fcm.enabled() { "CONFIGURED" } else { "PLACEHOLDER" },
            "admin_api": if st.admin_ok() { "CONFIGURED" } else { "PLACEHOLDER" },
        })),
    )
}

async fn run_reminders_now(
    axum::extract::State(st): axum::extract::State<Arc<AppState>>,
) -> axum::response::Response {
    let now = chrono::Utc::now();
    match jobs::run_reminders(st.db.as_ref(), st.fcm.as_ref(), now).await {
        Ok(n) => (StatusCode::OK, Json(json!({"reminders_sent": n}))).into_response(),
        Err(e) => (StatusCode::BAD_GATEWAY, Json(json!({"error": e}))).into_response(),
    }
}

async fn run_moderation_now(
    axum::extract::State(st): axum::extract::State<Arc<AppState>>,
) -> axum::response::Response {
    match jobs::run_moderation(st.db.as_ref()).await {
        Ok((d, a)) => {
            (StatusCode::OK, Json(json!({"dismissed": d, "approved": a}))).into_response()
        }
        Err(e) => (StatusCode::BAD_GATEWAY, Json(json!({"error": e}))).into_response(),
    }
}
