package az.qoshul.app.core.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * Söhbət fokus istəyi (D-055) — MapFocusState pariteti, amma Söhbətlər tabına.
 *
 *  - EventDetailSheet "Qrup söhbətinə bax" → [event]
 *  - MatchScreen "Söhbətə keç"            → [friend]
 *
 * Axın: istək düşür → AppShell Söhbətlər tabına keçir → ChatsScreen
 * konkret söhbəti açır və istəyi [consume] edir. Əvvəl bu düymələr ya
 * deaktiv idi, ya da yalnız nav.pop() edirdi (A6 audit).
 */
class ChatFocusState {

    sealed interface Target {
        data class Event(val eventId: String) : Target
        data class Friend(val personId: String) : Target
    }

    var request: Target? by mutableStateOf(null)
        private set

    fun event(eventId: String) {
        request = Target.Event(eventId)
    }

    fun friend(personId: String) {
        request = Target.Friend(personId)
    }

    fun consume() {
        request = null
    }
}
