package az.qoshul.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    MaterialTheme(
        colorScheme = QoshulColors,
        typography = qoshulTypography(),
        content = content,
    )
}
