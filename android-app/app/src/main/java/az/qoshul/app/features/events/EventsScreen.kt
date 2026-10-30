package az.qoshul.app.features.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.CategoryChip
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.SafetyNote
import az.qoshul.app.core.ui.SearchBarField
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.SectionLink
import az.qoshul.app.core.ui.Segmented
import az.qoshul.app.core.ui.imagePaint
import kotlinx.coroutines.launch

/**
 * Tədbirlər görünüşü — Flutter events_screen.dart portu (ROL TAB 3, D-034):
 * Kəşfin "Tədbirlər" daxili tabı: axtarış (pinned), kateqoriya çipləri,
 * Yaxınlaşan / Mənim tədbirlərim / Yaratdığım + D-026 L.1 sıralama.
 */
@Composable
fun EventsScreen(engine: AppEngine) {
    var tab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hamısı") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }

    val groupIcons = linkedMapOf(
        "Hamısı" to Icons.Filled.Explore, "İdman" to Icons.Filled.SportsVolleyball,
        "Təbiət" to Icons.Filled.Terrain, "Mədəniyyət" to Icons.Filled.AccountBalance,
        "Musiqi" to Icons.Filled.MusicNote, "Yemək və içki" to Icons.Filled.LocalCafe,
        "Təhsil" to Icons.Filled.MenuBook, "Texnologiya" to Icons.Filled.Memory,
        "Biznes və karyera" to Icons.Filled.BusinessCenter, "Oyunlar" to Icons.Filled.Casino,
        "İncəsənət" to Icons.Filled.Palette, "Könüllülük" to Icons.Filled.VolunteerActivism,
        "Səyahət və digər" to Icons.Filled.Flight,
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp)) {
                SearchBarField(value = search, onValueChange = { search = it }, hint = "Tədbir axtar...")
            }
            val st by engine.state.collectAsState()
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 4.dp, bottom = 132.dp
                ),
            ) {
                // — kateqoriya çipləri —
                item {
                    StaggerIn(0) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            groupIcons.forEach { (name, icon) ->
                                Box(Modifier.padding(end = 7.dp)) {
                                    CategoryChip(
                                        label = name, icon = icon, active = category == name,
                                        onClick = { category = name },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    StaggerIn(1) {
                        Segmented(
                            labels = listOf("Yaxınlaşan", "Mənim tədbirlərim", "Yaratdığım"),
                            active = tab, onChanged = { tab = it },
                        )
                    }
                }

                val q = search.lowercase()
                fun ok(vararg parts: String) = q.isEmpty() || parts.joinToString(" ").lowercase().contains(q)
                val upcoming = st.events.filter { !it.past && ok(it.title, it.location, it.category, it.city) }
                val mine = st.events.filter {
                    (it.id in st.joined || it.id in st.pending) && ok(it.title, it.location)
                }
                val createdByMe = st.events.filter { it.host == st.profileName && ok(it.title, it.location) }
                val base = when (tab) { 0 -> upcoming; 1 -> mine; else -> createdByMe }
                val list = base.filter { category == "Hamısı" || FakeQoshulDataSource.groupOf(it.category) == category }

                // D-026 (L.1): "Sənin səviyyənə görə"
                val proRanked = list.sortedByDescending {
                    XpRules.proWeight(it.level) * (st.rankIndex + 1)
                }
                if (tab == 0 && proRanked.isNotEmpty()) {
                    item {
                        SectionHeading(
                            title = "Sənin səviyyənə görə", icon = Icons.Filled.MilitaryTech,
                            trailing = { SectionLink(text = st.rank, icon = Icons.Filled.WorkspacePremium) },
                        )
                    }
                    items(2) { i ->
                        EventCard(
                            event = proRanked[i], joined = proRanked[i].id in st.joined,
                            pending = proRanked[i].id in st.pending, saved = proRanked[i].id in st.saved,
                            showLevel = true,
                            onToggleSave = { engine.toggleSave(proRanked[i].id) },
                            onOpen = { detailEvent = proRanked[i] },
                            onJoin = {
                                val okJoin = engine.join(proRanked[i].id)
                                if (okJoin) scope.launch {
                                    snackbar.showSnackbar(
                                        if (proRanked[i].approval) "Sorğun təşkilatçıya göndərildi."
                                        else "Tədbirə qoşuldun! +${XpRules.JOIN_XP} XP"
                                    )
                                }
                            },
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }

                item {
                    if (list.isEmpty()) {
                        EmptyState(
                            icon = if (tab == 2) Icons.Outlined.AddCircleOutline else Icons.Filled.Event,
                            text = if (tab == 0) {
                                if (q.isEmpty()) "Hazırda yaxınlaşan tədbir yoxdur."
                                else "\"$q\" üzrə tədbir tapılmadı."
                            } else if (tab == 1) {
                                "Hələ heç bir tədbirə qoşulmamısan. \"Yaxınlaşan\" siyahısından birini seç."
                            } else {
                                "Heç bir tədbir yaratmamısan. Mərkəzdəki \"Yarat\" düyməsi ilə başla."
                            },
                        )
                    }
                }
                items(list.size) { idx ->
                    val e = list[idx]
                    EventCard(
                        event = e, joined = e.id in st.joined, pending = e.id in st.pending,
                        saved = e.id in st.saved, showLevel = false,
                        onToggleSave = { engine.toggleSave(e.id) },
                        onOpen = { detailEvent = e },
                        onJoin = {
                            val okJoin = engine.join(e.id)
                            if (okJoin) scope.launch {
                                snackbar.showSnackbar(
                                    if (e.approval) "Sorğun təşkilatçıya göndərildi."
                                    else "Tədbirə qoşuldun! +${XpRules.JOIN_XP} XP"
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (tab == 0) {
                    item {
                        CommunityBanner(
                            icon = Icons.Filled.TableRestaurant,
                            title = "Tədbir yoxdur? Məkan rezerv et",
                            subtitle = "Partnyor məkanlarda stol ayır, QR al və iştirakla XP qazan.",
                            onClick = { /* VenueDetail — məkan rezerv axını */ },
                            trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                        )
                    }
                }
                item { SafetyNote() }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
        detailEvent?.let { ev ->
            EventDetailSheet(engine, ev, onDismiss = { detailEvent = null })
        }
    }
}

/**
 * .event-card — Flutter event_card.dart portu.
 * A4 düzəlişi: saxta avatar stack əvəzinə iştirakçı sayı çipi.
 * A5 düzəlişi: düymələr min 48dp toxunma hədəfi.
 */
@Composable
fun EventCard(
    event: QoshulEvent,
    joined: Boolean,
    pending: Boolean,
    saved: Boolean,
    showLevel: Boolean,
    onToggleSave: () -> Unit,
    onOpen: () -> Unit,
    onJoin: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    StaggerIn(0) {
        Column(
            Modifier
                .fillMaxWidth()
                .androidxCardTap(interaction, onOpen)
                .androidxCardBg()
        ) {
            // — şəkil + nişanlar —
            Box(Modifier.fillMaxWidth().height(156.dp)) {
                Image(
                    painter = imagePaint(event.image), contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(156.dp),
                )
                Box(
                    Modifier.fillMaxWidth().height(156.dp).androidxShade(),
                )
                if (event.past) {
                    WhiteBadge(Icons.Filled.Event, "Keçib", Modifier.padding(start = 12.dp, top = 12.dp))
                }
                if (showLevel && !event.past && event.level in listOf("Peşəkar", "Orta", "Başlanğıc")) {
                    WhiteBadge(
                        if (event.level == "Peşəkar") Icons.Filled.MilitaryTech else Icons.Filled.School,
                        event.level!!,
                        Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp),
                    )
                }
                if (!event.past) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(38.dp)
                            .androidxMinTouch { onToggleSave() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                "Yadda saxla", tint = if (saved) Q.primary else Q.foreground,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                if (!event.past) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 10.dp),
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(13.5.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${event.distance} km",
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 10.5.sp, color = Color.White))
                    }
                }
            }
            // — info —
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (event.category == "Qəhvə") Icons.Filled.LocalCafe else Icons.Filled.Groups,
                        null, tint = if (event.category == "Qəhvə") Q.accentFg else Q.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(event.category, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 10.5.sp,
                            fontWeight = FontWeight.W700,
                            color = if (event.category == "Qəhvə") Q.accentFg else Q.primary))
                }
                Spacer(Modifier.height(7.dp))
                Text(event.title, style = T.eventTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = Q.mutedFg, modifier = Modifier.size(13.5.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(event.location, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = T.meta, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, null, tint = Q.mutedFg, modifier = Modifier.size(13.5.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${event.date}, ${event.time}", style = T.meta)
                }
                // — footer —
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .androidxFooterBorder()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    // A4: iştirakçı sayı çipi (saxta avatar stack əvəzinə)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Groups, null, tint = Q.mutedFg, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${event.participants} nəfər qoşulub · ${event.spotsLeft} yer qalıb",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 9.5.sp, color = Q.mutedFg),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    JoinButton(event, joined, pending, onJoin)
                }
            }
        }
    }
}

private data class JoinBtn(
    val label: String, val icon: ImageVector?, val enabled: Boolean, val secondary: Boolean,
)

@Composable
private fun JoinButton(event: QoshulEvent, joined: Boolean, pending: Boolean, onJoin: () -> Unit) {
    val b = when {
        event.past -> JoinBtn("Keçib", null, false, true)
        joined -> JoinBtn("Qoşuldun", Icons.Filled.Check, true, true)
        pending -> JoinBtn("Təsdiq gözləyir", Icons.Filled.HourglassEmpty, false, true)
        event.isFull -> JoinBtn("Yer yoxdur", null, false, true)
        else -> JoinBtn("Qoşul", Icons.Filled.NorthEast, true, false)
    }
    Button(
        onClick = onJoin,
        enabled = b.enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        modifier = Modifier.height(38.dp), // A5: məzmun 34px + toxunma hədəfi
        shape = RoundedCornerShape(Q.rMd.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (b.secondary) Q.secondary else Q.primary,
            contentColor = if (b.secondary) Q.primary else Q.primaryFg,
            disabledContainerColor = Q.secondary,
            disabledContentColor = Q.primary,
        ),
    ) {
        b.icon?.let {
            Icon(it, null, modifier = Modifier.size(13.5.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(b.label, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 11.5.sp, fontWeight = FontWeight.W600))
    }
}

/** Ağ nişan (şəkil üzərində). */
@Composable
private fun WhiteBadge(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Q.card, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 5.5.dp),
    ) {
        Icon(icon, null, tint = Q.primary, modifier = Modifier.size(13.5.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 11.sp,
            fontWeight = FontWeight.W700, color = Q.primary))
    }
}

// — Modifier köməkçiləri (fayl daxili) —

private fun Modifier.androidxCardTap(interaction: MutableInteractionSource, onOpen: () -> Unit): Modifier =
    this.then(
        Modifier
            .scaleTap(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
    )

private fun Modifier.androidxCardBg(): Modifier = this.then(
    Modifier
        .background(Q.card, RoundedCornerShape(Q.rLg.dp))
        .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
)

private fun Modifier.androidxShade(): Modifier = this.then(
    Modifier.background(
        androidx.compose.ui.graphics.Brush.verticalGradient(
            0.4f to Color.Transparent,
            1.0f to Q.photoShade.copy(alpha = 0.5f),
        )
    )
)

private fun Modifier.androidxFooterBorder(): Modifier =
    this.then(Modifier.background(Q.border))

private fun Modifier.androidxMinTouch(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(onClick = onClick)
)
