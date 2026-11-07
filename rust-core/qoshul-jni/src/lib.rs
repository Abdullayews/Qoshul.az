//! Qoshul.az JNI sərhədi (M2, D-080).
//!
//! Kotlin tərəfi: `az.qoshul.app.core.rust.QoshulNative` (external funs).
//! Yükləmə ardıcıllığı: (1) `-Dqoshul.jni.path` (JVM testləri/CI host .so),
//! (2) `System.loadLibrary("qoshul_jni")` (Android jniLibs). Yüklənməyəndə
//! Kotlin fallback işləyir — paritet testləri hər iki yolu müqayisə edir.
//!
//! Səth qəsdən NUMERİK saxlanılıb (string JNI konversiyası YOXDUR):
//! award-idempotensiya pariteti qoshul-core testlərində + SQL unique
//! constraint-də sübut olunur; JNI yalnız TƏMİZ QAYDA funksiyalarını daşıyır.

#![allow(non_snake_case)] // JNI param adları Kotlin external fun-ları ilə eyni saxlanılır

use jni::objects::{JClass, JIntArray};
use jni::sys::{jboolean, jdouble, jint, jlong};
use jni::EnvUnowned;

use qoshul_core::economy::{preview_spend, SpendError, VenueConfig};
use qoshul_core::geo::{haversine_meters, within_checkin_range};
use qoshul_core::rules::{
    daily_login, is_quiet_hour, match_xp_allowed, rank_index, xp_progress, EventLevel,
    CHECK_IN_MAX_DISTANCE_M,
};

// jboolean jni-0.22-də `bool`-dur — yayda `as` cast yoxdur, birbaşa bool.

