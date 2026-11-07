//! Admin panel API (D-082) — N9: moderator/admin alətləri.
//!
//! Autentifikasiya: `X-Admin-Token` başlığı == ADMIN_TOKEN env (PLACEHOLDER
//! → 403; yalnız /health açıq). Service service_role ilə Supabase-ə gedir.
//! Hər manual əməliyyat `admin_actions` cədvəlinə audit yazır (N9).

use axum::extract::{Path, Query, State};
use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::Json;
use serde_json::{json, Value};

use crate::AppState;

fn forbidden() -> axum::response::Response {
    (StatusCode::FORBIDDEN, Json(json!({"error": "FORBIDDEN"}))).into_response()
}

/// X-Admin-Token başlığı DƏYƏRİ server tokeni ilə eyni olmalıdır
/// (yalnız «konfiq var» yoxlaması KİFAYƏT DEYİL — dəyər yoxlanır).
fn auth_ok(st: &AppState, headers: &axum::http::HeaderMap) -> bool {
    st.admin_ok()
        && headers
            .get("x-admin-token")
            .and_then(|v| v.to_str().ok())
            .map(|v| constant_time_eq(v, &st.admin_token))
            .unwrap_or(false)
}

/// Sabit-vaxt müqayisə (timing attack qarşısı).
fn constant_time_eq(a: &str, b: &str) -> bool {
    if a.len() != b.len() {
        return false;
    }
    a.bytes()
        .zip(b.bytes())
        .fold(0u8, |acc, (x, y)| acc | (x ^ y))
        == 0
}

fn bad_gateway(e: crate::supabase::RestError) -> axum::response::Response {
    (
        StatusCode::BAD_GATEWAY,
        Json(json!({"error": e.to_string()})),
    )
        .into_response()
}

async fn audit(
    st: &std::sync::Arc<AppState>,
    action: &str,
    target_type: &str,
    target_id: &str,
    note: &str,
) {
    // səs udulur — audit yazılması əməliyyatın uğurunu bloklamamalıdır
    let _ = st
        .db
        .insert(
            "admin_actions",
            json!({
                "actor_id": "00000000-0000-0000-0000-000000000000", // service (system)
                "action": action, "target_type": target_type,
                "target_id": target_id, "note": note,
            }),
        )
        .await;
}

pub async fn list_reports(
    State(st): State<std::sync::Arc<AppState>>,
    headers: axum::http::HeaderMap,
    Query(q): Query<Value>,
) -> axum::response::Response {
    if !auth_ok(&st, &headers) {
        return forbidden();
    }
    let status = q.get("status").and_then(Value::as_str).unwrap_or("OPEN");
    match st
        .db
        .select(
            "reports",
            &format!("select=*&status=eq.{status}&order=created_at"),
        )
        .await
    {
        Ok(rows) => (StatusCode::OK, Json(Value::Array(rows))).into_response(),
        Err(e) => bad_gateway(e),
    }
}

pub async fn resolve_report(
    State(st): State<std::sync::Arc<AppState>>,
    headers: axum::http::HeaderMap,
    Path(id): Path<String>,
    Json(body): Json<Value>,
) -> axum::response::Response {
    if !auth_ok(&st, &headers) {
        return forbidden();
    }
    let action = body
        .get("action")
        .and_then(Value::as_str)
        .unwrap_or("RESOLVED");
    if !matches!(action, "RESOLVED" | "DISMISSED") {
        return (
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "action ∈ RESOLVED|DISMISSED"})),
        )
            .into_response();
    }
    match st
        .db
        .patch(
            "reports",
            &format!("id=eq.{id}&status=eq.OPEN"),
            json!({"status": action}),
        )
        .await
    {
        Ok(rows) if !rows.is_empty() => {
            audit(&st, &format!("REPORT_{action}"), "REPORT", &id, "admin API").await;
            (
                StatusCode::OK,
                Json(json!({"resolved": id, "action": action})),
            )
                .into_response()
        }
        Ok(_) => (
            StatusCode::NOT_FOUND,
            Json(json!({"error": "tapılmadı / artıq qapalı"})),
        )
            .into_response(),
        Err(e) => bad_gateway(e),
    }
}

pub async fn restrict_profile(
    State(st): State<std::sync::Arc<AppState>>,
    headers: axum::http::HeaderMap,
    Path(id): Path<String>,
    Json(body): Json<Value>,
) -> axum::response::Response {
    if !auth_ok(&st, &headers) {
        return forbidden();
    }
    let unrestrict = body
        .get("unrestrict")
        .and_then(Value::as_bool)
        .unwrap_or(false);
    let payload = if unrestrict {
        json!({"restricted_until": null, "restriction_reason": ""})
    } else {
        let days = body
            .get("days")
            .and_then(Value::as_i64)
            .unwrap_or(7)
            .clamp(1, 365);
        let reason = body
            .get("reason")
            .and_then(Value::as_str)
            .unwrap_or("admin qərarı");
        let until = chrono::Utc::now() + chrono::Duration::days(days);
        json!({"restricted_until": until.to_rfc3339(), "restriction_reason": reason})
    };
    match st
        .db
        .patch("profiles", &format!("id=eq.{id}"), payload)
        .await
    {
        Ok(rows) if !rows.is_empty() => {
            audit(
                &st,
                if unrestrict { "UNRESTRICT" } else { "RESTRICT" },
                "PROFILE",
                &id,
                "admin API",
            )
            .await;
            (StatusCode::OK, Json(json!({"profile": id}))).into_response()
        }
        Ok(_) => (
            StatusCode::NOT_FOUND,
            Json(json!({"error": "profil tapılmadı"})),
        )
            .into_response(),
        Err(e) => bad_gateway(e),
    }
}

pub async fn moderate_review(
    State(st): State<std::sync::Arc<AppState>>,
    headers: axum::http::HeaderMap,
    Path(id): Path<String>,
    Json(body): Json<Value>,
) -> axum::response::Response {
    if !auth_ok(&st, &headers) {
        return forbidden();
    }
    let approve = body.get("approve").and_then(Value::as_bool).unwrap_or(true);
    match st
        .db
        .rpc(
            "fn_moderate_review",
            json!({ "p_review_id": id, "p_approve": approve }),
        )
        .await
    {
        Ok(_) => {
            audit(
                &st,
                if approve {
                    "REVIEW_APPROVE"
                } else {
                    "REVIEW_REJECT"
                },
                "REVIEW",
                &id,
                "admin API",
            )
            .await;
            (
                StatusCode::OK,
                Json(json!({"moderated": id, "approved": approve})),
            )
                .into_response()
        }
        Err(e) => bad_gateway(e),
    }
}

pub async fn list_actions(
    State(st): State<std::sync::Arc<AppState>>,
    headers: axum::http::HeaderMap,
) -> axum::response::Response {
    if !auth_ok(&st, &headers) {
        return forbidden();
    }
    match st
        .db
        .select("admin_actions", "select=*&order=created_at.desc&limit=100")
        .await
    {
        Ok(rows) => (StatusCode::OK, Json(Value::Array(rows))).into_response(),
        Err(e) => bad_gateway(e),
    }
}
