package az.qoshul.app.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * D-118/D-123 rəng sistemi — Q DİNAMİK token dəstidir: hər rəng snapshot
 * state-i (mutableStateOf) daşıyır; [apply] dizayn seçiminə (rəng × mod)
 * görə bütün tokenləri bir anda dəyişir və Compose oxunan hər yeri
 * recompose edir (~700 `Q.xxx` çağırış nöqtəsi dəyişməz qalır).
 *
 * D-123 (direktiv 2026-11-11: „rənglər sistemini qurmaq üçün özün aydınlıqla
 * baxaraq seç, əsas olan 2 rəng yerində qalmalıdır ama ağ və qara modunda
 * olarkən mətnlər asanlıqla oxuna bilməlidir. araşdırma edib rəng palitrası
 * qaydalarına uyğun olaraq dəyişdir + amoled modu əlavə et ağ və qara
 * yanında“) — İKİ SƏVİYYƏLİ BREND DİLİ (Material qara-tema qaydası:
 * dolgu brend rəngi, mətn desktrasiya edilmiş ton — hər ikisi WCAG
 * təsdiqli, scripts/contrast_audit.py + PaletteContrastTest ilə CI-də):
 *
 *  1) [primary] — BREND DOLGU (#008B2D yaşıl / #EAC66C qızılı — istifadəçi
 *     tərəfindən sabitlənib): düymə konteynerləri, FAB qradiyenti, pinlər,
 *     progress zolaqları. Məzmunu [primaryFg] (yaşıl üzərində ağ W700 —
 *     4.44:1, AA-large; qızılı üzərində tünd #3A2B07 — 8.4:1).
 *  2) [primaryText] — MƏTN/İKON TONU (mod-adaptiv): səthlər üzərində brend
 *     vurğuları. Ağ modda tünd variant (yaşıl #007428 — 5.2:1+; qızılı
 *     #7E5812 — 5.6:1+), qara/AMOLED modda işıq variant (yaşıl #4EC97A —
 *     6.1:1+; qızılı #EAC66C — 7.9:1+). D-118-də yalnız `primary` oxunurdu:
 *     qızılı+ağ modda brend mətn 1.57:1 (oxunmaz), yaşıl+qara modda
 *     secondary üzərində 2.9:1 idi — 65 çağırış nöqtəsi primaryText-ə keçib.
 *
 * Modlar (D-123): AĞ (default — #F8FAFC fon), QARA (Material #121212 dili —
 * yaşıl-çalarlı tünd boz #0B100D), **AMOLED** (tam qara #000000 — OLED
 * pikselləri söndürür, batareya qənaəti; kartlar #0D100E haşiyə ilə ayrılır).
 *
 * accentFg #7A3800 → **#6A3000** (accent #FE9A00 üzərində 4.11 → 4.83:1 —
 * badge 10.5sp W800 kiçik mətn üçün 4.5 həddi indi keçilir).
 */
enum class DesignColor(val label: String) {
    GREEN("Yaşıl"), GOLD("Qızılı");

    /** Seçim dialoqundakı nümayiş rəngi (swatch) — brend doldurması. */
    val swatch: Color get() = if (this == GREEN) Color(0xFF008B2D) else Color(0xFFEAC66C)
}

/** Dizayn modu — Ağ (default) / Qara / Amoled (D-119 + D-123). */
enum class DesignMode(val label: String) { LIGHT("Ağ"), DARK("Qara"), AMOLED("Amoled") }

/**
 * Q dizayn tokenləri — Flutter `Q` sinifinin portu (D-023/D-036), D-118-dən
 * dinamik, D-123-də iki-səviyyəli brend dili ilə. Token adları DƏYİŞMİR
 * (ekranlar eyni adlarla oxuyur; YENİ: primaryText).
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
    // D-123: səth üzərində brend MƏTN/İKON tonu (mod-adaptiv, WCAG ≥4.5:1)
    var primaryText by mutableStateOf(Color(0xFF007428))
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
    // D-123: #7A3800 → #6A3000 — accent üzərində 4.83:1 (kiçik badge mətni)
    var accentFg by mutableStateOf(Color(0xFF6A3000))
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

    /** Qara modduru? (Qara/Amoled — Theme.kt sistem bar görünüşü + şüşə tonu) */
    var isDark by mutableStateOf(false)
        private set

    /**
     * Dizayn seçimini bütün tokenlərə tətbiq et (D-118/D-119/D-123). Ayarlardakı
     * „Dizayn“ bölməsi çağırır (AppEngine.setDesignColor/setDesignMode);
     * yazılar snapshot olduğundan bütün kompozisiyalar dərhal yenilənir.
     */
    fun apply(color: DesignColor, mode: DesignMode) {
        val p = palette(color, mode)
        background = p.background; foreground = p.foreground; card = p.card
        primary = p.primary; primaryFg = p.primaryFg; primaryDeep = p.primaryDeep
        primaryText = p.primaryText
        secondary = p.secondary; muted = p.muted; mutedFg = p.mutedFg
        accent = p.accent; accentFg = p.accentFg; destructive = p.destructive
        border = p.border; rankBadgeBg = p.rankBadgeBg
        overlayDark = p.overlayDark; photoShade = p.photoShade
        glass = p.glass; glassBorder = p.glassBorder; aiBoxBg = p.aiBoxBg
        resvRejectedBg = p.resvRejectedBg; myLocation = p.myLocation
        tileFallback = p.tileFallback; avatarColors = p.avatarColors
        shimmerBase = p.shimmerBase; shimmerHighlight = p.shimmerHighlight
        isDark = mode != DesignMode.LIGHT
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
 * Bir dizayn kombinasiyasının tam token dəsti (D-118; D-123-də primaryText
 * əlavə olundu). `palette()` 6 dəyəri (rəng × mod) ilə qurur — brend tonları
 * rəngdən, neytrallar moddan asılıdır.
 */
private class PaletteTokens(
    val background: Color, val foreground: Color, val card: Color,
    val primary: Color, val primaryFg: Color, val primaryDeep: Color,
    val primaryText: Color,
    val secondary: Color, val muted: Color, val mutedFg: Color,
    val accent: Color, val accentFg: Color, val destructive: Color, val border: Color,
    val rankBadgeBg: Color, val overlayDark: Color, val photoShade: Color,
    val glass: Color, val glassBorder: Color, val aiBoxBg: Color,
    val resvRejectedBg: Color, val myLocation: Color, val tileFallback: Color,
    val avatarColors: List<Color>, val shimmerBase: Color, val shimmerHighlight: Color,
)

/**
 * Brend ton dəsti (D-123) — dolgu (primary) hər modda SABİT brend rəngidir;
 * mətn tonu (primaryText) moda görə tünd/işıq variant alır. Kontrastlar
 * scripts/contrast_audit.py + PaletteContrastTest (CI) ilə bağlanılıb:
 * primaryText ən çətin səthdə (secondary) belə ≥4.5:1.
 */
private class BrandSet(
    val primary: Color, val primaryFg: Color, val primaryDeep: Color,
    val primaryTextLight: Color, val primaryTextDark: Color,
    val secondaryLight: Color, val secondaryDark: Color,
    val aiBoxLight: Color, val aiBoxDark: Color,
    val tileLight: Color, val tileDark: Color, val tileAmoled: Color,
    val avatarLight: List<Color>, val avatarDark: List<Color>,
)

private fun brandSet(color: DesignColor) = when (color) {
    // YAŞIL #008B2D (brend dolgu — istifadəçi direktivi 2026-11-10).
    // primaryText: ağ modda #007428 (secondary #E2F4E8 üzərində 5.2:1),
    // qara/AMOLED modda #4EC97A (secondary #16382A üzərində 6.1:1) —
    // Material qara-tema qaydası: işıq modda ton-800, tünd modda ton-200.
    DesignColor.GREEN -> BrandSet(
        primary = Color(0xFF008B2D),
        primaryFg = Color(0xFFFFFFFF),
        primaryDeep = Color(0xFF007024),
        primaryTextLight = Color(0xFF007428),
        primaryTextDark = Color(0xFF4EC97A),
        secondaryLight = Color(0xFFE2F4E8), secondaryDark = Color(0xFF16382A),
        aiBoxLight = Color(0xFFEFF9F2), aiBoxDark = Color(0xFF122619),
        tileLight = Color(0xFFEDF3EF), tileDark = Color(0xFF121814), tileAmoled = Color(0xFF0F1412),
        avatarLight = listOf(Color(0xFFA5DDB6), Color(0xFF7BC894), Color(0xFFC9EFD6)),
        avatarDark = listOf(Color(0xFF2F7D4F), Color(0xFF26683F), Color(0xFF3B9160)),
    )
    // QIZILI #EAC66C (brend dolgu). primaryText: ağ modda #7E5812 (tünd kəhrəba
    // — brend özü ağ fonda 1.6:1 oxunmazdır), qara/AMOLED modda brend özü
    // (#EAC66C tünd səthlərdə 7.9:1+ — ən yaxşı hal).
    DesignColor.GOLD -> BrandSet(
        primary = Color(0xFFEAC66C),
        primaryFg = Color(0xFF3A2B07),
        primaryDeep = Color(0xFFD9AE55),
        primaryTextLight = Color(0xFF7E5812),
        primaryTextDark = Color(0xFFEAC66C),
        secondaryLight = Color(0xFFF8EFD8), secondaryDark = Color(0xFF3A3013),
        aiBoxLight = Color(0xFFFAF3E1), aiBoxDark = Color(0xFF2B2410),
        tileLight = Color(0xFFF5F0E2), tileDark = Color(0xFF171308), tileAmoled = Color(0xFF131006),
        avatarLight = listOf(Color(0xFFEBD9A8), Color(0xFFE3C77E), Color(0xFFF4E7C4)),
        avatarDark = listOf(Color(0xFF8A6F35), Color(0xFFA5853E), Color(0xFF6F5726)),
    )
}

/** Qara/AMOLED mod neytralları (D-123) — qara #121212 dili vs tam-qara AMOLED. */
private class DarkNeutrals(
    val bg: Color, val card: Color, val muted: Color, val mutedFg: Color,
    val border: Color, val shimmerBase: Color, val shimmerHighlight: Color,
)

private fun darkNeutrals(amoled: Boolean) = if (amoled) DarkNeutrals(
    bg = Color(0xFF000000), card = Color(0xFF0D100E), muted = Color(0xFF141815),
    mutedFg = Color(0xFF97A79C), border = Color(0xFF222A24),
    shimmerBase = Color(0xFF141815), shimmerHighlight = Color(0xFF1E2420),
) else DarkNeutrals(
    bg = Color(0xFF0B100D), card = Color(0xFF141B16), muted = Color(0xFF1C2620),
    mutedFg = Color(0xFF9DB0A4), border = Color(0xFF273529),
    shimmerBase = Color(0xFF1C2620), shimmerHighlight = Color(0xFF242F28),
)

/**
 * Dizayn kombinasiyası → token dəsti (D-118 tərifi, D-123 yenidən qurulub —
 * rəng [brandSet] + mod neytralları vahid cədvəldə). Sınaq: PaletteContrastTest
 * (WCAG riyaziyyatı) + DesignSettingsTest (apply matrisi).
 */
private fun palette(color: DesignColor, mode: DesignMode): PaletteTokens {
    val b = brandSet(color)
    val dark = mode != DesignMode.LIGHT
    val amoled = mode == DesignMode.AMOLED
    // — mod neytralları (rəngdən asılı deyil) —
    // AĞ: D-023 web-paritet. QARA: Material #121212 dili — yaşıl-çalarlı tünd
    // boz (gölgə dərinliyi görünür, göz üçün saf qaradan yumşaq).
    // AMOLED (D-123): tam qara #000000 — OLED pikselləri tam söndürür;
    // kart #0D100E + haşiyə ilə səviyyə ayrımı (qara üzərində kölgə görünməz).
    return if (!dark) {
        PaletteTokens(
            background = Color(0xFFF8FAFC), foreground = Color(0xFF0F172B), card = Color(0xFFFFFFFF),
            primary = b.primary, primaryFg = b.primaryFg, primaryDeep = b.primaryDeep,
            primaryText = b.primaryTextLight,
            secondary = b.secondaryLight, muted = Color(0xFFEEF2F5), mutedFg = Color(0xFF5A6474),
            accent = Color(0xFFFE9A00), accentFg = Color(0xFF6A3000),
            destructive = Color(0xFFE7000B), border = Color(0xFFE3E8ED),
            rankBadgeBg = Color(0xFFFEF2DE), overlayDark = Color(0xFF002211), photoShade = Color(0xFF00140E),
            glass = Color(0x21000000), glassBorder = Color(0x2EFFFFFF), aiBoxBg = b.aiBoxLight,
            resvRejectedBg = Color(0xFFFDE2E3), myLocation = Color(0xFF2D7FF9),
            tileFallback = b.tileLight,
            avatarColors = b.avatarLight,
            shimmerBase = Color(0xFFEEF2F5), shimmerHighlight = Color(0xFFFAFCFE),
        )
    } else {
        val n = darkNeutrals(amoled)
        PaletteTokens(
            background = n.bg, foreground = Color(0xFFEDF4EF), card = n.card,
            primary = b.primary, primaryFg = b.primaryFg, primaryDeep = b.primaryDeep,
            primaryText = b.primaryTextDark,
            secondary = b.secondaryDark, muted = n.muted, mutedFg = n.mutedFg,
            accent = Color(0xFFFE9A00), accentFg = Color(0xFF6A3000),
            // qara səthlərdə parlaq qırmızı (D-118 #FF4549 — 5.2:1; #E7000B 3.7:1 idi)
            destructive = Color(0xFFFF4549), border = n.border,
            rankBadgeBg = Color(0xFF33290F), overlayDark = Color(0xFF000000),
            photoShade = if (amoled) Color(0xFF000105) else Color(0xFF05080A),
            glass = Color(0x14FFFFFF), glassBorder = Color(0x2EFFFFFF), aiBoxBg = b.aiBoxDark,
            resvRejectedBg = Color(0xFF3B1D20), myLocation = Color(0xFF2D7FF9),
            tileFallback = if (amoled) b.tileAmoled else b.tileDark,
            avatarColors = b.avatarDark,
            shimmerBase = n.shimmerBase, shimmerHighlight = n.shimmerHighlight,
        )
    }
}
