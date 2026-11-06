package az.qoshul.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.os.Looper

/**
 * U2.2 (UI-02) testi — audit 2026-11-07:
 * "rememberSaveable istifadəsi 0 — proses ölümündə formalar/addımlar sıfırlanır".
 *
 * Hissə A (davranış): rememberSaveable state activity recreate-də sağ qalır —
 * Robolectric ActivityController + host-activity (setContent ONCREATE-da).
 * CI dərsləri: (1) Robolectric runner + @Config + NATIVE zəruri (FINGERPRINT
 * NPE — run 42390482778); (2) compose-test-rule scenario.recreate() işləmir
 * ("No compose hierarchies" — run 42391128810) — ona görə ActivityController.
 * Hissə B (source-audit): U2.2 siyahısındakı ekranlar saveable işlədir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SaveableStateTest {

    /** Host-un kompozisiyası ilə test arasında körpü (statik). */
    private object Hooks {
        var state: androidx.compose.runtime.MutableIntState? = null
    }

    /** rememberSaveable daşıyan host activity — onCreate→setContent.
     *  MutableIntState birbaşa exposed — assert/recreate sinxron olur (frame-dən asılı deyil). */
    private class SaveableHost : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                val step = rememberSaveable { mutableIntStateOf(0) }
                Hooks.state = step
                Column {
                    Text("step=${step.intValue}")
                    Button(onClick = { step.intValue++ }) { Text("Artır") }
                }
            }
        }
    }

    private fun idleMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `saveable state activity recreate de sag qalir`() {
        Hooks.state = null
        val controller: ActivityController<SaveableHost> =
            Robolectric.buildActivity(SaveableHost::class.java)
        controller.setup() // CREATE → RESUME; ilk kompozisiya sinxron: step=0
        assertEquals("ilkin kompozisiya: step 0", 0, Hooks.state?.intValue)

        // iki artırma — birbaşa state üzərindən (frame/rekompozisiyadan asılı deyil)
        Hooks.state!!.intValue++
        Hooks.state!!.intValue++
        assertEquals("iki artırmadan sonra: step 2", 2, Hooks.state?.intValue)

        // Activity recreate = proses ölümünün yaxın modeli (saved instance state)
        controller.recreate()
        // yeni instansiya onCreate(savedInstanceState) → rememberSaveable 2-ni bərpa edir
        assertEquals("recreate-dən sonra saveable state qalır: step 2", 2, Hooks.state?.intValue)
    }

    private fun repoDir(): File {
        var d = File(System.getProperty("user.dir"))
        while (d != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        return checkNotNull(d) { "repo kökü tapılmadı" }
    }

    @Test
    fun `u22 ekranlari saveable isledir source audit`() {
        val src = File(repoDir(), "app/src/main/java/az/qoshul/app/features")
        val expected = mapOf(
            "auth/AuthScreen.kt" to "rememberSaveable",
            "onboarding/OnboardingScreen.kt" to "rememberSaveable",
            "chats/ChatsScreen.kt" to "rememberSaveable",
            "shell/AppShell.kt" to "rememberSaveable",
            "events/CreateFinishSheets.kt" to "rememberSaveable",
            "venues/VenueDetailScreen.kt" to "rememberSaveable",
            "profile/PersonalityQuizScreen.kt" to "rememberSaveable",
        )
        val missing = expected.filter { (rel, pat) ->
            val f = File(src, rel)
            !f.exists() || !f.readText().contains(pat)
        }.keys.toList()
        assertTrue("Saveable istifadə etməyən ekranlar: $missing", missing.isEmpty())
    }
}
