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

    // D-156: Google Sign-In (master-spec §4.1). GOOGLE_CLIENT_IDS vergüllü
    // aud siyahısıdır (web_id,android_id — hər ikisi optional, istənilən biri
    // tək başına da işləyir). 28444 diaqnozu: setServerClientId yalnız WEB
    // tipli ID qəbul edir → WEB ID /config ilə APK-ya dinamik ötürülür.
    let google_client_ids = std::env::var("GOOGLE_CLIENT_IDS")
        .or_else(|_| std::env::var("GOOGLE_ANDROID_CLIENT_ID"))
        .unwrap_or_default();
    let google_web_client_id = std::env::var("GOOGLE_WEB_CLIENT_ID")
        .ok()
        .filter(|v| !v.is_empty() && v != "PLACEHOLDER");
    let google = if google_client_ids.is_empty() || google_client_ids == "PLACEHOLDER" {
        None
    } else {
        Some(Arc::new(qoshul_service::google_auth::GoogleVerifier::new(
            &google_client_ids,
        )))
    };
    // GoTrue admin: SUPABASE_URL=http://api-gw:8000 oldugundan auth servisine
    // birbasa (http://supabase-auth:9999) gedirik — Envoy'e ehtiyac yoxdur.
    let gotrue_admin = if cfg.supabase_ready() {
        let auth_base = std::env::var("GOTRUE_ADMIN_URL")
            .unwrap_or_else(|_| "http://supabase-auth:9999".into());
        Some(Arc::new(qoshul_service::google_auth::GoTrueAdmin::new(
            &auth_base,
            &cfg.service_role_key,
        )))
    } else {
        None
    };

    let state = Arc::new(AppState {
        db,
        fcm,
        admin_token,
        cfg: cfg.clone(),
        google,
        google_web_client_id,
        gotrue_admin,
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
