package az.qoshul.app.core.backend

import az.qoshul.app.core.safety.Safety
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add

/**
 * M1 (D-078): SUPABASE rejiminin güzgüsü.
 *
 * Arxitektura: [SyncEvent] (intent) → [mapEvent] (xalis funksiya, JVM-də
 * test olunur) → [BackendCall.Rpc] → `client.postgrest.rpc(...)`.
 *
 * Qayda 8 (product-spec): məbləğ/xəta serverdə YENİDƏN hesablanır — klient
 * rəqəmi güzgüdə yalnız identifikator kimi gedir (offerId, eventId...).
 * Serverin qəbulu avtoritativdir; uğursuzluqlar Safety ilə loglanır və
 * `lastErrors` axınında görünür (UI/developer üçün).
 *
 * versiya pin: supabase-kt 3.9.0 + gotrue-kt 2.6.1 (D-068; hər mərhələ
 * qapısında yenidən yoxlanılır). CIO engine — əlavə native asılılıq YOX.
 * QEYD (D-078): 3.9.0-da namespace transferi BAŞ verib — paket
 * `io.github.jan.supabase` (köhnə `io.github.jan.tennert.supabase` deyil).
 */
class SupabaseSync(
    private val scope: CoroutineScope,
    supabaseUrl: String = BackendConfig.supabaseUrl,
    supabaseAnonKey: String = BackendConfig.supabaseAnonKey,
) : SyncSink {

    /** Uğursuz RPC-lərin son N-i (debug/UI). */
    val lastErrors = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())

    private val client: SupabaseClient? = runCatching {
        createSupabaseClient(supabaseUrl, supabaseAnonKey) {
            install(Postgrest)
        }
    }.getOrElse {
        Safety.onCaught?.invoke("SupabaseSync", it)
        null
    }

    override fun report(event: SyncEvent) {
        val call = mapEvent(event) ?: return
        val c = client ?: return
        scope.launch {
            runCatching { c.postgrest.rpc(function = call.fn, parameters = call.params) }
                .onFailure { t ->
                    Safety.onCaught?.invoke("sync:${call.fn}", t)
                    lastErrors.value = (listOf("${call.fn}: ${t.message?.take(120)}") + lastErrors.value).take(20)
                }
        }
    }

    /** Şəbəkəsiz icra (inteqrasiya testi üçün) — eyni kod yolu. */
    suspend fun reportNow(event: SyncEvent): Result<Unit?>? {
        val call = mapEvent(event) ?: return null
        val c = client ?: return null
        return runCatching { c.postgrest.rpc(function = call.fn, parameters = call.params) }
            .map { null }
    }

    companion object {

        /** Xalis xəritə — JVM test obyekti (şəbəkə/client YOXDUR). */
        sealed interface BackendCall {
            val fn: String
            val params: JsonObject
        }

        fun mapEvent(e: SyncEvent): BackendCall? = when (e) {
            // lokal nəticələr güzgü getmir — server öz intent RPC-si daxilində
            // eyni qaydanı icra edir (double-authority OLMAZ, qayda 8)
            is SyncEvent.XpAwarded -> null
            is SyncEvent.DailyLoginIntent ->
                rpc("fn_daily_login")
            is SyncEvent.SwipeIntent ->
                rpc("fn_record_swipe", "p_target_id" to e.targetId, "p_interested" to e.interested)
            is SyncEvent.JoinEventIntent ->
                rpc("fn_join_event", "p_event_id" to e.eventId)
            is SyncEvent.CheckInIntent ->
                rpc(
                    "fn_check_in_event", "p_event_id" to e.eventId,
                    "p_lat" to e.lat, "p_lng" to e.lng,
                )
            is SyncEvent.CreateEventIntent ->
                rpc(
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
                rpc(
                    "fn_preview_spend", "p_venue_id" to e.venueId,
                    "p_xp_amount" to e.xpAmount, "p_bill_azn" to e.billAzn,
                )
            is SyncEvent.SpendIntent ->
                rpc(
                    "fn_spend_venue_xp", "p_venue_id" to e.venueId,
                    "p_xp_amount" to e.xpAmount, "p_bill_azn" to e.billAzn, "p_note" to e.note,
                )
            is SyncEvent.BuyCouponIntent ->
                rpc("fn_buy_coupon", "p_offer_id" to e.offerId)
            is SyncEvent.GrantVenueXpIntent ->
                // F2 (audit 2026-11-14): köhnə fn_grant_venue_xp client-in
                // amount-u ilə self-grant idi → authenticated-dən revok olundu.
                // Yeni axın: fn_finish_activity — host-müəlliflik, sabit məbləğ
                // (50/25) və idempotensiya server tərəfində yoxlanılır.
                rpc(
                    "fn_finish_activity", "p_event_id" to e.eventId,
                    "p_tagged_ids" to e.taggedIds,
                )
            is SyncEvent.XpCardIssueIntent ->
                rpc("fn_issue_xp_card_nonce", "p_venue_id" to e.venueId)
            is SyncEvent.XpCardConsumeIntent ->
                rpc("fn_consume_xp_card_nonce", "p_nonce" to e.nonce)
            is SyncEvent.CreateReservationIntent ->
                rpc(
                    "fn_create_reservation", "p_venue_id" to e.venueId,
                    "p_resv_date" to e.dateIso, "p_resv_time" to e.time,
                    "p_party" to e.party, "p_note" to e.note,
                    // F6 (audit 2026-11-14): köhnə kod p_coupon_id-yə KOD
                    // qoyurdu, SQL ULID id gözləyirdi → həmişə INVALID_INPUT.
                    // 0004 hər ikisini qəbul edir; M1 kod, M2 server-ID.
                    "p_coupon_code" to e.couponCode,
                )
            is SyncEvent.ConfirmReservationIntent ->
                rpc("fn_confirm_reservation", "p_qr_code" to e.qrCode)
            is SyncEvent.UseReservationIntent ->
                rpc("fn_use_reservation", "p_qr_code" to e.qrCode)
            is SyncEvent.CancelReservationIntent ->
                rpc("fn_cancel_reservation", "p_id" to e.reservationId)
            is SyncEvent.NoShowIntent ->
                rpc("fn_mark_no_show", "p_id" to e.reservationId)
            is SyncEvent.ClaimShareIntent ->
                rpc("fn_claim_share", "p_share_token" to e.shareToken)
            is SyncEvent.AddReviewIntent ->
                rpc(
                    "fn_add_review", "p_event_id" to e.eventId,
                    "p_rating" to e.rating, "p_body" to e.body,
                )
            is SyncEvent.ReportIntent ->
                rpc(
                    "fn_report", "p_target_type" to e.targetType,
                    "p_target_profile_id" to e.targetId, "p_reason" to e.reason, "p_note" to e.note,
                )
            is SyncEvent.BlockIntent ->
                rpc(
                    if (e.blocked) "fn_block_user" else "fn_unblock_user",
                    "p_target_id" to e.targetId,
                )
            is SyncEvent.MessageIntent ->
                rpc(
                    "fn_send_message", "p_kind" to e.kind,
                    "p_event_id" to e.eventId, "p_recipient_id" to e.personId, "p_body" to e.body,
                )
            is SyncEvent.InviteIntent -> rpc("fn_create_invite")
            is SyncEvent.LocationUpdateIntent ->
                rpc("fn_update_location", "p_lat" to e.lat, "p_lng" to e.lng)
            is SyncEvent.NotifPrefIntent ->
                rpc("fn_set_notif_pref", "p_notif_type" to e.notifType, "p_enabled" to e.enabled)
        }

        private data class RpcCall(override val fn: String, override val params: JsonObject) :
            BackendCall

        private fun rpc(fn: String, vararg pairs: Pair<String, Any?>): RpcCall {
            val obj = buildJsonObject {
                for ((k, v) in pairs) {
                    when (v) {
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
    }
}

/**
 * Fabrika — MainActivity bir sətirlə qurur (D-050/D-078):
 * PLACEHOLDER dəyərlər NoopSync verir, real konfiqurasiya [SupabaseSync].
 */
object BackendSync {
    fun create(scope: CoroutineScope): SyncSink = when (BackendConfig.mode) {
        BackendMode.MOCK -> NoopSync
        BackendMode.SUPABASE -> runCatching { SupabaseSync(scope) }
            .getOrElse { NoopSync }
    }
}
