//! Qaydalar — Kotlin `XpRules` (Models.kt:140-241) pariteti.
//!
//! Bütün dəyərlər add adda Kotlin sabitlərinə bərabərdir; dəyişiklik
//! YALNIZ hər iki tərəfdə birlikdə aparıla bilər (paritet testləri qayır).

/// Səviyyələr (ROL F) — `XpRules.rankMax`/`rankNames`.
pub const RANK_MAX: [i32; 5] = [99, 299, 699, 1499, 2999];
pub const RANK_NAMES: [&str; 6] = ["Yeni", "Kəşfiyyatçı", "Aktiv", "Sosial", "Lider", "Əfsanə"];

// — XP 1: peşəkarlıq (xərclənmir) — Models.kt:143-154 —
pub const REGISTRATION_XP: i32 = 50;
pub const PROFILE_COMPLETE_XP: i32 = 30;
pub const PHONE_VERIFY_XP: i32 = 40;
pub const DAILY_LOGIN_XP: i32 = 5;
pub const STREAK_BONUS_XP: i32 = 30;
pub const MATCH_XP: i32 = 10;
pub const JOIN_XP: i32 = 10;
pub const CHECK_IN_XP: i32 = 40;
pub const HOSTING_XP: i32 = 60;
pub const RESERVATION_XP: i32 = 30;
pub const REVIEW_XP: i32 = 15;
pub const INVITE_XP: i32 = 100;

// — limitlər —
pub const MATCH_XP_DAILY_CAP: i32 = 3;
pub const REVIEW_XP_WEEKLY_CAP: i32 = 3;
pub const HOSTING_MIN_PARTICIPANTS: i32 = 3;
pub const NO_SHOW_LIMIT: i32 = 3;
pub const NO_SHOW_RESTRICTION_DAYS: i32 = 7;
pub const COUPON_VALIDITY_DAYS: i32 = 14;

// — D-073 seriya semantikası (B2) —
pub const STREAK_FREEZE_DAYS: i64 = 2;
pub const STREAK_RESET_DAYS: i64 = 3;

// — ROL B —
pub const MIN_AGE: i32 = 18;
pub const MIN_INTERESTS: i32 = 3;
pub const BIO_LIMIT: i32 = 150;
pub const MAX_PHOTOS: i32 = 6;

// — B1: check-in radiusu (ölü sabit deyil — geo.rs istifadə edir) —
pub const CHECK_IN_MAX_DISTANCE_M: f64 = 200.0;

// — L.3/L.4-1 —
pub const ORGANIZER_XP: i32 = 50;
pub const TAGGED_PARTICIPANT_XP: i32 = 25;
pub const XP_CARD_TTL_SECONDS: i64 = 60;
pub const FALLBACK_CODE_DIGITS: usize = 6;

/// `XpRules.rankIndexForXp` pariteti: xp<=99→0 … else 5.
pub const fn rank_index(xp: i32) -> usize {
    if xp <= RANK_MAX[0] {
        0
    } else if xp <= RANK_MAX[1] {
        1
    } else if xp <= RANK_MAX[2] {
        2
    } else if xp <= RANK_MAX[3] {
        3
    } else if xp <= RANK_MAX[4] {
        4
    } else {
        5
    }
}

/// `XpRules.rankForXp`.
pub const fn rank_name(xp: i32) -> &'static str {
    RANK_NAMES[rank_index(xp)]
}

/// `XpRules.XpProgress` — A4 invariantı: **pct == 100 ⟺ toNext == 0**.
///
/// Kotlin pariteti (Models.kt:221-236): sərhəddə (99/299/…) pct 99-da
/// saxlanılır; 100% yalnız terminal haldadır (Əfsanə, xp ≥ 3000).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct XpProgress {
    pub next_name: Option<&'static str>,
    pub to_next: i32,
    pub pct: i32,
}

pub fn xp_progress(xp: i32) -> XpProgress {
    let mut lower: i32 = 0;
    let mut i = 0;
    while i < RANK_MAX.len() {
        let max = RANK_MAX[i];
        if xp <= max {
            let span = max - lower;
            let to_next = max + 1 - xp;
            // Kotlin: (((xp - lower) * 100.0 / span).toInt()).coerceIn(0, if toNext==0 {100} else {99})
            let raw = (xp - lower) as f64 * 100.0 / span as f64;
            let raw_int = raw as i32;
            let cap = if to_next == 0 { 100 } else { 99 };
            let pct = raw_int.clamp(0, cap);
            return XpProgress {
                next_name: Some(RANK_NAMES[i + 1]),
                to_next,
                pct,
            };
        }
        lower = max;
        i += 1;
    }
    XpProgress {
        next_name: None,
        to_next: 0,
        pct: 100,
    }
}

/// D-073: günlük giriş seriyası — `AppEngine.dailyLogin` (:351-368) pariteti.
///
///  * dünən gəlibsə (missed 0) → +1;
///  * 1-2 gün fasilə → DONDURULUR (dəyər yerində qalır);
///  * 3+ gün fasilə → 1-dən yenidən.
///
/// `last_day` = son qeydə alınan epoch-günü (None = heç vaxt).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct DailyLoginOutcome {
    pub streak: i32,
    /// Bu gün artıq sayılıbsa false (idempotent — heç nə dəyişmir).
    pub counted: bool,
    /// +30 bonus bu girişdə verilir (seriya 7-yə çatdı).
    pub bonus_due: bool,
}

