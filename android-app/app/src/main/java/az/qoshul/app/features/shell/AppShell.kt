package az.qoshul.app.features.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.features.chats.ChatsScreen
import az.qoshul.app.features.feed.FeedScreen
import az.qoshul.app.features.map.MapScreen
import az.qoshul.app.features.profile.ProfileScreen
import az.qoshul.app.core.glass.LiquidGlassSurface
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.PopIn
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.ChatFocusState
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.WindowWidthClass
import az.qoshul.app.core.ui.Wordmark
import az.qoshul.app.core.ui.rememberWindowWidthClass
import az.qoshul.app.core.ui.floatingtabbar.FloatingTabBar
import az.qoshul.app.core.ui.floatingtabbar.FloatingTabBarDefaults
import az.qoshul.app.core.ui.floatingtabbar.FloatingTabBarInlineBehavior
import az.qoshul.app.core.ui.floatingtabbar.FloatingTabBarScrollConnection
import az.qoshul.app.core.ui.floatingtabbar.rememberFloatingTabBarScrollConnection
import az.qoshul.app.core.model.QoshulEvent
// — D-107: əsl liquid glass (Echo-Music recipesi, Kyant0/backdrop) —
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import az.qoshul.app.core.glass.LocalAppBackdrop
import az.qoshul.app.core.glass.NavPalette
import az.qoshul.app.core.glass.isLiquidGlassAvailable
import az.qoshul.app.core.glass.liquidGlassNavBar
import az.qoshul.app.core.glass.navFallbackPalette
import az.qoshul.app.core.glass.navGlassPalette
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Əsas tətbiq qabığı — 4 tab: Kəşf · Xəritə · [Yarat] · Söhbətlər · Profil.
 * Kəşf = ana ekran; içində "Sənin üçün | Tədbirlər" daxili tabları.
 * Edge-to-edge (D-031): məzmun şəffaf barların altından axır.
 *
 * D-050: [focus] — xəritə fokus istəyi → Xəritə tabı ("Yol göstər/tap").
 * D-055: [chatFocus] — söhbət fokusunda Söhbətlər tabına keçid.
 *
 * UI-7 (D-098, direktiv 2026-11-08) — NAV AUTO-HIDE:
 *  - Söhbətlər tabına girişdə nav gizlənir və orada GİZLİ QALIR
 *    (yuxarı sürüşdərmə belə göstərmir — söhbət immersivdir);
 *  - digər tablarda: aşağı sürüşdürmə → gizlənir, yuxarı → qayıdır
 *    ("hover" hərəkəti — spring slide + fade);
 *  - Söhbətlərdən çıxış: sistem GERİ → Kəşf (açıq çatın öz
 *    BackHandler-i ChatsScreen-də prioritetli qalır);
 *  - mexanizm: NestedScrollConnection qabıq səviyyəsində — HEÇ bir
 *    ekrana toxunmur, bütün scrollable nəvələrin deltasını görür.
 *
 * D-102 (v0.35.0): ADAPTİV NAVİGASİYA — pəncərə eni ≥600dp (Medium/
 * Expanded — planşet, qatlanan açıq) alt panel əvəzinə solda
 * şaquli LiquidRail; Compact (telefon) davranışı BİT-BİT eyni qalır.
 * Rail rejimində auto-hide bağlanır (rail məzmunu örtmür, gizlənmə
 * mənasızdır); tab keçidləri tween əvəzinə spring fizikasına keçir.
 *
 * D-105 (v0.37.0, direktiv 2026-11-09: "göndərdiyim navigation bar
 * stilini implementasiya et; yığılmış haldakı sağ və solda olan
 * düymələr home və chat düyməsi olsun"): Compact alt panel artıq
 * FloatingTabBar-dır (iOS 26 dili — vendored core/ui/floatingtabbar).
 * D-106 (v0.38.0, direktiv 2026-11-09: „açıq halda hamsı eyni pill
 * içində olmalıdır, bağlandıqda isə home və mesajlar öz yerlərində,
 * ortada yox“):
 *  - AÇIQ: VAHİD PILL [Kəşf · Xəritə · Yarat · Profil · Söhbətlər
 *    dairəsi] — hamsı eyni həbin içində;
 *  - YIĞILMIŞ: [Kəşf həbi SOL KƏNARDA] [Söhbətlər dairəsi SAĞ
 *    KƏNARDA] — mərkəzdə klaster YOX;
 *  - aşağı sürüşmə = yığılma, yuxarı = açılma (14dp — D-098 həddi,
 *    gizlətmə YERİNƏ yığılma — bar həmişə əlçatandır);
 *  - Söhbətlər tabında nav gizli qalır (D-098/7 immersive), çıxışda
 *    AÇIQ vəziyyətdə qayıdır.
 */
@Composable
fun AppShell(
    engine: AppEngine,
    nav: NavState,
    onOpenCreate: () -> Unit,
    focus: MapFocusState = remember { MapFocusState() },
    chatFocus: ChatFocusState? = null,
    onFinishActivity: (QoshulEvent) -> Unit = {},
    onOpenChat: (QoshulEvent) -> Unit = {},
) {
    // U2.2 (UI-02): aktiv tab + nav gizlilik proses ölümündən bərpa olunur
    var index by rememberSaveable { mutableStateOf(0) }
    var navHidden by rememberSaveable { mutableStateOf(false) }
    var wasChats by rememberSaveable { mutableStateOf(false) }

    // "Yol göstər / Yol tap" → xəritə tabına keç (D-050)
    LaunchedEffect(focus.request) {
        if (focus.request != null) index = 1
    }

    // "Qrup söhbətinə bax / Söhbətə keç" → Söhbətlər tabına keç (D-055)
    LaunchedEffect(chatFocus?.request) {
        if (chatFocus?.request != null) index = 2
    }

    // D-105: collapse/expand scroll keçidi — komponentin öz
    // NestedScrollConnection-ı. 14dp hədd — D-098/7 tuningi miras alınır;
    // istiqamət dəyişəndə akkumulyator sıfırlanır (komponent daxilində).
    // Aşağı delta = YIĞ (inline), yuxarı delta = AÇ (expanded) — köhnə
    // "gizlət/göstər" ƏVƏZİNƏ: bar həmişə görünür, yalnız forma dəyişir.
    val navScroll = rememberFloatingTabBarScrollConnection(
        initialIsInline = false,
        scrollThreshold = 14.dp,
        inlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown,
    )

    // UI-7 (D-098) + D-105: Söhbətlər = immersiv (nav gizli, həmişə);
    // çıxışda nav AÇIQ vəziyyətdə qayıdır (yığılmış yox — tam tab dili
    // ilə qarşılama; wasChats keçidi proses ölümündən bərpa olunur).
    LaunchedEffect(index) {
        if (index == 2) {
            navHidden = true; wasChats = true
        } else {
            navHidden = false
            if (wasChats) {
                navScroll.expand()
                wasChats = false
            }
        }
    }

    // UI-7 (D-098): Söhbətlər tabında nav yoxdur — GERİ ilə Kəşf-ə qayıdış.
    // (açıq çatda ChatsScreen-in BackHandler-i bunu kölgədə qoyur)
    BackHandler(enabled = index == 2) { index = 0 }

    // D-102: adaptiv rejim — Compact = alt panel (əvvəlki axın),
    // Medium/Expanded = sol rail. navHidden yalnız alt panel üçün doğrudur.
    val widthClass = rememberWindowWidthClass()
    val rail = widthClass != WindowWidthClass.Compact

    // U2.4 (UI-11): badge state-dən törəmədir (st.unreadCount) — Header
    // zəngi + D-105 Söhbətlər dairəsi nöqtəsi eyni mənbəyi paylaşır
    val st by engine.state.collectAsStateWithLifecycle()

    // — D-107: app backdrop — şüşə pill-ın nümunə götürdüyü qat. Yalnız
    // Compact məzmun qutusu qeydə alınır (rail rejimində glass istehlakçı
    // yoxdur — qeydə alma modifier-i tətbiq olunmur, xərc sıfır). Qeydə
    // alma qutu çəkməsindən SONRA baş verir (LayerBackdropNode.draw:
    // drawContent + recordLayer) — bar özünü nümunə ala bilməz (overlay
    // Row-dan KƏNARDA qurulur).
    val appBackdrop = rememberLayerBackdrop {
        drawRect(Q.background)
        drawContent()
    }

    CompositionLocalProvider(LocalAppBackdrop provides appBackdrop) {
    Row(Modifier.fillMaxSize().background(Q.background)) {
        // — D-102: LiquidRail (yalnız geniş pəncərələrdə) —
        if (rail) {
            LiquidRail(
                index = index,
                onChanged = { index = it },
                onCreate = onOpenCreate,
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Header(
                onPartners = { nav.push(Route.Partners) },
                onAssistant = { nav.push(Route.Assistant) },
                onBell = { nav.push(Route.Notifications) },
                unread = st.unreadCount,
            )
            // D-102 + D-105: collapse/expand keçidi yalnız alt panel
            // rejimində qurulur (rail məzmunu örtmür — yığılma mənasızdır).
            // D-107: məzmun qutusu eyni zamanda glass-un geribesleme
            // qaynağıdır (layerBackdrop — ekran çəkildikdən sonra qat
            // qeydə alınır; nav pill bu qatdan nümunə götürür).
            Box(
                if (rail) Modifier.weight(1f)
                else Modifier.weight(1f).nestedScroll(navScroll)
                    .layerBackdrop(appBackdrop)
            ) {
                // D-088 keçid dili + D-102: slide artıq SPRİNG fizikasıdır
                // (spatial = spring, alpha = tween qaydası)
                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            spring(
                                dampingRatio = Motion.spatialDamping,
                                stiffness = Motion.spatialStiffness,
                                visibilityThreshold = IntOffset(8, 8),
                            )
                        ) { it / 5 * dir } +
                            fadeIn(tween(Motion.D2, easing = Motion.ease)))
                            .togetherWith(fadeOut(tween(Motion.D2)))
                    },
                    label = "tabs",
                ) { i ->
                    when (i) {
                        0 -> FeedScreen(engine, nav, focus, onFinishActivity, onOpenChat)
                        1 -> MapScreen(engine, nav, focus)
                        2 -> ChatsScreen(engine, chatFocus)
                        else -> ProfileScreen(engine, nav, onFinishActivity, onOpenChat)
                    }
                }
            }
        }
    }

    // — D-105: iOS 26 üslublu üzən alt panel (FloatingTabBar) — yalnız
    // Compact pəncərələrdə (rail rejimində ikinci naviqasiya ola bilməz)
    if (!rail) {
        Box(Modifier.fillMaxSize()) {
            FloatingNav(
                index = index,
                hidden = navHidden,
                connection = navScroll,
                unread = st.unreadCount,
                onChanged = { index = it },
                onCreate = onOpenCreate,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    } // CompositionLocalProvider (D-107 app backdrop)
}

/** .app-header — 67px, ağ, alt haşiyə (web-parity). */
@Composable
private fun Header(
    onPartners: () -> Unit,
    onAssistant: () -> Unit,
    onBell: () -> Unit,
    unread: Int,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Q.card)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(67.dp)
                .padding(horizontal = 12.dp),
        ) {
            Wordmark(fontSize = 25)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPartners) {
                // D-063: header ikonları vahid 22dp
                Icon(Icons.Filled.Handshake, "Tərəfdaşlar", tint = Q.foreground, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onAssistant) {
                Icon(Icons.Filled.SmartToy, "AI köməkçi", tint = Q.foreground, modifier = Modifier.size(22.dp))
            }
            Box {
                IconButton(onClick = onBell) {
                    Icon(Icons.Filled.NotificationsNone, "Bildirişlər", tint = Q.foreground, modifier = Modifier.size(22.dp))
                }
                if (unread > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 7.dp, end = 7.dp)
                    ) {
                        PopIn {
                            Text(
                                "$unread",
                                // U3.3 (UI-24): Q.accentFg kontrastı ≥ 4.5:1
                                color = Q.accentFg,
                                style = T.badge,
                                modifier = Modifier
                                    .background(Q.accent, CircleShape)
                                    .border(1.dp, Q.card, CircleShape)
                                    .padding(3.5.dp),
                            )
                        }
                    }
                }
            }
        }
        // alt haşiyə
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(Q.border)
        )
    }
}

