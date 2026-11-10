package az.qoshul.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Qoshul Material3 teması — Flutter buildQoshulTheme() portu.
 *
 * D-118 (v0.42.0): rəng sxemi ARTIQ daimi val deyil — hər kompozisiyada Q
 * tokenlərindən yenidən qurulur (Q.apply dəyişəndə MaterialTheme də
 * recompose olur). D-119 (direktiv 2026-11-10: „ağ (default) və qara“):
 * qara modda darkColorScheme + sistem barlarının görünüşü TÜND olur (ağ
 * saat/ikonlar); ağ modda dilək D-104-dəki kimi — tünd ikonlar.
 */
@Composable
private fun qoshulColors() = if (Q.isDark) {
    darkColorScheme(
        primary = Q.primary,
        onPrimary = Q.primaryFg,
        secondary = Q.secondary,
        // D-123: onSecondary = secondary konteynerindəki MƏTN — primaryText
        // (mod-adaptiv oxunaqlı brend tonu), primary dolğusu yox.
        onSecondary = Q.primaryText,
        surface = Q.card,
        onSurface = Q.foreground,
        surfaceContainerHighest = Q.muted,
        onSurfaceVariant = Q.mutedFg,
        error = Q.destructive,
        onError = Q.destructiveFg,
        outline = Q.border,
        outlineVariant = Q.border,
        background = Q.background,
        onBackground = Q.foreground,
    )
} else {
    lightColorScheme(
        primary = Q.primary,
        onPrimary = Q.primaryFg,
        secondary = Q.secondary,
        // D-123: onSecondary = secondary konteynerindəki MƏTN — primaryText
        // (mod-adaptiv oxunaqlı brend tonu), primary dolğusu yox.
        onSecondary = Q.primaryText,
        surface = Q.card,
        onSurface = Q.foreground,
        surfaceContainerHighest = Q.muted,
        onSurfaceVariant = Q.mutedFg,
        error = Q.destructive,
        onError = Q.primaryFg,
        outline = Q.border,
        outlineVariant = Q.border,
        background = Q.background,
        onBackground = Q.foreground,
    )
}

@Composable
fun QoshulTheme(content: @Composable () -> Unit) {
    // D-104 (2026-11-09) + D-119: sistem barlarının görünüşü MODA görədir —
    // ağ modda tünd status/nav ikonları (saat, wifi, batareya açıq fonda
    // oxunur, D-104), qara modda AĞ ikonlar (tünd fonda). D-118-dən sonra
    // görünüş Q.background luminalısından oxunur — dizayn seçimi dəyişəndə
    // hər recompose-da yenidən təsdiqlənir (konfiqurasiya/uiMode dəyişikliyi
    // hallarında da). MainActivity-dəki enableEdgeToEdge çağırışının
    // kompozisiya səviyyəsində təkrarı.
    val lightBars = Q.background.luminance() > 0.5f
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
    }
    // D-118: LocalContentColor TƏMİN OLUNUR — rəngsiz T.* stilləri (hero/h1/
    // h2/titleLg/body…) miras alır. MaterialTheme bunu özü vermir (Surface
    // verir); işıq modda default-qara təsadüfi düzgün idi, QARA MODDA isə
    // görünməz olurdu („Ayarlar“ başlığı, bölmə başlıqları — D-121 audit).
    // Material3 Surface-lər (dialoqlar) daxildən öz LocalContentColor-unu
    // qoyur — bizim təminat yalnız birbaşa mətnlərə çatır.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides Q.foreground,
    ) {
        MaterialTheme(
            colorScheme = qoshulColors(),
            typography = qoshulTypography(),
            content = content,
        )
    }
}
