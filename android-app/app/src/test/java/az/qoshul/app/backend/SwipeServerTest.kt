package az.qoshul.app.backend

import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.backend.SupabaseSync
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-176 (ikinci dalğa): match SERVER avtoriteti testləri.
 *  • likesBack model sahəsi SİLİNDİ (grep-paritet)
 *  • swipeRpc xətasında kart GERİ QAYIDIR (swipes-dən silinir)
 *  • matched=true → optimistic match + dostluq + XP
 *  • swipes + friendships applyServerData ilə bərpa olunur
 */
class SwipeServerTest {

    private fun engine() = AppEngine(dataSource = FakeQoshulDataSource)

    @Test fun `likesBack model sahəsi silinib`() {
        val src = java.io.File(
            "src/main/java/az/qoshul/app/core/model/Models.kt",
        ).readText()
        assertFalse(src.contains("likesBack"))
    }

    @Test fun `RPC xətasında kart geri qayıdır və xəta görünür`() {
        val e = engine()
        e.swipeOutcomeOverride = { _ -> AppEngine.SwipeOutcome.ERROR }
        val matched = e.swipe("p1", true)
        assertFalse(matched)
        // kart geri qayıdıb — yenidən swipe mümkündür
        assertFalse(e.state.value.swipes.containsKey("p1"))
        assertTrue(e.state.value.syncError.isNotEmpty())
    }

    @Test fun `matched true — match dostluq XP və consumeLastMatch`() {
        val e = engine()
        val xpBefore = e.state.value.xp
        e.swipeOutcomeOverride = { _ -> AppEngine.SwipeOutcome.MATCHED }
        assertTrue(e.swipe("p1", true))
        val st = e.state.value
        assertTrue("p1" in st.matches)
        assertEquals("accepted", st.friendships["p1"])
        assertTrue(st.friendMessages.containsKey("p1"))
        assertEquals("p1", e.consumeLastMatch())
        assertEquals(null, e.consumeLastMatch()) // tək istifadəlik
        assertEquals(xpBefore + 10, st.xp)
    }

    @Test fun `NOT_MATCHED — swipe qeyd olunur match yoxdur`() {
        val e = engine()
        e.swipeOutcomeOverride = { _ -> AppEngine.SwipeOutcome.NOT_MATCHED }
        assertFalse(e.swipe("p1", true))
        val st = e.state.value
        assertTrue(st.swipes["p1"] == true)
        assertFalse("p1" in st.matches)
        assertTrue(st.syncError.isEmpty())
    }

    @Test fun `OFFLINE — güzgü intent-i gedir match proqnozu yoxdur`() {
        val rec = RecordingSync()
        val e = AppEngine(dataSource = FakeQoshulDataSource, sync = rec)
        e.swipeOutcomeOverride = { _ -> AppEngine.SwipeOutcome.OFFLINE }
        assertFalse(e.swipe("p1", true))
        val s = rec.events.filterIsInstance<SyncEvent.SwipeIntent>().last()
        assertTrue(s.interested)
        assertFalse(s.matched)
    }

    @Test fun `swipes və friendships applyServerData-da bərpa olunur`() {
        val e = engine()
        // restart simulyasiyası: serverdən keçirilmiş kartlar + dostluqlar gəlir
        e.applyServerData(
            az.qoshul.app.core.backend.SupabaseReader.ServerData(
                swipedPeople = mapOf("p3" to false, "p4" to true),
                friendships = mapOf("p2" to "accepted", "p5" to "sent"),
                people = FakeQoshulDataSource.people,
            ),
        )
        val st = e.state.value
        assertEquals(false, st.swipes["p3"])
        assertEquals(true, st.swipes["p4"])
        assertEquals("accepted", st.friendships["p2"])
        assertEquals("sent", st.friendships["p5"])
        // keçilmiş kartlar match namizədlərindən düşür
        assertTrue(e.matchCandidates.none { it.id == "p3" || it.id == "p4" })
    }

    @Test fun `dostluq RPC-ləri xəritələnir`() {
        val req = SupabaseSync.mapEvent(SyncEvent.RequestFriendIntent("p2"))
        org.junit.Assert.assertNotNull(req)
        assertEquals("fn_request_friend", req!!.fn)
        val resp = SupabaseSync.mapEvent(SyncEvent.RespondFriendIntent("p2", true))
        assertEquals("fn_respond_friend", resp!!.fn)
    }

    @Test fun `requestFriend və acceptFriend RPC göndərir`() {
        val rec = RecordingSync()
        val e = AppEngine(dataSource = FakeQoshulDataSource, sync = rec)
        e.requestFriend("p2")
        assertEquals("sent", e.state.value.friendships["p2"])
        assertTrue(rec.events.any { it is SyncEvent.RequestFriendIntent && it.personId == "p2" })
        e.acceptFriend("p2", accept = true)
        assertEquals("accepted", e.state.value.friendships["p2"])
        assertTrue(rec.events.any { it is SyncEvent.RespondFriendIntent && it.accept })
        e.acceptFriend("p5", accept = false)
        assertFalse(e.state.value.friendships.containsKey("p5"))
    }
}