/**
 * FLOATING NAV — D-105 (v0.37.0, istifadəçi direktivi 2026-11-09: „bu
 * commitdən sonrakı commitdə göndərdiyim navigation bar stilini
 * implementasiya et; yığılmış haldakı sağ və solda olan düymələr home
 * və chat düyməsi olsun“): D-098 LiquidNav TAM əvəz olunur —
 * elyesmansour/compose-floating-tab-bar v1.0.1 (Apache-2.0, vendored:
 * core/ui/floatingtabbar) iOS 26 dili ilə. D-106 (v0.38.0, istifadəçi
 * direktivi 2026-11-09: „açıq halda hamsı eyni pill içində olmalıdır,
 * bağlandıqda isə home və mesajlar öz yerlərində, ortada yox“):
 *
 *  - AÇIQ (expanded): VAHİD PILL — [Kəşf · Xəritə · Yarat · Profil ·
 *    Söhbətlər dairəsi] hamısı EYNİ həbin içində; 4 slot weight(1f)
 *    ilə bərabər, dairə həbin sağ ucunda (içində — ayrı üzgü
 *    element YOX); seçili slotun arxasında Q.secondary kapsul
 *    sürüşür (spring 380/0.72).
 *  - YIĞILMIŞ (inline): [Kəşf həbi SOL KƏNARDA] [Söhbətlər dairəsi
 *    SAĞ KƏNARDA] — mərkəzdə klaster YOX (D-106 direktivi); sol həb
 *    HƏMİŞƏ Kəşf, sağ dairə HƏMİŞƏ Söhbətlər; keçid sharedElement
 *    morfudur (həb açılır, ikonlar yerini dəyişir).
 *  - Söhbətlər dairəsi: aktiv primary / qeyri-aktiv mutedFg ikon +
 *    oxunmamış sayı üçün Q.accent nöqtə (Header zəng badge-i ilə eyni
 *    mənbə — st.unreadCount; dairə clip-i daxilində, daşma yoxdur).
 *  - Yarat: həb içində action-slot (seçim dəyişməz — D-088 dili),
 *    create sheet-i açır.
 *  - D-103 rəng dili qorunur: aktiv ikon/mətn primary + W700,
 *    qeyri-aktiv mutedFg + W500 (rəng keçidi tween D2).
 *  - Gizlənmə (Söhbətlər immersive — D-098/7): „hover“ dili dəyişməz
 *    — spring slide + fade; 14dp scroll həddi D-098-dən miras (amma
 *    artıq gizlətmir — YIĞIR; bar həmişə əlçatandır).
 *
 * D-107 (v0.39.0, direktiv 2026-11-09: „olmursa echo music reposunun
 * koduna bax və yalnız liquid glass teması olan navigation bar stilini
 * tətbiq et. indiki versiyası heç yaxşı deyil“): barın GÖRÜNÜŞÜ
 * Echo-Music liquid glass dilinə keçir (yerləşmə D-105/D-106 dili
 * DƏYİŞMİR — pill/dairə/kənarlar qalır):
 *  - glass AÇIQ (API 31+ real cihaz): backgroundColor şəffaf,
 *    tabBarContentModifier = liquidGlassNavBar(percent 50 forma — həm
 *    həb, həm dairə üçün), məzmun monoxrom (seçili tam tünd, qeyri-seçili
 *    65% alfa), seçili kapsul ağ @ 0.25, kölgə glass Shadow.Default-dan
 *    (Compose shadow 0 — ikiqə kölgə yox);
 *  - glass BAĞLI (API <31, Robolectric): D-103 solid dili qalır
 *    (Q.card fon, primary/mutedFg, Q.secondary kapsul) — reqressiya yox.
 */
