package az.qoshul.app.core.backend

import az.qoshul.app.core.data.VenueReview
import az.qoshul.app.core.model.AppNotification
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.model.CouponOffer
import az.qoshul.app.core.model.NotifType
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.Reservation
import az.qoshul.app.core.model.ResvStatus
import az.qoshul.app.core.model.UserCoupon
import az.qoshul.app.core.model.VenueXpAccount
import az.qoshul.app.core.model.VenueXpConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * D-151 (2026-11-25): OXU YOLU (M2) — serverdən canlı data.
 *
 * Əvvəl tətbiq YALNIZ yazırdı (SyncEvent → RPC) və başlanğıc data fake idi.
 * İndi girişdən sonra bütün state serverdən oxunur: tədbirlər, məkanlar,
 * təkliflər, iştiraklar, matçlar, bloklar, mesajlar, bildirişlər, XP
 * hesabları, kuponlar, rezervlər. [AppEngine.applyServerData] engine-in
 * lokal state-ini bu məlumatla DOLDURUR — demo data mənbəyi YOXDUR.
 *
 * Oxu RLS ilə qorunur: hər cədvəl yalnız icazə verilən sətirləri qaytarır
 * (məs. başqasının kuponu GÖRÜNMÜR). PostgREST SELECT (RPC deyil) —
 * dəyişən parametrlərə ehtiyac yoxdur.
 *
 * Xəta toleransı: hər blok ayrıca runCatching — bir cədvəl xəta verəndə
 * digərləri yenə də yüklənir (offline təcrübə boş, amma crash yoxdur).
 */
object SupabaseReader {

    /** Serverdən oxunmuş vahid state (AppEngine.applyServerData-a ötürülür). */
    data class ServerData(
        val events: List<QoshulEvent> = emptyList(),
        val people: List<Person> = emptyList(),
        val partners: List<Partner> = emptyList(),
        val offers: List<CouponOffer> = emptyList(),
        val joinedEventIds: Set<String> = emptySet(),
        val pendingEventIds: Set<String> = emptySet(),  // D-169: approval gözləyənlər ayrıca
        val savedEventIds: Set<String> = emptySet(),
        val checkedInEventIds: Set<String> = emptySet(),
        val hostedEventIds: Set<String> = emptySet(),
        val matchIds: Set<String> = emptySet(),
        val friendMessagePeople: Map<String, List<ChatMessage>> = emptyMap(),
        val eventMessages: Map<String, List<ChatMessage>> = emptyMap(),
        val notifications: List<AppNotification> = emptyList(),
        val blockedIds: Set<String> = emptySet(),
        val myProfile: MyProfile? = null,
        /** I.2 (D-170): aktiv XP kartı (server CSPRNG nonce; null = yoxdur). */
        val xpCard: XpCard? = null,
        val venueXp: Map<String, VenueXpAccount> = emptyMap(),
        val coupons: List<UserCoupon> = emptyList(),
        val reservations: List<Reservation> = emptyList(),
        val venueReviews: Map<String, List<VenueReview>> = emptyMap(),
        val venueSlots: Map<String, List<String>> = emptyMap(),
        /** D-176: keçirilmiş kartlar (swipes) serverdən bərpa olunur. */
        val swipedPeople: Map<String, Boolean> = emptyMap(),
        /** D-176: dostluqlar (PENDING sorğu / ACCEPTED) serverdən. */
        val friendships: Map<String, String> = emptyMap(),
        /** D-177: venue rəy statusu — öz PENDING rəyi (dialoq birdəfəlik). */
        val myVenueReviews: Map<String, Boolean> = emptyMap(),
        /**
         * D-179: AI tövsiyə skorları serverdən (fn_recommend_events).
         * NULL = RPC yoxdur/bos → „AI seçimi“ zolağı GİZLƏDİLİR.
         */
        val aiRecommendations: Map<String, Int>? = null,
    )

    /** Mənim profilim (profiles cədvəlim) — D-152: onboarding sahələri + maraqlar. */
    data class MyProfile(
        val id: String,
        val firstName: String,
        val lastName: String,
        val city: String,
        val bio: String,
        val xp: Int,
        val onboarded: Boolean,
        val photoCount: Int,
        val gender: String = "",
        val status: String = "",
        val university: String = "",
        val workField: String = "",
        val languages: List<String> = emptyList(),
        val goals: List<String> = emptyList(),
        val groupSize: String = "",
        val budget: String = "",
        val availability: List<String> = emptyList(),
        val interests: List<String> = emptyList(),
        /** D-176: avatar açarı (photo_urls[0] — brend şəkil seçimi). */
        val avatar: String = "",
    )

