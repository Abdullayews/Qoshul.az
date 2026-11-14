//! M4 idempotent joblar (D-082).
//!
//! **Reminderlar (ROL E: 24s / 2s əvvəl):** reservations cədvəlindən
//! tarixi gələn sətirlər; `reminder_*_sent_at` sütunları İDEMPOTENTLİYİ
//! DB-də təmin edir (UPDATE ... WHERE sent_at IS NULL RETURNING — təkrar
//! işə düşmə heç nə görmür). Push FCM (enabled olduqda) + notifications
//! cədvəlinə yazı (per-type toggle və sükut saatı SQL tərəfində).
//!
//! **Moderasiya:** OPEN report-lardan >30 günlüklər DISMISSED;
//! PENDING review-lər >7 gündürsə avtomatik APPROVE (B4 axını bağlanır).

use crate::fcm::FcmSender;
use crate::supabase::{columns, SupabaseRest};
use chrono::{DateTime, Timelike, Utc};
use serde_json::{json, Value};

/// Reminder planının SAF hissəsi — testlərdə DB olmadan yoxlanılır.
#[derive(Debug, Clone, PartialEq)]
pub struct ReminderAction {
    pub reservation_id: String,
    pub profile_id: String,
    pub kind: ReminderKind,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ReminderKind {
    Hours24,
    Hours2,
}

impl ReminderKind {
    pub const fn label(self) -> &'static str {
        match self {
            ReminderKind::Hours24 => "24 saat əvvəl",
            ReminderKind::Hours2 => "2 saat əvvəl",
        }
    }
    pub const fn notif_type(self) -> &'static str {
        match self {
            ReminderKind::Hours24 | ReminderKind::Hours2 => "RESV_REMINDER",
        }
    }
}

/// Resv_time "18:00" → dəqiqə.
fn parse_hhmm(s: &str) -> Option<(u32, u32)> {
    let (h, m) = s.split_once(':')?;
    Some((h.parse().ok()?, m.parse().ok()?))
}

/// Saf planlaşdırıcı: rezervlər (JSON) + indi → hansı reminder bu turda
/// göndərilməlidir. DB-də `reminder_*_sent_at IS NULL` filtri çağıran
/// tərəfdən tətbiq olunur (idempotensiya sütunu UPDATE-də).
pub fn plan_reminders(reservations: &[Value], now: DateTime<Utc>) -> Vec<ReminderAction> {
    let mut out = Vec::new();
    for r in reservations {
        let Some(id) = r.get("id").and_then(Value::as_str) else {
            continue;
        };
        let Some(profile_id) = r.get("profile_id").and_then(Value::as_str) else {
            continue;
        };
        let Some(resv_date) = r.get("resv_date").and_then(Value::as_str) else {
            continue;
        };
        let Some(resv_time) = r.get("resv_time").and_then(Value::as_str) else {
            continue;
        };
        let Some((hh, mm)) = parse_hhmm(resv_time) else {
            continue;
        };
        // resv_date "YYYY-MM-DD" — vaxt zonası: AZ (UTC+4) — D-073; layihə TZ-si
        let (y, m, d) = (
            resv_date
                .get(0..4)
                .and_then(|s| s.parse::<i32>().ok())
                .unwrap_or(2026),
            resv_date
                .get(5..7)
                .and_then(|s| s.parse::<u32>().ok())
                .unwrap_or(1),
            resv_date
                .get(8..10)
                .and_then(|s| s.parse::<u32>().ok())
                .unwrap_or(1),
        );
        // AZ vaxtında (UTC+4) «indi» — D-073: layihə TZ Asia/Baku
        let az_now = now + chrono::Duration::hours(4);
        let az_date = az_now.date_naive();
        let target = match chrono::NaiveDate::from_ymd_opt(y, m, d) {
            Some(t) => t,
            None => continue,
        };
        let days = (target - az_date).num_days();
        let (hours_until, mins_until) = {
            let minutes_now = az_now.hour() * 60 + az_now.minute();
            let minutes_target = hh * 60 + mm;
            let delta = days * 24 * 60 + (minutes_target as i64 - minutes_now as i64);
            (delta / 60, delta)
        };
        // 24s: sabahın rezervi, indidən 20-28 saat qalıbsa (bir dəfəlik pəncərə)
        if (20..=28).contains(&hours_until) {
            out.push(ReminderAction {
                reservation_id: id.to_string(),
                profile_id: profile_id.to_string(),
                kind: ReminderKind::Hours24,
            });
        }
        // 2s: bugünkü rezerv, 90-150 dəq qalıb
        if days == 0 && (90..=150).contains(&mins_until) {
            out.push(ReminderAction {
                reservation_id: id.to_string(),
                profile_id: profile_id.to_string(),
                kind: ReminderKind::Hours2,
            });
        }
    }
    out
}