@Composable
private fun FloatingNav(
    index: Int,
    hidden: Boolean,
    connection: FloatingTabBarScrollConnection,
    unread: Int,
    onChanged: (Int) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // D-107: glass gate — backdrop təmin olunubsa və cihaz dəstəkləyirsə
    val glassOn = isLiquidGlassAvailable() && LocalAppBackdrop.current != null
    val pal = if (glassOn) navGlassPalette() else navFallbackPalette()
    AnimatedVisibility(
        visible = !hidden,
        modifier = modifier,
        enter = fadeIn(tween(200, easing = Motion.ease)) +
            slideInVertically(
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
            ) { it / 2 },
        exit = fadeOut(tween(160)) +
            slideOutVertically(tween(220, easing = Motion.ease)) { it },
    ) {
        FloatingTabBar(
            selectedTabKey = index,
            scrollConnection = connection,
            // D-107: glass rejimi — Echo-Music inteqrasiya nümunəsi:
            // şəffaf fon + effect modifier (şüşə özü çəkir); solid
            // fallback D-103 dilində qalır
            colors = FloatingTabBarDefaults.colors(
                backgroundColor = if (glassOn) Color.Transparent else Q.card,
                accessoryBackgroundColor = if (glassOn) Color.Transparent else Q.card,
                selectedIndicatorColor = pal.indicator,
            ),
            tabBarContentModifier =
                if (glassOn) Modifier.liquidGlassNavBar(RoundedCornerShape(percent = 50))
                else Modifier,
            shapes = FloatingTabBarDefaults.shapes(
                tabBarShape = RoundedCornerShape(100),
                tabShape = RoundedCornerShape(100),
                standaloneTabShape = CircleShape,
                accessoryShape = RoundedCornerShape(100),
            ),
            sizes = FloatingTabBarDefaults.sizes(
                tabBarContentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                tabInlineContentPadding = PaddingValues(12.dp),
                tabExpandedContentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                componentSpacing = 8.dp,
                tabSpacing = 0.dp,
            ),
            elevations = FloatingTabBarDefaults.elevations(
                // D-107: glass rejimində Compose kölgəsi sıfır — yumşaq
                // kölgəni Shadow.Default (glass pipeline) verir
                inlineElevation = if (glassOn) 0.dp else 6.dp,
                expandedElevation = if (glassOn) 0.dp else 10.dp,
            ),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(bottom = 10.dp)
                .navigationBarsPadding()
                // D-102: adaptiv test qarmağı + a11y — alt panel identikliyi
                .semantics { contentDescription = "Alt naviqasiya paneli" },
        ) {
            // — həb: Kəşf · Xəritə · Yarat (action) · Profil —
            // (D-107: pal — glass monoxrom / D-103 solid fallback)
            tab(
                key = 0,
                title = { NavLabel("Kəşf", index == 0, pal) },
                icon = { NavIcon(Icons.Outlined.Home, Icons.Rounded.Home, index == 0, "Kəşf", pal) },
                onClick = { onChanged(0) },
            )
            tab(
                key = 1,
                title = { NavLabel("Xəritə", index == 1, pal) },
                icon = { NavIcon(Icons.Outlined.Map, Icons.Rounded.Map, index == 1, "Xəritə", pal) },
                onClick = { onChanged(1) },
            )
            tab(
                // action-slot: seçim DƏYİŞMİR (D-088 „selectable=false“ dili)
                key = "create",
                title = { NavLabel("Yarat", false, pal) },
                icon = { NavIcon(Icons.Filled.Add, Icons.Filled.Add, false, "Yarat", pal) },
                onClick = onCreate,
            )
            tab(
                key = 3,
                title = { NavLabel("Profil", index == 3, pal) },
                icon = { NavIcon(Icons.Outlined.Person, Icons.Rounded.Person, index == 3, "Profil", pal) },
                onClick = { onChanged(3) },
            )
            // — standalone dairə: Söhbətlər — AÇIQ: həbin içində sağda
            // (D-106 vahid pill); YIĞILMIŞ: sağ kənar düyməsi —
            standaloneTab(
                key = 2,
                icon = {
                    val tint by animateColorAsState(
                        targetValue = if (index == 2) pal.selected else pal.unselected,
                        animationSpec = tween(Motion.D2), label = "chatTint",
                    )
                    Box {
                        Icon(
                            if (index == 2) Icons.Rounded.ChatBubble else Icons.Outlined.ChatBubble,
                            "Söhbətlər", tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                        if (unread > 0) {
                            // nöqtə dili: dairə clip-i daxilində qalır
                            // (say rəqəmi artıq Header zəngində görünür)
                            PopIn {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .size(9.dp)
                                        .background(Q.accent, CircleShape)
                                        .border(1.dp, Q.card, CircleShape)
                                )
                            }
                        }
                    }
                },
                onClick = { onChanged(2) },
            )
        }
    }
}

