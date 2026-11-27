package az.qoshul.app

import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.EventProposal
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.parseAssistantEvent
import az.qoshul.app.core.motion.Motion
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.T
import az.qoshul.app.core.ui.ChatFocusState
import az.qoshul.app.core.ui.GlassNavHost
import az.qoshul.app.core.ui.MapFocusState
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
import az.qoshul.app.features.profile.PersonalityQuizScreen
import az.qoshul.app.features.profile.TaskCenterScreen
import az.qoshul.app.features.profile.ProfileEditScreen
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
 *
 * D-104 (2026-11-09, istifadəçi direktivi: „saat, internet, şarj ilə iç içə
 * girir“): arqumentsiz enableEdgeToEdge() SystemBarStyle.auto — SİSTEMİN
 * qaranlıq rejiminə baxır; tətbiq isə yalnız İŞIQ temadır (D-023, Q.background
 * #F8FAFC). Sistem tünd olanda status/nav ikonları AĞ düşürdü və açıq fonda
 * oxunmurdu. Həll: hər iki barın görünüşü İŞİQ kimi SABİTLƏNİR (tünd ikonlar,
 * scrim şəffaf) — sistem temasından asılılıq aradan qalxır; edge-to-edge
 * (D-031) və cutout (D-051) dili dəyişmir. Paralel təsdiq: QoshulTheme
 * (SideEffect) — konfiqurasiya dəyişikliyində belə görünüşü yenidən qoyur.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        // D-051: full-screen sizing — çentikli (cutout) ekranlarda məzmun
        // bütün displeyə yayılır (default letterbox/qara zolaqlar əvəzinə).
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
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
 * D-050: xəritə fokus vəziyyəti (MapFocusState) burada sahiblənir —
 * AppShell (tab keçidi) və push-ekranlar (VenueDetail "Yol tap") paylaşır.
 * D-055: [chatFocus] — Söhbətlər tabına keçid siqnalı (MapFocus pariteti);
 * onFinishActivity — EventDetail → FinishActivitySheet (əvvəl kəsılırdı,
 * bitirmə axını bütün 695 sətirlik sheet UI-dan əlçatmaz idi).
 */
@Composable
fun QoshulApp(engineOverride: AppEngine? = null) {
    val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
    // D-170: tək rejim — SupabaseSync (MOCK/NoopSync yoxdur).
    // D-149: engineOverride — Robolectric UI testləri hazır engine verir
    // (demo düyməsi silindi; testlər onboarding-i ötürmək üçün seed-li
    // engine inyeksiya edir — istehsal kodu yolunu dəyişmir).
    val sync = remember { az.qoshul.app.core.backend.BackendSync.create(scope) }
    val engine = engineOverride ?: remember { AppEngine(scope = scope, sync = sync) }
    // I.4 (D-170): RPC xətaları → engine rollback + istifadəçiyə xəta
    androidx.compose.runtime.LaunchedEffect(engine) {
        az.qoshul.app.core.backend.SupabaseBackend.onRpcError = { fn, t ->
            engine.reportFailure("sync:$fn", t)
        }
    }
    val st by engine.state.collectAsStateWithLifecycle()

    // D-151: SESSİYA BƏRPASI — restart-dan sonra login qalır (SettingsStorage);
    // serverdən data oxunur (demo data mənbəyi yoxdur). Yalnız istehsal
    // engine-ində (test engineOverride gəlirsə keçilir).
    if (engineOverride == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (az.qoshul.app.core.backend.SupabaseBackend.isSignedIn()) {
                engine.signIn()
                engine.refreshFromServer()
            }
        }
    }
    val nav = rememberNavState()
    val focus = remember { MapFocusState() }
    val chatFocus = remember { ChatFocusState() }

    // modal sheet vəziyyətləri
    var createOpen by remember { mutableStateOf(false) }
    var createVenue by remember { mutableStateOf<Partner?>(null) }
    var assistantProposal by remember { mutableStateOf<EventProposal?>(null) }
    var finishEvent by remember { mutableStateOf<QoshulEvent?>(null) }

    // U2.4 (UI-10): AppShell-ə ötürülən lambda-lar stabilləşdirilir —
    // yoxsa hər emit-də yeni instansiyalar AppShell-in tam recompose-unu
    // məcbur edirdi (audit UI-10). Closure delegate üzərindən oxuyur/yazır.
    val openCreate: () -> Unit = remember { { createOpen = true } }
    val finishActivity: (QoshulEvent) -> Unit = remember { { e -> finishEvent = e } }
    val openChat: (QoshulEvent) -> Unit = remember { { e -> chatFocus.event(e.id) } }

    // D-128 (direktiv 2026-11-11: „qara/AMOLED temalarda animasiyalar arasında
    // ağ səhifə problemi həll olsun, temaya uyğun rəngli keçid animasiyası da
    // əlavə et“): kök qat TEMAYA UYĞUN fon daşıyır — (1) GlassNavHost push/pop
    // keçidinin yarı-şəffaf anlarında (D-134 2-fazalı keçid) arxada AĞ window
    // fonu deyil, tema rəngi görünür; (2) dizayn modu dəyişəndə (Ağ↔Qara↔Amoled)
    // fon yumşaq KEÇİR (animateColorAsState 400ms — „temaya uyğun keçid
    // animasiyası“), qəflən snap YOX. Window-un öz fonu Theme.kt-də (SideEffect)
    // tema rənginə boyanır — ilk kadr öncəsi flaşın qarşısı.
    val rootBg by animateColorAsState(
        targetValue = Q.background,
        animationSpec = tween(400, easing = Motion.ease),
        label = "rootBg",
    )
    Box(Modifier.fillMaxSize().background(rootBg)) {
        when {
            !st.signedIn -> AuthScreen(onSignedIn = { engine.signIn() }, engine = engine)
            // D-181: profil serverdən oxunana qədər onboarding AÇILMIR.
            // ƏVVƏLKİ BUG: onboarded qərarı profileLoaded gözləmirdi —
            // köhnə APK-da təkrar girişdə onboardedV=false defaultu ilə
            // OnboardingScreen DƏRHAL göstərilirdi; serverdən onboarded=true
            // gəlsə belə istifadəçi sualları artıq GÖRMÜŞDÜR. İndi:
            // profil gəlməmiş → ProfileLoadingScreen (spinner); gəldi →
            // onboarded=true → app, onboarded=false → onboarding (yalnız
            // yeni qeydiyyat).
            !st.profileLoaded -> ProfileLoadingScreen(engine)
            !st.onboarded -> OnboardingScreen(engine)
            else -> GlassNavHost(
                nav = nav,
                home = {
                    AppShell(
                        engine = engine,
                        nav = nav,
                        onOpenCreate = openCreate,
                        focus = focus,
                        chatFocus = chatFocus,
                        onFinishActivity = finishActivity,
                        onOpenChat = openChat,
                    )
                },
            ) { route ->
                RouteScreen(
                    route = route,
                    engine = engine,
                    nav = nav,
                    focus = focus,
                    chatFocus = chatFocus,
                    onOpenCreateAtVenue = { v -> createVenue = v; createOpen = true },
                    onOpenAssistantCreate = { proposal ->
                        assistantProposal = proposal; createOpen = true
                    },
                )
            }
        }
        // D-176 (ikinci dalğa): sync xətaları Snackbar — BoxScope daxilində
        SyncErrorSnackbar(st.syncError)
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
    // D-055: finishEvent axını İNDİ canlıdır — əvvəl heç kim bu state-i
    // yazmırdı (EventDetail onFinishActivity {} idi), FinishActivitySheet
    // (QR → şəkil → taq → XP) 695 sətirlik ölü kod idi.
    finishEvent?.let { ev ->
        FinishActivitySheet(engine = engine, event = ev, onDismiss = { finishEvent = null })
    }


}

/**
 * D-176: syncError Snackbar — shell daxilində xəta baş olanda (metadata
 * report xətası, kritik rollback) istifadəçiyə qısa bildiriş göstərir.
 * Xəta boş olanda heç nə göstərilmir; eyni mətn təkrar göstərilmir.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SyncErrorSnackbar(syncError: String) {
    if (syncError.isEmpty()) return
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(syncError) {
        snackbar.showSnackbar(syncError, duration = androidx.compose.material3.SnackbarDuration.Short)
    }
    androidx.compose.material3.SnackbarHost(
        hostState = snackbar,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
}

/** Push-ekran yönləndirməsi. */
@Composable
private fun RouteScreen(
    route: Route,
    engine: AppEngine,
    nav: NavState,
    focus: MapFocusState,
    chatFocus: ChatFocusState,
    onOpenCreateAtVenue: (Partner) -> Unit,
    onOpenAssistantCreate: (EventProposal) -> Unit,
) {
    when (route) {
        Route.Partners -> PartnersScreen(engine, nav, onOpenCreateAtVenue)
        Route.Assistant -> AssistantScreen(engine, nav,
            onOpenCreate = { lastProposal(engine)?.let(onOpenAssistantCreate) })
        Route.Notifications -> NotificationsScreen(engine, nav)
        Route.Match -> MatchScreen(engine, nav,
            onOpenChat = { p -> chatFocus.friend(p.id) })
        Route.Coupons -> CouponsScreen(engine, nav)
        Route.Reservations -> ReservationsScreen(engine, nav)
        Route.XpCard -> XpCardScreen(engine, nav)
        Route.Settings -> SettingsScreen(engine, nav)
        Route.Safety -> SafetyScreen(nav)
        Route.Quiz -> PersonalityQuizScreen(engine, nav) // D-056
        Route.TaskCenter -> TaskCenterScreen(engine, nav) // D-155
        Route.ProfileEdit -> ProfileEditScreen(engine, nav) // D-112
        is Route.VenueDetail -> VenueDetailScreen(engine, nav, route.venueId, focus)
    }
}

/** Assistant-dan son [TƏDBİR] təklifi (create sheet prefill). */
private fun lastProposal(engine: AppEngine): EventProposal? {
    val lastAi = engine.state.value.assistantMsgs.lastOrNull { !it.mine } ?: return null
    return parseAssistantEvent(lastAi.body).proposal
}

/**
 * C (D-170): giriş sonrası PROFİL YÜKLƏNMƏ ekranı. signIn() signedInV=true
 * edir, amma onboarded qərarı serverdən gələn profillə (profileLoaded)
 * verilir — bu müddətdə loading. Xəta → aydın mesaj + Yenidən cəhd
 * (səssiz udmа YOXDUR, C.2). Təkrar girişdə onboarding bir daha
 * GÖRÜNMÜR (onboarded=true serverdən gələnə qədər bu ekran qalır).
 */
@Composable
private fun ProfileLoadingScreen(engine: AppEngine) {
    val st by engine.state.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Q.background)
            .padding(horizontal = 20.dp),
    ) {
        if (st.syncError.isEmpty()) {
            CircularProgressIndicator(color = Q.primary, strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text("Profil yüklənir…", style = T.subtitle)
        } else {
            Text("Yükləmə alınmadı", style = T.h1)
            Spacer(Modifier.height(8.dp))
            Text(st.syncError, style = T.subtitle)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { engine.refreshFromServer() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Q.primary, contentColor = Q.primaryFg),
            ) { Text("Yenidən cəhd et", style = T.button) }
            Spacer(Modifier.height(10.dp))
            // Şəbəkə yoxdursa istifadəçi ayarlara özü baxa bilsin
            Text(
                "İnternet bağlantını yoxla və yenidən cəhd et.",
                style = T.chipXs.copy(color = Q.mutedFg),
            )
        }
    }
}
