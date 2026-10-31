package az.qoshul.app.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Q dizayn tokenləri — Flutter `Q` sinifinin birebir portu (D-023/D-036).
 * Mənbə: web (Amin099015/qoshul) styles.css OKLCH → sRGB.
 * Yoxlanış: docs/audit-flutter-v0.7.0.md bölmə 3.1.
 */
object Q {
    // :root tokenləri
    val background = Color(0xFFF8FAFC)
    val foreground = Color(0xFF0F172B)
    val card = Color(0xFFFFFFFF)
    val primary = Color(0xFF00A63E)
    val primaryFg = Color(0xFFFFFFFF)
    val secondary = Color(0xFFE6F7EA)
    val muted = Color(0xFFEEF2F5)
    // D-057: 4.5:1 AA həddinə yaxın idi — daha tünd (ağıllı günəş altında oxunaqlılıq)
    val mutedFg = Color(0xFF5A6474)
    val accent = Color(0xFFFE9A00)
    val accentFg = Color(0xFF7A3800)
    val destructive = Color(0xFFE7000B)
    val border = Color(0xFFE3E8ED)

    // törəmə rənglər
    val rankBadgeBg = Color(0xFFFEF2DE)
    val overlayDark = Color(0xFF002211)
    val photoShade = Color(0xFF00140E)
    val glass = Color(0x21000000)      // 13% qara
    val glassBorder = Color(0x2EFFFFFF) // 18% ağ

    // AiBox fonu (primary 6% + card)
    val aiBoxBg = Color(0xFFF1FBF4)

    // rezerv status çipi (rol.dart ResvStatusChip — rejected/noShow)
    val resvRejectedBg = Color(0xFFFDE2E3)

    // U3.3 (UI-24): adlı rəng tokenləri — Color.* hardcode QADAĞASI (QAYDA U1).
    // Semantik cütlüklər (primary/primaryFg modelində):
    val photoFg = Color(0xFFFFFFFF)       // mətn/ikon ŞƏKİL üzərində (photoShade fonu)
    val mapPin = Color(0xFFFFFFFF)        // xəritə markerləri (stroke/fill/text)
    val qrLight = Color(0xFFFFFFFF)       // QR işıq modulları (skan oxunuşu üçün ağ)
    val destructiveFg = Color(0xFFFFFFFF) // destructive konteynerin məzmunu

    // "mənim yerim" göstəricisi (xəritə)
    val myLocation = Color(0xFF2D7FF9)

    // tile fallback fonu (offline/test)
    val tileFallback = Color(0xFFEDF3EF)

    // avatar stack dairələri (event_card)
    val avatarColors = listOf(
        Color(0xFF8AD8A8),
        Color(0xFF6FCB95),
        Color(0xFFB7E4C7),
    )

    // D-098 (UI-7): "Yarat" FAB qradiyentinin tünd ucu — primary-dən
    // ~12% tünd yaşıl (marka dərinliyi, lens/FAB kölgələri ilə birlikdə)
    val primaryDeep = Color(0xFF008A33)

    // shimmer rəngləri (motion.dart)
    val shimmerBase = Color(0xFFEEF2F5)
    val shimmerHighlight = Color(0xFFFAFCFE)

    // radiuslar (dp) — web --radius-* ; D-058: Lilt dili
    // (istifadəçi direktivi 2026-10-31: düymə/grid/seçim/dolma barı
    // reference app qədər yumşaq): kartlar 8→16dp, pill %50 kapsul.
    // D-098 (UI-7, direktiv 2026-11-08: "proqram daxili detalları
    // dairəviləşdir"): bütün radiuslar bir pillə yumşaldıldı —
    // rSm 4→8, rMd 6→10, rLg 16→20, rXl 20→24, rMap 18→22, rAiBox 16→20.
    const val rSm = 8.0
    const val rMd = 10.0
    const val rLg = 20.0
    const val rXl = 24.0
    const val rMap = 22.0
    const val rAiBox = 20.0

    /** Kapsul radiusu (PERCENT, dp deyil!) — `RoundedCornerShape(Q.rPill)`
     * hər hündürlükdə tam-yuvarlaq (kapsul) forma verir: düymələr, çiplər. */
    const val rPill = 50
}
