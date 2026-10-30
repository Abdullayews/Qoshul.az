package az.qoshul.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.EventProposal
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.parseAssistantEvent
import az.qoshul.app.core.ui.GlassNavHost
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.core.ui.rememberNavState
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.features.assistant.AssistantScreen
import az.qoshul.app.features.auth.AuthScreen
import az.qoshul.app.features.coupons.CouponsScreen
import az.qoshul.app.features.events.CreateEventSheet
import az.qoshul.app.features.events.FinishActivitySheet
import az.qoshul.app.features.match.MatchScreen
import az.qoshul.app.features.notifications.NotificationsScreen
import az.qoshul.app.features.onboarding.OnboardingScreen
import az.qoshul.app.features.partners.PartnersScreen
import az.qoshul.app.features.reservations.ReservationsScreen
import az.qoshul.app.features.safety.SafetyScreen
import az.qoshul.app.features.settings.SettingsScreen
import az.qoshul.app.features.shell.AppShell
import az.qoshul.app.features.venues.VenueDetailScreen
import az.qoshul.app.features.xp.XpCardScreen
import kotlinx.coroutines.Dispatchers

/**
 * Tək Activity (D-036): bütün UI Compose; naviqasiya öz back-stack
 * abstraksiyasıdır (Flutter Navigator pariteti — qərar docs/decisions.md D-036).
 *
 * Edge-to-edge (D-031 pariteti): enableEdgeToEdge — məzmun şəffaf status/nav
 * barların altından axır; insetlər statusBarsPadding/navigationBarsPadding ilə.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            QoshulTheme {
                QoshulApp()
            }
        }
    }
}

/**
 * Kök — Flutter main.dart portu: Auth → Onboarding → AppShell (4 tab).
 * Mühərrik tətbiq miqyasındadır (composition-da bir dəfə yaradılır);
 * push-ekranlar GlassNavHost ilə (GlassPageRoute pariteti).
 */
@Composable
fun QoshulApp() {
    val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
    val engine = remember { AppEngine(scope = scope) }
    val st by engine.state.collectAsState()
    val nav = rememberNavState()

    // modal sheet vəziyyətləri
    var createOpen by remember { mutableStateOf(false) }
    var createVenue by remember { mutableStateOf<Partner?>(null) }
    var assistantProposal by remember { mutableStateOf<EventProposal?>(null) }
    var finishEvent by remember { mutableStateOf<QoshulEvent?>(null) }

    Box(Modifier.fillMaxSize()) {
        when {
            !st.signedIn -> AuthScreen(onSignedIn = { engine.signIn() })
            !st.onboarded -> OnboardingScreen(engine)
            else -> GlassNavHost(
                nav = nav,
                home = {
                    AppShell(
                        engine = engine,
                        nav = nav,
                        onOpenCreate = { createOpen = true },
                    )
                },
            ) { route ->
                RouteScreen(
                    route = route,
                    engine = engine,
                    nav = nav,
                    onOpenCreateAtVenue = { v -> createVenue = v; createOpen = true },
                    onOpenAssistantCreate = { proposal ->
                        assistantProposal = proposal; createOpen = true
                    },
                )
            }
        }
    }

    // — Modal sheet-lər (yaratma / bitirmə) —
    if (createOpen) {
        CreateEventSheet(
            engine = engine,
            onDismiss = {
                createOpen = false
                createVenue = null
                assistantProposal = null
            },
            initial = assistantProposal,
            venue = createVenue,
        )
    }
    finishEvent?.let { ev ->
        FinishActivitySheet(engine = engine, event = ev, onDismiss = { finishEvent = null })
    }
}

/** Push-ekran yönləndirməsi. */
@Composable
private fun RouteScreen(
    route: Route,
    engine: AppEngine,
    nav: NavState,
    onOpenCreateAtVenue: (Partner) -> Unit,
    onOpenAssistantCreate: (EventProposal) -> Unit,
) {
    when (route) {
        Route.Partners -> PartnersScreen(engine, nav, onOpenCreateAtVenue)
        Route.Assistant -> AssistantScreen(engine, nav,
            onOpenCreate = { lastProposal(engine)?.let(onOpenAssistantCreate) })
        Route.Notifications -> NotificationsScreen(engine, nav)
        Route.Match -> MatchScreen(engine, nav)
        Route.Coupons -> CouponsScreen(engine, nav)
        Route.Reservations -> ReservationsScreen(engine, nav)
        Route.XpCard -> XpCardScreen(engine, nav)
        Route.Settings -> SettingsScreen(engine, nav)
        Route.Safety -> SafetyScreen(nav)
        is Route.VenueDetail -> VenueDetailScreen(engine, nav, route.venueId)
    }
}

/** Assistant-dan son [TƏDBİR] təklifi (create sheet prefill). */
private fun lastProposal(engine: AppEngine): EventProposal? {
    val lastAi = engine.state.value.assistantMsgs.lastOrNull { !it.mine } ?: return null
    return parseAssistantEvent(lastAi.body).proposal
}
