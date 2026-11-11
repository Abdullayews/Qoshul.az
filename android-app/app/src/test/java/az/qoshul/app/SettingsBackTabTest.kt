package az.qoshul.app

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.GlassNavHost
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.features.settings.SettingsScreen
import az.qoshul.app.features.shell.AppShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-127 (v0.44.0): „ayarlara girib çıxandan sonra istifadəçi Profil
 * tabında qalsın, menyuya yönləndirilməsin“ sertifikatı.
 *
 * Mexanizm: GlassNavHost hər ekranı SaveableStateProvider içində çəkir —
 * push açılanda home-un rememberSaveable state-i (AppShell-in tab index-i)
 * saxlanılır, pop-da BƏRPA OLUNUR. Əvvəl index=0-a sıfırlanırdı.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
class SettingsBackTabTest {

    @get:Rule
    val compose = createComposeRule()

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
    }

    /** Profil görünüşünün landmark-ı — yalnız Profil tabında görünür
     *  (ikon contentDescription — ProfileScreen.kt:124). */
    private fun onProfile(): Boolean =
        compose.onAllNodesWithContentDescription("Profili redaktə et")
            .fetchSemanticsNodes().isNotEmpty()

    private fun host(nav: NavState, engine: AppEngine) {
        compose.setContent {
            QoshulTheme {
                GlassNavHost(
                    nav = nav,
                    home = {
                        AppShell(engine = engine, nav = nav, onOpenCreate = {})
                    },
                ) { route ->
                    if (route is Route.Settings) SettingsScreen(engine = engine, nav = nav)
                }
            }
        }
    }

    @Test
    fun `profilde ayarlara girib cixanda profil qalir`() {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        val nav = NavState()
        host(nav, engine)
        settle()

        // Profil tabına keç
        compose.onNodeWithText("Profil").performClick()
        settle()
        assertEquals("Profil görünüşündəyik", true, onProfile())

        // Ayarları aç (push) — home kompozisiyadan çıxır (ikon contentDescription)
        compose.onNodeWithContentDescription("Ayarlar").performClick()
        settle()
        assertEquals("Ayarlar push edildi", 1, nav.stack.size)
        assertEquals("Ayarlar açıqdır", false, onProfile())

        // GERİ — tab index-i BƏRPA olunmalıdır (D-127: əvvəl Ana menyuya düşürdü)
        nav.pop()
        settle()
        assertEquals("Stack boşaldı", 0, nav.stack.size)
        assertEquals(
            "Ayarlardan qayıdanda PROFİL tabında qalmalıdır (D-127)",
            true, onProfile(),
        )
    }

    @Test
    fun `her iki yolla geri qayidanda tab qorunur`() {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        val nav = NavState()
        host(nav, engine)
        settle()

        compose.onNodeWithText("Profil").performClick()
        settle()
        compose.onNodeWithContentDescription("Ayarlar").performClick()
        settle()

        // Sistem GERİ düyməsi (BackHandler) — eyni nəticə verməlidir
        compose.runOnIdle { }
        nav.pop()
        settle()
        assertEquals("Back sonrası Profil qalır", true, onProfile())
    }
}
