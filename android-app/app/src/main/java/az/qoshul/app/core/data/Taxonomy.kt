package az.qoshul.app.core.data

import az.qoshul.app.core.model.AzCollation
import az.qoshul.app.core.model.CategoryGroupData
import az.qoshul.app.core.model.City

/**
 * TAKSONOMİYA — tətbiq məzmunu (D-050 bölgüsü): kateqoriya qrupları, şəhərlər
 * və onboarding statik siyahıları rejimdən asılı deyil, həmişə buradan gəlir.
 *
 * D-151 (2026-11-25, direktiv: „demo məntiqi heç olmasın“): əvvəl bu dəyərlər
 * FakeQoshulDataSource-un üzvləri idi və ServerSeedDataSource onları fake
 * mənbəzindən oxuyurdu. İndi fake mənbə test scope-a köçüb — istehsal kodu
 * taksonomiyaya birbaşa buradan çatır, heç bir fake istinadı qalmır.
 */
object Taxonomy {

    // ── Kateqoriya qrupları — web categories.ts (birebir) ──
    val categoryGroups: List<CategoryGroupData> = listOf(
        CategoryGroupData("İdman", listOf(
            "Futbol", "Basketbol", "Voleybol", "Padel", "Tennis", "Stolüstü tennis", "Üzgüçülük",
            "Qaçış", "Velosiped", "Yoga", "Fitnes", "Döyüş idmanı", "Güləş", "Şahmat", "Boulinq")),
        CategoryGroupData("Təbiət", listOf(
            "Yürüş", "Kemping", "Alpinizm", "Balıqçılıq", "Piknik", "At sürmə", "Quş müşahidəsi")),
        CategoryGroupData("Mədəniyyət", listOf(
            "Muzey", "Teatr", "Kino", "Sərgi", "Kitab klubu", "Poeziya gecəsi", "Tarix turu")),
        CategoryGroupData("Musiqi", listOf(
            "Konsert", "Cem-sessiya", "Karaoke", "Muğam", "Caz", "DJ gecəsi", "Musiqi aləti dərsi")),
        CategoryGroupData("Yemək və içki", listOf(
            "Qəhvə", "Çay söhbəti", "Şam yeməyi", "Səhər yeməyi", "Kulinariya dərsi", "Dequstasiya",
            "Küçə yeməkləri")),
        CategoryGroupData("Təhsil", listOf(
            "Dil mübadiləsi", "Seminar", "Mühazirə", "İmtahana hazırlıq", "Birlikdə dərs", "Elm söhbəti")),
        CategoryGroupData("Texnologiya", listOf(
            "Hakaton", "Proqramlaşdırma", "Süni intellekt", "Startap", "Oyun hazırlama", "Dizayn",
            "Robototexnika")),
        CategoryGroupData("Biznes və karyera", listOf(
            "Networking", "Karyera", "Sahibkarlıq", "Marketinq", "Maliyyə savadlılığı",
            "Müsahibəyə hazırlıq")),
        CategoryGroupData("Oyunlar", listOf(
            "Stolüstü oyunlar", "Kibersport", "Kviz", "Mafiya", "Nərd", "Video oyunlar", "Escape room")),
        CategoryGroupData("İncəsənət", listOf(
            "Rəsm", "Foto-gəzinti", "Əl işləri", "Rəqs", "Xalçaçılıq", "Keramika", "Yazı emalatxanası")),
        CategoryGroupData("Könüllülük", listOf(
            "Təmizlik aksiyası", "Xeyriyyə", "Heyvanlara yardım", "Ağac əkmə", "Qan donorluğu")),
        CategoryGroupData("Səyahət və digər", listOf(
            "Şəhər turu", "Səfər", "Ailə", "Sağlamlıq", "Mənəvi inkişaf", "Valideynlər", "Tələbələr",
            "Digər")),
    )

    /** Bütün kateqoriyalar (qruplardan düzənir). */
    val allCategories: List<String> = categoryGroups.flatMap { it.items }

    /** Kateqoriyanın qrup adı ("Qəhvə" → "Yemək və içki"). */
    fun groupOf(category: String): String =
        categoryGroups.firstOrNull { it.items.contains(category) }?.group
            ?: "Səyahət və digər"

