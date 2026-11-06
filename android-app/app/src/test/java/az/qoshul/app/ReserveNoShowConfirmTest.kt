package az.qoshul.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.ResvStatus
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.features.reservations.ReservationsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * U1.5 (UI-30) regressiya testi — audit 2026-11-07:
 * "„Gəlməyəcəm" bir toxunuşla no-show yazır (3-ü = 7 gün qadağa) — təsdiqsiz".
 *
 * Gözlənti: „Gəlməyəcəm" toxunuşu DİALOQ açır; təsdiqə qədər no-show YAZILMIR;
 * „Bəli, bildir" → status NO_SHOW.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReserveNoShowConfirmTest {

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
    fun `gelmeyecem toxunusu tesdiq teleb edir ve tesdiqsiz yazilmir`() {
        val engine = newEngine()
        // Rezerv yarat (demo data-da rezerv yoxdur — axın özü yaradır)
        val venue = engine.partners.first()
        engine.createReservation(venue, date = "Bu şənbə", time = "19:00", party = 2)
        assertFalse(
            "hazırlıq: rezerv canCancel olmalıdır",
            engine.state.value.reservations.none { it.canCancel },
        )

        compose.setContent { QoshulTheme { ReservationsScreen(engine, NavState()) } }
        settle()

        // 1) „Gəlməyəcəm" sətri görünür → toxun
        compose.onNodeWithText("Gəlməyəcəm — əvvəlcədən bildir", substring = true)
            .performClick()
        settle()

        // 2) Təsdiq dialoqu açıldı; NO_SHOW HƏLƏLİK yazılmayıb
        compose.onNodeWithText("Gəlməyəcəyini bildir").assertExists()
        assertFalse(
            "dialoq açıqdır, lakin no-show hələ yazılmamalıdır",
            engine.state.value.reservations.any { it.status == ResvStatus.NO_SHOW },
        )

        // 3) „Ləğv et" → dialoq qapanır, no-show YOX
        compose.onNodeWithText("Ləğv et").performClick()
        settle()
        compose.onNodeWithText("Gəlməyəcəyini bildir").assertDoesNotExist()
        assertFalse(engine.state.value.reservations.any { it.status == ResvStatus.NO_SHOW })

        // 4) Yenidən aç → „Bəli, bildir" → status NO_SHOW
        compose.onNodeWithText("Gəlməyəcəm — əvvəlcədən bildir", substring = true)
            .performClick()
        settle()
        compose.onNodeWithText("Bəli, bildir").performClick()
        settle()
        assertTrue(
            "təsdiqdən sonra no-show yazılmalıdır",
            engine.state.value.reservations.any { it.status == ResvStatus.NO_SHOW },
        )
    }
}
