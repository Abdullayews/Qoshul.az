package az.qoshul.app.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.theme.MapPalette
import az.qoshul.app.core.theme.Q
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * GOOGLE MAPS KANVAS — D-108 (hazırlıq; direktiv 2026-11-09: „map da
 * google sdk keçmək üçün hazırlaş“). MapLibre kanvasının (MaplibreCanvas)
 * Google Maps SDK + maps-compose 9.0.1 ekvivalenti — EYNİ pin vizual
 * dili (D-027: məkan = yaşal dot + storefront qlifi, tədbir = narıncı
 * dot + təqvim qlifi, insan = tünd dot + baş hərf, seçim halosu,
 * „mənim yerim“ dotu) MarkerComposable üzərində qurulur.
 *
 * Keçid (local.properties): MAP_BACKEND=google + MAPS_API_KEY=AIza…
 * (manifest com.google.android.geo.API_KEY placeholder-ı). Atribusiya:
 * Google SDK öz logosunu xəritə üzərində özü çəkir (OSM krediti bu
 * backend-də tələb olunmur — tile-lar Google-dür).
 */
@Composable
internal fun GoogleMapCanvas(
    pins: List<MapPin>,
    myLat: Double,
    myLng: Double,
    // G.2 (D-170): dəqiqlik dairəsi (metr) + istiqamət (dərəcə); null = yox
    myAccuracyM: Float? = null,
    myBearing: Float? = null,
    selectedKey: String?,
    pendingCamera: Pair<Double, Double>?,
    onPinTap: (MapPin) -> Unit,
    onPinDismiss: () -> Unit,
    onCameraConsumed: () -> Unit,
    onMyLocationRequest: () -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(myLat, myLng), 13f)
    }

    // D-050 pariteti: fokus istəyi → kameranı pinə uçur (zoom ≥ 15).
    // pendingCamera = (lng, lat) cütü (MapScreen konvensiyası).
    LaunchedEffect(pendingCamera) {
        val pos = pendingCamera ?: return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(pos.second, pos.first),
                maxOf(cameraPositionState.position.zoom, 15f),
            ),
        )
        onCameraConsumed()
    }

    val scope = rememberCoroutineScope()

    // Kanvas kökü — align() (zoom həbləri) BoxScope tələb edir
    Box(Modifier.fillMaxSize()) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // F.2 (D-170, direktiv: „rəng kontrastını artır: parlaq, realistik
        // yol/su/yaşıllıq/bina rəngləri“): stil JSON — yollar parlaq sarı-ağ,
        // su canlı göy, yaşıllıq doyğun yaşıl, binalar aydıntılı bej, torpaq
        // isti çalarlı. Atribusiya/loqo SDK default-unada gizlədilə bilmir
        // (Google Maps SDK şərti) — amma miqyas/compass maps-ui-da bağlıdır.
        properties = MapProperties(
            mapType = MapType.NORMAL,
            mapStyleOptions = com.google.android.gms.maps.model.MapStyleOptions(
                // language=JSON — Google Maps Platform stil spesifikasiyası
                // F.2: rənglər MapPalette-dən (hər iki backend eyni palitra)
                buildString {
                    fun hex(c: Int) = "#" + Integer.toHexString(c)
                    append("[")
                    append("{\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.LAND)).append("\"}]},")
                    append("{\"elementType\":\"labels.text.stroke\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.LABEL_HALO)).append("\"},{\"weight\":2.2}]},")
                    append("{\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.LABEL_FILL)).append("\"}]},")
                    append("{\"featureType\":\"road\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.ROAD_LOCAL)).append("\"}]},")
                    append("{\"featureType\":\"road.arterial\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.ROAD_ARTERIAL)).append("\"}]},")
                    append("{\"featureType\":\"road.highway\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.ROAD_HIGHWAY)).append("\"}]},")
                    append("{\"featureType\":\"road.highway\",\"elementType\":\"geometry.stroke\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.ROAD_HIGHWAY_STROKE)).append("\"}]},")
                    append("{\"featureType\":\"water\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.WATER)).append("\"}]},")
                    append("{\"featureType\":\"landscape.natural\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.NATURE)).append("\"}]},")
                    append("{\"featureType\":\"poi.park\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.PARK)).append("\"}]},")
                    append("{\"featureType\":\"poi.park\",\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.PARK_LABEL)).append("\"}]},")
                    append("{\"featureType\":\"landscape.man_made\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.BUILDING)).append("\"}]},")
                    append("{\"featureType\":\"poi.business\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"").append(hex(MapPalette.POI)).append("\"}]},")
                    append("{\"featureType\":\"transit\",\"stylers\":[{\"visibility\":\"off\"}]}")
                    append("]")
                }
            ),
        ),
        // kamera həbləri bizimdir (MapControls) — SDK-nınkini söndürürük
        uiSettings = MapUiSettings(zoomControlsEnabled = false),
        // boş toxunma = seçimi burax (MapLibre onUnhandled pariteti)
        onMapClick = { onPinDismiss() },
    ) {
        pins.forEach { pin ->
            val key = pin.key()
            val selected = key == selectedKey
            val position = LatLng(pin.lat, pin.lng)

            // — seçilmiş pinin halosu (üst qat, MapLibre blur-halo pariteti) —
            if (selected) {
                MarkerComposable(
                    keys = arrayOf(key, "halo"),
                    state = rememberMarkerState(key = "$key-halo", position = position),
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 2f,
                    contentDescription = null,
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .blur(8.dp)
                            .background(Q.mapPin.copy(alpha = 0.30f), CircleShape),
                    )
                }
            }

            MarkerComposable(
                keys = arrayOf(key),
                state = rememberMarkerState(key = key, position = position),
                anchor = Offset(0.5f, 0.5f),
                zIndex = 1f,
                contentDescription = pin.label(),
                onClick = { onPinTap(pin); true },
            ) {
                PinVisual(pin)
            }
        }

        // — „mənim yerim“ dotu (üst qat) —
        // H3 (audit v0.49): rememberMarkerState-in position arqumenti yalnız
        // İLK kompozisiyada tətbiq olunur (SDK məhdudiyyəti) — real fix
        // gələndə (myLat/myLng dəyişəndə) marker köhnə fallback nöqtəsində
        // qalırdı. LaunchedEffect position-u manual sinxronlaşdırır.
        val meState = rememberMarkerState(key = "me", position = LatLng(myLat, myLng))
        LaunchedEffect(myLat, myLng) {
            meState.position = LatLng(myLat, myLng)
        }
        // G.2 (D-170): DƏQİQLİK DAİRƏSİ — real fix-in accuracyM radiusu
        // (yalnız canlı fix-də; fallback/stale halda çəkilmir)
        if (myAccuracyM != null && myAccuracyM > 0f) {
            Circle(
                center = LatLng(myLat, myLng),
                radius = myAccuracyM.toDouble(),
                fillColor = Q.myLocation.copy(alpha = 0.15f),
                strokeColor = Q.myLocation.copy(alpha = 0.35f),
                strokeWidth = 2f,
            )
        }
        MarkerComposable(
            keys = arrayOf(myLat, myLng),
            state = meState,
            anchor = Offset(0.5f, 0.5f),
            zIndex = 3f,
            contentDescription = null,
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .border(3.dp, Q.mapPin, CircleShape)
                    .background(Q.myLocation, CircleShape),
            ) {
                // G.2: İSTİQAMƏT oxu — bearing bilinirsə dot içindən çıxan
                // kiçik üçbucaq (hərəkət istiqaməti, Snapchat pariteti)
                if (myBearing != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 0.dp)
                            .size(width = 8.dp, height = 14.dp)
                            .graphics { rotationZ = myBearing!! }
                            .background(
                                Q.mapPin,
                                androidx.compose.ui.graphics.RectangleShape,
                            ),
                    )
                }
            }
        }
    }

    // — kamera həbləri (D-143: +/− ləğv; düzləşdirmə üst, „mənim yerim“
    // alt — ortaq xrom; kamera animasiyaları backend-ə xasdır) —
    MapControls(
        onResetBearing = {
            scope.launch {
                // D-117: fırlanmış/əyilmiş xəritəni şimala düzləşdir —
                // mövcqe/zoom saxlanılır, bearing/tilt sıfırlanır
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder(cameraPositionState.position)
                            .bearing(0f)
                            .tilt(0f)
                            .build()
                    )
                )
            }
        },
        onMyLocation = onMyLocationRequest,
    )
    } // Box (kanvas kökü)
}