    // ── Şəhərlər — D-126: Azərbaycanın BÜTÜN rəsmi şəhərləri (79), A-Z ──
    val cities: List<City> = listOf(
        City("Ağcabədi", "Qarabağ", 40.0528, 47.4589),
        City("Ağdam", "Qarabağ", 39.9906, 46.9231),
        City("Ağdaş", "Mərkəzi Aran", 40.6333, 47.4656),
        City("Ağdərə", "Qarabağ", 40.2131, 46.9722),
        City("Ağstafa", "Qazax-Tovuz", 41.1167, 45.4486),
        City("Ağsu", "Dağlıq Şirvan", 40.5667, 48.4000),
        City("Astara", "Lənkəran-Astara", 38.4558, 48.8725),
        City("Babək", "Naxçıvan MR", 39.1278, 45.4247),
        City("Bakı", "Bakı", 40.4093, 49.8671),
        City("Balakən", "Şəki-Zaqatala", 41.7342, 46.4008),
        City("Beyləqan", "Mil-Muğan", 39.7692, 47.6311),
        City("Bərdə", "Qarabağ", 40.3775, 47.1272),
        City("Biləsuvar", "Şirvan-Salyan", 39.4533, 48.5489),
        City("Cəbrayıl", "Şərqi Zəngəzur", 39.3988, 47.0273),
        City("Cəlilabad", "Lənkəran-Astara", 39.2158, 48.4983),
        City("Culfa", "Naxçıvan MR", 38.9533, 45.6308),
        City("Daşkəsən", "Gəncə-Daşkəsən", 40.5867, 46.0869),
        City("Dəliməmmədli", "Gəncə-Daşkəsən", 40.6372, 46.7433),
        City("Füzuli", "Qarabağ", 39.6008, 47.1450),
        City("Gədəbəy", "Qazax-Tovuz", 40.5656, 45.8019),
        City("Gəncə", "Gəncə-Daşkəsən", 40.6828, 46.3606),
        City("Goranboy", "Gəncə-Daşkəsən", 40.6011, 46.7944),
        City("Göyçay", "Mərkəzi Aran", 40.6531, 47.7406),
        City("Göygöl", "Gəncə-Daşkəsən", 40.5856, 46.3189),
        City("Göytəpə", "Lənkəran-Astara", 39.2075, 48.5683),
        City("Hacıqabul", "Şirvan-Salyan", 40.0333, 48.9331),
        City("Horadiz", "Qarabağ", 39.4486, 47.3417),
        City("Xaçmaz", "Quba-Xaçmaz", 41.4606, 48.8022),
        City("Xankəndi", "Qarabağ", 39.8181, 46.7529),
        City("Xırdalan", "Abşeron-Xızı", 40.4481, 49.7553),
        City("Xızı", "Abşeron-Xızı", 40.9492, 49.0847),
        City("Xocalı", "Qarabağ", 39.9156, 46.8361),
        City("Xocavənd", "Qarabağ", 39.7039, 47.0042),
        City("Xudat", "Quba-Xaçmaz", 41.6306, 48.6689),
        City("İmişli", "Mil-Muğan", 39.8717, 48.3544),
        City("İsmayıllı", "Dağlıq Şirvan", 40.7358, 48.1817),
        City("Kəlbəcər", "Şərqi Zəngəzur", 40.1031, 46.0331),
        City("Kürdəmir", "Mərkəzi Aran", 40.3353, 47.9772),
        City("Qax", "Şəki-Zaqatala", 41.4228, 46.9242),
        City("Qazax", "Qazax-Tovuz", 41.0819, 45.3652),
        City("Qəbələ", "Şəki-Zaqatala", 40.9875, 47.8439),
        City("Qobustan", "Dağlıq Şirvan", 40.5364, 48.9444),
        City("Qovlar", "Qazax-Tovuz", 40.9569, 46.5169),
        City("Quba", "Quba-Xaçmaz", 41.3625, 48.5122),
        City("Qubadlı", "Şərqi Zəngəzur", 39.3186, 46.6064),
        City("Qusar", "Quba-Xaçmaz", 41.4281, 48.4358),
        City("Laçın", "Şərqi Zəngəzur", 39.6653, 46.5400),
        City("Lerik", "Lənkəran-Astara", 38.7772, 48.4856),
        City("Lənkəran", "Lənkəran-Astara", 38.7529, 48.8475),
        City("Liman", "Lənkəran-Astara", 39.0447, 49.0061),
        City("Masallı", "Lənkəran-Astara", 38.9486, 48.6647),
        City("Mingəçevir", "Mərkəzi Aran", 40.7703, 47.0489),
        City("Naftalan", "Gəncə-Daşkəsən", 40.5044, 46.8397),
        City("Naxçıvan", "Naxçıvan MR", 39.2093, 45.4109),
        City("Neftçala", "Şirvan-Salyan", 39.3586, 49.2469),
        City("Oğuz", "Şəki-Zaqatala", 41.0675, 47.4608),
        City("Ordubad", "Naxçıvan MR", 38.9047, 46.0145),
        City("Saatlı", "Mil-Muğan", 39.9322, 48.3847),
        City("Sabirabad", "Mil-Muğan", 40.0131, 48.4864),
        City("Salyan", "Şirvan-Salyan", 39.5961, 48.9847),
        City("Samux", "Gəncə-Daşkəsən", 40.8492, 46.3542),
        City("Siyəzən", "Quba-Xaçmaz", 41.0683, 49.1072),
        City("Sumqayıt", "Abşeron-Xızı", 40.5897, 49.6686),
        City("Şabran", "Quba-Xaçmaz", 41.2844, 48.9256),
        City("Şahbuz", "Naxçıvan MR", 39.4056, 45.5836),
        City("Şamaxı", "Dağlıq Şirvan", 40.6314, 48.6406),
        City("Şəki", "Şəki-Zaqatala", 41.1919, 47.1706),
        City("Şəmkir", "Qazax-Tovuz", 40.8347, 46.0183),
        City("Şərur", "Naxçıvan MR", 39.5558, 44.9758),
        City("Şirvan", "Şirvan-Salyan", 39.9319, 48.9203),
        City("Şuşa", "Qarabağ", 39.8222, 46.7489),
        City("Tərtər", "Qarabağ", 40.3428, 46.9331),
        City("Tovuz", "Qazax-Tovuz", 40.9842, 45.6180),
        City("Ucar", "Mərkəzi Aran", 40.5181, 47.6492),
        City("Yardımlı", "Lənkəran-Astara", 38.9622, 48.2403),
        City("Yevlax", "Mərkəzi Aran", 40.6172, 47.1500),
        City("Zaqatala", "Şəki-Zaqatala", 41.6336, 46.6433),
        City("Zəngilan", "Şərqi Zəngəzur", 39.0869, 46.6483),
        City("Zərdab", "Mərkəzi Aran", 40.2167, 47.6986),
    ).sortedWith { a, b -> AzCollation.compare(a.name, b.name) }

