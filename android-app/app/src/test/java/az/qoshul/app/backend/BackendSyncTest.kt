package az.qoshul.app.backend

import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.backend.SupabaseSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M1 (D-078) testləri — iki qat:
 *
 *  1. [SupabaseMapTest] — [SupabaseSync.mapEvent] XARİS funksiyası:
 *     hər [SyncEvent] → düzgün RPC adı + parametrlər (şəbəkə YOXDUR;
 *     qayda 8: server funksiyaları supabase/0002_server_rules.sql ilə
 *     add adda tutmalıdır).
 *  2. [EngineSyncTest] — AppEngine + recording sink: mutasiyalar güzgü
 *     hadisələri çıxarır; sync=null (default) davranışı dəyişmir.
 */
class SupabaseMapTest {

    private fun fn(e: SyncEvent) = SupabaseSync.mapEvent(e)?.fn

    @Test fun `daily login intent fn_daily_login`() {
        val c = SupabaseSync.mapEvent(SyncEvent.DailyLoginIntent)!!
        assertEquals("fn_daily_login", c.fn)
        assertTrue(c.params.isEmpty())
    }

    @Test fun `swipe intent fn_record_swipe params`() {
        val c = SupabaseSync.mapEvent(
            SyncEvent.SwipeIntent(targetId = "uuid-1", interested = true, matched = true))!!
        assertEquals("fn_record_swipe", c.fn)
        assertEquals("uuid-1", c.params["p_target_id"]!!.toString().trim('"'))
        assertEquals("true", c.params["p_interested"]!!.toString())
    }

    @Test fun `xp awarded lokal nəticədir — güzgü getmir (qayda 8)`() {
        assertNull(SupabaseSync.mapEvent(SyncEvent.XpAwarded("join-e1", 10, "award")))
    }

    @Test fun `buy coupon intent fn_buy_coupon`() {
        val c = SupabaseSync.mapEvent(SyncEvent.BuyCouponIntent("o1"))!!
        assertEquals("fn_buy_coupon", c.fn)
        assertEquals("o1", c.params["p_offer_id"]!!.toString().trim('"'))
    }

    @Test fun `preview spend intent parametrləri`() {
        val c = SupabaseSync.mapEvent(
            SyncEvent.PreviewSpendIntent("v5", xpAmount = 100, billAzn = 40.0))!!
        assertEquals("fn_preview_spend", c.fn)
        assertEquals("v5", c.params["p_venue_id"]!!.toString().trim('"'))
        assertEquals(100.0, (c.params["p_xp_amount"] as kotlinx.serialization.json.JsonPrimitive).content.toDouble(), 1e-9)
    }

    @Test fun `spend intent note ile`() {
        val c = SupabaseSync.mapEvent(
            SyncEvent.SpendIntent("v5", 100, 40.0, "Kassada XP endirimi"))!!
        assertEquals("fn_spend_venue_xp", c.fn)
        assertEquals("Kassada XP endirimi", c.params["p_note"]!!.toString().trim('"'))
    }

    @Test fun `xp kartı issue + consume`() {
        assertEquals("fn_issue_xp_card_nonce",
            fn(SyncEvent.XpCardIssueIntent("v5")))
        val c = SupabaseSync.mapEvent(SyncEvent.XpCardConsumeIntent("QSHUL-XP-123456-9"))!!
        assertEquals("fn_consume_xp_card_nonce", c.fn)
        assertEquals("QSHUL-XP-123456-9", c.params["p_nonce"]!!.toString().trim('"'))
    }

    @Test fun `rezerv intentləri — create confirm use cancel noshow`() {
        assertEquals("fn_create_reservation",
            fn(SyncEvent.CreateReservationIntent("v5", "2026-11-09", "18:00", 4, "", null, "r1")))
        assertEquals("fn_confirm_reservation",
            fn(SyncEvent.ConfirmReservationIntent("QSH-r-abc")))
        assertEquals("fn_use_reservation",
            fn(SyncEvent.UseReservationIntent("QSH-r-abc")))
        assertEquals("fn_cancel_reservation",
            fn(SyncEvent.CancelReservationIntent("r1")))
        assertEquals("fn_mark_no_show", fn(SyncEvent.NoShowIntent("r1")))
    }

    @Test fun `check-in intent null koordinatla`() {
        val c = SupabaseSync.mapEvent(
            SyncEvent.CheckInIntent("e1", lat = null, lng = null))!!
        assertEquals("fn_check_in_event", c.fn)
        assertNull(c.params["p_lat"])
        assertNull(c.params["p_lng"])
    }

    @Test fun `review + report + block + message`() {
        assertEquals("fn_add_review",
            fn(SyncEvent.AddReviewIntent("e1", 5, "əla idi")))
        assertEquals("fn_report",
            fn(SyncEvent.ReportIntent("USER", "p2", "spam", "")))
        assertEquals("fn_block_user", fn(SyncEvent.BlockIntent("p2", blocked = true)))
        assertEquals("fn_unblock_user", fn(SyncEvent.BlockIntent("p2", blocked = false)))
        assertEquals("fn_send_message",
            fn(SyncEvent.MessageIntent("PERSONAL", null, "p2", "salam")))
    }

    @Test fun `create event intent bütün sahələr`() {
        val c = SupabaseSync.mapEvent(
            SyncEvent.CreateEventIntent(
                localId = "c1", title = "Yürüş", category = "Yürüş", city = "Bakı",
                location = "Qala", dateIso = "2026-11-14", time = "09:00", capacity = 10,
                description = "5 km", priceAzn = null, ageLimit = "Hamı üçün",
                language = "Azərbaycan dili", level = "Başlanğıc", format = "Oflayn",
                approval = false, recurring = "Bir dəfəlik",
                lat = 40.4, lng = 49.8, venueId = null, imageKey = "friends",
            ))!!
        assertEquals("fn_create_event", c.fn)
        assertEquals("2026-11-14", c.params["p_event_date"]!!.toString().trim('"'))
        assertNull(c.params["p_price_azn"])   // pulsuz → null
        assertEquals("10", (c.params["p_capacity"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals("Yürüş", c.params["p_title"]!!.toString().trim('"'))
    }

    @Test fun `grant venue xp intent`() {
        val c = SupabaseSync.mapEvent(
            SyncEvent.GrantVenueXpIntent("v5", 50, "Təşkilatçı"))!!
        assertEquals("fn_grant_venue_xp", c.fn)
        assertEquals("Təşkilatçı", c.params["p_note"]!!.toString().trim('"'))
    }

    @Test fun `location + notif pref + invite`() {
        assertEquals("fn_update_location",
            fn(SyncEvent.LocationUpdateIntent(40.4, 49.8)))
        assertEquals("fn_set_notif_pref",
            fn(SyncEvent.NotifPrefIntent("MARKETING", enabled = false)))
        assertEquals("fn_create_invite", fn(SyncEvent.InviteIntent))
    }

    @Test fun `claim share intent`() {
        assertEquals("fn_claim_share", fn(SyncEvent.ClaimShareIntent("shr-abc")))
    }
}
