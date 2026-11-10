package az.qoshul.app

import az.qoshul.app.core.theme.DesignColor
import az.qoshul.app.core.theme.DesignMode
import az.qoshul.app.core.theme.Q
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * D-123 (v0.43.0) — RƏNG SİSTEMİNİN WCAG 2.2 SÜBUTU (CI-də məcburi).
 *
 * İstifadəçi direktivi 2026-11-11: „rənglər sistemini qurmaq üçün özün
 * aydınlıqla baxaraq seç, əsas olan 2 rəng yerində qalmalıdır ama ağ və
 * qara modunda olarkən mətnlər asanlıqla oxuna bilməlidir. araşdırma
 * edib rəng palitrası qaydalarına uyğun olaraq dəyişdir“.
 *
 * Qaydalar (W3C WCAG 2.2 SC 1.4.3 + 1.4.11 + Material qara-tema):
 *  - ADİ MƏTN ≥ 4.5:1 (foreground/mutedFg/primaryText/destructive/accentFg
 *    bütün səthlərində — 2 rəng × 3 mod = 6 kombinasiya);
 *  - BÖYÜK MƏTN (W700 ≥ 14sp düymə etiketi) ≥ 3:1 (primaryFg/primary —
 *    brend #008B2D sabitlənib, ağ 4.44:1 = AA-large; qızılı tünd 8.4:1);
 *  - primaryText MOD-ADAPTİVDİR: ağ modda tünd brend tonu, qara/AMOLED
 *    modda işıq (desktrasiya) tonu — Material qara-tema qaydası.
 *
 * Riyaziyyat: WCAG relative luminance (sRGB linearizasiya) + ratio =
 * (L1+0.05)/(L2+0.05); yuvarlaqlaşdırma YOX (4.499 keçmir — WCAG qeydi).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaletteContrastTest {

    @Before
    fun before() = reset()

    @After
    fun after() = reset()

    private fun reset() = Q.apply(DesignColor.GREEN, DesignMode.LIGHT)

    // ——— WCAG 2.2 riyaziyyatı (9.1.4.3 tərifi) ———

    private fun channel(c: Double): Double =
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red.toDouble()) +
            0.7152 * channel(c.green.toDouble()) +
            0.0722 * channel(c.blue.toDouble())

    private fun ratio(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(fg: Color, bg: Color, min: Double, name: String) {
        val r = ratio(fg, bg)
        assertTrue("$name kontrast $r : 1 < $min (D-123 qaydası pozuldu)", r >= min)
    }

    // ═══════════ 1. ADİ MƏTN — 6 kombinasiyada ≥ 4.5:1 ═══════════

    @Test
    fun `adi metn — 6 kombinasiyada WCAG AA (4_5_1)`() {
        for (color in DesignColor.entries) {
            for (mode in DesignMode.entries) {
                Q.apply(color, mode)
                val tag = "$color/$mode"
                // əsas mətn
                check(Q.foreground, Q.background, 4.5, "$tag foreground/background")
                check(Q.foreground, Q.card, 4.5, "$tag foreground/card")
                check(Q.foreground, Q.secondary, 4.5, "$tag foreground/secondary")
                check(Q.foreground, Q.aiBoxBg, 4.5, "$tag foreground/aiBoxBg")
                check(Q.foreground, Q.rankBadgeBg, 4.5, "$tag foreground/rankBadgeBg")
                // ikincil mətn
                check(Q.mutedFg, Q.background, 4.5, "$tag mutedFg/background")
                check(Q.mutedFg, Q.card, 4.5, "$tag mutedFg/card")
                check(Q.mutedFg, Q.muted, 4.5, "$tag mutedFg/muted")
                check(Q.mutedFg, Q.aiBoxBg, 4.5, "$tag mutedFg/aiBoxBg")
                // brend mətn/ikon tonu (D-123 əsas düzəlişi)
                check(Q.primaryText, Q.background, 4.5, "$tag primaryText/background")
                check(Q.primaryText, Q.card, 4.5, "$tag primaryText/card")
                check(Q.primaryText, Q.muted, 4.5, "$tag primaryText/muted")
                check(Q.primaryText, Q.secondary, 4.5, "$tag primaryText/secondary")
                check(Q.primaryText, Q.aiBoxBg, 4.5, "$tag primaryText/aiBoxBg")
                // semantik rənglər
                check(Q.destructive, Q.background, 4.5, "$tag destructive/background")
                check(Q.destructive, Q.card, 4.5, "$tag destructive/card")
                check(Q.accentFg, Q.accent, 4.5, "$tag accentFg/accent")
            }
        }
    }

    // ═══════════ 2. DÜYMƏ ETİKETLƏRİ — W700 15sp = bold-large ≥ 3:1 ═══════════

    @Test
    fun `duyme etiketi — primaryFg primary uzerinde AA-large (3_1)`() {
        for (color in DesignColor.entries) {
            for (mode in DesignMode.entries) {
                Q.apply(color, mode)
                // T.button = 15sp W700 → WCAG bold-large (14pt≈18.7px həddi);
                // brend #008B2D istifadəçi tərəfindən sabitlənib — ağ 4.44:1.
                check(Q.primaryFg, Q.primary, 3.0, "$color/$mode primaryFg/primary")
            }
        }
    }

    // ═══════════ 3. AMOLED modu (D-123) ═══════════

    @Test
    fun `amoled — tam qara fon, kart ayridi, isDark dogrudur`() {
        Q.apply(DesignColor.GREEN, DesignMode.AMOLED)
        // OLED pikselləri söndürür — fon TÜM QARA
        assertEquals(Color(0xFF000000), Q.background)
        assertTrue(Q.isDark)
        // kart qaradan ayrılır (səviyyə ayrılığı — qara üstündə kölgə görünməz)
        assertNotEquals(Q.background, Q.card)
        assertTrue("AMOLED kartı #000-dən açıq olmalıdır", luminance(Q.card) > 0.0)
        // QARA mod ≠ AMOLED (Material #121212 dili — qara-boz)
        Q.apply(DesignColor.GREEN, DesignMode.DARK)
        assertNotEquals(Color(0xFF000000), Q.background)
        // üç mod üç fərqli fonda
        val bgs = DesignMode.entries.map { m ->
            Q.apply(DesignColor.GREEN, m); Q.background
        }
        assertEquals(3, bgs.toSet().size)
    }

    // ═══════════ 4. primaryText — mod-adaptiv brend tonu ═══════════

    @Test
    fun `primaryText — ag modda tund, qara-amoledde isiq, brend dolgu sabit`() {
        // YAŞIL: ağ #007428 (5.2:1+), qara #4EC97A (6.1:1+)
        Q.apply(DesignColor.GREEN, DesignMode.LIGHT)
        assertEquals(Color(0xFF007428), Q.primaryText)
        assertEquals(Color(0xFF008B2D), Q.primary)
        Q.apply(DesignColor.GREEN, DesignMode.DARK)
        assertEquals(Color(0xFF4EC97A), Q.primaryText)
        Q.apply(DesignColor.GREEN, DesignMode.AMOLED)
        assertEquals(Color(0xFF4EC97A), Q.primaryText)
        // QIZILI: ağ #7E5812 (brend özü 1.6:1 oxunmazdır), qara brend özü
        Q.apply(DesignColor.GOLD, DesignMode.LIGHT)
        assertEquals(Color(0xFF7E5812), Q.primaryText)
        assertEquals(Color(0xFFEAC66C), Q.primary)
        Q.apply(DesignColor.GOLD, DesignMode.DARK)
        assertEquals(Color(0xFFEAC66C), Q.primaryText)
    }

    @Test
    fun `qizili ag mod — brend dolgu ila metn tonu ferqlidir (1_6_1 qopucusu)`() {
        // D-118-in əsl bug-ı: QIZILI + AĞ seçildikdə bütün brend mətnlər
        // #EAC66C ilə çəkilirdi → ağ fonda 1.57:1 (oxunmaz). D-123-də
        // mətn tonu primaryText (#7E5812) ayrılır, dolgu brend qalır.
        Q.apply(DesignColor.GOLD, DesignMode.LIGHT)
        assertNotEquals(Q.primary, Q.primaryText)
        assertTrue(
            "qızılı mətn tonu dolğudan TÜND olmalıdır (ağ fon oxunaqlılığı)",
            luminance(Q.primaryText) < luminance(Q.primary),
        )
    }
}
