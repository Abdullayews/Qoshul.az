//! Qoshul.az M4 mikroservisi — bin (D-082). Lib: qoshul_service.

use qoshul_service::config::Config;
use qoshul_service::fcm::FcmSender;
use qoshul_service::supabase::SupabaseClient;
use qoshul_service::{build_router, AppState};
use std::sync::Arc;

#[tokio::main]
async fn main() {
    let cfg = Config::from_env();
    let db = Arc::new(SupabaseClient::new(
        &cfg.supabase_url,
        &cfg.service_role_key,
    ));
    let fcm = Arc::new(FcmSender::new(&cfg.fcm_service_account));
    let admin_token = std::env::var("ADMIN_TOKEN")
        .ok()
        .filter(|v| !v.is_empty())
        .unwrap_or_else(|| "PLACEHOLDER".into());

    let state = Arc::new(AppState {
        db,
        fcm,
        admin_token,
        cfg: cfg.clone(),
    });

    // Job dövrü (cron əvəzinə interval — hər tur idempotentdir)
    {
        let st = state.clone();
        tokio::spawn(async move {
            loop {
                tokio::time::sleep(st.cfg.jobs_interval).await;
                let now = chrono::Utc::now();
                match qoshul_service::jobs::run_reminders(st.db.as_ref(), st.fcm.as_ref(), now)
                    .await
                {
                    Ok(n) if n > 0 => println!("[jobs] reminders: {n} göndərildi"),
                    Ok(_) => {}
                    Err(e) => eprintln!("[jobs] reminders xətası: {e}"),
                }
                match qoshul_service::jobs::run_moderation(st.db.as_ref()).await {
                    Ok((d, a, x)) if d + a + x > 0 => {
                        println!(
                            "[jobs] moderasiya: {d} dismissed, {a} approved, {x} kupon expired"
                        )
                    }
                    Ok(_) => {}
                    Err(e) => eprintln!("[jobs] moderasiya xətası: {e}"),
                }
            }
        });
    }

    let app = build_router(state.clone());
    let addr = format!("0.0.0.0:{}", cfg.port);
    println!(
        "qoshul-service {addr} — supabase: {}, fcm: {}, admin: {}",
        cfg.supabase_ready(),
        state.fcm.enabled(),
        state.admin_ok()
    );
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
