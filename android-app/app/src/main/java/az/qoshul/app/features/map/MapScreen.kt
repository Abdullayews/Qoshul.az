package az.qoshul.app.features.map

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.glass.GlassPill
import az.qoshul.app.core.glass.GlassSurface
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.theme.JakartaSans
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.imagePaint
import az.qoshul.app.features.events.EventDetailSheet
import az.qoshul.app.features.venues.ReserveSheet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

/**
 * Xəritə tabı (ROL TAB 2, D-027/D-030/D-048): MapLibre Compose 0.20 +
 * OpenFreeMap "liberty" vektor stili — pulsuz, API açarı və hesablaşma YOX
 * (D-038-in "ağır SDK" rəddi 2026 FFI nəsli ilə köhnəldi: nüvə .so cəmi
 * ~1 MB/ABI). GPU vektor render, hamar pinch/pan/fırlanma, kamera animasiyası.
 *
 * Pinlər stil qatlarındadır (Circle + Symbol): məkan = yaşal dot + storefront
 * qlifi, tədbir = narıncı dot + təqvim qlifi, insan = adın baş hərfi.
 * Seçim halosu, boş toxunma = seçimi buraxır. OSM atribusiyası saxlanılıb.
 */
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Liberty stilinin qlif şrifti (initial hərflər üçün). */
private const val STYLE_TEXT_FONT = "Noto Sans Regular"

/** Xəritə pin modeli. */
sealed interface MapPin {
    val lat: Double
    val lng: Double
    data class Venue(val v: Partner) : MapPin { override val lat = v.lat; override val lng = v.lng }
    data class Event(val e: QoshulEvent) : MapPin { override val lat = e.lat; override val lng = e.lng }
    data class PersonP(val p: Person) : MapPin { override val lat = p.mapLat; override val lng = p.mapLng }
}

/** Qat kliklərində pini tapmaq üçün stabil açar. */
private fun MapPin.key(): String = when (this) {
    is MapPin.Venue -> "v${v.id}"
    is MapPin.Event -> "e${e.id}"
    is MapPin.PersonP -> "p${p.id}"
}

/** Pin siyahısından GeoJSON feature kolleksiyası. */
private fun pinFeatures(pins: List<MapPin>): FeatureCollection<Point, JsonObject> = FeatureCollection(
    pins.map { pin ->
        Feature(
            geometry = Point(Position(longitude = pin.lng, latitude = pin.lat)),
            properties = buildJsonObject {
                put("key", pin.key())
                if (pin is MapPin.PersonP) put("initial", pin.p.firstName.take(1))
            },
        )
    },
)

