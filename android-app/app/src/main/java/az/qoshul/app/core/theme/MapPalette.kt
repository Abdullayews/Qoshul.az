package az.qoshul.app.core.theme

/**
 * F.2 (D-170): XƏRİTƏ PALİTRASI — hər iki backend-in (MapLibre bright stil +
 * Google stil JSON) ortaq rəng sabitləri.
 *
 * Direktiv: „rəng kontrastını artır: parlaq, realistik yol/su/yaşıllıq/bina
 * rəngləri. PaletteContrastTest-i yenilə“.
 *
 * WCAG 1.4.11 (non-text ≥ 3:1) ayrılma — PaletteContrastTest-də sübutlu:
 *   su↔torpaq 3.72:1, yaşıllıq↔torpaq 3.13:1, arterial konturu↔torpaq 3.3:1,
 *   park etiketi↔park fonu 3.22:1.
 * Lokal ağ yollar və bej binalar realistiklik üçün yumşaq qalır (1.1–1.2:1)
 * — onlar magistral/arterial kontur və kənar xəttlə ilə ayrılır.
 */
object MapPalette {
    /** Torpaq / fon (landuse-base) — isti açıq bej */
    const val LAND = 0xFFFAF7F0.toInt()

    /** Yol şəbəkəsi — lokal küçələr (ağ, realistik) */
    const val ROAD_LOCAL = 0xFFFFFFFF.toInt()

    /** Arterial yollar — parlaq sarı + tünd kontur (3.3:1 ayrılma) */
    const val ROAD_ARTERIAL = 0xFFF2B705.toInt()
    const val ROAD_ARTERIAL_STROKE = 0xFFB37F00.toInt()

    /** Magistrallar — tünd konturlu narıncı */
    const val ROAD_HIGHWAY = 0xFFFFC94D.toInt()
    const val ROAD_HIGHWAY_STROKE = 0xFFC97F1E.toInt()

    /** Su — canlı göy (3.72:1) */
    const val WATER = 0xFF2E86BE.toInt()

    /** Yaşıllıq — doyğun yaşıl park (3.13:1) + təbii landşaft */
    const val PARK = 0xFF4E9E32.toInt()
    const val NATURE = 0xFF9CD07C.toInt()
    const val PARK_LABEL = 0xFF17470F.toInt()

    /** Binalar / insan qurğuları — aydıntılı bej */
    const val BUILDING = 0xFFE8DFCE.toInt()
    const val POI = 0xFFDCD2BC.toInt()

    /** Etiket mətni və halqa rəngi */
    const val LABEL_FILL = 0xFF3A3A3A.toInt()
    const val LABEL_HALO = 0xFFFFFFFF.toInt()
}
