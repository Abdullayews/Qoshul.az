package az.qoshul.app.features.feed

import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import az.qoshul.app.core.motion.pressable
import az.qoshul.app.core.motion.scaleTap
import az.qoshul.app.core.ui.LiveBadge
import az.qoshul.app.features.events.CityPickerSheet
import az.qoshul.app.features.events.EventDetailSheet
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.engine.AppUiState
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.motion.StaggerIn
import az.qoshul.app.core.theme.OpenSans
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.EmptyState
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.SearchBarField
import az.qoshul.app.core.ui.SectionHeading
import az.qoshul.app.core.ui.SectionLink
import az.qoshul.app.core.ui.SafetyNote
import az.qoshul.app.core.ui.VenueMiniCard
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.PersonMiniCard
import az.qoshul.app.features.events.EventsScreen

/**
 * Kəşf (ANA EKRAN) — Flutter feed_screen.dart portu (ROL TAB 1, D-034):
 * "Sənin üçün | Tədbirlər" daxili tabları; axtarış pinned (D-029).
 * D-115 (v0.41.0, direktiv 2026-11-09: „ana ekranda salam kimsə və xp
 * kimi olan barı sil"): salamlaşma bloku („Salam, Kimsə!“ + rütbə
 * nişanı + alt yazı) və XP trekı LƏĞV — axın birbaşa aktiv
 * tədbirlərlə açılır; şəhər həbi axtarış sətrinə keçir (funksiya
 * itmir). onViewChange parametri D-130-də ləğv edildi — AI FAB artıq
 * görünüşə deyil, yalnız tab-a bağlıdır (xəritə/söhbət xaricində).
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

    // U1.1 (UI-03 pattern): axtarış mətni doludursa GERİ əvvəlcə onu təmizləyir
    // (tab rejimində back app-i bağlayırdı — axtarış konteksti udulurdu).
    BackHandler(enabled = search.isNotEmpty()) { search = "" }

    // D-125 (direktiv: „ana menyuda şəhər və tabların olduğu yeri
    // sürüşdürərkən sabit yox adi qalacağı moda gətir“): tab zolağı və
    // axtarış+şəhər sətri SABİT deyil — hər görünüşün LazyColumn-unun İLK
    // elementləridir, məzmunla birgə sürüşür (D-029 pinned-axtarış dili
    // ləğv; tab keçidi eyni fade-through qalır — D-113).
    AnimatedContent(
        targetState = view,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            // D-113 (direktiv: „tablar arası keçiş animasiyasını
            // dəyişdir“): spring-slide əvəzinə SÜRƏTLİ FADE — köhnə
            // görüntü 150ms-də yox olur (yavaş spring-də köhnə səhifə
            // gözlədirdi); GİRİŞ 220ms ease ilə yumşaq qalır.
            (fadeIn(tween(220, easing = Motion.ease)))
                .togetherWith(fadeOut(tween(150)))
        },
        label = "feedTabs",
    ) { v ->
        val tabs: @Composable () -> Unit = {
            ViewTabs(
                active = v,
                onChanged = { view = it },
                horizontal = 0.dp,
            )
        }
        if (v == 0) {
            ForYouView(engine, nav, focus, search = search, onSearchChange = { search = it },
                onOpenEvents = { view = 1 }, onFinishActivity = onFinishActivity, onOpenChat = onOpenChat,
                tabs = tabs)
        } else {
            EventsScreen(engine, nav, focus, onFinishActivity = onFinishActivity, onOpenChat = onOpenChat,
                tabs = tabs)
        }
    }
}

/** Daxili tab zolağı — "Sənin üçün | Tədbirlər" (Instagram-vari üst tablar).
 *  D-090: arxa boz track SİLİNDİ (100% şəffaf) — aktiv ağ kapsul qalır,
 *  pill-lərin arasındakı "boz qalıntı" artıq görünmür.
 *  D-125: [horizontal] — LC contentPadding içində olduqda 0dp (ötürülür). */
@Composable
private fun ViewTabs(active: Int, onChanged: (Int) -> Unit, horizontal: Dp = 20.dp) {
    val tabs = listOf(
        Triple(Icons.Outlined.AutoAwesome, Icons.Rounded.AutoAwesome, "Sənin üçün"),
        Triple(Icons.Outlined.Event, Icons.Rounded.Event, "Tədbirlər"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = horizontal, end = horizontal, top = 10.dp, bottom = 6.dp)
            .background(androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(Q.rPill))
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
                        tint = if (isActive) Q.primaryText else Q.mutedFg,
                        modifier = Modifier.height(17.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        label, maxLines = 1,
                        style = T.h3.copy(fontWeight = if (isActive) FontWeight.W800 else FontWeight.W500, color = if (isActive) Q.primaryText else Q.mutedFg),
                    )
                }
            }
        }
    }
}

