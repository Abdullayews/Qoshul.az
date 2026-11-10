package az.qoshul.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Wordmark
import az.qoshul.app.features.auth.AuthScreen
import az.qoshul.app.features.shell.AppShell
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
 * Compose UI smoke testləri — Robolectric 4.17, JVM-də (D-042).
 *
 * Flutter-in 28 widget/no-op testindən native-ə düşən paritet boşluğunu
 * bağlayır: əsas səhnələr REENDER olunur + rejim/keçid qarşılıqlı işləyir
 * (emulator tələb etmir, CI-da işləyir). Pixel-dəqiq iddialar
 * instrumentasiya fazasına qalır (audit bölmə 8 qeydi).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun newEngine() = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `brend wordmark render olunur`() {
        compose.setContent { QoshulTheme { Wordmark(fontSize = 25) } }
        compose.onNodeWithText("qoshul").assertIsDisplayed()
        compose.onNodeWithText(".az").assertIsDisplayed()
    }

    @Test
    fun `auth daxil ol rejimi render olunur və qeydiyyata keçid işləyir`() {
        compose.setContent {
            QoshulTheme { AuthScreen(onSignedIn = {}) }
        }
        compose.onNodeWithText("Xoş gəldin!").assertIsDisplayed()
        // düymə scroll konteynerində aşağıda qala bilər → mövcudluq + scroll
        compose.onNodeWithText("Daxil ol").assertExists()
        // rejim keçidi: ENTER → UP (link səhifənin altındadır → scroll)
        compose.onNodeWithText("Hesabın yoxdur? Qeydiyyatdan keç")
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Hesab yarat").assertExists()
        compose.onNodeWithText("Qeydiyyatdan keç").assertExists()
    }

    @Test
    fun `shell tabları render olunur və tab dəyişməsi işləyir`() {
        val engine = newEngine()
        engine.signIn()
        compose.setContent {
            QoshulTheme {
                AppShell(engine = engine, nav = NavState(), onOpenCreate = {})
            }
        }
        compose.onNodeWithText("Ana menyu", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Xəritə", substring = true).assertIsDisplayed()
        // D-105: Söhbətlər artıq standalone dairədir (mətn yoxdur —
        // yığılmış halın sağ düyməsi) — contentDescription ilə tapılır
        compose.onNodeWithContentDescription("Söhbətlər").assertIsDisplayed()
        compose.onNodeWithText("Profil", substring = true).assertIsDisplayed()
        // tab keçidi
        compose.onNodeWithText("Profil", substring = true).performClick()
        compose.waitForIdle()
        assertTrue("mühərrik çökmədi", engine.state.value.signedIn)
    }

    /**
     * UI-7 (D-098): Söhbətlər tabına girişdə nav gizlənir (hover
     * animasiyası), sistem GERİ isə Ana menyuya qaytarır və nav qayıdır.
     */
    @Test
    fun `söhbətlərdə nav gizlidir geri qayıdanda görünür`() {
        val engine = newEngine()
        engine.signIn()
        compose.setContent {
            QoshulTheme {
                AppShell(engine = engine, nav = NavState(), onOpenCreate = {})
            }
        }
        // əvvəl nav görünür
        compose.onNodeWithText("Ana menyu", substring = true).assertIsDisplayed()

        // Söhbətlər tabına keç (D-105: dairə — contentDescription) → nav gizlənir
        compose.onNodeWithContentDescription("Söhbətlər").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("Ana menyu", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Profil", substring = true).assertDoesNotExist()

        // sistem GERİ (BackHandler) → Ana menyu tabı + nav qayıdır
        // (performPressBack bu ui-test versiyasında yoxdur — dispatcher
        // vasitəsilə, ChatsBackHandlerTest-in sübut olunmuş pattern-i)
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.onNodeWithText("Ana menyu", substring = true).assertIsDisplayed()
    }
}
