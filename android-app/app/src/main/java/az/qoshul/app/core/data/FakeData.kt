package az.qoshul.app.core.data

import az.qoshul.app.core.model.AppNotification
import az.qoshul.app.core.model.CategoryGroupData
import az.qoshul.app.core.model.ChatMessage
import az.qoshul.app.core.model.City
import az.qoshul.app.core.model.CouponOffer
import az.qoshul.app.core.model.ImageKey
import az.qoshul.app.core.model.NotifType
import az.qoshul.app.core.model.Partner
import az.qoshul.app.core.model.Person
import az.qoshul.app.core.model.QoshulEvent
import az.qoshul.app.core.model.VenueXpAccount
import az.qoshul.app.core.model.VenueXpConfig
import az.qoshul.app.core.model.XpLedgerEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * FAKE data mənbəyi (adlandırılmış fake — qayda ③).
 * Flutter `core/data/mock_data.dart` portu — məzmun birebir (docs/audit
 * bölmə 4.11). A6/A7 düzəlişləri: epochDay + sabit insan pin koordinatları.
 */
object FakeQoshulDataSource : QoshulDataSource {

    private val imgCoffee = ImageKey.COFFEE
    private val imgPadel = ImageKey.PADEL
    private val imgTech = ImageKey.TECH
    private val imgFriends = ImageKey.FRIENDS

    private fun LocalDate.epochDay(): Int = toEpochDay().toInt()
    private val today: LocalDate = LocalDate.now()

    /** Növbəti həftə günü (bugündən sonra). */
    private fun nextDow(dow: DayOfWeek): Int {
        var d = today
        while (d.dayOfWeek != dow || !d.isAfter(today)) d = d.plusDays(1)
        return d.epochDay()
    }

