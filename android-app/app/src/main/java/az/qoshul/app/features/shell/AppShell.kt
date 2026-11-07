package az.qoshul.app.features.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.ChatFocusState
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.Wordmark
import az.qoshul.app.core.model.QoshulEvent
import kotlin.math.roundToInt

/**
 * Əsas tətbiq qabığı — Flutter app_shell.dart portu (ROL C + D-034):
 * 4 tab alt paneli: Kəşf · Xəritə · [Yarat] · Söhbətlər · Profil.
 * Kəşf = ana ekran; içində "Sənin üçün | Tədbirlər" daxili tabları.
 * Edge-to-edge (D-031): məzmun şəffaf barların altından axır.
 *
 * D-050: [focus] — xəritə fokus istəyi gələndə avtomatik Xəritə tabına
 * keçilir ("Yol göstər"/"Yol tap" düymələri, MapFocusState).
 * D-055: [chatFocus] — söhbət fokusunda Söhbətlər tabına keçid;
 * onFinishActivity/onOpenChat — Feed və Profile ekranlarına ötürülür.
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
    var index by remember { mutableStateOf(0) }

    // "Yol göstər / Yol tap" → xəritə tabına keç (D-050)
    LaunchedEffect(focus.request) {
        if (focus.request != null) index = 1
    }

    // "Qrup söhbətinə bax / Söhbətə keç" → Söhbətlər tabına keç (D-055)
    LaunchedEffect(chatFocus?.request) {
        if (chatFocus?.request != null) index = 2
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Q.background)
            .statusBarsPadding()
    ) {
        Header(
            onPartners = { nav.push(Route.Partners) },
            onAssistant = { nav.push(Route.Assistant) },
            onBell = { nav.push(Route.Notifications) },
            unread = engine.unreadCount,
        )
        Box(Modifier.weight(1f)) {
            // D-088: GD3/qfw səhifə keçidi — istiqamətli slide + fade
            // (Ghost-Downloader-3 qfw_patch PopUpAniStackedWidget dili, 250ms)
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(Motion.D2, easing = Motion.ease)) { it / 5 * dir } +
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

    // — Şüşə alt pill — üzən funksional qat (D-025 səth #1) —
    Box(Modifier.fillMaxSize()) {
        GlassNav(
            index = index,
            onChanged = { index = it },
            onCreate = onOpenCreate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
        )
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
                // D-063: header ikonları vahid 22dp (əvvəl 21/22/22 qarışıq)
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
                                color = Color.White,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 8.5.sp,
                                    fontWeight = FontWeight.W800,
                                ),
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
 * Şüşə alt panel — D-025 səth #1; D-037 vizual; D-060 "0.1x liquid glass":
 * fon demək olar opaq (şəffaflıq istifadəçi direktivi ilə geri çəkildi),
 * şüşə aksentləri (spekulyar kənar + işıq qradiyenti + yumşaq kölgə)
 * yalnız 10% gücdə. Ölçülər dəyişməz (68dp hündürlük, 24dp radius).
 *
 * D-088 (M6, 2026-11-07): Ghost-Downloader-3 nav SİSTEMİNİN tam
 * inteqrasiyası (istifadəçi direktivi; D-064 "sadələşdirilmiş variant"
 * əvəz olunur). Mənbə: XiaoYouChR/Ghost-Downloader-3 (GPL-3)
 * app/view/windows/main_window.py — MSFluentWindow + qfluentwidgets
 * NavigationInterface. Yalnız DİZAYN/HƏRƏKƏT DİLİ götürülüb, kod
 * köçürülməyib (lisenziya qeydi — decisions.md D-088):
 *   - qrup semantikası: TOP = Kəşf·Xəritə·Söhbətlər (addItem
 *     position=TOP), BOTTOM = Profil (Ghost-da SettingPage bu
 *     mövqedədir — ən sağda);
 *   - "Yarat" = selectable=False ACTION (Ghost 『新建任务』 ADD):
 *     barın İÇINDƏ, daşma YOX (əvvəlki -5dp poke ləğv edildi);
 *   - pill: 18×4dp Q.primary, panelin ALT kənarına YAPIŞIQ
 *     (əvvəl -5dp üzürdü — "yerləşdirmə səhvdir" şikayəti), 250ms
 *     easeOutCubic, slot mərkəzləri layout-dan ölçülür;
 *   - aktiv item tint: NEYTRAL 6% (WinUI NavView seçim dili;
 *     əvvəl primary 10% idi) — aksent ikon/mətn + pill-də qalır;
 *   - aktiv ikon ~3dp aşağı enir (qfw iconAni, D-064-dən qalır).
 */
