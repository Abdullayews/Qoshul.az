package az.qoshul.app.features.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.features.chats.ChatsScreen
import az.qoshul.app.features.feed.FeedScreen
import az.qoshul.app.features.map.MapScreen
import az.qoshul.app.features.profile.ProfileScreen
import az.qoshul.app.core.glass.GlassSurface
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
import androidx.compose.runtime.LaunchedEffect
import az.qoshul.app.core.model.QoshulEvent

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
            // Fade-through tab keçidi (D-025)
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (fadeIn(tween(Motion.D2, easing = Motion.ease)) +
                        scaleIn(initialScale = 0.94f, animationSpec = tween(Motion.D2, easing = Motion.ease)))
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
                Icon(Icons.Filled.Handshake, "Tərəfdaşlar", tint = Q.foreground, modifier = Modifier.size(21.dp))
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

/** Şüşə alt panel — Liquid Glass üzən pill (D-025 səth #1, D-037 vizual). */
@Composable
private fun GlassNav(
    index: Int,
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

    GlassSurface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        sigma = 18.dp,
        luminance = 0.52f,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(68.dp)
                .padding(horizontal = 6.dp),
        ) {
            items.forEachIndexed { i, item ->
                if (i == 2) CreateButton(onCreate)
                NavItem(
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

/** Mərkəzi "Yarat" düyməsi (D-034) — üstə qalxan yaşıl dairə. */
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
                .graphicsLayer { translationY = -5.dp.toPx() }
                .size(47.dp)
                .shadowNav()
                .background(Q.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "Yarat", tint = Q.primaryFg, modifier = Modifier.size(24.dp))
        }
        Text(
            "Yarat",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 10.5.sp, color = Q.mutedFg
            ),
            modifier = Modifier.graphicsLayer { translationY = -1.dp.toPx() },
        )
    }
}

private fun Modifier.androidxNavBg(color: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.then(Modifier.background(color, shape))

private fun Modifier.shadowNav(): Modifier = this.then(
    Modifier.shadow(
        elevation = 6.dp,
        shape = CircleShape,
        ambientColor = Q.primary.copy(alpha = 0.28f),
        spotColor = Q.primary.copy(alpha = 0.28f),
    )
)

/** Alt nav item — aktiv: yaşıl kapsul (10%) + filled ikon bounce + nöqtə (D-033). */
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
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = tween(Motion.D2, easing = if (active) Motion.enter else Motion.ease),
        label = "navIcon",
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
                if (active) Q.primary.copy(alpha = 0.10f) else Color.Transparent,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Icon(
            if (active) activeIcon else icon, label, tint = tint,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Spacer(Modifier.height(5.dp))
        Box {
            Text(
                label,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 10.5.sp,
                    fontWeight = if (active) FontWeight.W700 else FontWeight.W400,
                    color = if (active) Q.primary else Q.mutedFg,
                ),
            )
            // aktiv nöqtə göstəricisi
            val dot by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (active) 4.dp else 0.dp,
                animationSpec = tween(Motion.D1, easing = Motion.ease),
                label = "navDot",
            )
            if (dot > 0.dp) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { translationY = 5.dp.toPx() }
                        .size(dot)
                        .background(Q.primary, CircleShape)
                )
            }
        }
    }
}
