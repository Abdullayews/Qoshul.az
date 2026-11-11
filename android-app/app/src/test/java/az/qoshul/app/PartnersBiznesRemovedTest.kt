package az.qoshul.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.core.ui.Route
import az.qoshul.app.features.partners.PartnersScreen
import az.qoshul.app.features.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-131 (v0.44.0): „şəkildəki bölmədən biznes ilə əlaqəli yerləri çıxart,
 * yalnız ayarlarda biznes hesabına keçiləndə olsun“ sertifikatı:
 *   1) Tərəfdaşlar ekranında biznes CTA-ları YOXDUR („Öz məkanını əlavə et“,
 *      „Biznes rejimi“) — yalnız məkanlar + „Görüş təşkil et“;
 *   2) Ayarlar → „Şirkət hesabına keç“ → „Biznes rejimini aç“ →
 *      BusinessModeSheet AÇILIR (biznesin yeganə giriş nöqtəsi).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
class PartnersBiznesRemovedTest {

    @get:Rule
    val compose = createComposeRule()

    private fun engine(): AppEngine {
        val e = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        e.signIn()
        return e
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
    }

    @Test
    fun `tərəfdaşlarda biznes CTA-ları yoxdur — məkanlar və görüş qalır`() {
        val e = engine()
        compose.setContent {
            QoshulTheme {
                PartnersScreen(engine = e, nav = NavState(), onOpenCreateAtVenue = {})
            }
        }
        settle()
        // qalanlar: məkan siyahısı + istifadəçi (host) axını
        compose.onNodeWithText("Görüş təşkil et — XP qazan").assertIsDisplayed()
        compose.onNodeWithText("Sevdiyin məkanlar").assertIsDisplayed()
        // ləğv edilənlər: biznes CTA kartları
        assertEquals(
            "„Öz məkanını əlavə et“ Tərəfdaşlarda olmamalıdır",
            0, compose.onAllNodesWithText("Öz məkanını əlavə et").fetchSemanticsNodes().size,
        )
        assertEquals(
            "„Biznes rejimi — XP endirimi ver“ Tərəfdaşlarda olmamalıdır",
            0, compose.onAllNodesWithText("Biznes rejimi — XP endirimi ver").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `ayarlardan şirkət dialoqu biznes rejimini açır`() {
        val e = engine()
        val nav = NavState()
        compose.setContent {
            QoshulTheme { SettingsScreen(engine = e, nav = nav) }
        }
        settle()
        // „Şirkət hesabına keç“ → dialoq → „Biznes rejimini aç“ → sheet.
        // Düymə siyahının SONUNA yaxındır — lazy list-də hələ kompozisiya
        // olunmayıb: əvvəlcə siyahını performScrollToNode ilə ora sürüşdür
        // (DesignSettingsTest-in performScrollTo dilindən fərqli — o elementlər
        // viewport içində idi; bu son elementdir).
        compose.onNode(hasScrollAction())
            .performScrollToNode(hasText("Şirkət hesabına keç"))
        compose.onNodeWithText("Şirkət hesabına keç").performClick()
        settle()
        compose.onNodeWithText("Biznes rejimini aç").assertIsDisplayed()
        compose.onNodeWithText("Biznes rejimini aç").performClick()
        settle()
        // BusinessModeSheet açılır — işçi modunun başlığı görünür
        compose.onNodeWithText("Biznes rejimi — XP endirimi").assertIsDisplayed()
        assertEquals("Route stackində Settings qalır", 0, nav.stack.size)
    }
}
