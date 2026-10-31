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
 * SERVER_ONLY rejimi data mənbəyi (D-050, server-only branch).
 *
 * İstifadəçi direktivi: "serverə bağlanana qədər heçnə olmasın yəni yalnız
 * server data modu" — BÜTÜN istifadəçi/sosial məlumat boşdur. Tədbirlər,
 * insanlar, məkanlar, təkliflər, söhbətlər, bildirişlər, slotlar, rəylər —
 * hamısı yalnız real backend qoşulanda gələcək (backend kontraktı fazasında
 * bu sinifə API inteqrasiyası yazılacaq; kontrakt sümüyü ApiClient-dədir).
 *
 * TAKSONOMİYA (kateqoriyalar, statuslar, ROL seçimləri) interfeysin
 * default-metodlarından gəlir — tətbiq məzmunudur, serverdən asılı deyil.
 *
 * UI təsiri: bütün siyahı ekranları mövcud EmptyState-ləri göstərir;
 * giriş (AuthScreen) yalnız real server autentifikasiyası ilə açılır.
 */
object ServerQoshulDataSource : QoshulDataSource {

    override val events: List<QoshulEvent> = emptyList()
    override val people: List<Person> = emptyList()
    override val partners: List<Partner> = emptyList()
    override val cities: List<City> = emptyList()
    override val offers: List<CouponOffer> = emptyList()
    override val initialNotifications: List<AppNotification> = emptyList()
    override val initialEventMessages: Map<String, List<ChatMessage>> = emptyMap()
    override val initialFriendMessages: Map<String, List<ChatMessage>> = emptyMap()
    override val venueSlotTimes: Map<String, List<String>> = emptyMap()

    override fun initialVenueXp(): Map<String, VenueXpAccount> = emptyMap()

    override val mockVenueReviews: Map<String, List<VenueReview>> = emptyMap()

    // Taksonomiya: interfeys default-ları (categoryGroups boşdur — server
    // kateqoriyaları gələnə qədər create formu boş siyahı ilə işləyir).
    override val categoryGroups: List<CategoryGroupData> = emptyList()
    override val statusLabels: Map<String, String> = emptyMap()
    override val statuses: List<String> = emptyList()
    override val universities: List<String> = emptyList()
    override val languageOptions: List<String> = emptyList()
    override val languageLevels: List<String> = emptyList()
    override val goals: List<String> = emptyList()
    override val availabilityOptions: List<String> = emptyList()
    override val groupSizes: List<String> = emptyList()
    override val budgets: List<String> = emptyList()
    override val traitLabels: Map<String, String> = emptyMap()
    override val personalityQuestions: List<Pair<String, Pair<String, Boolean>>> = emptyList()
    override val rolCategories: List<String> = emptyList()
    override val rolCategoryHints: Map<String, String> = emptyMap()
}
