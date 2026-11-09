package az.qoshul.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Qoshul Material3 teması — Flutter buildQoshulTheme() portu.
 * Yalnız İŞIQ rejim (web-parity, D-023; qaranlıq tema məhsulda yoxdur).
 */
private val QoshulColors = lightColorScheme(
    primary = Q.primary,
    onPrimary = Q.primaryFg,
    secondary = Q.secondary,
    onSecondary = Q.primary,
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

@Composable
fun QoshulTheme(content: @Composable () -> Unit) {
    // D-104 (2026-11-09): tema yalnız İŞIQDIR → sistem barlarının görünüşü
    // də həmişə İŞIQ olmalıdır (tünd status/nav ikonları — saat, wifi,
    // batareya açıq #F8FAFC fonda oxunur). MainActivity-dəki
    // SystemBarStyle.light çağırışının kompozisiya səviyyəsində təkrarı:
    // sistem qaranlıq rejimi İŞLƏK app ikən dəyişsə belə (uiMode
    // konfiqurasiyası) görünüş hər recompose-da yenidən təsdiqlənir.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = QoshulColors,
        typography = qoshulTypography(),
        content = content,
    )
}
