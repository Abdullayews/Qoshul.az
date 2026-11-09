package az.qoshul.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.features.map.MapScreen
import az.qoshul.app.features.shell.AppShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-108 (v0.39.0): XƏRİTƏ BACKEND SEAM SERTİFİKATI (direktiv 2026-11-09:
 * „map da google sdk keçmək üçün hazırlaş“) — keçid tektonikası:
 *
 *  (1) MAPLIBRE DEFAULT — BuildConfig.MAP_BACKEND local.properties
 *      göstərişi olmadan „maplibre“dir (CI-da OpenFreeMap axını dəyişməz);
 *  (2) JİRA YOX — hər iki kanvas (MapLibre FFI / Google Play Services)
 *      JVM testdə kompozisiya olunmur (MapScreen isJvmTest gate) — xəritə
 *      xromu (filtr, sayğac) backenddən asılı deyil, test görünəndir;
 *  (3) KEÇİD = 2 sətir local.properties: MAP_BACKEND=google +
 *      MAPS_API_KEY=AIza… (manifest placeholder) — kod dəyişikliyi YOX.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapBackendTest {

    @get:Rule
    val compose = createComposeRule()

    private fun newEngine() = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `backend default maplibredir ci axtarissiz isleyir`() {
        assertEquals(
            "local.properties-də MAP_BACKEND yoxdursa default maplibre",
            "maplibre",
            BuildConfig.MAP_BACKEND,
        )
    }

    @Test
    fun `mapscreen xromu backendden asili olmayaq render olunur`() {
        // JVM test — kanvas yoxdur (isJvmTest gate), amma filtr paneli,
        // sayğac və tab inteqrasiyası işləyir: seam xromu pozmadı
        val engine = newEngine()
        engine.signIn()
        compose.setContent {
            QoshulTheme {
                MapScreen(engine = engine, nav = NavState(), focus = MapFocusState())
            }
        }
        // filtr tetikleyiciləri (kateqoriya + məsafə)
        compose.onNodeWithText("Hamısı", substring = true).assertIsDisplayed()
        compose.onNodeWithText("10 km", substring = true).assertIsDisplayed()
        compose.waitForIdle()
        assertTrue("mühərrik çökmədi", engine.state.value.signedIn)
    }

    @Test
    fun `shellde xarite tabi kecidli lexte saxlanilir`() {
        // tam qabıqda tab keçidi — MapScreen-in yeni imzası (backend
        // budaqlanması daxildə) AppShell ilə uyğundur
        val engine = newEngine()
        engine.signIn()
        compose.setContent {
            QoshulTheme {
                AppShell(engine = engine, nav = NavState(), onOpenCreate = {})
            }
        }
        compose.onNodeWithText("Xəritə", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Alt naviqasiya paneli").assertIsDisplayed()
    }
}
