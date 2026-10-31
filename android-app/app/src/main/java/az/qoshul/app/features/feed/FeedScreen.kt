package az.qoshul.app.features.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.ui.LiveBadge
import az.qoshul.app.features.events.CityPickerSheet
import az.qoshul.app.features.events.EventDetailSheet
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.engine.AppUiState
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.RankBadge
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.SearchBarField
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.SectionLink
import az.qoshul.app.core.ui.SafetyNote
import az.qoshul.app.core.ui.VenueMiniCard
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.PersonMiniCard
import az.qoshul.app.core.ui.XpTrack
import az.qoshul.app.features.events.EventsScreen

/**
 * Kəşf (ANA EKRAN) — Flutter feed_screen.dart portu (ROL TAB 1, D-034):
 * "Sənin üçün | Tədbirlər" daxili tabları; axtarış pinned (D-029).
 * D-050: bütün kart bağlantıları canlıdır (məkan → detal, insan → Match,
 * endirim → Kuponlar) — ölü onClick-lər aradan qaldırıldı.
 * D-054: "Aktiv tədbirlər" — bugünkü canlı tədbirlər birbaşa ana ekranda
 * üfüqi kart zolağı kimi (istifadəçi tələbi: aktiv tədbirlər ana ekranda
 * görünməlidir; "Bu axşam" mətn banneri əvəzinə real kartlar).
 * D-055: LiveEventCard artıq tədbir DETALINI açır (əvvəl siyahı açırdı);
 *        şəhər həqiqətən dəyişilə bilər (CityPill + CityPickerSheet).
 * D-056: "AI seçimi" bölməsi — engine.aiRecommendations canlıdır.
 * D-057: salamlaşma yenidən qurulub (badge addırda, şəhər həbı sağda),
 *        bölmə araları vahid 20dp ritmi, "N məkan" artıq saxta link deyil.
 */
@Composable
fun FeedScreen(
    engine: AppEngine,
    nav: NavState,
    focus: MapFocusState,
    onFinishActivity: (QoshulEvent) -> Unit = {},
    onOpenChat: (QoshulEvent) -> Unit = {},
) {
    var view by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ViewTabs(active = view, onChanged = { view = it })
        AnimatedContent(
            targetState = view,
            transitionSpec = {
                (fadeIn(tween(Motion.D2, easing = Motion.ease)) +
                    scaleIn(initialScale = 0.94f, animationSpec = tween(Motion.D2, easing = Motion.ease)))
                    .togetherWith(fadeOut(tween(Motion.D2)))
            },
            label = "feedTabs",
        ) { v ->
            if (v == 0) {
                ForYouView(engine, nav, focus, search = search, onSearchChange = { search = it },
                    onOpenEvents = { view = 1 }, onFinishActivity = onFinishActivity, onOpenChat = onOpenChat)
            } else {
                EventsScreen(engine, nav, focus, onFinishActivity = onFinishActivity, onOpenChat = onOpenChat)
            }
        }
    }
}

/** Daxili tab zolağı — "Sənin üçün | Tədbirlər" (Instagram-vari üst tablar). */
@Composable
private fun ViewTabs(active: Int, onChanged: (Int) -> Unit) {
    val tabs = listOf(
        Triple(Icons.Outlined.AutoAwesome, Icons.Rounded.AutoAwesome, "Sənin üçün"),
        Triple(Icons.Outlined.Event, Icons.Rounded.Event, "Tədbirlər"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp)
            .background(Q.muted, RoundedCornerShape(Q.rPill))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, (icon, activeIcon, label) ->
            val isActive = i == active
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(if (isActive) Q.card else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(Q.rPill))
                    .clickable { onChanged(i) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isActive) activeIcon else icon, null,
                        tint = if (isActive) Q.primary else Q.mutedFg,
                        modifier = Modifier.height(17.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        label, maxLines = 1,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = JakartaSans, fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.W800 else FontWeight.W500,
                            color = if (isActive) Q.primary else Q.mutedFg,
                        ),
                    )
                }
            }
        }
    }
}