    // ── Kateqoriya qrupları — web categories.ts (birebir) ──
    override val categoryGroups = listOf(
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

    // allCategories / groupOf / imageForCategory / findCity / statusLabel —
    // D-050-dən etibarən QoshulDataSource interfeysinin default-metodlarıdır
    // (taksonomiya = tətbiq məzmunu, hər iki rejimdə eyni).

    // ── Şəhərlər — web cities.ts (birebir) ──
    override val cities = listOf(
        City("Bakı", "Abşeron-Xızı", 40.4093, 49.8671),
        City("Sumqayıt", "Abşeron-Xızı", 40.5897, 49.6686),
        City("Xırdalan", "Abşeron-Xızı", 40.4481, 49.7553),
        City("Gəncə", "Gəncə-Daşkəsən", 40.6828, 46.3606),
        City("Göygöl", "Gəncə-Daşkəsən", 40.5856, 46.3189),
        City("Naftalan", "Gəncə-Daşkəsən", 40.5044, 46.8397),
        City("Mingəçevir", "Mərkəzi Aran", 40.7703, 47.0489),
        City("Yevlax", "Mərkəzi Aran", 40.6172, 47.15),
        City("Göyçay", "Mərkəzi Aran", 40.6531, 47.7406),
        City("Şirvan", "Şirvan-Salyan", 39.9319, 48.9203),
        City("Salyan", "Şirvan-Salyan", 39.5961, 48.9847),
        City("Neftçala", "Şirvan-Salyan", 39.3586, 49.2469),
        City("Şəki", "Şəki-Zaqatala", 41.1919, 47.1706),
        City("Zaqatala", "Şəki-Zaqatala", 41.6336, 46.6433),
        City("Qax", "Şəki-Zaqatala", 41.4228, 46.9242),
        City("Quba", "Quba-Xaçmaz", 41.3625, 48.5122),
        City("Xaçmaz", "Quba-Xaçmaz", 41.4606, 48.8022),
        City("Lənkəran", "Lənkəran-Astara", 38.7529, 48.8475),
        City("Astara", "Lənkəran-Astara", 38.4558, 48.8725),
        City("Naxçıvan", "Naxçıvan MR", 39.2093, 45.4109),
        City("Şamaxı", "Şirvan-Salyan", 40.6314, 48.6406),
        City("Qəbələ", "Şəki-Zaqatala", 40.9875, 47.8439),
        City("İsmayıllı", "Şəki-Zaqatala", 40.7358, 48.1817),
        City("Şuşa", "Qarabağ", 39.8222, 46.7489),
        City("Xankəndi", "Qarabağ", 39.8181, 46.7529),
    )

    // findCity — interfeys default-metodudur (City?; çağıran fallback edir).

    // ── Mock tədbirlər — realistik Azərbaycan məzmunu (birebir port) ──
    override val events = listOf(
        QoshulEvent(
            id = "e1", title = "Şənbə səhəri qəhvə və söhbət", category = "Qəhvə",
            image = imgCoffee, city = "Bakı", location = "Coffee Lab, Nərimanov",
            lat = 40.3922, lng = 49.8522, distance = 1.2, time = "10:00", date = "Şənbə",
            epochDay = nextDow(DayOfWeek.SATURDAY), past = false, capacity = 8, participants = 5,
            host = "Leyla Məmmədova",
            description = "Səhər qəhvəmizi birlikdə içib həftəni planlaşdıraq. Yeni tanışlıqlar, " +
                "ilham verici söhbətlər və gözəl əhval-ruhiyyə gözləyirik. Gələcəkdə bu görüşləri " +
                "hər şənbə təkrarlayacayıq.",
            language = "Azərbaycan dili", level = "Hamı üçün",
        ),
        QoshulEvent(
            id = "e2", title = "Padel oyunu — yeni başlayanlar üçün", category = "Padel",
            image = imgPadel, city = "Bakı", location = "Padel Arena, Binəqədi",
            lat = 40.4236, lng = 49.8236, distance = 3.4, time = "18:00", date = "Bu gün",
            epochDay = today.epochDay(), past = false, capacity = 4, participants = 3,
            host = "Rəşad Əliyev",
            description = "Padel ilk dəfə sınayırsansa, bu görüş sənin üçündür! Raketlər və meydan " +
                "tərəfdaş məkanından, gələcəyin tərəfdaş endirimi ilə təmin olunur. Sualın olsa yaz.",
            price = 15, level = "Başlanğıc", language = "Qarışıq",
        ),
        QoshulEvent(
            id = "e3", title = "Şənbə yürüşü — Şıx düzü marşrutu", category = "Yürüş",
            image = imgFriends, city = "Bakı", location = "Şıx düzü başlanğıc nöqtəsi",
            lat = 40.3292, lng = 49.7689, distance = 7.8, time = "08:00", date = "Şənbə",
            epochDay = nextDow(DayOfWeek.SATURDAY), past = false, capacity = 15, participants = 9,
            host = "Nigar Quliyeva",
            description = "Orta çətinlikdə 8 km marşrut. Su və rahat ayaqqabı yanına al. Yolda " +
                "fotopauzalar olacaq. Qoşulma istəyi göndər — yerlər təsdiqləndikcə görünəcək.",
            approval = true, ageLimit = "16+",
        ),
        QoshulEvent(
            id = "e4", title = "Dostluq kuboku — 7x7 futbol", category = "Futbol",
            image = imgPadel, city = "Bakı", location = "Zığ idman kompleksi",
            lat = 40.3736, lng = 49.8356, distance = 5.1, time = "17:00", date = "Bazar",
            epochDay = nextDow(DayOfWeek.SUNDAY), past = false, capacity = 14, participants = 11,
            host = "Orxan Həsənov",
            description = "Ənənəvi həftəlik futbol görüşümüz. Komandalar gəliş sırası ilə bölünür, " +
                "səviyyə fərqi vacib deyil — əsas məqsəd hərəkət və yaxşı əhvaldır.",
            level = "Orta",
        ),
        QoshulEvent(
            id = "e5", title = "Stolüstü oyunlar gecəsi", category = "Stolüstü oyunlar",
            image = imgFriends, city = "Bakı", location = "Board Game Cafe, İçərişəhər",
            lat = 40.3662, lng = 49.8339, distance = 2.3, time = "19:30", date = "Bu gün",
            epochDay = today.epochDay(), past = false, capacity = 10, participants = 7,
            host = "Aysel İsmayılova",
            description = "Karbon, Katoma, Şahmat və daha 30+ oyun. Yeni oyunlar öyrənmək " +
                "istəyənlər üçün masada izah edənlər olacaq. Hamılıqla çay sifariş veririk.",
            price = 5,
        ),
        QoshulEvent(
            id = "e-run", title = "Səhər qaçışı — Bulvar marşrutu", category = "Qaçış",
            image = imgFriends, city = "Bakı", location = "Dənizkənarı bulvar, Azneft başlanğıcı",
            lat = 40.3652, lng = 49.8412, distance = 1.8, time = "07:30", date = "Bu gün",
            epochDay = today.epochDay(), past = false, capacity = 12, participants = 8,
            host = "Elvin Soltanov",
            description = "5 km asan temp ilə səhər enerjisi. Qaçışdan sonra bulvarda birlikdə " +
                "çay içirik — yeni başlayanlar üçün oxuyucu qrupda arxada gəlir.",
            level = "Asan", language = "Azərbaycan dili",
        ),
        QoshulEvent(
            id = "e6", title = "Flutter meetup: state management", category = "Proqramlaşdırma",
            image = imgTech, city = "Bakı", location = "Techno Hub, Nərimanov",
            lat = 40.3903, lng = 49.8556, distance = 3.9, time = "14:00", date = "Noy 8",
            epochDay = LocalDate.of(2026, 11, 8).epochDay(), past = false, capacity = 40,
            participants = 26, host = "Orxan Həsənov",
            description = "Bu dəfəki mövzu: real layihələrdə state management seçimi. İki qısa " +
                "təqdimat + açıq müzakirə + networking. Təcrübəli iştirakçılar da, yeni başlayanlar " +
                "da xoş gəlir.",
            language = "Qarışıq",
        ),
        QoshulEvent(
            id = "e7", title = "Dil mübadiləsi: AZ-EN söhbət klubu", category = "Dil mübadiləsi",
            image = imgCoffee, city = "Bakı", location = "Fountain Square",
            lat = 40.3777, lng = 49.8731, distance = 0.8, time = "18:30", date = "Şənbə",
            epochDay = nextDow(DayOfWeek.SATURDAY), past = false, capacity = 12, participants = 8,
            host = "Leyla Məmmədova",
            description = "Yarısı Azərbaycan, yarısı İngilis dilli söhbət formatı. Hər 15 dəqiqədə " +
                "dəyişirik. Səviyyənə görə qruplar yoxdur — hamı bir masada.",
            level = "Hamı üçün",
        ),
        QoshulEvent(
            id = "e8", title = "Kviz gecəsi — komandalı yarış", category = "Kviz",
            image = imgFriends, city = "Bakı", location = "Dinamo, Səbail",
            lat = 40.3617, lng = 49.8358, distance = 4.2, time = "20:00", date = "Cümə",
            epochDay = nextDow(DayOfWeek.FRIDAY), past = false, capacity = 24, participants = 18,
            host = "Rəşad Əliyev",
            description = "Ümumi bilik + Azərbaycan tarixi + musiqi roundu. 4 nəfərlik komandalar. " +
                "Qalib komandanın adı lövhədə qalır. Tək gələnləri yerindəcə komandalara bölürük.",
            price = 10,
        ),
        QoshulEvent(
            id = "e9", title = "Gəncədə piknik — Gənc-Göl sahili", category = "Piknik",
            image = imgFriends, city = "Gəncə", location = "Gənc-Göl istirahət zonası",
            lat = 40.7156, lng = 46.4711, distance = 2.0, time = "12:00", date = "Bazar",
            epochDay = nextDow(DayOfWeek.SUNDAY), past = false, capacity = 20, participants = 6,
            host = "Nigar Quliyeva",
            description = "Xalı-piknik formatı: hər kəs bir növsnack gətirir, çay bizdən. Oyunlar, " +
                "akustik gitara və göl kənarında sakit axşam. Yağış halında təxirə salınacaq.",
        ),
        QoshulEvent(
            id = "e10", title = "Yoga səhəri — Dənizkənarı bulvar", category = "Yoga",
            image = imgPadel, city = "Bakı", location = "Dənizkənarı bulvar, Kristal zal yanında",
            lat = 40.3695, lng = 49.8367, distance = 1.7, time = "07:30", date = "Şənbə",
            epochDay = nextDow(DayOfWeek.SATURDAY), past = false, capacity = 12, participants = 10,
            host = "Aysel İsmayılova",
            description = "Dəniz havasında 60 dəqiqəlik yumşaq vinyasa. Matlar limitli sayda var — " +
                "yazarkən qeyd et. Sonrasında birlikdə sağlam səhər yeməyi.",
            price = 8, level = "Başlanğıc",
        ),
        QoshulEvent(
            id = "e11", title = "Film gecəsi: azərbaycan klassikləri", category = "Kino",
            image = imgFriends, city = "Bakı", location = "Nizami kino mərkəzi",
            lat = 40.3719, lng = 49.8397, distance = 2.9, time = "19:00", date = "Cümə",
            epochDay = nextDow(DayOfWeek.FRIDAY), past = false, capacity = 18, participants = 14,
            host = "Leyla Məmmədova",
            description = "Bu ayın filmi müzakirə ilə seçilib. Seansdan sonra yaxınlıqdakı kafedə " +
                "film haqqında söhbət. Biletləri əvvəlcədən alırıq.",
            price = 6,
        ),
        QoshulEvent(
            id = "e12", title = "Könüllülük: Xəzər sahilinin təmizliyi", category = "Təmizlik aksiyası",
            image = imgFriends, city = "Bakı", location = "Şıx çimərliyi",
            lat = 40.3453, lng = 49.7553, distance = 11.4, time = "10:00", date = "Bazar",
            epochDay = nextDow(DayOfWeek.SUNDAY), past = false, capacity = 30, participants = 12,
            host = "Aysel İsmayılova",
            description = "Əlcəklər və zibil qabları təşkilatçılardan. 2 saatlıq birgə iş + " +
                "sonrasında çay. Şəxsi su şüşəni gətir — plastik istifadə etməyək.",
            ageLimit = "Hamı üçün",
        ),
        QoshulEvent(
            id = "e0", title = "Keçən həftə qaçış klubu", category = "Qaçış",
            image = imgPadel, city = "Bakı", location = "Dənizkənarı bulvar",
            lat = 40.3695, lng = 49.8367, distance = 1.5, time = "07:00", date = "Keçən bazar",
            epochDay = Int.MIN_VALUE, past = true, capacity = 12, participants = 12,
            host = "Rəşad Əliyev",
            description = "5 km asan temp. Qoşulduğun üçün +10 XP qazandın — rəyini yazaraq əlavə +15 XP qazana bilərsən.",
        ),
        // D-026 (L.3) demo: istifadəçinin öz təşkil etdiyi, bitmiş aktivite.
        QoshulEvent(
            id = "e-mafiya", title = "Mafiya oyunu gecəsi", category = "Stolüstü oyunlar",
            image = imgFriends, city = "Bakı", location = "Xankəndi Restoranı, Nərimanov",
            lat = 40.3842, lng = 49.8641, distance = 2.2, time = "20:00", date = "Keçən cümə",
            epochDay = Int.MIN_VALUE, past = true, capacity = 10, participants = 5,
            host = "Dost", // onboarding-də profileName ilə əvəz olunur (AppEngine)
            description = "Xankəndi Restoranında klassik mafiya gecəsi. 5 nəfərlik masa, oyun " +
                "kartları bizdən. Aktivite bitəndə çıxışda restoranın QR-ını oxudub şəkilləri " +
                "paylaşacağam — taq edilən hər kəs obyekt XP-si qazanır.",
            level = "Orta", venueId = "v5",
        ),
        // D-026 (L.1): peşəkar sıralama üçün yüksək çəkili nümunə.
        QoshulEvent(
            id = "e-chess", title = "Blits şahmat turniri — reytinqlilər", category = "Şahmat",
            image = imgTech, city = "Bakı", location = "Techno Hub, Nərimanov",
            lat = 40.3903, lng = 49.8556, distance = 3.1, time = "19:30", date = "Şənbə",
            epochDay = nextDow(DayOfWeek.SATURDAY), past = false, capacity = 16, participants = 11,
            host = "Nurlan Vəliyev",
            description = "5 dəqiqəlik blits, isveçrə sistemi, 7 raund. Reytinqi 1600+ olan " +
                "oyunçular üçün. Yeni başlayanlara ayrıca masa olacaq.",
            level = "Peşəkar", price = 10,
        ),
    )

    // ── Mock insanlar — web data.ts + ROL D kart sahələri ──
    override val people = listOf(
        Person(
            id = "p1", firstName = "Leyla", lastName = "Məmmədova", image = imgCoffee,
            city = "Bakı", status = "student", org = "Bakı Dövlət Universiteti",
            interest = "Qəhvə · Səyahət", match = 94,
            reason = "Hər ikiniz qəhvə görüşlərini və şəhər gəzintilərini sevirsiniz.",
            age = 23, distanceKm = 1.2,
            rolInterests = listOf("Kafe və Yemək", "Təbiət və Səyahət", "Mədəniyyət və İncəsənət"),
            likesBack = true, mapLat = 40.3851, mapLng = 49.8456,
        ),
        Person(
            id = "p2", firstName = "Rəşad", lastName = "Əliyev", image = imgCoffee,
            city = "Bakı", status = "working", org = "Texnologiya sektoru",
            interest = "İdman · Texnologiya", match = 96,
            reason = "İdman və texnologiya maraqlarınız üst-üstə düşür — padel görüşü üçün ideal.",
            age = 27, distanceKm = 3.4,
            rolInterests = listOf("İdman və Aktivlik", "Təhsil və Karyera", "Oyun və Hobbi"),
            likesBack = true, mapLat = 40.4108, mapLng = 49.8312,
        ),
        Person(
            id = "p3", firstName = "Nigar", lastName = "Quliyeva", image = imgFriends,
            city = "Bakı", status = "student", org = "Azərbaycan Dillər Universiteti",
            interest = "Mədəniyyət · Musiqi", match = 91,
            reason = "Mədəni tədbirlərə birgə getmək üçün yüksək uyğunluq.",
            age = 22, distanceKm = 0.8,
            rolInterests = listOf("Mədəniyyət və İncəsənət", "Musiqi", "Könüllülük və Sosial"),
            likesBack = false, mapLat = 40.3654, mapLng = 49.8342,
        ),
        Person(
            id = "p4", firstName = "Orxan", lastName = "Həsənov", image = imgTech,
            city = "Bakı", status = "working", org = "Frilanser",
            interest = "Kodlaşdırma · Şahmat", match = 89,
            reason = "Texnologiya meetuplarında birlikdə iştirak üçün uyğun profil.",
            age = 29, distanceKm = 5.1,
            rolInterests = listOf("Təhsil və Karyera", "Oyun və Hobbi", "İdman və Aktivlik"),
            likesBack = false, mapLat = 40.3982, mapLng = 49.8545,
        ),
        Person(
            id = "p5", firstName = "Aysel", lastName = "İsmayılova", image = imgFriends,
            city = "Bakı", status = "student", org = "ADA Universiteti",
            interest = "Səyahət · Dizayn", match = 93,
            reason = "Səyahət planları və dizayn maraqlarınız uyğundur.",
            age = 25, distanceKm = 2.3,
            rolInterests = listOf("Təbiət və Səyahət", "Mədəniyyət və İncəsənət", "Musiqi"),
            likesBack = true, mapLat = 40.3761, mapLng = 49.8201,
        ),
    )

    // ── Mock tərəfdaş məkanlar — ROL E sahələri ilə ──
    override val partners = listOf(
        Partner(
            id = "v1", name = "Padel Arena", category = "İdman",
            address = "Böyük Şor şossesi 15", city = "Bakı",
            deal = "Səhər saatları -20%", hours = "08:00–12:00", image = imgPadel,
            lat = 40.4236, lng = 49.8236, priceRange = "₼₼₼",
            autoAccept = false, rating = 4.8, reviewsCount = 34,
            openFrom = 8, openTo = 23, discountPercent = 20,
        ),
        Partner(
            id = "v2", name = "Coffee Lab", category = "Qəhvə",
            address = "Ə. Haqverdiyev 12, Nərimanov", city = "Bakı",
            deal = "2+1 çay təklifi", hours = "09:00–22:00", image = imgCoffee,
            lat = 40.3922, lng = 49.8522, priceRange = "₼",
            autoAccept = true, rating = 4.6, reviewsCount = 51,
            openFrom = 9, openTo = 22, discountPercent = 15,
        ),
        Partner(
            id = "v3", name = "Techno Hub", category = "Texnologiya",
            address = "Nərimanov rayonu", city = "Bakı",
            deal = "İlk ziyarət pulsuz", hours = "10:00–20:00", image = imgTech,
            lat = 40.3903, lng = 49.8556, priceRange = "₼₼",
            autoAccept = true, rating = 4.4, reviewsCount = 19,
            openFrom = 10, openTo = 20, discountPercent = 0,
        ),
        Partner(
            id = "v4", name = "Dostlar Bağı", category = "Piknik",
            address = "Şıx düzü", city = "Bakı",
            deal = "Qruplara -10%", hours = "11:00–23:00", image = imgFriends,
            lat = 40.3292, lng = 49.7689, priceRange = "₼",
            autoAccept = true, rating = 4.7, reviewsCount = 27,
            openFrom = 11, openTo = 23, discountPercent = 10,
        ),
        // D-026 (L.2): obyektə-məxsus Endirim XP bazası demo-su.
        Partner(
            id = "v5", name = "Xankəndi Restoranı", category = "Restoran",
            address = "Şəfəq qəsəbəsi, Nərimanov", city = "Bakı",
            deal = "XP kartı ilə hesabda endirim", hours = "10:00–23:00", image = imgFriends,
            lat = 40.3842, lng = 49.8641, priceRange = "₼₼",
            autoAccept = true, rating = 4.5, reviewsCount = 68,
            openFrom = 10, openTo = 23, discountPercent = 0,
        ),
        Partner(
            id = "v6", name = "Kokaltı Kafesi", category = "Kafe",
            address = "Kokaltı küçəsi 8", city = "Bakı",
            deal = "Yeni açılış — XP qazanmaq üçün aktivite keçir", hours = "09:00–22:00",
            image = imgCoffee, lat = 40.3781, lng = 49.8469, priceRange = "₼",
            autoAccept = true, rating = 4.3, reviewsCount = 9,
            openFrom = 9, openTo = 22, discountPercent = 0,
        ),
    )

    // ── Status etiketləri (web onboarding.ts) ──
    override val statusLabels = mapOf(
        "school" to "Məktəbli", "student" to "Tələbə", "working" to "İşləyirəm",
        "job_seeking" to "İş axtarıram", "entrepreneur" to "Sahibkaram",
        "freelancer" to "Frilanserəm", "other" to "Digər",
    )

    // statusLabel — interfeys default-metodudur (statusLabels xəritəsindən).

    override val statuses = listOf(
        "school", "student", "working", "job_seeking", "entrepreneur", "freelancer", "other"
    )
    override val universities = listOf(
        "Bakı Dövlət Universiteti", "ADA Universiteti",
        "Azərbaycan Dövlət İqtisad Universiteti (UNEC)", "Azərbaycan Texniki Universiteti",
        "Xəzər Universiteti", "Qafqaz Universiteti", "Bakı Mühəndislik Universiteti",
        "Azərbaycan Dillər Universiteti", "Azərbaycan Tibb Universiteti", "Digər",
    )
    override val languageOptions = listOf(
        "Azərbaycan dili", "İngilis dili", "Rus dili", "Türk dili", "Alman dili",
        "Fransız dili", "Ərəb dili", "Fars dili", "Çin dili", "İspan dili",
        "Gürcü dili", "Koreya dili", "Yapon dili", "İtalyan dili",
    )
    override val languageLevels = listOf("Başlanğıc", "Orta", "Yaxşı", "Ana dili")
    override val goals = listOf(
        "Yeni dostlar tapmaq", "İdman yoldaşı", "Networking və karyera", "Dil mübadiləsi",
        "Hobbi qrupu", "Birlikdə öyrənmək", "Səyahət yoldaşı", "Könüllülük",
        "Şəhəri kəşf etmək", "Startap komandası",
    )
    override val availabilityOptions = listOf(
        "Həftəiçi səhər", "Həftəiçi gündüz", "Həftəiçi axşam", "Şənbə", "Bazar",
    )
    override val groupSizes = listOf(
        "Təkbətək", "Kiçik qrup (3–6)", "Orta qrup (7–15)", "Böyük qrup (15+)"
    )
    override val budgets = listOf(
        "Yalnız pulsuz", "10 AZN-ə qədər", "30 AZN-ə qədər", "Fərq etmir"
    )

    override val traitLabels = mapOf(
        "openness" to "Açıqlıq",
        "conscientiousness" to "Məsuliyyətlilik",
        "extraversion" to "Ekstroversiya",
        "agreeableness" to "Xoş münasibət",
        "stability" to "Emosional sabitlik",
    )

    override val personalityQuestions = listOf(
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

    override val rolCategories = listOf(
        "Əyləncə və Gecə həyatı", "Kafe və Yemək", "İdman və Aktivlik",
        "Təbiət və Səyahət", "Mədəniyyət və İncəsənət", "Təhsil və Karyera",
        "Oyun və Hobbi", "Musiqi", "Könüllülük və Sosial",
    )

    override val rolCategoryHints = mapOf(
        "Əyləncə və Gecə həyatı" to "klub, bar, karaoke",
        "Kafe və Yemək" to "kafe, restoran, kofe görüşləri",
        "İdman və Aktivlik" to "futbol, tennis, qaçış, velosiped, fitnes",
        "Təbiət və Səyahət" to "hiking, kamp, Qəbələ, Şahdağ",
        "Mədəniyyət və İncəsənət" to "teatr, muzey, konsert, sərgi",
        "Təhsil və Karyera" to "dil klubları, networking, seminar",
        "Oyun və Hobbi" to "stolüstü oyunlar, PlayStation, kitab klubu, foto",
        "Musiqi" to "canlı musiqi, jam session",
        "Könüllülük və Sosial" to "xeyriyyə, təmizlik aksiyaları",
    )

    // ── Məkanların rezerv saat slotları (ROL E) ──
    override val venueSlotTimes = mapOf(
        "v1" to listOf("10:00", "12:00", "16:00", "18:00", "20:00"),
        "v2" to listOf("09:00", "11:00", "13:00", "15:00", "17:00", "19:00", "21:00"),
        "v3" to listOf("10:00", "12:00", "14:00", "16:00", "18:00"),
        "v4" to listOf("12:00", "14:00", "16:00", "18:00", "20:00"),
        // D-055: v5/v6 rezerv axınında boş slot toru qalırdı (A6 audit)
        "v5" to listOf("12:00", "14:00", "18:00", "20:00", "22:00"),
        "v6" to listOf("10:00", "12:00", "16:00", "19:00"),
    )

    // nextDateLabels → core/data/DateLabels.kt (tarix hesablaması, mənbəyə aid deyil)

    /** Məkan rəyləri (ROL E) — model DataSource.kt-də (VenueReview). */
    override val mockVenueReviews = mapOf(
        "v1" to listOf(
            VenueReview("Elnur K.", 5, "Meydanlar əla, səhər saatları endirim həqiqətən işləyir."),
            VenueReview("Sara M.", 4, "Təşkilat yaxşıdır, yalnız qaraj yer axtarmaq lazım oldu."),
        ),
        "v2" to listOf(
            VenueReview("Aysu R.", 5, "Sərin qəhvə, mehriban kollektiv. 2+1 çay təklifi əladır."),
            VenueReview("Kamran S.", 4, "Bir az kalabalıq olur axşam, amma atmosfer xoşdur."),
            VenueReview("Nərmin A.", 5, "İlk görüş üçün ideal sakit yer."),
        ),
        "v3" to listOf(
            VenueReview("Rəşad Ə.", 5, "Meetup-lar üçün ən yaxşı məkan, proyektor və board var."),
        ),
        "v4" to listOf(
            VenueReview("Günay T.", 4, "Qrupla piknik üçün çox rahat, çay dəstəyi yaxşı qurulub."),
            VenueReview("Vüqar H.", 5, "Axşam canlı musiqi olur, ailəvi şərait."),
        ),
    )

    // ── Mock endirim təklifləri (ROL G) ──
    override val offers = listOf(
        CouponOffer(
            id = "o1", venueId = "v2", venueName = "Coffee Lab",
            label = "15% endirim", percent = 15, minRankIndex = 0, xpPrice = 200,
        ),
        CouponOffer(
            id = "o2", venueId = "v1", venueName = "Padel Arena",
            label = "20% endirim", percent = 20, minRankIndex = 2, xpPrice = 300,
        ),
        CouponOffer(
            id = "o3", venueId = "v4", venueName = "Dostlar Bağı",
            label = "10% endirim", percent = 10, minRankIndex = 0, xpPrice = 100,
            validityDays = 30,
        ),
        // Səviyyəyə görə avtomatik açılanlar (ROL G-a)
        CouponOffer(
            id = "o4", venueId = "v2", venueName = "Coffee Lab",
            label = "5% avtomatik endirim", percent = 5, minRankIndex = 2,
            xpPrice = 0, autoByLevel = true,
        ),
        CouponOffer(
            id = "o5", venueId = "v3", venueName = "Techno Hub",
            label = "İlk ziyarət pulsuz", fixedAzn = 5, minRankIndex = 1,
            xpPrice = 0, autoByLevel = true,
        ),
    )

    // ── İlkin bildirişlər (ROL H) ──
    override val initialNotifications = listOf(
        AppNotification(
            id = "n1", type = NotifType.NEARBY_EVENT,
            title = "Yaxınlıqda uyğun tədbir",
            body = "Şənbə saat 10:00-da Coffee Lab-da qəhvə söhbəti — maraqlarına uyğundur.",
            at = LocalDateTime.now().minusHours(2),
        ),
        AppNotification(
            id = "n2", type = NotifType.MARKETING,
            title = "Qoshul-a xoş gəlmisən!",
            body = "Profilini tamamla, +30 XP qazan və ilk tədbirinə qoşul.",
            at = LocalDateTime.now().minusDays(1), read = true,
        ),
        AppNotification(
            id = "n3", type = NotifType.EVENT_REMINDER,
            title = "Tədbirə 24 saat qalıb",
            body = "Padel oyunu — bu gün 18:00, Padel Arena.",
            at = LocalDateTime.now().minusDays(1).minusHours(3), read = true,
        ),
    )

    // ── D-026 (L.2): ilkin obyektə-məxsus Endirim XP hesabları ──
    // FRESH instance-lar (mutable hesablar singleton-da paylaşıla bilməz);
    // tarixçə seedHistory ilə — balans dəyişmir (Flutter ..ledger.addAll pariteti).
    override fun initialVenueXp(): Map<String, VenueXpAccount> = mapOf(
        "v5" to account("v5", "Xankəndi Restoranı", 325,
            VenueXpConfig(xpPerAzn = 20, maxDiscountPercent = 25, minBillAzn = 10),
            listOf(
                az.qoshul.app.core.model.XpLedgerEntry(
                    "xl1", "v5", true, 50, 50,
                    "Təşkilatçı mükafatı — \"Mafiya oyunu gecəsi\"",
                    LocalDateTime.of(2026, 10, 24, 23, 10)),
                az.qoshul.app.core.model.XpLedgerEntry(
                    "xl2", "v5", true, 275, 325,
                    "11 taq edilən iştirakçı · aktivite şəkli paylaşılıb",
                    LocalDateTime.of(2026, 10, 24, 23, 12)),
            )),
        "v2" to account("v2", "Coffee Lab", 120,
            VenueXpConfig(xpPerAzn = 25, maxDiscountPercent = 20, minBillAzn = 0),
            listOf(
                az.qoshul.app.core.model.XpLedgerEntry(
                    "xl3", "v2", true, 120, 120,
                    "Təşkilatçı + taq edilən iştirakçılar — \"Səhər qəhvə klubu\"",
                    LocalDateTime.of(2026, 10, 18, 10, 5)),
            )),
        "v6" to account("v6", "Kokaltı Kafesi", 0,
            VenueXpConfig(xpPerAzn = 20, maxDiscountPercent = 15, minBillAzn = 0), emptyList()),
        "v1" to account("v1", "Padel Arena", 340,
            VenueXpConfig(xpPerAzn = 20, maxDiscountPercent = 30, minBillAzn = 0),
            listOf(
                az.qoshul.app.core.model.XpLedgerEntry(
                    "xl4", "v1", true, 340, 340,
                    "Təşkilatçı + taq edilən iştirakçılar — \"Padel turniri\"",
                    LocalDateTime.of(2026, 10, 11, 21, 40)),
            )),
        "v4" to account("v4", "Dostlar Bağı", 150,
            VenueXpConfig(xpPerAzn = 30, maxDiscountPercent = 20, minBillAzn = 0),
            listOf(
                az.qoshul.app.core.model.XpLedgerEntry(
                    "xl5", "v4", true, 150, 150,
                    "Təşkilatçı + 4 taq edilən iştirakçı — \"Piknik günü\"",
                    LocalDateTime.of(2026, 10, 4, 17, 20)),
            )),
    )

    private fun account(
        id: String, name: String, balance: Int,
        config: VenueXpConfig, history: List<az.qoshul.app.core.model.XpLedgerEntry>,
    ) = VenueXpAccount(id, name, balance, config).apply {
        history.forEach { seedHistory(it) }
    }

    // ── Başlanğıc söhbətlər ──
    override val initialEventMessages = mapOf(
        "e1" to listOf(
            ChatMessage("Leyla Məmmədova", "Salam hamıya! Şənbə saat 10:00-da Coffee Lab-da görüşürük."),
            ChatMessage("Rəşad Əliyev", "Yarışıq, yerlər tükənməsin."),
            ChatMessage("Nigar Quliyeva", "Yeni gələnlər üçün masa rezervasiya etdim, narahat olmayın."),
        ),
        "e5" to listOf(
            ChatMessage("Aysel İsmayılova", "Bu gün 19:30-da İçərişəhərdəyik. Giriş kodu: 1994."),
            ChatMessage("Orxan Həsənov", "Karbon turniri üçün 2 nəfər axtarıram, kim qoşulur?"),
        ),
    )

    override val initialFriendMessages = mapOf(
        "p2" to listOf(
            ChatMessage("Rəşad Əliyev", "Salam! Padel görüşünə qoşuldun, əla seçimdir."),
            ChatMessage("Sən", "Salam, bəli! İlk dəfə sınayıram.", mine = true),
        ),
    )
}
