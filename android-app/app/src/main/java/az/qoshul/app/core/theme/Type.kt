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
 * Open Sans statik instansiyaları (D-100) res/font-da: "noto-set" rəsmi
 * build (Ə/₼ daxil) + ← → qlifləri PJS-dən portlanıb (OFL 1.1, RFN yoxdur;
 * təfsilat docs/font-licenses.md). Ölçü tokenləri dəyişməz (QAYDA U1).
 */
val OpenSans = FontFamily(
    Font(R.font.opensans_regular, FontWeight.W400),
    Font(R.font.opensans_medium, FontWeight.W500),
    Font(R.font.opensans_semibold, FontWeight.W600),
    Font(R.font.opensans_bold, FontWeight.W700),
    Font(R.font.opensans_extrabold, FontWeight.W800),
)

object T {
    val hero = TextStyle(fontFamily = OpenSans, fontSize = 27.sp, fontWeight = FontWeight.W800, lineHeight = 35.sp)
    val h1 = TextStyle(fontFamily = OpenSans, fontSize = 24.sp, fontWeight = FontWeight.W800, lineHeight = 32.sp)
    val h2 = TextStyle(fontFamily = OpenSans, fontSize = 19.sp, fontWeight = FontWeight.W800, lineHeight = 27.sp)
    val h3 = TextStyle(fontFamily = OpenSans, fontSize = 13.sp, fontWeight = FontWeight.W800, lineHeight = 20.sp)
    val eventTitle = TextStyle(fontFamily = OpenSans, fontSize = 16.sp, fontWeight = FontWeight.W800, lineHeight = 24.sp)
    val personName = TextStyle(fontFamily = OpenSans, fontSize = 15.sp, fontWeight = FontWeight.W800, lineHeight = 21.sp)
    // D-118: rəng daşıyan stillər GETTER-prop-dur — TextStyle bir dəfəlik
    // donmasın; Q.apply (dizayn seçimi) dəyəşəndə hər oxunuş təzə rəng qaytarır.
    val subtitle: TextStyle
        get() = TextStyle(fontFamily = OpenSans, fontSize = 13.sp, fontWeight = FontWeight.W400, lineHeight = 22.sp, color = Q.mutedFg)
    val body = TextStyle(fontFamily = OpenSans, fontSize = 13.5.sp, lineHeight = 23.sp)
    val bodySm = TextStyle(fontFamily = OpenSans, fontSize = 12.5.sp, lineHeight = 20.sp)
    val meta: TextStyle
        get() = TextStyle(fontFamily = OpenSans, fontSize = 12.sp, lineHeight = 18.sp, color = Q.mutedFg)
    val metaSm: TextStyle
        get() = TextStyle(fontFamily = OpenSans, fontSize = 10.5.sp, lineHeight = 16.sp, color = Q.mutedFg)
    val category: TextStyle
        get() = TextStyle(fontFamily = OpenSans, fontSize = 10.5.sp, fontWeight = FontWeight.W700, color = Q.primaryText)
    val chip: TextStyle
        get() = TextStyle(fontFamily = OpenSans, fontSize = 11.5.sp, fontWeight = FontWeight.W500, color = Q.mutedFg)
    val chipActive: TextStyle
        get() = TextStyle(fontFamily = OpenSans, fontSize = 11.5.sp, fontWeight = FontWeight.W700, color = Q.primaryFg)
    // D-058: 13sp/W600 → 15sp/W700 — reference app düymə etiketi (16px/600) miqyası
    val button = TextStyle(fontFamily = OpenSans, fontSize = 15.sp, fontWeight = FontWeight.W700)
    val label = TextStyle(fontFamily = OpenSans, fontSize = 12.sp, fontWeight = FontWeight.W600)
    val input = TextStyle(fontFamily = OpenSans, fontSize = 13.5.sp, fontWeight = FontWeight.W400)
    val stat = TextStyle(fontFamily = OpenSans, fontSize = 22.sp, fontWeight = FontWeight.W800)

    // U3.2 (UI-07): audit-in ən çox təkrarlanan inline kombinasiyaları üçün
    // tokenlər (141 inline TextStyle → T.*; ölçülər YALNIZ tokenlərdən — QAYDA U1).
    // Harmoniya qeydləri: 17→18 (titleLg), 14/14.5→15 (personName), 11→10.5/11.5.
    val bodyXs = TextStyle(fontFamily = OpenSans, fontSize = 12.sp, lineHeight = 18.sp)            // W400
    val metaBold = TextStyle(fontFamily = OpenSans, fontSize = 12.sp, fontWeight = FontWeight.W700, lineHeight = 18.sp)
    val chipXs = TextStyle(fontFamily = OpenSans, fontSize = 11.5.sp, lineHeight = 17.sp)          // W400
    val bodySmBold = TextStyle(fontFamily = OpenSans, fontSize = 12.5.sp, fontWeight = FontWeight.W700, lineHeight = 20.sp)
    val bodySmSemi = TextStyle(fontFamily = OpenSans, fontSize = 12.5.sp, fontWeight = FontWeight.W600, lineHeight = 20.sp)
    val bodySmW = TextStyle(fontFamily = OpenSans, fontSize = 12.5.sp, fontWeight = FontWeight.W800, lineHeight = 20.sp)
    val bodyBold = TextStyle(fontFamily = OpenSans, fontSize = 13.sp, fontWeight = FontWeight.W700, lineHeight = 22.sp)
    val bodySemi = TextStyle(fontFamily = OpenSans, fontSize = 13.sp, fontWeight = FontWeight.W600, lineHeight = 22.sp)
    val titleLg = TextStyle(fontFamily = OpenSans, fontSize = 18.sp, fontWeight = FontWeight.W800, lineHeight = 26.sp)
    val statSm = TextStyle(fontFamily = OpenSans, fontSize = 20.sp, fontWeight = FontWeight.W800, lineHeight = 28.sp)
    /** Badge (UI-25): minimum oxunulmaz hədd 10.5sp — 8.5/9sp buraya harmonizə olunur. */
    val badge = TextStyle(fontFamily = OpenSans, fontSize = 10.5.sp, fontWeight = FontWeight.W800, lineHeight = 15.sp)

    /** Şüşə üzərində mətn (glass.dart glassText portu). */
    fun glassText(
        size: Int = 11,
        w: FontWeight = FontWeight.W700,
        color: androidx.compose.ui.graphics.Color = Q.foreground,
    ) = TextStyle(fontFamily = OpenSans, fontSize = size.sp, fontWeight = w, lineHeight = (size * 1.4).sp, color = color)

    /** U3.2 (UI-07): brand elementləri (Logo) — parametrləşdirilmiş ölçü.
     *  Yalnız brand/wordmark üçün; mətn ölçüləri sabit tokenlərdən gəlməlidir. */
    fun brand(size: Double, color: androidx.compose.ui.graphics.Color = Q.foreground) =
        TextStyle(fontFamily = OpenSans, fontSize = size.sp, fontWeight = FontWeight.W800, color = color)
}

/** Material3 Typography — Material komponent default-ları T ilə əvəz olunur. */
fun qoshulTypography(): Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = OpenSans),
        displayMedium = base.displayMedium.copy(fontFamily = OpenSans),
        displaySmall = base.displaySmall.copy(fontFamily = OpenSans),
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