@Composable
private fun GlassNav(
    index: Int,
    onChanged: (Int) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Item(val icon: ImageVector, val activeIcon: ImageVector, val label: String)
    // qfw NavigationItemPosition: TOP qrupu (0..2) + BOTTOM (Profil —
    // Ghost SettingPage analogu, ən sağda)
    val items = listOf(
        Item(Icons.Outlined.Home, Icons.Rounded.Home, "Kəşf"),
        Item(Icons.Outlined.Map, Icons.Rounded.Map, "Xəritə"),
        Item(Icons.Outlined.ChatBubble, Icons.Rounded.ChatBubble, "Söhbətlər"),
        Item(Icons.Outlined.Person, Icons.Rounded.Person, "Profil"),
    )

    // Slot mərkəzləri nav koordinatlarında (px) — sürüşən indikator üçün.
    val slotCenters = remember { mutableStateListOf(0f, 0f, 0f, 0f) }
    val measured = slotCenters.all { it > 0f }

    LiquidGlassSurface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        intensity = 0.1f,
    ) {
        Box(Modifier.height(68.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
            ) {
                items.forEachIndexed { i, item ->
                    if (i == 2) CreateButton(onCreate)  // action: TOP/BOTTOM arasında (mərkəz)
                    NavItem(
                        icon = item.icon,
                        activeIcon = item.activeIcon,
                        label = item.label,
                        active = index == i,
                        onClick = { onChanged(i) },
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { coords ->
                                val cx = coords.boundsInParent().center.x
                                if (slotCenters[i] != cx) slotCenters[i] = cx
                            },
                    )
                }
            }
            // GD3/qfw pill — ALT kənara yapışıq (qfw panelində pill kənar
            // xəttində durur). İlk ölçmədən sonra görünür.
            if (measured) {
                val x by animateFloatAsState(
                    targetValue = slotCenters[index],
                    animationSpec = tween(Motion.D2, easing = Motion.ease),
                    label = "navIndicator",
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset {
                            IntOffset(
                                (x - 9.dp.toPx()).roundToInt(),
                                -1.dp.roundToPx(),
                            )
                        }
                        .size(width = 18.dp, height = 4.dp)
                        .background(Q.primary, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/**
 * "Yarat" — D-088: qfw selectable=False ACTION item (Ghost 『新建任务』
 * FluentIcon.ADD): barın İÇINDƏ, daşma YOX (əvvəlki -5dp poke və 47dp
 * üzən dairə ləğv edildi — "yerləşdirmə səhvdir" şikayəti). 36dp yumşaq
 * kafel (12dp radius, D-058 dili), primary + yumşaq kölgə, etiket altda.
 */
@Composable
private fun CreateButton(onCreate: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onCreate)
            .padding(horizontal = 8.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .shadowNav()
                .background(Q.primary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "Yarat", tint = Q.primaryFg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "Yarat",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 10.5.sp, color = Q.mutedFg,
                fontWeight = FontWeight.W600,
            ),
        )
    }
}

private fun Modifier.androidxNavBg(color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.then(Modifier.background(color, shape))

private fun Modifier.shadowNav(): Modifier = this.then(
    Modifier.shadow(
        elevation = 6.dp,
        shape = RoundedCornerShape(12.dp),
        ambientColor = Q.primary.copy(alpha = 0.28f),
        spotColor = Q.primary.copy(alpha = 0.28f),
    )
)

/**
 * Alt nav item — D-088: qfw NavigationInterface seçim dili: NEYTRAL 6%
 * tint kapsul (WinUI NavView; əvvəl primary 10% idi) + aksent ikon/mətn;
 * aktiv ikon ~3dp AŞAĞI enir (iconAni, D-064-dən); pill nav səviyyəsində
 * SÜRÜŞƏN indikator kimi qalır (item-altı nöqtə YOX).
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    activeIcon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val dip by animateDpAsState(
        targetValue = if (active) 3.dp else 0.dp,
        animationSpec = tween(Motion.D1, easing = Motion.ease),
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
            .androidxNavBg(
                if (active) Q.foreground.copy(alpha = 0.06f) else Color.Transparent,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Icon(
            if (active) activeIcon else icon, label, tint = tint,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer { translationY = dip.toPx() },
        )
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 10.5.sp,
                fontWeight = if (active) FontWeight.W700 else FontWeight.W400,
                color = if (active) Q.primary else Q.mutedFg,
            ),
        )
    }
}
