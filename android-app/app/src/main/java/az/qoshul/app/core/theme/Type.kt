package az.qoshul.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import az.qoshul.app.R

/**
 * T tipografiya tokenləri — Flutter `T` sinifinin portu (D-035 mobil şkalası).
 * Plus Jakarta Sans statik instansiyaları (OFL 1.1) res/font-da.
 * Yoxlanış: docs/audit-flutter-v0.7.0.md bölmə 3.2.
 */
val JakartaSans = FontFamily(
    Font(R.font.plusjakartasans_regular, FontWeight.W400),
    Font(R.font.plusjakartasans_medium, FontWeight.W500),
    Font(R.font.plusjakartasans_semibold, FontWeight.W600),
    Font(R.font.plusjakartasans_bold, FontWeight.W700),
    Font(R.font.plusjakartasans_extrabold, FontWeight.W800),
)

object T {
    val hero = TextStyle(fontFamily = JakartaSans, fontSize = 27.sp, fontWeight = FontWeight.W800, lineHeight = 35.sp)
    val h1 = TextStyle(fontFamily = JakartaSans, fontSize = 24.sp, fontWeight = FontWeight.W800, lineHeight = 32.sp)
    val h2 = TextStyle(fontFamily = JakartaSans, fontSize = 19.sp, fontWeight = FontWeight.W800, lineHeight = 27.sp)
    val h3 = TextStyle(fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W800, lineHeight = 20.sp)
    val eventTitle = TextStyle(fontFamily = JakartaSans, fontSize = 16.sp, fontWeight = FontWeight.W800, lineHeight = 24.sp)
    val personName = TextStyle(fontFamily = JakartaSans, fontSize = 15.sp, fontWeight = FontWeight.W800, lineHeight = 21.sp)
    val subtitle = TextStyle(fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W400, lineHeight = 22.sp, color = Q.mutedFg)
    val body = TextStyle(fontFamily = JakartaSans, fontSize = 13.5.sp, lineHeight = 23.sp)
    val bodySm = TextStyle(fontFamily = JakartaSans, fontSize = 12.5.sp, lineHeight = 20.sp)
    val meta = TextStyle(fontFamily = JakartaSans, fontSize = 12.sp, lineHeight = 18.sp, color = Q.mutedFg)
    val metaSm = TextStyle(fontFamily = JakartaSans, fontSize = 10.5.sp, lineHeight = 16.sp, color = Q.mutedFg)
    val category = TextStyle(fontFamily = JakartaSans, fontSize = 10.5.sp, fontWeight = FontWeight.W700, color = Q.primary)
    val chip = TextStyle(fontFamily = JakartaSans, fontSize = 11.5.sp, fontWeight = FontWeight.W500, color = Q.mutedFg)
    val chipActive = TextStyle(fontFamily = JakartaSans, fontSize = 11.5.sp, fontWeight = FontWeight.W700, color = Q.primaryFg)
    // D-058: 13sp/W600 → 15sp/W700 — reference app düymə etiketi (16px/600) miqyası
    val button = TextStyle(fontFamily = JakartaSans, fontSize = 15.sp, fontWeight = FontWeight.W700)
    val label = TextStyle(fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W600)
    val input = TextStyle(fontFamily = JakartaSans, fontSize = 13.5.sp, fontWeight = FontWeight.W400)
    val stat = TextStyle(fontFamily = JakartaSans, fontSize = 22.sp, fontWeight = FontWeight.W800)

    /** Şüşə üzərində mətn (glass.dart glassText portu). */
    fun glassText(
        size: Int = 11,
        w: FontWeight = FontWeight.W700,
        color: androidx.compose.ui.graphics.Color = Q.foreground,
    ) = TextStyle(fontFamily = JakartaSans, fontSize = size.sp, fontWeight = w, lineHeight = (size * 1.4).sp, color = color)
}

/** Material3 Typography — Material komponent default-ları T ilə əvəz olunur. */
fun qoshulTypography(): Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = JakartaSans),
        displayMedium = base.displayMedium.copy(fontFamily = JakartaSans),
        displaySmall = base.displaySmall.copy(fontFamily = JakartaSans),
        headlineLarge = T.h1,
        headlineMedium = T.h2,
        headlineSmall = T.h2,
        titleLarge = T.h2,
        titleMedium = T.h3,
        titleSmall = T.button,
        bodyLarge = T.body,
        bodyMedium = T.bodySm,
        bodySmall = T.meta,
        labelLarge = T.button,
        labelMedium = T.label,
        labelSmall = T.metaSm,
    )
}
