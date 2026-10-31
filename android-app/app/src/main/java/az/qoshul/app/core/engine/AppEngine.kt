package az.qoshul.app.core.engine

import az.qoshul.app.core.data.AppConfig
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.data.QoshulDataSource
import az.qoshul.app.core.data.dateLabelFor
import az.qoshul.app.core.model.ActivityXpResult
import az.qoshul.app.core.model.AppNotification
import az.qoshul.app.core.model.AssistantParse
import az.qoshul.app.core.model.BadgeDef
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.model.CouponOffer
import az.qoshul.app.core.model.EventProposal
import az.qoshul.app.core.model.LocalDate
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.model.NotifType
import az.qoshul.app.core.model.NotificationPrefs
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.model.PersonalityResult
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.ReportEntry
import az.qoshul.app.core.model.ResvStatus
import az.qoshul.app.core.model.Reservation
import az.qoshul.app.core.model.SpendPreview
import az.qoshul.app.core.model.UserCoupon
import az.qoshul.app.core.model.VenueXpAccount
import az.qoshul.app.core.model.XpCardCode
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.model.badgeDefs
import az.qoshul.app.core.model.isQuietHours
import az.qoshul.app.core.safety.Safety
import az.qoshul.app.core.model.parseAssistantEvent
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tətbiq mühərriki — Flutter `core/state/app_state.dart` (1009 sətir) portu.
 *
 * Arxitektura (D-036):
 *  - MÜHƏRRİK (bu sinif) saf Kotlin-dir — Android asılılığı YOX; JVM testləri
 *    ilə Flutter testlərinin əhatəsi birebir təkrarlanır (Phase 7).
 *  - UI, [state] axınından (StateFlow) oxuyur — hər mutasiya yeni immutabl
 *    snapshot yayır (`version` artır → data-class equals həmişə false →
 *    recompose; Flutter ChangeNotifier semantikası ilə eyni).
 *  - Vaxt əvəzləməsi: [clock] injeksiyası — testlərdə vaxt idarə olunur
 *    (Flutter-də DateTime.now hardcode idi; burada yaxşılaşdırma).
 *  - A2 düzəlişi (audit): həftəlik rəy limiti indi həqiqətən həftədə sıfırlanır.
 *  - A10 düzəlişi: mock cavablar Job ləğvi ilə təhlükəsizdir.
 *
 * Bütün məntiq qaydaları ROL (docs/product-spec.md) + D-026 ilə eynidir.
 */