@Composable
fun MapScreen(engine: AppEngine, nav: NavState, focus: MapFocusState) {
    val st by engine.state.collectAsState()
    var category by remember { mutableStateOf("Hamısı") }
    var distanceKm by remember { mutableStateOf(10) }
    var openNow by remember { mutableStateOf(false) }
    var withDiscount by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MapPin?>(null) }
    var reserveVenue by remember { mutableStateOf<Partner?>(null) }
    var detailEvent by remember { mutableStateOf<QoshulEvent?>(null) }
    var pendingCamera by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val hour = remember { java.time.LocalDateTime.now().hour }

    val myLat = 40.4093
    val myLng = 49.8671

    // — pinlər (filtrlərlə, D-027 görünüş rejimi) —
    val venuePins = engine.partners.filter { v ->
        v.city == st.city && (category == "Hamısı" || v.category == category) &&
            (!openNow || v.isOpenAt(hour)) && (!withDiscount || v.hasDiscount)
    }.map { MapPin.Venue(it) }
    val eventPins = engine.cityEvents.filter {
        (category == "Hamısı" || it.category == category) && it.distance <= distanceKm
    }.map { MapPin.Event(it) }
    val peoplePins = engine.matchCandidates
        .filter { it.distanceKm <= distanceKm }
        .filter {
            st.mapVisibility == MapVisibility.EVERYONE ||
                (st.mapVisibility == MapVisibility.FRIENDS && st.friendships[it.id] == "accepted")
        }.map { MapPin.PersonP(it) }
    val pins = venuePins + eventPins + peoplePins

    // Robolectric (JVM test): FFI .so yüklənə bilməz → xəritə səthi yoxdur;
    // UI smoke-testləri qalan qatları (çiplər, kartlar) yoxlayır.
    val isJvmTest = remember { Build.FINGERPRINT.contains("robolectric", ignoreCase = true) }

    val scope = rememberCoroutineScope()

    // — D-050: xəritə fokus istəyi ("Yol göstər"/"Yol tap") — pin seç + kameraya al
    LaunchedEffect(focus.request) {
        when (val t = focus.request) {
            is MapFocusState.Target.Venue ->
                venuePins.find { it.v.id == t.venueId }?.let {
                    selected = it; pendingCamera = it.lng to it.lat
                }
            is MapFocusState.Target.Event ->
                eventPins.find { it.e.id == t.eventId }?.let {
                    selected = it; pendingCamera = it.lng to it.lat
                }
            null -> Unit
        }
        focus.consume()
    }

    Box(Modifier.fillMaxSize().background(Q.tileFallback)) {
        if (!isJvmTest) {
            val mapState = rememberMapState(
                baseStyle = BaseStyle.Uri(MAP_STYLE_URL),
                initialCameraPosition = CameraPosition(
                    center = Position(longitude = myLng, latitude = myLat),
                    zoom = 13.0,
                ),
            ) {
                QoshulPinLayers(
                    pins = pins,
                    myLat = myLat,
                    myLng = myLng,
                    selectedKey = selected?.key(),
                    onPinTap = { selected = it },
                )
            }

            // D-050: fokus istəyi → kameranı pinə uçur (zoom ≥ 15)
            LaunchedEffect(pendingCamera) {
                val pos = pendingCamera ?: return@LaunchedEffect
                mapState.animateCamera(
                    CameraUpdate(
                        center = Position(longitude = pos.first, latitude = pos.second),
                        zoom = maxOf(mapState.cameraPosition.zoom, 15.0),
                    ),
                    CameraAnimation.Ease.Standard,
                )
                pendingCamera = null
            }

            // — REAL xəritə: MapLibre vektor render (D-048) —
            MaplibreMap(
                state = mapState,
                modifier = Modifier.fillMaxSize(),
                uiOptions = MapUiOptions { loadColor = Q.tileFallback },
                interactions = MapInteractions {
                    callbacks {
                        click {
                            // heç bir qat tutmadı → seçimi burax
                            onUnhandled { selected = null; ClickResult.Pass }
                        }
                    }
                },
            )

            // — zoom həbləri (şüşə, D-030) — kamera animasiyası ilə —
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 170.dp, end = 12.dp)
            ) {
                GlassPill(dense = true, horizontal = 7.dp, vertical = 7.dp) {
                    Icon(Icons.Filled.Add, "Yaxınlaşdır", tint = Q.foreground,
                        modifier = Modifier.size(16.dp).clickable {
                            scope.launch {
                                mapState.animateCamera(
                                    CameraUpdate(zoom = (mapState.cameraPosition.zoom + 1.0).coerceAtMost(18.0)),
                                    CameraAnimation.Ease.Standard,
                                )
                            }
                        })
                }
                Spacer(Modifier.height(8.dp))
                GlassPill(dense = true, horizontal = 7.dp, vertical = 7.dp) {
                    Icon(Icons.Filled.Remove, "Uzaqlaşdır", tint = Q.foreground,
                        modifier = Modifier.size(16.dp).clickable {
                            scope.launch {
                                mapState.animateCamera(
                                    CameraUpdate(zoom = (mapState.cameraPosition.zoom - 1.0).coerceAtLeast(3.0)),
                                    CameraAnimation.Ease.Standard,
                                )
                            }
                        })
                }
            }

            // — OSM atribusiyası (istifadə qaydası — məcburi kredit) —
            // D-053 fix: atribusiya nav pill-in arxasında gizlənirdi — yuxarı qalxdı
            Text(
                "© OpenStreetMap contributors",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = JakartaSans, fontSize = 8.5.sp, color = Q.mutedFg),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 6.dp, bottom = 96.dp)
                    .background(Q.card.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        // — şüşə filtr paneli (D-025 səth #2) —
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(16.dp),
            sigma = 16.dp,
            luminance = 0.5f,
        ) {
            Column(Modifier.padding(10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(
                        "Hamısı" to Icons.Filled.Public, "İdman" to Icons.Filled.SportsVolleyball,
                        "Qəhvə" to Icons.Filled.LocalCafe, "Texnologiya" to Icons.Filled.Memory,
                        "Piknik" to Icons.Filled.Park,
                    ).forEach { (name, icon) ->
                        GlassFilterChip(name, icon, category == name) { category = name }
                        Spacer(Modifier.width(7.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(1, 3, 5, 10).forEach { km ->
                        GlassFilterChip("$km km", Icons.Filled.NearMe, distanceKm == km) { distanceKm = km }
                        Spacer(Modifier.width(7.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    GlassFilterChip("indi açıqdır", Icons.Filled.Schedule, openNow) { openNow = !openNow }
                    Spacer(Modifier.width(7.dp))
                    GlassFilterChip("endirimi var", Icons.Filled.Percent, withDiscount) { withDiscount = !withDiscount }
                }
            }
        }

        // — sayğac (şüşə həb) —
        // D-053 fix: nav pill zone-unun üstünə qaldırıldı (əvvəl arxasında gizlənirdi)
        GlassPill(
            modifier = Modifier.align(Alignment.BottomStart)
                .navigationBarsPadding().padding(start = 12.dp, bottom = 96.dp),
            dense = true, horizontal = 9.dp, vertical = 6.dp,
        ) {
            Text(
                "${venuePins.size} məkan · ${eventPins.size} tədbir · ${peoplePins.size} insan",
                style = T.glassText(size = 10, w = FontWeight.W600),
            )
        }

        // — D-027: görünüş rejimi həbi —
        if (st.mapVisibility != MapVisibility.EVERYONE) {
            GlassPill(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .navigationBarsPadding().padding(end = 12.dp, bottom = 96.dp),
                dense = true, horizontal = 9.dp, vertical = 6.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (st.mapVisibility == MapVisibility.FRIENDS) Icons.Filled.Group
                        else Icons.Filled.VisibilityOff, null,
                        tint = Q.primary, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (st.mapVisibility == MapVisibility.FRIENDS) "Yalnız dostlar görünür" else "Gizli rejim",
                        style = T.glassText(size = 10, w = FontWeight.W600),
                    )
                }
            }
        }

        // — seçilmiş pinin alt kartı (D-025 səth #3) —
        // D-053 fix: kart nav pill-in arxasında qalırdı — "Ətraflı"/"Rezerv et"
        // düymələri basıla bilmirdi. Pill zone = navbar inset + 10 + 68dp;
        // kart inset + 96dp-dən yuxarı başlayır (18dp boşluq, hər navbar növündə).
        selected?.let { pin ->
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 96.dp),
                shape = RoundedCornerShape(Q.rXl.dp),
                sigma = 18.dp,
                luminance = 0.58f,
            ) {
                BottomCardContent(
                    pin = pin,
                    onVenueDetail = { nav.push(Route.VenueDetail(it.id)); selected = null },
                    onReserve = { reserveVenue = it },
                    onEventDetail = { detailEvent = it },
                    onMeetPerson = { nav.push(Route.Match); selected = null },
                )
            }
        }
    }

    // — alt kartın açdığı sheetlər (D-050: ölü düymələr canlandı) —
    reserveVenue?.let { v ->
        ReserveSheet(engine, v, onDismiss = { reserveVenue = null })
    }
    detailEvent?.let { ev ->
        EventDetailSheet(
            engine, ev,
            onDismiss = { detailEvent = null },
            onShowOnMap = { e -> detailEvent = null; focus.event(e.id) },
        )
    }
}

