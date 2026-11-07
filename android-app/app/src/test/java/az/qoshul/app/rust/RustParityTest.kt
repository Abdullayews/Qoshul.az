package az.qoshul.app.rust

import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.rust.QoshulNative
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * M2 (D-080): Rust ↔ Kotlin PARİTET testləri.
 *
 * Şərait: host `libqoshul_jni.so` mövcuddursa (`-Dqoshul.jni.path`, CI və
 * lokal `cargo build --release`) hər funksiya EYNİ girşlərdə eyni nəticə
 * verməlidir. .so yoxdursa testlər skip olunur (Kotlin fallback tək başına
 * 165 test ilə örtülüdür) — CI hər push-da .so qurur, ona görə orada
 * məcburidir.
 */
class RustParityTest {

    private val native = QoshulNative

    // ── A4: xpProgress — 0..3100 TAM pariteti ─────────────────────────────

    @Test
    fun `xpProgress tam paritet 0-3100`() {
        assumeTrue(native.available)
        for (xp in 0..3100) {
            val kt = XpRules.xpProgress(xp)
            assertEquals("toNext fərqli (xp=$xp)", kt.toNext, native.xpProgressToNext(xp))
            assertEquals("pct fərqli (xp=$xp)", kt.pct, native.xpProgressPct(xp))
            // A4 invariantı hər iki tərəfdə:
            assertEquals("pct==100 ⟺ toNext==0 (xp=$xp)", kt.pct == 100, kt.toNext == 0)
        }
    }

    @Test
    fun `rankIndex paritet sərhədlərdə`() {
        assumeTrue(native.available)
        for (xp in listOf(0, 99, 100, 299, 300, 699, 700, 1499, 1500, 2999, 3000, 99999)) {
            assertEquals("rank (xp=$xp)", XpRules.rankIndexForXp(xp), native.rankIndexFor(xp))
        }
    }

    // ── D-073: streak — tam matris (missed 0..8, streak 1..9) ─────────────

    /** Kotlin modeli — AppEngine.dailyLogin pariteti (test spesifikasiyası). */
    private fun ktStreak(streak: Int, last: Long?, today: Long): Triple<Int, Boolean, Boolean> {
        if (last != null && today <= last) return Triple(streak, false, false)
        val missed = if (last == null) 0L else today - last - 1
        var s = when {
            missed <= 0 -> streak + 1
            missed <= XpRules.STREAK_FREEZE_DAYS -> streak
            else -> 1
        }
        var bonus = false
        if (s == 7) { bonus = true; s = 1 }
        return Triple(s, true, bonus)
    }

    @Test
    fun `streak matris pariteti`() {
        assumeTrue(native.available)
        for (streak in 1..9) {
            for (last in listOf<Long?>(null, 10_000L)) {
                for (gap in 0..8) {
                    val today = 10_000L + gap
                    val lastArg = last ?: -1L
                    val (ktS, ktC, ktB) = ktStreak(streak, last, today)
                    assertEquals(
                        "streak (s=$streak last=$last gap=$gap)",
                        ktS, native.streakOnLogin(streak, lastArg, today),
                    )
                    assertEquals("counted (s=$streak last=$last gap=$gap)",
                        ktC, native.streakCounted(streak, lastArg, today))
                    assertEquals("bonus (s=$streak last=$last gap=$gap)",
                        ktB, native.streakBonusDue(streak, lastArg, today))
                }
            }
        }
    }

    // ── L.1 / ROL H / D-074 ────────────────────────────────────────────────

    @Test
    fun `proWeight paritet`() {
        assumeTrue(native.available)
        val levels = mapOf(
            "Peşəkar" to 3, "Orta" to 2, "Başlanğıc" to 1,
            "" to 0, "?" to 0,
        )
        for ((name, expected) in levels) {
            assertEquals(expected, native.proWeight(ordinal(name)))
        }
    }

    private fun ordinal(level: String?): Int = when (level) {
        "Başlanğıc" -> 1; "Orta" -> 2; "Peşəkar" -> 3; else -> 0
    }

    @Test
    fun `isQuietHour paritet 0-23`() {
        assumeTrue(native.available)
        for (h in 0..23) {
            assertEquals("hour=$h", isQuietHoursAt(h), native.isQuietHour(h))
        }
    }

