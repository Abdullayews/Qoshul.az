package az.qoshul.app.core.data

import az.qoshul.app.core.model.AppNotification
import az.qoshul.app.core.model.CategoryGroupData
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.model.City
import az.qoshul.app.core.model.CouponOffer
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.VenueXpAccount

/**
 * Data mənbəyi kontraktı (D-036, qayda ③/④).
 *
 * UI mərhələsində yeganə implementasiya [FakeQoshulDataSource]-dur (adlandırılmış
 * fake). Backend fazasında eyni interfeysə Retrofit/API implementasiyası yazılacaq
 * (OpenAPI kontraktı hazır olanda); mühərrik (AppEngine) dəyişməz qalır.
 *
 * Qeyd: interfeys yalnız BAŞLANĞIC dəstini verir — bütün mutasiyalar
 * (join, swipe, rezerv, XP) mühərrikin yaddaşında simulyasiya olunur,
 * Flutter AppState ilə eyni semantika.
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
}