    private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /** Tam yükləmə — girişdən/onboarding-dən sonra bir dəfə çağırılır. */
    suspend fun loadAll(now: LocalDateTime = LocalDateTime.now()): ServerData {
        val d = ServerData()
        val events = runCatching { readEvents(now) }.getOrElse { emptyList() }
        val partners = runCatching { readPartners() }.getOrElse { emptyList() }
        val offers = runCatching { readOffers() }.getOrElse { emptyList() }
        val people = runCatching { readPeople() }.getOrElse { emptyList() }
        val myProfile = runCatching { readMyProfile() }.getOrNull()
        val participations = runCatching { readParticipations() }
            .getOrElse { Participation(emptySet(), emptySet(), emptySet(), emptySet()) }
        val hosted = runCatching { readHostedEventIds() }.getOrElse { emptySet() }
        val matches = runCatching { readMatches() }.getOrElse { emptySet() }
        val blocks = runCatching { readBlocks() }.getOrElse { emptySet() }
        val messages = runCatching { readMessages() }
            .getOrElse { emptyMap<String, List<ChatMessage>>() to emptyMap<String, List<ChatMessage>>() }
        val notifications = runCatching { readNotifications() }.getOrElse { emptyList() }
        val venueXp = runCatching { readVenueXp() }.getOrElse { emptyMap() }
        val coupons = runCatching { readCoupons(now) }.getOrElse { emptyList() }
        val reservations = runCatching { readReservations() }.getOrElse { emptyList() }
        val reviews = runCatching { readVenueReviews() }.getOrElse { emptyMap() }
        val slots = runCatching { readVenueSlots() }.getOrElse { emptyMap() }
        val xpCard = runCatching { readActiveXpCard() }.getOrNull()
        // D-176: swipes + dostluqlar serverdən bərpa olunur (restart-da
        // keçilmiş kartlar / dostluqlar İTİRDİ — indi server avtoriteti)
        val swipedPeople = runCatching { readSwipedPeople() }.getOrElse { emptyMap() }
        val friendships = runCatching { readFriendships() }.getOrElse { emptyMap() }
        val myVenueReviews = runCatching { readMyVenueReviews() }.getOrElse { emptyMap() }
        // D-179: AI tövsiyə RPC (yoxdursa null → zolaq gizlədilir)
        val aiRecommendations = runCatching { readAiRecommendations() }.getOrNull()

        return d.copy(
            events = events, partners = partners, offers = offers, people = people,
            myProfile = myProfile,
            joinedEventIds = participations.joined,
            pendingEventIds = participations.pending,
            savedEventIds = participations.saved,
            checkedInEventIds = participations.checkedIn,
            hostedEventIds = hosted,
            matchIds = matches, blockedIds = blocks,
            eventMessages = messages.first, friendMessagePeople = messages.second,
            notifications = notifications, venueXp = venueXp,
            xpCard = xpCard,
            coupons = coupons, reservations = reservations,
            venueReviews = reviews, venueSlots = slots,
            swipedPeople = swipedPeople, friendships = friendships,
            myVenueReviews = myVenueReviews,
            aiRecommendations = aiRecommendations,
        )
    }

    // ── Tədbirlər ──

