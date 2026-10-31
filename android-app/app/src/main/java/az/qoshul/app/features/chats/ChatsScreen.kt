package az.qoshul.app.features.chats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.reportReasons
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.motion.TypingDots
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.ChatBubble
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.ViewHeading
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.auth.QoshulTextField
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
    var activeEventId by remember { mutableStateOf<String?>(null) }
    var activeFriendId by remember { mutableStateOf<String?>(null) }
    var reportFor by remember { mutableStateOf<Person?>(null) }
    val st by engine.state.collectAsState()

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

    if (activeEvent != null) {
        ChatView(
            title = activeEvent.title,
            subtitle = "${activeEvent.participants} iştirakçı",
            imageKey = activeEvent.image,
            messages = st.eventMessages[activeEvent.id] ?: emptyList(),
            onSend = { engine.sendEventMessage(activeEvent.id, it) },
            onBack = { activeEventId = null },
            calendar = true,
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
                                colors = RadioButtonDefaults.colors(selectedColor = Q.primary),
                            )
                            Text(r, style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.sp))
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
    val st by engine.state.collectAsState()
    val joinedEvents = st.events.filter { it.id in st.joined }
    val friends = engine.acceptedFriends

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 21.dp, bottom = 128.dp
        ),
    ) {
        item { ViewHeading(title = "Söhbətlər", subtitle = "Görüşdən əvvəl tanış olaq.") }
        if (joinedEvents.isEmpty() && friends.isEmpty()) {
            item {
                EmptyState(
                    icon = androidx.compose.material.icons.Icons.Outlined.ChatBubble,
                    text = "İlk söhbətin səni gözləyir.\nTədbirə qoşulanda onun qrup söhbəti, " +
                        "dostluq qəbul edəndə isə şəxsi söhbət burada görünəcək.",
                )
            }
        } else {
            if (joinedEvents.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = "Qruplar", icon = Icons.Filled.Groups,
                        // D-056: joinedUpcomingCount engine-də hesablanırdı, heç yerdə göstərilmirdi
                        trailing = {
                            if (engine.joinedUpcomingCount > 0) {
                                Text("${engine.joinedUpcomingCount} yaxınlaşan", style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 11.5.sp,
                                    fontWeight = FontWeight.W600, color = Q.mutedFg))
                            }
                        },
                    )
                }
                items(joinedEvents) { e ->
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
                items(friends) { p ->
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
            .padding(vertical = 18.dp),
    ) {
        Image(
            painter = imagePaint(imageKey), contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(58.dp).androidxChatClip(),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W800))
            Spacer(Modifier.height(5.dp))
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 12.5.sp, color = Q.mutedFg))
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
    calendar: Boolean = false,
    friendActions: Boolean = false,
    quickReplies: List<String> = emptyList(),
    onBlock: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReport: () -> Unit = {},
) {
    var text by remember { mutableStateOf("") }
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
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Geri", tint = Q.primary, modifier = Modifier.size(22.dp))
            }
            Image(
                painter = imagePaint(imageKey), contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).androidxChatClip2(),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 13.sp, fontWeight = FontWeight.W800))
                Text(subtitle, style = T.metaSm)
                if (typing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TypingDots(color = Q.primary, size = 4f)
                        Spacer(Modifier.width(5.dp))
                        Text("yazır...", style = T.metaSm.copy(color = Q.primary))
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
            } else if (calendar) {
                // A3: təqvim ikonu — backend gözləyir, deaktiv işarə
                Icon(Icons.Filled.Event, "Təqvimə əlavə et (tezliklə)",
                    tint = Q.mutedFg.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
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
                    itemsIndexed(messages) { i, m ->
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
        // — tez cavablar + yazma zolağı (D-056) —
        Column(
            Modifier
                .fillMaxWidth()
                .androidxComposerBorder()
                .background(Q.card)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (text.isBlank() && quickReplies.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    quickReplies.forEach { qr ->
                        Text(
                            qr,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 12.sp,
                                fontWeight = FontWeight.W600, color = Q.foreground),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .androidxChatTap { sendAction(qr) }
                                .background(Q.muted, RoundedCornerShape(Q.rPill))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    QoshulTextField(value = text, onValueChange = { text = it }, hint = "Bir mesaj yaz...")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { sendAction(text) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(42.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    shape = RoundedCornerShape(Q.rPill),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Q.primary, contentColor = Q.primaryFg,
                        disabledContainerColor = Q.muted, disabledContentColor = Q.mutedFg),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Göndər", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun Modifier.androidxChatTap(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

private fun Modifier.androidxChatClip(): Modifier =
    this.then(Modifier.clip(RoundedCornerShape(Q.rLg.dp)))

private fun Modifier.androidxChatClip2(): Modifier =
    this.then(Modifier.clip(RoundedCornerShape(Q.rMd.dp)))

private fun Modifier.androidxChatHeaderBorder(): Modifier =
    this.then(Modifier.border(1.dp, Q.border))

private fun Modifier.androidxComposerBorder(): Modifier =
    this.then(Modifier.border(1.dp, Q.border))
