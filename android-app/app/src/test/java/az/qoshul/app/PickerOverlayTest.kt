package az.qoshul.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.features.events.CreateEventSheet
import az.qoshul.app.features.events.EventsScreen
import az.qoshul.app.features.shell.AppShell
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-089 (M6) regressiya testləri — "kateqoriya mini" pick sistemi:
 * sürüşdürülən çip zolaqları əvəzinə trigger + üzərinə çıxan panel
 * (istifadəçi direktivi 2026-11-07: "dropdown menu və digər menyuları
 * sürüşdürmək yerinə onların üzərinə çıxıb seçilə bilən kategoriyalar
 * formatına keçirt").
 *
 * Əhatə: EventsScreen (kateqoriya), MapScreen (kateqoriya + məsafə),
 * CreateEventSheet (DropdownLine) — CreateSheetDirectTest əvvəlki
 * dropdown matriksini öz axınında saxlayır.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PickerOverlayTest {

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
    fun `events kategoriyasi — panel uzarina cixir ve secilir`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme { EventsScreen(engine, NavState(), MapFocusState()) }
        }
        settle()

        // bağlı: trigger "Hamısı" göstərir, panel seçimləri YOX
        compose.onNodeWithText("Hamısı").assertExists()
        compose.onNodeWithText("Könüllülük").assertDoesNotExist()

        // aç: panel üzərinə çıxır — bütün 13 qrup görünür
        compose.onNodeWithText("Hamısı").performClick()
        settle()
        compose.onNodeWithText("Könüllülük").assertExists()
        compose.onNodeWithText("Səyahət və digər").assertExists()

        // seç: panel bağlanır, trigger yeni dəyəri göstərir
        // D-110: panel VAHİD SÜTUN siyahısıdır (12 sətir ~700dp > 376dp max)
        // — seçiləcək variant əvvəlcə scroll ilə görünürə gətirilir
        compose.onNodeWithText("Könüllülük").performScrollTo().performClick()
        settle()
        compose.onNodeWithText("Səyahət və digər").assertDoesNotExist()
        // trigger "Könüllülük" — bir dənə (panel bağlı)
        compose.onNodeWithText("Könüllülük").assertExists()
    }

    @Test
    fun `map filtri — kateqoriya ve mesafe panelleri`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme { AppShell(engine = engine, nav = NavState(), onOpenCreate = {}) }
        }
        settle()
        compose.onNodeWithText("Xəritə", substring = true).performClick()
        settle()

        // kateqoriya: Hamısı → Qəhvə (panel opsiyası, sonra trigger)
        compose.onNodeWithText("Hamısı").performClick()
        settle()
        compose.onNodeWithText("Piknik").assertExists()
        compose.onNodeWithText("Qəhvə").performClick()
        settle()
        // trigger artıq "Qəhvə" göstərir; panel bağlı
        compose.onNodeWithText("Piknik").assertDoesNotExist()

        // məsafə: 10 km → 3 km
        compose.onNodeWithText("10 km").performClick()
        settle()
        compose.onNodeWithText("3 km").assertExists()
        compose.onNodeWithText("3 km").performClick()
        settle()
        // trigger "3 km" göstərir — filter dəyişdi (peoplePins ≤ 3km)
        compose.onNodeWithText("3 km").assertExists()
    }
}
