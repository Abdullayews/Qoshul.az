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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.qoshul.app.core.theme.Q
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
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
    selectedKey: String?,
    pendingCamera: Pair<Double, Double>?,
    onPinTap: (MapPin) -> Unit,
    onPinDismiss: () -> Unit,
    onCameraConsumed: () -> Unit,
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
        properties = MapProperties(mapType = MapType.NORMAL),
        // zoom həbləri bizimdir (MapZoomControls) — SDK-nınkini söndürürük
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
        MarkerComposable(
            keys = arrayOf(myLat, myLng),
            state = rememberMarkerState(key = "me", position = LatLng(myLat, myLng)),
            anchor = Offset(0.5f, 0.5f),
            zIndex = 3f,
            contentDescription = null,
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .border(3.dp, Q.mapPin, CircleShape)
                    .background(Q.myLocation, CircleShape),
            )
        }
    }

    // — zoom həbləri (ortaq xrom, D-030 dili) —
    MapZoomControls(
        onZoom = { delta ->
            scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.zoomBy(delta.toFloat()))
            }
        },
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
