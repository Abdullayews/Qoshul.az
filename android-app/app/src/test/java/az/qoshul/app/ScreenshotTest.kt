package az.qoshul.app

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.theme.QoshulTheme
import az.qoshul.app.core.ui.MapFocusState
import az.qoshul.app.core.ui.NavState
import az.qoshul.app.features.auth.AuthScreen
import az.qoshul.app.features.events.CreateEventSheet
import az.qoshul.app.features.events.EventDetailSheet
import az.qoshul.app.features.match.MatchScreen
import az.qoshul.app.features.settings.SettingsScreen
import az.qoshul.app.features.shell.AppShell
import az.qoshul.app.features.venues.VenueDetailScreen
import az.qoshul.app.features.xp.XpCardScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * VİZUAL REQRESSİYA HARNESSİ (D-054) — Robolectric NATIVE graphics ilə
 * Compose UI-ni JVM-də real piksel renderə çevirib PNG yazır.
 * Emulator (KVM) olmadan "proqramın üzünü görmək" üçün: PNG-lər
 * download/shots/ qovluğuna düşür, VLM ilə gözdən keçirilir.
 *
 * autoAdvance: açıq saxlanılır (standart idle sync). Sonsuz animasiya
 * (TypingDots) yalnız açıq chat detalındadır — burada capture olunmur,
 * ona görə waitForIdle asqıya düşmür.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * PNG çıxış qovluğu — mühitdən asılıdır (CI-də sandbox yolu YOXDUR):
     * (1) QOSHUL_SHOTS_DIR env override; (2) lokal sandbox yolu;
     * (3) build/shots (CI — artifact kimi yüklənir); (4) /tmp fallback.
     * Heç biri yazıla bilmirsə shot() ötürülür (test düşmur — PNG optional).
     */
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
            // PNG yazmaq mümkün deyilsə (CI read-only və s.) — ötür, testi düşürtmə
            println("SHOT-SKIP: $name (${t.message})")
        }
    }

    /**
     * Giriş animasiyalarının (StaggerIn/PopIn — delay + fade) bitməsini təmin edir.
     * waitForIdle delay()-də asılı qalmış korutinləri gözləmir — saatı əl ilə
     * irəli salmaq lazımdır, yoxsa alpha=0 kadrları çəkilir.
     */
    private fun settle() {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1600)
        compose.waitForIdle()
    }

    private fun newEngine(): AppEngine {
        val engine = AppEngine(scope = CoroutineScope(Dispatchers.Unconfined))
        engine.signIn()
        return engine
    }

    // ═══════════════ 1. Ana axın: shell + 4 tab ═══════════════

    @Test
    fun `ana ekranlar — kəşf, tədbirlər, söhbətlər, profil`() {
        val engine = newEngine()
        val nav = NavState()
        compose.setContent {
            QoshulTheme { AppShell(engine = engine, nav = nav, onOpenCreate = {}) }
        }
        settle()
        shot("02-home")

        compose.onNodeWithText("Tədbirlər").performClick()
        settle()
        shot("03-events")

        compose.onNodeWithText("Söhbətlər").performClick()
        settle()
        shot("04-chats")

        compose.onNodeWithText("Profil").performClick()
        settle()
        shot("05-profile-top")

        compose.onNodeWithText("Nişanlar").performScrollTo()
        settle()
        shot("06-profile-badges")
    }

    // ═══════════════ 2. Auth ekranı ═══════════════

    @Test
    fun `auth ekranı`() {
        val engine = newEngine()
        compose.setContent { QoshulTheme { AuthScreen(onSignedIn = {}, engine = engine) } }
        settle()
        shot("01-auth")
    }

    // ═══════════════ 3. Tədbir yaratma sheet-i ═══════════════

    @Test
    fun `tədbir yaratma sheeti`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme {
                AppShell(engine = engine, nav = NavState(), onOpenCreate = {})
                CreateEventSheet(engine = engine, onDismiss = {})
            }
        }
        settle()
        shot("07-create")
    }

    // ═══════════════ 4. Tədbir detal sheet-i ═══════════════

    @Test
    fun `tədbir detal sheeti`() {
        val engine = newEngine()
        val event = engine.cityEvents.first()
        compose.setContent {
            QoshulTheme {
                AppShell(engine = engine, nav = NavState(), onOpenCreate = {})
                EventDetailSheet(engine, event, onDismiss = {})
            }
        }
        settle()
        shot("08-event-detail")
    }

    // ═══════════════ 4.5 Match (tanış ol) kart dəsti ═══════════════

    @Test
    fun `match kart desti`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme { MatchScreen(engine = engine, nav = NavState()) }
        }
        settle()
        shot("14-match")
    }

    // ═══════════════ 5. Push ekranları (bir setContent, state əsaslı keçid) ═══════════════

    @Test
    fun `push ekranları — venue, settings, xp kart`() {
        val engine = newEngine()
        val nav = NavState()
        val venueId = engine.partners.first().id
        var screen by mutableIntStateOf(0)
        compose.setContent {
            QoshulTheme {
                androidx.compose.runtime.key(screen) {
                    when (screen) {
                        0 -> VenueDetailScreen(engine, nav, venueId, focus = MapFocusState())
                        1 -> SettingsScreen(engine, nav)
                        else -> XpCardScreen(engine, nav)
                    }
                }
            }
        }
        settle()
        shot("09-venue-detail")

        screen = 1
        settle()
        shot("10-settings")

        screen = 2
        settle()
        shot("11-xp-card")
    }

    // ═══════════════ 6. DPI AUDİTİ — sıx/kompakt ekran (560dpi, w360dp) ═══════════════
    // D-054 tələbi: "hərşey dpi ilə uyğunlaşan olmalıdır" — dəyişən sahələr
    // (ana ekran aktiv zolağı + profil nişanları) fərqli sıxlıqda da düzgün
    // render olunmalıdır. Kompakt en + yüksək sıxlıq = ən stresli hal.

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h740dp-560dpi")
    fun `dpi audit — kompakt sıx ekranda ana ekran və profil`() {
        val engine = newEngine()
        compose.setContent {
            QoshulTheme { AppShell(engine = engine, nav = NavState(), onOpenCreate = {}) }
        }
        settle()
        shot("12-dpi-home")

        compose.onNodeWithText("Profil").performClick()
        settle()
        shot("13-dpi-profile")
    }
}
