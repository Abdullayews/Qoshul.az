package az.qoshul.app

import android.os.Build
import androidx.compose.ui.graphics.toArgb

import az.qoshul.app.core.glass.NavGlassRecipe
import az.qoshul.app.core.glass.isLiquidGlassAvailable
import az.qoshul.app.core.glass.navFallbackPalette
import az.qoshul.app.core.glass.navGlassPalette
import az.qoshul.app.core.theme.Q
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * D-107 (v0.39.0): LIQUID GLASS NAV BAR SERTİFİKATI — Echo-Music recipesinin
 * (istifadəçi direktivi 2026-11-09: „olmursa echo music reposunun koduna bax
 * və yalnız liquid glass teması olan navigation bar stilini tətbiq et“)
 * parametr + gate müqaviləsi:
 *
 *  (1) GATE — Robolectric JVM-testdə glass BAĞLIDIR (Build.FINGERPRINT
 *      „robolectric“ — MapScreen isJvmTest qaydası ilə eyni): bütün UI
 *      testləri D-103 fallback görünüşündə deterministik qalır, AGSL
 *      RuntimeShader riski testlərə girmir; real cihazda (API 31+) isə
 *      glass açılır — o yalnız cihazda vizual olaraq doğrulanır.
 *  (2) RESEPT — parametrlər Echo-Music „Apple-matched“ dəyərlərinə bərabərdir
 *      (blur 8dp, saturasiya ×1.5, lens 24/24dp, ton #FAFAFA @ 0.4,
 *      kapsul ağ @ 0.25, qeyri-seçili 65% alfa).
 *  (3) PALET — glass: monoxrom adaptiv (seçili = Q.foreground, qeyri-seçili
 *      65% alfa, kapsul ağ@0.25); fallback: D-103 (primary/mutedFg/secondary).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiquidGlassNavTest {

    @Test
    fun `gate robolectricde bagli real cihaz aciq menededir`() {
        // JVM test mühiti — FINGERPRINT „robolectric“ ehtiva edir → bağlı
        assertFalse(
            "Robolectric-də glass bağlı olmalıdır (determinizm)",
            isLiquidGlassAvailable(),
        )
        // qaydanın özü: API 31+ və FINGERPRINT şərti
        assertFalse(
            "qayda FINGERPRINT-i yoxlayır",
            Build.FINGERPRINT.contains("robolectric", ignoreCase = true) &&
                isLiquidGlassAvailable(),
        )
    }

    @Test
    fun `resept echo music apple matched deyerleridir`() {
        assertEquals("fon bluru 8dp (Echo default)", 8f, NavGlassRecipe.BLUR_DP)
        assertEquals("saturasiya ×1.5 (vibrancy)", 1.5f, NavGlassRecipe.SATURATION)
        assertEquals("lens 24dp hündürlük (0.5×48)", 24f, NavGlassRecipe.LENS_DP)
        assertEquals("səth tonu alfası 0.4", 0.4f, NavGlassRecipe.SURFACE_OPACITY)
        assertEquals("seçili kapsul ağ @ 0.25", 0.25f, NavGlassRecipe.INDICATOR_ALPHA)
        assertEquals("qeyri-seçili 65% alfa", 0.65f, NavGlassRecipe.UNSELECTED_ALPHA)
        // işıq tema tonu #FAFAFA (Echo adaptive tint), tünd #121212
        assertEquals(
            "səth tonu (#FAFAFA — Echo işıq dəyəri)",
            android.graphics.Color.parseColor("#FAFAFA"),
            NavGlassRecipe.SURFACE_TINT_LIGHT.toArgb(),
        )
        assertEquals(
            "tünd ton (#121212 — gələcək tünd tema üçün)",
            android.graphics.Color.parseColor("#121212"),
            NavGlassRecipe.SURFACE_TINT_DARK.toArgb(),
        )
    }

    @Test
    fun `glass paleti monoxromdur fallback ise d103 dilidir`() {
        val glass = navGlassPalette()
        // işıq temada (D-023) seçili = tam tünd mətn rəngi (Echo monoxromu)
        assertEquals(Q.foreground, glass.selected)
        assertEquals(
            "qeyri-seçili = seçilinin 65%-i",
            Q.foreground.copy(alpha = 0.65f),
            glass.unselected,
        )
        assertEquals(
            "seçili kapsul ağ @ 0.25 (Echo dili)",
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f),
            glass.indicator,
        )

        val fallback = navFallbackPalette()
        // D-123: fallback aktiv ton = primaryText (səth üzərində brend MƏTN
        // dili — qızılı+ağ modda #EAC66C 1.6:1 oxunmaz idi; hər modda ≥4.5:1)
        assertEquals("fallback aktiv = D-103/D-123 primaryText", Q.primaryText, fallback.selected)
        assertEquals("fallback qeyri-aktiv = D-103 mutedFg", Q.mutedFg, fallback.unselected)
        assertEquals("fallback kapsul = D-103 secondary", Q.secondary, fallback.indicator)
    }
}
