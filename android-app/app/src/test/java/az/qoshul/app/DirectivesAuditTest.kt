package az.qoshul.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * D-126..D-134 (v0.44.0) qoruyucu source-audit testi — istifadəçi
 * direktivlərinin CI-də daimi təsdiqi (TokenAuditTest dili):
 *   D-126: bütün şəhərlər mənbədə (79) + axtarışda şəhər nəticələri;
 *   D-127: SaveableStateProvider GlassNavHost-da;
 *   D-128: window background tema rənginə boyanır + kök fon keçidi;
 *   D-129: Manifest-də runtime icazələri;
 *   D-130: AI FAB liquid glass üzlüyü + index != 1 && index != 2;
 *   D-131: biznes CTA mətnləri PartnersScreen-də YOX;
 *   D-132: „minimum 2“ etiketi + chats izahat barı mətni YOX;
 *   D-133: türkizm/ASCII-dırnaq qalıntıları UI stringlərində YOX;
 *   D-134: push/pop keçidi 2-fazalı (delay 140ms).
 */
class DirectivesAuditTest {

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

    // ═══════════ D-126: şəhərlər ═══════════

    @Test
    fun `D126 fake data 79 seher daşıyır`() {
        val src = read("$main/core/data/FakeData.kt")
        val count = Regex("City\\(\"").findAll(src).count()
        assertEquals("FakeData-da 79 şəhər olmalıdır", 79, count)
    }

    @Test
    fun `D126 ana menyu axtarışı şəhər nəticələri göstərir`() {
        val feed = read("$main/features/feed/FeedScreen.kt")
        assertTrue("FeedScreen-də cityHits olmalıdır", feed.contains("cityHits"))
        assertTrue(
            "axtarış hint-i şəhəri də əhatə etməlidir",
            feed.contains("Məkan, insan və ya şəhər axtar"),
        )
    }

    // ═══════════ D-127: tab qorunması ═══════════

    @Test
    fun `D127 glassnavhost saveablestateprovider isledir`() {
        val nav = read("$main/core/ui/Nav.kt")
        assertTrue(
            "GlassNavHost SaveableStateProvider içində çəkməlidir",
            nav.contains("SaveableStateProvider"),
        )
    }

    // ═══════════ D-128: ağ flaş həlli ═══════════

    @Test
    fun `D128 window fonu tema rengine boyanır`() {
        val theme = read("$main/core/theme/Theme.kt")
        assertTrue(
            "Theme.kt window background-ı Q.background rənginə boyamalıdır",
            theme.contains("setBackgroundDrawable"),
        )
        val mainAct = read("$main/MainActivity.kt")
        assertTrue(
            "MainActivity kök fonu animateColorAsState ilə keçirməlidir",
            mainAct.contains("animateColorAsState"),
        )
    }

    // ═══════════ D-129: runtime icazələr ═══════════

    @Test
    fun `D129 manifestde icazeler elan edilib`() {
        val mf = read("app/src/main/AndroidManifest.xml")
        listOf(
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.POST_NOTIFICATIONS",
        ).forEach { p ->
            assertTrue("Manifest-də $p olmalıdır", mf.contains(p))
        }
    }

    @Test
    fun `D129 onboarding real launcher işlədir`() {
        val ob = read("$main/features/onboarding/OnboardingScreen.kt")
        assertTrue(
            "Onboarding real permission launcher işlətməlidir",
            ob.contains("rememberLauncherForActivityResult"),
        )
        assertTrue(
            "Şəkil/saxlama kartı əlavə olunmalıdır",
            ob.contains("Şəkil və saxlama"),
        )
    }

    // ═══════════ D-130: AI FAB ═══════════

    @Test
    fun `D130 ai fab liquid glass ve tab qaydası`() {
        val shell = read("$main/features/shell/AppShell.kt")
        assertTrue(
            "AiFab liquidGlassNavBar recipesi istifadə etməlidir",
            shell.contains("liquidGlassNavBar(CircleShape)"),
        )
        assertTrue(
            "Görünüş qaydası: yalnız xəritə (1) və söhbətlər (2) gizli",
            shell.contains("visible = index != 1 && index != 2"),
        )
    }

    // ═══════════ D-131: biznes yalnız ayarlarda ═══════════

