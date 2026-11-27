package az.qoshul.app.core.engine

import az.qoshul.app.core.data.QoshulDataSource
import az.qoshul.app.core.data.Taxonomy
import az.qoshul.app.core.data.ServerDataSource
import az.qoshul.app.core.data.dateLabelFor
import az.qoshul.app.core.data.dateLabelToIso
import az.qoshul.app.core.model.ActivityXpResult
import az.qoshul.app.core.model.AppNotification
import az.qoshul.app.core.model.AssistantParse
import az.qoshul.app.core.model.BadgeDef
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.model.CouponOffer
import az.qoshul.app.core.model.CouponPurchase
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
import az.qoshul.app.core.model.SpendError
import az.qoshul.app.core.model.SpendPreview
import az.qoshul.app.core.model.UserCoupon
import az.qoshul.app.core.model.VenueXpAccount
import az.qoshul.app.core.model.XpCardCode
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.model.badgeDefs
import az.qoshul.app.core.model.isQuietHours
import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.backend.SyncSink
import az.qoshul.app.core.safety.Safety
import az.qoshul.app.core.model.parseAssistantEvent
import az.qoshul.app.core.theme.DesignColor
import az.qoshul.app.core.theme.DesignMode
import az.qoshul.app.core.theme.Q
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
 *  - A10 düzəlişi: uzun gedişlər Job ləğvi ilə təhlükəsizdir.
 *
 * Bütün məntiq qaydaları ROL (docs/product-spec.md) + D-026 ilə eynidir.
 */