// ── Səviyyə / XP 1 (ROL F) ─────────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_rankIndexFor(
    _env: EnvUnowned,
    _class: JClass,
    xp: jint,
) -> jint {
    rank_index(xp) as jint
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_xpProgressToNext(
    _env: EnvUnowned,
    _class: JClass,
    xp: jint,
) -> jint {
    xp_progress(xp).to_next
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_xpProgressPct(
    _env: EnvUnowned,
    _class: JClass,
    xp: jint,
) -> jint {
    xp_progress(xp).pct
}

// ── Streak (D-073) ─────────────────────────────────────────────────────────

/// lastEpochDay = -1 → heç vaxt gəlməyib (None).
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_streakOnLogin(
    _env: EnvUnowned,
    _class: JClass,
    currentStreak: jint,
    lastEpochDay: jlong,
    todayEpochDay: jlong,
) -> jint {
    let last = if lastEpochDay < 0 {
        None
    } else {
        Some(lastEpochDay)
    };
    daily_login(currentStreak, last, todayEpochDay).streak
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_streakCounted(
    _env: EnvUnowned,
    _class: JClass,
    currentStreak: jint,
    lastEpochDay: jlong,
    todayEpochDay: jlong,
) -> jboolean {
    let last = if lastEpochDay < 0 {
        None
    } else {
        Some(lastEpochDay)
    };
    daily_login(currentStreak, last, todayEpochDay).counted
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_streakBonusDue(
    _env: EnvUnowned,
    _class: JClass,
    currentStreak: jint,
    lastEpochDay: jlong,
    todayEpochDay: jlong,
) -> jboolean {
    let last = if lastEpochDay < 0 {
        None
    } else {
        Some(lastEpochDay)
    };
    daily_login(currentStreak, last, todayEpochDay).bonus_due
}

// ── L.1 proWeight ──────────────────────────────────────────────────────────

/// level: 0=none 1=Başlanğıc 2=Orta 3=Peşəkar → 0..3 çəki.
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_proWeight(
    _env: EnvUnowned,
    _class: JClass,
    level: jint,
) -> jint {
    let lv = match level {
        1 => EventLevel::Beginner,
        2 => EventLevel::Intermediate,
        3 => EventLevel::Professional,
        _ => EventLevel::None,
    };
    qoshul_core::rules::pro_weight(lv)
}

// ── Sükut saatı / match cap ────────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_isQuietHour(
    _env: EnvUnowned,
    _class: JClass,
    hour: jint,
) -> jboolean {
    is_quiet_hour(hour.unsigned_abs() % 24)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_matchXpAllowed(
    _env: EnvUnowned,
    _class: JClass,
    awardedToday: jint,
) -> jboolean {
    match_xp_allowed(awardedToday)
}

// ── Geo (B1) ───────────────────────────────────────────────────────────────

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_haversineMeters(
    _env: EnvUnowned,
    _class: JClass,
    lat1: jdouble,
    lng1: jdouble,
    lat2: jdouble,
    lng2: jdouble,
) -> jdouble {
    haversine_meters(lat1, lng1, lat2, lng2)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_checkInWithinRange(
    _env: EnvUnowned,
    _class: JClass,
    lat1: jdouble,
    lng1: jdouble,
    lat2: jdouble,
    lng2: jdouble,
) -> jboolean {
    within_checkin_range(lat1, lng1, lat2, lng2)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_checkInMaxDistanceM(
    _env: EnvUnowned,
    _class: JClass,
) -> jdouble {
    CHECK_IN_MAX_DISTANCE_M
}

// ── XP 2: preview/spend ────────────────────────────────────────────────────

/// xpAmount XP → AZN endirimi (xp_per_azn məzənnəsi); konfiq 0 → NaN.
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_previewDiscount(
    _env: EnvUnowned,
    _class: JClass,
    xpAmount: jint,
    xpPerAzn: jint,
) -> jdouble {
    if xpPerAzn <= 0 {
        return f64::NAN;
    }
    xpAmount as f64 / xpPerAzn as f64
}

/// C1/D-072: cap yoxlaması (REDD — clamp yox).
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_spendCapOk(
    _env: EnvUnowned,
    _class: JClass,
    discount: jdouble,
    bill: jdouble,
    maxPercent: jint,
) -> jboolean {
    let cap = bill * maxPercent as f64 / 100.0;
    discount <= cap + 0.001
}

/// B6: atomik xərcləmə — uğurda yeni balans; xətada -1 (INSUFFICIENT),
/// -2 (INVALID ≤0). Balans mənfi ola bilməz.
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_venueSpend(
    _env: EnvUnowned,
    _class: JClass,
    balance: jint,
    amount: jint,
) -> jint {
    if amount <= 0 {
        return -2;
    }
    if amount > balance {
        return -1;
    }
    balance - amount
}

/// A1 typed xəta kodu — uğur -1; xəta index-i:
/// 0 NO_ACCOUNT, 1 INVALID_INPUT, 2 BAD_CONFIG, 3 INSUFFICIENT_BALANCE,
/// 4 BILL_BELOW_MIN, 5 CAP_EXCEEDED.
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_previewSpendCode(
    _env: EnvUnowned,
    _class: JClass,
    balance: jint,
    xpPerAzn: jint,
    minBill: jint,
    maxPct: jint,
    xpAmount: jint,
    bill: jdouble,
) -> jint {
    let cfg = VenueConfig {
        xp_per_azn: xpPerAzn,
        max_discount_percent: maxPct,
        min_bill_azn: minBill,
    };
    match preview_spend(balance, &cfg, xpAmount, bill) {
        Ok(_) => -1,
        Err(e) => match e {
            SpendError::NoAccount => 0,
            SpendError::InvalidInput => 1,
            SpendError::BadConfig => 2,
            SpendError::InsufficientBalance => 3,
            SpendError::BillBelowMin => 4,
            SpendError::CapExceeded => 5,
        },
    }
}

// ── QR (M3, D-081): qrcodegen 1.8.0 — Nayuki Java-nın Rust portu ──────────

/// QrGeometry.of pariteti: data[0] = size (21..177), data[1..1+size²] = 0/1.
/// Xəta yoxdur (geometry_for_ui heç vaxt iflas etmir); siqnal — RuntimeException.
#[unsafe(no_mangle)]
pub extern "system" fn Java_az_qoshul_app_core_rust_QoshulNative_qrEncodeUi<'caller>(
    mut env: jni::EnvUnowned<'caller>,
    _class: JClass<'caller>,
    payload: jni::objects::JString<'caller>,
) -> JIntArray<'caller> {
    env.with_env(|e| -> Result<JIntArray<'_>, jni::errors::Error> {
        let text: String = payload.try_to_string(e)?;
        let m = qoshul_core::qr::geometry_for_ui(&text);
        let mut data: Vec<i32> = Vec::with_capacity(1 + m.dark.len());
        data.push(m.size as i32);
        data.extend(m.dark.iter().map(|&b| i32::from(b)));
        let arr = e.new_int_array(data.len())?;
        arr.set_region(e, 0, &data)?;
        Ok(arr)
    })
    .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}
