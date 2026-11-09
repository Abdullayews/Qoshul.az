package az.qoshul.app.features.map

import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.model.QoshulEvent

/**
 * Xəritə pin modeli (D-027) — BACKEND-AGNOSTİK (D-108): həm MapLibre
 * qatları (MaplibreCanvas — GeoJSON feature-lara çevrilir), həm Google
 * Maps markerləri (GoogleMapCanvas — MarkerComposable) eyni modeldən
 * qurulur. Yerləşdirmə/filtr/fokus məntiqi MapScreen-də təkdir — backend
 * yalnız RENDER edir.
 */
sealed interface MapPin {
    val lat: Double
    val lng: Double

    data class Venue(val v: Partner) : MapPin {
        override val lat = v.lat
        override val lng = v.lng
    }

    data class Event(val e: QoshulEvent) : MapPin {
        override val lat = e.lat
        override val lng = e.lng
    }

    data class PersonP(val p: Person) : MapPin {
        override val lat = p.mapLat
        override val lng = p.mapLng
    }
}

/** Qat kliklərində pini tapmaq üçün stabil açar (backend-agnostik, D-108). */
fun MapPin.key(): String = when (this) {
    is MapPin.Venue -> "v${v.id}"
    is MapPin.Event -> "e${e.id}"
    is MapPin.PersonP -> "p${p.id}"
}