/** A11y üçün pin etiketi (məkan/tədbir adı, insan adı). */
private fun MapPin.label(): String = when (this) {
    is MapPin.Venue -> v.name
    is MapPin.Event -> e.title
    is MapPin.PersonP -> p.fullName
}

/**
 * Pin vizualı — MapLibre qat dilinin (QoshulPinLayers) Compose ekvivalenti:
 * 17dp radius dot (rəng tipə görə) + 2.5dp kontur + mərkəz qlifi/baş hərfi.
 */
@Composable
private fun PinVisual(pin: MapPin) {
    val dot = when (pin) {
        is MapPin.Venue -> Q.primary
        is MapPin.Event -> Q.accent
        is MapPin.PersonP -> Q.foreground
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .border(2.5.dp, Q.mapPin, CircleShape)
            .background(dot, CircleShape),
    ) {
        when (pin) {
            is MapPin.Venue -> Icon(
                Icons.Filled.Storefront, null, tint = Q.mapPin,
                modifier = Modifier.size(16.dp),
            )
            is MapPin.Event -> Icon(
                Icons.Filled.Event, null, tint = Q.mapPin,
                modifier = Modifier.size(16.dp),
            )
            is MapPin.PersonP -> Text(
                pin.p.firstName.take(1),
                color = Q.mapPin,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
            )
        }
    }
}
