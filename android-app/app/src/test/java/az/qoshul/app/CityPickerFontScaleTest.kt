package az.qoshul.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.features.events.CityPickerSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * U1.4 (UI-09) regressiya testi — audit 2026-11-07:
 * "CityPickerSheet: fixed 420dp LazyColumn nested scrollable Column içində,
 * kiçik ekranda kəsilir".
 *
 * Gözlənti (qəbul meyarı): 360dp en + fontScale 1.5-də siyahı tam görünür və
 * scroll olur — adaptiv fillMaxHeight(0.6f) + scrollable=false sheet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h640dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CityPickerFontScaleTest {

    @get:Rule
    val compose = createComposeRule()

    private fun newEngine(): AppEngine {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        return engine
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(60)
        compose.waitForIdle()
    }

    @Test
    fun `fontScale 1_5 ve 360dp enli ekranda siyahi render olur ve scroll olur`() {
        // RuntimeEnvironment.setFontScale — Robolectric 4.17 (density/fontScale
        // configuration-a yazılır; Compose LocalDensity setContent-də oxunur)
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f)

        val engine = newEngine()
        val cities = engine.dataSource.cities
        assertTrue("demo data: şəhər siyahısı boş deyil", cities.size > 10)
        val firstCity = cities.first().name
        val lastCity = cities.last().name

        var dismissed = false
        compose.setContent {
            QoshulTheme { CityPickerSheet(engine, onDismiss = { dismissed = true }) }
        }
        settle()

        // 1) Sheet başlığı görünür (render OK — kəsilmir)
        compose.onNodeWithText("Şəhərini seç").assertExists()

        // 2) Görünən hissədə şəhər sətirləri var (list boş deyil)
        assertTrue(
            "görünən siyahıda heç bir şəhər yoxdur — siyahı kəsilir",
            compose.onAllNodesWithText(firstCity, substring = true).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText(cities[1].name, substring = true).fetchSemanticsNodes().isNotEmpty(),
        )

        // 3) Siyahı scroll olur: scrollable konteyner üzərindən SON şəhərə qədər
        //    gedilir (lazy item-lar yalnız scroll zamanı kompoze olunur —
        //    performScrollToNode düz API; nested-scroll xətası olsaydı — 420dp
        //    fixed LazyColumn scrollable Column içində — scroll çatmırdı)
        compose.onNode(androidx.compose.ui.test.hasScrollAction())
            .performScrollToNode(androidx.compose.ui.test.hasText(lastCity, substring = true))
        compose.onNodeWithText(lastCity, substring = true).assertExists()
        assertTrue("dismiss yanlışlıqla çağırılmamalıdır", !dismissed)
    }
}