/** "Sənin üçün" görünüşü — Instagram-paradigmalı axın.
 *  D-125: [tabs] — üst tab zolağı məzmunla birgə sürüşür (pinned deyil). */
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
    tabs: @Composable () -> Unit,
) {
    val st by engine.state.collectAsStateWithLifecycle()
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }
    var cityOpen by remember { mutableStateOf(false) }
    // D-056: AI seçimi — şəhər dəyişəndə yenidən hesablanır (LazyColumn
    // daxilində remember çağırmaq olmaz — @Composable konteksti deyil)
    val aiPicks = remember(st.city) { engine.aiRecommendations() }

    // U2.4 (UI-44): axtarış filtrləri — yalnız (axtarış, şəhər) dəyişəndə
    // yenidən hesablanır; hər hərf üçün bütün „Sənin üçün“ görünüşü
    // recompose olmur (audit UI-44).
    val venues = remember(search, st.city) {
        engine.partners.filter { v ->
            v.city == st.city && (search.isEmpty() ||
                "${v.name} ${v.category} ${v.address}".lowercase().contains(search.lowercase()))
        }
    }
    val people = remember(search) {
        engine.matchCandidates.filter { p ->
            search.isEmpty() || "${p.firstName} ${p.lastName}".lowercase().contains(search.lowercase())
        }
    }

    // D-126 (direktiv: „bütün Azərbaycan şəhərləri ... ana menyudakı axtarışda,
    // A-Z sıralı“): axtarış mətni şəhər adına tuş olanda üst 5 şəhər nəticəsi
    // göstərilir (mənbə siyahısı AzCollation A-Z sıralıdır); seçəndə
    // engine.setCity — məkan/insan bölmələri yeni şəhərə keçir, şəhər həbi
    // yenilənir, axtarış təmizlənir.
    val cityHits = remember(search) {
        if (search.isBlank()) emptyList()
        else engine.dataSource.cities.filter {
            it.name.lowercase().contains(search.lowercase())
        }.take(5)
    }

    // D-125: tablar + axtarış + şəhər həbi məzmunla BİRGƏ sürüşür — sabit
    // başlıq bloku YOX (köhnə Column+pinned-Row ləğv; axtarışın üstündəki
    // 6+10dp ritm axtarış sətrinin bottom-16dp-də qorunub).
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 0.dp, bottom = 132.dp
        ),
    ) {
        item(key = "feedTabs") { tabs() }
        item(key = "feedSearch") {
            // — axtarış + şəhər həbi (D-115: şəhər seçimi axtarış sətirində) —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    SearchBarField(
                        value = search, onValueChange = onSearchChange,
                        hint = "Məkan, insan və ya şəhər axtar...",
                    )
                }
                Spacer(Modifier.width(10.dp))
                CityPill(city = st.city, onClick = { cityOpen = true })
            }
        }
        // — D-126: Şəhərlər bölməsi (axtarışa uyğun şəhərlər, A-Z) —
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
                            onSearchChange("")
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
                        items(active, key = { it.id }, contentType = { "event" }) { e ->
                            LiveEventCard(
                                event = e, joined = e.id in st.joined, pending = e.id in st.pending,
                                onClick = { detailEvent = e },
                            )
                        }
                    }
                    // D-063: karusel→bölmə 16dp (heading top 4 ilə = 20dp vahid ritm)
                    Spacer(Modifier.height(16.dp))
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
                        items(aiPicks, key = { it.first.id }, contentType = { "ai-pick" }) { (e, reason) ->
                            AiPickCard(event = e, reason = reason, joined = e.id in st.joined,
                                onClick = { detailEvent = e })
                        }
                    }
                    // D-063: 20→16dp (vahid ritm)
                    Spacer(Modifier.height(16.dp))
                }
            }
            // — Populyar məkanlar — (filtr yuxarıda, ForYouView gövdəsində — U2.4)
            if (venues.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = "Populyar məkanlar", icon = Icons.Filled.Storefront,
                        // D-057: sayğac — saxta yaşıl link YOX (kliklənə bilməz görünürdü)
                        trailing = {
                            Text("${venues.size} məkan", style = T.chipXs.copy(fontWeight = FontWeight.W600, color = Q.mutedFg))
                        },
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(venues, key = { it.id }, contentType = { "venue" }) { v ->
                            VenueMiniCard(
                                imageKey = v.image, name = v.name, category = v.category,
                                priceRange = v.priceRange, rating = v.rating,
                                discount = v.discountPercent,
                                onClick = { nav.push(Route.VenueDetail(v.id)) },
                            )
                        }
                    }
                    // D-063: 20→16dp (vahid ritm)
                    Spacer(Modifier.height(16.dp))
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
            // (people filtr də yuxarıda hesablandı — U2.4)
            if (people.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        text = if (search.isEmpty())
                            "Yaxınlıqda yeni insan yoxdur.\nGörüşlərə qoşul, tanış ol!"
                        else "„$search“ üzrə insan tapılmadı.",
                    )
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(people, key = { it.id }, contentType = { "person" }) { p ->
                            PersonMiniCard(
                                imageKey = p.image, name = p.firstName, age = p.age,
                                distanceLabel = if (st.showDistance) p.distanceLabel else p.city,
                                matchPercent = p.match,
                                onClick = { nav.push(Route.Match) },
                            )
                        }
                    }
                    // D-063: 20→16dp (vahid ritm)
                    Spacer(Modifier.height(16.dp))
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
                        items(discountOffers, key = { it.id }, contentType = { "offer" }) { o ->
                            OfferMiniCard(
                                label = o.label,
                                venueLine = if (o.autoByLevel) "${o.venueName} · səviyyən ilə açıq"
                                else "${o.venueName} · ${o.xpPrice} XP",
                                onClick = { nav.push(Route.Coupons) },
                            )
                        }
                    }
                    // D-063: 20→16dp (vahid ritm)
                    Spacer(Modifier.height(16.dp))
                }
            }
            // — cəmiyyət banneri (D-055: ölü banner deyil — Tərəfdaşlara aparır) —
            item {
                CommunityBanner(
                    icon = Icons.Filled.School,
                    title = "Eyni maraqlar, yeni tanışlıqlar",
                    subtitle = "Sevdiyin məkanlarda görüş, əlavə XP qazan.",
                    onClick = { nav.push(Route.Partners) },
                    trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primaryText, modifier = Modifier.height(20.dp)) },
                )
            }
            item { SafetyNote() }
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
        Icon(Icons.Filled.LocationOn, null, tint = Q.primaryText, modifier = Modifier.height(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(city, style = T.metaBold)
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
            // D-063: kart daxili 13→14dp (vahid kart padding-i)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Q.primaryText, modifier = Modifier.height(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("AI tövsiyəsi", style = T.badge.copy(fontWeight = FontWeight.W800, color = Q.primaryText))
        }
        Spacer(Modifier.height(8.dp))
        // D-057: 14.5sp — 232dp kartda 2 sətirə rahat yerləşir (16sp-də
        // başlıq ortadan ellipsizə olunurdu — vizual audit tapıntısı)
        // D-063: minLines=2 — başlıq 1 sətir olsa da kartlar eyni hündür qalır
        Text(event.title, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
            style = T.personName.copy(fontWeight = FontWeight.W800))
        Spacer(Modifier.height(6.dp))
        // D-063: minLines=2 — səbəb sətri də vahid hündürlük verir
        Text(reason, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
            style = T.meta.copy(lineHeight = 17.sp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = if (joined) Q.primaryText else Q.mutedFg, modifier = Modifier.size(13.5.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (joined) "Qoşulsan · ${event.time}" else "${event.date} · ${event.time}",
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = T.metaSm.copy(fontWeight = FontWeight.W500, color = if (joined) Q.primaryText else Q.mutedFg),
            )
        }
    }
}

