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

/** Məkan rəyi (ROL E) — fake və server implementasiyalarının ortaq modeli. */
data class VenueReview(val author: String, val stars: Int, val text: String)

/**
 * Data mənbəyi kontraktı (D-036, qayda ③/④; D-050 ikili rejim).
 *
 * İKİ implementasiya, İKİ branch:
 *  - `main`      → [FakeQoshulDataSource] (placeholder/demo məlumat)
 *  - `server-only` → ServerQoshulDataSource (boş — serverə qoşulana qədər heç nə)
 *
 * BÖLÜŞMƏ (D-050): istifadəçi/sosial MƏLUMAT (events, people, partners, offers,
 * söhbətlər, bildirişlər, slotlar, rəylər) mənbəyə aiddir; TAKSONOMİYA
 * (kateqoriya qrupları, status etiketləri, ROL seçimləri) tətbiq məzmunudur —
 * interfeys default-metodları kimi hər iki rejimdə eyni verir.
 *
 * Qeyd: interfeys yalnız BAŞLANĞIC dəstini verir — mutasiyalar mühərrikin
 * yaddaşında simulyasiya olunur (backend fazasında server sinxronu gələcək).
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

    /** Məkan rəyləri — server rejimində boş (serverdən gələcək). */
    val mockVenueReviews: Map<String, List<VenueReview>>
        get() = emptyMap()

    // ── Taksonomiya (tətbiq məzmunu — rejimdən asılı deyil) ──

    /** Bütün kateqoriyalar (qruplardan düzənir). */
    val allCategories: List<String>
        get() = categoryGroups.flatMap { it.items }

    /** Kateqoriyanın qrup adı ("Qəhvə" → "Yemək və içki"). */
    fun groupOf(category: String): String =
        categoryGroups.firstOrNull { it.items.contains(category) }?.group
            ?: "Səyahət və digər"

    /** Yeni tədbir üçün şəkil açarı — qrupa görə (lokal asset xəritəsi). */
    fun imageForCategory(category: String): String = when (groupOf(category)) {
        "İdman", "Təbiət" -> ImageKey.PADEL
        "Yemək və içki" -> ImageKey.COFFEE
        "Texnologiya", "Biznes və karyera", "Təhsil" -> ImageKey.TECH
        else -> ImageKey.FRIENDS
    }

    /** Şəhər adından koordinat (naməlum → null; çağıran fallback edir). */
    fun findCity(name: String): City? = cities.firstOrNull { it.name == name }

    // Onboarding statik siyahıları (web onboarding.ts)
    val statusLabels: Map<String, String>
    val statuses: List<String>
    val universities: List<String>
    val languageOptions: List<String>
    val languageLevels: List<String>
    val goals: List<String>
    val availabilityOptions: List<String>
    val groupSizes: List<String>
    val budgets: List<String>
    val traitLabels: Map<String, String>
    val personalityQuestions: List<Pair<String, Pair<String, Boolean>>>
    val rolCategories: List<String>
    val rolCategoryHints: Map<String, String>

    /** Status ID → görünən etiket (statusLabels xəritəsindən). */
    fun statusLabel(id: String?): String =
        if (id == null) "" else (statusLabels[id] ?: "")
}

