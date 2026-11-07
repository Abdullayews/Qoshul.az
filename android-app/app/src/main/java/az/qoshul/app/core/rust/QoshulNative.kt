package az.qoshul.app.core.rust

/**
 * M2 (D-080): Rust nüvəsi JNI körpüsü.
 *
 * Yükləmə ardıcıllığı:
 *  1. `-Dqoshul.jni.path=<dir>` → `System.load("<dir>/libqoshul_jni.so")`
 *     (JVM testləri/CI host build-i);
 *  2. `System.loadLibrary("qoshul_jni")` (Android: CI cargo-ndk jniLibs-ə
 *     qoyur — arm64-v8a + armeabi-v7a).
 *
 * Yüklənməyəndə [available] = false → paritet testləri skip edir, tətbiq
 * Kotlin (XpRules) fallback-da işləyir — davranış EYNİ qaydalar üzərindən
 * qurulub (paritet: Rust proptest + RustParityTest).
 *
 * Səth qəsdən NUMERİKDIR (A1 xətaları index ilə) — string JNI yoxdur.
 */
object QoshulNative {

    @Volatile
    var available: Boolean = false
        private set

    init {
        available = runCatching {
            val dir = System.getProperty("qoshul.jni.path")
            if (dir != null) {
                System.load("$dir/libqoshul_jni.so")
            } else {
                System.loadLibrary("qoshul_jni")
            }
        }.isSuccess
    }

    // — ROL F: səviyyə/progress (A4 invariantı Rust proptest ilə sübut) —

    external fun rankIndexFor(xp: Int): Int
    external fun xpProgressToNext(xp: Int): Int
    external fun xpProgressPct(xp: Int): Int

    // — D-073: streak (lastEpochDay = -1 → heç vaxt) —

    external fun streakOnLogin(currentStreak: Int, lastEpochDay: Long, todayEpochDay: Long): Int
    external fun streakCounted(currentStreak: Int, lastEpochDay: Long, todayEpochDay: Long): Boolean
    external fun streakBonusDue(currentStreak: Int, lastEpochDay: Long, todayEpochDay: Long): Boolean

    // — L.1 proWeight: level 0=none 1=Başlanğıc 2=Orta 3=Peşəkar —

    external fun proWeight(level: Int): Int

    // — ROL H / D-074 —

    external fun isQuietHour(hour: Int): Boolean
    external fun matchXpAllowed(awardedToday: Int): Boolean

    // — B1: geo (haversine, metr) —

    external fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double
    external fun checkInWithinRange(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Boolean
    external fun checkInMaxDistanceM(): Double

    // — L.4: XP 2 (B6 atomik; C1 REDD) —

    external fun previewDiscount(xpAmount: Int, xpPerAzn: Int): Double
    external fun spendCapOk(discount: Double, bill: Double, maxPercent: Int): Boolean
    external fun venueSpend(balance: Int, amount: Int): Int   // -1 INSUFFICIENT, -2 INVALID
    external fun previewSpendCode(
        balance: Int, xpPerAzn: Int, minBill: Int, maxPct: Int,
        xpAmount: Int, bill: Double,
    ): Int  // -1 OK; 0..5 = A1 SpendError index-ləri
}