    @Test
    fun `matchXpAllowed paritet 0-10`() {
        assumeTrue(native.available)
        for (n in 0..10) assertEquals("awarded=$n", n < 3, native.matchXpAllowed(n))
    }

    // ── B1: haversine — Kotlin düsturu (R ≈ 6 371 008.8 m) ────────────────

    private fun ktHaversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_008.8 // IUGG orta radius (geo crate HaversineMeasure GRS80 ilə uyğun)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    @Test
    fun `haversine paritet Bakı nöqtələrində`() {
        assumeTrue(native.available)
        val pairs = listOf(
            // Bakı mərkəzi → Padel Arena (FakeData koordinatları)
            Triple(40.4093, 49.8671, 40.4236 to 49.8236),
            Triple(40.4093, 49.8671, 40.3922 to 49.8522), // Coffee Lab
            Triple(40.4093, 49.8671, 40.3842 to 49.8641), // Xankəndi
            Triple(40.4093, 49.8671, 40.4093 to 49.8671), // özünə
        )
        for ((lat1, lng1, b) in pairs) {
            val kt = ktHaversine(lat1, lng1, b.first, b.second)
            val rs = native.haversineMeters(lat1, lng1, b.first, b.second)
            assertTrue("fərq > 1m: kt=$kt rs=$rs", kotlin.math.abs(kt - rs) < 1.0)
        }
        // radius sabiti
        assertEquals(200.0, native.checkInMaxDistanceM(), 1e-9)
    }

    @Test
    fun `checkInWithinRange paritet 200m sərhədi`() {
        assumeTrue(native.available)
        // ~100 m şimal (Bakı mərkəzdən): 0.0009° ≈ 100 m
        assertTrue(native.checkInWithinRange(40.4093, 49.8671, 40.4101, 49.8671))
        // ~1 km
        assertTrue(!native.checkInWithinRange(40.4093, 49.8671, 40.4183, 49.8671))
        // 200 m həddi (200.0 ± bir az)
        val nearEdge = 40.4111 // ~200 m
        val kt = ktHaversine(40.4093, 49.8671, nearEdge, 49.8671)
        assertEquals("sərhəd düzgün", kt <= 200.0, native.checkInWithinRange(40.4093, 49.8671, nearEdge, 49.8671))
    }

    // ── L.4: preview/spend (B6, C1) ────────────────────────────────────────

    @Test
    fun `venueSpend paritet — mənfi balans qadağan`() {
        assumeTrue(native.available)
        assertEquals(-2, native.venueSpend(100, 0))    // INVALID ≤ 0
        assertEquals(-2, native.venueSpend(100, -5))
        assertEquals(-1, native.venueSpend(100, 101))  // INSUFFICIENT
        assertEquals(30, native.venueSpend(100, 70))   // OK → 30
        assertEquals(0, native.venueSpend(70, 70))
        assertEquals(-1, native.venueSpend(0, 1))
    }

    @Test
    fun `previewSpendCode paritet — A1 typed xətalar`() {
        assumeTrue(native.available)
        // v5 config: xpPerAzn 20, max 25%, minBill 10
        assertEquals(-1, native.previewSpendCode(325, 20, 10, 25, 100, 40.0))  // OK
        assertEquals(3, native.previewSpendCode(50, 20, 10, 25, 100, 40.0))    // INSUFFICIENT
        assertEquals(4, native.previewSpendCode(325, 20, 10, 25, 100, 5.0))    // BILL_BELOW_MIN
        assertEquals(5, native.previewSpendCode(600, 20, 10, 25, 500, 40.0))   // CAP
        assertEquals(2, native.previewSpendCode(100, 0, 10, 25, 100, 40.0))    // BAD_CONFIG
        assertEquals(1, native.previewSpendCode(325, 20, 10, 25, 0, 40.0))     // INVALID
    }

    @Test
    fun `spendCapOk və previewDiscount paritet`() {
        assumeTrue(native.available)
        assertEquals(5.0, native.previewDiscount(100, 20), 1e-9)
        assertTrue(native.spendCapOk(5.0, 40.0, 25))       // cap 10 → 5 ≤ 10
        assertTrue(!native.spendCapOk(25.0, 40.0, 25))     // 25 > 10.001
    }
}

/** isQuietHours-in saat-only variantı (Domain: t.hour >= 23 || t.hour < 8). */
private fun isQuietHoursAt(h: Int): Boolean = h >= 23 || h < 8