    /** Şəhər adından koordinat (naməlum → null). */
    fun findCity(name: String): City? = cities.firstOrNull { it.name == name }

    // ── Onboarding statik siyahıları (web onboarding.ts) ──

    val statusLabels: Map<String, String> = mapOf(
        "school" to "Məktəbli", "student" to "Tələbə", "working" to "İşləyirəm",
        "job_seeking" to "İş axtarıram", "entrepreneur" to "Sahibkaram",
        "freelancer" to "Frilanserəm", "other" to "Digər",
    )

    val statuses: List<String> = listOf(
        "school", "student", "working", "job_seeking", "entrepreneur", "freelancer", "other"
    )

    val universities = listOf(
        "Bakı Dövlət Universiteti", "ADA Universiteti",
        "Azərbaycan Dövlət İqtisad Universiteti (UNEC)", "Azərbaycan Texniki Universiteti",
        "Xəzər Universiteti", "Qafqaz Universiteti", "Bakı Mühəndislik Universiteti",
        "Azərbaycan Dillər Universiteti", "Azərbaycan Tibb Universiteti", "Digər",
    )
    val languageOptions = listOf(
        "Azərbaycan dili", "İngilis dili", "Rus dili", "Türk dili", "Alman dili",
        "Fransız dili", "Ərəb dili", "Fars dili", "Çin dili", "İspan dili",
        "Gürcü dili", "Koreya dili", "Yapon dili", "İtalyan dili",
    )
    val languageLevels = listOf("Başlanğıc", "Orta", "Yaxşı", "Ana dili")
    val goals = listOf(
        "Yeni dostlar tapmaq", "İdman yoldaşı", "Networking və karyera", "Dil mübadiləsi",
        "Hobbi qrupu", "Birlikdə öyrənmək", "Səyahət yoldaşı", "Könüllülük",
        "Şəhəri kəşf etmək", "Startap komandası",
    )
    val availabilityOptions = listOf(
        "Həftəiçi səhər", "Həftəiçi gündüz", "Həftəiçi axşam", "Şənbə", "Bazar",
    )
    val groupSizes = listOf(
        "Təkbətək", "Kiçik qrup (3–6)", "Orta qrup (7–15)", "Böyük qrup (15+)"
    )
    val budgets = listOf(
        "Yalnız pulsuz", "10 AZN-ə qədər", "30 AZN-ə qədər", "Fərq etmir"
    )