/**
 * D-105: həb etiketi — D-103 rəng dili (aktiv primary/W700, qeyri-aktiv
 * mutedFg/W500); D-107: [pal] glass monoxromu (seçili tam tünd / 65% alfa)
 * və ya fallback D-103 dili.
 */
@Composable
private fun NavLabel(label: String, active: Boolean, pal: NavPalette) {
    Text(
        label,
        style = T.metaSm.copy(
            fontWeight = if (active) FontWeight.W700 else FontWeight.W500,
            color = if (active) pal.selected else pal.unselected,
        ),
        maxLines = 1,
    )
}

/** D-105: həb ikonu — aktiv/qeyri-aktiv variant + rəng keçidi (tween D2); D-107: [pal]. */
@Composable
private fun NavIcon(
    inactive: ImageVector,
    active: ImageVector,
    isActive: Boolean,
    desc: String,
    pal: NavPalette,
) {
    val tint by animateColorAsState(
        targetValue = if (isActive) pal.selected else pal.unselected,
        animationSpec = tween(Motion.D2), label = "navTint",
    )
    Icon(
        if (isActive) active else inactive,
        desc,
        tint = tint,
        modifier = Modifier.size(20.dp),
    )
}


/**
 * LIQUID RAIL — D-102 (v0.35.0): geniş pəncərələrdə (≥600dp) sol şaquli
 * naviqasiya dəmiryolu. D-105-dən sonra alt panel FloatingTabBar-dır —
 * rail şüşə nav dilinin (D-098/D-103: 0.35 intensity səth, rəng-seçim
 * dili — lentz kapsulu yoxdur, "Yarat" qradiyenti) yeganə daşıyıcısıdır;
 * slot sırası əvvəlki kimidir: Kəşf · Xəritə · [Yarat] · Söhbətlər ·
 * Profil. Planşet/qatlanan istifadəsində baş barmaq zonada qalır,
 * məzmun tam eni qazanır (alt panelin 82dp-i qaytarılır).
 *
 * Hündürlük: tam ekran (statusBar daxil — edge-to-edge D-031 dili);
 * en: 84dp (4 slot + FAB şaquli düzümdə rahat toxunma hədəfi 48dp+).
 */
