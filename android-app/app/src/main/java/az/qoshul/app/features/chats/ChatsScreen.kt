package az.qoshul.app.features.chats

import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.ui.ChatFocusState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.reportReasons
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.motion.TypingDots
import az.qoshul.app.core.motion.pressable
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.ChatBubble
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.ViewHeading
import az.qoshul.app.core.ui.imagePaint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Söhbətlər — Flutter chats_screen.dart portu (ROL TAB 4):
 * "Şəxsi" (match/dost) + "Qruplar" (tədbir); report / blok / sil.
 * D-055: [chatFocus] — EventDetail "Qrup söhbətinə bax" və Match
 * "Söhbətə keç" düymələri konkret söhbəti açır; "Report et" menyu
 * elementi artıq real dialoqdur (əvvəl heç nə etmirdi).
 * D-056: şəxsi söhbətlərdə tez cavab çipləri + avtomatik cavab (engine).
 */
@Composable
fun ChatsScreen(engine: AppEngine, chatFocus: ChatFocusState? = null) {
    // U2.2 (UI-02): proses ölümündə açıq söhbət bərpa olunur
    var activeEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFriendId by rememberSaveable { mutableStateOf<String?>(null) }
    var reportFor by remember { mutableStateOf<Person?>(null) }
    val st by engine.state.collectAsStateWithLifecycle()

    // D-055: ChatFocusState istəyi → konkret söhbəti aç
    LaunchedEffect(chatFocus?.request) {
        when (val r = chatFocus?.request) {
            is ChatFocusState.Target.Event -> {
                activeEventId = r.eventId; activeFriendId = null; chatFocus.consume()
            }
            is ChatFocusState.Target.Friend -> {
                activeFriendId = r.personId; activeEventId = null; chatFocus.consume()
            }
            null -> Unit
        }
    }

    val activeEvent = activeEventId?.let { engine.eventById(it) }
    val activeFriend = activeFriendId?.let { engine.personById(it) }

    // U1.1 (UI-03): açıq söhbətdə sistem GERİ app-i yox, söhbəti bağlayır
    // (əvvəl: tab rejimində back tətbiqi BAĞLAYIRDI). Mənbə: audit 2026-11-07,
    // ChatsScreen.kt:116-125; NIA pattern — daxili kontekst üçün BackHandler.
    BackHandler(enabled = activeEventId != null || activeFriendId != null) {
        activeEventId = null
        activeFriendId = null
    }

    if (activeEvent != null) {
        ChatView(
            title = activeEvent.title,
            subtitle = "${activeEvent.participants} iştirakçı",
            imageKey = activeEvent.image,
            messages = st.eventMessages[activeEvent.id] ?: emptyList(),
            onSend = { engine.sendEventMessage(activeEvent.id, it) },
            onBack = { activeEventId = null },
        )
    } else if (activeFriend != null) {
        ChatView(
            title = activeFriend.fullName,
            subtitle = "Şəxsi söhbət",
            imageKey = activeFriend.image,
            messages = st.friendMessages[activeFriend.id] ?: emptyList(),
            onSend = { engine.sendFriendMessage(activeFriend.id, it) },
            onBack = { activeFriendId = null },
            friendActions = true,
            // D-056: Instagram DM pariteti — tez cavab çipləri
            quickReplies = listOf("Salam!", "Necəsən?", "Hara görüşək?"),
            onBlock = {
                engine.blockUser(activeFriend.id)
                activeFriendId = null
            },
            onDelete = {
                engine.deleteChat(activeFriend.id)
                activeFriendId = null
            },
            onReport = { reportFor = activeFriend },
        )
    } else {
        ChatList(engine,
            onOpenEvent = { id -> activeEventId = id },
            onOpenFriend = { id -> activeFriendId = id },
        )
    }

    // — report dialoqu (D-055: menyu elementi əvvəl no-op idi) —
    reportFor?.let { p ->
        var reason by remember { mutableStateOf(reportReasons.first()) }
        AlertDialog(
            onDismissRequest = { reportFor = null },
            title = { Text("Report et") },
            text = {
                Column {
                    Text("${p.fullName} haqqında bildiriş. Səbəb seç:", style = T.subtitle)
                    Spacer(Modifier.height(8.dp))
                    reportReasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = reason == r, onClick = { reason = r },
                                colors = RadioButtonDefaults.colors(selectedColor = Q.primaryText),
                            )
                            Text(r, style = T.bodyXs)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    engine.reportUser(p.id, reason)
                    reportFor = null
                    activeFriendId = null
                }) { Text("Göndər") }
            },
            dismissButton = {
                TextButton(onClick = { reportFor = null }) { Text("İmtina") }
            },
        )
    }
}

