package az.qoshul.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.DesignColor
import az.qoshul.app.core.theme.DesignMode
import az.qoshul.app.core.theme.Q
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.features.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D-118/D-119 (v0.42.0) dizayn sistemi testləri — istifadəçi direktivi
 * 2026-11-10: „#008b2d main rəng olacaq, ayarlarda dizayn bölməsində ağ və
 * qara modu əlavə olunacaq, seçim olaraq rənglər #008b2d (default) və
 * #eac66c. ağ (default) və qara“.
 *
 * Əhatə:
 *  - Q.apply matrisi (rəng × mod → token dəstləri — saf yoxlama);
 *  - AppEngine setterləri (state + Q tokenləri birgə dəyişir);
 *  - Ayarlar → Dizayn UI axını (dialoq → seçim → dərhal tətbiq).
 *
 * QEYD: Q GLOBALdır (snapshot-backed) — hər testə əvvəl/sonra SIFIRLAMA
 * (@Before/@After) qoyulur ki, digər testlərin default (Ağ+Yaşıl) dili
 * pozulmasın.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSettingsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun reset() = Q.apply(DesignColor.GREEN, DesignMode.LIGHT)

    @Before
    fun before() = reset()

    @After
    fun after() = reset()

    private fun newEngine(): AppEngine {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        return engine
    }

    // ═══════════════════ 1. Q.apply matrisi (D-118) ═══════════════════

    @Test
    fun `default — yasil ve ag (yeni brend 008B2D)`() {
        // başlanğıc (Q obyektinin ilkin dəyərləri) = AĞ + YAŞIL
        assertEquals(Color(0xFF008B2D), Q.primary)
        assertFalse(Q.isDark)
        assertEquals(Color(0xFFF8FAFC), Q.background)
        assertEquals(Color(0xFFFFFFFF), Q.primaryFg)
    }

    @Test
    fun `apply GOLD LIGHT — primary EAC66C, primaryFg tund`() {
        Q.apply(DesignColor.GOLD, DesignMode.LIGHT)
        assertEquals(Color(0xFFEAC66C), Q.primary)
        // qızılı üstündə ağ yazı 1.6:1 — oxunmaz; primaryFg TÜND dəri
        assertEquals(Color(0xFF3A2B07), Q.primaryFg)
        assertFalse(Q.isDark)
    }

    @Test
    fun `apply GREEN DARK — qara neytrallar, primary qalir`() {
        Q.apply(DesignColor.GREEN, DesignMode.DARK)
        assertTrue(Q.isDark)
        assertEquals(Color(0xFF0B100D), Q.background)
        assertEquals(Color(0xFFECF3EE), Q.foreground)
        assertEquals(Color(0xFF141B16), Q.card)
        // brend rəngi moddan asılı deyil (rəng seçimidir)
        assertEquals(Color(0xFF008B2D), Q.primary)
    }

    @Test
    fun `apply GOLD DARK — hibrid qizili brend ve qara neytrallar`() {
        Q.apply(DesignColor.GOLD, DesignMode.DARK)
        assertTrue(Q.isDark)
        assertEquals(Color(0xFFEAC66C), Q.primary)
        assertEquals(Color(0xFF3A2B07), Q.primaryFg)
    }

    @Test
    fun `apply geri GREEN LIGHT — tam bərpa`() {
        Q.apply(DesignColor.GOLD, DesignMode.DARK)
        reset()
        assertEquals(Color(0xFF008B2D), Q.primary)
        assertFalse(Q.isDark)
        assertEquals(Color(0xFFF8FAFC), Q.background)
    }

    // ═══════════════════ 2. AppEngine setterləri (D-119) ═══════════════════

    @Test
    fun `setDesignColor — state və Q birgə dəyişir`() {
        val engine = newEngine()
        assertEquals(DesignColor.GREEN, engine.state.value.designColor)
        engine.setDesignColor(DesignColor.GOLD)
        assertEquals(DesignColor.GOLD, engine.state.value.designColor)
        assertEquals(Color(0xFFEAC66C), Q.primary)
        // mod dəyişməz
        assertFalse(Q.isDark)
    }

    @Test
    fun `setDesignMode — state və Q birgə dəyişir`() {
        val engine = newEngine()
        engine.setDesignMode(DesignMode.DARK)
        assertEquals(DesignMode.DARK, engine.state.value.designMode)
        assertTrue(Q.isDark)
        assertEquals(Color(0xFF0B100D), Q.background)
    }

    // ═══════════════════ 3. Ayarlar → Dizayn UI axını (D-119) ═══════════════════

    @Test
    fun `ayarlar dizayn — reng dialoqu Qizili secer`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme {
                SettingsScreen(engine = engine, nav = NavState())
            }
        }
        compose.waitForIdle()
        // Dizayn bölməsi — Rəng sətri görünür və cari dəyər Yaşıl-dır
        compose.onNodeWithText("Rəng").performScrollTo()
        compose.onNodeWithText("Yaşıl").assertExists()
        // dialoq açılır
        compose.onNodeWithText("Rəng").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Qızılı").performClick()
        compose.waitForIdle()
        // dərhal tətbiq: state + Q tokenləri
        assertEquals(DesignColor.GOLD, engine.state.value.designColor)
        assertEquals(Color(0xFFEAC66C), Q.primary)
    }

    @Test
    fun `ayarlar dizayn — mod dialoqu Qara secer`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme {
                SettingsScreen(engine = engine, nav = NavState())
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Mod").performScrollTo()
        compose.onNodeWithText("Mod").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Qara").performClick()
        compose.waitForIdle()
        assertEquals(DesignMode.DARK, engine.state.value.designMode)
        assertTrue(Q.isDark)
        assertEquals(Color(0xFF0B100D), Q.background)
    }
}
