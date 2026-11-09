package az.qoshul.app.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
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
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Liberty stilinin qlif şrifti (initial hərflər üçün). */
private const val STYLE_TEXT_FONT = "Noto Sans Regular"

/** D-048: OpenFreeMap „liberty“ vektor stili — pulsuz, açarsız. */
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

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

/**
 * MAPLIBRE KANVAS — D-048 (MapLibre Compose 0.20 + OpenFreeMap liberty) —
 * D-108-də MapScreen-dən AYRILDI: backend kanvası kimi (google rejimində
 * GoogleMapCanvas onu əvəz edir). Özündə: tile səthi + stil yüklenmə
 * retry (D-061) + zoom həbləri + OSM atribusiyası (Google tile-larında
 * YOX — orada SDK öz logosunu çəkir).
 *
 * Pin qatları (D-027 render dili): seçim halosu → məkan yaşal dot +
 * storefront qlifi → tədbir narıncı dot + təqvim qlifi → insan tünd dot +
 * baş hərf → „mənim yerim“ dotu. Boş toxunma = seçimi buraxır.
 */
@Composable
internal fun MaplibreCanvas(
    pins: List<MapPin>,
    myLat: Double,
    myLng: Double,
    selectedKey: String?,
    pendingCamera: Pair<Double, Double>?,
    onPinTap: (MapPin) -> Unit,
    onPinDismiss: () -> Unit,
    onCameraConsumed: () -> Unit,
) {
    // D-061: key(mapRetry) — „Yenidən dənə“ bütün xəritə alt-ağacını
    // (rememberMapState daxil) söküb yenidən qurur: təzə stil yükləmə cəhdi.
    var mapRetry by remember { mutableStateOf(0) }
    key(mapRetry) {
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
                selectedKey = selectedKey,
                onPinTap = onPinTap,
            )
        }

        // D-061 „nə olar əgər…“: stil yüklənməsə (offline/DNS/server 503)
        // fallback kart (MapScreen-də render olunur — styleFailed state-i
        // burada izlənilir). loadState = MutableState-backed (maplibre-compose
        // 0.20 classes.jar sübutu — D-061-in düzəlişi).
        var styleFailed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            snapshotFlow { mapState.style.loadState }
                .collect { state -> styleFailed = state is StyleLoadState.Failed }
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
            onCameraConsumed()
        }

        // animateCamera suspend-dir — zoom həbləri coroutine scope-da işə düşür
        val scope = rememberCoroutineScope()

        // Kanvas kökü — align() (zoom/atribusiya/fail kart) BoxScope tələb edir
        Box(Modifier.fillMaxSize()) {
        // — REAL xəritə: MapLibre vektor render (D-048) —
        MaplibreMap(
            state = mapState,
            modifier = Modifier.fillMaxSize(),
            uiOptions = MapUiOptions { loadColor = Q.tileFallback },
            interactions = MapInteractions {
                callbacks {
                    click {
                        // heç bir qat tutmadı → seçimi burax
                        onUnhandled { onPinDismiss(); ClickResult.Pass }
                    }
                }
            },
        )

        // — zoom həbləri (şüşə, D-030) — kamera animasiyası ilə —
        MapZoomControls(
            onZoom = { delta ->
                scope.launch {
                    mapState.animateCamera(
                        CameraUpdate(
                            zoom = (mapState.cameraPosition.zoom + delta)
                                .coerceIn(3.0, 18.0),
                        ),
                        CameraAnimation.Ease.Standard,
                    )
                }
            },
        )

        // — OSM atribusiyası (istifadə qaydası — məcburi kredit; yalnız
        // OSM tile-larında — Google backend-də SDK logosu özü çəkilir) —
        // D-053 fix: atribusiya nav pill-in arxasında gizlənirdi — yuxarı qalxdı
        Text(
            "© OpenStreetMap contributors",
            style = T.metaSm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 6.dp, bottom = 96.dp)
                .background(Q.card.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )

        // D-061: stil yüklenmə xətası — fallback kart + təkrar cəhd
        if (styleFailed) {
            MapLoadFailedCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                onRetry = { mapRetry++ },
            )
        }
        } // Box (kanvas kökü)
    }
}

/**
 * Pin qatları — MapLibre stil kompozisiyası (rememberMapState content).
 *
 * Render ardıcıllığı (elan sırası): seçim halosu → məkan dot+qlif →
 * tədbir dot+qlif → insan dot+baş hərf → „mənim yerim“ dotu.
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

    // Qat klikləri: feature „key“ xassəsindən pin tapılır
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
            color = const(Q.mapPin.copy(alpha = 0.30f)),
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
            strokeColor = const(Q.mapPin),
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
                colorFilter = ColorFilter.tint(Q.mapPin),
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
            strokeColor = const(Q.mapPin),
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
                colorFilter = ColorFilter.tint(Q.mapPin),
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
            strokeColor = const(Q.mapPin),
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
            textColor = const(Q.mapPin),
            textAllowOverlap = const(true),
            textIgnorePlacement = const(true),
            onClick = pinClick,
            hitPadding = 6.dp,
        )
    }

    // — „mənim yerim“ (üst qat) —
    CircleLayer(
        id = "qoshul-me-dot",
        source = meSource,
        radius = const(10.dp),
        color = const(Q.myLocation),
        strokeColor = const(Q.mapPin),
        strokeWidth = const(3.dp),
    )
}