class AppEngine(
    /**
     * D-151: yalnız server mənbəyi — başlanğıc BOŞ; [applyServerData]
     * [ServerDataSource] mutable listlərinə serverdən oxunan datanı yazır,
     * bütün getterlər dataSource üzərindən oxuyur (fake mənbə APK-da YOXDUR).
     */
    val dataSource: QoshulDataSource = ServerDataSource,
    private val scope: CoroutineScope? = null,
    private val clock: () -> LocalDateTime = { LocalDateTime.now() },
    /**
     * Backend güzgüsü. Mutasiyalar [SyncEvent] kimi [sync]-ə çatdırılır
     * (D-170: ikili rejim YOXDUR — testlər RecordingSync verir, istehsal
     * SupabaseSync qurur);
     * qaydaları server tərəfdə SQL funksiyaları zorlayır (supabase/0002).
     */
    private val sync: SyncSink? = null,
) {
    /**
     * A2 (M0): vahid vaxt mənbəyi — ARTIQ PUBLİK. Modellerin/UI-ın bütün
     * "indi" ehtiyacları bu tək nöqtədən ötürülür (clock injeksiyası
     * bypass edilə bilmir); testlərdə frozen clock məcburidir.
     */
    fun now(): LocalDateTime = clock()

    /** A2: tarix ehtiyacı olan UI-lar üçün (xəritə "indi açıqdır", tarix seçici). */
    fun today(): LocalDate = now().toLocalDate()

    // ═══════════════ Daxili dəyişən hal (Flutter field-ləri) ═══════════════

    // — auth/onboarding —
    private var signedInV = false
    private var onboardedV = false
    // C (D-170): onboarded qərarı YALNIZ serverdən gələn profillə verilir.
    // profileLoadedV=false → UI loading göstərir, onboarding AÇILMIR;
    // xəta → syncErrorV + UI retry düyməsi (səssiz udma YOXDUR)
    private var profileLoadedV = false
    private var syncErrorV = ""
    // A.1 (D-170): BÜTÜN başlanğıc dəyərlər BOŞ — static/default məlumat
    // yoxdur. Profil məlumatı yalnız qeydiyyatda istifadəçidən alınır
    // (onboarding) və ya serverdən gəlir (applyServerData). Boş String/
    // boş List tiplər saxlanılıb — UI-da null yoxlaması lazım deyil.
    private var profileNameV = ""
    private var firstNameV = ""
    private var lastNameV = ""
    // D-112: profil şəkli açarı (imagePaint açarları — coffee/padel/tech/
    // friends_baku); profil redaktə ekranından seçilir; boş → imagePaint
    // default avatar (friends_baku) göstərir
    private var avatarV = ""
    private var cityV = ""
    private var statusV = ""
    private var universityV = ""
    private var workFieldV = ""
    private var languagesV = listOf<String>()
    private var interestsV = listOf<String>()
    private var goalsV = listOf<String>()
    private var personalityV: PersonalityResult? = null
    private var availabilityV = listOf<String>()
    private var groupSizeV = ""
    private var budgetV = ""

    // — ROL B: profil yaradılması —
    private var birthDateV: LocalDate? = null
    private var genderV = ""
    private var bioV = ""
    // E (D-170): şəkil backendi hələ AKTİV DEYİL — photo_count 0-dan
    // başlayır; storage API hazır olanda redaktə ekranı açılacaq
    private var photoCountV = 0
    private var phoneVerifiedV = false
    private var profileCompletedV = false

    // — ROL TAB 5 + D-027: məxfilik —
    private var mapVisibilityV = MapVisibility.EVERYONE
    private var showDistanceV = true

    // — D-119: dizayn seçimi (Rəng: Yaşıl default / Qızılı; Mod: Ağ default /
    // Qara) — başlanğıc Q tokenlərinin ilkin dəyərləri ilə eynidir —
    private var designColorV = DesignColor.GREEN
    private var designModeV = DesignMode.LIGHT

    // — XP 1: Peşəkarlıq (xərclənmir) —
    // I.1 (D-170): başlanğıc XP 0 — köhnə 320 static idi; XP yalnız
    // serverdən (applyServerData → me.xp) gəlir
    private var xpV = 0
    private val xpEventIds = mutableSetOf<String>()
    private val earnedBadgesV = mutableSetOf<String>()
    private var matchXpToday = 0
    // A2: field-initsializatorlar artıq clock-dan bəslənir (bypass yoxdur)
    private var matchXpDay: LocalDate = clock().toLocalDate()
    private var reviewXpThisWeek = 0
    private var reviewXpWeekKey = weekKey(clock().toLocalDate())
    private var loginStreakV = 1
    // B2: son giriş günü (epochDay) — gap semantikası üçün; dailyLoginDone
    // proses-bayrağı əvəzinə (proseslərarası/saat-keçidi düzgün işləyir)
    private var lastLoginDayV: Long? = null

    // — XP 2: Endirim (obyektə-məxsus, D-026) — serverdən oxunur (D-151) —
    private val venueXpV: MutableMap<String, VenueXpAccount> =
        dataSource.initialVenueXp().toMutableMap()
    val venueXp: Map<String, VenueXpAccount> get() = venueXpV

    /**
     * A3 (M0): başlanğıc data invariantları — pozuntu SƏSSİZ deyil, qurulma
     * anda dərhal xəta (fail-fast; köhnə: v3 Techno Hub hesabsız qalıb
     * buyCoupon səssiz-null edirdi). M1-də FK, M2-də typed ID-lər bunu
     * tip səviyyəsinə daşıyacaq.
     */
    init {
        val problems = dataInvariants()
        check(problems.isEmpty()) {
            "DataSource invariantları pozulub (A3): ${problems.joinToString("; ")}"
        }
    }

    private fun dataInvariants(): List<String> = buildList {
        dataSource.partners.forEach { p ->
            if (venueXp[p.id] == null) {
                add("partner ${p.id} (${p.name}) üçün venue XP hesabı yoxdur")
            }
        }
        dataSource.offers.forEach { o ->
            if (dataSource.partners.none { it.id == o.venueId }) {
                add("təklif ${o.id} naməlum obyektə (${o.venueId}) istinad edir")
            }
        }
        dataSource.events.forEach { e ->
            val vid = e.venueId ?: return@forEach
            if (dataSource.partners.none { it.id == vid }) {
                add("tədbir ${e.id} naməlum obyektə ($vid) istinad edir")
            }
        }
    }

    private val finishedActivitiesV = mutableSetOf<String>()
    private var xpCardV: XpCardCode? = null
    private var xpCardVenueIdV: String? = null

    // — tədbirlər —
    // D-151: başlanğıc BOŞ — applyServerData serverdən oxunan tədbirləri
    // yazır; "e0" hardcoded demo iştirakı silindi.
    private val eventsV = dataSource.events.toMutableList()
    private val joinedV = mutableSetOf<String>()
    private val pendingV = mutableSetOf<String>()
    private val savedV = mutableSetOf<String>()
    private val checkedInV = mutableSetOf<String>()

    // — insanlar / dostluqlar —
    // D-176: başlanğıc hardcoded ("p2" to "accepted") SİLİNDİ — dostluqlar
    // yalnız serverdən gəlir (applyServerData → d.friendships)
    private val friendshipsV = mutableMapOf<String, String>()

    // — ROL D: match —
    // D-176: swipes serverdən bərpa olunur (applyServerData → d.swipedPeople);
    // likesBack proqnozu SİLİNDİ — match qərarı fn_record_swipe cavabıdır
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
    private var assistantErrorV = ""
    private val replyJobs = mutableListOf<Job>()

    // — D-179: AI tövsiyə skorları (serverdən; null = zolaq gizlədilir) —
    private var aiScoresV: Map<String, Int>? = null

    private var ledgerSeq = 100
    private var version = 0L

    // ═════════════════════════ State axını ═════════════════════════

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(buildState())
    val state: kotlinx.coroutines.flow.StateFlow<AppUiState> get() = _state

    private fun buildState() = AppUiState(
        version = ++version,
        signedIn = signedInV, onboarded = onboardedV,
        profileLoaded = profileLoadedV, syncError = syncErrorV,
        profileName = profileNameV, firstName = firstNameV, lastName = lastNameV,
        avatar = avatarV,
        city = cityV, status = statusV, university = universityV, workField = workFieldV,
        languages = languagesV, interests = interestsV, goals = goalsV,
        personality = personalityV, availability = availabilityV,
        groupSize = groupSizeV, budget = budgetV,
        birthDate = birthDateV, gender = genderV, bio = bioV,
        photoCount = photoCountV, phoneVerified = phoneVerifiedV,
        profileCompleted = profileCompletedV,
        mapVisibility = mapVisibilityV, showDistance = showDistanceV,
        designColor = designColorV, designMode = designModeV,
        xp = xpV, awardedKeys = xpEventIds.toSet(), earnedBadges = earnedBadgesV.toSet(), loginStreak = loginStreakV,
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
        assistantError = assistantErrorV,
        aiRecommendationsAvailable = aiScoresV != null,
        finishedActivities = finishedActivitiesV.toSet(),
    )

    private fun emit() {
        _state.value = buildState()
    }

    private fun weekKey(d: LocalDate): String = "${d.year}-W${d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}"

    // ═════════════════════════ Köməkçi getterlər ═════════════════════════

    val upcoming: List<QoshulEvent> get() = eventsV.filter { !it.past }
    // A.1 (D-170): şəhər seçilməyib (onboarding öncəsi / server boş) →
    // filtr YOXDUR: bütün şəhərlərin tədbirləri görünür (boş ekran əvəzinə)
    val cityEvents: List<QoshulEvent>
        get() = if (cityV.isBlank()) upcoming else upcoming.filter { it.city == cityV }
    /** D-054: bugünkü tədbirlər — ana ekranda "Aktiv tədbirlər" zolağı üçün. */
    val activeEvents: List<QoshulEvent>
        get() = cityEvents.filter { it.epochDay == today().toEpochDay().toInt() }
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

    /**
     * İdemponent Peşəkarlıq XP mükafatı — eyni açar ikinci dəfə sayılmır.
     *
     * I.1/I.4 (D-170): XP optimistic artırılır (UI dərhal reaksiya verir),
     * amma server avtoritativdir — sync xətası gələndə ROLLBACK olunur
     * ([onSyncError] kanalı) və növbəti refreshFromServer server XP-sini
     * qaytarır. XpAwarded güzgü getmir (qayda 8) — server eyni qaydanı
     * öz intent RPC-si daxilində icra edir.
     */
    private fun award(key: String, amount: Int): Boolean {
        if (xpEventIds.contains(key)) return false
        xpEventIds.add(key)
        val beforeRank = rankIndex
        xpV += amount
        pendingXpAwards[key] = amount
        report(SyncEvent.XpAwarded(key, amount, "award"))
        maybeLevelUp(beforeRank)
        return true
    }

    /**
     * I.4: gözləmədə olan optimistic XP məbləğləri (rollback üçün).
     * D-175 (ikinci dalğa): açar → (məbləğ, kritiklik). KRİTİK əməliyyatların
     * (join/leave/save/swipe/buy/spend/reserve/checkin/finish) XP-ləri
     * xətada rollback olunur; metadata-only hadisələr optimistic qalır.
     */
    private val pendingXpAwards = linkedMapOf<String, Int>()

    /**
     * D-176 (ikinci dalğa): report() təsnifatı — hər [SyncEvent] ya KRİTİK,
     * ya METADATA-dır. KRİTİK = server dəyişikliyi görünən state-i dəyişir;
     * xətada optimistic dəyişikliklər rollback olunmalıdır. METADATA =
     * güzgü məlumatı (prefslər, lokasiya, bildiriş seçimi); xətada yalnız
     * Snackbar ilə bildirilir, rollback lazım deyil (server refresh-də
     * doğruluğu bərpa edir).
     */
    internal fun criticalityOf(e: SyncEvent): Criticality = when (e) {
        is SyncEvent.XpAwarded -> Criticality.METADATA // güzgü getmir (qayda 8)
        is SyncEvent.JoinEventIntent,
        is SyncEvent.LeaveEventIntent,
        is SyncEvent.SaveEventIntent,
        is SyncEvent.SwipeIntent,
        is SyncEvent.BuyCouponIntent,
        is SyncEvent.SpendIntent,
        is SyncEvent.CreateReservationIntent,
        is SyncEvent.UseReservationIntent,
        is SyncEvent.CancelReservationIntent,
        is SyncEvent.NoShowIntent,
        is SyncEvent.CheckInIntent,
        is SyncEvent.GrantVenueXpIntent,
        is SyncEvent.RequestFriendIntent,
        is SyncEvent.RespondFriendIntent,
        is SyncEvent.AddVenueReviewIntent,
        is SyncEvent.CreateEventIntent -> Criticality.CRITICAL
        else -> Criticality.METADATA
    }

    /** D-176: əməliyyat kritikliyi (rollback siyasəti üçün). */
    enum class Criticality { CRITICAL, METADATA }

    /**
     * I.4 (D-170): sync xətası → optimistic dəyişikliklər geri qaytarılır
     * və istifadəçiyə xəta görünür. SupabaseSync.rpcAsync onFailure
     * kanalından çağırılır (aşağıda AppEngine.reportFailure).
     *
     * D-175 (ikinci dalğa): report() təsnifatı — kritik əməliyyatların
     * xətası rollback-in dəstəklənməsi (pendingXpAwards + əməliyyat-specific
     * rollback bərpası refreshFromServer ilə), metadata xətalar yalnız
     * syncErrorV-ə yazılır (Snackbar UI-da göstərir).
     */
    fun reportFailure(tag: String, err: Throwable) {
        // XP rollback: gözləmədə olan mükafatlar geri alınır
        if (pendingXpAwards.isNotEmpty()) {
            pendingXpAwards.values.forEach { xpV -= it }
            pendingXpAwards.clear()
            syncErrorV = "Sinxronizasiya xətası — dəyişikliklər geri alındı (${err.message?.take(80) ?: err::class.simpleName})"
            emit()
        }
        az.qoshul.app.core.safety.Safety.onCaught?.invoke(tag, err)
    }

    /**
     * M1: güzgü çıxışı — heç vaxt atmır, heç vaxt bloklamır (Safety qatır).
     * Server tərəfdə eyni qayda intent RPC-ləri içində icra olunur (qayda 8).
     *
     * D-176 (ikinci dalğa): KRİTİK hadisələrin güzgüsi çıxışda kritiklik
     * etiketi ilə qeyd olunur — rpcAsync xətası gələndə reportFailure
     * yalnız kritik əməliyyatlar üçün rollback edir.
     */
    private fun report(e: SyncEvent) {
        val s = sync ?: return
        Safety.run("engine:report") { s.report(e) }
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
        // D-155 TaskCenter: 16P testi tapşırığı (Novice tier)
        award("personality-set", XpRules.PHONE_VERIFY_XP)
        completeProfileFull()
    }

    /** D-056: dost dəvəti → +100 XP (bir dəfə, XpRules.INVITE_XP əvvələr ölü idi). */
    fun inviteFriend(): Boolean {
        report(SyncEvent.InviteIntent)
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

    /**
     * ROL F: günlük giriş +5; 7 günlük seriya bonusu +30.
     *
     * B2 (istifadəçi qərarı, 2026-11-07): seriya gap semantikası —
     *  - dünən gəlibsə: seriya +1 (ardıcıl);
     *  - 1-2 gün gəlməsə: seriya DONDURULUR (dəyəri yerində qalır);
     *  - 3+ gün gəlməsə: seriya SIFIRLANIR (bugünkü gün yeni seriyanın 1-inci günüdür).
     * Giriş edilen hər gün +5 XP verilir; 7-yə çatanda +30 bonus və dövrə yenidən başlayır.
     * A2: default parametr clock-dan.
     */
    fun dailyLogin(today: LocalDate = now().toLocalDate()) {
        report(SyncEvent.DailyLoginIntent)
        val day = today.toEpochDay()
        val last = lastLoginDayV
        if (last != null && day <= last) return // bu gün artıq sayılıb / saat geri
        val missedDays = if (last == null) 0 else (day - last - 1).toInt()
        when {
            missedDays <= 0 -> loginStreakV++ // ardıcıl gün
            missedDays <= XpRules.STREAK_FREEZE_DAYS -> Unit // dondurulma (B2)
            else -> loginStreakV = 1 // 3+ gün → sıfırlanma (B2)
        }
        lastLoginDayV = day
        award("login-$today", XpRules.DAILY_LOGIN_XP)
        if (loginStreakV == 7) {
            award("streak-7-$today", XpRules.STREAK_BONUS_XP)
            loginStreakV = 1
        }
        emit()
    }

    // ═════════════════════════ Auth ═════════════════════════

    /**
     * Giriş tamamlandı — UI-ni imza vəziyyətinə keçirir. D-149: real
     * GoTrue axını (SupabaseAuth) uğurdan sonra məhz bunu çağırır;
     * (test engine-ləri seeded state ilə eyni qapıdan keçir.)
     */

    // ═════════════════════════ D-151: server data tətbiqi ═════════════════════════

    /**
     * SupabaseReader nəticəsini engine state-inə tətbiq edir — login/
     * refresh zamanı çağırılır. Demo data mənbəyi YOXDUR: bütün listlər
     * serverdən gəlir, boş qalırsa UI empty state göstərir.
     */
    fun applyServerData(d: az.qoshul.app.core.backend.SupabaseReader.ServerData) {
        // I.2 (D-170): aktiv XP kartı SERVERDƏN (CSPRNG nonce) —
        // regenerateXpCard sonrası refresh-də bərpa olunur
        d.xpCard?.let { card ->
            val exp = runCatching {
                java.time.Instant.parse(card.expiresAtIso).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            }.getOrNull() ?: return@let
            xpCardV = az.qoshul.app.core.model.XpCardCode.fromServer(
                card.nonce, card.fallbackCode, exp)
            xpCardVenueIdV = card.venueId
        }
        // C (D-170): profil gəldi → onboarded qərarı serverdəndir
        if (d.myProfile != null) {
            profileLoadedV = true
            syncErrorV = ""
        }
        // profil — D-152: onboarding sahələri və maraqlar da bərpa olunur
        d.myProfile?.let { me ->
            // A.1 (D-170): server fallback-i YOX — boş gəlirsə boş qalır
            // (onboarding tamamlanmayınca ad boş ola bilər)
            firstNameV = me.firstName
            lastNameV = me.lastName
            profileNameV = listOf(me.firstName, me.lastName)
                .filter { it.isNotBlank() }.joinToString(" ")
            cityV = me.city.ifBlank { cityV }
            bioV = me.bio
            xpV = me.xp
            onboardedV = me.onboarded
            photoCountV = me.photoCount
            if (me.gender.isNotBlank()) genderV = me.gender
            if (me.status.isNotBlank()) statusV = me.status
            if (me.university.isNotBlank()) universityV = me.university
            if (me.languages.isNotEmpty()) languagesV = me.languages
            if (me.goals.isNotEmpty()) goalsV = me.goals
            if (me.groupSize.isNotBlank()) groupSizeV = me.groupSize
            if (me.budget.isNotBlank()) budgetV = me.budget
            if (me.availability.isNotEmpty()) availabilityV = me.availability
            if (me.interests.isNotEmpty()) {
                // slug → insan-oxunan ad (Taxonomy əks xəritəsi)
                interestsV = me.interests.map { slug ->
                    Taxonomy.allCategories.firstOrNull { c -> slugifyCategory(c) == slug } ?: slug
                }
            }
            // D-176: avatar açarı serverdən bərpa (fn_set_avatar orada yazır)
            if (me.avatar.isNotBlank()) avatarV = me.avatar
        }
        // listlər — ServerDataSource mutable konteynerlərinə yazılır
        (dataSource as? ServerDataSource)?.apply {
            eventsM.clear(); eventsM.addAll(d.events)
            peopleM.clear(); peopleM.addAll(d.people)
            partnersM.clear(); partnersM.addAll(d.partners)
            offersM.clear(); offersM.addAll(
                d.offers.map { o ->
                    if (o.venueName.isBlank())
                        o.copy(venueName = partnersM.firstOrNull { it.id == o.venueId }?.name ?: "")
                    else o
                },
            )
            notificationsM.clear(); notificationsM.addAll(d.notifications)
            eventMessagesM.clear(); eventMessagesM.putAll(d.eventMessages)
            friendMessagesM.clear(); friendMessagesM.putAll(d.friendMessagePeople)
            venueSlotsM.clear(); venueSlotsM.putAll(d.venueSlots)
        }
        // D-151 düzəliş: eventsV konstruktor anında dataSource surətini çəkib
        // dondururdu — serverdən gələn tədbirlər UI-a çatmırdı. İndi hər
        // applyServerData-da eventsV yenidən qurulur (upcoming/eventById/
        // activeEvents getterləri eventsV üzərindən oxuyur).
        eventsV.clear()
        eventsV.addAll(
            (dataSource as? ServerDataSource)?.eventsM?.toList() ?: d.events,
        )
        // iştiraklar
        joinedV.clear(); joinedV.addAll(d.joinedEventIds)
        // D-169: approval gözləyən iştiraklar serverdən bərpa olunur
        // (əvvəl PENDING joined-a qatılırdı — „Təsdiq gözləyir“ çipi itirdi)
        pendingV.clear(); pendingV.addAll(d.pendingEventIds)
        savedV.clear(); savedV.addAll(d.savedEventIds)
        checkedInV.clear(); checkedInV.addAll(d.checkedInEventIds)
        // iştirakçı sayları (JOINED sayı) — eventsV-də participants sahəsi
        val joinedCount = d.joinedEventIds + d.hostedEventIds
        eventsV.replaceAll { e -> e.copy(participants = if (e.id in joinedCount) e.participants.coerceAtLeast(1) else e.participants) }
        // sosial
        matchesV.clear(); matchesV.addAll(d.matchIds)
        blockedV.clear(); blockedV.addAll(d.blockedIds)
        // D-176: keçirilmiş kartlar və dostluqlar SERVERDƏN bərpa olunur —
        // əvvəl restart-da keçilən kartlar və dostluqlar İTİRİRDİ
        swipesV.clear(); swipesV.putAll(d.swipedPeople)
        friendshipsV.clear(); friendshipsV.putAll(d.friendships)
        // D-179: AI tövsiyə skorları serverdən (null → zolaq gizlədilir)
        aiScoresV = d.aiRecommendations
        // D-178: öz məkan rəylərim (dialoq birdəfəlik)
        myVenueReviewsV.clear(); myVenueReviewsV.putAll(d.myVenueReviews)
        eventMessagesV.clear()
        d.eventMessages.forEach { (k, v) -> eventMessagesV[k] = v.toMutableList() }
        friendMessagesV.clear()
        d.friendMessagePeople.forEach { (k, v) -> friendMessagesV[k] = v.toMutableList() }
        notificationsV.clear(); notificationsV.addAll(d.notifications)
        couponsV.clear(); couponsV.addAll(d.coupons)
        reservationsV.clear(); reservationsV.addAll(d.reservations)
        venueXpV.clear(); venueXpV.putAll(d.venueXp)
        emit()
    }

    /**
     * Serverdən yenidən oxu (pull-to-refresh / re-entry).
     *
     * C (D-170): nəticə state-i — [AppUiState.profileLoaded] /
     * [AppUiState.syncError]. XƏTA SƏSSİZ UDULMUR: onFailure xətanı
     * syncErrorV-yə yazır (UI retry düyməsi göstərir) + Safety.log-a
     * düşür. Test engine-ləri (FakeData fixture) təsirlənmir —
     * PLACEHOLDER build-də funksiya heç başlamır.
     */
    fun refreshFromServer() {
        val s = scope ?: return
        if (!az.qoshul.app.core.backend.BackendConfig.isConfigured) return
        if (az.qoshul.app.core.backend.SupabaseBackend.client == null) return
        s.launch {
            runCatching {
                applyServerData(az.qoshul.app.core.backend.SupabaseReader.loadAll(now()))
                // profil oxundu — onboarded qərarı artıq etibarlıdır
                profileLoadedV = true
                syncErrorV = ""
                emit()
            }.onFailure {
                az.qoshul.app.core.safety.Safety.onCaught?.invoke("refresh", it)
                // C: xəta İSTİFADƏÇİYƏ AYDIN görünür (səssiz udmа yoxdur)
                profileLoadedV = false
                syncErrorV = it.message?.take(160) ?: "Şəbəkə xətası — yenidən cəhd et"
                emit()
            }
        }
    }

    /**
     * Giriş + D-152: dərhal serverdən data yüklə.
     * ƏVVƏLKİ BUG: signIn() yalnız signedInV=true edirdi — data heç vaxt
     * yüklənmirdi, istifadəçi boş ekranlar görürdü („Supabase qoşulmayıb“).
     * Artıq hər giriş yolu (email/OTP/Google/sessiya-bərpası) avtomatik
     * refresh alır.
     */
    fun signIn() {
        signedInV = true
        emit()
        refreshFromServer()
    }

    // D-149: signInDemo() SİLİNDİ — vahid hardcoded demo hesabı (D-050
    // qalığı, demo@qoshul.com sabitləri) istifadəçi APK-sında görünmür.
    // Real axın: SupabaseAuth.signUp/signIn/signInWithGoogle → signIn().

    fun signOut() {
        signedInV = false
        // C: növbəti girişdə profil yenidən serverdən oxunana qədər loading
        profileLoadedV = false
        syncErrorV = ""
        onboardedV = false
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
        // D-152: onboarding addımı serverə PERSIST olunur (restart-da itmir)
        report(
            SyncEvent.SaveProfileIntent(
                firstName = first.ifBlank { null },
                lastName = last.ifBlank { null },
                birthDateIso = birth.toString(),
                gender = gdr.ifBlank { null },
                bio = bioText.ifBlank { null },
                photoCount = photos,
            ),
        )
    }

    fun setInterests(intr: List<String>) {
        interestsV = intr
        emit()
        // D-152: maraqlar serverdə atomik əvəz olunur (slug-larla)
        report(
            SyncEvent.SaveProfileIntent(interests = intr.map { slugifyCategory(it) }),
        )
    }

    /** ROL B axınının sonu: +50 XP, günlük giriş, onboarded. A2: default clock-dan. */
    fun finishOnboarding(today: LocalDate = now().toLocalDate()) {
        completeRegistration()
        onboardedV = true
        dailyLogin(today)
        emit()
        // D-152: onboarded=true serverdə qeyd alınır — restart-dan sonra
        // istifadəçi bir daha onboarding görmür; şəhər/status və s. də
        // bu son paketdə göndərilir (boş olmayanlar).
        report(
            SyncEvent.SaveProfileIntent(
                firstName = firstNameV.takeIf { it.isNotBlank() },
                lastName = lastNameV.takeIf { it.isNotBlank() },
                city = cityV.takeIf { it.isNotBlank() },
                status = statusV.takeIf { it.isNotBlank() },
                gender = genderV.takeIf { it.isNotBlank() },
                bio = bioV.takeIf { it.isNotBlank() },
                onboarded = true,
            ),
        )
    }

    /**
     * Kategoria adı → slug (serverdə categories.slug FK qaydası; D-152).
     * Dəqiq xəritə [Taxonomy.slugOf] — seed ilə birebir (89 kateqoriya).
     */
    private fun slugifyCategory(name: String): String = Taxonomy.slugOf(name)

    // D-151: adoptDemoHostedEvent() SİLİNDİ — "e-mafiya" demo tədbiri
    // (D-026 qalığı) server data ilə əvəz olundu; host adı serverdən gəlir.

    // ═════════════════════════ Profil ═════════════════════════

    fun setProfileName(name: String) {
        profileNameV = name
        val parts = name.trim().split(Regex("\\s+"))
        firstNameV = if (parts.isNotEmpty()) parts.first() else name
        lastNameV = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""
        emit()
    }

    /**
     * D-112 (profil redaktə ekranı): ad + soyad AYRI sahələr — vahid
     * profileName törəməsi "Ad Soyad" (boş soyadda yalnız ad).
     */
    fun setProfileFullName(first: String, last: String) {
        firstNameV = first.trim()
        lastNameV = last.trim()
        profileNameV = listOf(firstNameV, lastNameV)
            .filter { it.isNotEmpty() }.joinToString(" ")
        emit()
        // D-152: profil redaktə persistensiya
        report(
            SyncEvent.SaveProfileIntent(
                firstName = firstNameV.takeIf { it.isNotEmpty() },
                lastName = lastNameV.takeIf { it.isNotEmpty() },
            ),
        )
    }

    /** D-112: profil şəkli seçimi — imagePaint açarı. */
    fun setAvatar(key: String) {
        avatarV = key
        emit()
        // D-152: avatar serverdə photo_urls[0] kəimi saxlanılır
        report(SyncEvent.SetAvatarIntent(key))
    }

    /**
     * D.1 (D-170): profil seçimləri Ayarlardan idarə olunur — hər biri
     * dərhal serverə yazılır (SaveProfileIntent). Onboarding eyni
     * setter-ləri işlədir (mənbə fərqi yoxdur).
     */
    fun setStatus(id: String) {
        statusV = id
        emit()
        report(SyncEvent.SaveProfileIntent(status = id.ifBlank { null }))
    }

    fun setLanguages(langs: List<String>) {
        languagesV = langs
        emit()
        report(SyncEvent.SaveProfileIntent(languages = langs.ifEmpty { null }))
    }

    fun setGroupSize(gs: String) {
        groupSizeV = gs
        emit()
        report(SyncEvent.SaveProfileIntent(groupSize = gs.ifBlank { null }))
    }

    fun setBudget(b: String) {
        budgetV = b
        emit()
        report(SyncEvent.SaveProfileIntent(budget = b.ifBlank { null }))
    }

    fun setCity(name: String) {
        cityV = name
        emit()
        // D-152: şəhər dəyişimi serverə yazılır
        report(SyncEvent.SaveProfileIntent(city = name.takeIf { it.isNotBlank() }))
    }

    fun setMapVisibility(v: MapVisibility) {
        mapVisibilityV = v
        emit()
        // H.1 (D-170): görünmə seçimi SERVERƏ yazılır (EVERYONE/FRIENDS/OFF);
        // server RLS ikinci cihazdan baxışda tətbiq edir, klient süzgəci
        // yalnız ikinci qatdır (server avtoriteti)
        report(SyncEvent.PrefsIntent(mapVisibility = v.name))
    }

    /**
     * G.1 (D-170): davamlı lokasiya axınından gələn real fix → server.
     * Throttle/dəqiqlik filtri QoshulLocation-da (≤50m, ≥15m hərəkət,
     * 4s interval) — bura yalnız TƏMİZ fix-lər çatır.
     */
    fun reportLocation(lat: Double, lng: Double) {
        report(SyncEvent.LocationUpdateIntent(lat, lng))
    }

    fun setShowDistance(v: Boolean) {
        showDistanceV = v
        emit()
        // H.1 (D-170): məsafə göstərişi serverdə saxlanılır
        report(SyncEvent.PrefsIntent(showDistance = v))
    }

    // ═════════════════════════ Dizayn (D-118/D-119) ═════════════════════════

    /**
     * D-119 (direktiv 2026-11-10): dizayn seçimi — brend rəngi (Yaşıl default /
     * Qızılı) və mod (Ağ default / Qara). Setter Q.apply çağırır — bütün tokenlər
     * snapshot-state olduğundan bütün kompozisiyalar dərhal yenidən çəkilir.
     * (Demo mərhələsi: digər ayarlar kimi — şəhər, görünüş rejimi — sessiya
     * daxilində saxlanılır, saxlamanın D-079 backend-i gələndə davam edəcək.)
     */
    fun setDesignColor(c: DesignColor) {
        if (designColorV == c) return
        designColorV = c
        Q.apply(designColorV, designModeV)
        emit()
    }

    fun setDesignMode(m: DesignMode) {
        if (designModeV == m) return
        designModeV = m
        Q.apply(designColorV, designModeV)
        emit()
    }

    // ═════════════════════════ Tədbir əməliyyatları (ROL TAB 3) ═════════════════════════

    fun join(id: String): Boolean {
        val e = eventById(id) ?: return false
        if (e.past || e.isFull || joinedV.contains(id)) return false
        // D-169: qoşulma SERVER-ə gedir (əvvəl yalnız lokal idi —
        // restart-da iştirak İTİRDİ; server FULL/PAST/approval yoxlaması
        // avtoritativdir, lokal optimistic UI dərhal göstərir)
        report(SyncEvent.JoinEventIntent(id))
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
        // D-169: çıxış serverdə (status LEFT) — əvvəl yalnız lokal idi
        report(SyncEvent.LeaveEventIntent(id))
        joinedV.remove(id)
        pendingV.remove(id)
        emit()
    }

    fun toggleSave(id: String) {
        // D-169: saxla/sil serverdə (saved_events) — əvvəl yalnız lokal idi
        report(SyncEvent.SaveEventIntent(id))
        if (!savedV.remove(id)) savedV.add(id)
        emit()
    }

    /** ROL TAB 3: məkanda QR/yerləşmə ilə check-in → +40 XP + nişanlar. */
    fun checkInEvent(id: String, at: LocalDateTime = now()): Boolean {
        val e = eventById(id) ?: return false
        if (!joinedV.contains(id) || checkedInV.contains(id)) return false
        report(SyncEvent.CheckInIntent(id, lat = null, lng = null))
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
                date = dateLabelFor(epochDay.toLong(), today()),
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
        report(
            SyncEvent.CreateEventIntent(
                localId = id, title = title, category = category, city = city,
                location = if (format == "Onlayn") "Onlayn görüş" else location,
                dateIso = java.time.LocalDate.ofEpochDay(epochDay.toLong()).toString(),
                time = time, capacity = capacity, description = description,
                priceAzn = if (price > 0) price else null,
                ageLimit = ageLimit, language = language, level = level,
                format = format, approval = approval, recurring = recurring,
                lat = lat ?: c?.lat, lng = lng ?: c?.lng, venueId = null,
                imageKey = dataSource.imageForCategory(category),
            )
        )
        emit()
    }

    /**
     * ROL F: rəy +15, həftədə max 3. A2: həftə dönüşündə sıfırlanır; default clock-dan.
     *
     * N3 (M0): həftə slotu yalnız REAL mükafatdan sonra yanır — eyni tədbirə
     * təkrar rəy (award idempotensiyası) slot YANDIRMIR. Əlavə: tədbir
     * mövcud olmalıdır (naməlum ID rədd).
     */
    fun addReview(eventId: String, rating: Int, comment: String, today: LocalDate = now().toLocalDate()): Boolean {
        if (eventById(eventId) == null) return false
        report(SyncEvent.AddReviewIntent(eventId, rating, comment))
        val key = weekKey(today)
        if (key != reviewXpWeekKey) {
            reviewXpWeekKey = key
            reviewXpThisWeek = 0
        }
        if (xpEventIds.contains("review-$eventId")) return false // N3: təkrar — slot yanmır
        if (reviewXpThisWeek >= XpRules.REVIEW_XP_WEEKLY_CAP) return false
        if (!award("review-$eventId", XpRules.REVIEW_XP)) return false
        reviewXpThisWeek++
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

    /**
     * ROL D: "Maraqlıyam"/"Keç". D-176 (ikinci dalğa): match qərarı SERVERDƏN
     * gəlir — fn_record_swipe cavabındakı `matched` bayrağı. Köhnə klient
     * tərəfli `Person.likesBack` proqnozu SİLİNDİ (server avtoriteti, qayda 8).
     *
     * Axın (maraq bildiriləndə): TƏK RPC — fn_record_swipe birbaşa çağırılır
     * (ikili güzgü report() YOX — ikiqat yazma olmazdı). Cavab:
     *  • MATCHED     → optimistic match UI + XP (serverdə onsuz da hazır);
     *  • NOT_MATCHED → swipe serverdə qeyd olundu, match yoxdur;
     *  • ERROR       → KART GERİ QAYIDIR (swipesV-dən silinir — istifadəçi
     *                  yenidən swipe edə bilər), xəta istifadəçiyə görünür;
     *  • OFFLINE     → server yoxdur (test engine / PLACEHOLDER) — güzgü
     *                  intent-i çıxır, match məlum deyil (false).
     */
    fun swipe(personId: String, interested: Boolean): Boolean {
        if (swipesV.containsKey(personId) || blockedV.contains(personId)) return false
        swipesV[personId] = interested
        if (!interested) {
            report(SyncEvent.SwipeIntent(personId, interested = false, matched = false))
            emit()
            return false
        }
        val p = personById(personId)
        if (p == null) {
            // D-176: naməlum ID — kartı keçmək olmaz, rollback (RPC getmir)
            swipesV.remove(personId)
            return false
        }
        when (swipeRpc(personId)) {
            SwipeOutcome.OFFLINE -> {
                // server yoxdur — yalnız güzgü intent-i (test/PLACEHOLDER)
                report(SyncEvent.SwipeIntent(personId, interested = true, matched = false))
                emit()
                return false
            }
            SwipeOutcome.ERROR -> {
                // D-176: RPC xətası — KART GERİ QAYIDIR + xəta görünür
                swipesV.remove(personId)
                syncErrorV = "Swipe göndərilə bilmədi — kart geri qayıtdı, yenidən cəhd et"
                emit()
                return false
            }
            SwipeOutcome.NOT_MATCHED -> {
                emit()
                return false
            }
            SwipeOutcome.MATCHED -> {
                // Match! → bildiriş + chat + XP (gündə max 3; serverdə də var)
                matchesV.add(personId)
                friendshipsV[personId] = "accepted"
                friendMessagesV.getOrPut(personId) { mutableListOf() }
                lastMatchedPersonIdV = personId
                notify(
                    NotifType.MATCH,
                    "Yeni match: ${p.fullName}",
                    "Qarşılıqlı maraq yarandı — söhbəti başlat!",
                )
                val today = now().toLocalDate()
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
        }
    }

    /** D-176: fn_record_swipe RPC nəticəsi. */
    enum class SwipeOutcome { MATCHED, NOT_MATCHED, ERROR, OFFLINE }

    /** Server təsdiqli son match (UI dialoq üçün; consume ilə təmizlənir). */
    private var lastMatchedPersonIdV: String? = null

    /**
     * D-176: swipe RPC — fn_record_swipe birbaşa (sinxron, timeout qorumalı).
     * Match dialoqu kart uçuş animasiyası ilə birgə açılmalıdır (UX qərarı);
     * RPC xətası/timeout → ERROR (kart geri qayıdır). Test engine-lər
     * (scope=null / konfiqurasiyasız) → OFFLINE (güzgü report() gedir).
     * [swipeOutcomeOverride] — testlər nəticəni inyeksiya edir (SwipeServerTest).
     */
    internal var swipeOutcomeOverride: ((personId: String) -> SwipeOutcome)? = null

    internal fun swipeRpc(personId: String): SwipeOutcome {
        swipeOutcomeOverride?.let { return it(personId) }
        if (scope == null) return SwipeOutcome.OFFLINE
        if (!az.qoshul.app.core.backend.BackendConfig.isConfigured) return SwipeOutcome.OFFLINE
        if (az.qoshul.app.core.backend.SupabaseBackend.client == null) return SwipeOutcome.OFFLINE
        val call = az.qoshul.app.core.backend.SupabaseSync.mapEvent(
            SyncEvent.SwipeIntent(personId, interested = true, matched = false),
        ) ?: return SwipeOutcome.ERROR
        return kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(SWIPE_RPC_TIMEOUT_MS) {
                val r = az.qoshul.app.core.backend.SupabaseBackend.rpcNow(call).getOrNull()
                // fn_record_swipe → [{"matched": true, "awarded_xp": true}]
                val arr = r as? kotlinx.serialization.json.JsonArray
                val obj = arr?.firstOrNull() as? kotlinx.serialization.json.JsonObject
                val prim = obj?.get("matched") as? kotlinx.serialization.json.JsonPrimitive
                when (prim?.content) {
                    "true" -> SwipeOutcome.MATCHED
                    "false" -> SwipeOutcome.NOT_MATCHED
                    else -> SwipeOutcome.ERROR
                }
            } ?: SwipeOutcome.ERROR // timeout
        }
    }

    /** D-176: swipe RPC gözləmə həddi (kart animasiyası ~300ms; ANR qoruması). */
    private companion object {
        const val SWIPE_RPC_TIMEOUT_MS = 5000L
    }

    /** D-176: son server-təsdiqli match ID-si (MatchScreen dialoqu açır). */
    fun consumeLastMatch(): String? {
        val v = lastMatchedPersonIdV
        lastMatchedPersonIdV = null
        return v
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
            if (coupon == null || !coupon.isActive(now()) || coupon.venueId != venue.id) return null
        }
        val id = "local-${'$'}{reservationsV.size + 1}"
        val resv = Reservation(
            id = id, venueId = venue.id, venueName = venue.name, venueImage = venue.image,
            date = date, time = time, party = party, note = note,
            couponCode = coupon?.code,
            // I.2 (D-170): QR KODU SERVERDƏ YARANIR (fn_create_reservation —
            // CSPRNG). Klient mock QR ÜRETMİR: boş string = "kod gəlir";
            // refreshFromServer rezervasiyanı server ID/QR ilə əvəz edir.
            qrCode = "",
            createdAt = now(),
            status = if (venue.autoAccept) ResvStatus.CONFIRMED else ResvStatus.PENDING,
        )
        reservationsV.add(0, resv)
        // M1: QR/share token serverdə yaranır (N1/N8) — bu görüntü yalnız lokal
        report(
            SyncEvent.CreateReservationIntent(
                venueId = venue.id,
                dateIso = dateLabelToIso(date, today()) ?: today().toString(),
                time = time, party = party, note = note,
                couponCode = coupon?.code, localId = id,
            )
        )
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
        report(SyncEvent.UseReservationIntent(resv.qrCode))
        resv.status = ResvStatus.USED
        award("resv-$id", XpRules.RESERVATION_XP)
        if (resv.couponCode != null) {
            couponsV.filter { it.code == resv.couponCode }.forEach { it.used = true }
        }
        emit()
        return true
    }

    /** ROL E-7: ləğv — server qaydası (fn_cancel_reservation) avtoritativdir. */
    fun cancelReservation(id: String) {
        reservationsV.filter { it.id == id && it.canCancel }.forEach { r ->
            report(SyncEvent.CancelReservationIntent(id))
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
            report(SyncEvent.NoShowIntent(id))
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

    /**
     * D-026 (L.6): XP ilə kupon al — həmin obyektin Endirim XP balansından.
     *
     * N2/A3 (M0): köhnə 5 səssiz-null yolu typed nəticə ilə GÖRÜNƏNDİR —
     * [CouponPurchase.Failure.error] səbəbi UI-da izahat kimi göstərilir.
     * B6/istifadəçi qərarı (2026-11-07): balansda VAR OLANDAN çox xərcləmək
     * olmaz — kupon diskret alışdır, qismən alınmır → REDD.
     */
    fun buyCoupon(offerId: String): CouponPurchase {
        report(SyncEvent.BuyCouponIntent(offerId))
        val offer = offersV.firstOrNull { it.id == offerId }
            ?: return CouponPurchase.Failure(SpendError.OFFER_NOT_FOUND)
        if (offer.autoByLevel) return CouponPurchase.Failure(SpendError.AUTO_ONLY)
        if (!offer.unlockedFor(rankIndex)) return CouponPurchase.Failure(SpendError.LEVEL_LOCKED)
        val acct = venueXp[offer.venueId]
            ?: return CouponPurchase.Failure(SpendError.NO_ACCOUNT)
        if (acct.balance < offer.xpPrice) return CouponPurchase.Failure(SpendError.INSUFFICIENT_BALANCE)
        // B6: spend artıq mənfiləşmir — uğursuzluq (balans preview arası
        // dəyişibsə) yenə balans xətası kimi görünəndir.
        if (!acct.spend(offer.xpPrice, "Kupon alındı: ${offer.label}", "xl${ledgerSeq++}", now())) {
            return CouponPurchase.Failure(SpendError.INSUFFICIENT_BALANCE)
        }
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
        return CouponPurchase.Success(coupon)
    }

    /** Bu məkanda rezervə tətbiq edilə bilən aktiv kuponlar (ROL E-3). A2: now parametrli. */
    fun activeCouponsFor(venueId: String): List<UserCoupon> =
        couponsV.filter { it.isActive(now()) && it.venueId == venueId }

    // ═════════════════════════ Endirim XP (D-026, product-spec L) ═════════════════════════

    /**
     * L.4-4: xərcləmə önizləməsi — məzənnə + limit yoxlaması.
     *
     * A1 (M0): xəta açarları TYPED ([SpendError]) — köhnə "hesap" tipo-
     * orfoqrafiyası UI-da `else -> ""` ilə səssiz yox olurdu.
     * C1 (istifadəçi qərarı, 2026-11-07): cap/balans aşımında əməliyyat
     * REDD edilir (CLAMP deyil) — "yalnız hesabda var olan qədəri istifadə
     * oluna bilər".
     */
    fun previewSpend(venueId: String, xpAmount: Int, billAzn: Double): SpendPreview {
        report(SyncEvent.PreviewSpendIntent(venueId, xpAmount, billAzn))
        val acct = venueXp[venueId]
            ?: return SpendPreview(false, 0.0, SpendError.NO_ACCOUNT)
        if (xpAmount <= 0 || billAzn <= 0) {
            return SpendPreview(false, 0.0, SpendError.INVALID_INPUT)
        }
        // D-061 "nə olar əgər": xp_per_azn konfiqurasiyası 0/negativ gəlsə
        // (gələcək server xp_config cədvəli) sıfıra bölmə/Infinity olmasın.
        if (acct.config.xpPerAzn <= 0) {
            return SpendPreview(false, 0.0, SpendError.BAD_CONFIG)
        }
        if (acct.balance < xpAmount) return SpendPreview(false, 0.0, SpendError.INSUFFICIENT_BALANCE)
        if (acct.config.minBillAzn > 0 && billAzn < acct.config.minBillAzn) {
            return SpendPreview(false, 0.0, SpendError.BILL_BELOW_MIN)
        }
        val discount = xpAmount.toDouble() / acct.config.xpPerAzn
        val cap = billAzn * acct.config.maxDiscountPercent / 100
        if (discount > cap + 0.001) {
            return SpendPreview(false, cap, SpendError.CAP_EXCEEDED)
        }
        return SpendPreview(true, discount, null)
    }

    /** L.4-5: atomik xərcləmə — double-spend qadağan (serverdə fn_spend_venue_xp). */
    fun spendVenueXp(
        venueId: String, xpAmount: Int, billAzn: Double,
        note: String = "Kassada XP endirimi",
    ): Double? {
        report(SyncEvent.SpendIntent(venueId, xpAmount, billAzn, note))
        val p = previewSpend(venueId, xpAmount, billAzn)
        if (!p.ok) return null
        // D-061 "nə olar əgər" — preview ilə spend arası hesab yoxa çıxsa
        // (gələcək server sinxronu) !! əvəzinə təyin olunmuş null qayıdışı.
        val acct = venueXp[venueId] ?: return null
        // B6 (M0): atomik xərcləmə — spend mənfi balansa yol vermir;
        // uğursuzluq (preview arası balans dəyişibsə) səssiz deyil.
        if (!acct.spend(xpAmount, note, "xl${ledgerSeq++}", now())) return null
        notify(
            NotifType.COUPON_EXPIRING,
            "XP endirimi tətbiq olundu",
            "${acct.venueName} · $xpAmount XP → " +
                String.format(java.util.Locale.US, "%.2f", xpAmount.toDouble() / acct.config.xpPerAzn) +
                " ₼ endirim. Receipt ledger-ə yazıldı.",
        )
        emit()
        return p.discount
    }

    // — L.4-1: müştərinin dinamik XP kartı (60s, tək istifadəlik) —

    fun regenerateXpCard(venueId: String) {
        report(SyncEvent.XpCardIssueIntent(venueId))
        // I.2 (D-170): nonce SERVERDƏ yaranır (fn_issue_xp_card_nonce —
        // pgcrypto CSPRNG). Klient köhnə mockCode-ı GÖSTƏRMİR: kart yalnız
        // server cavabından (applyServerData.xpCard) qurulur. Optimistic
        // boş kart = "yaradılır" vəziyyəti — refresh gələndə dolur.
        xpCardV = null
        xpCardVenueIdV = venueId
        emit()
    }

    /** Dinamik QR işlənib → təkrar istifadə mümkün deyil (L.4 qorunması). */
    fun markXpCardUsed() {
        xpCardV?.let { report(SyncEvent.XpCardConsumeIntent(it.nonce)) }
        xpCardV?.used = true
        emit()
    }

    /** XP kartı etibarlıdırmı (venue uyğun + vaxt bitməyib + işlənməyib). */
    fun xpCardValidFor(venueId: String): Boolean =
        // D-061: !! zənciri əvəzinə null-safe (vəziyyət paralel dəyişə bilər)
        xpCardV?.let { card -> xpCardVenueIdV == venueId && card.isValid(now()) } == true

    /**
     * D-175 (ikinci dalğa): XP kartının vaxtı bitəndə avtomatik RE-ISSUE.
     * XpCardScreen kartı açıq saxladıqda hər saniyə çağırılır (tick) —
     * vaxtı bitmiş kart (60s TTL) istifadəçinin əl ilə „Yenilə“ basmağını
     * gözləmir, serverdən yenisi dərhal istənir. Yalnız aktiv ekranda
     * işləyir (tick mənbəyi XpCardScreen-in LaunchedEffect-i).
     */
    fun reissueExpiredXpCard() {
        val venueId = xpCardVenueIdV ?: return
        val card = xpCardV ?: return
        if (!card.isValid(now()) && !card.used) {
            regenerateXpCard(venueId)
        }
    }

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

        // F2 (audit): server axını artıq fn_finish_activity-dir — event ID +
        // taq edilmiş iştirakçılar gedir; məbləğ serverdə sabitlənib.
        report(SyncEvent.GrantVenueXpIntent(acct.venueId, e.id, taggedIds))

        // Təşkilatçı mükafatı — obyekt bazasına grant (A2: at = vahid clock).
        acct.grant(
            XpRules.ORGANIZER_XP,
            "Təşkilatçı mükafatı — „${e.title}“",
            "xl${ledgerSeq++}",
            now(),
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
            "+${XpRules.ORGANIZER_XP} Endirim Qoşa Coin qazandın",
            "${acct.venueName} · „${e.title}“ bitirildi. Şəkillər obyektin " +
                "biznes hesabına düşdü; ${taggedIds.size} iştirakçı taq edildi " +
                "(hər birinə +${XpRules.TAGGED_PARTICIPANT_XP} Qoşa Coin).",
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

    /** ROL H: tədbir xatırlatması (24s / 2s əvvəl) — m4 jobs cron tetikləyir. */
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
        report(SyncEvent.BlockIntent(personId, blocked = true))
        blockedV.add(personId)
        friendshipsV.remove(personId)
        friendMessagesV.remove(personId)
        matchesV.remove(personId)
        emit()
    }

    /**
     * D-151 (master-spec bölmə 3): unmatch — uyğunluğu ləğv edir. Profil
     * aktiv matçlar və təkliflər siyahısından dərhal silinir (StateFlow
     * real-vaxt yenilənmə), blok siyahısına DÜŞMÜR (yumşaq ləğv).
     */
    fun unmatch(personId: String) {
        matchesV.remove(personId)
        friendshipsV.remove(personId)
        friendMessagesV.remove(personId)
        swipesV[personId] = false
        emit()
    }

    fun unblockUser(personId: String) {
        report(SyncEvent.BlockIntent(personId, blocked = false))
        blockedV.remove(personId)
        emit()
    }

    /** ROL I: report — eyni istifadəçiyə 3+ → avtomatik məhdudlaşdırma. */
    fun reportUser(personId: String, reason: String, note: String = "") {
        report(SyncEvent.ReportIntent("USER", personId, reason, note))
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
        report(SyncEvent.MessageIntent("EVENT", eventId, null, text))
        val list = eventMessagesV.getOrPut(eventId) { mutableListOf() }
        list.add(ChatMessage("Sən", text, mine = true))
        emit()
    }

    fun sendFriendMessage(personId: String, text: String) {
        report(SyncEvent.MessageIntent("PERSONAL", null, personId, text))
        val list = friendMessagesV.getOrPut(personId) { mutableListOf() }
        list.add(ChatMessage("Sən", text, mine = true))
        emit()
    }

    /** ROL TAB 4: "Söhbəti sil" — şəxsi söhbəti bağlayır (lokal UI state). */
    fun deleteChat(personId: String) {
        friendMessagesV.remove(personId)
        friendshipsV.remove(personId)
        emit()
    }

    /** Test köməkçisi: ləngimiş cavabların gözlənilməməsi üçün (keçidlilik). */
    fun deliverPendingReplies() {
        replyJobs.forEach { it.cancel() }
        replyJobs.clear()
    }

    // — insanlar —
    /**
     * D-176: dostluq sorğusu SERVERƏ gedir (fn_request_friend). Optimistic
     * „sent“ statusu — RPC xətasında reportFailure rollback edir və
     * refreshFromServer server həqiqətini qaytarır.
     */
    fun requestFriend(personId: String) {
        friendshipsV[personId] = "sent"
        emit()
        report(SyncEvent.RequestFriendIntent(personId))
    }

    /**
     * D-176: gələn sorğuya cavab SERVERƏ gedir (fn_respond_friend).
     * [accept]=true → „accepted“, əks halda sorğu silinir.
     */
    fun acceptFriend(personId: String, accept: Boolean = true) {
        if (accept) {
            friendshipsV[personId] = "accepted"
        } else {
            friendshipsV.remove(personId)
        }
        emit()
        report(SyncEvent.RespondFriendIntent(personId, accept))
    }

    val acceptedFriends: List<Person>
        get() = dataSource.people.filter { p ->
            friendshipsV[p.id] == "accepted" && !blockedV.contains(p.id) &&
                !restrictedUsersV.contains(p.id)
        }

    // ═════════════════════════ Assistant (server axını, D-175) ═════════════════════════

    /**
     * D-175 (ikinci dalğa): „Qoşa“ köməkçisi — cavab SERVERDƏN gəlir
     * (fn_assistant_reply RPC). Köhnə klient tərəfli assistantReplyFor
     * şablon generatoru TAM SİLİNDİ (qayda 8 — məntiq serverdə).
     *
     * Axın: sual → optimistic „Sən“ mesajı + busy → RPC (scope daxilində)
     * → cavab gələndə tam mətn bir dəfəyə əlavə olunur (yazı simulyasiyası
     * YOXDUR — typing yalnız Supabase Realtime kanalı ilə görünür).
     * Xətada: köməkçi XƏTA KARTI görünür (syncError-a da düşür — Snackbar).
     * Server konfiqurasiyasızdırsa (PLACEHOLDER/test) dərhal xəta kartı.
     */
    fun sendAssistant(text: String) {
        if (text.isBlank() || assistantBusyV) return
        assistantMsgsV.add(ChatMessage("Sən", text.trim(), mine = true))
        assistantBusyV = true
        assistantErrorV = ""
        emit()

        val s = scope
        if (s == null ||
            !az.qoshul.app.core.backend.BackendConfig.isConfigured ||
            az.qoshul.app.core.backend.SupabaseBackend.client == null
        ) {
            // Server yoxdur — köməkçi xəta kartı (uydurma cavab YOX)
            assistantBusyV = false
            assistantErrorV = "Köməkçi serverə qoşula bilmədi — internet bağlantını yoxla"
            emit()
            return
        }
        assistantJob?.cancel()
        assistantJob = s.launch {
            // Safety.of bloku non-suspend olduqlarından rpcNow (suspend)
            // burada birbaşa try/catch ilə çağırılır
            val reply = try {
                val call = az.qoshul.app.core.backend.SupabaseSync.mapEvent(
                    SyncEvent.AssistantIntent(text.trim()),
                )
                if (call == null) "" else {
                    val r = az.qoshul.app.core.backend.SupabaseBackend.rpcNow(call).getOrNull()
                    // fn_assistant_reply → "cavab mətni" (skalar string)
                    (r as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                }
            } catch (t: Throwable) {
                az.qoshul.app.core.safety.Safety.onCaught?.invoke("assistant-rpc", t)
                ""
            }
            if (reply.isBlank()) {
                assistantErrorV = "Cavab alınmadı — bir azdan yenidən yaz"
                az.qoshul.app.core.safety.Safety.onCaught?.invoke(
                    "assistant",
                    IllegalStateException("fn_assistant_reply boş cavab"),
                )
            } else {
                assistantMsgsV.add(ChatMessage("Qoşa", reply))
            }
            assistantBusyV = false
            emit()
        }
    }

    /**
     * „AI ilə seç“ (Sənin üçün) — D-179 (ikinci dalğa): skor SERVERDƏN
     * gəlir (fn_recommend_events → aiScoresV; applyServerData yazır).
     * RPC yoxdursa / xəta verərsə aiScoresV=null → BOŞ siyahı qayıdır →
     * „AI seçimi“ zolağı GİZLƏDİLİR (klient tərəfli 2-bit skor SİLİNDİ).
     * Səbəb izahları server skoruna görə: 2 = maraq uyğunluğu, 1 = yer az.
     */
    fun aiRecommendations(): List<Pair<QoshulEvent, String>> {
        val scores = aiScoresV ?: return emptyList()
        return cityEvents
            .mapNotNull { e -> scores[e.id]?.let { e to it } }
            .sortedByDescending { it.second }
            .take(3)
            .map { (e, score) ->
                e to when {
                    score >= 2 -> "Profilindəki maraqlarına uyğundur."
                    score == 1 -> "Yerlər az qalıb — tez qoşul."
                    else -> "Yeni başlayanlar üçün uyğun səviyyə."
                }
            }
    }

    /** Şəkil/qrup köməkçiləri UI üçün (D-050: mənbəyə yönəldilmiş). */
    fun statusLabel(id: String): String = dataSource.statusLabel(id)

    /** Bütün məkanlar (server rejimində serverdən gələn siyahı). */
    val partners: List<Partner> get() = dataSource.partners

    /** Məkan ID ilə (server rejimində server siyahısından). */
    fun venueById(id: String): Partner? = dataSource.partners.firstOrNull { it.id == id }

    /** Məkan rəyləri (D-178: readVenueReviews-dən — yalnız APPROVED). */
    fun venueReviews(venueId: String) = dataSource.venueReviewsSeed[venueId] ?: emptyList()

    /** D-178: bu məkana rəy yazıbmı? (serverdən — dialoq birdəfəlik). */
    fun venueReviewedByMe(venueId: String): Boolean = myVenueReviewsV[venueId] == true

    /** D-178: serverdən oxunan öz rəy statusları (applyServerData yazır). */
    private val myVenueReviewsV = mutableMapOf<String, Boolean>()

    /**
     * D-178: məkan rəyi SERVERƏ yazılır (fn_add_venue_review — PENDING,
     * moderasiyadan sonra hamıya görünür). Klient tərəfdə rəy siyahısına
     * DƏRHAL ƏLAVƏ OLUNMUR (moderasiya gecikməsi var — yalnız APPROVED
     * rəylər oxunur); „yazıldı“状态 yalnız myVenueReviewsV ilə görünür.
     */
    fun addVenueReview(venueId: String, stars: Int, body: String): Boolean {
        if (stars !in 1..5) return false
        if (myVenueReviewsV[venueId] == true) return false
        myVenueReviewsV[venueId] = true
        emit()
        report(SyncEvent.AddVenueReviewIntent(venueId, stars, body.trim()))
        return true
    }

    /** Məkan rezerv slotları. */
    fun venueSlots(venueId: String) = dataSource.venueSlotTimes[venueId] ?: emptyList()
}

/** UI-nin oxuduğu immutabl snapshot. */
data class AppUiState(
    val version: Long,
    // auth/onboarding
    val signedIn: Boolean,
    val onboarded: Boolean,
    /** C (D-170): profil serverdən oxunub? false → loading ekranı. */
    val profileLoaded: Boolean = false,
    /** C (D-170): son sync xətası (boş = xəta yoxdur). */
    val syncError: String = "",
    val profileName: String,
    val firstName: String,
    val lastName: String,
    /** D-112: profil şəkli açarı (imagePaint). */
    val avatar: String,
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
    // dizayn (D-119)
    val designColor: DesignColor,
    val designMode: DesignMode,
    // XP 1
    val xp: Int,
    /** D-155 TaskCenter: verilmiş XP açarları (prefiks uyğunluğu ilə yoxlanır). */
    val awardedKeys: Set<String> = emptySet(),
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
    /** D-175: son köməkçi xətası (boş = xəta yoxdur; UI xəta kartı göstərir). */
    val assistantError: String = "",
    /** D-179: AI tövsiyə RPC mövcuddurmu? false → „AI seçimi“ zolağı gizlədilir. */
    val aiRecommendationsAvailable: Boolean = false,
    // bitmiş aktivitelər
    val finishedActivities: Set<String>,
) {
    val rankIndex: Int get() = XpRules.rankIndexForXp(xp)
    val rank: String get() = XpRules.rankForXp(xp)
    val badgeList: List<BadgeDef> get() = badgeDefs

    /** U2.4 (UI-11): oxunmamış sayı STATE-dən törəmədir — badge reaktiv yenilənir
     *  (əvvəl: AppShell engine.unreadCount getter-indən oxuyurdu — state dəyişsə
     *  də recompose tetikleyə bilmirdi). */
    val unreadCount: Int get() = notifications.count { !it.read }
}
