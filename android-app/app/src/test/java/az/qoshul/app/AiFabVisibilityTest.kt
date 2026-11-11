package az.qoshul.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
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
 * D-130 (v0.44.0): AI FAB görünüş sertifikatı — direktiv 2026-11-11:
 * „ai düyməsini dairədə, liquid glass, nav barın üstündə sağda, YALNIZ
 * xəritə və chat-da gizli“. Yəni:
 *   - Ana menyu (index 0, hər iki daxili görünüş) → GÖRÜNÜR
 *   - Xəritə (index 1) → GİZLİ
 *   - Söhbətlər (index 2) → GİZLİ
 *   - Profil (index 3) → GÖRÜNÜR
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
class AiFabVisibilityTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shell() {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        compose.setContent {
            QoshulTheme { AppShell(engine = engine, nav = NavState(), onOpenCreate = {}) }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
    }

    private fun fabCount(): Int =
        compose.onAllNodesWithContentDescription("AI köməkçi").fetchSemanticsNodes().size

    @Test
    fun `ana menyuda görünür — hər iki daxili görünüşdə`() {
        shell()
        // Sənin üçün görünüşü
        assertEquals(1, fabCount())
        compose.onNodeWithContentDescription("AI köməkçi").assertIsDisplayed()
        // Tədbirlər görünüşünə keçid (D-130: görünüş fərqi YOXDUR — hər ikisində görünür)
        compose.onNodeWithText("Tədbirlər").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        assertEquals(1, fabCount())
    }

    @Test
    fun `xəritə tabında gizlidir`() {
        shell()
        compose.onNodeWithText("Xəritə").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        assertEquals("Xəritədə AI FAB gizli olmalıdır", 0, fabCount())
    }

    @Test
    fun `söhbətlər tabında gizlidir`() {
        shell()
        compose.onNodeWithContentDescription("Söhbətlər").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        assertEquals("Söhbətlərdə AI FAB gizli olmalıdır", 0, fabCount())
    }

    @Test
    fun `profil tabında görünür`() {
        shell()
        compose.onNodeWithText("Profil").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(700)
        compose.waitForIdle()
        assertEquals("Profil-də AI FAB görünməlidir", 1, fabCount())
    }
}