class AppEngine(
    val dataSource: QoshulDataSource = FakeQoshulDataSource,
    private val scope: CoroutineScope? = null,
    private val clock: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private fun now(): LocalDateTime = clock()

    // ═══════════════ Daxili dəyişən hal (Flutter field-ləri) ═══════════════

    // — auth/onboarding —
    private var signedInV = false
    private var onboardedV = false
    private var profileNameV = "Dost"
    private var firstNameV = "Dost"
    private var lastNameV = ""
    private var cityV = "Bakı"
    private var statusV = "student"
    private var universityV = ""
    private var workFieldV = ""
    private var languagesV = listOf("Azərbaycan dili")
    private var interestsV = listOf<String>()
    private var goalsV = listOf<String>()
    private var personalityV: PersonalityResult? = null
    private var availabilityV = listOf<String>()
    private var groupSizeV = "Kiçik qrup (3–6)"
    private var budgetV = "10 AZN-ə qədər"

    // — ROL B: profil yaradılması —
    private var birthDateV: LocalDate? = null
    private var genderV = ""
    private var bioV = ""
    private var photoCountV = 1
    private var phoneVerifiedV = false
    private var profileCompletedV = false

    // — ROL TAB 5 + D-027: məxfilik —
    private var mapVisibilityV = MapVisibility.EVERYONE
    private var showDistanceV = true

    // — XP 1: Peşəkarlıq (xərclənmir) —
    private var xpV = 320
    private val xpEventIds = mutableSetOf<String>()
    private val earnedBadgesV = mutableSetOf<String>()
    private var matchXpToday = 0
    private var matchXpDay: LocalDate = LocalDate.now()
    private var reviewXpThisWeek = 0
    private var reviewXpWeekKey = weekKey(LocalDate.now())
    private var loginStreakV = 1
    private var dailyLoginDone = false

    // — XP 2: Endirim (obyektə-məxsus, D-026) —
    val venueXp: Map<String, VenueXpAccount> =
        dataSource.initialVenueXp()
    private val finishedActivitiesV = mutableSetOf<String>()
    private var xpCardV: XpCardCode? = null
    private var xpCardVenueIdV: String? = null

    // — tədbirlər —
    private val eventsV = dataSource.events.toMutableList()
    private val joinedV = mutableSetOf("e0")
    private val pendingV = mutableSetOf<String>()
    private val savedV = mutableSetOf<String>()
    private val checkedInV = mutableSetOf<String>()

    // — insanlar / dostluqlar —
    private val friendshipsV = mutableMapOf("p2" to "accepted")

    // — ROL D: match —
    private val swipesV = mutableMapOf<String, Boolean>()
    private val matchesV = mutableSetOf<String>()

    // — ROL E: rezervlər —
    private val reservationsV = mutableListOf<Reservation>()
    private var noShowCountV = 0
    private var restrictedUntilV: LocalDateTime? = null

    // — ROL G: kuponlar —
    private val offersV = dataSource.offers.toMutableList()
    private val couponsV = mutableListOf<UserCoupon>()
    private var couponSeq = 1

    // — ROL H: bildirişlər —
    private val notificationsV = dataSource.initialNotifications.toMutableList()
    val notifPrefs = NotificationPrefs()

    // — ROL I: moderasiya —
    private val blockedV = mutableSetOf<String>()
    private val reportCountsV = mutableMapOf<String, Int>()
    private val restrictedUsersV = mutableSetOf<String>()
    private val reportsV = mutableListOf<ReportEntry>()

    // — söhbətlər —
    private val eventMessagesV =
        dataSource.initialEventMessages.mapValues { it.value.toMutableList() }.toMutableMap()
    private val friendMessagesV =
        dataSource.initialFriendMessages.mapValues { it.value.toMutableList() }.toMutableMap()

    // — assistant —
    private val assistantMsgsV = mutableListOf<ChatMessage>()
    private var assistantBusyV = false
    private var assistantJob: Job? = null
    private val replyJobs = mutableListOf<Job>()

    private var ledgerSeq = 100
    private var version = 0L

    // ═════════════════════════ State axını ═════════════════════════

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(buildState())
    val state: kotlinx.coroutines.flow.StateFlow<AppUiState> get() = _state

    private fun buildState() = AppUiState(
        version = ++version,
        signedIn = signedInV, onboarded = onboardedV,
        profileName = profileNameV, firstName = firstNameV, lastName = lastNameV,
        city = cityV, status = statusV, university = universityV, workField = workFieldV,
        languages = languagesV, interests = interestsV, goals = goalsV,
        personality = personalityV, availability = availabilityV,
        groupSize = groupSizeV, budget = budgetV,
        birthDate = birthDateV, gender = genderV, bio = bioV,
        photoCount = photoCountV, phoneVerified = phoneVerifiedV,
        profileCompleted = profileCompletedV,
        mapVisibility = mapVisibilityV, showDistance = showDistanceV,
        xp = xpV, earnedBadges = earnedBadgesV.toSet(), loginStreak = loginStreakV,
        venueXp = venueXp, discountXpTotal = venueXp.values.sumOf { it.balance },
        xpCard = xpCardV, xpCardVenueId = xpCardVenueIdV,
        events = eventsV.toList(), joined = joinedV.toSet(), pending = pendingV.toSet(),
        saved = savedV.toSet(), checkedIn = checkedInV.toSet(),
        friendships = friendshipsV.toMap(), swipes = swipesV.toMap(),
        matches = matchesV.toSet(),
        reservations = reservationsV.toList(), noShowCount = noShowCountV,
        restrictedUntil = restrictedUntilV,
        offers = offersV.toList(), coupons = couponsV.toList(),
        notifications = notificationsV.toList(),
        blocked = blockedV.toSet(), restrictedUsers = restrictedUsersV.toSet(),
        eventMessages = eventMessagesV.mapValues { it.value.toList() },
        friendMessages = friendMessagesV.mapValues { it.value.toList() },
        assistantMsgs = assistantMsgsV.toList(), assistantBusy = assistantBusyV,
        finishedActivities = finishedActivitiesV.toSet(),
    )

    private fun emit() {
        _state.value = buildState()
    }

    private fun weekKey(d: LocalDate): String = "${d.year}-W${d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}"

    // ═════════════════════════ Köməkçi getterlər ═════════════════════════

    val upcoming: List<QoshulEvent> get() = eventsV.filter { !it.past }
    val cityEvents: List<QoshulEvent> get() = upcoming.filter { it.city == cityV }
    /** D-054: bugünkü tədbirlər — ana ekranda "Aktiv tədbirlər" zolağı üçün. */
    val activeEvents: List<QoshulEvent>
        get() = cityEvents.filter { it.epochDay == LocalDate.now().toEpochDay().toInt() }
    val rank: String get() = XpRules.rankForXp(xpV)
    val rankIndex: Int get() = XpRules.rankIndexForXp(xpV)
    val xpProgress: XpRules.XpProgress get() = XpRules.xpProgress(xpV)
    val discountXpTotal: Int get() = venueXp.values.sumOf { it.balance }

    fun eventById(id: String): QoshulEvent? = eventsV.firstOrNull { it.id == id }
    fun venueXpFor(venueId: String): VenueXpAccount? = venueXp[venueId]
    fun personById(id: String): Person? = dataSource.people.firstOrNull { it.id == id }

    val joinedUpcomingCount: Int
        get() = joinedV.count { id -> eventById(id)?.past == false }

    val unreadCount: Int get() = notificationsV.count { !it.read }

    // ═════════════════════════ XP mühərriki ═════════════════════════

    /** İdemponent Peşəkarlıq XP mükafatı — eyni açar ikinci dəfə sayılmır. */
    private fun award(key: String, amount: Int): Boolean {
        if (xpEventIds.contains(key)) return false
        xpEventIds.add(key)
        val beforeRank = rankIndex
        xpV += amount
        maybeLevelUp(beforeRank)
        return true
    }

    private fun maybeLevelUp(beforeIndex: Int) {
        val after = rankIndex
        if (after > beforeIndex) {
            notify(
                NotifType.LEVEL_UP,
                "Yeni səviyyə: ${XpRules.rankNames[after]}",
                "Təbriklər! Səviyyə artımı yeni endirim imkanları açır.",
            )
        }
    }

    /** ROL B Addım 6: qeydiyyatı tamamlayana +50 XP (bir dəfə). */
    fun completeRegistration() {
        award("registration", XpRules.REGISTRATION_XP)
    }

    /** ROL F: profil 100% → +30 (bir dəfə). */
    fun completeProfileFull() {
        if (photoCountV >= 1 && bioV.isNotBlank() && interestsV.size >= XpRules.MIN_INTERESTS) {
            if (award("profile-full", XpRules.PROFILE_COMPLETE_XP)) {
                profileCompletedV = true
            }
        }
        emit()
    }

    /**
     * D-056: şəxsiyyət testi nəticəsi (engine-də setter yox idi — test
     * UI-ı heç vaxt personality-ni doldura bilmirdi, TraitBars boş qalırdı).
     * Nəticə profil tamamlanması (+30) şərtlərini də yoxlayır.
     */
    fun setPersonality(result: PersonalityResult) {
        personalityV = result
        completeProfileFull()
    }

    /** D-056: dost dəvəti → +100 XP (bir dəfə, XpRules.INVITE_XP əvvələr ölü idi). */
    fun inviteFriend(): Boolean {
        val awarded = award("invite", XpRules.INVITE_XP)
        if (awarded) {
            notify(
                NotifType.LEVEL_UP,
                "+${XpRules.INVITE_XP} XP qazandın",
                "Dost dəvəti göndərildi — dostun qoşulanda tədbirləri birlikdə planlayın!",
            )
            emit()
        }
        return awarded
    }

    /** ROL B: telefon təsdiqi → badge + +40 XP (bir dəfə). */
    fun verifyPhone() {
        if (phoneVerifiedV) return
        phoneVerifiedV = true
        earnedBadgesV.add("verified")
        award("phone-verify", XpRules.PHONE_VERIFY_XP)
        emit()
    }

    /** ROL F: günlük giriş +5; 7 günlük seriya bonusu +30. */
    fun dailyLogin(today: LocalDate = LocalDate.now()) {
        if (dailyLoginDone) return
        dailyLoginDone = true
        award("login-${today}", XpRules.DAILY_LOGIN_XP)
        if (loginStreakV == 7) {
            award("streak-7-${today}", XpRules.STREAK_BONUS_XP)
            loginStreakV = 1
        } else {
            loginStreakV++
        }
        emit()
    }

    // ═════════════════════════ Auth ═════════════════════════

    fun signIn() {
        signedInV = true
        emit()
    }

    /**
     * PLACEHOLDER rejimi (D-050): vahid hardcoded demo hesabı — bir toxunuşla
     * dolu profil. Onboarding keçilir (qeydiyyat XP + demo host adoptu + günlük
     * giriş), bütün funksiyalar dərhal görünür. Yalnız PLACEHOLDER rejimində
     * çağırılmalı (AuthScreen qapısı təmin edir).
     */
    fun signInDemo() {
        val demo = AppConfig.DemoAccount
        firstNameV = demo.FIRST_NAME
        lastNameV = demo.LAST_NAME
        profileNameV = demo.FULL_NAME
        cityV = demo.CITY
        bioV = demo.BIO
        statusV = "student"
        universityV = "Bakı Dövlət Universiteti"
        languagesV = listOf("Azərbaycan dili", "İngilis dili")
        interestsV = listOf("Qəhvə", "Yürüş", "Stolüstü oyunlar", "Foto-gəzinti")
        goalsV = listOf("Yeni tanışlıqlar", "Hobbi paylaşmaq")
        groupSizeV = "Kiçik qrup (3–6)"
        budgetV = "10 AZN-ə qədər"
        birthDateV = java.time.LocalDate.of(2000, 1, 1)
        photoCountV = 1
        signedInV = true
        if (!onboardedV) finishOnboarding()
        emit()
    }

    fun signOut() {
        signedInV = false
        emit()
    }

    // ═════════════════════════ Onboarding (ROL B) ═════════════════════════

    fun setProfileCreation(
        first: String, last: String, birth: LocalDate,
        gdr: String = "", bioText: String = "", photos: Int = 1,
    ) {
        firstNameV = first
        lastNameV = last
        profileNameV = "$first $last".trim()
        birthDateV = birth
        genderV = gdr
        bioV = bioText
        photoCountV = photos
        emit()
    }

    fun setInterests(intr: List<String>) {
        interestsV = intr
        emit()
    }

    /** ROL B axınının sonu: +50 XP, demo host adoptu, günlük giriş, onboarded. */
    fun finishOnboarding(today: LocalDate = LocalDate.now()) {
        completeRegistration()
        onboardedV = true
        adoptDemoHostedEvent()
        dailyLogin(today)
        emit()
    }

    /** D-026 (L.3): demo "Mafiya oyunu gecəsi" host-u real profil adı ilə. */
    private fun adoptDemoHostedEvent() {
        val i = eventsV.indexOfFirst { it.id == "e-mafiya" }
        if (i >= 0) eventsV[i] = eventsV[i].copyWithHost(profileNameV)
    }

    // ═════════════════════════ Profil ═════════════════════════

    fun setProfileName(name: String) {
        profileNameV = name
        val parts = name.trim().split(Regex("\\s+"))
        firstNameV = if (parts.isNotEmpty()) parts.first() else name
        lastNameV = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""
        adoptDemoHostedEvent()
        emit()
    }

    fun setCity(name: String) {
        cityV = name
        emit()
    }

    fun setMapVisibility(v: MapVisibility) {
        mapVisibilityV = v
        emit()
    }

    fun setShowDistance(v: Boolean) {
        showDistanceV = v
        emit()
    }

    // ═════════════════════════ Tədbir əməliyyatları (ROL TAB 3) ═════════════════════════

    fun join(id: String): Boolean {
        val e = eventById(id) ?: return false
        if (e.past || e.isFull || joinedV.contains(id)) return false
        if (e.approval) {
            pendingV.add(id)
        } else {
            joinedV.add(id)
            award("join-$id", XpRules.JOIN_XP)
        }
        emit()
        return true
    }

    fun leave(id: String) {
        joinedV.remove(id)
        emit()
    }

    fun toggleSave(id: String) {
        if (!savedV.remove(id)) savedV.add(id)
        emit()
    }

    /** ROL TAB 3: məkanda QR/yerləşmə ilə check-in → +40 XP + nişanlar. */
    fun checkInEvent(id: String, at: LocalDateTime = now()): Boolean {
        val e = eventById(id) ?: return false
        if (!joinedV.contains(id) || checkedInV.contains(id)) return false
        checkedInV.add(id)
        award("checkin-$id", XpRules.CHECK_IN_XP)
        // Nişanlar (ROL F)
        if (!earnedBadgesV.contains("firstEvent")) {
            earnedBadgesV.add("firstEvent")
        } else if (checkedInV.size >= 10) {
            earnedBadgesV.add("tenEvents")
        }
        if (e.city != cityV) earnedBadgesV.add("traveler")
        if (at.hour >= 20) earnedBadgesV.add("nightOwl")
        // ROL F: öz tədbirimdə ≥3 iştirakçı gəlibsə hosting XP
        if (e.host == profileNameV && e.participants >= XpRules.HOSTING_MIN_PARTICIPANTS) {
            award("hosting-$id", XpRules.HOSTING_XP)
        }
        emit()
        return true
    }

    fun createEvent(
        title: String, category: String, city: String, location: String,
        date: String, time: String, capacity: Int, description: String,
        epochDay: Int, price: Int = 0, ageLimit: String = "Hamı üçün",
        language: String = "Azərbaycan dili", level: String = "Hamı üçün",
        format: String = "Oflayn", approval: Boolean = false,
        recurring: String = "Bir dəfəlik", tags: List<String> = emptyList(),
        lat: Double? = null, lng: Double? = null,
    ) {
        val c = dataSource.findCity(city) // D-050: naməlum şəhər → Bakı mərkəzi
        val id = "c${eventsV.size + 1}-${now().nano / 1_000_000 % 100000}"
        eventsV.add(
            0,
            QoshulEvent(
                id = id, title = title, category = category,
                image = dataSource.imageForCategory(category),
                city = city, location = if (format == "Onlayn") "Onlayn görüş" else location,
                lat = lat ?: c?.lat ?: 40.4093, lng = lng ?: c?.lng ?: 49.8671, distance = 0.0,
                time = time,
                // D-051 fix: ISO tarix yerinə etiket ("Bu gün"/"Sabah"/"Noy 2") —
                // fake data ilə eyni format, kartlar və "Bu axşam" sayğısı tutarlı.
                date = dateLabelFor(epochDay.toLong()),
                epochDay = epochDay, past = false,
                capacity = capacity, participants = 1, host = profileNameV,
                description = description,
                price = if (price > 0) price else null,
                ageLimit = ageLimit, language = language, level = level,
                format = format, approval = approval, recurring = recurring, tags = tags,
            ),
        )
        joinedV.add(id)
        // ROL F: yaratma XP dərhal YOX — ≥3 iştirakçı check-in-də verilir.
        emit()
    }

    /** ROL F: rəy +15, həftədə max 3. A2 düzəlişi: həftə dönüşündə sıfırlanır. */
    fun addReview(eventId: String, rating: Int, comment: String, today: LocalDate = LocalDate.now()): Boolean {
        val key = weekKey(today)
        if (key != reviewXpWeekKey) {
            reviewXpWeekKey = key
            reviewXpThisWeek = 0
        }
        if (reviewXpThisWeek >= XpRules.REVIEW_XP_WEEKLY_CAP) return false
        reviewXpThisWeek++
        award("review-$eventId", XpRules.REVIEW_XP)
        emit()
        return true
    }

    // ═════════════════════════ Match (ROL D) ═════════════════════════

    /** Match namizədləri: swipe edilməyib, bloklanıb YOX, məhdudlaşdırılıb YOX. */
    val matchCandidates: List<Person>
        get() = dataSource.people.filter { p ->
            !swipesV.containsKey(p.id) && !blockedV.contains(p.id) &&
                !restrictedUsersV.contains(p.id)
        }

    /** ROL D: "Maraqlıyam"/"Keç". Qaytarır: true → qarşılıqlı match (mock likesBack). */
    fun swipe(personId: String, interested: Boolean): Boolean {
        if (swipesV.containsKey(personId) || blockedV.contains(personId)) return false
        swipesV[personId] = interested
        if (!interested) {
            emit()
            return false
        }
        val p = personById(personId)
        if (p == null || !p.likesBack) {
            emit()
            return false
        }
        // Match! → bildiriş + chat + XP (gündə max 3)
        matchesV.add(personId)
        friendshipsV[personId] = "accepted"
        friendMessagesV.getOrPut(personId) { mutableListOf() }
        notify(
            NotifType.MATCH,
            "Yeni match: ${p.fullName}",
            "Qarşılıqlı maraq yarandı — söhbəti başlat!",
        )
        val today = LocalDate.now()
        if (matchXpDay != today) {
            matchXpDay = today
            matchXpToday = 0
        }
        if (matchXpToday < XpRules.MATCH_XP_DAILY_CAP) {
            matchXpToday++
            award("match-$personId", XpRules.MATCH_XP)
        }
        emit()
        return true
    }

    /** Demo üçün: kart dəstini sıfırla (yalnız mock). */
    fun resetSwipes() {
        swipesV.clear()
        emit()
    }

    // ═════════════════════════ Rezerv (ROL E) ═════════════════════════

    val reservationRestricted: Boolean
        get() = restrictedUntilV != null && now().isBefore(restrictedUntilV)

    /** ROL E axını: tarix → slot → nəfər → qeyd → kupon → təsdiq. */
    fun createReservation(
        venue: Partner, date: String, time: String, party: Int,
        note: String = "", couponCode: String? = null,
    ): Reservation? {
        if (reservationRestricted) return null
        // Kupon yoxlaması (ROL G): aktiv + bu məkana aiddir
        var coupon: UserCoupon? = null
        if (couponCode != null) {
            coupon = couponsV.firstOrNull { it.code == couponCode }
            if (coupon == null || !coupon.isActive || coupon.venueId != venue.id) return null
        }
        val id = "r${reservationsV.size + 1}"
        val resv = Reservation(
            id = id, venueId = venue.id, venueName = venue.name, venueImage = venue.image,
            date = date, time = time, party = party, note = note,
            couponCode = coupon?.code,
            qrCode = "QSH-$id-${now().nano / 1_000_000 % 10000}",
            createdAt = now(),
            status = if (venue.autoAccept) ResvStatus.CONFIRMED else ResvStatus.PENDING,
        )
        reservationsV.add(0, resv)
        notify(
            NotifType.RESV_UPDATE,
            if (venue.autoAccept) "Rezerv təsdiqləndi" else "Rezerv gözləyir",
            "${venue.name} · $date $time · $party nəfər" +
                if (venue.autoAccept) "" else " — partnyor təsdiqini gözləyir",
        )
        emit()
        return resv
    }

    /** ROL E-6: partnyor QR-i skan edir → İSTİFADƏ OLUNDU + XP + kupon istifadə. */
    fun checkInReservation(id: String): Boolean {
        val resv = reservationsV.firstOrNull { it.id == id } ?: return false
        if (!resv.canCheckIn) return false
        resv.status = ResvStatus.USED
        award("resv-$id", XpRules.RESERVATION_XP)
        if (resv.couponCode != null) {
            couponsV.filter { it.code == resv.couponCode }.forEach { it.used = true }
        }
        emit()
        return true
    }

    /** ROL E-7: ləğv (mock: 2 saatdan əvvəl həmişə pulsuz). */
    fun cancelReservation(id: String) {
        reservationsV.filter { it.id == id && it.canCancel }.forEach { r ->
            r.status = ResvStatus.CANCELLED
            notify(
                NotifType.RESV_UPDATE,
                "Rezerv ləğv edildi",
                "${r.venueName} · ${r.date} ${r.time} rezervi ləğv olundu.",
            )
        }
        emit()
    }

    /** ROL E-7: gəlməmə — 3 dəfə → 7 gün rezerv hüququ məhdudiyyəti. */
    fun markNoShow(id: String) {
        reservationsV.filter { it.id == id && it.canCancel }.forEach { r ->
            r.status = ResvStatus.NO_SHOW
            noShowCountV++
            if (noShowCountV >= XpRules.NO_SHOW_LIMIT) {
                restrictedUntilV = now().plusDays(XpRules.NO_SHOW_RESTRICTION_DAYS.toLong())
                notify(
                    NotifType.RESV_UPDATE,
                    "Rezerv hüququ müvəqqəti məhdudlaşdırıldı",
                    "3 dəfə gəlməmə qeydə alındı — ${XpRules.NO_SHOW_RESTRICTION_DAYS} " +
                        "gün sonra rezerv yenidən açılacaq.",
                )
            }
        }
        emit()
    }

    // ═════════════════════════ Kuponlar (ROL G) ═════════════════════════

    /** Bu istifadəçiyə görünən təkliflər: səviyyə-avto (açılıb) + XP ilə alınanlar. */
    val visibleOffers: List<CouponOffer>
        get() = offersV.filter { o -> if (o.autoByLevel) o.unlockedFor(rankIndex) else true }

    /** D-026 (L.6): XP ilə kupon al — həmin obyektin Endirim XP balansından. */
    fun buyCoupon(offerId: String): UserCoupon? {
        val offer = offersV.firstOrNull { it.id == offerId } ?: return null
        if (offer.autoByLevel) return null
        if (!offer.unlockedFor(rankIndex)) return null
        val acct = venueXp[offer.venueId] ?: return null
        if (acct.balance < offer.xpPrice) return null
        acct.spend(offer.xpPrice, "Kupon alındı: ${offer.label}", "xl${ledgerSeq++}")
        val coupon = UserCoupon(
            id = "uc$couponSeq",
            offerId = offer.id,
            code = "QSHUL-${couponSeq.toString().padStart(4, '0')}-${offer.venueId.uppercase()}",
            venueId = offer.venueId,
            venueName = offer.venueName,
            label = offer.label,
            expiresAt = now().plusDays(offer.validityDays.toLong()),
            purchasedAt = now(),
        )
        couponSeq++
        couponsV.add(0, coupon)
        notify(
            NotifType.COUPON_EXPIRING,
            "Kupon alındı: ${offer.label}",
            "${offer.venueName} · ${offer.validityDays} gün etibarlıdır. " +
                "Vaxtı bitərsə XP geri qaytarılmır.",
        )
        emit()
        return coupon
    }

    /** Bu məkanda rezervə tətbiq edilə bilən aktiv kuponlar (ROL E-3). */
    fun activeCouponsFor(venueId: String): List<UserCoupon> =
        couponsV.filter { it.isActive && it.venueId == venueId }

    // ═════════════════════════ Endirim XP (D-026, product-spec L) ═════════════════════════

    /** L.4-4: xərcləmə önizləməsi — məzənnə + limit yoxlaması. */
    fun previewSpend(venueId: String, xpAmount: Int, billAzn: Double): SpendPreview {
        val acct = venueXp[venueId]
            ?: return SpendPreview(false, 0.0, "hesap")
        if (xpAmount <= 0 || billAzn <= 0) {
            return SpendPreview(false, 0.0, "hesab")
        }
        // D-061 "nə olar əgər": xp_per_azn konfiqurasiyası 0/negativ gəlsə
        // (gələcək server xp_config cədvəli) sıfıra bölmə/Infinity olmasın.
        if (acct.config.xpPerAzn <= 0) {
            return SpendPreview(false, 0.0, "hesap")
        }
        if (acct.balance < xpAmount) return SpendPreview(false, 0.0, "balans")
        if (acct.config.minBillAzn > 0 && billAzn < acct.config.minBillAzn) {
            return SpendPreview(false, 0.0, "minBill")
        }
        val discount = xpAmount.toDouble() / acct.config.xpPerAzn
        val cap = billAzn * acct.config.maxDiscountPercent / 100
        if (discount > cap + 0.001) {
            return SpendPreview(false, cap, "cap")
        }
        return SpendPreview(true, discount, null)
    }

    /** L.4-5: atomik xərcləmə — double-spend qadağan (mock-da tək axın). */
    fun spendVenueXp(
        venueId: String, xpAmount: Int, billAzn: Double,
        note: String = "Kassada XP endirimi",
    ): Double? {
        val p = previewSpend(venueId, xpAmount, billAzn)
        if (!p.ok) return null
        // D-061 "nə olar əgər" — preview ilə spend arası hesab yoxa çıxsa
        // (gələcək server sinxronu) !! əvəzinə təyin olunmuş null qayıdışı.
        val acct = venueXp[venueId] ?: return null
        acct.spend(xpAmount, note, "xl${ledgerSeq++}")
        notify(
            NotifType.COUPON_EXPIRING,
            "XP endirimi tətbiq olundu",
            "${acct.venueName} · $xpAmount XP → " +
                String.format("%.2f", xpAmount.toDouble() / acct.config.xpPerAzn) +
                " ₼ endirim. Receipt ledger-ə yazıldı.",
        )
        emit()
        return p.discount
    }

    // — L.4-1: müştərinin dinamik XP kartı (60s, tək istifadəlik) —

    fun regenerateXpCard(venueId: String) {
        xpCardV = XpCardCode.fresh(now())
        xpCardVenueIdV = venueId
        emit()
    }

    /** Dinamik QR işlənib → təkrar istifadə mümkün deyil (L.4 qorunması). */
    fun markXpCardUsed() {
        xpCardV?.used = true
        emit()
    }

    /** XP kartı etibarlıdırmı (venue uyğun + vaxt bitməyib + işlənməyib). */
    fun xpCardValidFor(venueId: String): Boolean =
        // D-061: !! zənciri əvəzinə null-safe (vəziyyət paralel dəyişə bilər)
        xpCardV?.let { card -> xpCardVenueIdV == venueId && card.isValid } == true

    // — L.3: təşkilatçı bitirmə axını (statik QR → şəkil → taq) —

    fun isActivityFinished(eventId: String): Boolean = finishedActivitiesV.contains(eventId)

    fun canFinishActivity(e: QoshulEvent): Boolean =
        e.past && e.venueId != null && e.host == profileNameV &&
            !finishedActivitiesV.contains(e.id)

    /** Aktivite bitir: təşkilatçı +50, hər taq edilən iştirakçı +25 (L.3). */
    fun finishActivity(e: QoshulEvent, taggedIds: List<String>): ActivityXpResult? {
        if (!canFinishActivity(e) || taggedIds.isEmpty()) return null
        // D-061: canFinishActivity venueId-ni yoxlayır, amma müdafiə
        // qatında təkrar null-check (gələcəkdə şərtlər dəyişərsə çökmə YOX)
        val venueId = e.venueId ?: return null
        val acct = venueXp[venueId] ?: return null
        finishedActivitiesV.add(e.id)

        // Təşkilatçı mükafatı — obyekt bazasına grant.
        acct.grant(
            XpRules.ORGANIZER_XP,
            "Təşkilatçı mükafatı — \"${e.title}\"",
            "xl${ledgerSeq++}",
        )

        val result = ActivityXpResult(
            organizerXp = XpRules.ORGANIZER_XP,
            perParticipantXp = XpRules.TAGGED_PARTICIPANT_XP,
            taggedCount = taggedIds.size,
            venueId = acct.venueId,
            venueName = acct.venueName,
        )

        notify(
            NotifType.LEVEL_UP,
            "+${XpRules.ORGANIZER_XP} Endirim XP qazandın",
            "${acct.venueName} · \"${e.title}\" bitirildi. Şəkillər obyektin " +
                "biznes hesabına düşdü; ${taggedIds.size} iştirakçı taq edildi " +
                "(hər birinə +${XpRules.TAGGED_PARTICIPANT_XP} XP).",
        )
        emit()
        return result
    }

    // ═════════════════════════ Bildirişlər (ROL H) ═════════════════════════

    private fun notify(type: NotifType, title: String, body: String) {
        if (!notifPrefs.isEnabled(type)) return
        val t = now()
        if (type.isMarketing && isQuietHours(t)) return
        notificationsV.add(
            0,
            AppNotification(
                id = "n${notificationsV.size + 1}-${t.nano / 1_000_000 % 100000}",
                type = type, title = title, body = body, at = t,
            ),
        )
        if (notificationsV.size > 50) notificationsV.removeAt(notificationsV.size - 1)
    }

    fun markAllNotificationsRead() {
        notificationsV.forEach { it.read = true }
        emit()
    }

    fun toggleNotifPref(t: NotifType, v: Boolean) {
        notifPrefs.setEnabled(t, v)
        emit()
    }

    /** ROL H: tədbir xatırlatması (24s / 2s əvvəl) — mock tetikleyici. */
    fun eventReminder(e: QoshulEvent, whenText: String) {
        notify(
            NotifType.EVENT_REMINDER,
            "Tədbir $whenText",
            "${e.title} — ${e.date}, ${e.time} · ${e.location}",
        )
        emit()
    }

    // ═════════════════════════ Moderasiya (ROL I) ═════════════════════════

    /** ROL I: blok — qarşılıqlı görünməzlik, chat bağlanır. */
    fun blockUser(personId: String) {
        blockedV.add(personId)
        friendshipsV.remove(personId)
        friendMessagesV.remove(personId)
        matchesV.remove(personId)
        emit()
    }

    fun unblockUser(personId: String) {
        blockedV.remove(personId)
        emit()
    }

    /** ROL I: report — eyni istifadəçiyə 3+ → avtomatik məhdudlaşdırma. */
    fun reportUser(personId: String, reason: String, note: String = "") {
        reportsV.add(
            ReportEntry(
                targetType = "user", targetId = personId,
                reason = reason, note = note, at = now(),
            )
        )
        val count = (reportCountsV[personId] ?: 0) + 1
        reportCountsV[personId] = count
        if (count >= 3 && !restrictedUsersV.contains(personId)) {
            restrictedUsersV.add(personId)
        }
        emit()
    }

    // ═════════════════════════ Söhbətlər ═════════════════════════

    fun sendEventMessage(eventId: String, text: String) {
        val list = eventMessagesV.getOrPut(eventId) { mutableListOf() }
        list.add(ChatMessage("Sən", text, mine = true))
        mockReply(list, eventId)
        emit()
    }

    fun sendFriendMessage(personId: String, text: String) {
        val list = friendMessagesV.getOrPut(personId) { mutableListOf() }
        list.add(ChatMessage("Sən", text, mine = true))
        mockFriendReply(list, personId) // D-056: şəxsi söhbətdə də cavab gəlir
        emit()
    }

    /** D-056: şəxsi söhbət mock cavabı — hadisə mockReply pariteti. */
    private fun mockFriendReply(list: MutableList<ChatMessage>, personId: String) {
        val scope = scope ?: return // JVM testlərində avtomatik cavab bağlıdır
        replyJobs += scope.launch {
            delay(1400)
            val p = personById(personId) ?: return@launch
            val replies = listOf(
                "Razıyam, nə vaxt görüşək?",
                "Yaxşı fikirdir! Saat dəqiq desən, gələrəm.",
                "Super — sözə görə!",
            )
            list.add(ChatMessage(p.firstName, replies[now().second % replies.size]))
            emit()
        }
    }

    /** ROL TAB 4: "Söhbəti sil" — şəxsi söhbəti bağlayır (mock). */
    fun deleteChat(personId: String) {
        friendMessagesV.remove(personId)
        friendshipsV.remove(personId)
        emit()
    }

    /** A10 düzəlişi: cavab Job kimi — ləğv edilə bilən, dispose-də exception yox. */
    private fun mockReply(list: MutableList<ChatMessage>, eventId: String) {
        val scope = scope ?: return // JVM testlərində avtomatik cavab bağlıdır
        replyJobs += scope.launch {
            delay(1200)
            val e = eventById(eventId)
            val replies = listOf(
                "Əla, görüşərik!",
                "Super, gözləyirəm.",
                "Razıyam, saat dəqiqdir.",
            )
            list.add(ChatMessage(e?.host ?: "İştirakçı", replies[now().second % replies.size]))
            emit()
        }
    }

    /** Test köməkçisi: ləngimiş cavabları gözləmədən işə salır. */
    fun deliverPendingReplies() {
        replyJobs.forEach { it.cancel() }
        replyJobs.clear()
    }

    // — insanlar —
    fun requestFriend(personId: String) {
        friendshipsV[personId] = "sent"
        emit()
    }

    fun acceptFriend(personId: String) {
        friendshipsV[personId] = "accepted"
        emit()
    }

    val acceptedFriends: List<Person>
        get() = dataSource.people.filter { p ->
            friendshipsV[p.id] == "accepted" && !blockedV.contains(p.id) &&
                !restrictedUsersV.contains(p.id)
        }

    // ═════════════════════════ Assistant (mock AI) ═════════════════════════

    fun sendAssistant(text: String) {
        if (text.isBlank() || assistantBusyV) return
        assistantMsgsV.add(ChatMessage("Sən", text.trim(), mine = true))
        assistantBusyV = true
        emit()

        val reply = assistantReplyFor(text.trim())
        assistantJob?.cancel()
        val s = scope
        if (s != null) {
            assistantJob = s.launch {
                delay(400)
                // Yazı simulyasiyası: 24ms-də 2 simvol (Flutter pariteti).
                var shown = ""
                assistantMsgsV.add(ChatMessage("Köməkçi", ""))
                var idx = assistantMsgsV.size - 1
                while (shown.length < reply.length && assistantBusyV) {
                    // D-061: siyahı paralel dəyişsə (idx artıq keçərsə)
                    // yazı simulyasiyası yeni elementə keçir, çökmür.
                    idx = idx.coerceAtMost(assistantMsgsV.size - 1)
                    shown = reply.substring(0, minOf(shown.length + 2, reply.length))
                    assistantMsgsV[idx] = ChatMessage("Köməkçi", shown)
                    emit()
                    delay(24)
                }
                assistantBusyV = false
                emit()
            }
        } else {
            // scope yoxdur (JVM test): dərhal tam cavab.
            assistantMsgsV.add(ChatMessage("Köməkçi", reply))
            assistantBusyV = false
            emit()
        }
    }

    private fun assistantReplyFor(q: String): String {
        val lower = lowercaseAz(q)
        val wantsProposal = listOf("fikri", "fikir", "təklif", "plan", "şənbə", "bazar", "təşkil")
            .any { lower.contains(it) }
        if (wantsProposal) {
            return "Şənbə üçün əla planım var! Yeni başlayanlar üçün asan yürüş marşrutu, " +
                "sonrasında isə birlikdə çay. Təklifim aşağıda — bir düymə ilə tədbirə çevirə " +
                "bilərsən.\n[TƏDBİR]{\"title\":\"Şənbə səhəri Qala yürüşü\",\"category\":\"Yürüş\"," +
                "\"date\":\"${nextSaturdayIso()}\",\"time\":\"09:00\",\"capacity\":10," +
                "\"location\":\"Qala qalası, Bakı\",\"description\":\"Qala qalası ətrafında 5 km asan " +
                "yürüş + qala divarlarında fotopauza. Sonrasında yaxınlıqdakı kafedə çay.\"," +
                "\"partner\":\"\"}"
        }
        if (lower.contains("pulsuz") || lower.contains("ödənişsiz")) {
            val free = upcoming.filter { it.price == null && it.city == cityV }.take(3)
            if (free.isEmpty()) return "Bu şəhərdə hazırda pulsuz tədbir görmürəm. Amma özün pulsuz tədbir yarada bilərsən!"
            return "Pulsuz tədbirlər bunlardır:\n\n" +
                free.joinToString("\n") { "• ${it.title} — ${it.date}, ${it.time} (${it.location})" }
        }
        if (lower.contains("idman") || lower.contains("spor")) {
            val sport = upcoming.filter {
                dataSource.groupOf(it.category) == "İdman" ||
                    dataSource.groupOf(it.category) == "Təbiət"
            }.take(3)
            if (sport.isEmpty()) return "İdman tədbirləri hələ azdır — ilk idman görüşünü sən yarat!"
            return "Aktiv idman tədbirləri:\n\n" +
                sport.joinToString("\n") { "• ${it.title} — ${it.date} ${it.time}, ${it.participants}/${it.capacity} nəfər" }
        }
        if (lower.contains("gəncə")) {
            val g = upcoming.filter { it.city == "Gəncə" }
            if (g.isEmpty()) return "Gəncədə hazırda plan yoxdur — amma e9 pikniki yaxındadır, bax!"
            return "Gəncədə olan görüşlər:\n\n" +
                g.joinToString("\n") { "• ${it.title} — ${it.date}, ${it.time}" }
        }
        val rec = cityEvents.take(2)
        if (rec.isEmpty()) return "Bu şəhərdə hələ tədbir yoxdur. İlk tədbiri sən yarat — mən kömək edərəm!"
        return "Sənin üçün tövsiyələrim:\n\n" +
            rec.joinToString("\n") { "• ${it.title} (${it.category}) — ${it.date} ${it.time}" } +
            "\n\nHər hansı birini seçmək istəsən, \"Sənin üçün\" bölməsində AI seçimi də sınaya bilərsən."
    }

    /** Azərbaycan hərfləri üçün lowercase (I→ı və İ→i xüsusi halları). */
    private fun lowercaseAz(s: String): String =
        s.replace("I", "ı").replace("İ", "i").lowercase()

    private fun nextSaturdayIso(): String {
        // D-061: nə olar əgər saat/vaxt mənbəyi nasazdırsa — limitsiz
        // dövr qadağanı (7 gündən çox fırlanma mantıq xətası deməkdir).
        var d = LocalDate.now()
        var guard = 0
        while ((d.dayOfWeek != java.time.DayOfWeek.SATURDAY || !d.isAfter(LocalDate.now())) && guard < 400) {
            d = d.plusDays(1)
            guard++
        }
        return Safety.of("next-saturday", "2026-01-01") {
            "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
        }
    }

    /** "AI ilə seç" (Sənin üçün) — mock tövsiyə səbəbləri. */
    fun aiRecommendations(): List<Pair<QoshulEvent, String>> {
        val list = cityEvents.take(3)
        val reasons = listOf(
            "Profilindəki maraqlara ən yaxın tədbir.",
            "Yaxın məsafədədir və yerlər az qalıb.",
            "Yeni başlayanlar üçün uyğun səviyyə.",
        )
        return list.mapIndexed { i, e -> e to reasons.getOrElse(i) { "" } }
    }

    /** Şəkil/qrup köməkçiləri UI üçün (D-050: mənbəyə yönəldilmiş). */
    fun statusLabel(id: String): String = dataSource.statusLabel(id)

    /** Bütün məkanlar (server rejimində serverdən gələn siyahı). */
    val partners: List<Partner> get() = dataSource.partners

    /** Məkan ID ilə (server rejimində server siyahısından). */
    fun venueById(id: String): Partner? = dataSource.partners.firstOrNull { it.id == id }

    /** Məkan rəyləri (D-050: fake yalnız PLACEHOLDER rejimində doludur). */
    fun venueReviews(venueId: String) = dataSource.mockVenueReviews[venueId] ?: emptyList()

    /** Məkan rezerv slotları. */
    fun venueSlots(venueId: String) = dataSource.venueSlotTimes[venueId] ?: emptyList()
}

