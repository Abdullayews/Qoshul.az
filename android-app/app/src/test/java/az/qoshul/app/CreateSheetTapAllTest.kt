package az.qoshul.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import az.qoshul.app.core.theme.QoshulTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-062: "Bu tabda hər hansısa birşeyə basmaq proqramın çökməsi ilə
 * nəticələnir" — REAL istifadəçi yolu ilə regressiya:
 * Yarat düyməsi → sheet → dropdown AÇ (əvvəl BURADA çökürdü) → seç →
 * doldur → yarat → "Yaratdığım"da görünür.
 *
 * Reproduksiya (fix-dən əvvəl): hər dropdown AÇILANDA
 * IllegalStateException atırdı — DropdownLine-ın açıq siyahısı
 * `verticalScroll().height(200.dp)` idi: scroll node XARİCDƏ qalıb
 * sheet-in verticalScroll-unun SONSUZ hündürlüyü ilə ölçülürdü.
 * Fix: `height(200.dp).verticalScroll(...)` — height XARİCDƏ.
 * (Tam dropdown matrisi: CreateSheetDirectTest — düz kompozisiyada.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CreateSheetTapAllTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun signInDemoAndWait() {
        compose.onNodeWithText("Demo hesabı ilə davam et").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithText("Salam, Demo!").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            } catch (e: IllegalStateException) {
                false
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `yarat axini - dropdown acmaq crash vermir`() {
        compose.setContent { QoshulTheme { QoshulApp() } }
        signInDemoAndWait()

        // mərkəzi Yarat düyməsi → sheet (istifadəçinin çökdüyü tab)
        compose.onNodeWithContentDescription("Yarat").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Birlikdə yeni plan quraq").assertExists()

        // ƏVVƏL BURADA ÇÖKÜRDÜ: kateqoriya sərinə toxunma → açılan siyahı
        // sonsuz constraint ilə ölçülürdü → IllegalStateException.
        compose.onAllNodesWithText("Qəhvə")[0].performScrollTo().performClick()
        compose.waitForIdle()
        // siyahı açıldı — 95 variantlı siyahıdan seçim
        compose.onNodeWithText("Futbol").assertExists()
        compose.onNodeWithText("Futbol").performClick()
        compose.waitForIdle()

        // form doldur + yarat
        compose.onNodeWithText("Məsələn, şənbə günü qəhvə görüşü").performTextInput("Sınaq görüşü")
        compose.onNodeWithText("Birlikdə nə edəcəyik?").performTextInput("Sınaq təsviri")
        compose.onNodeWithText("Tədbir yarat").performScrollTo().performClick()
        compose.waitForIdle()

        // "Yaratdığım" tabında yeni tədbir
        compose.onNodeWithText("Tədbirlər").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Yaratdığım").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sınaq görüşü").assertExists()
    }

    @Test
    fun `sheetde geri duymesu dismiss edir - crash yoxdur`() {
        compose.setContent { QoshulTheme { QoshulApp() } }
        signInDemoAndWait()
        compose.onNodeWithContentDescription("Yarat").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Məsələn, şənbə günü qəhvə görüşü").performTextInput("Sınaq")
        compose.waitForIdle()
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
        // tətbiq yaşayır, ana shell qayıdır
        compose.onNodeWithText("Salam, Demo!").assertExists()
    }
}
