package az.qoshul.app

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.features.events.CreateEventSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-062: dropdown-ların açılıb-seçilməsi — DÜZ kompozisiya (feed qarışığı
 * olmadan): CreateEventSheet birbaşa compose olunur (ScreenshotTest üsulu).
 * Hər dropdown: aç → option DÜYÜNÜ exists → seç → kollapsda yeni dəyər.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CreateSheetDirectTest {

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

    /** Kollaps sətirinə klik ([current] görünən dəyər) → aç.
     *  D-063: forma sahələrinin yeri dəyişə bilər — əvvəl scroll et
     *  (görünməyən node-a klik səssiz uğursuz olur — D-062 dərsi). */
    private fun open(current: String) {
        compose.onAllNodesWithText(current)[0].performScrollTo().performClick()
        settle()
    }

    /** Açıq listdən [option] seç (yalnız option kimi mövcud olan mətn).
     *  D-063: uzun siyahılarda option scroll gətirilərək kliklənir. */
    private fun pick(option: String) {
        compose.onNodeWithText(option).performScrollTo().performClick()
        settle()
    }

    @Test
    fun `butun dropdownlar acilib-secilir crash yoxdur`() {
        val engine = newEngine()
        compose.setContent { QoshulTheme { CreateEventSheet(engine = engine, onDismiss = {}) } }
        settle()

        // ── Kateqoriya: Qəhvə → Futbol ──
        open("Qəhvə")
        compose.onNodeWithText("Futbol").assertExists()   // siyahı açıldı
        pick("Futbol")

        // ── Format: Oflayn → Onlayn → geri Oflayn ──
        open("Oflayn")
        compose.onNodeWithText("Onlayn").assertExists()
        pick("Onlayn")
        compose.onNodeWithText("Məkan və ünvan").assertDoesNotExist()
        open("Onlayn")
        compose.onNodeWithText("Oflayn").assertExists()
        pick("Oflayn")
        compose.onNodeWithText("Məkan və ünvan").assertExists()

        // ── Şəhər: Bakı → Sumqayıt ──
        open("Bakı")
        compose.onNodeWithText("Sumqayıt").assertExists()
        pick("Sumqayıt")

        // ── Tarix: Sabah → Bu gün ──
        open("Sabah")
        compose.onNodeWithText("Bu gün").assertExists()
        pick("Bu gün")

        // ── Saat: 18:00 → 06:00 ──
        open("18:00")
        compose.onNodeWithText("06:00").assertExists()
        pick("06:00")

        // ── İştirak haqqı: Pulsuz → Ödənişli → Pulsuz ──
        // D-063: forma sahələrinin boşluqları artdı — açılan 200dp siyahı
        // viewport-dan aşağı düşüb klik səssiz itirdi. Əvvəl formanın ALTDAKI
        // elementini görünürə gətir → haqq dropdown-u viewportun yuxarısında
        // qalır və açılan siyahı sığışır (D-062 "görünməyən node" dərsi).
        compose.onNodeWithText("Qoşulma istəklərini özüm təsdiqləyim").performScrollTo()
        settle()
        open("Pulsuz")
        compose.onNodeWithText("Ödənişli").assertExists()
        pick("Ödənişli")
        // Ödənişli rejimdə "Qiymət (AZN)" sətri açılır (input sahəsi görünür)
        compose.onNodeWithText("Qiymət (AZN)").assertExists()
        open("Ödənişli")
        compose.onNodeWithText("Pulsuz").assertExists()
        pick("Pulsuz")

        // ── Yaş həddi: Hamı üçün → 18+ ──
        open("Hamı üçün")
        compose.onNodeWithText("18+").assertExists()
        pick("18+")

        // ── Checkbox ──
        compose.onNodeWithText("Qoşulma istəklərini özüm təsdiqləyim").performClick()
        settle()
        compose.onNodeWithText("Qoşulma istəklərini özüm təsdiqləyim").performClick()
        settle()

        // ── Mətn sahələri ──
        compose.onNodeWithText("Məsələn, şənbə günü qəhvə görüşü").performTextInput("Sınaq görüşü")
        compose.onNodeWithText("Məkanın adı və ünvanı").performTextInput("Sınaq mekanı")
        compose.onNode(hasSetTextAction() and hasText("8")).performTextReplacement("12")
        compose.onNodeWithText("tələbələr, açıq hava, pulsuz").performTextInput("açiq hava")

        // ── Təsvir (AI-dən ƏVVƏL — hint boş olanda görünür) ──
        compose.onNodeWithText("Birlikdə nə edəcəyik?").performTextInput("Sınaq təsviri")

        // ── AI doldur (idea → 1.1s → title/desc/kateqoriya/say doldurulur) ──
        compose.onNodeWithText("Məsələn, şənbə səhəri dağa yürüş").performTextInput("şənbə səhəri dağa yürüş")
        compose.onNodeWithText("Tədbiri AI ilə doldur").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            try { compose.onNodeWithText("Şənbə səhəri dağa yürüş").fetchSemanticsNode(); true }
            catch (e: AssertionError) { false }
        }
        compose.waitForIdle()

        // ── Yarat (performClick real toxunmadır — əvvəl görünəndə gətir) ──
        compose.onNodeWithText("Tədbir yarat").performScrollTo().performClick()
        settle()

        // ── Yoxlama: mühərrik state-ində yeni tədbir (UI-dan asılı olmayaraq) ──
        val st = engine.state.value
        check(st.events.any { it.title == "Şənbə səhəri dağa yürüş" && it.host == st.profileName }) {
            "yaradılan tədbir engine state-də yoxdur: ${st.events.map { it.title }}"
        }
    }
}
