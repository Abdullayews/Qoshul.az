package az.qoshul.app.core.data

import az.qoshul.app.core.model.AppNotification
import az.qoshul.app.core.model.CategoryGroupData
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.model.City
import az.qoshul.app.core.model.CouponOffer
import az.qoshul.app.core.model.ImageKey
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.VenueXpAccount

/** Məkan rəyi (ROL E) — data modeli. */
data class VenueReview(val author: String, val stars: Int, val text: String)

/**
 * Data mənbəyi kontraktı.
 *
 * D-151 (2026-11-25, direktiv: „demo məntiqi heç olmasın“): tətbiq YALNIZ
 * server rejimindədir. Başlanğıc dəst boşdur — bütün istifadəçi/sosial data
 * [az.qoshul.app.core.backend.SupabaseReader] ilə canlı serverdən oxunur və
 * [AppEngine.applyServerData] ilə tətbiq olunur. Taksonomiya (kateqoriya
 * qrupları, şəhərlər, onboarding siyahıları) tətbiq məzmunudur — [Taxonomy].
 *
 * Qeyd: JVM/Robolectric testləri üçün `app/src/test` qovluğunda
 * FakeQoshulDataSource mövcuddur (test fixture) — istehsal APK-sına düşmür.
 */
interface QoshulDataSource {
    val events: List<QoshulEvent>
    val people: List<Person>
    val partners: List<Partner>
    val cities: List<City>
    val categoryGroups: List<CategoryGroupData>
    val offers: List<CouponOffer>
    val initialNotifications: List<AppNotification>
    val initialEventMessages: Map<String, List<ChatMessage>>
    val initialFriendMessages: Map<String, List<ChatMessage>>
    val venueSlotTimes: Map<String, List<String>>

    /** Hər çağırışda FRESH hesablar (mutable olduqlarından paylaşıla bilməz). */
    fun initialVenueXp(): Map<String, VenueXpAccount>

    /** Məkan rəyləri — serverdən gəlir. */
    val mockVenueReviews: Map<String, List<VenueReview>>
        get() = emptyMap()

    // ── Taksonomiya (tətbiq məzmunu — [Taxonomy] mənbəyi) ──

    /** Bütün kateqoriyalar (qruplardan düzənir). */
    val allCategories: List<String>
        get() = Taxonomy.allCategories

    /** Kateqoriyanın qrup adı ("Qəhvə" → "Yemək və içki"). */
    fun groupOf(category: String): String = Taxonomy.groupOf(category)

    /** Yeni tədbir üçün şəkil açarı — qrupa görə (lokal asset xəritəsi). */
    fun imageForCategory(category: String): String = when (groupOf(category)) {
        "İdman", "Təbiət" -> ImageKey.PADEL
        "Yemək və içki" -> ImageKey.COFFEE
        "Texnologiya", "Biznes və karyera", "Təhsil" -> ImageKey.TECH
        else -> ImageKey.FRIENDS
    }

    /** Şəhər adından koordinat (naməlum → null; çağıran fallback edir). */
    fun findCity(name: String): City? = Taxonomy.findCity(name)

    // Onboarding statik siyahıları (web onboarding.ts)
    val statusLabels: Map<String, String>
        get() = Taxonomy.statusLabels
    val statuses: List<String>
        get() = Taxonomy.statuses
    val universities: List<String>
        get() = Taxonomy.universities
    val languageOptions: List<String>
        get() = Taxonomy.languageOptions
    val languageLevels: List<String>
        get() = Taxonomy.languageLevels
    val goals: List<String>
        get() = Taxonomy.goals
    val availabilityOptions: List<String>
        get() = Taxonomy.availabilityOptions
    val groupSizes: List<String>
        get() = Taxonomy.groupSizes
    val budgets: List<String>
        get() = Taxonomy.budgets
    val traitLabels: Map<String, String>
        get() = Taxonomy.traitLabels
    val personalityQuestions: List<Pair<String, Pair<String, Boolean>>>
        get() = Taxonomy.personalityQuestions
    val rolCategories: List<String>
        get() = Taxonomy.rolCategories
    val rolCategoryHints: Map<String, String>
        get() = Taxonomy.rolCategoryHints

    /** Status ID → görünən etiket (statusLabels xəritəsindən). */
    fun statusLabel(id: String?): String = Taxonomy.statusLabel(id)
}

/**
 * İstehsal mənbəyi (D-151): başlanğıc state TAM BOŞ — bütün məlumat
 * serverdən oxunur ([SupabaseReader] → [AppEngine.applyServerData]).
 * Konteynerlər MUTABLE-dır: applyServerData girişdən sonra serverdən
 * gələn datanı buraya yazır, engine getterləri `dataSource` üzərindən
 * oxuyur. Taksonomiya interfeys default-metodlarından [Taxonomy]-yə düşür.
 */
object ServerDataSource : QoshulDataSource {
    // Taksonomiya — Taxonomy mənbəyindən (rejimdən asılı deyil)
    override val cities: List<City> get() = Taxonomy.cities
    override val categoryGroups: List<CategoryGroupData> get() = Taxonomy.categoryGroups

    val eventsM = mutableListOf<QoshulEvent>()
    val peopleM = mutableListOf<Person>()
    val partnersM = mutableListOf<Partner>()
    val offersM = mutableListOf<CouponOffer>()
    val notificationsM = mutableListOf<AppNotification>()
    val eventMessagesM = mutableMapOf<String, List<ChatMessage>>()
    val friendMessagesM = mutableMapOf<String, List<ChatMessage>>()
    val venueSlotsM = mutableMapOf<String, List<String>>()

    override val events: List<QoshulEvent> get() = eventsM.toList()
    override val people: List<Person> get() = peopleM.toList()
    override val partners: List<Partner> get() = partnersM.toList()
    override val offers: List<CouponOffer> get() = offersM.toList()
    override val initialNotifications: List<AppNotification> get() = notificationsM.toList()
    override val initialEventMessages: Map<String, List<ChatMessage>> get() = eventMessagesM.toMap()
    override val initialFriendMessages: Map<String, List<ChatMessage>> get() = friendMessagesM.toMap()
    override val venueSlotTimes: Map<String, List<String>> get() = venueSlotsM.toMap()
    override fun initialVenueXp(): Map<String, VenueXpAccount> = emptyMap()
    override val mockVenueReviews: Map<String, List<VenueReview>> = emptyMap()
}