/**
 * Pin qatları — MapLibre stil kompozisiyası (rememberMapState content).
 *
 * Render ardıcıllığı (elan sırası): seçim halosu → məkan dot+qlif →
 * tədbir dot+qlif → insan dot+baş hərf → "mənim yerim" dotu.
 */
@Composable
private fun QoshulPinLayers(
    pins: List<MapPin>,
    myLat: Double,
    myLng: Double,
    selectedKey: String?,
    onPinTap: (MapPin) -> Unit,
) {
    val pinByKey = remember(pins) { pins.associateBy { it.key() } }

    // Qat klikləri: feature "key" xassəsindən pin tapılır
    val pinClick: FeaturesClickHandler = { feats ->
        feats.firstNotNullOfOrNull { f ->
            pinByKey[f.properties?.get("key")?.jsonPrimitive?.contentOrNull]
        }?.let(onPinTap)
        ClickResult.Consume
    }

    val venues = pins.filterIsInstance<MapPin.Venue>()
    val events = pins.filterIsInstance<MapPin.Event>()
    val people = pins.filterIsInstance<MapPin.PersonP>()

    val allSource = rememberGeoJsonSource(
        remember(pins) { GeoJsonData.Features(pinFeatures(pins)) },
    )
    val venueSource = rememberGeoJsonSource(
        remember(venues) { GeoJsonData.Features(pinFeatures(venues)) },
    )
    val eventSource = rememberGeoJsonSource(
        remember(events) { GeoJsonData.Features(pinFeatures(events)) },
    )
    val peopleSource = rememberGeoJsonSource(
        remember(people) { GeoJsonData.Features(pinFeatures(people)) },
    )
    val meSource = rememberGeoJsonSource(
        remember(myLat, myLng) {
            GeoJsonData.Features(
                FeatureCollection(
                    listOf(
                        Feature(
                            geometry = Point(Position(longitude = myLng, latitude = myLat)),
                            properties = null,
                        ),
                    ),
                ),
            )
        },
    )

    // — seçilmiş pinin halosu —
    if (selectedKey != null) {
        CircleLayer(
            id = "qoshul-selected-halo",
            source = allSource,
            filter = feature["key"] eq const(selectedKey),
            radius = const(24.dp),
            color = const(Color.White.copy(alpha = 0.30f)),
            blur = const(0.65f),
        )
    }

    // — məkan pinləri (yaşal dot + storefront qlifi) —
    if (venues.isNotEmpty()) {
        CircleLayer(
            id = "qoshul-venue-dot",
            source = venueSource,
            radius = const(17.dp),
            color = const(Q.primary),
            strokeColor = const(Color.White),
            strokeWidth = const(2.5.dp),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
        SymbolLayer(
            id = "qoshul-venue-glyph",
            source = venueSource,
            iconImage = image(
                rememberVectorPainter(Icons.Filled.Storefront),
                size = DpSize(16.dp, 16.dp),
                colorFilter = ColorFilter.tint(Color.White),
            ),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
    }

    // — tədbir pinləri (narıncı dot + təqvim qlifi) —
    if (events.isNotEmpty()) {
        CircleLayer(
            id = "qoshul-event-dot",
            source = eventSource,
            radius = const(17.dp),
            color = const(Q.accent),
            strokeColor = const(Color.White),
            strokeWidth = const(2.5.dp),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
        SymbolLayer(
            id = "qoshul-event-glyph",
            source = eventSource,
            iconImage = image(
                rememberVectorPainter(Icons.Filled.Event),
                size = DpSize(16.dp, 16.dp),
                colorFilter = ColorFilter.tint(Color.White),
            ),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
    }

    // — insan pinləri (tünd dot + adın baş hərfi) —
    if (people.isNotEmpty()) {
        CircleLayer(
            id = "qoshul-people-dot",
            source = peopleSource,
            radius = const(17.dp),
            color = const(Q.foreground),
            strokeColor = const(Color.White),
            strokeWidth = const(2.5.dp),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
        SymbolLayer(
            id = "qoshul-people-initial",
            source = peopleSource,
            textField = feature["initial"].asString(),
            textFont = const(listOf(STYLE_TEXT_FONT)),
            textSize = const(13.sp),
            textAnchor = const(SymbolAnchor.Center),
            textColor = const(Color.White),
            textAllowOverlap = const(true),
            textIgnorePlacement = const(true),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
    }

    // — "mənim yerim" (üst qat) —
    CircleLayer(
        id = "qoshul-me-dot",
        source = meSource,
        radius = const(10.dp),
        color = const(Q.myLocation),
        strokeColor = const(Color.White),
        strokeWidth = const(3.dp),
    )
}

/** Alt kart məzmunu — pin tipinə görə (D-050: bütün düymələr canlıdır). */
@Composable
private fun BottomCardContent(
    pin: MapPin,
    onVenueDetail: (Partner) -> Unit,
    onReserve: (Partner) -> Unit,
    onEventDetail: (QoshulEvent) -> Unit,
    onMeetPerson: () -> Unit,
) {
    when (pin) {
        is MapPin.Venue -> {
            val v = pin.v
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = imagePaint(v.image), contentDescription = v.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(Q.rMd.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(v.name, style = T.glassText(size = 14))
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = Q.accent, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(String.format("%.1f · %s", v.rating, v.priceRange), style = T.meta)
                            if (v.hasDiscount) {
                                Spacer(Modifier.width(6.dp))
                                Text("-${v.discountPercent}%", style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = JakartaSans, fontSize = 11.5.sp,
                                    fontWeight = FontWeight.W800, color = Q.accentFg))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(v.address, style = T.metaSm)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    BottomCardButton("Ətraflı", primary = false, onClick = { onVenueDetail(v) })
                    Spacer(Modifier.width(8.dp))
                    BottomCardButton("Rezerv et", primary = true, onClick = { onReserve(v) })
                }
            }
        }
        is MapPin.Event -> {
            val e = pin.e
            Column(Modifier.padding(14.dp)) {
                Text(e.title, style = T.glassText(size = 14))
                Spacer(Modifier.height(4.dp))
                Text("${e.category} · ${e.date}, ${e.time} · ~${e.distance} km", style = T.meta)
                Spacer(Modifier.height(12.dp))
                BottomCardButton("Ətraflı", primary = true, onClick = { onEventDetail(e) }, fill = true)
            }
        }
        is MapPin.PersonP -> {
            val p = pin.p
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = imagePaint(p.image), contentDescription = p.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${p.firstName}, ${p.age}", style = T.glassText(size = 14))
                        Spacer(Modifier.height(3.dp))
                        Text("${p.distanceLabel} · ${p.match}% uyğunluq", style = T.meta)
                    }
                }
                Spacer(Modifier.height(12.dp))
                BottomCardButton("Tanış ol", primary = true, onClick = onMeetPerson, fill = true)
            }
        }
    }
}

@Composable
private fun BottomCardButton(
    label: String, primary: Boolean, onClick: () -> Unit, fill: Boolean = false,
) {
    val bg = if (primary) Q.primary else Color.White.copy(alpha = 0.5f)
    val fg = if (primary) Q.primaryFg else Q.foreground
    val line = if (fill) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(0.42f)
    Box(
        line
            .height(42.dp)
            .background(bg, RoundedCornerShape(Q.rMd.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = T.button.copy(color = fg)) }
}

/** Şüşə filtr çipi — xəritə üzərində üzən seçim həbi. */
@Composable
private fun GlassFilterChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .androidxChipTap(interaction, onClick)
            .height(31.dp)
            .androidxChipBg(active)
            .padding(horizontal = 11.dp),
    ) {
        Icon(icon, null, tint = if (active) Q.primaryFg else Q.foreground, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = androidx.compose.ui.text.TextStyle(
            fontFamily = JakartaSans, fontSize = 11.5.sp,
            fontWeight = if (active) FontWeight.W700 else FontWeight.W500,
            color = if (active) Q.primaryFg else Q.foreground))
    }
}

private fun Modifier.androidxChipTap(interaction: MutableInteractionSource, onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick))

private fun Modifier.androidxChipBg(active: Boolean): Modifier = this.then(
    Modifier
        .background(if (active) Q.primary.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.42f), RoundedCornerShape(Q.rMd.dp))
        .border(
            if (active) 1.2.dp else 1.dp,
            if (active) Q.primary else Color.White.copy(alpha = 0.6f),
            RoundedCornerShape(Q.rMd.dp),
        )
)