/// Reminder icraçısı — idempotent UPDATE + notification + FCM.
pub async fn run_reminders(
    db: &dyn SupabaseRest,
    fcm: &FcmSender,
    now: DateTime<Utc>,
) -> Result<usize, String> {
    // aktiv rezervlər — F4 düzəlişi: İKİ select (24s və 2s üçün ayrı
    // idempotensiya sütunları). Köhnə tək filtri reminder_24h_sent_at=is.null
    // idi → 24s göndərilən kimi sətir selektdən düşürdülər və 2 SAATLIQ
    // xatırlatma heç vaxt seçilmirdi.
    let base_cols =
        "select=id,profile_id,resv_date,resv_time,reminder_24h_sent_at,reminder_2h_sent_at";
    let rows_24 = db
        .select(
            "reservations",
            &format!(
                "{base_cols}&status=in.(PENDING,CONFIRMED)&reminder_24h_sent_at=is.null&resv_date=gte.today"
            ),
        )
        .await
        .map_err(|e| e.to_string())?;
    let rows_2h = db
        .select(
            "reservations",
            &format!(
                "{base_cols}&status=in.(PENDING,CONFIRMED)&reminder_2h_sent_at=is.null&resv_date=gte.today"
            ),
        )
        .await
        .map_err(|e| e.to_string())?;
    let mut rows = rows_24;
    rows.extend(rows_2h);
    let plans = plan_reminders(&rows, now);
    let mut sent = 0usize;
    for action in plans {
        let col = match action.kind {
            ReminderKind::Hours24 => columns::REMINDER_24H,
            ReminderKind::Hours2 => columns::REMINDER_2H,
        };
        // İDEMPOTENT UPDATE: yalnız NULL-dursa yazılır; təkrar tur boş qayıdır
        let updated = db
            .patch(
                "reservations",
                &format!("id=eq.{}&{}.is.null", action.reservation_id, col),
                json!({ col: now.to_rfc3339() }),
            )
            .await
            .map_err(|e| e.to_string())?;
        if updated.is_empty() {
            continue; // başqa tur artıq göndərib — bu İDEMPOTENTLİYİN sübutudur
        }
        sent += 1;
        // F5 düzəlişi: köhnə kod fn_set_notif_pref çağırırdı — yəni
        // İSTİFADƏÇİNİN BİLDİRİŞ TƏRCİHİNİ yandırırdı, notification YAZMIRDI.
        // Əvəzinə service-only fn_push_notification (tərcih + sükut saatı
        // SQL tərəfində; qaytarır: push göndərilsinmi).
        let push_ok = db
            .rpc(
                "fn_push_notification",
                json!({
                    "p_profile_id": action.profile_id,
                    "p_type": action.kind.notif_type(),
                    "p_title": "Rezerv xatırlatması",
                    "p_body": action.kind.label(),
                }),
            )
            .await
            .ok()
            .and_then(|v: Value| v.as_bool())
            .unwrap_or(false);
        // FCM: push token-ləri (yalnız tərcihə görə + sükut saatı xaricində)
        if fcm.enabled() && push_ok {
            let tokens = db
                .select(
                    "push_tokens",
                    &format!("select=token&profile_id=eq.{}", action.profile_id),
                )
                .await
                .unwrap_or_default();
            for t in tokens {
                if let Some(tok) = t.get("token").and_then(Value::as_str) {
                    let _ = fcm
                        .send(tok, "Rezerv xatırlatması", action.kind.label())
                        .await;
                }
            }
        }
    }
    Ok(sent)
}