@Composable
private fun LiquidRail(
    index: Int,
    onChanged: (Int) -> Unit,
    onCreate: () -> Unit,
) {
    data class Item(val icon: ImageVector, val activeIcon: ImageVector, val label: String)
    val items = listOf(
        Item(Icons.Outlined.Home, Icons.Rounded.Home, "Kəşf"),
        Item(Icons.Outlined.Map, Icons.Rounded.Map, "Xəritə"),
        Item(Icons.Outlined.ChatBubble, Icons.Rounded.ChatBubble, "Söhbətlər"),
        Item(Icons.Outlined.Person, Icons.Rounded.Person, "Profil"),
    )

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxHeight()
            .width(84.dp)
            // D-102: adaptiv test qarmağı + a11y — rail identikliyi
            .semantics { contentDescription = "Yan naviqasiya paneli" },
        shape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp),
        intensity = 0.35f,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 14.dp),
        ) {
            // — slotlar: Kəşf, Xəritə, [Yarat], Söhbətlər, Profil —
            // (D-103: fon kapsulu yoxdur — yalnız rəng dili)
            items.forEachIndexed { i, item ->
                if (i == 2) {
                    Spacer(Modifier.height(14.dp))
                    CreateFab(onCreate)
                    Spacer(Modifier.height(14.dp))
                }
                LiquidNavItem(
                    icon = item.icon,
                    activeIcon = item.activeIcon,
                    label = item.label,
                    active = index == i,
                    onClick = { onChanged(i) },
                    modifier = Modifier.width(64.dp),
                )
            }
        }
    }
}

