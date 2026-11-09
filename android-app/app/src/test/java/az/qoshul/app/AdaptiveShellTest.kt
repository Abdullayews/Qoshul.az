package az.qoshul.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.features.shell.AppShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * D-102 (v0.35.0): ADAPTİV QABIQ SERTİFİKATI — pəncərə eninə görə naviqasiya
 * rejimi (hesabat §7.4 "v0.35+ adaptive: Navigation bar → rail adaptasiyası
 * qiymətləndirilir"). Hər test öz @Config qualifiers-i ilə pəncərə enini
 * SIMULASİYA edir (Robolectric Configuration → LocalConfiguration.
 * screenWidthDp → widthClassOf):
 *
 *  - Compact  (w412dp): alt FloatingNav (D-105) — "Alt naviqasiya
 *    paneli" VAR, "Yan naviqasiya paneli" YOX (telefon axını: yalnız
 *    alt bar, heç vaxt rail);
 *  - Medium   (w720dp): sol LiquidRail — "Yan..." VAR, "Alt..." YOX;
 *  - Expanded (w900dp): eynilə Medium.
 *
 * PNG çıxışı: vizual audit üçün (D-054 harness dili) — compact/regression
 * və rail kadrları download/shots/ qovluğuna düşür.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveShellTest {

    @get:Rule
    val compose = createComposeRule()

    private val outDir: File by lazy {
        sequenceOf(
            System.getenv("QOSHUL_SHOTS_DIR")?.let(::File),
            File("/home/z/my-project/download/shots"),
            File("build/shots"),
            File("app/build/shots"),
        ).filterNotNull().firstOrNull { it.isDirectory || it.mkdirs() }
            ?: File("/tmp/qoshul-shots").apply { mkdirs() }
    }

    private fun shot(name: String) {
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        try {
            File(outDir, "$name.png").outputStream().use {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (t: Throwable) {
            println("SHOT-SKIP: $name (${t.message})")
        }
    }

    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1600)
        compose.waitForIdle()
    }

    private fun shell() {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        compose.setContent {
            QoshulTheme { AppShell(engine = engine, nav = NavState(), onOpenCreate = {}) }
        }
        settle()
    }

    // ═══════════════ 1. Compact — telefon: alt panel ═══════════════

    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
    fun `compact — alt FloatingNav qalır, rail yoxdur`() {
        shell()
        compose.onNodeWithContentDescription("Alt naviqasiya paneli").assertExists()
        assertEquals(0, compose.onAllNodesWithContentDescription("Yan naviqasiya paneli").fetchSemanticsNodes().size)
        shot("d102-compact-412dp")
    }

    /**
     * D-105: dar telefon (360dp — Android bazasında ən yaygın en).
     * FloatingTabBar həbi weight(1f) ilə çəkilir — hər slot bərabər;
     * ən son etiket ("Profil") GÖRÜNÜR = həddindən artıq genişlənmə
     * (mətn kəsilməsi/daşması) YOXDUR.
     */
    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp-420dpi")
    fun `compact en — 360dp-də həb sığır, etiketlər görünür`() {
        shell()
        compose.onNodeWithContentDescription("Alt naviqasiya paneli").assertExists()
        // dəqiq match — feed-də "Profilini tamamla…" elementi var (substring üst-üstə düşməsin)
        compose.onNodeWithText("Profil").assertIsDisplayed()
        compose.onNodeWithContentDescription("Söhbətlər").assertIsDisplayed()
        shot("d105-compact-narrow-360dp")
    }

    // ═══════════════ 2. Medium — planşet dar / qatlanan açıq ═══════════════

    @Test
    @Config(sdk = [36], qualifiers = "w720dp-h1024dp-420dpi")
    fun `medium — sol LiquidRail, alt panel yoxdur`() {
        shell()
        compose.onNodeWithContentDescription("Yan naviqasiya paneli").assertExists()
        assertEquals(0, compose.onAllNodesWithContentDescription("Alt naviqasiya paneli").fetchSemanticsNodes().size)
        shot("d102-medium-rail-720dp")
    }

    // ═══════════════ 3. Expanded — planşet enli ═══════════════

    @Test
    @Config(sdk = [36], qualifiers = "w900dp-h1280dp-420dpi")
    fun `expanded — sol LiquidRail, alt panel yoxdur`() {
        shell()
        compose.onNodeWithContentDescription("Yan naviqasiya paneli").assertExists()
        assertEquals(0, compose.onAllNodesWithContentDescription("Alt naviqasiya paneli").fetchSemanticsNodes().size)
        shot("d102-expanded-rail-900dp")
    }
}
