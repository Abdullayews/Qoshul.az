package az.qoshul.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
    val compose = createComposeRule()

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
        compose.onNodeWithText("Kəşf", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Xəritə", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Söhbətlər", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Profil", substring = true).assertIsDisplayed()
        // tab keçidi
        compose.onNodeWithText("Profil", substring = true).performClick()
        compose.waitForIdle()
        assertTrue("mühərrik çökmədi", engine.state.value.signedIn)
    }
}
