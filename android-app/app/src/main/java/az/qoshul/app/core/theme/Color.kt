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
    val mutedFg = Color(0xFF687280)
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

    // shimmer rəngləri (motion.dart)
    val shimmerBase = Color(0xFFEEF2F5)
    val shimmerHighlight = Color(0xFFFAFCFE)

    // radiuslar (dp) — web --radius-*
    const val rSm = 4.0
    const val rMd = 6.0
    const val rLg = 8.0
    const val rXl = 8.0
    const val rMap = 18.0
    const val rAiBox = 16.0
}