@Composable
private fun ChatList(
    engine: AppEngine,
    onOpenEvent: (String) -> Unit,
    onOpenFriend: (String) -> Unit,
) {
    val st by engine.state.collectAsStateWithLifecycle()
    val joinedEvents = st.events.filter { it.id in st.joined }
    val friends = engine.acceptedFriends

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            // UI-7 (D-098): 132→24dp — Söhbətlər tabında nav həmişə GİZLİDİR,
            // ona görə nav üçün rezerv alt padding artıq lazım deyil
            // (D-063 vahid 132dp qaydası yalnız nav-li tablara aiddir)
            start = 20.dp, end = 20.dp, top = 21.dp, bottom = 24.dp
        ),
    ) {
        item { ViewHeading(title = "Söhbətlər", subtitle = "Görüşdən əvvəl tanış olaq.") }
        // D-115 (direktiv: „chat menyusunda yeni söhbətlər burda açılır
        // yerini sil“): boş vəziyyət placeholder-i LƏĞV — siyahı boşdursa
        // heç nə göstərilir (başlıq qalır).
        if (joinedEvents.isNotEmpty() || friends.isNotEmpty()) {
            if (joinedEvents.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = "Qruplar", icon = Icons.Filled.Groups,
                        // D-056: joinedUpcomingCount engine-də hesablanırdı, heç yerdə göstərilmirdi
                        trailing = {
                            if (engine.joinedUpcomingCount > 0) {
                                Text("${engine.joinedUpcomingCount} yaxınlaşan", style = T.chipXs.copy(fontWeight = FontWeight.W600, color = Q.mutedFg))
                            }
                        },
                    )
                }
                items(joinedEvents, key = { it.id }) { e ->
                    StaggerIn(0) {
                        ChatRow(
                            imageKey = e.image, title = e.title,
                            subtitle = "${e.participants} iştirakçı · Tədbir qrupu" +
                                if (e.past) " · keçib" else "",
                            onClick = { onOpenEvent(e.id) },
                        )
                    }
                }
            }
            if (friends.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeading(title = "Şəxsi", icon = Icons.Filled.Person)
                }
                items(friends, key = { it.id }) { p ->
                    StaggerIn(0) {
                        ChatRow(
                            imageKey = p.image, title = p.fullName,
                            subtitle = "${p.city} · " +
                                (if (p.id in st.matches) "Match" else "Dost") + " · Şəxsi",
                            onClick = { onOpenFriend(p.id) },
                        )
                    }
                }
            }
            // D-132 (direktiv 2026-11-11: „chatlar menyusunda yeni söhbətlər
            // burda açılır barını sil“): D-063 izahat banneri LƏĞV — siyahı
            // artıq yalnız real söhbətlərdən ibarətdir.
        }
    }
}

@Composable
private fun ChatRow(imageKey: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .androidxChatTap(onClick)
            // D-063: sətir padding 18→12dp — sıx, müasir söhbət siyahısı ritmi
            // (vizual audit: "rows quite tall ~24dp+ padding — sparse")
            .padding(vertical = 12.dp),
    ) {
        Image(
            painter = imagePaint(imageKey), contentDescription = title,
            contentScale = ContentScale.Crop,
            // D-063: 58→56dp (8dp-grid)
            modifier = Modifier.size(56.dp).androidxChatClip(),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = T.h3)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = T.bodySm.copy(color = Q.mutedFg))
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Q.mutedFg, modifier = Modifier.size(18.dp))
    }
}

