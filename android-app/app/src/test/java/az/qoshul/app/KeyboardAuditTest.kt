package az.qoshul.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * D-136..D-139 (v0.45.0) qoruyucu source-audit testi — klaviatura UX və
 * toxunma hədəfi direktivlərinin CI-də daimi təsdiqi (ListKeyTest dili):
 *   D-136: rəqəm/ondalıq sahələr düzgün KeyboardType alır;
 *   D-137: bütün single-line mətn sahələri ImeAction daşıyır;
 *   D-138: EventCard save hit sahəsi 48dp;
 *   D-139: save ikonu vəziyyət etiketi + OTP rəqəm filtri.
 */
class KeyboardAuditTest {

    private fun repoDir(): File {
        var d = File(System.getProperty("user.dir"))
        while (d != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        return checkNotNull(d) { "repo kökü tapılmadı" }
    }

    private fun read(rel: String): String =
        File(repoDir(), rel).readText()

    private val main: String get() = "app/src/main/java/az/qoshul/app"

    // ═══════════ D-136: klaviatura növləri ═══════════

    @Test
    fun `D136 settings otp reqem klaviaturasi alir`() {
        val src = read("$main/features/settings/SettingsScreen.kt")
        assertTrue(
            "Settings OTP sahəsi KeyboardType.Number almalıdır",
            src.contains("keyboardType = KeyboardType.Number"),
        )
    }

    @Test
    fun `D136 create sheet capacity ve price reqem klaviaturasi alir`() {
        val src = read("$main/features/events/CreateFinishSheets.kt")
        assertTrue(
            "CreateFinishSheets-də ən azı 2 KeyboardType.Number olmalıdır (capacity + price)",
            Regex("keyboardType = KeyboardType.Number").findAll(src).count() >= 2,
        )
    }

    @Test
    fun `D136 partners bill onaliq klaviaturasi alir`() {
        val src = read("$main/features/partners/PartnersScreen.kt")
        assertTrue(
            "PartnersScreen bill sahəsi KeyboardType.Decimal almalıdır",
            src.contains("keyboardType = KeyboardType.Decimal"),
        )
    }

    // ═══════════ D-137: imeAction örtüyü ═══════════

    @Test
    fun `D137 auth zenciri imeAction dasiyir`() {
        val src = read("$main/features/auth/AuthScreen.kt")
        assertTrue(
            "AuthScreen-də ImeAction.Next olmalıdır (ad→e-poçt→şifrə zənciri)",
            src.contains("ImeAction.Next"),
        )
        assertTrue(
            "AuthScreen-də ImeAction.Done + KeyboardActions(onDone = submit) olmalıdır",
            src.contains("ImeAction.Done") && src.contains("KeyboardActions(onDone ="),
        )
    }

    @Test
    fun `D137 chats composer enter ile gonderir`() {
        val src = read("$main/features/chats/ChatsScreen.kt")
        assertTrue(
            "ChatsScreen ComposerField ImeAction.Send + onSend almalıdır",
            src.contains("ImeAction.Send") && src.contains("KeyboardActions(onSend ="),
        )
    }

    @Test
    fun `D137 assistant enter ile gonderir`() {
        val src = read("$main/features/assistant/AssistantScreen.kt")
        assertTrue(
            "AssistantScreen input ImeAction.Send almalıdır",
            src.contains("ImeAction.Send"),
        )
    }

    @Test
    fun `D137 axtaris klaviaturasi search ile baglanir`() {
        val src = read("$main/core/ui/Ui.kt")
        assertTrue(
            "SearchBarField ImeAction.Search + clearFocus almalıdır",
            src.contains("ImeAction.Search") && src.contains("clearFocus()"),
        )
    }

    // ═══════════ D-138: toxunma hədəfi ═══════════

    @Test
    fun `D138 eventcard save hit sahesi 48dp dir`() {
        val src = read("$main/features/events/EventsScreen.kt")
        val saveBlock = Regex("if \\(!event\\.past\\) \\{[\\s\\S]{0,900}?androidxMinTouch")
            .find(src)?.value ?: ""
        assertTrue(
            "EventCard save hit sahəsi tapılmadı",
            saveBlock.isNotEmpty(),
        )
        assertTrue(
            "save hit sahəsi 48dp olmalıdır (38dp vardı), blok: ${saveBlock.take(120)}",
            Regex("\\.size\\(48\\.dp\\)").containsMatchIn(saveBlock),
        )
    }

    // ═══════════ D-139: dinamik etiket + OTP filtri ═══════════

    @Test
    fun `D139 save ikonu vezifevi etiket dasiyir`() {
        val src = read("$main/features/events/EventsScreen.kt")
        assertTrue(
            "save ikonu saved vəziyyətinə görə etiket dəyişməlidir",
            src.contains("\"Saxlanmışlardan çıxart\""),
        )
    }

    @Test
    fun `D139 settings otp yalniz reqem qebul edir`() {
        val src = read("$main/features/settings/SettingsScreen.kt")
        // onValueChange filtri rəqəm yoxlayır (Auth OTP ilə eyni pattern)
        assertTrue(
            "Settings OTP onValueChange rəqəm filtri daşımalıdır",
            src.contains("it.all { c -> c.isDigit() }"),
        )
    }
}