/** UI-nin oxuduğu immutabl snapshot. */
data class AppUiState(
    val version: Long,
    // auth/onboarding
    val signedIn: Boolean,
    val onboarded: Boolean,
    val profileName: String,
    val firstName: String,
    val lastName: String,
    val city: String,
    val status: String,
    val university: String,
    val workField: String,
    val languages: List<String>,
    val interests: List<String>,
    val goals: List<String>,
    val personality: PersonalityResult?,
    val availability: List<String>,
    val groupSize: String,
    val budget: String,
    val birthDate: LocalDate?,
    val gender: String,
    val bio: String,
    val photoCount: Int,
    val phoneVerified: Boolean,
    val profileCompleted: Boolean,
    // məxfilik
    val mapVisibility: MapVisibility,
    val showDistance: Boolean,
    // XP 1
    val xp: Int,
    val earnedBadges: Set<String>,
    val loginStreak: Int,
    // XP 2
    val venueXp: Map<String, VenueXpAccount>,
    val discountXpTotal: Int,
    val xpCard: XpCardCode?,
    val xpCardVenueId: String?,
    // tədbirlər
    val events: List<QoshulEvent>,
    val joined: Set<String>,
    val pending: Set<String>,
    val saved: Set<String>,
    val checkedIn: Set<String>,
    // insanlar
    val friendships: Map<String, String>,
    val swipes: Map<String, Boolean>,
    val matches: Set<String>,
    // rezervlər
    val reservations: List<Reservation>,
    val noShowCount: Int,
    val restrictedUntil: LocalDateTime?,
    // kuponlar
    val offers: List<CouponOffer>,
    val coupons: List<UserCoupon>,
    // bildirişlər
    val notifications: List<AppNotification>,
    // moderasiya
    val blocked: Set<String>,
    val restrictedUsers: Set<String>,
    // söhbətlər
    val eventMessages: Map<String, List<ChatMessage>>,
    val friendMessages: Map<String, List<ChatMessage>>,
    // assistant
    val assistantMsgs: List<ChatMessage>,
    val assistantBusy: Boolean,
    // bitmiş aktivitelər
    val finishedActivities: Set<String>,
) {
    val rankIndex: Int get() = XpRules.rankIndexForXp(xp)
    val rank: String get() = XpRules.rankForXp(xp)
    val badgeList: List<BadgeDef> get() = badgeDefs
}