/** "Sənin üçün" görünüşü — Instagram-paradigmalı axın. */
@Composable
private fun ForYouView(
    engine: AppEngine,
    nav: NavState,
    focus: MapFocusState,
    search: String,
    onSearchChange: (String) -> Unit,
    onOpenEvents: () -> Unit,
    onFinishActivity: (QoshulEvent) -> Unit,
    onOpenChat: (QoshulEvent) -> Unit,
) {
    val st by engine.state.collectAsStateWithLifecycle()
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }
    var cityOpen by remember { mutableStateOf(false) }
    // D-056: AI seçimi — şəhər dəyişəndə yenidən hesablanır (LazyColumn
    // daxilində remember çağırmaq olmaz — @Composable konteksti deyil)
    val aiPicks = remember(st.city) { engine.aiRecommendations() }

    Column(Modifier.fillMaxSize()) {
        // — sabit axtarış (D-029) —
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp)) {
            SearchBarField(
                value = search, onValueChange = onSearchChange,
                hint = "Məkan və ya insan axtar...",
            )
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, bottom = 132.dp
            ),
        ) {
            // salamlaşma (D-057: badge addırda — "ad + rütbə" bir vahid;
            // sağda şəhər həbı — boş sahə problemi də həll olundu)
            item {
                StaggerIn(0) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Salam, ${st.firstName}!", maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, style = T.h1)
                                Spacer(Modifier.width(8.dp))
                                RankBadge(label = st.rank)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Bu gün yeni bir hekayəyə qoşul.", style = T.subtitle)
                        }
                        Spacer(Modifier.width(10.dp))
                        CityPill(city = st.city, onClick = { cityOpen = true })
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                StaggerIn(1) { XpTrack(xp = st.xp) }
                Spacer(Modifier.height(12.dp))
            }
            // — Aktiv tədbirlər (D-054): bugünkü tədbirlər birbaşa ana ekranda —
            val active = engine.activeEvents
            if (active.isNotEmpty()) {
                item {
                    StaggerIn(2) {
                        SectionHeading(
                            title = "Aktiv tədbirlər", icon = Icons.Rounded.Event,
                            trailing = {
                                SectionLink(text = "Hamısı", icon = Icons.Filled.NorthEast, onClick = onOpenEvents)
                            },
                        )
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(active) { e ->
                            LiveEventCard(
                                event = e, joined = e.id in st.joined, pending = e.id in st.pending,
                                onClick = { detailEvent = e },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            // — AI seçimi (D-056): aiRecommendations əvvəlcədən var idi, UI-a bağlı deyildi —
            if (aiPicks.isNotEmpty()) {
                item {
                    StaggerIn(2) {
                        SectionHeading(
                            title = "AI seçimi", icon = Icons.Rounded.AutoAwesome,
                            trailing = {
                                SectionLink(text = "AI köməkçi", icon = Icons.Filled.NorthEast, onClick = {
                                    nav.push(Route.Assistant)
                                })
                            },
                        )
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(aiPicks) { (e, reason) ->
                            AiPickCard(event = e, reason = reason, joined = e.id in st.joined,
                                onClick = { detailEvent = e })
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            // — Populyar məkanlar —
            val venues = engine.partners.filter { v ->
                v.city == st.city && (search.isEmpty() ||
                    "${v.name} ${v.category} ${v.address}".lowercase().contains(search.lowercase()))
            }
            if (venues.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = "Populyar məkanlar", icon = Icons.Filled.Storefront,
                        // D-057: sayğac — saxta yaşıl link YOX (kliklənə bilməz görünürdü)
                        trailing = {
                            Text("${venues.size} məkan", style = androidx.compose.ui.text.TextStyle(
                                fontFamily = JakartaSans, fontSize = 11.5.sp,
                                fontWeight = FontWeight.W600, color = Q.mutedFg))
                        },
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(venues) { v ->
                            VenueMiniCard(
                                imageKey = v.image, name = v.name, category = v.category,
                                priceRange = v.priceRange, rating = v.rating,
                                discount = v.discountPercent,
                                onClick = { nav.push(Route.VenueDetail(v.id)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            // — Sənə uyğun insanlar —
            item {
                SectionHeading(
                    title = "Sənə uyğun insanlar", icon = Icons.Filled.FavoriteBorder,
                    trailing = {
                        SectionLink(text = "Hamısına bax", icon = Icons.Filled.NorthEast, onClick = {
                            nav.push(Route.Match)
                        })
                    },
                )
            }
            val people = engine.matchCandidates.filter { p ->
                search.isEmpty() || "${p.firstName} ${p.lastName}".lowercase().contains(search.lowercase())
            }
            if (people.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        text = if (search.isEmpty())
                            "Yaxınlıqda yeni insan yoxdur.\nGörüşlərə qoşul, tanış ol!"
                        else "\"$search\" üzrə insan tapılmadı.",
                    )
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(people) { p ->
                            PersonMiniCard(
                                imageKey = p.image, name = p.firstName, age = p.age,
                                distanceLabel = if (st.showDistance) p.distanceLabel else p.city,
                                matchPercent = p.match,
                                onClick = { nav.push(Route.Match) },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            // — Endirimli təkliflər —
            item {
                SectionHeading(
                    title = "Endirimli təkliflər", icon = Icons.Filled.Percent,
                    trailing = {
                        SectionLink(text = "Bütün endirimlər", icon = Icons.Filled.NorthEast, onClick = {
                            nav.push(Route.Coupons)
                        })
                    },
                )
            }
            val discountOffers = engine.visibleOffers.filter { !it.autoByLevel || it.percent > 0 }
            if (discountOffers.isEmpty()) {
                item { EmptyState(icon = Icons.Filled.Percent, text = "Hazırda endirim təklifi yoxdur.") }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(discountOffers) { o ->
                            OfferMiniCard(
                                label = o.label,
                                venueLine = if (o.autoByLevel) "${o.venueName} · səviyyən ilə açıq"
                                else "${o.venueName} · ${o.xpPrice} XP",
                                onClick = { nav.push(Route.Coupons) },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            // — cəmiyyət banneri (D-055: ölü banner deyil — Tərəfdaşlara aparır) —
            item {
                CommunityBanner(
                    icon = Icons.Filled.School,
                    title = "Eyni maraqlar, yeni tanışlıqlar",
                    subtitle = "Sevdiyin məkanlarda görüş, əlavə XP qazan.",
                    onClick = { nav.push(Route.Partners) },
                    trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                )
            }
            item { SafetyNote() }
        }
    }

    // — modal sheet-lər (D-055/D-056) —
    if (cityOpen) {
        CityPickerSheet(engine = engine, onDismiss = { cityOpen = false })
    }
    detailEvent?.let { ev ->
        EventDetailSheet(
            engine = engine, event = ev,
            onDismiss = { detailEvent = null },
            onFinishActivity = onFinishActivity,
            onShowOnMap = { e -> detailEvent = null; focus.event(e.id) },
            onOpenChat = onOpenChat,
        )
    }
}

/**
 * Şəhər həbı (D-055) — salamlaşma sətirində sağda; CityPickerSheet-i açır.
 * setCity əvvəl yalnız testlərdən çatışıldığı üçün ölü idi — bu, 25 şəhəri
 * istifadəçiyə açır (feed/map/aktiv tədbirlər şəhərə görə filtrlənir).
 */
@Composable
private fun CityPill(city: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scaleTap(interaction)
            .androidxLiveTap(interaction, onClick)
            .background(Q.card, RoundedCornerShape(Q.rPill))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rPill))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Filled.LocationOn, null, tint = Q.primary, modifier = Modifier.height(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(city, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 12.sp, fontWeight = FontWeight.W800))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = Q.mutedFg, modifier = Modifier.height(16.dp))
    }
}

/**
 * AI tövsiyə kartı (D-056) — "AI seçimi" zolağı üçün; AiBox-a yaxın yaşıl çalar.
 */
@Composable
private fun AiPickCard(event: QoshulEvent, reason: String, joined: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(232.dp)
            .androidxLiveTap(interaction, onClick)
            .background(Q.aiBoxBg, RoundedCornerShape(Q.rLg.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Q.primary, modifier = Modifier.height(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("AI tövsiyəsi", style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 10.5.sp, fontWeight = FontWeight.W800, color = Q.primary))
        }
        Spacer(Modifier.height(8.dp))
        // D-057: 14.5sp — 232dp kartda 2 sətirə rahat yerləşir (16sp-də
        // başlıq ortadan ellipsizə olunurdu — VLM audit tapıntısı)
        Text(event.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = JakartaSans, fontSize = 14.5.sp, fontWeight = FontWeight.W800))
        Spacer(Modifier.height(6.dp))
        Text(reason, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = T.meta.copy(lineHeight = 17.sp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = if (joined) Q.primary else Q.mutedFg, modifier = Modifier.size(13.5.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (joined) "Qoşulsan · ${event.time}" else "${event.date} · ${event.time}",
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 10.5.sp, fontWeight = FontWeight.W500,
                    color = if (joined) Q.primary else Q.mutedFg),
            )
        }
    }
}

/**
 * Aktiv tədbir kartı (D-054) — bugünkü tədbir üçün üfüqi zolaq kartı:
 * şəkil + yaşıl nöqtəli "CANLI" nişanı (D-057: LiveBadge tək mənbədən),
 * ad, məkan, saat, iştirak vəziyyəti. D-055: klik → tədbir DETALI.
 */
@Composable
private fun LiveEventCard(event: QoshulEvent, joined: Boolean, pending: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(248.dp)
            .androidxLiveTap(interaction, onClick)
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .androidxLiveBorder()
    ) {
        // — şəkil + CANLI nişanı (D-057: Ui.LiveBadge — inline nüsxə silindi) —
        Box(Modifier.fillMaxWidth().height(92.dp)) {
            Image(
                painter = imagePaint(event.image), contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(92.dp),
            )
            Box(Modifier.fillMaxWidth().height(92.dp).androidxLiveShade())
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                LiveBadge(text = "CANLI")
            }
        }
        // — məlumat —
        Column(Modifier.padding(12.dp)) {
            Text(event.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = T.eventTitle)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = Q.mutedFg, modifier = Modifier.size(13.5.dp))
                Spacer(Modifier.width(6.dp))
                Text(event.location, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = T.meta, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = Q.mutedFg, modifier = Modifier.size(13.5.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    when {
                        joined -> "Qoşulsan · ${event.time}"
                        pending -> "Təsdiq gözləyir · ${event.time}"
                        else -> "Bu gün · ${event.time} · ${event.spotsLeft} yer qalıb"
                    },
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = JakartaSans, fontSize = 10.5.sp,
                        fontWeight = if (joined) FontWeight.W700 else FontWeight.W500,
                        color = if (joined) Q.primary else Q.mutedFg),
                )
            }
        }
    }
}

private fun Modifier.androidxLiveTap(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
)

private fun Modifier.androidxLiveBorder(): Modifier =
    this.then(Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp)))

private fun Modifier.androidxLiveShade(): Modifier = this.then(
    Modifier.background(
        androidx.compose.ui.graphics.Brush.verticalGradient(
            0.55f to androidx.compose.ui.graphics.Color.Transparent,
            1.0f to Q.photoShade.copy(alpha = 0.5f),
        )
    )
)

/** Endirim yalı kartı (108px, D-035 ölçüsü). */
@Composable
private fun OfferMiniCard(label: String, venueLine: String, onClick: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(230.dp)
            .height(108.dp)
            .androidxTap(interaction, onClick)
            .background(Q.card, RoundedCornerShape(Q.rLg.dp))
            .androidxBorder()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Percent, null, tint = Q.accentFg, modifier = Modifier.height(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 14.5.sp, fontWeight = FontWeight.W800))
        }
        Spacer(Modifier.height(6.dp))
        Text(venueLine, maxLines = 2, overflow = TextOverflow.Ellipsis, style = T.meta)
    }
}

private fun Modifier.androidxTap(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
)

private fun Modifier.androidxBorder(): Modifier =
    this.then(Modifier.border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp)))

/** collectAsStateWithLifecycle — lifecycle-runtime-compose olmadan (D-036: asılılıq yox). */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycle(): androidx.compose.runtime.State<T> =
    this.collectAsState()
