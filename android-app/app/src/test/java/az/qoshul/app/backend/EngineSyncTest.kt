package az.qoshul.app.backend

import az.qoshul.app.core.backend.BackendConfig
import az.qoshul.app.core.backend.NoopSync
import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.backend.SyncSink
import az.qoshul.app.core.engine.AppEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** Test köməkçisi — hadisələri yığıır (mühərrikin güzgü çıxışını sübut edir). */
class RecordingSync : SyncSink {
    val events = mutableListOf<SyncEvent>()
    override fun report(event: SyncEvent) {
        events += event
    }
}

class EngineSyncTest {

    private val frozen: LocalDateTime = LocalDateTime.of(2026, 11, 7, 12, 0)

    private fun engine(rec: RecordingSync? = RecordingSync()) =
        AppEngine(clock = { frozen }, sync = rec)

    @Test fun `dailyLogin DailyLoginIntent çıxarır`() {
        val rec = RecordingSync()
        engine(rec).dailyLogin()
        assertTrue(rec.events.any { it is SyncEvent.DailyLoginIntent })
        // lokal award da hadisələnir (XpAwarded — güzgü getmir, amma qeyd olunur)
        assertTrue(rec.events.filterIsInstance<SyncEvent.XpAwarded>().any { it.key.startsWith("login-") })
    }

    @Test fun `swipe match hadisəsi Interested=true Matched=true`() {
        val rec = RecordingSync()
        val e = engine(rec)
        // p1 likesBack=true olan şəxsdir (FakeData)
        val matched = e.swipe("p1", true)
        assertTrue(matched)
        val s = rec.events.filterIsInstance<SyncEvent.SwipeIntent>().last()
        assertTrue(s.interested)
        assertTrue(s.matched)
    }

    @Test fun `buyCoupon uğursuz olsa da intent gedir (server qərarı avtoritativ)`() {
        val rec = RecordingSync()
        val e = engine(rec)
        val res = e.buyCoupon("o1")   // o1: Coffee Lab, xpPrice=200, balans 120 → uğursuz
        assertTrue(res is az.qoshul.app.core.model.CouponPurchase.Failure)
        assertTrue(rec.events.any { it is SyncEvent.BuyCouponIntent && it.offerId == "o1" })
    }

    @Test fun `spendVenueXp SpendIntent çıxarır`() {
        val rec = RecordingSync()
        val e = engine(rec)
        val d = e.spendVenueXp("v5", 100, 40.0)
        assertTrue(d != null)
        val s = rec.events.filterIsInstance<SyncEvent.SpendIntent>().first()
        assertEquals("v5", s.venueId)
        assertEquals(100, s.xpAmount)
    }

    @Test fun `checkInReservation UseReservationIntent qr ilə`() {
        val rec = RecordingSync()
        val e = engine(rec)
        val venue = e.partners.first { it.id == "v2" }   // autoAccept=true
        val r = e.createReservation(venue, "Bu gün", "18:00", 2, "", null)
        assertTrue(r != null)
        assertTrue(e.checkInReservation(r!!.id))
        val u = rec.events.filterIsInstance<SyncEvent.UseReservationIntent>().first()
        assertEquals(r.qrCode, u.qrCode)
    }

    @Test fun `createReservation CreateReservationIntent ISO tarixlə`() {
        val rec = RecordingSync()
        val e = engine(rec)
        val venue = e.partners.first { it.id == "v2" }
        e.createReservation(venue, "Bu gün", "18:00", 2, "", null)
        val c = rec.events.filterIsInstance<SyncEvent.CreateReservationIntent>().first()
        assertEquals("2026-11-07", c.dateIso)   // frozen clock — "Bu gün" → ISO
        assertEquals("v2", c.venueId)
        assertEquals(2, c.party)
    }

    @Test fun `regenerateXpCard və markXpCardUsed issue-consume cütü`() {
        val rec = RecordingSync()
        val e = engine(rec)
        e.regenerateXpCard("v5")
        e.markXpCardUsed()
        assertTrue(rec.events.any { it is SyncEvent.XpCardIssueIntent && it.venueId == "v5" })
        val consume = rec.events.filterIsInstance<SyncEvent.XpCardConsumeIntent>().first()
        assertTrue(consume.nonce.startsWith("QSHUL-XP-"))
    }

    @Test fun `reportUser + blockUser + messages`() {
        val rec = RecordingSync()
        val e = engine(rec)
        e.reportUser("p3", "spam", "")
        e.blockUser("p3")
        e.unblockUser("p3")
        e.sendEventMessage("e1", "salam")
        e.sendFriendMessage("p2", "nə vaxt?")
        assertTrue(rec.events.any { it is SyncEvent.ReportIntent && it.targetId == "p3" })
        assertTrue(rec.events.filterIsInstance<SyncEvent.BlockIntent>().any { it.blocked })
        assertTrue(rec.events.filterIsInstance<SyncEvent.BlockIntent>().any { !it.blocked })
        assertEquals(2, rec.events.filterIsInstance<SyncEvent.MessageIntent>().size)
    }

    @Test fun `finishActivity GrantVenueXpIntent çıxarır`() {
        val rec = RecordingSync()
        val e = engine(rec)
        // e-mafiya: past + venueId=v5; host adını bizimkiləşdiririk
        val past = e.state.value.events.first { it.id == "e-mafiya" && it.venueId != null && it.past }
        e.setProfileName(past.host)
        val res = e.finishActivity(past, listOf("p1", "p2"))
        assertTrue(res != null)
        // F2 (audit 2026-11-14): məbləğ artıq klientdən getmir — intent
        // yalnız event ID + taq edilənləri daşıyır; məbləği serverdə
        // fn_finish_activity sabitləyir (50/25, XpRules pariteti).
        assertTrue(
            rec.events.any {
                it is SyncEvent.GrantVenueXpIntent &&
                    it.eventId == past.id &&
                    it.taggedIds == listOf("p1", "p2")
            },
        )
    }

    @Test fun `sync null default — crash yoxdur, behavior dəyişmir`() {
        val e = AppEngine(clock = { frozen })   // sync YOXDUR (köhnə çağırışlar)
        e.dailyLogin()
        assertTrue(e.state.value.xp > 0)
        NoopSync.report(SyncEvent.DailyLoginIntent)   // NoopSync heç nə etmirmə? — səssiz
    }

    @Test fun `BackendConfig PLACEHOLDER dəyərlərlə MOCK-a düşür`() {
        // CI/local: BuildConfig dəyərləri PLACEHOLDER_* → MOCK (D-079)
        if (BackendConfig.isUsableUrl("PLACEHOLDER_SUPABASE_URL")) {
            throw AssertionError("PLACEHOLDER URL usable sayılmamalıdır")
        }
        if (BackendConfig.isUsableKey("PLACEHOLDER_SUPABASE_ANON_KEY")) {
            throw AssertionError("PLACEHOLDER key usable sayılmamalıdır")
        }
        if (BackendConfig.isUsableUrl("https://abcd.supabase.co")) {
            // normal URL usable-dır
        } else throw AssertionError("real URL usable olmalıdır")
    }
}
