package az.qoshul.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.activity.ComponentActivity
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-051: "event yaratmağa çalışanda proqram çökür" — regressiya testləri.
 *
 * Reproduksiya (fix-dən əvvəl): CreateEventSheet-in ilk kompozisiyası
 * CreateFinishSheets.kt:94-də NPE atırdı — initial==null ikən
 * (initial?.capacity ?: 8) in 2..500 → true → initial!! partlayırdı.
 * "Yarat" düyməsini vuran İSTİFADƏÇİ bu crash-i alırdı.
 *
 * Bu testlər tam istifadəçi axınını JVM-də (Robolectric) sürür:
 * demo daxil ol → Yarat → sheet doldur → Tədbir yarat → yoxla → detal aç.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CreateEventFlowTest {

    // createAndroidComposeRule — geri-düymə testi üçün activity.onBackPressedDispatcher
    // lazımdır (createComposeRule host activity-ni açmır)
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()


    /**
     * D-149: demo düyməsi silindi — testlər seed-li engine inyeksiya edir
     * (QoshulApp(engineOverride)). signInDemo köhnə profil dəyərlərini
     * burada bərpa edir (D-050 semantikası: onboarding keçilib, dolu profil).
     */
    private fun seededEngine(): AppEngine {
        val e = AppEngine()
        e.setProfileCreation(
            "Demo", "İstifadəçi", java.time.LocalDate.of(2000, 1, 1),
            gdr = "", bioText = "Qoshul demo — UI test seed.", photos = 1,
        )
        e.setInterests(listOf("Qəhvə", "Yürüş", "Stolüstü oyunlar", "Foto-gəzinti"))
        e.signIn()
        // signInDemo pariteti: onboarding KEÇİLMƏLİDİR (finishOnboarding
        // onboardedV=true edir + qeydiyyat XP + günlük giriş) — yoxsa
        // QoshulApp OnboardingScreen göstərir və "Sənin üçün" heç vaxt gəlmir
        e.finishOnboarding()
        return e
    }

    /** Engine hazır + onboarding keçilib — „Sənin üçün" gözlə. */
    private fun signInWithSeededEngine() {
        compose.setContent { QoshulTheme { QoshulApp(engineOverride = seededEngine()) } }
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithText("Sənin üçün").fetchSemanticsNode()
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
    fun `tam axin - yarad sheet doldurulur ve tedbir yaranir`() {
        // D-149: seed-li engine inyeksiyası (demo düyməsi silindi)
        signInWithSeededEngine()
        compose.onNodeWithText("Sənin üçün").assertIsDisplayed()

        // 2) mərkəzi Yarat düyməsi → sheet açılır (əvvəl BURADA NPE)
        compose.onNodeWithContentDescription("Yarat").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Birlikdə yeni plan quraq").assertExists()

        // 3) başlıq + təsvir doldur (enabled şərti: hər ikisi dolu)
        compose.onNodeWithText("Məsələn, şənbə günü qəhvə görüşü").performTextInput("Sınaq görüşü")
        compose.onNodeWithText("Birlikdə nə edəcəyik?").performTextInput("Sınaq təsviri")

        // 4) Yarat düyməsi (sheet daxilində)
        compose.onNodeWithText("Tədbir yarat").performScrollTo().performClick()
        compose.waitForIdle()

        // 5) sheet bağlandı; "Yaratdığım" tabında yeni tədbir görünür
        compose.onNodeWithText("Tədbirlər").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Yaratdığım").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sınaq görüşü", substring = true).assertExists()
    }

    @Test
    fun `kateqoriya filtri ile 1-2 tedbirli siyahida crash olmamalidir`() {
        signInWithSeededEngine()

        // Tədbirlər tabı (dəqiq match — banner "Tədbirlər bölməsində" eyniləşdirməsin deyil)
        compose.onNodeWithText("Tədbirlər").performClick()
        compose.waitForIdle()

        // az tədbirli kateqoriya seç (siyahıda 1-2 element qalır).
        // D-089: kateqoriya artıq overlay paneldən seçilir — əvvəlcə
        // trigger ("Hamısı") açır, sonra paneldən seçir
        compose.onNodeWithText("Hamısı").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Könüllülük").performClick()
        compose.waitForIdle()

        // heç bir istisna atılmamalıdır (proRanked[1] IndexOutOfBounds əvəzinə)
        compose.onNodeWithText("Yaxınlaşan", substring = true).assertExists()
    }

    @Test
    fun `tedbir detali sheeti crash-siz acilir`() {
        signInWithSeededEngine()

        compose.onNodeWithText("Tədbirlər").performClick()
        compose.waitForIdle()

        // "Sənin səviyyənə görə" kartına toxunuş → EventDetailSheet açılır
        // (zəif nöqtə: detail sheet-in öz daxilində crash ola bilərdi)
        compose.onNodeWithText("Blits şahmat turniri", substring = true)
            .performScrollTo().performClick()
        compose.waitForIdle()
        // detail sheet açıldı — unikal alt sətir (yalnız orda görünür)
        compose.onNodeWithText("tərəfindən təşkil olunur", substring = true).assertExists()
    }

    /**
     * D-053: sistem geri düyməsi push-ekranı POP etməlidir — bütün tətbiqi
     * bağlamamalı. Əvvəl heç bir BackHandler yox idi: geri basanda
     * onBackPressed default olaraq Activity-ni bitirirdi (tətbiq bağlanırdı).
     */
    @Test
    fun `push ekranda geri duymesu routu pop edir - app baglanmir`() {
        signInWithSeededEngine()

        // header-dəki Tərəfdaşlar ikonu → Partners push-ekranı
        compose.onNodeWithContentDescription("Tərəfdaşlar").performClick()
        compose.waitForIdle()
        // push-ekranın başlığı görünür (ana shell yox — AnimatedContent yalnız targeti qurur)
        compose.onNodeWithText("Tərəfdaşlar").assertExists()
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithText("Sənin üçün").fetchSemanticsNode()
                false
            } catch (e: AssertionError) {
                true
            }
        }

        // sistem geri düyməsi simulyasiyası (BackHandler dispatcher-ə gəlir)
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()

        // BackHandler routu pop etdi: ana shell QAYITMALI (əks halda activity
        // bitir və "Salam, Demo!" əsla görünməzdi)
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithText("Sənin üçün").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        // keçid animasiyası bitəndən sonra push başlığı tam yox olmalıdır
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithText("Tərəfdaşlar").fetchSemanticsNode()
                false
            } catch (e: AssertionError) {
                true
            }
        }
    }

    /**
     * D-053: Yarat sheetində tarix dropdown başlanğıcı "Sabah" etiketidir
     * (əvvəl "01.11" — options siyahısında olmayan format) və capacity
     * sahəsi ara dəyərləri rədd etmir (əvvəl "1" yazmaq silinirdi: 1 ∉ 2..500).
     */
    @Test
    fun `yarat sheetinde tarix etiketi ve capacity yazimi duzgundur`() {
        signInWithSeededEngine()

        compose.onNodeWithContentDescription("Yarat").performClick()
        compose.waitForIdle()

        // default tarix = sabah → dropdown başlığı "Sabah" (options-la eyni format)
        compose.onNodeWithText("Sabah").assertExists()

        // capacity sahəsi: "8" → "1" yaz (ara dəyər — əvvəl rədd edilirdi)
        compose.onNodeWithText("8").performTextReplacement("1")
        compose.waitForIdle()
        // yalnız redaktə edilən sahəyə bax (header-dəki oxunmamış nişanı "1"
        // ilə qarışmasın deyə hasSetTextAction filtri)
        compose.onNode(hasSetTextAction() and hasText("1")).assertExists()
    }
}
