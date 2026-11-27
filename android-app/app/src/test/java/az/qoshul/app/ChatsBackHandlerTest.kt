package az.qoshul.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.features.chats.ChatsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * U1.1 (UI-03) regressiya testi — audit 2026-11-07:
 * "Açıq çatda sistem GERİ tətbiqi bağlayır (BackHandler yoxdur)".
 *
 * Gözlənti: açıq söhbətdə sistem GERİ söhbəti BAĞAYIR (activity YAŞAYIR),
 * ikinci GERİ (siyahıda) isə adi Android davranışıdır.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatsBackHandlerTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun newEngine(): AppEngine {
        val engine = AppEngine(dataSource = az.qoshul.app.core.data.FakeQoshulDataSource, scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        return engine
    }

    /**
     * D-176: keçmişdə konstruktor-da hardcoded ("p2" to "accepted") seed var
     * idi (demo qalığı). Artıq dostluq SERVERDƏN gəlir — UI testləri bu
     * helper ilə server state-i simulyasiya edir (applyServerData).
     */
    private fun seedServerState(engine: AppEngine) {
        engine.applyServerData(
            az.qoshul.app.core.backend.SupabaseReader.ServerData(
                events = az.qoshul.app.core.data.FakeQoshulDataSource.events,
                people = az.qoshul.app.core.data.FakeQoshulDataSource.people,
                offers = az.qoshul.app.core.data.FakeQoshulDataSource.offers,
                partners = az.qoshul.app.core.data.FakeQoshulDataSource.partners,
                joinedEventIds = setOf("e1"),
                friendships = mapOf("p2" to "accepted"),
                friendMessagePeople = az.qoshul.app.core.data.FakeQoshulDataSource.initialFriendMessages,
                eventMessages = az.qoshul.app.core.data.FakeQoshulDataSource.initialEventMessages,
                venueXp = az.qoshul.app.core.data.FakeQoshulDataSource.initialVenueXp(),
            ),
        )
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(60)
        compose.waitForIdle()
    }

    @Test
    fun `aciq sobhesde sistem geri sobheti baglayir teleq yox`() {
        val engine = newEngine()
        seedServerState(engine) // D-176: dostluq + joined tədbir serverdən
        val st = engine.state.value
        val ev = st.events.firstOrNull { it.id in st.joined }
        val friend = engine.acceptedFriends.firstOrNull()
        // Açılacaq söhbət: bağlı tədbir yoxdursa dost söhbəti
        val openEvent = ev != null
        check(ev != null || friend != null) { "demo data: nə joined tədbir, nə dost tapıldı" }
        // ChatView-only marker: mesaj input hinti „Bir mesaj yaz..." —
        // siyahıda belə element YOX (siyahı sətirləri „iştirakçı" substring-i
        // ehtiva etdiyindən subtitle marker işləmirdi).
        val openMarker = "Bir mesaj yaz..."

        compose.setContent { QoshulTheme { ChatsScreen(engine) } }
        settle()

        // 1) Siyahı görünür, ChatView (input) YOX
        compose.onNodeWithText(openMarker).assertDoesNotExist()

        // 2) Söhbəti aç (siyahı sətri — substring: "N iştirakçı · Tədbir qrupu"
        //    və ya "şəhər · Şəxsi")
        // D-151: dost sətri formatı "şəhər · Dost/Match · Şəxsi" — substring
        // "· Şəxsi" hər iki halda unikaldır (tədbir qrupu "Tədbir qrupu" daşıyır).
        val listRowText = if (openEvent) "${ev!!.participants} iştirakçı · Tədbir qrupu"
        else "${friend!!.city} · "
        compose.onAllNodesWithText(listRowText, substring = true)[0].performClick()
        settle()
        compose.onNodeWithText(openMarker).assertExists()

        // 3) Sistem GERİ (BackHandler işə düşməli)
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()

        // 4) ChatView BAĞLANDI, activity YAŞAYIR (app bağlanmır)
        compose.onNodeWithText(openMarker).assertDoesNotExist()
        assertFalse("back açıq çatda app-i bağlamamalıdır", compose.activity.isFinishing)
    }
}
