package az.qoshul.app.features.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.engine.AppUiState
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
import az.qoshul.app.core.ui.CommunityBanner
import az.qoshul.app.core.ui.PersonMiniCard
import az.qoshul.app.core.ui.XpTrack
import az.qoshul.app.features.events.EventsScreen

/**
 * Kəşf (ANA EKRAN) — Flutter feed_screen.dart portu (ROL TAB 1, D-034):
 * "Sənin üçün | Tədbirlər" daxili tabları; axtarış pinned (D-029).
 * D-050: bütün kart bağlantıları canlıdır (məkan → detal, insan → Match,
 * endirim → Kuponlar) — ölü onClick-lər aradan qaldırıldı.
 */
@Composable
fun FeedScreen(engine: AppEngine, nav: NavState, focus: MapFocusState) {
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
                ForYouView(engine, nav, search = search, onSearchChange = { search = it },
                    onOpenEvents = { view = 1 })
            } else {
                EventsScreen(engine, nav, focus)
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
            .background(Q.muted, RoundedCornerShape(13.dp))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, (icon, activeIcon, label) ->
            val isActive = i == active
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(if (isActive) Q.card else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
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
    search: String,
    onSearchChange: (String) -> Unit,
    onOpenEvents: () -> Unit,
) {
    val st by engine.state.collectAsStateWithLifecycle()

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
            // salamlaşma
            item {
                StaggerIn(0) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("Salam, ${st.firstName}!", style = T.h1)
                            Spacer(Modifier.height(4.dp))
                            Text("Bu gün yeni bir hekayəyə qoşul.", style = T.subtitle)
                        }
                        RankBadge(label = st.rank)
                    }
                }
            }
            item {
                Spacer(Modifier.height(17.dp))
                StaggerIn(1) { XpTrack(xp = st.xp) }
                Spacer(Modifier.height(10.dp))
            }
            // "Bu axşam" körpüsü
            val tonightCount = engine.cityEvents.count { it.date == "Bu gün" }
            if (tonightCount > 0) {
                item {
                    StaggerIn(2) {
                        CommunityBanner(
                            icon = Icons.Filled.NightlightRound,
                            title = "Bu axşam $tonightCount tədbir var",
                            subtitle = "Görüşlər, idman, sənət — hamısı Tədbirlər bölməsində.",
                            onClick = onOpenEvents,
                            trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                        )
                    }
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
                        trailing = { SectionLink(text = "${venues.size} məkan") },
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
                    Spacer(Modifier.height(16.dp))
                }
            }
            // — cəmiyyət banneri —
            item {
                CommunityBanner(
                    icon = Icons.Filled.School,
                    title = "Eyni maraqlar, yeni tanışlıqlar",
                    subtitle = "Sevdiyin məkanlarda görüş, əlavə XP qazan.",
                    trailing = { Icon(Icons.Filled.NorthEast, null, tint = Q.primary, modifier = Modifier.height(20.dp)) },
                )
            }
            item { SafetyNote() }
        }
    }
}

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
