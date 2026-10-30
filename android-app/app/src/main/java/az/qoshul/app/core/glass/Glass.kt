package az.qoshul.app.core.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass — Flutter `core/widgets/glass.dart` portu (D-025 minimal).
 *
 * 4 prinsip (Apple iOS 26 HIG): lensing · işıq qatı (qradiyent) · spekulyar
 * kənar (ağ kontur) · funksional qat (yalnız naviqasiya/kontrol səthləri).
 * 5 səth: alt nav pill · xəritə filtr paneli · xəritə alt kartı · sayğac
 * həbləri · match fəaliyyət düymələri.
 *
 * D-037 (native qərarı): Flutter BackdropFilter Compose-da birbaşa ekvivalent
 * yoxdur (Modifier.blur YALNIZ öz məzmununu blur edir, arxanı yox).
 * Bu port Flutter-in `GlassQuality.blur=false` ucuz fallback vizual dili ilə
 * işləyir: yarı-şəffaf ağ + işıq qradiyenti + spekulyar kənar — bütün
 * cihazlarda deterministik, RenderEffect xərci yox. Geri qalan fərq
 * (əsl lensing) Phase 8+ qiymətləndirməsinə qalıb (Haze / RenderEffect öz
 * qatı) — docs/decisions.md D-037.
 */
object GlassQuality {
    /** Native portda həmişə "ucuz" vizual dil (yuxarıdakı qərar). */
    const val blur: Boolean = false
}

/**
 * Əsas şüşə səthi: yarı-şəffaf ağ + işıq qradiyenti (sol-üstdən sağ-alta) +
 * spekulyar kənar.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    sigma: Dp = 14.dp, // D-037: parametr saxlanılır (API pariteti), vizual təsirsiz
    /** Ağ işıq qatının gücü (0–1) — Apple "Clear/Tinted" rejimlərinin qarşılığı. */
    luminance: Float = 0.5f,
    content: @Composable () -> Unit,
) {
    val lo = (luminance - 0.14f).coerceAtLeast(0f)
    val hi = (luminance + 0.10f).coerceAtMost(1f)
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    start = Offset.Zero,
                    end = Offset.Infinite,
                    colors = listOf(Color.White.copy(alpha = hi), Color.White.copy(alpha = lo)),
                ),
                shape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
    ) { content() }
}

/** Şüşə həblər (çip/düymə) — xəritə filtr çipləri və match düymələri üçün. */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    horizontal: Dp = 12.dp,
    vertical: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(if (dense) 10.dp else 14.dp),
        sigma = 12.dp,
        luminance = if (dense) 0.34f else 0.4f,
    ) {
        Box(modifier = Modifier.padding(horizontal = horizontal, vertical = vertical)) {
            content()
        }
    }
}
