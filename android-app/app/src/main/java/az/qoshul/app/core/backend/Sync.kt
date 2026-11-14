package az.qoshul.app.core.backend


/**
 * M1 (D-078): lokal mutasiya → server intent hadisələri.
 *
 * Semantika: hər hadisə AppEngine-də BAŞ VERMİŞ əməliyyatın güzgüsüdür;
 * [SupabaseSync] onu müvafiq RPC-yə çevirir. Server nəticəsi avtoritativdir
 * (qayda 8): məbləğ/xəta serverdə YENİDƏN hesablanır — klient rəqəmi etibar
 * edilmir. M1-də engine lokal vəziyyəti offline-cache kimi saxlayır;
 * tam server-avtoritet keçidi M2 (Rust nüvə) planındadır.
 */
sealed interface SyncEvent {

    /** fn_award_xp tərəfindən idempotent qeyd alınan XP (lokal award uğurlu oldu). */
    data class XpAwarded(val key: String, val amount: Int, val note: String) : SyncEvent

    /** fn_daily_login — D-073 seriya qaydası serverdə icra olunur. */
    data object DailyLoginIntent : SyncEvent

    /** fn_record_swipe — D-074: hərəkət limitsiz; XP cap serverdə. */
    data class SwipeIntent(val targetId: String, val interested: Boolean, val matched: Boolean) : SyncEvent

    /** fn_join_event — FULL/PAST/approval yoxlamaları serverdə. */
    data class JoinEventIntent(val eventId: String) : SyncEvent

    /** fn_check_in_event — B1: 200 m ST_DWithin serverdə; GPS koordinatı varsa. */
    data class CheckInIntent(val eventId: String, val lat: Double?, val lng: Double?) : SyncEvent

    /** fn_create_event — N1: ID serverdə ULID ilə yaranır (lokalId yalnız log üçün). */
    data class CreateEventIntent(
        val localId: String, val title: String, val category: String, val city: String,
        val location: String, val dateIso: String, val time: String, val capacity: Int,
        val description: String, val priceAzn: Int?, val ageLimit: String,
        val language: String, val level: String, val format: String,
        val approval: Boolean, val recurring: String,
        val lat: Double?, val lng: Double?, val venueId: String?, val imageKey: String,
    ) : SyncEvent

    /** fn_preview_spend — A1 typed xəta serverdə. */
    data class PreviewSpendIntent(val venueId: String, val xpAmount: Int, val billAzn: Double) : SyncEvent

    /** fn_spend_venue_xp — B6 atomik; D-072 balans sərhədi serverdə. */
    data class SpendIntent(
        val venueId: String, val xpAmount: Int, val billAzn: Double, val note: String,
    ) : SyncEvent

    /** fn_buy_coupon — D-072: yalnız balansda var olandan; REDD. */
    data class BuyCouponIntent(val offerId: String) : SyncEvent

    /**
     * fn_finish_activity — L.3 təşkilatçı bitirmə axını (F2, audit 2026-11-14).
     * Köhnə fn_grant_venue_xp self-grant idi; yeni server funksiyası host-
     * müəllifliyi, sabit məbləği (50/25) və idempotensiya açarını özü yoxlayır.
     */
    data class GrantVenueXpIntent(
        val venueId: String, val eventId: String, val taggedIds: List<String>,
    ) : SyncEvent

    /** fn_issue_xp_card_nonce — B5: CSPRNG serverdə. */
    data class XpCardIssueIntent(val venueId: String) : SyncEvent

    /** fn_consume_xp_card_nonce — təkistifadə serverdə. */
    data class XpCardConsumeIntent(val nonce: String) : SyncEvent

    /** fn_create_reservation — QR/share_token serverdə yaranır (N1/N8). */
    data class CreateReservationIntent(
        val venueId: String, val dateIso: String, val time: String, val party: Int,
        val note: String, val couponCode: String?, val localId: String,
    ) : SyncEvent

    /** fn_confirm_reservation — partnyor təsdiqi (PENDING→CONFIRMED). */
    data class ConfirmReservationIntent(val qrCode: String) : SyncEvent

    /** fn_use_reservation — məkanda skan (CONFIRMED→USED +30 XP). */
    data class UseReservationIntent(val qrCode: String) : SyncEvent

    data class CancelReservationIntent(val reservationId: String) : SyncEvent

    /** fn_mark_no_show — ×3 → 7 gün restrict (serverdə). */
    data class NoShowIntent(val reservationId: String) : SyncEvent

    /** fn_claim_share — N8 "Qoşul" iddiası. */
    data class ClaimShareIntent(val shareToken: String) : SyncEvent

    /** fn_add_review — B4: PENDING; XP yalnız moderasiyadan sonra. */
    data class AddReviewIntent(val eventId: String, val rating: Int, val body: String) : SyncEvent

    /** fn_report — N9: 3+ müxtəlif reporter → avto-restrict. */
    data class ReportIntent(
        val targetType: String, val targetId: String, val reason: String, val note: String,
    ) : SyncEvent

    data class BlockIntent(val targetId: String, val blocked: Boolean) : SyncEvent

    /** fn_send_message — N6 rate limit + spam serverdə. */
    data class MessageIntent(
        val kind: String, val eventId: String?, val personId: String?, val body: String,
    ) : SyncEvent

    /** fn_create_invite — B3: XP birinci check-in-dən sonra (serverdə). */
    data object InviteIntent : SyncEvent

    /** fn_update_location — B7 görünüş filtri bunun üzərindən. */
    data class LocationUpdateIntent(val lat: Double, val lng: Double) : SyncEvent

    /** fn_set_notif_pref. */
    data class NotifPrefIntent(val notifType: String, val enabled: Boolean) : SyncEvent
}

/** Güzgü çıxışı — implementasiyalar bloklamamalı, heç vaxt atılmamalıdır. */
fun interface SyncSink {
    fun report(event: SyncEvent)
}

/** MOCK rejimi — heç nə etmir (default; testlər deterministik qalır). */
val NoopSync: SyncSink = SyncSink { }
