//! M4 (D-082): konfiqurasiya — BÜTÜN dəyərlər PLACEHOLDER ilə başlayır
//! (istifadəçi direktivi: „keçici placeholder qoy"). Service PLACEHOLDER
//! halında DEQRADE işləyir: FCM söndürülür, Supabase əməliyyatları xəta
//! qaytarır — amma /health və admin API sxematik cavab verir; crash YOX.

use std::time::Duration;

pub const PLACEHOLDER: &str = "PLACEHOLDER";

#[derive(Debug, Clone)]
pub struct Config {
    /// 0.0.0.0:8080 (container) — PORT env ilə dəyişilir.
    pub port: u16,
    pub supabase_url: String,
    /// YALNız mikroservisdə istifadə olunur — tətbiqə heç vaxt düşmür.
    pub service_role_key: String,
    /// FCM service-account JSON (PLACEHOLDER → push söndürülür).
    pub fcm_service_account: String,
    /// Jobların işə düşmə periodu (cron əvəzinə interval — sadə determinizm).
    pub jobs_interval: Duration,
}

impl Config {
    pub fn from_env() -> Self {
        Self {
            port: env_or("PORT", "8080").parse().unwrap_or(8080),
            supabase_url: env_or("SUPABASE_URL", PLACEHOLDER),
            service_role_key: env_or("SUPABASE_SERVICE_ROLE_KEY", PLACEHOLDER),
            fcm_service_account: env_or("FCM_SERVICE_ACCOUNT_JSON", PLACEHOLDER),
            jobs_interval: Duration::from_secs(
                env_or("JOBS_INTERVAL_SECS", "60").parse().unwrap_or(60),
            ),
        }
    }

    pub fn supabase_ready(&self) -> bool {
        self.supabase_url != PLACEHOLDER && self.service_role_key != PLACEHOLDER
    }

    pub fn fcm_ready(&self) -> bool {
        self.fcm_service_account != PLACEHOLDER
    }
}

fn env_or(key: &str, default: &str) -> String {
    std::env::var(key)
        .ok()
        .filter(|v| !v.is_empty())
        .unwrap_or_else(|| default.to_string())
}
