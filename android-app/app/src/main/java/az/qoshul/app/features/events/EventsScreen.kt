package az.qoshul.app.features.events

import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Memory
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
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.motion.pressable
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.SelectBoxTrigger
import az.qoshul.app.core.ui.PickHostOverlay
import az.qoshul.app.core.ui.PickOption
import az.qoshul.app.core.ui.pickAnchor
import az.qoshul.app.core.ui.pickHost
import az.qoshul.app.core.ui.rememberPickHostState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
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
 * D-055: onFinishActivity/onOpenChat ötürülür — bitirmə axını və qrup
 * söhbəti keçidi əvvəl MainAktivite-də kəsılırdı.
 */
@Composable
fun EventsScreen(
    engine: AppEngine,
    nav: NavState,
    focus: MapFocusState,
    onFinishActivity: (QoshulEvent) -> Unit = {},
    onOpenChat: (QoshulEvent) -> Unit = {},
    // D-125: üst tab zolağı (FeedScreen-dən) — opsional, LC-nin ilk elementi
    // kimi məzmunla birgə sürüşür; birbaşa istifadədə (PickerOverlayTest) yoxdur.
    tabs: (@Composable () -> Unit)? = null,
) {
    var tab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hamısı") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }
    // D-089: kateqoriya pick host-u — root Box host, trigger anchor ölçür
    val pickHost = rememberPickHostState()

    // D-110: kateqoriya ikonları + izahları (selector box panel sətirləri);
    // direktiv „biznes ilə əlaəqli yerləri sil“ — „Biznes və karyera“ qrupu
    // filterdən çıxarıldı (biznes məzmunu tərəfdaş/şirkət axınına aiddir,
    // sosial kəşf axınında YOX — D-115)
    val groupIcons = linkedMapOf(
        "Hamısı" to Icons.Filled.Explore, "İdman" to Icons.Filled.SportsVolleyball,
        "Təbiət" to Icons.Filled.Terrain, "Mədəniyyət" to Icons.Filled.AccountBalance,
        "Musiqi" to Icons.Filled.MusicNote, "Yemək və içki" to Icons.Filled.LocalCafe,
        "Təhsil" to Icons.AutoMirrored.Filled.MenuBook, "Texnologiya" to Icons.Filled.Memory, // U4-4: MenuBook AutoMirrored
        "Oyunlar" to Icons.Filled.Casino,
        "İncəsənət" to Icons.Filled.Palette, "Könüllülük" to Icons.Filled.VolunteerActivism,
        "Səyahət və digər" to Icons.Filled.Flight,
    )
    val groupHints = mapOf(
        "Hamısı" to "Bütün kateqoriyalarda axtarış",
        "İdman" to "Futbol, voleybol, qaçış, fitnes",
        "Təbiət" to "Yürüşlər, pikniklər, açıq hava",
        "Mədəniyyət" to "Teatr, muzey, sərgilər",
        "Musiqi" to "Canlı musiqi, konsertlər",
        "Yemək və içki" to "Kafe, restoran, qəhvə görüşləri",
        "Təhsil" to "Dil klubları, seminarlar, kurslar",
        "Texnologiya" to "IT görüşlər, hackathonlar",
        "Oyunlar" to "Stolüstü oyunlar, oyun gecələri",
        "İncəsənət" to "Rəsm, foto, dizayn",
        "Könüllülük" to "Xeyriyyə aksiyaları, təmizlik",
        "Səyahət və digər" to "Səfərlər və digər tədbirlər",
    )

    // D-126: axtarışa uyğun şəhər nəticələri (üst 5, A-Z) — hesablama
    // LazyColumn-dən ƏVVƏL (LazyListScope @Composable kontekst deyil);
    // filtr yalnız axtarış mətnindən asılıdır, „selected“ işarəsi st.city
    // dəyişəndə recompose ilə yenilənir.
    val cityHits = remember(search) {
        if (search.isBlank()) emptyList()
        else engine.dataSource.cities.filter {
            it.name.lowercase().contains(search.lowercase())
        }.take(5)
    }

    // D-125 (direktiv: „ana menyuda şəhər və tabların olduğu yeri
    // sürüşdürərkən sabit yox adi qalacağı moda gətir“): tablar (ötürüldükdə)
    // və axtarış sətri LC-nin İLK elementləridir — sabit deyil, məzmunla
    // sürüşür (köhnə Column+pinned-Box ləğv; axtarışın altındakı 6+4dp
    // ritm bottom-10dp-də qorunub).
    Box(Modifier.fillMaxSize().pickHost(pickHost)) {
        val st by engine.state.collectAsStateWithLifecycle()
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 0.dp, bottom = 132.dp
            ),
        ) {
            tabs?.let { t ->
                item(key = "feedTabs") { t() }
            }
            item(key = "eventsSearch") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp)
                ) {
                    SearchBarField(value = search, onValueChange = { search = it }, hint = "Tədbir və ya şəhər axtar...")
                }
            }
            // — D-126: Şəhərlər bölməsi — Ana menyunun hər iki görünüşündə eyni
            // axtarış dili (FeedScreen pariteti): şəhər adına tuş olanda üst 5
            // nəticə; seçəndə şəhər dəyişir (tədbir siyahısı yeni şəhərə keçir),
            // axtarış təmizlənir.
            if (cityHits.isNotEmpty()) {
                item(key = "cityHitsHeading") {
                    SectionHeading(
                        title = "Şəhərlər", icon = Icons.Filled.LocationOn,
                        trailing = {
                            Text("${cityHits.size} nəticə", style = T.chipXs.copy(fontWeight = FontWeight.W600, color = Q.mutedFg))
                        },
                    )
                }
                items(cityHits, key = { "city-${it.name}" }) { c ->
                    val selected = st.city == c.name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(onClick = {
                                engine.setCity(c.name)
                                search = ""
                            })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            Icons.Filled.LocationOn, null,
                            tint = if (selected) Q.primaryText else Q.mutedFg,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            c.name,
                            style = T.personName.copy(
                                fontWeight = if (selected) FontWeight.W700 else FontWeight.W500,
                                color = if (selected) Q.primaryText else Q.foreground,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            c.region,
                            style = T.metaSm.copy(color = Q.mutedFg),
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(Icons.Filled.Check, null, tint = Q.primaryText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                item(key = "cityHitsSpace") { Spacer(Modifier.height(12.dp)) }
            }
                // — kateqoriya çipləri —
                item {
                    StaggerIn(0) {
                        // D-110: selector box — etiket + cari dəyər qutusu; panel
                        // siyahının ÜZƏRİNƏ çıxır (PickHostOverlay, vahid sütun)
                        SelectBoxTrigger(
                            fieldLabel = "Kateqoriya",
                            value = category,
                            icon = groupIcons[category] ?: Icons.Filled.Explore,
                            open = pickHost.openReq?.key == "cat",
                            onClick = {
                                pickHost.toggle(
                                    "cat",
                                    groupIcons.map {
                                        PickOption(it.key, it.value, groupHints[it.key])
                                    },
                                    category,
                                ) { category = it }
                            },
                            modifier = Modifier.pickAnchor(pickHost, "cat"),
                        )
                    }
                }
                // D-063: çiplər → seqment 20dp ayrılıq (tək kontrol zolağı kimi
                // birləşib görünürdü — vizual audit tapıntısı)
                item { Spacer(Modifier.height(4.dp)) }
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
                // D-053 fix: "Yaxınlaşan" artıq tarixə görə artan sıralanır —
                // əvvəl yeni yaradılan tədbir index 0-a düşdüyündən gələcək
                // həftənin tədbiri bu gününkündən öndə görünürdü.
                val upcoming = st.events.filter { !it.past && ok(it.title, it.location, it.category, it.city) }
                    .sortedBy { it.epochDay }
                val mine = st.events.filter {
                    (it.id in st.joined || it.id in st.pending) && ok(it.title, it.location)
                }
                val createdByMe = st.events.filter { it.host == st.profileName && ok(it.title, it.location) }
                val base = when (tab) { 0 -> upcoming; 1 -> mine; else -> createdByMe }
                val list = base.filter { category == "Hamısı" || engine.dataSource.groupOf(it.category) == category }

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
                    // D-051 fix: items(2) siyahı 1 elementli olanda IndexOutOfBounds
                    // atırdı (proRanked[1]) — minOf ilə sərhədləndi.
                    // U2.1 (UI-01): key = stabil id (take(2) — D-051 sərhədi qorunur).
                    // LazyColumn açarları BÜTÜN siyahı üzrə unikal olmalıdır — "pro-"
                    // prefiksi aşağıdakı tam list ilə toqquşmur (CI dərsi: e12 ×2)
                    items(proRanked.take(2), key = { "pro-${it.id}" }) { e ->
                        EventCard(
                            event = e, joined = e.id in st.joined,
                            pending = e.id in st.pending, saved = e.id in st.saved,
                            showLevel = true,
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
                                else "„$q“ üzrə tədbir tapılmadı."
                            } else if (tab == 1) {
                                "Hələ heç bir tədbirə qoşulmamısan. „Yaxınlaşan“ siyahısından birini seç."
                            } else {
                                "Heç bir tədbir yaratmamısan. Mərkəzdəki „Yarat“ düyməsi ilə başla."
                            },
                        )
                    }
                }
                items(list, key = { "ev-${it.id}" }) { e ->
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
                            onClick = { nav.push(Route.Partners) },
                            trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primaryText, modifier = Modifier.height(20.dp)) },
                        )
                    }
                }
                item { SafetyNote() }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
        // D-110: kateqoriya paneli — siyahının ÜZƏRİNƏ (root Box son qatı);
        // pickAuto paneli avtomatik aşağı/yuxarı açır; sətirlər ikon +
        // başlıq + izah ilə (vahid sütun — şəkil direktivi dili).
        PickHostOverlay(
            host = pickHost,
            panelModifier = Modifier.align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            maxHeight = 376.dp,
        )
        detailEvent?.let { ev ->
            EventDetailSheet(
                engine, ev,
                onDismiss = { detailEvent = null },
                onShowOnMap = { e -> detailEvent = null; focus.event(e.id) },
                onFinishActivity = onFinishActivity,
                onOpenChat = onOpenChat,
            )
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
                // D-061: level null olsa (nə olar əgər) nişan sadəcə yoxdur
                val lvl = event.level
                if (showLevel && !event.past && lvl != null && lvl in listOf("Peşəkar", "Orta", "Başlanğıc")) {
                    WhiteBadge(
                        if (lvl == "Peşəkar") Icons.Filled.MilitaryTech else Icons.Filled.School,
                        lvl,
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
                                .background(Q.photoFg.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                "Yadda saxla", tint = if (saved) Q.primaryText else Q.foreground,
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
                        Icon(Icons.Filled.LocationOn, null, tint = Q.photoFg, modifier = Modifier.size(13.5.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${event.distance} km",
                            style = T.metaSm.copy(color = Q.photoFg))
                    }
                }
            }
            // — info —
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (event.category == "Qəhvə") Icons.Filled.LocalCafe else Icons.Filled.Groups,
                        null, tint = if (event.category == "Qəhvə") Q.accentFg else Q.primaryText,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(event.category, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = T.metaSm.copy(fontWeight = FontWeight.W700, color = if (event.category == "Qəhvə") Q.accentFg else Q.primaryText))
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
                            style = T.metaSm,
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
        shape = RoundedCornerShape(Q.rPill),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (b.secondary) Q.secondary else Q.primary,
            contentColor = if (b.secondary) Q.primaryText else Q.primaryFg,
            disabledContainerColor = Q.secondary,
            disabledContentColor = Q.primary,
        ),
    ) {
        b.icon?.let {
            Icon(it, null, modifier = Modifier.size(13.5.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(b.label, style = T.chipXs.copy(fontWeight = FontWeight.W600))
    }
}

/** Ağ nişan (şəkil üzərində). */
@Composable
private fun WhiteBadge(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Q.card, RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 5.5.dp),
    ) {
        Icon(icon, null, tint = Q.primaryText, modifier = Modifier.size(13.5.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = T.chipXs.copy(fontWeight = FontWeight.W700, color = Q.primaryText))
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
    Modifier.pressable(onClick = onClick)
)
