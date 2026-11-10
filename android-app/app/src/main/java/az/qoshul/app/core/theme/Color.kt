package az.qoshul.app.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * D-118 (v0.42.0, direktiv 2026-11-10: „#008b2d main rəng olacaq,
 * ayarlarda dizayn bölməsində ağ və qara modu əlavə olunacaq, seçim
 * olaraq rənglər #008b2d (default) və #eac66c. ağ (default) və qara“):
 * Q artıq DİNAMİK token dəstidir — hər rəng snapshot state-ini (mutableStateOf)
 * daşıyır; [apply] dizayn seçiminə (rəng × mod) görə bütün tokenləri bir anda
 * dəyişir və Compose oxunan hər yeri recompose edir. D-116-nın indiqo əsas
 * rəngi LƏĞV olundu — brend əsası YAŞIL #008B2D-dür (istifadəçi direktivi).
 *
 * Mexanika (D-118 qərarı): əvvəl Q sabit obyekt idi (~700 istifadə nöqtəsi
 * `Q.xxx` şəklində) — CompositionLocal/q parametr ötürməsi ilə yenidən yazmaq
 * əvəzinə property-lər `by mutableStateOf` backed oldu: kompozisiya zamanı
 * oxunan hər token avtomatik müşahidə olunur, [apply] yazanda bütün asılı
 * kompozisiyalar yenidən çəkilir. Qeyri-kompozisiya kontekstlərində (xətta
 * `remember {}` içində) oxunuş adi property kimi işləyir — sıfır call-site
 * dəyişikliyi. T tipografiyasındakı rəngli stillər (Type.kt) getter-prop
 * oldu — TextStyle dəyəri bir dəfəlik donmasın.
 *
 * Dizayn seçimləri (D-119): Rəng — Yaşıl (#008B2D, default) / Qızılı
 * (#EAC66C); Mod — Ağ (default) / Qara. 4 kombinasiya [PaletteTokens]
 * ilə tərif olunub; neytrallar (fon/kart/mətn/haşiyə) moddan, brend
 * tonları (primary/secondary/aiBoxBg/avatar) rəngdən asılıdır. Qara modda
 * primaryFg qızılı üçün TÜND (ağ yazı #EAC66C üzərində 1.6:1 — oxunmaz),
 * yaşıl üçün AĞ qalır (4.4:1, W700 düymə mətni üçün AA-böyük).
 */
enum class DesignColor(val label: String) {
    GREEN("Yaşıl"), GOLD("Qızılı");

    /** Seçim dialoquundakı nümayiş rəngi (swatch). */
    val swatch: Color get() = if (this == GREEN) Color(0xFF008B2D) else Color(0xFFEAC66C)
}

/** Dizayn modu — Ağ (default) / Qara (D-119). */
enum class DesignMode(val label: String) { LIGHT("Ağ"), DARK("Qara") }

/**
 * Q dizayn tokenləri — Flutter `Q` sinifinin portu (D-023/D-036), D-118-dən
 * dinamik. Token adları/dili DƏYİŞMİR (ekranlar eyni adlarla oxuyur).
 * Yoxlanış: docs/audit-flutter-v0.7.0.md bölmə 3.1.
 */
object Q {
    // — :root tokenləri (D-118: snapshot-backed; başlanğıc = AĞ + YAŞIL) —
    var background by mutableStateOf(Color(0xFFF8FAFC))
        private set
    var foreground by mutableStateOf(Color(0xFF0F172B))
        private set
    var card by mutableStateOf(Color(0xFFFFFFFF))
        private set
    var primary by mutableStateOf(Color(0xFF008B2D))
        private set
    var primaryFg by mutableStateOf(Color(0xFFFFFFFF))
        private set
    var secondary by mutableStateOf(Color(0xFFE2F4E8))
        private set
    var muted by mutableStateOf(Color(0xFFEEF2F5))
        private set
    // D-057: 4.5:1 AA həddinə yaxın idi — daha tünd (ağıllı günəş altında oxunaqlılıq)
    var mutedFg by mutableStateOf(Color(0xFF5A6474))
        private set
    var accent by mutableStateOf(Color(0xFFFE9A00))
        private set
    var accentFg by mutableStateOf(Color(0xFF7A3800))
        private set
    var destructive by mutableStateOf(Color(0xFFE7000B))
        private set
    var border by mutableStateOf(Color(0xFFE3E8ED))
        private set

    // — törəmə rənglər —
    var rankBadgeBg by mutableStateOf(Color(0xFFFEF2DE))
        private set
    var overlayDark by mutableStateOf(Color(0xFF002211))
        private set
    var photoShade by mutableStateOf(Color(0xFF00140E))
        private set
    var glass by mutableStateOf(Color(0x21000000)) // 13% qara
        private set
    var glassBorder by mutableStateOf(Color(0x2EFFFFFF)) // 18% ağ
        private set

    // AiBox fonu (primary ~6% + kart tonu)
    var aiBoxBg by mutableStateOf(Color(0xFFEFF9F2))
        private set

    // rezerv status çipi (rol.dart ResvStatusChip — rejected/noShow)
    var resvRejectedBg by mutableStateOf(Color(0xFFFDE2E3))
        private set

    // U3.3 (UI-24): adlı rəng tokenləri — Color.* hardcode QADAĞASI (QAYDA U1).
    // Semantik cütlüklər (primary/primaryFg modelində) — bütün modlarda sabit:
    val photoFg = Color(0xFFFFFFFF)       // mətn/ikon ŞƏKİL üzərində (photoShade fonu)
    val mapPin = Color(0xFFFFFFFF)        // xəritə markerləri (stroke/fill/text)
    val qrLight = Color(0xFFFFFFFF)       // QR işıq modulları (skan oxunuşu üçün ağ)
    val destructiveFg = Color(0xFFFFFFFF) // destructive konteynerin məzmunu

    // „mənim yerim“ göstəricisi (xəritə)
    var myLocation by mutableStateOf(Color(0xFF2D7FF9))
        private set

    // tile fallback fonu (offline/test)
    var tileFallback by mutableStateOf(Color(0xFFEDF3EF))
        private set

    // avatar stack dairələri (event_card) — brend ailəsi (rəng seçiminə görə)
    var avatarColors by mutableStateOf(
        listOf(Color(0xFFA5DDB6), Color(0xFF7BC894), Color(0xFFC9EFD6))
    )
        private set

    // "Yarat"/AI FAB qradiyentinin tünd ucu — primary-dən ~12% tünd brend
    var primaryDeep by mutableStateOf(Color(0xFF007024))
        private set

    // shimmer rəngləri (motion.dart)
    var shimmerBase by mutableStateOf(Color(0xFFEEF2F5))
        private set
    var shimmerHighlight by mutableStateOf(Color(0xFFFAFCFE))
        private set

    /** Qara modduru? (Theme.kt sistem bar görünüşü + şüşə tonu üçün) */
    var isDark by mutableStateOf(false)
        private set

    /**
     * Dizayn seçimini bütün tokenlərə tətbiq et (D-118/D-119). Ayarlardakı
     * „Dizayn“ bölməsi çağırır (AppEngine.setDesignColor/setDesignMode);
     * yazılar snapshot olduğundan bütün kompozisiyalar dərhal yenilənir.
     */
    fun apply(color: DesignColor, mode: DesignMode) {
        val p = palette(color, mode)
        background = p.background; foreground = p.foreground; card = p.card
        primary = p.primary; primaryFg = p.primaryFg; primaryDeep = p.primaryDeep
        secondary = p.secondary; muted = p.muted; mutedFg = p.mutedFg
        accent = p.accent; accentFg = p.accentFg; destructive = p.destructive
        border = p.border; rankBadgeBg = p.rankBadgeBg
        overlayDark = p.overlayDark; photoShade = p.photoShade
        glass = p.glass; glassBorder = p.glassBorder; aiBoxBg = p.aiBoxBg
        resvRejectedBg = p.resvRejectedBg; myLocation = p.myLocation
        tileFallback = p.tileFallback; avatarColors = p.avatarColors
        shimmerBase = p.shimmerBase; shimmerHighlight = p.shimmerHighlight
        isDark = mode == DesignMode.DARK
    }

    // radiuslar (dp) — web --radius-* ; D-058: Lilt dili
    // (istifadəçi direktivi 2026-10-31: düymə/grid/seçim/dolma barı
    // reference app qədər yumşaq): kartlar 8→16dp, pill %50 kapsul.
    // D-098 (UI-7, direktif 2026-11-08: "proqram daxili detalları
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

/**
 * Bir dizayn kombinasiyasının tam token dəsti (D-118). `palette()` 4 dəyəri
 * (rəng × mod) ilə qurur — brend tonları rəngdən, neytrallar moddan.
 */
private class PaletteTokens(
    val background: Color, val foreground: Color, val card: Color,
    val primary: Color, val primaryFg: Color, val primaryDeep: Color,
    val secondary: Color, val muted: Color, val mutedFg: Color,
    val accent: Color, val accentFg: Color, val destructive: Color, val border: Color,
    val rankBadgeBg: Color, val overlayDark: Color, val photoShade: Color,
    val glass: Color, val glassBorder: Color, val aiBoxBg: Color,
    val resvRejectedBg: Color, val myLocation: Color, val tileFallback: Color,
    val avatarColors: List<Color>, val shimmerBase: Color, val shimmerHighlight: Color,
)

/** Dizayn kombinasiyası → token dəsti (D-118 tərifi; sınaq: Q.apply + token oxunuşu). */
private fun palette(color: DesignColor, mode: DesignMode): PaletteTokens {
    val dark = mode == DesignMode.DARK
    val brand = when (color) {
        // YAŞIL #008B2D (default): ağ üzərində 4.4:1; törəmə tonlar v0.39
        // yaşıl ailəsindən (#00A63E) daha düd rəngə uyğunlaşdırılıb.
        DesignColor.GREEN -> listOf(
            Color(0xFF008B2D), // primary
            Color(0xFFFFFFFF), // primaryFg
            Color(0xFF007024), // primaryDeep
            if (dark) Color(0xFF16382A) else Color(0xFFE2F4E8), // secondary
            if (dark) Color(0xFF122619) else Color(0xFFEFF9F2), // aiBoxBg
        ) + if (dark) {
            listOf(Color(0xFF2F7D4F), Color(0xFF26683F), Color(0xFF3B9160)) // avatar
        } else {
            listOf(Color(0xFFA5DDB6), Color(0xFF7BC894), Color(0xFFC9EFD6))
        }
        // QIZILI #EAC66C: ağ yazı 1.6:1 — oxunmaz; primaryFg TÜND dəridir
        // (kontrast ~9:1), W700 düymə etiketləri AA keçir.
        DesignColor.GOLD -> listOf(
            Color(0xFFEAC66C),
            Color(0xFF3A2B07),
            Color(0xFFD9AE55),
            if (dark) Color(0xFF3A3013) else Color(0xFFF8EFD8),
            if (dark) Color(0xFF2B2410) else Color(0xFFFAF3E1),
        ) + if (dark) {
            listOf(Color(0xFF8A6F35), Color(0xFFA5853E), Color(0xFF6F5726))
        } else {
            listOf(Color(0xFFEBD9A8), Color(0xFFE3C77E), Color(0xFFF4E7C4))
        }
    }
    // neytrallar moddan asılıdır (rəngdən YOX)
    return if (!dark) {
        PaletteTokens(
            background = Color(0xFFF8FAFC), foreground = Color(0xFF0F172B), card = Color(0xFFFFFFFF),
            primary = brand[0], primaryFg = brand[1], primaryDeep = brand[2],
            secondary = brand[3], muted = Color(0xFFEEF2F5), mutedFg = Color(0xFF5A6474),
            accent = Color(0xFFFE9A00), accentFg = Color(0xFF7A3800),
            destructive = Color(0xFFE7000B), border = Color(0xFFE3E8ED),
            rankBadgeBg = Color(0xFFFEF2DE), overlayDark = Color(0xFF002211), photoShade = Color(0xFF00140E),
            glass = Color(0x21000000), glassBorder = Color(0x2EFFFFFF), aiBoxBg = brand[4],
            resvRejectedBg = Color(0xFFFDE2E3), myLocation = Color(0xFF2D7FF9),
            tileFallback = if (color == DesignColor.GOLD) Color(0xFFF5F0E2) else Color(0xFFEDF3EF),
            avatarColors = brand.subList(5, 8),
            shimmerBase = Color(0xFFEEF2F5), shimmerHighlight = Color(0xFFFAFCFE),
        )
    } else {
        PaletteTokens(
            background = Color(0xFF0B100D), foreground = Color(0xFFECF3EE), card = Color(0xFF141B16),
            primary = brand[0], primaryFg = brand[1], primaryDeep = brand[2],
            secondary = brand[3], muted = Color(0xFF1C2620), mutedFg = Color(0xFF9DB0A4),
            accent = Color(0xFFFE9A00), accentFg = Color(0xFF7A3800),
            destructive = Color(0xFFFF4549), border = Color(0xFF273529),
            rankBadgeBg = Color(0xFF33290F), overlayDark = Color(0xFF000000), photoShade = Color(0xFF05080A),
            glass = Color(0x14FFFFFF), glassBorder = Color(0x2EFFFFFF), aiBoxBg = brand[4],
            resvRejectedBg = Color(0xFF3B1D20), myLocation = Color(0xFF2D7FF9),
            tileFallback = if (color == DesignColor.GOLD) Color(0xFF171308) else Color(0xFF121814),
            avatarColors = brand.subList(5, 8),
            shimmerBase = Color(0xFF1C2620), shimmerHighlight = Color(0xFF242F28),
        )
    }
}
