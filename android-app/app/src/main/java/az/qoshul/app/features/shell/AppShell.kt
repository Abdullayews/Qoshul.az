package az.qoshul.app.features.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import az.qoshul.app.core.model.QoshulEvent

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
 * D-102 (v0.35.0): ADAPTİV NAVİQASİYA — pəncərə eni ≥600dp (Medium/
 * Expanded — planşet, qatlanan açıq) alt LiquidNav əvəzinə solda
 * şaquli LiquidRail; Compact (telefon) davranışı BİT-BİT eyni qalır.
 * Rail rejimində auto-hide bağlanır (rail məzmunu örtmür, gizlənmə
 * mənasızdır); tab keçidləri tween əvəzinə spring fizikasına keçir.
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

    // "Yol göstər / Yol tap" → xəritə tabına keç (D-050)
    LaunchedEffect(focus.request) {
        if (focus.request != null) index = 1
    }

    // "Qrup söhbətinə bax / Söhbətə keç" → Söhbətlər tabına keç (D-055)
    LaunchedEffect(chatFocus?.request) {
        if (chatFocus?.request != null) index = 2
    }

    // UI-7 (D-098): tab dəyişəndə nav vəziyyəti — Söhbətlər = həmişə
    // gizli (daxili keçidlər də daxil), digər tablara qayıdışda görünür.
    LaunchedEffect(index) { navHidden = index == 2 }

    // UI-7 (D-098): Söhbətlər tabında nav yoxdur — GERİ ilə Kəşf-ə qayıdış.
    // (açıq çatda ChatsScreen-in BackHandler-i bunu kölgədə qoyur)
    BackHandler(enabled = index == 2) { index = 0 }

    // UI-7 (D-098): scroll əsaslı auto-hide — qabıq səviyyəli NestedScroll
    // connection: aşağı delta (dy<0) ~14dp toplananda gizlət, yuxarı delta
    // (dy>0) toplananda göstər. İstiqamət dəyişəndə akkumulyator sıfırlanır
    // (cavabvericilik). Söhbətlər tabında siqnal iqnor edilir.
    val density = LocalDensity.current
    val scrollAcc = remember { mutableFloatStateOf(0f) }
    val scrollConnection = remember(density) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (index == 2 || dy == 0f) return Offset.Zero
                if (scrollAcc.floatValue != 0f && (scrollAcc.floatValue > 0f) != (dy > 0f)) {
                    scrollAcc.floatValue = 0f
                }
                scrollAcc.floatValue += dy
                val threshold = with(density) { 14.dp.toPx() }
                when {
                    scrollAcc.floatValue <= -threshold -> {
                        navHidden = true; scrollAcc.floatValue = 0f
                    }
                    scrollAcc.floatValue >= threshold -> {
                        navHidden = false; scrollAcc.floatValue = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    // D-102: adaptiv rejim — Compact = alt panel (əvvəlki axın),
    // Medium/Expanded = sol rail. navHidden yalnız alt panel üçün doğrudur.
    val widthClass = rememberWindowWidthClass()
    val rail = widthClass != WindowWidthClass.Compact

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
            // U2.4 (UI-11): badge state-dən törəmədir (st.unreadCount)
            val st by engine.state.collectAsStateWithLifecycle()
            Header(
                onPartners = { nav.push(Route.Partners) },
                onAssistant = { nav.push(Route.Assistant) },
                onBell = { nav.push(Route.Notifications) },
                unread = st.unreadCount,
            )
            // D-102: auto-hide yalnız alt panel rejimində qurulur (rail
            // məzmunu örtmür — gizlətmə mənasızdır); Compact davranışı
            // dəyişmir: eyni connection, eyni hədd.
            Box(
                if (rail) Modifier.weight(1f)
                else Modifier.weight(1f).nestedScroll(scrollConnection)
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

    // — Liquid glass alt panel (UI-7 D-098: sıfırdan yenidən yazılıb) —
    // D-102: yalnız Compact pəncərələrdə (rail rejimində ikinci naviqasiya
    // ola bilməz)
    if (!rail) {
        Box(Modifier.fillMaxSize()) {
            LiquidNav(
                index = index,
                hidden = navHidden,
                onChanged = { index = it },
                onCreate = onOpenCreate,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
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
 * LIQUID NAV — UI-7 (D-098, direktiv 2026-11-08: "Navigation bar tam
 * sıfırdan yaz, liquid glass stilində"). D-088-in qfw pill dili TAM
 * əvəz olunur; ölçü dili (72dp panel, 5 slot, "Yarat" mərkəzdə) qalır.
 *
 * D-103 (2026-11-09, istifadəçi direktivi: "navigation tab düymələrinin
 * ətrafındakı dairəni sil"): aktiv slotun arxasındakı lentz kapsulu
 * SİLİNDİ — seçim artıq YALNIZ rəng dili ilə ifadə olunur (aktiv ikon/
 * mətn primary + 2dp dip; qeyri-aktiv mutedFg). Glass.kt-in phase-
 * lensing qatına (həqiqi linza effekti) toxunulmur — bu, bəzək kapsulu
 * idi, riyazi lens deyil.
 *
 * Dizayn — Glass.kt-in liquid-glass prinsipləri üzərində:
 *  1) SƏTH: gerçek şüşə intensivliyi (intensity 0.35 — D-060-ın
 *     "0.1x" rejimindən güclü, amma məzmun barın içindən oxunmur);
 *     30dp radius (D-098 dairəviləşdirmə), yumşaq kölgə.
 *  2) YARAT: marka qradiyenti (primary→primaryDeep) + parlaq kölgəli
 *     dairə — barın İÇİNDƏ, daşma yox (D-088 qərarı qorunur).
 *  3) HƏRƏKƏT: gizlənmə/göstərmə — "hover" (spring slide + fade,
 *     aşağıdan yarı-hündürlüklə qayıdış); ikon 2dp dip.
 */
@Composable
private fun LiquidNav(
    index: Int,
    hidden: Boolean,
    onChanged: (Int) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Item(val icon: ImageVector, val activeIcon: ImageVector, val label: String)
    val items = listOf(
        Item(Icons.Outlined.Home, Icons.Rounded.Home, "Kəşf"),
        Item(Icons.Outlined.Map, Icons.Rounded.Map, "Xəritə"),
        Item(Icons.Outlined.ChatBubble, Icons.Rounded.ChatBubble, "Söhbətlər"),
        Item(Icons.Outlined.Person, Icons.Rounded.Person, "Profil"),
    )

    // — hover hərəkəti: spring ilə aşağı süüşmə + fade —
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
        LiquidGlassSurface(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(bottom = 10.dp)
                .navigationBarsPadding()
                // D-102: adaptiv test qarmağı + a11y — alt panel identiklik
                .semantics { contentDescription = "Alt naviqasiya paneli" },
            shape = RoundedCornerShape(30.dp),
            intensity = 0.35f,
        ) {
            Box(Modifier.height(72.dp)) {
                // — slotlar — (D-103: fon kapsulu yoxdur — yalnız rəng dili) —
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                ) {
                    items.forEachIndexed { i, item ->
                        if (i == 2) CreateFab(onCreate)  // action: mərkəz
                        LiquidNavItem(
                            icon = item.icon,
                            activeIcon = item.activeIcon,
                            label = item.label,
                            active = index == i,
                            onClick = { onChanged(i) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * LIQUID RAIL — D-102 (v0.35.0): geniş pəncərələrdə (≥600dp) sol şaquli
 * naviqasiya dəmiryolu. Alt LiquidNav-in dili DAŞINIR (eyni şüşə səthi
 * 0.35, eyni rəng-seçim dili — D-103: lentz kapsulu burada da silinib,
 * eyni "Yarat" qradiyenti, eyni slot sırası: Kəşf · Xəritə · [Yarat] ·
 * Söhbətlər · Profil) — yalnız istiqamət dəyişir. Planşet/qatlanan
 * istifadəsində baş barmaq zonada qalır, məzmun tam eni qazanır (alt
 * panelin 82dp-i qaytarılır).
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
