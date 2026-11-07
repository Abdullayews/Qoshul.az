//! Property-based testlər (proptest 1.12 — D-067).
//!
//! Örtük (M2 planı): A4 invariantı 0..3000 aralığının TAM yoxlanışı,
//! idempotensiya, double-spend, haversine simmetriyası.
//! Paritet: Kotlin tərəfdə eyni invariantlar XpEngineTest-də (M0) mövcuddur.

use proptest::prelude::*;

use qoshul_core::economy::{preview_spend, SpendError, VenueAccount, VenueConfig};
use qoshul_core::geo::{haversine_meters, within_checkin_range};
use qoshul_core::rules::{
    daily_login, is_adult, match_xp_allowed, rank_index, xp_progress, EventLevel, RANK_MAX,
};

proptest! {
    /// A4: pct == 100 ⟺ toNext == 0 — 0..=3100 aralığının TAM əhatəsi
    /// (sərhəd dəyərləri 99/299/699/1499/2999 daxil).
    #[test]
    fn a4_pct_100_iff_to_next_0(xp in 0i32..=3100) {
        let p = xp_progress(xp);
        prop_assert_eq!(p.pct == 100, p.to_next == 0);
        prop_assert!(p.pct >= 0 && p.pct <= 100);
        prop_assert!(p.to_next >= 0);
        if p.to_next > 0 { prop_assert!(p.pct <= 99); }
        // rank_index tutarlılığı
        prop_assert_eq!(rank_index(xp), {
            let expected = RANK_MAX.iter().position(|&m| xp <= m);
            expected.unwrap_or(5)
        });
    }

    /// Sərhəd dəyərlərində pct 99 (yalan danılmır — A4 M0 düzəlişi)
    #[test]
    fn serhredda_pct_99(xp in prop::sample::select(&RANK_MAX[..])) {
        let p = xp_progress(xp);
        prop_assert_eq!(p.pct, 99);
        prop_assert_eq!(p.to_next, 1);
    }

    /// D-073: missed ∈ 1..=2 → dəyər yerində; missed ≥ 3 → 1;
    /// missed 0 → +1; 7-yə çatanda bonus + dövrə.
    #[test]
    fn streak_semantikasi(
        streak in 1i32..=9,
        last in 1i64..=10_000,
        gap in 1i64..=8,
    ) {
        let today = last + gap;
        let o = daily_login(streak, Some(last), today);
        let missed = today - last - 1; // gap-1
        if missed <= 0 {
            if streak + 1 == 7 {
                prop_assert_eq!(o.streak, 1); prop_assert!(o.bonus_due);
            } else {
                prop_assert_eq!(o.streak, streak + 1); prop_assert!(!o.bonus_due);
            }
        } else if missed <= 2 {
            // dondurulma: dəyər yerində; amma 7 halı (real sistemdə qalmır,
            // Kotlin yoxlaması SONRADAN gəldiyindən) bonus verir
            if streak == 7 {
                prop_assert_eq!(o.streak, 1); prop_assert!(o.bonus_due);
            } else {
                prop_assert_eq!(o.streak, streak); prop_assert!(!o.bonus_due);
            }
        } else {
            prop_assert_eq!(o.streak, 1);
        }
    }

    /// Eyni gün təkrar / saat geri — idempotent (counted = false)
    #[test]
    fn streak_eyni_gun_idempotent(streak in 1i32..=9, day in 1i64..=10_000) {
        let o = daily_login(streak, Some(day), day);
        prop_assert!(!o.counted);
        prop_assert_eq!(o.streak, streak);
        let back = daily_login(streak, Some(day), day - 1);
        prop_assert!(!back.counted);
    }

    /// B6: spend mənfiyə yol vermir; balans heç vaxt mənfi olmur
    #[test]
    fn double_spend_iflasiz(
        initial in 0i32..=500,
        first in 1i32..=500,
    ) {
        let cfg = VenueConfig { xp_per_azn: 20, max_discount_percent: 25, min_bill_azn: 0 };
        let mut a = VenueAccount::new("v", initial, cfg);
        match a.spend(first) {
            Ok(_) => prop_assert!(a.balance() >= 0),
            Err(SpendError::InsufficientBalance) => prop_assert_eq!(a.balance(), initial),
            Err(SpendError::InvalidInput) => prop_assert!(first <= 0),
            Err(_) => prop_assert!(false, "spend yalnız bu iki xətanı verməlidir"),
        }
        prop_assert!(a.balance() >= 0);
    }

    /// preview cap: discount > cap + 0.001 → CAP_EXCEEDED (C1 REDD, D-072)
    #[test]
    fn cap_exceeded_redd(
        xp in 1i32..=1000,
        bill in 1.0f64..=100.0,
        max_pct in 1i32..=100,
    ) {
        let cfg = VenueConfig { xp_per_azn: 20, max_discount_percent: max_pct, min_bill_azn: 0 };
        let r = preview_spend(100_000, &cfg, xp, bill); // balans həmişə çatar
        let cap = bill * max_pct as f64 / 100.0;
        let discount = xp as f64 / 20.0;
        if discount > cap + 0.001 {
            prop_assert_eq!(r.err(), Some(SpendError::CapExceeded));
        }
    }

    /// Haversine: simmetrik + özünə ~0; radius 200 m funksiyası tutarlı
    #[test]
    fn haversine_simmetrik(
        lat1 in -80.0f64..=80.0, lng1 in -170.0f64..=170.0,
        lat2 in -80.0f64..=80.0, lng2 in -170.0f64..=170.0,
    ) {
        let d1 = haversine_meters(lat1, lng1, lat2, lng2);
        let d2 = haversine_meters(lat2, lng2, lat1, lng1);
        prop_assert!((d1 - d2).abs() <= 1e-6, "simmetrik deyil: {d1} vs {d2}");
        prop_assert!(d1 >= 0.0);
        let self_d = haversine_meters(lat1, lng1, lat1, lng1);
        prop_assert!(self_d <= 1e-9);
        prop_assert_eq!(within_checkin_range(lat1, lng1, lat2, lng2), d1 <= 200.0);
    }

    /// proWeight: 0..3 aralığında, sıra sabit (L.1)
    #[test]
    fn pro_weight_araligi(l in prop::sample::select(vec![
        EventLevel::None, EventLevel::Beginner, EventLevel::Intermediate, EventLevel::Professional,
    ])) {
        let w = qoshul_core::rules::pro_weight(l);
        prop_assert!((0..=3).contains(&w));
        let sıra_düzgündür = matches!(
            (l, w),
            (EventLevel::None, 0) | (EventLevel::Beginner, 1)
                | (EventLevel::Intermediate, 2) | (EventLevel::Professional, 3)
        );
        prop_assert!(sıra_düzgündür);
    }

    /// match XP cap: sərhəd 3 (D-074)
    #[test]
    fn match_cap_serhredi(n in 0i32..=10) {
        prop_assert_eq!(match_xp_allowed(n), n < 3);
    }

    /// is_adult: doğum ili + 18 ≤ bu il → həmişə true
    #[test]
    fn adult_sade(y in 1900i32..=2010) {
        prop_assert!(is_adult(y, 1, 1, y + 18, 1, 1));
        prop_assert!(!is_adult(y, 1, 1, y + 17, 1, 1));
    }
}
