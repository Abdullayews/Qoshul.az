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
 * D-149: SUPABASE rejiminin başlanğıc mənbəyi — istifadəçi/sosial MƏLUMAT
 * BOŞDUR (data canlı serverdən gəlir; fake seed oxunmur — direktiv
 * 2026-11-24: „FakeData mənbəyi SUPABASE rejimində oxunmamalı“).
 *
 * TAKSONOMİYA (kateqoriya qrupları, şəhərlər, onboarding statik siyahıları)
 * tətbiq məzmunudur — D-050 bölgüsünə görə hər iki rejimdə eyni verilir;
 * mənbəyi FakeQoshulDataSource-un sabit dəyərləridir (serverdəki
 * `categories` sistem cədvəli ilə eyni kökdən — 0003_seed).
 *
 * Qeyd: REST-dən oxuma ayrı mərhələdir (M2 oxu yolu); bu obyekt yalnız
 * təmiz başlanğıc vəziyyəti təmin edir — boş listlər UI-da „hələ heç nə
 * yoxdur“ boşluqlarını göstərir, crash yoxdur.
 */
object ServerSeedDataSource : QoshulDataSource {

    override val events: List<QoshulEvent> = emptyList()
    override val people: List<Person> = emptyList()
    override val partners: List<Partner> = emptyList()
    override val offers: List<CouponOffer> = emptyList()
    override val initialNotifications: List<AppNotification> = emptyList()
    override val initialEventMessages: Map<String, List<ChatMessage>> = emptyMap()
    override val initialFriendMessages: Map<String, List<ChatMessage>> = emptyMap()
    override val venueSlotTimes: Map<String, List<String>> = emptyMap()

    override fun initialVenueXp(): Map<String, VenueXpAccount> = emptyMap()

    override val mockVenueReviews: Map<String, List<VenueReview>> = emptyMap()

    // ── Taksonomiya: FakeData ilə eyni mənbə (tətbiq məzmunu, D-050) ──

    override val categoryGroups: List<CategoryGroupData> =
        FakeQoshulDataSource.categoryGroups

    override val cities: List<City> = FakeQoshulDataSource.cities

    override val statusLabels: Map<String, String> = FakeQoshulDataSource.statusLabels
    override val statuses: List<String> = FakeQoshulDataSource.statuses
    override val universities: List<String> = FakeQoshulDataSource.universities
    override val languageOptions: List<String> = FakeQoshulDataSource.languageOptions
    override val languageLevels: List<String> = FakeQoshulDataSource.languageLevels
    override val goals: List<String> = FakeQoshulDataSource.goals
    override val availabilityOptions: List<String> = FakeQoshulDataSource.availabilityOptions
    override val groupSizes: List<String> = FakeQoshulDataSource.groupSizes
    override val budgets: List<String> = FakeQoshulDataSource.budgets
    override val traitLabels: Map<String, String> = FakeQoshulDataSource.traitLabels
    override val personalityQuestions: List<Pair<String, Pair<String, Boolean>>> =
        FakeQoshulDataSource.personalityQuestions
    override val rolCategories: List<String> = FakeQoshulDataSource.rolCategories
    override val rolCategoryHints: Map<String, String> = FakeQoshulDataSource.rolCategoryHints
}
