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
import az.qoshul.app.features.map.resolveMapBackend
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
 * D-108/D-109 (v0.39.0/v0.40.0): XƏRİTƏ BACKEND SEAM SERTİFİKATI
 * (direktivlər 2026-11-09: „map da google sdk keçmək üçün hazırlaş“ +
 * „google ilə dəyişdirib“) — keçid tektonikası:
 *
 *  (1) GOOGLE ƏSAS — MAP_BACKEND mənbəyi local.properties → env (CI)
 *      → default „google“; amma açar (MAPS_API_KEY) yoxdursa Google
 *      SDK tile yükləyə bilməz → resolveMapBackend maplibre FALLBACK
 *      seçir (D-048 açarsız OpenFreeMap — xəritə heç vaxt boş qalmır);
 *  (2) JİRA YOX — hər iki kanvas (MapLibre FFI / Google Play Services)
 *      JVM testdə kompozisiya olunmur (MapScreen isJvmTest gate) — xəritə
 *      xromu (filtr, sayğac) backenddən asılı deyil, test görünəndir;
 *  (3) KEÇİD = 1 sətir: MAPS_API_KEY=AIza… (local.properties və ya CI
 *      secret MAPS_API_KEY — native.yml ötürür) — kod dəyişikliyi YOX.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapBackendTest {

    @get:Rule
    val compose = createComposeRule()

    private fun newEngine() = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `google acar olanda esas backend secilir`() {
        assertEquals(
            "MAP_BACKEND=google + açar → GoogleMapCanvas",
            "google",
            resolveMapBackend(configured = "google", keyPresent = true),
        )
    }

    @Test
    fun `acarsiz google maplibre fallbackdur`() {
        // Açar yoxdursa Google SDK tile yükləyə bilməz — xəritə boş
        // qalmasın deyə D-048 açarsız axını devralır (D-109)
        assertEquals(
            "MAP_BACKEND=google, açar YOX → maplibre fallback",
            "maplibre",
            resolveMapBackend(configured = "google", keyPresent = false),
        )
    }

    @Test
    fun `maplibre aciq secimde acardan asili deyil`() {
        assertEquals(
            "MAP_BACKEND=maplibre həmişə maplibre (açar məcburi deyil)",
            "maplibre",
            resolveMapBackend(configured = "maplibre", keyPresent = true),
        )
        assertEquals(
            resolveMapBackend("maplibre", false),
            resolveMapBackend("maplibre", true),
        )
    }

    @Test
    fun `buildconfig backend deyeri duzgun formadir`() {
        // D-109: default google (local.properties/env göstərişi YOXDURSA);
        // build konfiqi hər machine-də bu ikisindən biri olmalıdır
        assertTrue(
            "MAP_BACKEND ya google, ya maplibre olmalıdır: ${BuildConfig.MAP_BACKEND}",
            BuildConfig.MAP_BACKEND == "google" || BuildConfig.MAP_BACKEND == "maplibre",
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
