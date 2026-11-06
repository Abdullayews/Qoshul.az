package az.qoshul.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * U2.3 (UI-12) qoruyucu testi — audit 2026-11-07:
 * "Saxta collectAsStateWithLifecycle (özəlləşdirilmiş ad, plain collectAsState)".
 *
 * QAYDALAR:
 * 1) src/main-də heç bir `private fun ... collectAsStateWithLifecycle` yoxdur
 *    (saxta extension ləğv olundu);
 * 2) heç bir `*.collectAsState()` (plain) yoxdur — lifecycle-aware olmalıdır;
 * 3) libs.versions.toml + build.gradle.kts lifecycle-runtime-compose asılılığı
 *    elan olunub (həqiqi kitabxana).
 */
class CollectLifecycleTest {

    private fun repoDir(): File {
        var d = File(System.getProperty("user.dir"))
        while (d != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        return checkNotNull(d) { "repo kökü tapılmadı" }
    }

    @Test
    fun `saxta collectAsStateWithLifecycle extension i yoxdur`() {
        val src = File(repoDir(), "app/src/main/java")
        val hits = src.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { f ->
                f.readLines().mapIndexed { i, ln -> Triple(f.name, i + 1, ln) }
            }
            .filter { it.third.contains("fun") && it.third.contains("collectAsStateWithLifecycle") }
            .map { "${it.first}:${it.second}" }
            .toList()
        assertTrue("Saxta extension hələ DURUR: $hits", hits.isEmpty())
    }

    @Test
    fun `plain collectAsState cagrislari yoxdur main sourcede`() {
        val src = File(repoDir(), "app/src/main/java")
        val hits = src.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { f ->
                f.readLines().mapIndexed { i, ln -> Triple(f.name, i + 1, ln) }
            }
            .filter { it.third.contains(".collectAsState()") }
            .map { "${it.first}:${it.second}: ${it.third.trim().take(60)}" }
            .toList()
        assertTrue("Plain collectAsState() qalıntıları: $hits", hits.isEmpty())
    }

    @Test
    fun `lifecycle runtime compose asililigi elan olunub`() {
        val toml = File(repoDir(), "gradle/libs.versions.toml").readText()
        assertTrue("toml-da lifecycle-runtime-compose yoxdur", "lifecycle-runtime-compose" in toml)
        val gradle = File(repoDir(), "app/build.gradle.kts").readText()
        assertTrue(
            "build.gradle.kts-də libs.androidx.lifecycle.runtime.compose yoxdur",
            "libs.androidx.lifecycle.runtime.compose" in gradle,
        )
    }
}