    @Test
    fun `D131 partnersde biznes cta metnleri yoxdur`() {
        val p = read("$main/features/partners/PartnersScreen.kt")
        assertTrue(
            "„Öz məkanını əlavə et“ PartnersScreen-də olmamalıdır",
            !p.contains("Öz məkanını əlavə et"),
        )
        assertTrue(
            "„Biznes rejimi — XP endirimi ver“ PartnersScreen-də olmamalıdır",
            !p.contains("Biznes rejimi — XP endirimi ver"),
        )
    }

    @Test
    fun `D131 ayarlarda şirkət dialoqu biznesi açır`() {
        val s = read("$main/features/settings/SettingsScreen.kt")
        assertTrue(
            "Settings-də Biznes rejimi açılışı olmalıdır",
            s.contains("Biznes rejimini aç"),
        )
    }

    // ═══════════ D-132: minimum 2 + chats + boş chat ═══════════

    @Test
    fun `D132 iştirakçı etiketi minimum 2 dir`() {
        val c = read("$main/features/events/CreateFinishSheets.kt")
        assertTrue(
            "Etiket „İştirakçı sayı (minimum 2)“ olmalıdır",
            c.contains("İştirakçı sayı (minimum 2)"),
        )
        assertTrue(
            "köhnə „(2–500)“ etiketi qalmamalıdır",
            !c.contains("(2–500)"),
        )
    }

    @Test
    fun `D132 chats izahat barı ləğv edilib`() {
        val ch = read("$main/features/chats/ChatsScreen.kt")
        assertTrue(
            "„Yeni söhbətlər burada açılır“ barı ChatsScreen-də olmamalıdır",
            !ch.contains("Yeni söhbətlər burada açılır"),
        )
    }

    @Test
    fun `D132 boş ai chatı mərkəzləşdirilib`() {
        val a = read("$main/features/assistant/AssistantScreen.kt")
        assertTrue(
            "Boş chatda „İlk mesajı sən yaz“ başlığı olmalıdır",
            a.contains("İlk mesajı sən yaz"),
        )
        assertTrue(
            "Sütun mərkəzə düzülməlidir",
            a.contains("Alignment.CenterHorizontally"),
        )
    }

    // ═══════════ D-133: orfoqrafiya ═══════════

    @Test
    fun `D133 turkizm qalıntıları ui stringlerinde yoxdur`() {
        val banned = listOf(
            "tercih", "Aktiviteyi", "aktivite", "Müştəçi",
            "hərkəsə", "seçenek", "değil", "sadece",
        )
        val root = File(repoDir(), "$main")
        val hits = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { f ->
                // şərhləri kənarlaşdır (yalnız string literallar)
                val noComment = f.readText()
                    .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
                    .replace(Regex("//[^\\n]*"), "")
                banned.map { b -> f.name to b to noComment.contains(b) }
            }.filter { it.second }.toList()
        assertTrue(
            "UI stringlərində türkizm/imla qalıntıları: ${hits.map { it.first }}",
            hits.isEmpty(),
        )
    }

    // ═══════════ D-134: 2-fazalı push/pop ═══════════

    @Test
    fun `D134 push pop keçidi 2 fazalıdır`() {
        val nav = read("$main/core/ui/Nav.kt")
        assertTrue(
            "Girişin 140ms gecikməsi (çıxış fazasından sonra) olmalıdır",
            nav.contains("delayMillis = 140"),
        )
    }

    // ═══════════ Xəritə attribution (D-117 qorunması — v0.44 yoxlaması) ═══════════

    @Test
    fun `xerite overlay lgv qorunur — attribution yoxdur`() {
        val m = read("$main/features/map/MaplibreCanvas.kt")
        assertTrue(
            "MapLibre default overlay (loqo/atribusiya) söndürülmüş qalmalıdır",
            m.contains("overlay = {}"),
        )
    }

    // ═══════════ Əsas rənglər sabit (istifadəçi direktivi: dəyişməz) ═══════════

    @Test
    fun `esas brend rengleri 008B2D ve EAC66C sabitdir`() {
        val c = read("$main/core/theme/Color.kt")
        assertTrue(
            "Yaşıl brend #008B2D sabit qalmalıdır",
            c.contains("0xFF008B2D"),
        )
        assertTrue(
            "Qızılı brend #EAC66C sabit qalmalıdır",
            c.contains("0xFFEAC66C"),
        )
    }
}