    private suspend fun readEvents(now: LocalDateTime): List<QoshulEvent> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("events")
            .select(Columns.raw("id,title,category,city,location,location_geo,epoch_day,event_time,event_date,capacity,description,price_azn,age_limit,language,level,format,approval,recurring,tags,image,host_profile_id,venue_id"))
            .decodeAs()
        // host adları ayrıca profillərdən
        val hosts = readPeopleNames()
        return rows.mapNotNull { it.toEvent(now, hosts[it["host_profile_id"]?.jsonPrimitive?.contentOrNull]) }
    }

    private fun JsonObject.toEvent(now: LocalDateTime, hostName: String? = null): QoshulEvent? = runCatching {        val epochDay = this["epoch_day"]?.jsonPrimitive?.longOrNull?.toInt() ?: return null
        val today = now.toLocalDate().toEpochDay().toInt()
        val date = LocalDate.ofEpochDay(epochDay.toLong())
        val dateLabel = when {
            epochDay == today -> "Bu gün"
            epochDay == today + 1 -> "Sabah"
            else -> "${date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("az"))} ${date.dayOfMonth}"
        }
        val geo = this["location_geo"]?.jsonPrimitive?.jsonObject
        val lat = geo?.get("coordinates")?.jsonArray?.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: 0.0
        val lng = geo?.get("coordinates")?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: 0.0
        QoshulEvent(
            id = this["id"]!!.jsonPrimitive.content,
            title = this["title"]?.jsonPrimitive?.content ?: "",
            category = this["category"]?.jsonPrimitive?.content ?: "Digər",
            // D-177 (ikinci dalğa): image fallback "friends" SİLİNDİ — boş
            // açar kateqoriyadan törəyir (imageForCategory; DataSource-da)
            image = this["image"]?.jsonPrimitive?.contentOrNull
                ?: az.qoshul.app.core.data.Taxonomy.imageKeyForCategory(
                    this["category"]?.jsonPrimitive?.contentOrNull ?: "",
                ),
            city = this["city"]?.jsonPrimitive?.content ?: "",
            location = this["location"]?.jsonPrimitive?.content ?: "",
            lat = lat, lng = lng, distance = 0.0,
            time = this["event_time"]?.jsonPrimitive?.content ?: "",
            date = dateLabel,
            epochDay = epochDay,
            past = epochDay < today,
            capacity = this["capacity"]?.jsonPrimitive?.intOrNull ?: 8,
            participants = 0, // iştirakçı sayı event_participants-dan gəlir (applyServerData)
            host = hostName ?: "",
            description = this["description"]?.jsonPrimitive?.content ?: "",
            price = this["price_azn"]?.jsonPrimitive?.intOrNull,
            ageLimit = this["age_limit"]?.jsonPrimitive?.contentOrNull,
            language = this["language"]?.jsonPrimitive?.contentOrNull,
            level = this["level"]?.jsonPrimitive?.contentOrNull,
            format = this["format"]?.jsonPrimitive?.content ?: "Oflayn",
            approval = this["approval"]?.jsonPrimitive?.booleanOrNull ?: false,
            recurring = this["recurring"]?.jsonPrimitive?.content ?: "Bir dəfəlik",
            tags = this["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
            venueId = this["venue_id"]?.jsonPrimitive?.contentOrNull,
        )
    }.getOrNull()


    // ── Məkanlar ──

    private suspend fun readPartners(): List<Partner> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("venues")
            .select(Columns.raw("id,name,category,address,city,deal,hours,image,image_url,price_range,auto_accept,rating,reviews_count,open_from,open_to,discount_percent,location,xp_per_azn,max_discount_percent,min_bill_azn"))
            .decodeAs()
        return rows.mapNotNull { it.toPartner() }
    }

    private fun JsonObject.toPartner(): Partner? = runCatching {
        val geo = this["location"]?.jsonPrimitive?.jsonObject
        val lng = geo?.get("coordinates")?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: 0.0
        val lat = geo?.get("coordinates")?.jsonArray?.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: 0.0
        // D-177: imageUrl = venues.image_url (NULL → kateqoriya ikonlu placeholder)
        val imageUrl = this["image_url"]?.jsonPrimitive?.contentOrNull
        Partner(
            id = this["id"]!!.jsonPrimitive.content,
            name = this["name"]?.jsonPrimitive?.content ?: "",
            category = this["category"]?.jsonPrimitive?.content ?: "",
            address = this["address"]?.jsonPrimitive?.content ?: "",
            city = this["city"]?.jsonPrimitive?.content ?: "",
            deal = this["deal"]?.jsonPrimitive?.contentOrNull,
            hours = this["hours"]?.jsonPrimitive?.content ?: "",
            // D-177: paket açarı boş olanda brendə düşmür — kateqoriya
            // ikonlu placeholder (imageForCategory) UI-da həll olunur
            image = this["image"]?.jsonPrimitive?.content ?: "",
            imageUrl = imageUrl,
            lat = lat, lng = lng,
            priceRange = this["price_range"]?.jsonPrimitive?.content ?: "₼₼",
            autoAccept = this["auto_accept"]?.jsonPrimitive?.booleanOrNull ?: true,
            rating = this["rating"]?.jsonPrimitive?.doubleOrNull ?: 4.5,
            reviewsCount = this["reviews_count"]?.jsonPrimitive?.intOrNull ?: 0,
            openFrom = this["open_from"]?.jsonPrimitive?.intOrNull ?: 9,
            openTo = this["open_to"]?.jsonPrimitive?.intOrNull ?: 22,
            discountPercent = this["discount_percent"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    }.getOrNull()

    // ── Təkliflər ──

    private suspend fun readOffers(): List<CouponOffer> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("coupons_offers")
            .select(Columns.raw("id,venue_id,label,percent,fixed_azn,min_rank_index,xp_price,validity_days,auto_by_level"))
            .decodeAs()
        return rows.mapNotNull { o ->
            runCatching {
                CouponOffer(
                    id = o["id"]!!.jsonPrimitive.content,
                    venueId = o["venue_id"]?.jsonPrimitive?.content ?: "",
                    venueName = "", // AppEngine applyServerData-da partner adı ilə doldurulur
                    label = o["label"]?.jsonPrimitive?.content ?: "",
                    percent = o["percent"]?.jsonPrimitive?.intOrNull ?: 0,
                    fixedAzn = o["fixed_azn"]?.jsonPrimitive?.intOrNull ?: 0,
                    minRankIndex = o["min_rank_index"]?.jsonPrimitive?.intOrNull ?: 0,
                    xpPrice = o["xp_price"]?.jsonPrimitive?.intOrNull ?: 0,
                    validityDays = o["validity_days"]?.jsonPrimitive?.intOrNull ?: 14,
                    autoByLevel = o["auto_by_level"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            }.getOrNull()
        }
    }

    // ── İnsanlar (matç kandidatları + dostlar) ──

    private suspend fun readPeople(): List<Person> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("profile_cards")
            .select(Columns.raw("id,first_name,last_name,city,status,university,work_field,goals,xp,map_lat,map_lng"))
            .decodeAs()
        return rows.mapNotNull { p ->
            runCatching {
                val id = p["id"]!!.jsonPrimitive.content
                // D-180: xəritə pini üçün REAL (yuvarlaqlaşdırılmış ~1.1km)
                // koordinat — 0013 profile_cards.map_lat/map_lng. Yoxdursa /
                // OFF-dursa (0,0) → MapScreen null-island filtri süzür
                // (əvvəl: hamı üçün sabit Bakı default — pinlər üst-üstə).
                val mapLat = p["map_lat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val mapLng = p["map_lng"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                Person(
                    id = id,
                    mapLat = mapLat,
                    mapLng = mapLng,
                    firstName = p["first_name"]?.jsonPrimitive?.content ?: "",
                    lastName = p["last_name"]?.jsonPrimitive?.content ?: "",
                    // D-177 (ikinci dalğa): sabit "friends" fallback SİLİNDİ —
                    // profil şəkli yoxdursa boş açar default avatara düşür
                    // (imagePaint-in else qolu — brend şəkil, kateqoriya deyil)
                    image = "",
                    city = p["city"]?.jsonPrimitive?.content ?: "",
                    status = p["status"]?.jsonPrimitive?.content ?: "other",
                    org = p["university"]?.jsonPrimitive?.contentOrNull
                        ?: p["work_field"]?.jsonPrimitive?.contentOrNull ?: "",
                    interest = p["goals"]?.jsonArray?.joinToString(" · ") { it.jsonPrimitive.content } ?: "",
                    match = 50, // real uyğunluq skoru M3 (matchmaking alqoritmi)
                    reason = "",
                )
            }.getOrNull()
        }
    }

    // ── Mənim profilim ──

    private suspend fun readMyProfile(): MyProfile? {
        val c = SupabaseBackend.client ?: return null
        val rows: List<JsonObject> = c.postgrest.from("profiles")
            .select(Columns.raw("id,first_name,last_name,city,bio,xp,onboarded,photo_count," +
                "gender,status,university,work_field,languages,goals,group_size,budget,availability,photo_urls"))
            .decodeAs()
        val p = rows.firstOrNull() ?: return null
        // D-152: maraqlar (user_interests) ayrı cədvəldən oxunur
        val interests: List<String> = runCatching {
            val irows: List<JsonObject> = c.postgrest.from("user_interests")
                .select(Columns.raw("category_slug")).decodeAs()
            irows.mapNotNull { it["category_slug"]?.jsonPrimitive?.contentOrNull }
        }.getOrElse { emptyList() }
        // D-176: avatar açarı photo_urls[0]-dan (fn_set_avatar oraya yazır)
        val avatar = p["photo_urls"]?.jsonArray
            ?.firstOrNull()?.jsonPrimitive?.contentOrNull ?: ""
        return runCatching {
            MyProfile(
                id = p["id"]!!.jsonPrimitive.content,
                firstName = p["first_name"]?.jsonPrimitive?.content ?: "",
                lastName = p["last_name"]?.jsonPrimitive?.content ?: "",
                city = p["city"]?.jsonPrimitive?.content ?: "",
                bio = p["bio"]?.jsonPrimitive?.content ?: "",
                xp = p["xp"]?.jsonPrimitive?.intOrNull ?: 0,
                onboarded = p["onboarded"]?.jsonPrimitive?.booleanOrNull ?: false,
                photoCount = p["photo_count"]?.jsonPrimitive?.intOrNull ?: 0,
                gender = p["gender"]?.jsonPrimitive?.contentOrNull ?: "",
                status = p["status"]?.jsonPrimitive?.contentOrNull ?: "",
                university = p["university"]?.jsonPrimitive?.contentOrNull ?: "",
                workField = p["work_field"]?.jsonPrimitive?.contentOrNull ?: "",
                languages = p["languages"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                goals = p["goals"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                groupSize = p["group_size"]?.jsonPrimitive?.contentOrNull ?: "",
                budget = p["budget"]?.jsonPrimitive?.contentOrNull ?: "",
                availability = p["availability"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                interests = interests,
                avatar = avatar,
            )
        }.getOrNull()
    }

    /** I.2 (D-170): serverdən gələn aktiv XP kartı. */
    data class XpCard(
        val nonce: String, val fallbackCode: String,
        val venueId: String, val expiresAtIso: String,
    )

    private suspend fun readActiveXpCard(): XpCard? {
        val c = SupabaseBackend.client ?: return null
        return runCatching {
            val rows: List<JsonObject> = c.postgrest.from("xp_card_nonces")
                .select(Columns.raw("nonce,fallback_code,venue_id,expires_at")) {
                    filter {
                        eq("used_at", "null")
                        gt("expires_at", "now()")
                    }
                    order("issued_at", Order.DESCENDING)
                    limit(1)
                }.decodeAs()
            rows.firstOrNull()?.let {
                XpCard(
                    nonce = it["nonce"]?.jsonPrimitive?.contentOrNull ?: return null,
                    fallbackCode = it["fallback_code"]?.jsonPrimitive?.contentOrNull ?: "",
                    venueId = it["venue_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    expiresAtIso = it["expires_at"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            }
        }.getOrNull()
    }

    // ── İştiraklarım ──

    /** D-169: iştirak vəziyyəti — JOINED/PENDING/checkedIn/saved birlikdə. */
    data class Participation(
        val joined: Set<String>, val pending: Set<String>,
        val checkedIn: Set<String>, val saved: Set<String>,
    )

    private suspend fun readParticipations(): Participation {
        val c = SupabaseBackend.client ?: return Participation(emptySet(), emptySet(), emptySet(), emptySet())
        val rows: List<JsonObject> = c.postgrest.from("event_participants")
            .select(Columns.raw("event_id,status,checked_in_at"))
            .decodeAs()
        val joined = mutableSetOf<String>()
        val pending = mutableSetOf<String>()
        val checkedIn = mutableSetOf<String>()
        for (r in rows) {
            val ev = r["event_id"]?.jsonPrimitive?.contentOrNull ?: continue
            when (r["status"]?.jsonPrimitive?.contentOrNull) {
                "JOINED" -> joined.add(ev)
                "PENDING" -> pending.add(ev)  // D-169: approval gözləyənlər ayrıca
                // LEFT iştirak siyahısına düşmür (tarixçə yalnız serverdə)
            }
            if (r["checked_in_at"]?.jsonPrimitive?.contentOrNull != null) checkedIn.add(ev)
        }
        // D-169: saxlanılanlar ayrı cədvəldən (0009) — köhnə "SAVED" status
        // heç vaxt yazılmırdı; saved_events RLS ilə yalnız sahibi oxuyur.
        // (Ayrı suspend funksiya — runCatching+decodeAs inline tip çıxarışı
        // K2-də pozulurdu: "Cannot infer type for R")
        val saved = readSavedEventIds()
        return Participation(joined, pending, checkedIn, saved)
    }

    /** D-169: saved_events cədvərindən istifadəçinin saxladığı tədbirlər. */
    private suspend fun readSavedEventIds(): Set<String> {
        val c = SupabaseBackend.client ?: return emptySet()
        return runCatching {
            val rows: List<JsonObject> = c.postgrest.from("saved_events")
                .select(Columns.raw("event_id")).decodeAs()
            rows.mapNotNull { it["event_id"]?.jsonPrimitive?.contentOrNull }.toSet()
        }.getOrDefault(emptySet())
    }

    private suspend fun readHostedEventIds(): Set<String> {
        val c = SupabaseBackend.client ?: return emptySet()
        val rows: List<JsonObject> = c.postgrest.from("events")
            .select(Columns.raw("id")) {
                filter { eq("host_profile_id", "me()") }
            }.decodeAs()
        return rows.mapNotNull { it["id"]?.jsonPrimitive?.contentOrNull }.toSet()
    }

    // ── Matçlarım ──

    private suspend fun readMatches(): Set<String> {
        val c = SupabaseBackend.client ?: return emptySet()
        val rows: List<JsonObject> = c.postgrest.from("matches")
            .select(Columns.raw("a_id,b_id")).decodeAs()
        val me = readMyProfile()?.id
        return rows.mapNotNull { m ->
            val a = m["a_id"]?.jsonPrimitive?.contentOrNull
            val b = m["b_id"]?.jsonPrimitive?.contentOrNull
            when (me) {
                a -> b; b -> a; else -> null
            }
        }.toSet()
    }

    // ── Bloklarımm ──

    private suspend fun readBlocks(): Set<String> {
        val c = SupabaseBackend.client ?: return emptySet()
        val rows: List<JsonObject> = c.postgrest.from("blocks")
            .select(Columns.raw("blocked_id")).decodeAs()
        return rows.mapNotNull { it["blocked_id"]?.jsonPrimitive?.contentOrNull }.toSet()
    }

    // ── Mesajlarım (hadisə + şəxsi) ──

    private suspend fun readMessages(): Pair<Map<String, List<ChatMessage>>, Map<String, List<ChatMessage>>> {
        val c = SupabaseBackend.client ?: return emptyMap<String, List<ChatMessage>>() to emptyMap<String, List<ChatMessage>>()
        val me = readMyProfile() ?: return emptyMap<String, List<ChatMessage>>() to emptyMap<String, List<ChatMessage>>()
        senderNames = readPeopleNames()
        val rows: List<JsonObject> = c.postgrest.from("messages")
            .select(Columns.raw("id,kind,event_id,sender_id,recipient_id,body,created_at")) {
                order("created_at", Order.ASCENDING)
            }
            .decodeAs()
        val eventMsgs = mutableMapOf<String, MutableList<ChatMessage>>()
        val friendMsgs = mutableMapOf<String, MutableList<ChatMessage>>()
        for (r in rows) {
            val body = r["body"]?.jsonPrimitive?.contentOrNull ?: continue
            val senderId = r["sender_id"]?.jsonPrimitive?.contentOrNull
            val senderName = senderNames[senderId] ?: "İstifadəçi"
            val mine = senderId == me.id
            val kind = r["kind"]?.jsonPrimitive?.contentOrNull ?: continue
            if (kind == "EVENT") {
                val ev = r["event_id"]?.jsonPrimitive?.contentOrNull ?: continue
                eventMsgs.getOrPut(ev) { mutableListOf() }.add(ChatMessage(senderName, body, mine))
            } else {
                val other = if (mine) r["recipient_id"]?.jsonPrimitive?.contentOrNull ?: continue
                else senderId ?: continue
                friendMsgs.getOrPut(other) { mutableListOf() }.add(ChatMessage(senderName, body, mine))
            }
        }
        return eventMsgs to friendMsgs
    }

    /** Sender adları — mesaj oxunan zaman doldurulur (bir sorğu). */
    private var senderNames: Map<String, String> = emptyMap()

    private suspend fun readPeopleNames(): Map<String, String> {
        val c = SupabaseBackend.client ?: return emptyMap()
        val rows: List<JsonObject> = c.postgrest.from("profiles")
            .select(Columns.raw("id,first_name,last_name")).decodeAs()
        return rows.mapNotNull { p ->
            val id = p["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val fn = p["first_name"]?.jsonPrimitive?.contentOrNull ?: ""
            val ln = p["last_name"]?.jsonPrimitive?.contentOrNull ?: ""
            id to "$fn $ln".trim()
        }.toMap()
    }

    // ── Bildirişlər ──

    private suspend fun readNotifications(): List<AppNotification> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("notifications")
            .select(Columns.raw("id,notif_type,title,body,read,created_at")) {
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeAs()
        return rows.mapNotNull { n ->
            runCatching {
                AppNotification(
                    id = n["id"]!!.jsonPrimitive.content,
                    type = runCatching {
                        NotifType.valueOf(n["notif_type"]?.jsonPrimitive?.content ?: "MARKETING")
                    }.getOrElse { NotifType.MARKETING },
                    title = n["title"]?.jsonPrimitive?.content ?: "",
                    body = n["body"]?.jsonPrimitive?.content ?: "",
                    at = LocalDateTime.parse(n["created_at"]?.jsonPrimitive?.content?.substringBefore("+") ?: "", DATE_FMT),
                    read = n["read"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            }.getOrNull()
        }
    }

    // ── Venue XP hesablarım ──

    private suspend fun readVenueXp(): Map<String, VenueXpAccount> {
        val c = SupabaseBackend.client ?: return emptyMap()
        val rows: List<JsonObject> = c.postgrest.from("venue_xp_accounts")
            .select(Columns.raw("venue_id,balance")).decodeAs()
        // konfiqurasiya (xp_per_azn və s.) venues cədvəlindən gəlir
        val venues = runCatching { readPartners() }.getOrElse { emptyList() }
        return rows.mapNotNull { r ->
            val vid = r["venue_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val balance = r["balance"]?.jsonPrimitive?.intOrNull ?: 0
            val v = venues.firstOrNull { it.id == vid }
            vid to VenueXpAccount(
                venueId = vid,
                venueName = v?.name ?: vid,
                balance = balance,
                config = VenueXpConfig(
                    xpPerAzn = 20, maxDiscountPercent = 20, minBillAzn = 0,
                ),
            )
        }.toMap()
    }

    // ── Kuponlarım ──

    private suspend fun readCoupons(now: LocalDateTime): List<UserCoupon> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("user_coupons")
            .select(Columns.raw("id,offer_id,code,venue_id,venue_name,label,status,expires_at,purchased_at,used_at"))
            .decodeAs()
        return rows.mapNotNull { u ->
            runCatching {
                UserCoupon(
                    id = u["id"]!!.jsonPrimitive.content,
                    offerId = u["offer_id"]?.jsonPrimitive?.content ?: "",
                    code = u["code"]?.jsonPrimitive?.content ?: "",
                    venueId = u["venue_id"]?.jsonPrimitive?.content ?: "",
                    venueName = u["venue_name"]?.jsonPrimitive?.content ?: "",
                    label = u["label"]?.jsonPrimitive?.content ?: "",
                    expiresAt = LocalDateTime.parse(
                        u["expires_at"]?.jsonPrimitive?.content?.substringBefore("+") ?: "", DATE_FMT),
                    purchasedAt = now,
                    used = u["status"]?.jsonPrimitive?.content == "USED",
                )
            }.getOrNull()
        }
    }

    // ── Rezervlərim ──

    private suspend fun readReservations(): List<Reservation> {
        val c = SupabaseBackend.client ?: return emptyList()
        val rows: List<JsonObject> = c.postgrest.from("reservations")
            .select(Columns.raw("id,venue_id,venue_name,venue_image,resv_date,resv_time,party,note,qr_code,status,created_at")) {
                order("created_at", Order.DESCENDING)
            }
            .decodeAs()
        return rows.mapNotNull { r ->
            runCatching {
                Reservation(
                    id = r["id"]!!.jsonPrimitive.content,
                    venueId = r["venue_id"]?.jsonPrimitive?.content ?: "",
                    venueName = r["venue_name"]?.jsonPrimitive?.content ?: "",
                    venueImage = r["venue_image"]?.jsonPrimitive?.content ?: "",
                    date = r["resv_date"]?.jsonPrimitive?.content ?: "",
                    time = r["resv_time"]?.jsonPrimitive?.content ?: "",
                    party = r["party"]?.jsonPrimitive?.intOrNull ?: 1,
                    note = r["note"]?.jsonPrimitive?.content ?: "",
                    couponCode = null,
                    qrCode = r["qr_code"]?.jsonPrimitive?.content ?: "",
                    createdAt = LocalDateTime.now(),
                    status = runCatching {
                        ResvStatus.valueOf(r["status"]?.jsonPrimitive?.content ?: "PENDING")
                    }.getOrElse { ResvStatus.PENDING },
                )
            }.getOrNull()
        }
    }

    // ── Məkan rəyləri ──

    private suspend fun readVenueReviews(): Map<String, List<VenueReview>> {
        val c = SupabaseBackend.client ?: return emptyMap()
        val rows: List<JsonObject> = c.postgrest.from("venue_reviews")
            .select(Columns.raw("venue_id,author_name,stars,body,status"))
            .decodeAs()
        return rows.filter { it["status"]?.jsonPrimitive?.contentOrNull == "APPROVED" }
            .groupBy(
                { it["venue_id"]?.jsonPrimitive?.contentOrNull ?: "" },
                {
                    VenueReview(
                        author = it["author_name"]?.jsonPrimitive?.contentOrNull ?: "",
                        stars = it["stars"]?.jsonPrimitive?.intOrNull ?: 5,
                        text = it["body"]?.jsonPrimitive?.contentOrNull ?: "",
                    )
                },
            )
    }

    // ── Rezerv slotları ──

    private suspend fun readVenueSlots(): Map<String, List<String>> {
        val c = SupabaseBackend.client ?: return emptyMap()
        val rows: List<JsonObject> = c.postgrest.from("venue_slots")
            .select(Columns.raw("venue_id,slot_time,capacity"))
            .decodeAs()
        return rows.groupBy(
            { it["venue_id"]?.jsonPrimitive?.contentOrNull ?: "" },
            { it["slot_time"]?.jsonPrimitive?.contentOrNull ?: "" },
        ).mapValues { it.value.sorted() }
    }

    // ── D-176: keçirilmiş kartlar (swipes) — restart-da itmir ──

    /**
     * Mənim swipe hərəkətlərim (fn_read_swipe RPC — həm LIKE, həm PASS).
     * Qaytarılan map: personId → interested. Boş = heç bir kart keçilməyib.
     */
    private suspend fun readSwipedPeople(): Map<String, Boolean> {
        val c = SupabaseBackend.client ?: return emptyMap()
        return runCatching {
            val rows: List<JsonObject> = c.postgrest.rpc(
                function = "fn_read_swipes",
            ).decodeAs()
            rows.mapNotNull { r ->
                val id = r["person_id"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                val interested = r["interested"]?.jsonPrimitive?.booleanOrNull ?: false
                id to interested
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    // ── D-176: dostluqlar (friendships) — serverdən bərpa ──

    /**
     * Dostluq vəziyyətləri: personId → status („sent“ = mən sorğu göndərmişəm
     * PENDING; „accepted“ = qəbul olunub). DECLINED göstərilmir (sanki yoxdur).
     */
    private suspend fun readFriendships(): Map<String, String> {
        val c = SupabaseBackend.client ?: return emptyMap()
        return runCatching {
            val rows: List<JsonObject> = c.postgrest.from("friendships")
                .select(Columns.raw("requester_id,addressee_id,status")).decodeAs()
            val me = readMyProfile()?.id
            if (me == null) return@runCatching emptyMap()
            rows.mapNotNull { f ->
                val requester = f["requester_id"]?.jsonPrimitive?.contentOrNull
                val addressee = f["addressee_id"]?.jsonPrimitive?.contentOrNull
                val status = f["status"]?.jsonPrimitive?.contentOrNull
                when {
                    requester == me && status == "PENDING" ->
                        (addressee ?: return@mapNotNull null) to "sent"
                    requester == me && status == "ACCEPTED" ->
                        (addressee ?: return@mapNotNull null) to "accepted"
                    addressee == me && status == "ACCEPTED" ->
                        (requester ?: return@mapNotNull null) to "accepted"
                    addressee == me && status == "PENDING" ->
                        // mənə gələn sorğu — „incoming“ (UI cavab düymələri göstərir)
                        (requester ?: return@mapNotNull null) to "incoming"
                    else -> null
                }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    // ── D-178: öz məkan rəylərim (bir məkana bir rəy) ──

    /** venueId → rəy yazıbmı (istənilən statusda). Dialoq birdəfəlik açılır. */
    private suspend fun readMyVenueReviews(): Map<String, Boolean> {
        val c = SupabaseBackend.client ?: return emptyMap()
        return runCatching {
            val rows: List<JsonObject> = c.postgrest.from("venue_reviews")
                .select(Columns.raw("venue_id")).decodeAs()
            rows.mapNotNull { it["venue_id"]?.jsonPrimitive?.contentOrNull }
                .associateWith { true }
        }.getOrDefault(emptyMap())
    }

    // ── D-179: AI tövsiyə skorları — fn_recommend_events RPC ──

    /**
     * Server skorları: eventId → score (2 = maraq uyğunluğu, 1 = yer qalığı).
     * RPC mövcud deyilsə və ya xəta verərsə NULL qayıdır → „AI seçimi“
     * zolağı GİZLƏDİLİR (klient tərəfli fallback skor YOXDUR).
     */
    private suspend fun readAiRecommendations(): Map<String, Int>? {
        val c = SupabaseBackend.client ?: return null
        return runCatching {
            val rows: List<JsonObject> = c.postgrest.rpc(
                function = "fn_recommend_events",
            ).decodeAs()
            rows.mapNotNull { r ->
                val id = r["event_id"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                val score = r["score"]?.jsonPrimitive?.intOrNull ?: 0
                id to score
            }.toMap()
        }.getOrNull()
    }
}