pub fn daily_login(current_streak: i32, last_day: Option<i64>, today: i64) -> DailyLoginOutcome {
    if let Some(last) = last_day {
        if today <= last {
            return DailyLoginOutcome {
                streak: current_streak,
                counted: false,
                bonus_due: false,
            };
        }
    }
    let missed = match last_day {
        None => 0,
        Some(l) => today - l - 1,
    };
    let mut streak = if missed <= 0 {
        current_streak + 1
    } else if missed <= STREAK_FREEZE_DAYS {
        current_streak // dondurulma
    } else {
        1 // 3+ gün → sıfırlanma
    };
    let mut bonus = false;
    if streak == 7 {
        bonus = true;
        streak = 1; // dövrə yenidən başlayır
    }
    DailyLoginOutcome {
        streak,
        counted: true,
        bonus_due: bonus,
    }
}

/// L.1: Kəşf peşəkar sıralaması — `XpRules.proWeight(level)`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EventLevel {
    None,
    Beginner,     // „Başlanğıc"
    Intermediate, // „Orta"
    Professional, // „Peşəkar"
}

pub const fn pro_weight(level: EventLevel) -> i32 {
    match level {
        EventLevel::None => 0,
        EventLevel::Beginner => 1,
        EventLevel::Intermediate => 2,
        EventLevel::Professional => 3,
    }
}

/// `proWeight` üçün sətir → enum (Kotlin when pariteti).
pub fn pro_weight_str(level: Option<&str>) -> i32 {
    pro_weight(match level {
        Some("Başlanğıc") => EventLevel::Beginner,
        Some("Orta") => EventLevel::Intermediate,
        Some("Peşəkar") => EventLevel::Professional,
        _ => EventLevel::None,
    })
}

/// ROL H: sükut saatı 23:00–08:00 (Domain.kt:127).
pub const fn is_quiet_hour(hour: u32) -> bool {
    hour >= 23 || hour < 8
}

/// D-074: hərəkət limitsiz — yalnız XP mükafatı gündə max 3.
pub const fn match_xp_allowed(awarded_today: i32) -> bool {
    awarded_today < MATCH_XP_DAILY_CAP
}

/// ROL B: 18+ (Models.kt isAdult pariteti; `now` — çağıran tərəfdən).
pub fn is_adult(
    birth_year: i32,
    birth_month: u32,
    birth_day: u32,
    today_year: i32,
    today_month: u32,
    today_day: u32,
) -> bool {
    let mut age = today_year - birth_year;
    let had_birthday =
        today_month > birth_month || (today_month == birth_month && today_day >= birth_day);
    if !had_birthday {
        age -= 1;
    }
    age >= MIN_AGE
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rank_names_paritet() {
        assert_eq!(rank_name(0), "Yeni");
        assert_eq!(rank_name(99), "Yeni");
        assert_eq!(rank_name(100), "Kəşfiyyatçı");
        assert_eq!(rank_name(300), "Aktiv");
        assert_eq!(rank_name(700), "Sosial");
        assert_eq!(rank_name(1500), "Lider");
        assert_eq!(rank_name(3000), "Əfsanə");
        assert_eq!(rank_name(99999), "Əfsanə");
    }

    #[test]
    fn a4_pct_100_iff_to_next_0_sərhədlərdə_99() {
        // bütün 5 sərhəddə pct 99, toNext 1
        for &xp in RANK_MAX.iter() {
            let p = xp_progress(xp);
            assert_eq!(p.pct, 99, "sərhəddə pct 99 olmalıdır (xp={xp})");
            assert_eq!(p.to_next, 1);
        }
        // terminal
        let t = xp_progress(3000);
        assert_eq!(t.to_next, 0);
        assert_eq!(t.pct, 100);
        assert_eq!(t.next_name, None);
    }

    #[test]
    fn streak_dondurulma_və_sıfırlanma() {
        // ardıcıl
        let o = daily_login(3, Some(100), 101);
        assert_eq!(o.streak, 4);
        assert!(o.counted);
        assert!(!o.bonus_due);
        // 1 gün fasilə — dondurulma
        let o = daily_login(4, Some(100), 102);
        assert_eq!(o.streak, 4);
        // 2 gün fasilə — dondurulma
        let o = daily_login(4, Some(100), 103);
        assert_eq!(o.streak, 4);
        // 3 gün fasilə — sıfırlanma
        let o = daily_login(4, Some(100), 104);
        assert_eq!(o.streak, 1);
        // 7-yə çatma → bonus + dövrə
        let o = daily_login(6, Some(100), 101);
        assert_eq!(o.streak, 1);
        assert!(o.bonus_due);
        // eyni gün təkrar — idempotent
        let o = daily_login(5, Some(101), 101);
        assert!(!o.counted);
    }

    #[test]
    fn pro_weight_paritet() {
        assert_eq!(pro_weight_str(Some("Peşəkar")), 3);
        assert_eq!(pro_weight_str(Some("Orta")), 2);
        assert_eq!(pro_weight_str(Some("Başlanğıc")), 1);
        assert_eq!(pro_weight_str(None), 0);
        assert_eq!(pro_weight_str(Some("?")), 0);
    }

    #[test]
    fn quiet_hours() {
        assert!(is_quiet_hour(23));
        assert!(is_quiet_hour(3));
        assert!(!is_quiet_hour(8));
        assert!(!is_quiet_hour(22));
    }

    #[test]
    fn match_xp_cap() {
        assert!(match_xp_allowed(0));
        assert!(match_xp_allowed(2));
        assert!(!match_xp_allowed(3));
        assert!(!match_xp_allowed(10));
    }

    #[test]
    fn adult_paritet() {
        assert!(is_adult(2000, 1, 1, 2026, 11, 7));
        assert!(!is_adult(2010, 1, 1, 2026, 11, 7));
        // ad günü bu ay, hələ keçməyib
        assert!(!is_adult(2008, 11, 30, 2026, 11, 7));
    }
}
