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
import androidx.compose.ui.draw.shadow
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

/**
 * D-060: "0.1x liquid glass" (istifadəçi direktivi 2026-11-01: "bottom
 * navigation bar boyları normaldır amma şəffaflıq səhv deyiləcək qədər
 * çoxdur. 0.1x liquid glass efekti də əlavə et ona").
 *
 * Şəffaflıq geri çəkildi: fon indi demək olar OPAX ağdır — [intensity]=0.1
 * üçün alfa 0.962→0.938 (əvvəl 0.62→0.38 idi — məzmun barın içindən
 * görünürdü). Şüşə dili yalnız 10% gücdə yaşayır: zərif diaqonal işıq
 * qradiyenti + spekulyar kənar (0.55 → 0.22 alfa) + yumşaq kölgə.
 *
 * [intensity] 0..1 aralığında "şüşələnmə" dərəcəsi: 0 = tam solid ağ
 * həb, 0.1 = alt nav rejimi (bu direktiv), 1.0 = köhnə GlassSurface-ə
 * bərabər şəffaflıq. Ölçülər (hündürlük/padding/ikon) DİYİŞDİRİLMİR —
 * direktivdə "boyları normaldır".
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    intensity: Float = 0.1f,
    content: @Composable () -> Unit,
) {
    val g = intensity.coerceIn(0f, 1f)
    // Solid ağ (1.0) → köhnə şüşə (0.62/0.38) arası xətti qarışım.
    val baseHi = 1f - 0.38f * g   // 0.1 → 0.962
    val baseLo = 1f - 0.62f * g   // 0.1 → 0.938
    val rim = 0.18f + 0.37f * g   // 0.1 → 0.217 (spekulyar kənar)
    Box(
        modifier = modifier
            .shadow(
                elevation = (8.dp + 16.dp * g), // 0.1 → 9.6dp yumşaq kölgə
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.14f),
            )
            .background(
                Brush.linearGradient(
                    start = Offset.Zero,
                    end = Offset.Infinite,
                    colors = listOf(
                        Color.White.copy(alpha = baseHi),
                        Color.White.copy(alpha = baseLo),
                    ),
                ),
                shape,
            )
            .border(1.dp, Color.White.copy(alpha = rim), shape)
    ) { content() }
}