/// Moderasiya jobu: köhnə OPEN report-lar DISMISSED; >7 günlük PENDING
/// review-lər avtomatik APPROVE (fn_moderate_review → XP + həftəlik cap SQL-də);
/// vaxtı bitmiş ACTIVE kuponlar EXPIRED (F9 — fn_expire_coupons əvvəl heç
/// kim tərəfindən çağırılmırdı).
pub async fn run_moderation(db: &dyn SupabaseRest) -> Result<(usize, usize, usize), String> {
    // 1) report-lar
    let open = db
        .select("reports", "select=id,created_at&status=eq.OPEN")
        .await
        .map_err(|e| e.to_string())?;
    let mut dismissed = 0usize;
    for r in &open {
        let created = r
            .get("created_at")
            .and_then(Value::as_str)
            .and_then(|s| DateTime::parse_from_rfc3339(s).ok());
        let is_stale = created
            .map(|c| Utc::now() - c.with_timezone(&Utc) > chrono::Duration::days(30))
            .unwrap_or(false);
        if !is_stale {
            continue;
        }
        let id = r.get("id").and_then(Value::as_str).unwrap_or_default();
        let updated = db
            .patch(
                "reports",
                &format!("id=eq.{id}&status=eq.OPEN"),
                json!({"status": "DISMISSED"}),
            )
            .await
            .map_err(|e| e.to_string())?;
        dismissed += updated.len();
    }
    // 2) review-lər — APPROVE RPC (idempotent: yalnız PENDING-də təsir edir)
    let pending = db
        .select("reviews", "select=id,created_at&status=eq.PENDING")
        .await
        .map_err(|e| e.to_string())?;
    let mut approved = 0usize;
    for r in &pending {
        let created = r
            .get("created_at")
            .and_then(Value::as_str)
            .and_then(|s| DateTime::parse_from_rfc3339(s).ok());
        let is_old = created
            .map(|c| Utc::now() - c.with_timezone(&Utc) > chrono::Duration::days(7))
            .unwrap_or(false);
        if !is_old {
            continue;
        }
        let id = r.get("id").and_then(Value::as_str).unwrap_or_default();
        let res = db
            .rpc(
                "fn_moderate_review",
                json!({ "p_review_id": id, "p_approve": true }),
            )
            .await;
        if res.is_ok() {
            approved += 1;
        }
    }
    // 3) F9: kupon köhnəlmə işi — fn_expire_coupons idempotentdir
    //    (yalnız ACTIVE + vaxtı keçmiş sətirlər UPDATE olunur).
    let expired = db
        .rpc("fn_expire_coupons", json!({}))
        .await
        .ok()
        .and_then(|v| v.as_i64())
        .unwrap_or(0) as usize;
    Ok((dismissed, approved, expired))
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::TimeZone;

    fn resv(id: &str, date: &str, time: &str) -> Value {
        json!({
            "id": id, "profile_id": "p1", "resv_date": date, "resv_time": time,
            "reminder_24h_sent_at": null, "reminder_2h_sent_at": null,
        })
    }

    #[test]
    fn plan_24h_pəncərəsi() {
        // indi: 2026-11-07 12:00 AZ-da; sabah 18:00 → 30 saat yox
        let now = Utc.with_ymd_and_hms(2026, 11, 7, 8, 0, 0).unwrap(); // 12:00 AZ
                                                                       // sabah 18:00 = indidən 30 saat → pəncərə xarici (20-28 deyil)
        let rows = vec![resv("r1", "2026-11-08", "18:00")];
        assert!(plan_reminders(&rows, now).is_empty());
        // sabah 10:00 = 22 saat → 24s pəncərəsi DAXİL
        let rows = vec![resv("r2", "2026-11-08", "10:00")];
        let plan = plan_reminders(&rows, now);
        assert_eq!(plan.len(), 1);
        assert_eq!(plan[0].kind, ReminderKind::Hours24);
        assert_eq!(plan[0].reservation_id, "r2");
    }

    #[test]
    fn plan_2h_pəncərəsi() {
        let now = Utc.with_ymd_and_hms(2026, 11, 7, 8, 0, 0).unwrap(); // 12:00 AZ
                                                                       // bugün 15:30 → 210 dəq → 2s pəncərəsi (90-150) xarici
        assert!(plan_reminders(&[resv("r1", "2026-11-07", "15:30")], now).is_empty());
        // bugün 13:45 → 105 dəq → DAXİL
        let plan = plan_reminders(&[resv("r2", "2026-11-07", "13:45")], now);
        assert_eq!(plan.len(), 1);
        assert_eq!(plan[0].kind, ReminderKind::Hours2);
    }

    #[test]
    fn plan_xətalı_vaxt_ötürülür() {
        let now = Utc.with_ymd_and_hms(2026, 11, 7, 8, 0, 0).unwrap();
        let rows = vec![
            resv("r1", "2026-11-08", "25:99"),
            resv("r2", "bad-date", "10:00"),
        ];
        assert!(plan_reminders(&rows, now).is_empty());
    }
}
