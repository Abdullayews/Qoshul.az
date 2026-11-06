package az.qoshul.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * U2.1 (UI-01) qoruyucu testi — audit 2026-11-07:
 * "Lazy siyahılarda key= yoxdur (0/23)".
 *
 * QAYDA: features/ daxilində hər items()/itemsIndexed() çağırışı
 * stabil `key =` ilə gəlməlidir (NIA: NewsFeed.kt:57-61 patterni).
 * Bu, source-audit testidir — CI-də pozulma dərhal qırmızı verir.
 */
class ListKeyTest {

    private fun moduleDir(): File {
        var d = File(System.getProperty("user.dir"))
        while (d != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        checkNotNull(d) { "module kökü tapılmadı (settings.gradle.kts)" }
        return File(d, "app/src/main/java/az/qoshul/app/features")
    }

    @Test
    fun `butun items cagrislari key e malikdir`() {
        val root = moduleDir()
        val violations = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { f ->
                val all = f.readLines()
                all.forEachIndexed { i, raw ->
                    val line = raw.trim()
                    val isItemsCall = Regex("^(items|itemsIndexed)\\(").containsMatchIn(line) &&
                        !line.startsWith("//") && !line.startsWith("*")
                    if (isItemsCall) {
                        val window = all.subList(i, minOf(i + 3, all.size)).joinToString(" ")
                        if (!Regex("key\\s*=").containsMatchIn(window)) {
                            violations.add("${f.name}:${i + 1}: ${line.take(70)}")
                        }
                    }
                }
            }
        assertTrue(
            "key= olmayan items çağırışları (${violations.size}):\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}