/**
 * "Yarat" — UI-7 (D-098): marka qradiyentli (primary→primaryDeep)
 * 42dp dairə, parlaq yaşıl kölgə, etiket altda. D-088 qərarı:
 * barın İÇİNDƏ, daşma YOX.
 */
@Composable
private fun CreateFab(onCreate: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onCreate)
            .padding(horizontal = 6.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .shadow(
                    9.dp, CircleShape,
                    ambientColor = Q.primary.copy(alpha = 0.32f),
                    spotColor = Q.primary.copy(alpha = 0.38f),
                )
                .background(
                    Brush.linearGradient(listOf(Q.primary, Q.primaryDeep)),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "Yarat", tint = Q.primaryFg, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("Yarat", style = T.metaSm.copy(fontWeight = FontWeight.W600))
    }
}

/**
 * Alt nav item — UI-7 (D-098) + D-103: item özü NEYTRALDIR — fon tinti
 * və arxa kapsul YOXDUR; seçim YALNIZ rəng dili ilə ifadə olunur
 * (aktiv ikon/mətn primary + ikon 2dp aşağı enir; qeyri-aktiv mutedFg).
 */
@Composable
private fun LiquidNavItem(
    icon: ImageVector,
    activeIcon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    // D-102: dip artıq spring (mikro bounce — spatial=spring qaydası;
    // tween 150ms qopuq gəlirdi, spring toxunuşa "cavab" hissi verir)
    val dip by animateDpAsState(
        targetValue = if (active) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Motion.dipDamping,
            stiffness = Motion.dipStiffness,
        ),
        label = "navIconDip",
    )
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) Q.primary else Q.mutedFg,
        animationSpec = tween(Motion.D2), label = "navColor",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Icon(
            if (active) activeIcon else icon, label, tint = tint,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer { translationY = dip.toPx() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = T.metaSm.copy(
                fontWeight = if (active) FontWeight.W700 else FontWeight.W500,
                color = if (active) Q.primary else Q.mutedFg,
            ),
        )
    }
}