/** Tam çat görünüşü — başlıq + mesajlar + "yazır..." + yazma zolağı. */
@Composable
private fun ChatView(
    title: String,
    subtitle: String,
    imageKey: String,
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    friendActions: Boolean = false,
    quickReplies: List<String> = emptyList(),
    onBlock: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReport: () -> Unit = {},
) {
    // U2.2 (UI-02): yazılmamış mesaj qaralaması proses ölümündən sağ qalır
    var text by rememberSaveable { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    /** Göndər + "yazır..." simulyasiyası — düymə və tez-cavab çipi üçün ortaq. */
    fun sendAction(body: String) {
        if (body.isBlank()) return
        onSend(body.trim())
        text = ""
        typing = true
        scope.launch {
            delay(1400); typing = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        // — başlıq —
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Q.card)
                .androidxChatHeaderBorder()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Geri", tint = Q.primaryText, modifier = Modifier.size(22.dp))
            }
            Image(
                painter = imagePaint(imageKey), contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).androidxChatClip2(),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = T.h3)
                Text(subtitle, style = T.metaSm)
                if (typing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypingDots(color = Q.primaryText, size = 4f)
                        Spacer(Modifier.width(5.dp))
                        Text("yazır...", style = T.metaSm.copy(color = Q.primaryText))
                    }
                }
            }
            if (friendActions) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, "Menyu", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Report et") },
                            onClick = { menuOpen = false; onReport() },
                        )
                        DropdownMenuItem(
                            text = { Text("Blokla") },
                            onClick = { menuOpen = false; onBlock() },
                        )
                        DropdownMenuItem(
                            text = { Text("Söhbəti sil") },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
        // — mesajlar —
        Box(Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Text("Hələ mesaj yoxdur. İlk mesajı sən yaz!", style = T.subtitle,
                    modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp, vertical = 8.dp
                    ),
                ) {
                    // U2.1 (UI-01): ChatMessage-də id yoxdur — append-only axın;
                    // index-prefix kompozit açar (AssistantScreen ilə eyni qayda)
                    itemsIndexed(messages, key = { i, m -> "$i-${m.body.hashCode()}" }) { i, m ->
                        StaggerIn(i.coerceIn(0, 8)) {
                            ChatBubble(
                                sender = m.sender, body = m.body, mine = m.mine,
                                status = if (m.mine && i == messages.size - 1 && typing)
                                    "çatdırıldı" else if (m.mine) "oxundu" else null,
                            )
                        }
                    }
                }
            }
        }
        // — tez cavablar + yazma zolağı (D-056; UI-7 D-098 restayl:
        // "mesaj yazma blokunu yaxşılaşdır") — yumşaq qalxan səth:
        // 24dp yuxarı radius + kölgə; kapsul input; dairəvi göndər —
        // tez cavab çipləri yaşıl secondary dilinə keçdi —
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(
                    12.dp,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    ambientColor = Q.overlayDark.copy(alpha = 0.06f),
                    spotColor = Q.overlayDark.copy(alpha = 0.10f),
                )
                .background(
                    Q.card,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .androidxComposerBorder()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (text.isBlank() && quickReplies.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    quickReplies.forEach { qr ->
                        Text(
                            qr,
                            style = T.label.copy(color = Q.primaryText),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .androidxChatTap { sendAction(qr) }
                                .background(Q.secondary, RoundedCornerShape(Q.rPill))
                                .border(
                                    1.dp,
                                    Q.primary.copy(alpha = 0.25f),
                                    RoundedCornerShape(Q.rPill),
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
            ) {
                ComposerField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                SendFab(enabled = text.isNotBlank(), onClick = { sendAction(text) })
            }
        }
    }
}

/**
 * UI-7 (D-098): mesaj yazma sahəsi — tam-yuvarlaq kapsul; toxunulmamış
 * halda zəif Q.muted fon (sərhədsiz), fokusda ağ fon + yaşıl kontur.
 * SearchBarField (D-029) dilinin çat variantı; hint boş sahədə görünür.
 */
@Composable
private fun ComposerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = T.input.copy(color = Q.foreground),
        cursorBrush = SolidColor(Q.primaryText),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) Q.card else Q.muted, RoundedCornerShape(Q.rPill))
            .border(
                if (focused) 1.4.dp else 1.dp,
                if (focused) Q.primaryText else Color.Transparent,
                RoundedCornerShape(Q.rPill),
            ),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 18.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Bir mesaj yaz...", style = T.input.copy(color = Q.mutedFg))
                    }
                    inner()
                }
            }
        },
    )
}

/**
 * UI-7 (D-098): göndər düyməsi — 48dp dairə; mətn yazılanda marka
 * rənginə keçir (rəng + scale animasiyası) və parlaq yaşıl kölgə alır,
 * boş olanda solğun kiçikləşir. Material Button əvəzinə öz səthi.
 */
@Composable
private fun SendFab(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (enabled) Q.primaryText else Q.muted,
        animationSpec = tween(Motion.D2), label = "sendBg",
    )
    val fg by animateColorAsState(
        targetValue = if (enabled) Q.primaryFg else Q.mutedFg,
        animationSpec = tween(Motion.D2), label = "sendFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "sendScale",
    )
    Box(
        Modifier
            .size(48.dp)
            .scale(scale)
            .scaleTap(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .shadow(
                if (enabled) 8.dp else 0.dp, CircleShape,
                ambientColor = Q.primary.copy(alpha = 0.30f),
                spotColor = Q.primary.copy(alpha = 0.35f),
            )
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Send, "Göndər",
            tint = fg, modifier = Modifier.size(20.dp),
        )
    }
}

private fun Modifier.androidxChatTap(onClick: () -> Unit): Modifier =
    this.then(Modifier.pressable(onClick = onClick))

private fun Modifier.androidxChatClip(): Modifier =
    this.then(Modifier.clip(RoundedCornerShape(Q.rLg.dp)))

private fun Modifier.androidxChatClip2(): Modifier =
    this.then(Modifier.clip(RoundedCornerShape(Q.rMd.dp)))

private fun Modifier.androidxChatHeaderBorder(): Modifier =
    this.then(Modifier.border(1.dp, Q.border))

private fun Modifier.androidxComposerBorder(): Modifier = this.then(
    Modifier.border(
        1.dp, Q.border,
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    )
)
