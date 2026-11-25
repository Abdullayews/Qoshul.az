package az.qoshul.app.core.backend

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add

/**
 * SyncEvent → RPC xəritəsi (D-078-dən D-151-yə köçürülüb — xalis funksiya,
 * JVM-də test olunur; şəbəkə/client YOXDUR).
 *
 * Qayda 8 (product-spec): məbləğ/xəta serverdə YENİDƏN hesablanır — klient
 * rəqəmi güzgüdə yalnız identifikator kimi gedir (offerId, eventId...).
 * Serverin qəbulu avtoritativdir.
 */
internal fun mapEventImpl(e: SyncEvent): BackendCall? = when (e) {
    // lokal nəticələr güzgü getmir — server öz intent RPC-si daxilində
    // eyni qaydanı icra edir (double-authority OLMAZ, qayda 8)
    is SyncEvent.XpAwarded -> null
    is SyncEvent.DailyLoginIntent ->
        rpcCall("fn_daily_login")
    is SyncEvent.SwipeIntent ->
        rpcCall("fn_record_swipe", "p_target_id" to e.targetId, "p_interested" to e.interested)
    is SyncEvent.JoinEventIntent ->
        rpcCall("fn_join_event", "p_event_id" to e.eventId)
    is SyncEvent.CheckInIntent ->
        rpcCall(
            "fn_check_in_event", "p_event_id" to e.eventId,
            "p_lat" to e.lat, "p_lng" to e.lng,
        )
    is SyncEvent.CreateEventIntent ->
        rpcCall(
            "fn_create_event",
            "p_title" to e.title, "p_category" to e.category, "p_city" to e.city,
            "p_location" to e.location, "p_event_date" to e.dateIso,
            "p_event_time" to e.time, "p_capacity" to e.capacity,
            "p_description" to e.description, "p_price_azn" to e.priceAzn,
            "p_age_limit" to e.ageLimit, "p_language" to e.language,
            "p_level" to e.level, "p_format" to e.format,
            "p_approval" to e.approval, "p_recurring" to e.recurring,
            "p_lat" to e.lat, "p_lng" to e.lng, "p_venue_id" to e.venueId,
            "p_image" to e.imageKey,
        )
    is SyncEvent.PreviewSpendIntent ->
        rpcCall(
            "fn_preview_spend", "p_venue_id" to e.venueId,
            "p_xp_amount" to e.xpAmount, "p_bill_azn" to e.billAzn,
        )
    is SyncEvent.SpendIntent ->
        rpcCall(
            "fn_spend_venue_xp", "p_venue_id" to e.venueId,
            "p_xp_amount" to e.xpAmount, "p_bill_azn" to e.billAzn, "p_note" to e.note,
        )
    is SyncEvent.BuyCouponIntent ->
        rpcCall("fn_buy_coupon", "p_offer_id" to e.offerId)
    is SyncEvent.GrantVenueXpIntent ->
        // F2 (audit 2026-11-14): fn_grant_venue_xp self-grant idi →
        // fn_finish_activity host-müəlliflik + sabit məbləğ (50/25) + idempotensiya.
        rpcCall(
            "fn_finish_activity", "p_event_id" to e.eventId,
            "p_tagged_ids" to e.taggedIds,
        )
    is SyncEvent.XpCardIssueIntent ->
        rpcCall("fn_issue_xp_card_nonce", "p_venue_id" to e.venueId)
    is SyncEvent.XpCardConsumeIntent ->
        rpcCall("fn_consume_xp_card_nonce", "p_nonce" to e.nonce)
    is SyncEvent.CreateReservationIntent ->
        rpcCall(
            "fn_create_reservation", "p_venue_id" to e.venueId,
            "p_resv_date" to e.dateIso, "p_resv_time" to e.time,
            "p_party" to e.party, "p_note" to e.note,
            // F6 (audit): M1 kupon KODU, M2 server-ID; 0004 hər ikisini qəbul edir.
            "p_coupon_code" to e.couponCode,
        )
    is SyncEvent.ConfirmReservationIntent ->
        rpcCall("fn_confirm_reservation", "p_qr_code" to e.qrCode)
    is SyncEvent.UseReservationIntent ->
        rpcCall("fn_use_reservation", "p_qr_code" to e.qrCode)
    is SyncEvent.CancelReservationIntent ->
        rpcCall("fn_cancel_reservation", "p_id" to e.reservationId)
    is SyncEvent.NoShowIntent ->
        rpcCall("fn_mark_no_show", "p_id" to e.reservationId)
    is SyncEvent.ClaimShareIntent ->
        rpcCall("fn_claim_share", "p_share_token" to e.shareToken)
    is SyncEvent.AddReviewIntent ->
        rpcCall(
            "fn_add_review", "p_event_id" to e.eventId,
            "p_rating" to e.rating, "p_body" to e.body,
        )
    is SyncEvent.ReportIntent ->
        rpcCall(
            "fn_report", "p_target_type" to e.targetType,
            "p_target_profile_id" to e.targetId, "p_reason" to e.reason, "p_note" to e.note,
        )
    is SyncEvent.BlockIntent ->
        rpcCall(
            if (e.blocked) "fn_block_user" else "fn_unblock_user",
            "p_target_id" to e.targetId,
        )
    is SyncEvent.MessageIntent ->
        rpcCall(
            "fn_send_message", "p_kind" to e.kind,
            "p_event_id" to e.eventId,
            "p_recipient_id" to e.personId, // UUID string; null EVENT mesajlarında JsonNull gedir
            "p_body" to e.body,
        )
    is SyncEvent.InviteIntent -> rpcCall("fn_create_invite")
    is SyncEvent.LocationUpdateIntent ->
        rpcCall("fn_update_location", "p_lat" to e.lat, "p_lng" to e.lng)
    is SyncEvent.NotifPrefIntent ->
        rpcCall("fn_set_notif_pref", "p_notif_type" to e.notifType, "p_enabled" to e.enabled)
    is SyncEvent.SaveProfileIntent -> rpcCall(
        "fn_save_profile",
        "p_first_name" to e.firstName,
        "p_last_name" to e.lastName,
        "p_birth_date" to e.birthDateIso,
        "p_gender" to e.gender,
        "p_bio" to e.bio,
        "p_photo_count" to e.photoCount,
        "p_city" to e.city,
        "p_status" to e.status,
        "p_university" to e.university,
        "p_work_field" to e.workField,
        "p_languages" to e.languages,
        "p_goals" to e.goals,
        "p_personality" to e.personality?.let { p ->
            kotlinx.serialization.json.buildJsonObject { p.forEach { (k, v) -> put(k, v) } }
        },
        "p_availability" to e.availability,
        "p_group_size" to e.groupSize,
        "p_budget" to e.budget,
        "p_onboarded" to e.onboarded,
        "p_profile_completed" to e.profileCompleted,
        "p_interests" to e.interests,
    )
    is SyncEvent.SetAvatarIntent -> rpcCall("fn_set_avatar", "p_image_key" to e.imageKey)
}

internal data class RpcCall(
    override val fn: String,
    override val params: JsonObject,
) : BackendCall

private fun rpcCall(fn: String, vararg pairs: Pair<String, Any?>): RpcCall {
    val obj = buildJsonObject {
        for ((k, v) in pairs) {
            when (v) {
                // D-152 qərarı: null açarlar ÇIXARILIR (PostgREST default
                // parametr dəyərləri tətbiq olunur; köhnə testlərin
                // kontraktı da bundır). PGRST202 yalnız imza uyğunsuzluğunda
                // baş verirdi — 0007-də bütün p_* default null-dır.
                null -> {}
                is Number -> put(k, v)
                is Boolean -> put(k, v)
                is String -> put(k, v)
                is List<*> -> putJsonArray(k) { v.forEach { add(it.toString()) } }
                else -> put(k, v.toString())
            }
        }
    }
    return RpcCall(fn, obj)
}
