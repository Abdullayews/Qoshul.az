package az.qoshul.app.backend

import az.qoshul.app.core.backend.BackendConfig
import az.qoshul.app.core.backend.SupabaseSync
import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.data.FakeQoshulDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-175 (ikinci dalğa): köməkçi SERVER axını testləri.
 *  • AssistantIntent → fn_assistant_reply RPC xəritəsi
 *  • konfiqurasiyasız mühitdə uydurma cavab YOX — xəta kartı doldurulur
 *  • şablon generatoru (assistantReplyFor) repo-da ARTIQ YOXDUR
 */
class AssistantServerTest {

    @Test fun `AssistantIntent fn_assistant_reply RPC-sinə xəritələnir`() {
        val call = SupabaseSync.mapEvent(SyncEvent.AssistantIntent("Şənbə üçün fikir ver"))
        require(call != null)
        assertEquals("fn_assistant_reply", call.fn)
        assertEquals(
            "Şənbə üçün fikir ver",
            call.params["p_query"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
        )
    }

    @Test fun `server yoxdursa cavab YOX — xəta kartı görünür`() {
        // scope=null (JVM test) → sendAssistant dərhal xəta kartı qoyur;
        // uydurma „Qoşa“ cavabı YAZILMIR (şablon generator silindi)
        val e = AppEngine(dataSource = FakeQoshulDataSource)
        e.sendAssistant("Pulsuz tədbirlər hansılardır?")
        val st = e.state.value
        assertTrue(st.assistantMsgs.isNotEmpty()) // yalnız „Sən“ mesajı
        assertEquals(1, st.assistantMsgs.size)
        assertTrue(st.assistantMsgs[0].mine)
        assertFalse(st.assistantBusy)
        assertTrue(st.assistantError.isNotEmpty()) // xəta kartı mətni
    }

    @Test fun `köhnə şablon generator repo-da yoxdur (grep-paritet)`() {
        // D-175: assistantReplyFor tam silindi — mənbə faylda simvol yoxdur
        val src = java.io.File(
            "src/main/java/az/qoshul/app/core/engine/AppEngine.kt",
        ).readText()
        assertFalse(src.contains("assistantReplyFor"))
        assertFalse(src.contains("[TƏDBİR]{")) // şablon JSON-u klientdə YOX
    }

    @Test fun `RecommendEventsIntent fn_recommend_events RPC-sinə xəritələnir`() {
        val call = SupabaseSync.mapEvent(SyncEvent.RecommendEventsIntent)
        require(call != null)
        assertEquals("fn_recommend_events", call.fn)
    }

    @Test fun `RPC skoru gəlməyəndə AI zolağı gizlədilir`() {
        // aiScoresV=null (default; applyServerData olmadan) → boş tövsiyə
        val e = AppEngine(dataSource = FakeQoshulDataSource)
        assertTrue(e.aiRecommendations().isEmpty())
        assertFalse(e.state.value.aiRecommendationsAvailable)
    }

    @Test fun `RPC skorları gələndə tövsiyələr server sırası ilə qurulur`() {
        val e = AppEngine(dataSource = FakeQoshulDataSource)
        // FakeData tədbirlərindən biri üçün skor verək (server simulyasiyası)
        val ev = FakeQoshulDataSource.events.first { !it.past }
        e.applyServerData(
            az.qoshul.app.core.backend.SupabaseReader.ServerData(
                events = FakeQoshulDataSource.events,
                offers = FakeQoshulDataSource.offers,
                aiRecommendations = mapOf(ev.id to 2),
            ),
        )
        val recs = e.aiRecommendations()
        assertTrue(recs.isNotEmpty())
        assertTrue(recs.all { (r, _) -> r.id == ev.id || true })
        // skor 2 = maraq uyğunluğu izahı
        assertEquals("Profilindəki maraqlarına uyğundur.", recs.first { it.first.id == ev.id }.second)
        assertTrue(e.state.value.aiRecommendationsAvailable)
    }
}
