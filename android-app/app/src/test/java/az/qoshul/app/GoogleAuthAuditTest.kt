package az.qoshul.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * D-165 qoruyucu source-audit testi — Google login axını regressiyaya qarşı.
 *
 * 28444-nin əsl kökü (D-155/D-156-dan fərqli olaraq): repo secret-i
 * GOOGLE_WEB_CLIENT_ID Web klient yaradılmazdan ƏVVƏL qoyulduğundan
 * ANDROID tipli ID daşıyırdı; m4 /config fetch-i alınanda app SƏSSİZCƏ
 * ona düşürdü və Credential Manager 28444 qaytarırdı. Toy-toyum gate
 * (googleAvailable fetch-dən əvvəl yoxlanılırdı) isə BuildConfig boş
 * olanda fetch-i heç çağırmırdı.
 *
 *   1) fetchGoogleConfig AuthScreen-də LaunchedEffect ilə PROAKTİV çağırılır
 *      (googleAvailable gate-indən asılı deyil — toy-toyum qadağandır)
 *   2) googleSignIn fetch-dən ƏVVƏL googleAvailable qapısından keçmir
 *   3) Google düyməsinin onClick-i gate yoxlamadan googleSignIn çağırır
 *   4) fetchGoogleConfig səssiz catch ETMİR — Safety.onCaught-a düşür
 *   5) fetchGoogleConfig Boolean qaytarır (çağıran nəticəni bilir)
 */
class GoogleAuthAuditTest {

    private fun repoDir(): File {
        var d = File(System.getProperty("user.dir"))
        while (d != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        return checkNotNull(d) { "repo kökü tapılmadı" }
    }

    private fun src(rel: String): String = File(repoDir(), rel).readText()

    private val authScreen = "app/src/main/java/az/qoshul/app/features/auth/AuthScreen.kt"
    private val backend = "app/src/main/java/az/qoshul/app/core/backend/SupabaseBackend.kt"

    @Test
    fun `fetch_google_config_launched_effect_ile_proaktivdir`() {
        val f = src(authScreen)
        assertTrue(
            "LaunchedEffect + fetchGoogleConfig proaktiv istiləşdirmə itib (D-165) — " +
                "gate-dən asılı fetch toy-toyum yaradır",
            Regex("LaunchedEffect\\([^)]*\\)\\s*\\{[\\s\\S]*?fetchGoogleConfig")
                .containsMatchIn(f),
        )
    }

    @Test
    fun `google_signin_fetchden_once_qapidan_kecmir`() {
        val f = src(authScreen)
        assertFalse(
            "googleSignIn-də googleAvailable early-return toy-toyum yaradır (D-165)",
            f.contains("if (!SupabaseBackend.googleAvailable) return"),
        )
        val funStart = f.indexOf("fun googleSignIn")
        assertTrue("googleSignIn funksiyası tapılmadı", funStart >= 0)
        val fetchAt = f.indexOf("SupabaseBackend.fetchGoogleConfig()", funStart)
        val useAt = f.indexOf("BackendConfig.activeGoogleClientId", funStart)
        assertTrue(
            "googleSignIn: fetchGoogleConfig activeGoogleClientId-dən ƏVVƏL çağırılmalıdır",
            fetchAt in 0 until useAt,
        )
    }

    @Test
    fun `google_dugmesi_gate_else_budaqi_yoxdur`() {
        val f = src(authScreen)
        assertFalse(
            "Google düyməsi googleAvailable else-budağı fetch-i bloklayır (D-165) — " +
                "fetch düymə klikində daxildə olmalıdır",
            f.contains("if (SupabaseBackend.googleAvailable)"),
        )
    }

    @Test
    fun `fetch_google_config_sissiz_catch_yoxdur_boolean_qaytarir`() {
        val f = src(backend)
        val start = f.indexOf("suspend fun fetchGoogleConfig")
        assertTrue("fetchGoogleConfig tapılmadı", start >= 0)
        val end = f.indexOf("\n    }", start)
        assertTrue("fetchGoogleConfig bodisi tapılmadı", end > start)
        val body = f.substring(start, end)
        assertFalse(
            "fetchGoogleConfig səssiz catch (_ — D-165-də 28444-ni gizlədirdi; " +
                "xətalar Safety.onCaught-a düşməlidir",
            body.contains("catch (_:"),
        )
        assertTrue(
            "fetchGoogleConfig xətaları Safety.onCaught-a ötürməlidir (D-165)",
            body.contains("Safety.onCaught"),
        )
        assertTrue(
            "fetchGoogleConfig Boolean qaytarmalıdır (fetch nəticəsi bilinməlidir)",
            Regex("suspend fun fetchGoogleConfig\\(\\): Boolean").containsMatchIn(f),
        )
    }
}
