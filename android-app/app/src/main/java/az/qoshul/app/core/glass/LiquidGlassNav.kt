package az.qoshul.app.core.glass

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.theme.Q
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * D-107 — ƏSL liquid glass naviqasiya dili (istifadəçi direktivi 2026-11-09:
 * „olmursa echo music reposunun koduna bax və yalnız liquid glass teması
 * olan navigation bar stilini tətbiq et. indiki versiyası heç yaxşı deyil“).
 *
 * Echo-Music-in (EchoMusicApp/Echo-Music) liquid glass naviqasiya barı eyni
 * vendored FloatingTabBar üzərindədir; fərq — şüşə təbəqəsidir:
 * backgroundColor = şəffaf + tabBarContentModifier = glass. Texniki daşıyıcı:
 * Kyant0/backdrop 2.0.1 (Apache-2.0, io.github.kyant0:backdrop — „Compose
 * Multiplatform Liquid Glass effects“; RenderEffect API 31+, lens
 * refraksiyası RuntimeShader API 33+ — kitabxananın öz qeydləri alt tierdə
 * no-op). D-037-nin „ucuz şüşə“ (ağ qradiyent + kənar) portu alt bar üçün
 * ləğv olunur — o, yalnız LiquidRail (D-102) və xəritə səthlərində qalır.
 *
 * Recipesi (Echo-Music „Apple-matched“ parametrləri, Glasses of iOS 26 dili):
 *  - vibrancy: saturasiya ×1.5 (rənglər şüşədən canlı keçir);
 *  - blur 8dp (fon yumşaq smudge — Echo şəkillərində görünən güclü mat);
 *  - lens 24dp/24dp + xromatik aberasiya + dərinlik (kənar brişinqi);
 *  - səth tonu: işıq temada #FAFAFA @ 0.4 alfa (tünd temada #121212 —
 *    Qoshul yalnız işıq temadır, D-023, amma kod adaptivdir);
 *  - spekulyar kənar (Highlight.Default 0.5dp) + yumşaq kölgə
 *    (Shadow.Default 24dp/10%) — pill „üstə üzmə“ hissi;
 *  - məzmun monoxromdur: seçili = tam tünd mətn, qeyri-seçili = 65% alfa,
 *    seçili kapsul = ağ @ 0.25 (Echo dili — rəng aksenti YOX).
 *
 * Gate: RenderEffect API 31-dən aşağı və Robolectric (JVM test — AGSL
 * RuntimeShader riski; MapScreen isJvmTest qaydası ilə eyni) → modifier
 * dəyişməz qaytarılır, AppShell fallback solid görünüşə düşür (D-103 rəng
 * dili — köhnə cihazlarda reqressiya yoxdur).
 */
object NavGlassRecipe {
    /** Fon bluru (dp) — Echo „Apple-matched“ defaultu. */
    const val BLUR_DP = 8f

    /** Vibrancy — saturasiya multplikatoru (1f = toxunma; Echo default 1f → ×1.5). */
    const val SATURATION = 1.5f

    /** Linz refraksiyası (dp) — həm hündürlük, həm miqdar (Echo 0.5×48). */
    const val LENS_DP = 24f

    /** Səth tonunun alfası (0 = tam şəffaf, 1 = opaq). */
    const val SURFACE_OPACITY = 0.4f

    /** Seçili slotun kapsul alfası (Echo: Color.White @ 0.25). */
    const val INDICATOR_ALPHA = 0.25f

    /** Qeyri-seçili ikon/mətn alfası (Echo: adaptiveTextColor @ 0.65). */
    const val UNSELECTED_ALPHA = 0.65f

    /** İşıq tema şüşə tonu (Echo adaptive tint). */
    val SURFACE_TINT_LIGHT = Color(0xFFFAFAFA)

    /** Tünd tema şüşə tonu (gələcəkdə tünd tema gəlsə — D-023 işıq-only). */
    val SURFACE_TINT_DARK = Color(0xFF121212)
}

/** App məzmununun geribesleme qatı — MainActivity/AppShell-də təmin olunur. */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Şüşə boru xətti işləyəbiləndirmi? API 31+ (RenderEffect) və real cihaz
 * (Robolectric JVM test deyil — MapScreen-in isJvmTest qaydası ilə paralel).
 * API 33-dən aşağıda lens refraksiyası kitabxana tərəfindən no-op olur
 * (isRuntimeShaderSupported gate-i) — blur+vibrancy+kənar qalır.
 */
fun isLiquidGlassAvailable(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !Build.FINGERPRINT.contains("robolectric", ignoreCase = true)

/** Echo recipesi ilə şüşə səthi — [LocalAppBackdrop] boşdursa/gate bağlıdırsa dəyişməz. */
@Composable
fun Modifier.liquidGlassNavBar(shape: CornerBasedShape): Modifier {
    val backdrop = LocalAppBackdrop.current ?: return this
    if (!isLiquidGlassAvailable()) return this
    val density = LocalDensity.current
    val blurPx = with(density) { NavGlassRecipe.BLUR_DP.dp.toPx() }
    val lensPx = with(density) { NavGlassRecipe.LENS_DP.dp.toPx() }
    // Apple dili: işıq məzmun üstündə işıq şüşə, tünd üstündə tünd
    // (Q.background D-023 üzrə işıqdır → #FAFAFA @ 0.4).
    val tint =
        if (Q.background.luminance() > 0.5f) NavGlassRecipe.SURFACE_TINT_LIGHT
        else NavGlassRecipe.SURFACE_TINT_DARK
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = NavGlassRecipe.SATURATION)
            blur(blurPx)
            lens(
                refractionHeight = lensPx,
                refractionAmount = lensPx,
                depthEffect = true,
                chromaticAberration = true,
            )
        },
        highlight = { Highlight.Default },
        shadow = { Shadow.Default },
        onDrawSurface = {
            drawRect(
                color = tint.copy(alpha = NavGlassRecipe.SURFACE_OPACITY),
                size = size,
            )
        },
    )
}

/**
 * Şüşə üzərindəki naviqasiya məzmununun rəng dili (Echo monoxromu):
 * seçili = tam əsas mətn rəngi (W700 çəki ilə), qeyri-seçili = 65% alfa,
 * seçili kapsul = ağ @ 0.25. Fallback (glass bağlıdırsa) — D-103 dili.
 */
data class NavPalette(
    val selected: Color,
    val unselected: Color,
    val indicator: Color,
)

/** Glass AÇIQ üçün adaptiv monoxrom palet. */
fun navGlassPalette(): NavPalette {
    val base =
        if (Q.background.luminance() > 0.5f) Q.foreground else Color(0xFFF5F5F5)
    return NavPalette(
        selected = base,
        unselected = base.copy(alpha = NavGlassRecipe.UNSELECTED_ALPHA),
        indicator = Color.White.copy(alpha = NavGlassRecipe.INDICATOR_ALPHA),
    )
}

/** Glass BAĞLI fallback (D-103: aktiv primary, qeyri-aktiv mutedFg, kapsul secondary).
 *  D-123: aktiv ton primaryText — səth üzərində brend MƏTN/İKON dili
 *  (qızılı+ağ modda #EAC66C 1.6:1 oxunmaz idi; primaryText hər modda ≥4.5:1). */
fun navFallbackPalette(): NavPalette =
    NavPalette(selected = Q.primaryText, unselected = Q.mutedFg, indicator = Q.secondary)
