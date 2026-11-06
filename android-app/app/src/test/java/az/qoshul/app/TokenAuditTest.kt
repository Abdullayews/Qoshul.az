package az.qoshul.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * U3 (UI-06/07/24/25/26/48) qoruyucu source-audit testi — audit 2026-11-07.
 * VI meyarlarının CI-də daimi qorunması:
 *   1) rg 'TextStyle(fontFamily' → 0 (yalnız Type.kt-də)
 *   2) hardcoded Color.* → 0 features/-da (Glass.kt = dizayn-sistem qatı, icazəli)
 *   3) 10.5sp-altı mətn / .copy(fontSize) → 0 (Type.kt xaricində)
 *   4) Q.glass birbaşə istifadəsi features-da → 0 (şüşə YALNIZ core/glass
 *      komponentləri ilə, 5 icazəli səthdə — design-spec §2, D-096)
 *   5) glass komponent sayı features-da = 6 çağırış (5 səth tipi)
 */
class TokenAuditTest {

    private fun repoDir(): File {
        var d = File(System.getProperty("user.dir"))
        while (d != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        return checkNotNull(d) { "repo kökü tapılmadı" }
    }

    private val main: File get() = File(repoDir(), "app/src/main/java")
    private val features: File get() = File(main, "az/qoshul/app/features")

    private fun ktLines(root: File): List<Pair<String, String>> =
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { f -> f.readLines().asSequence().map { f.name to it } }
            .toList()

    @Test
    fun `inline TextStyle yalniz Type kt de ola biler`() {
        val hits = ktLines(main)
            .filter { (name, ln) -> name != "Type.kt" && ln.contains("TextStyle(") }
        assertTrue("Type.kt xaricində TextStyle(: ${hits.take(3)}", hits.isEmpty())
    }

    @Test
    fun `hardcoded color features da yoxdur`() {
        val named = Regex("Color\\.(White|Black|Red|Green|Blue|Yellow|Gray|DarkGray|LightGray|Magenta|Cyan)\\b")
        val hits = ktLines(features).filter { (_, ln) -> named.containsMatchIn(ln) && !ln.trim().startsWith("//") }
        assertTrue("features-da hardcoded Color.*: ${hits.take(3)}", hits.isEmpty())
    }

    @Test
    fun `copy fontSize ve 10_5 alti olcu yoxdur type kt xericinde`() {
        val hits = ktLines(main)
            .filter { (name, ln) -> name != "Type.kt" }
            .filter { (_, ln) -> ln.contains(".copy(fontSize") }
        assertTrue(".copy(fontSize) qalıntıları: ${hits.take(3)}", hits.isEmpty())
        // 10.5sp-altı: Type.kt-də yalnız ≥10.5 olmalıdır
        val small = Regex("fontSize\\s*=\\s*([0-9.]+)\\.sp")
        val bad = ktLines(File(main, "az/qoshul/app/core/theme"))
            .filter { (name, _) -> name == "Type.kt" }
            .flatMap { (_, ln) -> small.findAll(ln).map { it.groupValues[1] } }
            .filter { it.toDouble() < 10.5 }
        assertTrue("Type.kt-də 10.5-altı ölçü: $bad", bad.isEmpty())
    }

    @Test
    fun `q glass birbasa istifadesi features da yoxdur`() {
        val hits = ktLines(features)
            .filter { (_, ln) -> Regex("Q\\.glass\\b").containsMatchIn(ln) && !ln.trim().startsWith("//") }
        assertTrue("features-da Q.glass (şüşə səth yalnız core/glass komponenti ilə): ${hits.take(3)}", hits.isEmpty())
    }

    @Test
    fun `glass komponent sayi 5 icazali sethde saxlanilir`() {
        val uses = ktLines(features)
            .filter { (_, ln) -> Regex("(GlassPill|GlassSurface|LiquidGlassSurface)\\(").containsMatchIn(ln) }
            .map { it.first to it.second.trim().take(50) }
        // gözlənti: AppShell nav pill (1) + MapScreen filtr paneli (1) + MapScreen
        // alt kart (1) + MapScreen sayğac (1) + MatchScreen Keç/Maraqlıyam (2)
        assertEquals(
            "glass səthləri: ${uses.joinToString("; ")}",
            6,
            uses.size,
        )
    }

    @Test
    fun `yonlisli ikonlar automirrored olmalidir`() {
        // U4-4 (D-097): yönlü ikonların AutoMirrored olmayan variantları
        // (run #42 compiler warning sinfi) — gələcək regresiya qoruması.
        val directional = Regex(
            "Icons\\.(Filled|Outlined|Rounded)\\." +
                "(Logout|MenuBook|ReceiptLong|Send|ArrowBack|Login|" +
                "KeyboardArrowLeft|KeyboardArrowRight|Sort)\\b"
        )
        val hits = ktLines(main)
            .filter { (_, ln) -> directional.containsMatchIn(ln) && !ln.trim().startsWith("//") }
        assertTrue("yönlü ikonlar AutoMirrored variantla işlədilməlidir: ${hits.take(3)}", hits.isEmpty())
    }
}
