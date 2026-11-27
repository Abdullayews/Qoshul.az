package az.qoshul.app.backend

import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-176 (ikinci dalğa): report() təsnifatı testləri.
 * KRİTİK: join/leave/save/swipe/buy/spend/reserve/checkin/finish —
 * xətada rollback; METADATA (prefs/lokasiya/bildiriş) — optimistic qalır.
 */
class ReportClassificationTest {

    private fun engine() = AppEngine(dataSource = FakeQoshulDataSource)

    private fun isCritical(e: SyncEvent): Boolean =
        engine().criticalityOf(e) == AppEngine.Criticality.CRITICAL

    @Test fun `kritik əməliyyatlar CRITICAL təsnifatındadır`() {
        assertTrue(isCritical(SyncEvent.JoinEventIntent("e1")))
        assertTrue(isCritical(SyncEvent.LeaveEventIntent("e1")))
        assertTrue(isCritical(SyncEvent.SaveEventIntent("e1")))
        assertTrue(isCritical(SyncEvent.SwipeIntent("p1", true, false)))
        assertTrue(isCritical(SyncEvent.BuyCouponIntent("o1")))
        assertTrue(isCritical(SyncEvent.SpendIntent("v1", 100, 40.0, "")))
        assertTrue(isCritical(
            SyncEvent.CreateReservationIntent("v1", "2026-11-27", "18:00", 2, "", null, "local-1")))
        assertTrue(isCritical(SyncEvent.UseReservationIntent("QR")))
        assertTrue(isCritical(SyncEvent.CheckInIntent("e1", null, null)))
        assertTrue(isCritical(SyncEvent.GrantVenueXpIntent("v1", "e1", listOf("p1"))))
    }

    @Test fun `metadata əməliyyatları METADATA təsnifatındadır`() {
        assertTrue(!isCritical(SyncEvent.DailyLoginIntent))
        assertTrue(!isCritical(SyncEvent.LocationUpdateIntent(40.4, 49.8)))
        assertTrue(!isCritical(SyncEvent.PrefsIntent(mapVisibility = "OFF")))
        assertTrue(!isCritical(SyncEvent.SetAvatarIntent("coffee")))
        assertTrue(!isCritical(SyncEvent.XpAwarded("k", 10, "")))
    }

    @Test fun `rollback XP-ni geri alır və xətanı göstərir`() {
        val e = engine()
        val xpBefore = e.state.value.xp
        e.applyServerData(
            az.qoshul.app.core.backend.SupabaseReader.ServerData(
                events = FakeQoshulDataSource.events,
                offers = FakeQoshulDataSource.offers,
                myProfile = az.qoshul.app.core.backend.SupabaseReader.MyProfile(
                    id = "t1", firstName = "T", lastName = "", city = "Bakı",
                    bio = "", xp = xpBefore, onboarded = true, photoCount = 0,
                ),
            ),
        )
        // kritik əməliyyat: join → optimistic XP
        e.join(FakeQoshulDataSource.events.first { !it.past }.id)
        val xpAfterJoin = e.state.value.xp
        assertTrue(xpAfterJoin >= xpBefore)
        // RPC xətası → rollback
        e.reportFailure("sync:fn_join_event", IllegalStateException("HTTP 500"))
        assertTrue(e.state.value.xp < xpAfterJoin || e.state.value.xp == xpBefore)
        assertTrue(e.state.value.syncError.isNotEmpty())
    }
}