    val traitLabels: Map<String, String> = mapOf(
        "openness" to "Açıqlıq",
        "conscientiousness" to "Məsuliyyətlilik",
        "extraversion" to "Ekstroversiya",
        "agreeableness" to "Xoş münasibət",
        "stability" to "Emosional sabitlik",
    )

    /** Şəxsiyyət testi sualları — (sual to (ölçü to revers)). */
    val personalityQuestions: List<Pair<String, Pair<String, Boolean>>> = listOf(
        "Yeni insanlarla tanış olmaq mənə enerji verir." to ("extraversion" to false),
        "Böyük məclislərdə özümü yorğun hiss edirəm." to ("extraversion" to true),
        "Söhbəti adətən mən başladıram." to ("extraversion" to false),
        "Yeni və qeyri-adi şeyləri sınamağı xoşlayıram." to ("openness" to false),
        "İncəsənət və musiqi məni dərindən maraqlandırır." to ("openness" to false),
        "Planlarımı vaxtında və dəqiq yerinə yetirirəm." to ("conscientiousness" to false),
        "Görüşlərə adətən gecikirəm." to ("conscientiousness" to true),
        "Başqalarının hisslərinə qarşı həssasam." to ("agreeableness" to false),
        "Mübahisədə güzəştə getməyi bacarıram." to ("agreeableness" to false),
        "Kiçik stresslərdən asanlıqla özümü topluyam." to ("stability" to false),
    )

    // D-115: „biznes ilə əlaəqli yerləri sil“ — maraqlar sosial/hobbi dəstində.
    val rolCategories = listOf(
        "Əyləncə və Gecə həyatı", "Kafe və Yemək", "İdman və Aktivlik",
        "Təbiət və Səyahət", "Mədəniyyət və İncəsənət",
        "Oyun və Hobbi", "Musiqi", "Könüllülük və Sosial",
    )

    val rolCategoryHints = mapOf(
        "Əyləncə və Gecə həyatı" to "klub, bar, karaoke",
        "Kafe və Yemək" to "kafe, restoran, kofe görüşləri",
        "İdman və Aktivlik" to "futbol, tennis, qaçış, velosiped, fitnes",
        "Təbiət və Səyahət" to "hiking, kamp, Qəbələ, Şahdağ",
        "Mədəniyyət və İncəsənət" to "teatr, muzey, konsert, sərgi",
        "Oyun və Hobbi" to "stolüstü oyunlar, PlayStation, kitab klubu, foto",
        "Musiqi" to "canlı musiqi, jam session",
        "Könüllülük və Sosial" to "xeyriyyə, təmizlik aksiyaları",
    )

    /** Status ID → görünən etiket. */
    fun statusLabel(id: String?): String =
        if (id == null) "" else (statusLabels[id] ?: "")
}