/**
 * Aktiv tədbir kartı (D-054) — bugünkü tədbir üçün üfüqi zolaq kartı:
 * şəkil + yaşıl nöqtəli "Aktiv" nişanı (D-057: LiveBadge tək mənbədən;
 * UI-7 D-098: "CANLI" → "Aktiv" — istifadəçi direktivi 2026-11-08),
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
        // — şəkil + Aktiv nişanı (D-057: Ui.LiveBadge; UI-7 D-098: CANLI→Aktiv) —
        Box(Modifier.fillMaxWidth().height(92.dp)) {
            Image(
                painter = imagePaint(event.image), contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(92.dp),
            )
            Box(Modifier.fillMaxWidth().height(92.dp).androidxLiveShade())
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                LiveBadge(text = "Aktiv")
            }
        }
        // — məlumat —
        Column(Modifier.padding(12.dp)) {
            // D-063: minLines=2 — başlıq 1 sətir olsa da kartlar eyni hündür
            // (dpi ekranda qeyri-bərabər karusel görünürdü — vizual audit sev 4)
            Text(event.title, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, style = T.eventTitle)
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
                    style = T.metaSm.copy(fontWeight = if (joined) FontWeight.W700 else FontWeight.W500, color = if (joined) Q.primaryText else Q.mutedFg),
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
            // U2 (UI-28): sabit 108dp — fontScale 1.8+ mətni kəsirdi;
            // min hədd: normal ölçüdə D-035 (108dp) qalır, böyük fontda artır.
            .heightIn(min = 108.dp)
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
                style = T.personName.copy(fontWeight = FontWeight.W800))
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
