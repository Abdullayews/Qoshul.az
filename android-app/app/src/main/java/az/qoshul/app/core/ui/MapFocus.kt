package az.qoshul.app.core.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * Xəritə fokus istəyi (D-050) — hər hansı ekrandan xəritəyə "götür məni"
 * siqnalı. A3 auditinin "Yol göstər / Yol tap — xəritə fazasında" deaktiv
 * düymələrinin aktivləşdirilməsi:
 *
 *  - EventDetailSheet "Yol göstər"  → [event]
 *  - VenueDetailScreen "Yol tap"    → [venue]
 *
 * Axın: istək düşür → AppShell Xəritə tabına keçir → MapScreen pin seçir,
 * kameranı uçurur və istəyi [consume] edir. Android istənilən ekrandan
 * çağırıla bildiyindən CompositionLocal yerinə açıq paylanır (MainActivity
 * sahiblik edir, AppShell + push-ekranlara ötürülür).
 */
class MapFocusState {

    sealed interface Target {
        data class Venue(val venueId: String) : Target
        data class Event(val eventId: String) : Target
    }

    var request: Target? by mutableStateOf(null)
        private set

    fun venue(venueId: String) {
        request = Target.Venue(venueId)
    }

    fun event(eventId: String) {
        request = Target.Event(eventId)
    }

    fun consume() {
        request = null
    }
}
