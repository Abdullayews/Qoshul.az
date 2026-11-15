package az.qoshul.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * D-126..D-134 (v0.44.0) + D-143 (v0.48.0) qoruyucu source-audit testi —
 * istifadəçi direktivlərinin CI-də daimi təsdiqi (TokenAuditTest dili):
 *   D-126: bütün şəhərlər mənbədə (79) + axtarışda şəhər nəticələri;
 *   D-127: SaveableStateProvider GlassNavHost-da;
 *   D-128: window background tema rənginə boyanır + kök fon keçidi;
 *   D-129: Manifest-də runtime icazələri;
 *   D-130: AI FAB liquid glass üzlüyü + index != 1 && index != 2;
 *   D-131: biznes CTA mətnləri PartnersScreen-də YOX;
 *   D-132: „minimum 2“ etiketi + chats izahat barı mətni YOX;
 *   D-133: türkizm/ASCII-dırnaq qalıntıları UI stringlərində YOX;
 *   D-134: push/pop keçidi 2-fazalı (delay 140ms);
 *   D-143: xəritədən +/− həbləri silindi, düzləşdirmə böyütmə yerinə,
 *     kiçiltmə yerinə real lokasiya (telefon datası, mock RƏDD).
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

    // ═══════════ D-143: xəritə həbləri (v0.48) ═══════════
    // Direktiv 2026-11-14: „böyüdüb kiçiltmə düymələrini sil, düzləşdirmə
    // düyməsini böyütmə düyməsinin yerinə qoy, kiçiltmə yerinə telefondan
    // alınan dataya görə istifadəçinin lokasiyasına getmək — mock YOX“.

    @Test
    fun `D-143 zoom plus minus hebleri silinir`() {
        val chrome = read("$main/features/map/MapChrome.kt")
        assertFalse(
            "MapChrome-da böyütmə/kiçiltmə həbi qalmamalıdır",
            chrome.contains("Icons.Filled.Add") || chrome.contains("Icons.Filled.Remove"),
        )
        assertFalse(
            "MapZoomControls ləğv olunub — MapControls var",
            chrome.contains("fun BoxScope.MapZoomControls"),
        )
    }

    @Test
    fun `D-143 duzlesdirme birinci heybede lokasiya ikincidedir`() {
        // sütun sırası: Explore (düzləşdirmə) ÜST, MyLocation ALT
        val chrome = read("$main/features/map/MapChrome.kt")
        val iExplore = chrome.indexOf("Icons.Filled.Explore")
        val iMyLoc = chrome.indexOf("Icons.Filled.MyLocation")
        assertTrue("hər iki həb mövcuddur", iExplore >= 0 && iMyLoc >= 0)
        assertTrue(
            "düzləşdirmə (böyütmə yeri) lokasiyadan (kiçiltmə yeri) YUXARIDA",
            iExplore < iMyLoc,
        )
    }

    @Test
    fun `D-143 canvaslar mapcontrols istifade edir ve onzoom yoxdur`() {
        for (f in listOf("MaplibreCanvas.kt", "GoogleMapCanvas.kt")) {
            val src = read("$main/features/map/$f")
            assertTrue("$f MapControls çağırmalıdır", src.contains("MapControls("))
            assertFalse("$f onZoom parametri qalmamalıdır", src.contains("onZoom"))
        }
    }

    @Test
    fun `D-143 istifadeci lokasiyasi mock data ola bilmez`() {
        val loc = read("$main/core/location/QoshulLocation.kt")
        assertTrue(
            "mock provayder fix-ləri rədd edilməlidir (isFromMockProvider)",
            loc.contains("isFromMockProvider"),
        )
        // MapScreen: „mənim yerim“ real fix-dən gəlir — hardcoded dəyər
        // YALNIZ fallback (?: operatoru) kimi qala bilər
        val scr = read("$main/features/map/MapScreen.kt")
        assertTrue(
            "MapScreen QoshulLocation istifadə etməlidir (real fix)",
            scr.contains("QoshulLocation."),
        )
        assertFalse(
            "hardcoded koordinat birbaşa təyinat ola bilməz — fallback olmalıdır",
            Regex("val myLat = \\s*40\\.4093\\s*$", RegexOption.MULTILINE).containsMatchIn(scr),
        )
    }

    // ═══════════ D-144: köhnə loqo ləğv + böyük hərf (v0.49) ═══════════
    // Direktiv 2026-11-14: „hər yerdə köhnə loqonu sil. Qoşul adını
    // böyük hərflə başlat“.

    @Test
    fun `D-144 kohnelogo qaliqilari yoxdur`() {
        // favicon portu (yaşıl kvadrat + halqalar) hər qatda ləğv:
        assertFalse("ic_logo_mark.xml silinməlidir", File(repoDir(), "app/src/main/res/drawable/ic_logo_mark.xml").exists())
        assertFalse("ic_launcher_monochrome.xml (köhnə halqalar) silinməlidir", File(repoDir(), "app/src/main/res/drawable/ic_launcher_monochrome.xml").exists())
        for (f in listOf("ic_launcher.xml", "ic_launcher_round.xml")) {
            val xml = read("app/src/main/res/mipmap-anydpi-v26/$f")
            assertFalse("$f tək-rəngli köhnə qatına istinad etməməlidir", xml.contains("monochrome"))
        }
        // README əsl Git kökündədir (repoDir = android-app/) — mövcuddursa yoxla
        val gitRoot = repoDir().parentFile
        val readme = File(gitRoot, "README.md")
        if (readme.exists()) {
            assertFalse(
                "README-də köhnə favicon şəkli qalmamalıdır",
                readme.readText().contains("favicon.svg"),
            )
        }
    }

    @Test
    fun `D-144 brend marki yeni ikondan gelir`() {
        val ui = read("$main/core/ui/Ui.kt")
        assertTrue(
            "LogoMark yeni brand_mark resursunu çəkməlidir",
            ui.contains("R.drawable.brand_mark"),
        )
        assertTrue(
            "brand_mark.webp mövcuddur (drawable-nodpi)",
            File(repoDir(), "app/src/main/res/drawable-nodpi/brand_mark.webp").exists(),
        )
    }

    @Test
    fun `D-144 qoshul adi boyuk herfle baslayir`() {
        // Görünən brend mətnlərində "qoshul" kiçik başlanğıc YOX
        // (URL/domen kod identifikatorları istisnadır — yalnız UI mətnləri)
        val ui = read("$main/core/ui/Ui.kt")
        assertFalse(
            "Wordmark 'qoshul' kiçik hərflə başlamamalıdır",
            ui.contains("\"qoshul\""),
        )
        assertTrue(
            "Wordmark 'Qoshul' olmalıdır",
            ui.contains("\"Qoshul\""),
        )
    }

    // ═══════════ D-145: audit v0.49 düzəlişlərinin qorunması ═══════════

    @Test
    fun `D-145 location listener teberligi awaitclose ile qorunur`() {
        // H1: callbackFlow-da return@callbackFlow awaitClose-i atmamalıdır
        val loc = read("$main/core/location/QoshulLocation.kt")
        assertFalse(
            "awaitClose atlanmamalıdır — return@callbackFlow yolverilməz",
            loc.contains("return@callbackFlow"),
        )
        assertTrue(
            "provayderlər fərdi qeydiyyata alınmalıdır (mapNotNull)",
            loc.contains("mapNotNull"),
        )
    }

    @Test
    fun `D-145 xarite kateqoriya filtri events paritetidir`() {
        // H2: tədbir pin-ləri QRUP üzrə filtrlənir (groupOf)
        val scr = read("$main/features/map/MapScreen.kt")
        assertTrue(
            "eventPins groupOf ilə filtrlənməlidir (EventsScreen pariteti)",
            scr.contains("groupOf(it.category) == category"),
        )
    }

    @Test
    fun `D-145 google me markeri sinxronlasdirilir`() {
        // H3: rememberMarkerState ilk pozisiyadan sonra yenilənmir —
        // LaunchedEffect(manual sync) olmalıdır
        val g = read("$main/features/map/GoogleMapCanvas.kt")
        assertTrue(
            "meState.position manual sinxronlaşdırılmalıdır",
            g.contains("meState.position ="),
        )
    }

    @Test
    fun `D-145 seher merkezi fallback ve permission recompose`() {
        // M1: fallback sabit Bakı deyil — findCity(st.city)
        val scr = read("$main/features/map/MapScreen.kt")
        assertTrue(
            "fallback şəhər mərkəzindən gəlməlidir (findCity)",
            scr.contains("findCity(st.city)"),
        )
        // M2: icazə dialoqundan qayıdış recompose tetikləməlidir
        assertTrue(
            "launcher callback-i locateRequest-i artırmalıdır",
            scr.contains("if (granted) locateRequest++"),
        )
    }

    @Test
    fun `D-145 sohbet mesaj acari stabildir`() {
        // M4: LazyColumn key-i body hash-inə deyil, mesaj id-sinə bağlanır
        val a = read("$main/features/assistant/AssistantScreen.kt")
        val c = read("$main/features/chats/ChatsScreen.kt")
        assertTrue("AssistantScreen key = m.id olmalıdır", a.contains("key = { _, m -> m.id }"))
        assertTrue("ChatsScreen key = m.id olmalıdır", c.contains("key = { _, m -> m.id }"))
        assertFalse(
            "köhnə body-hash kompozit açar qalmamalıdır",
            a.contains("body.hashCode()") || c.contains("body.hashCode()"),
        )
        // typing simulyasiyası copy() ilə id qoruyur
        val e = read("$main/core/engine/AppEngine.kt")
        assertTrue("typing copy(body=…) ilə yaymalıdır", e.contains(".copy(body = shown)"))
    }

    @Test
    fun `D-145 locale bagli formatlar us drdir`() {
        // L4: onluq ayırıcı locale-dən asılı olmamalıdır
        for (f in listOf(
            "$main/features/map/MapScreen.kt",
            "$main/features/venues/VenueDetailScreen.kt",
            "$main/features/partners/PartnersScreen.kt",
            "$main/core/ui/Ui.kt",
            "$main/core/engine/AppEngine.kt",
        )) {
            assertFalse(
                "$f — Locale.US-suz String.format qalmamalıdır",
                read(f).contains("String.format(\""),
            )
        }
    }
}
