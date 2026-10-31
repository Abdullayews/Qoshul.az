package az.qoshul.app

import az.qoshul.app.core.data.dateLabelFor
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.AssistantParse
import az.qoshul.app.core.model.parseAssistantEvent
import az.qoshul.app.core.safety.Safety
import az.qoshul.app.core.ui.MapLogGate
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * D-061 "nə olar əgər…" testləri — istifadəçi direktivi 2026-11-01:
 * "hər function üçün bu olsa nə olar bu olsa nə olar halını placeholder
 * kimi doldur". Hər qoruma qatının fallback-i burada SÜBUT olunur:
 *
 *  - Safety.of            → exception-da təyin olunmuş fallback
 *  - parseAssistantEvent  → xarab formatda çökmür, mətn saxlanılır
 *  - dateLabelFor         → aralıq xarici epochDay → "—"
 *  - MapLogGate           → eyni imza 15s-də 1 dəfə, pəncərədə maksimum 3
 *  - AppEngine naməlum ID → null/false (çökmə YOX)
 */
class WhatIfSafetyTest {

    // ── Safety ──

    @Test fun `SafetyOf exception-da fallback qaytarır`() {
        val r = Safety.of("t", fallback = 42) { error("partladı") }
        assertEquals(42, r)
    }

    @Test fun `SafetyOf normalda blok dəyərini verir`() {
        val r = Safety.of("t", fallback = 42) { 7 }
        assertEquals(7, r)
    }

    @Test(expected = CancellationException::class)
    fun `SafetyOf CancellationException-u ötürür`() {
        Safety.of("t", fallback = 42) { throw CancellationException("dayandır") }
    }

    @Test fun `SafetyRun exception-da udur və sink işə düşür`() {
        var caughtTag: String? = null
        Safety.onCaught = { tag, _ -> caughtTag = tag }
        Safety.run("sink-test") { error("boom") }
        assertEquals("sink-test", caughtTag)
        Safety.onCaught = null
    }

    // ── parseAssistantEvent — xarab girişlər ──

    @Test fun `parser adi mətni olduğu kimi qaytarır`() {
        val p = parseAssistantEvent("Salam! Sabah nə edirik?")
        assertEquals("Salam! Sabah nə edirik?", p.text)
        assertNull(p.proposal)
    }

    @Test fun `parser bağlanmamış JSON-da təklif null olur`() {
        val p = parseAssistantEvent("Plan hazır!\n[TƏDBİR]{\"title\":\"Yürüş\"")
        assertNull(p.proposal)
        assertTrue(p.text.isNotBlank())
    }

    @Test fun `parser marker-dan sonra JSON olmayanda təklif null`() {
        val p = parseAssistantEvent("Cavab:\n[TƏDBİR] bu sadəcə mətndir")
        assertNull(p.proposal)
    }

    @Test fun `parser düzgün JSON-u oxuyur`() {
        val raw = "Plan!\n[TƏDBİR]{\"title\":\"Qala yürüşü\",\"category\":\"Yürüş\"," +
            "\"date\":\"2026-11-07\",\"time\":\"09:00\",\"capacity\":10," +
            "\"location\":\"Qala\",\"description\":\"5 km\",\"partner\":\"\"}"
        val p = parseAssistantEvent(raw)
        assertEquals("Plan!", p.text.trim())
        assertEquals("Qala yürüşü", p.proposal?.title)
        assertEquals(10, p.proposal?.capacity)
    }

    @Test fun `parser heç bir girişdə exception çıxarmır`() {
        // fuzz-vari: müxtəlif qəribə girişlər — hamısı AssistantParse qaytarır
        val inputs = listOf("", "[TƏDBİR]", "{", "}}}{", "[TƏDBİR]{}",
            "[TƏDBİR]{\"capacity\":\"abc\"}", "\u0000\u0001", "null")
        inputs.forEach { input ->
            val p: AssistantParse = parseAssistantEvent(input)
            assertNull("giriş: ${input.take(20)}", p.proposal)
        }
    }

    // ── dateLabelFor — aralıq xarici epochDay ──

    @Test fun `dateLabelFor bugün və sabah`() {
        val today = java.time.LocalDate.now()
        assertEquals("Bu gün", dateLabelFor(today.toEpochDay()))
        assertEquals("Sabah", dateLabelFor(today.plusDays(1).toEpochDay()))
    }

    @Test fun `dateLabelFor korlanmış epochDay-də placeholder verir`() {
        assertEquals("—", dateLabelFor(Long.MAX_VALUE))
        assertEquals("—", dateLabelFor(Long.MIN_VALUE))
    }

    // ── MapLogGate — rate-limit məntiqi ──

    @Before fun gateReset() = MapLogGate.resetForTest()

    @After fun gateResetAfter() = MapLogGate.resetForTest()

    @Test fun `imza rəqəm fərqlərini normalizə edir`() {
        val a = MapLogGate.signature("tiles", "Failed to load tile 13/4523/2891")
        val b = MapLogGate.signature("tiles", "Failed to load tile 13/4523/2892")
        val c = MapLogGate.signature("tiles", "Failed to load tile 14/9999/0001")
        assertEquals(a, b)   // fərqli koordinat, eyni imza
        assertEquals(a, c)   // hamısı eyni şablondur
    }

    @Test fun `eyni imza pəncərə içində yalnız bir dəfə keçir`() {
        assertTrue(MapLogGate.allow(1_000, "sig-a"))
        assertFalse(MapLogGate.allow(2_000, "sig-a"))  // 15s içində təkrar
        assertTrue(MapLogGate.allow(2_500, "sig-b"))   // fərqli imza keçir
    }

    @Test fun `eyni imza yeni pəncərədə yenidən keçir`() {
        assertTrue(MapLogGate.allow(1_000, "sig-a"))
        assertFalse(MapLogGate.allow(2_000, "sig-a"))
        assertTrue(MapLogGate.allow(20_000, "sig-a"))  // pəncərə döndü
    }

    @Test fun `pəncərə başına maksimum 3 qeyd keçir`() {
        assertTrue(MapLogGate.allow(1_000, "s1"))
        assertTrue(MapLogGate.allow(1_100, "s2"))
        assertTrue(MapLogGate.allow(1_200, "s3"))
        assertFalse(MapLogGate.allow(1_300, "s4"))     // cap dolub
        assertFalse(MapLogGate.allow(1_400, "s1"))     // həm də təkrar
        assertTrue(MapLogGate.allow(20_000, "s4"))     // yeni pəncərə açıldı
    }

    // ── AppEngine — naməlum ID-lər heç vaxt çökmür ──

    private val engine = AppEngine()

    @Test fun `naməlum məkanda xərcləmə null qaytarır`() {
        assertNull(engine.spendVenueXp("no-such-venue", 100, 50.0))
    }

    @Test fun `naməlum təklifdə kupon alınmır`() {
        assertNull(engine.buyCoupon("no-such-offer"))
    }

    @Test fun `naməlum tədbirə qoşulma false`() {
        assertFalse(engine.join("no-such-event"))
    }

    @Test fun `naməlum tədbirə check-in false`() {
        assertFalse(engine.checkInEvent("no-such-event"))
    }

    @Test fun `kart yoxdanda xpCardValidFor false`() {
        assertFalse(engine.xpCardValidFor("v1"))
    }

    @Test fun `eventReminder mövcud tədbirlə problemsiz işləyir`() {
        val e = engine.eventById("e0") ?: return
        engine.eventReminder(e, "24 saatdan sonra")
        // çökmədi — bildiriş əlavə olundu (pref-lərdən asılı olaraq)
    }
}
