import '../models/coupon.dart';
import '../models/models.dart';
import '../models/notification.dart';
import '../models/xp_economy.dart';

/// Mock data — UI mərhələsi (D-023). Məzmun web tətbiqinin seed datasına
/// əsaslanır; backend inteqrasiyasında API cavabları ilə əvəz olunacaq.

const imgFriends = 'assets/images/friends-baku.jpg';
const imgCoffee = 'assets/images/coffee.jpg';
const imgPadel = 'assets/images/padel.jpg';
const imgTech = 'assets/images/tech.jpg';

/// Şəkil seçimi: kateqoriya qrupuna görə (web-də seed-lər belə işləyir).
String imageForCategory(String category) {
  final g = groupOf(category);
  switch (g) {
    case 'İdman':
    case 'Təbiət':
      return imgPadel;
    case 'Yemək və içki':
      return imgCoffee;
    case 'Texnologiya':
    case 'Biznes və karyera':
    case 'Təhsil':
      return imgTech;
    default:
      return imgFriends;
  }
}

/// Kateqoriya qrupları — web categories.ts (birebir).
const categoryGroups = <CategoryGroupData>[
  CategoryGroupData('İdman', [
    'Futbol', 'Basketbol', 'Voleybol', 'Padel', 'Tennis', 'Stolüstü tennis', 'Üzgüçülük',
    'Qaçış', 'Velosiped', 'Yoga', 'Fitnes', 'Döyüş idmanı', 'Güləş', 'Şahmat', 'Boulinq',
  ]),
  CategoryGroupData('Təbiət', [
    'Yürüş', 'Kemping', 'Alpinizm', 'Balıqçılıq', 'Piknik', 'At sürmə', 'Quş müşahidəsi',
  ]),
  CategoryGroupData('Mədəniyyət', [
    'Muzey', 'Teatr', 'Kino', 'Sərgi', 'Kitab klubu', 'Poeziya gecəsi', 'Tarix turu',
  ]),
  CategoryGroupData('Musiqi', [
    'Konsert', 'Cem-sessiya', 'Karaoke', 'Muğam', 'Caz', 'DJ gecəsi', 'Musiqi aləti dərsi',
  ]),
  CategoryGroupData('Yemək və içki', [
    'Qəhvə', 'Çay söhbəti', 'Şam yeməyi', 'Səhər yeməyi', 'Kulinariya dərsi', 'Dequstasiya',
    'Küçə yeməkləri',
  ]),
  CategoryGroupData('Təhsil', [
    'Dil mübadiləsi', 'Seminar', 'Mühazirə', 'İmtahana hazırlıq', 'Birlikdə dərs', 'Elm söhbəti',
  ]),
  CategoryGroupData('Texnologiya', [
    'Hakaton', 'Proqramlaşdırma', 'Süni intellekt', 'Startap', 'Oyun hazırlama', 'Dizayn',
    'Robototexnika',
  ]),
  CategoryGroupData('Biznes və karyera', [
    'Networking', 'Karyera', 'Sahibkarlıq', 'Marketinq', 'Maliyyə savadlılığı',
    'Müsahibəyə hazırlıq',
  ]),
  CategoryGroupData('Oyunlar', [
    'Stolüstü oyunlar', 'Kibersport', 'Kviz', 'Mafiya', 'Nərd', 'Video oyunlar', 'Escape room',
  ]),
  CategoryGroupData('İncəsənət', [
    'Rəsm', 'Foto-gəzinti', 'Əl işləri', 'Rəqs', 'Xalçaçılıq', 'Keramika', 'Yazı emalatxanası',
  ]),
  CategoryGroupData('Könüllülük', [
    'Təmizlik aksiyası', 'Xeyriyyə', 'Heyvanlara yardım', 'Ağac əkmə', 'Qan donorluğu',
  ]),
  CategoryGroupData('Səyahət və digər', [
    'Şəhər turu', 'Səfər', 'Ailə', 'Sağlamlıq', 'Mənəvi inkişaf', 'Valideynlər', 'Tələbələr',
    'Digər',
  ]),
];

final allCategories = <String>[
  for (final g in categoryGroups) ...g.items,
];

String groupOf(String category) {
  for (final g in categoryGroups) {
    if (g.items.contains(category)) return g.group;
  }
  return 'Səyahət və digər';
}

/// Şəhərlər — web cities.ts-dən əsas seçim (bölgələrlə).
const cities = <City>[
  City('Bakı', 'Abşeron-Xızı', 40.4093, 49.8671),
  City('Sumqayıt', 'Abşeron-Xızı', 40.5897, 49.6686),
  City('Xırdalan', 'Abşeron-Xızı', 40.4481, 49.7553),
  City('Gəncə', 'Gəncə-Daşkəsən', 40.6828, 46.3606),
  City('Göygöl', 'Gəncə-Daşkəsən', 40.5856, 46.3189),
  City('Naftalan', 'Gəncə-Daşkəsən', 40.5044, 46.8397),
  City('Mingəçevir', 'Mərkəzi Aran', 40.7703, 47.0489),
  City('Yevlax', 'Mərkəzi Aran', 40.6172, 47.15),
  City('Göyçay', 'Mərkəzi Aran', 40.6531, 47.7406),
  City('Şirvan', 'Şirvan-Salyan', 39.9319, 48.9203),
  City('Salyan', 'Şirvan-Salyan', 39.5961, 48.9847),
  City('Neftçala', 'Şirvan-Salyan', 39.3586, 49.2469),
  City('Şəki', 'Şəki-Zaqatala', 41.1919, 47.1706),
  City('Zaqatala', 'Şəki-Zaqatala', 41.6336, 46.6433),
  City('Qax', 'Şəki-Zaqatala', 41.4228, 46.9242),
  City('Quba', 'Quba-Xaçmaz', 41.3625, 48.5122),
  City('Xaçmaz', 'Quba-Xaçmaz', 41.4606, 48.8022),
  City('Lənkəran', 'Lənkəran-Astara', 38.7529, 48.8475),
  City('Astara', 'Lənkəran-Astara', 38.4558, 48.8725),
  City('Naxçıvan', 'Naxçıvan MR', 39.2093, 45.4109),
  City('Şamaxı', 'Şirvan-Salyan', 40.6314, 48.6406),
  City('Qəbələ', 'Şəki-Zaqatala', 40.9875, 47.8439),
  City('İsmayıllı', 'Şəki-Zaqatala', 40.7358, 48.1817),
  City('Şuşa', 'Qarabağ', 39.8222, 46.7489),
  City('Xankəndi', 'Qarabağ', 39.8181, 46.7529),
];

City findCity(String name) {
  for (final c in cities) {
    if (c.name == name) return c;
  }
  return cities.first;
}

/// Mock tədbirlər — realistik Azərbaycan məzmunu (web seed üslubu).
final mockEvents = <QoshulEvent>[
  const QoshulEvent(
    id: 'e1',
    title: 'Şənbə səhəri qəhvə və söhbət',
    category: 'Qəhvə',
    image: imgCoffee,
    city: 'Bakı',
    location: 'Coffee Lab, Nərimanov',
    lat: 40.3922,
    lng: 49.8522,
    distance: 1.2,
    time: '10:00',
    date: 'Şənbə',
    past: false,
    capacity: 8,
    participants: 5,
    host: 'Leyla Məmmədova',
    description:
        'Səhər qəhvəmizi birlikdə içib həftəni planlaşdıraq. Yeni tanışlıqlar, ilham verici söhbətlər və gözəl əhval-ruhiyyə gözləyirik. Gələcəkdə bu görüşləri hər şənbə təkrarlayacayıq.',
    language: 'Azərbaycan dili',
    level: 'Hamı üçün',
  ),
  const QoshulEvent(
    id: 'e2',
    title: 'Padel oyunu — yeni başlayanlar üçün',
    category: 'Padel',
    image: imgPadel,
    city: 'Bakı',
    location: 'Padel Arena, Binəqədi',
    lat: 40.4236,
    lng: 49.8236,
    distance: 3.4,
    time: '18:00',
    date: 'Bu gün',
    past: false,
    capacity: 4,
    participants: 3,
    host: 'Rəşad Əliyev',
    description:
        'Padel ilk dəfə sınayırsansa, bu görüş sənin üçündür! Raketlər və meydan tərəfdaş məkanından, gələcəyin tərəfdaş endirimi ilə təmin olunur. Sualın olsa yaz.',
    price: 15,
    level: 'Başlanğıc',
    language: 'Qarışıq',
  ),
  const QoshulEvent(
    id: 'e3',
    title: 'Şənbə yürüşü — Şıx düzü marşrutu',
    category: 'Yürüş',
    image: imgFriends,
    city: 'Bakı',
    location: 'Şıx düzü başlanğıc nöqtəsi',
    lat: 40.3292,
    lng: 49.7689,
    distance: 7.8,
    time: '08:00',
    date: 'Şənbə',
    past: false,
    capacity: 15,
    participants: 9,
    host: 'Nigar Quliyeva',
    description:
        'Orta çətinlikdə 8 km marşrut. Su və rahat ayaqqabı yanına al. Yolda fotopauzalar olacaq. Qoşulma istəyi göndər — yerlər təsdiqləndikcə görünəcək.',
    approval: true,
    ageLimit: '16+',
  ),
  const QoshulEvent(
    id: 'e4',
    title: 'Dostluq kuboku — 7x7 futbol',
    category: 'Futbol',
    image: imgPadel,
    city: 'Bakı',
    location: 'Zığ idman kompleksi',
    lat: 40.3736,
    lng: 49.8356,
    distance: 5.1,
    time: '17:00',
    date: 'Bazar',
    past: false,
    capacity: 14,
    participants: 11,
    host: 'Orxan Həsənov',
    description:
        'Ənənəvi həftəlik futbol görüşümüz. Komandalar gəliş sırası ilə bölünür, səviyyə fərqi vacib deyil — əsas məqsəd hərəkət və yaxşı əhvaldır.',
    level: 'Orta',
  ),
  const QoshulEvent(
    id: 'e5',
    title: 'Stolüstü oyunlar gecəsi',
    category: 'Stolüstü oyunlar',
    image: imgFriends,
    city: 'Bakı',
    location: 'Board Game Cafe, İçərişəhər',
    lat: 40.3662,
    lng: 49.8339,
    distance: 2.3,
    time: '19:30',
    date: 'Bu gün',
    past: false,
    capacity: 10,
    participants: 7,
    host: 'Aysel İsmayılova',
    description:
        'Karbon, Katoma, Şahmat və daha 30+ oyun. Yeni oyunlar öyrənmək istəyənlər üçün masada izah edənlər olacaq. Hamılıqla çay sifariş veririk.',
    price: 5,
  ),
  const QoshulEvent(
    id: 'e6',
    title: 'Flutter meetup: state management',
    category: 'Proqramlaşdırma',
    image: imgTech,
    city: 'Bakı',
    location: 'Techno Hub, Nərimanov',
    lat: 40.3903,
    lng: 49.8556,
    distance: 3.9,
    time: '14:00',
    date: 'Noy 8',
    past: false,
    capacity: 40,
    participants: 26,
    host: 'Orxan Həsənov',
    description:
        'Bu dəfəki mövzu: real layihələrdə state management seçimi. İki qısa təqdimat + açıq müzakirə + networking. Təcrübəli iştirakçılar da, yeni başlayanlar da xoş gəlir.',
    language: 'Qarışıq',
  ),
  const QoshulEvent(
    id: 'e7',
    title: 'Dil mübadiləsi: AZ-EN söhbət klubu',
    category: 'Dil mübadiləsi',
    image: imgCoffee,
    city: 'Bakı',
    location: 'Fountain Square',
    lat: 40.3777,
    lng: 49.8731,
    distance: 0.8,
    time: '18:30',
    date: 'Şənbə',
    past: false,
    capacity: 12,
    participants: 8,
    host: 'Leyla Məmmədova',
    description:
        'Yarısı Azərbaycan, yarısı İngilis dilli söhbət formatı. Hər 15 dəqiqədə dəyişirik. Səviyyənə görə qruplar yoxdur — hamı bir masada.',
    level: 'Hamı üçün',
  ),
  const QoshulEvent(
    id: 'e8',
    title: 'Kviz gecəsi — komandalı yarış',
    category: 'Kviz',
    image: imgFriends,
    city: 'Bakı',
    location: 'Dinamo, Səbail',
    lat: 40.3617,
    lng: 49.8358,
    distance: 4.2,
    time: '20:00',
    date: 'Cümə',
    past: false,
    capacity: 24,
    participants: 18,
    host: 'Rəşad Əliyev',
    description:
        'Ümumi bilik + Azərbaycan tarixi + musiqi roundu. 4 nəfərlik komandalar. Qalib komandanın adı lövhədə qalır. Tək gələnləri yerindəcə komandalara bölürük.',
    price: 10,
  ),
  const QoshulEvent(
    id: 'e9',
    title: 'Gəncədə piknik — Gənc-Göl sahili',
    category: 'Piknik',
    image: imgFriends,
    city: 'Gəncə',
    location: 'Gənc-Göl istirahət zonası',
    lat: 40.7156,
    lng: 46.4711,
    distance: 2.0,
    time: '12:00',
    date: 'Bazar',
    past: false,
    capacity: 20,
    participants: 6,
    host: 'Nigar Quliyeva',
    description:
        'Xalı-piknik formatı: hər kəs bir növsnack gətirir, çay bizdən. Oyunlar, akustik gitara və göl kənarında sakit axşam. Yağış halında təxirə salınacaq.',
  ),
  const QoshulEvent(
    id: 'e10',
    title: 'Yoga səhəri — Dənizkənarı bulvar',
    category: 'Yoga',
    image: imgPadel,
    city: 'Bakı',
    location: 'Dənizkənarı bulvar, Kristal zal yanında',
    lat: 40.3695,
    lng: 49.8367,
    distance: 1.7,
    time: '07:30',
    date: 'Şənbə',
    past: false,
    capacity: 12,
    participants: 10,
    host: 'Aysel İsmayılova',
    description:
        'Dəniz havasında 60 dəqiqəlik yumşaq vinyasa. Matlar limitli sayda var — yazarkən qeyd et. Sonrasında birlikdə sağlam səhər yeməyi.',
    price: 8,
    level: 'Başlanğıc',
  ),
  const QoshulEvent(
    id: 'e11',
    title: 'Film gecəsi: azərbaycan klassikləri',
    category: 'Kino',
    image: imgFriends,
    city: 'Bakı',
    location: 'Nizami kino mərkəzi',
    lat: 40.3719,
    lng: 49.8397,
    distance: 2.9,
    time: '19:00',
    date: 'Cümə',
    past: false,
    capacity: 18,
    participants: 14,
    host: 'Leyla Məmmədova',
    description:
        'Bu ayın filmi müzakirə ilə seçilib. Seansdan sonra yaxınlıqdakı kafedə film haqqında söhbət. Biletləri əvvəlcədən alırıq.',
    price: 6,
  ),
  const QoshulEvent(
    id: 'e12',
    title: 'Könüllülük: Xəzər sahilinin təmizliyi',
    category: 'Təmizlik aksiyası',
    image: imgFriends,
    city: 'Bakı',
    location: 'Şıx çimərliyi',
    lat: 40.3453,
    lng: 49.7553,
    distance: 11.4,
    time: '10:00',
    date: 'Bazar',
    past: false,
    capacity: 30,
    participants: 12,
    host: 'Aysel İsmayılova',
    description:
        'Əlcəklər və zibil qabları təşkilatçılardan. 2 saatlıq birgə iş + sonrasında çay. Şəxsi su şüşəni gətir — plastik istifadə etməyək.',
    ageLimit: 'Hamı üçün',
  ),
  const QoshulEvent(
    id: 'e0',
    title: 'Keçən həftə qaçış klubu',
    category: 'Qaçış',
    image: imgPadel,
    city: 'Bakı',
    location: 'Dənizkənarı bulvar',
    lat: 40.3695,
    lng: 49.8367,
    distance: 1.5,
    time: '07:00',
    date: 'Keçən bazar',
    past: true,
    capacity: 12,
    participants: 12,
    host: 'Rəşad Əliyev',
    description:
        '5 km asan temp. Qoşulduğun üçün +50 XP qazandın — rəyini yazaraq əlavə +20 XP qazana bilərsən.',
  ),
  // D-026 (L.3) demo: istifadəçinin öz təşkil etdiyi, bitmiş aktivite —
  // "Aktiviteyi bitir" axını (statik QR → şəkil → taq → 50/25 XP) üçün.
  const QoshulEvent(
    id: 'e-mafiya',
    title: 'Mafiya oyunu gecəsi',
    category: 'Stolüstü oyunlar',
    image: imgFriends,
    city: 'Bakı',
    location: 'Xankəndi Restoranı, Nərimanov',
    lat: 40.3842,
    lng: 49.8641,
    distance: 2.2,
    time: '20:00',
    date: 'Keçən cümə',
    past: true,
    capacity: 10,
    participants: 5,
    host: 'Dost', // onboarding-də profileName ilə əvəz olunur (AppState)
    description:
        'Xankəndi Restoranında klassik mafiya gecəsi. 5 nəfərlik masa, oyun kartları bizdən. '
        'Aktivite bitəndə çıxışda restoranın QR-ını oxudub şəkilləri paylaşacağam — '
        'taq edilən hər kəs obyekt XP-si qazanır.',
    level: 'Orta',
    venueId: 'v5',
  ),
  // D-026 (L.1): Kəşf peşəkar sıralaması üçün yüksək çəkili nümunələr.
  const QoshulEvent(
    id: 'e-chess',
    title: 'Blits şahmat turniri — reytinqlilər',
    category: 'Şahmat',
    image: imgTech,
    city: 'Bakı',
    location: 'Techno Hub, Nərimanov',
    lat: 40.3903,
    lng: 49.8556,
    distance: 3.1,
    time: '19:30',
    date: 'Şənbə',
    past: false,
    capacity: 16,
    participants: 11,
    host: 'Nurlan Vəliyev',
    description:
        '5 dəqiqəlik blits, isveçrə sistemi, 7 raund. Reytinqi 1600+ olan oyunçular üçün. '
        'Yeni başlayanlara ayrıca masa olacaq.',
    level: 'Peşəkar',
    price: 10,
  ),
];

/// Mock insanlar — web data.ts people (adlar birebir) + ROL D kart sahələri
/// (yaş, ~məsafə, ortaq maraqlar; likesBack = mock qarşılıqlılıq).
const mockPeople = <Person>[
  Person(
    id: 'p1',
    firstName: 'Leyla',
    lastName: 'Məmmədova',
    image: imgCoffee,
    city: 'Bakı',
    status: 'student',
    org: 'Bakı Dövlət Universiteti',
    interest: 'Qəhvə · Səyahət',
    match: 94,
    reason: 'Hər ikiniz qəhvə görüşlərini və şəhər gəzintilərini sevirsiniz.',
    age: 23,
    distanceKm: 1.2,
    rolInterests: ['Kafe və Yemək', 'Təbiət və Səyahət', 'Mədəniyyət və İncəsənət'],
    likesBack: true,
  ),
  Person(
    id: 'p2',
    firstName: 'Rəşad',
    lastName: 'Əliyev',
    image: imgCoffee,
    city: 'Bakı',
    status: 'working',
    org: 'Texnologiya sektoru',
    interest: 'İdman · Texnologiya',
    match: 96,
    reason: 'İdman və texnologiya maraqlarınız üst-üstə düşür — padel görüşü üçün ideal.',
    age: 27,
    distanceKm: 3.4,
    rolInterests: ['İdman və Aktivlik', 'Təhsil və Karyera', 'Oyun və Hobbi'],
    likesBack: true,
  ),
  Person(
    id: 'p3',
    firstName: 'Nigar',
    lastName: 'Quliyeva',
    image: imgFriends,
    city: 'Bakı',
    status: 'student',
    org: 'Azərbaycan Dillər Universiteti',
    interest: 'Mədəniyyət · Musiqi',
    match: 91,
    reason: 'Mədəni tədbirlərə birgə getmək üçün yüksək uyğunluq.',
    age: 22,
    distanceKm: 0.8,
    rolInterests: ['Mədəniyyət və İncəsənət', 'Musiqi', 'Könüllülük və Sosial'],
    likesBack: false,
  ),
  Person(
    id: 'p4',
    firstName: 'Orxan',
    lastName: 'Həsənov',
    image: imgTech,
    city: 'Bakı',
    status: 'working',
    org: 'Frilanser',
    interest: 'Kodlaşdırma · Şahmat',
    match: 89,
    reason: 'Texnologiya meetuplarında birlikdə iştirak üçün uyğun profil.',
    age: 29,
    distanceKm: 5.1,
    rolInterests: ['Təhsil və Karyera', 'Oyun və Hobbi', 'İdman və Aktivlik'],
    likesBack: false,
  ),
  Person(
    id: 'p5',
    firstName: 'Aysel',
    lastName: 'İsmayılova',
    image: imgFriends,
    city: 'Bakı',
    status: 'student',
    org: 'ADA Universiteti',
    interest: 'Səyahət · Dizayn',
    match: 93,
    reason: 'Səyahət planları və dizayn maraqlarınız uyğundur.',
    age: 25,
    distanceKm: 2.3,
    rolInterests: ['Təbiət və Səyahət', 'Mədəniyyət və İncəsənət', 'Musiqi'],
    likesBack: true,
  ),
];

/// Mock tərəfdaş məkanlar — ROL E sahələri ilə (qiymət aralığı, endirim %,
/// avtomatik/manuel təsdiq, reyting, iş saatları).
const mockPartners = <Partner>[
  Partner(
    id: 'v1',
    name: 'Padel Arena',
    category: 'İdman',
    address: 'Böyük Şor şossesi 15',
    city: 'Bakı',
    deal: 'Səhər saatları -20%',
    hours: '08:00–12:00',
    image: imgPadel,
    lat: 40.4236,
    lng: 49.8236,
    priceRange: '₼₼₼',
    autoAccept: false, // partnyor təsdiqi tələb edir → GÖZLƏYİR
    rating: 4.8,
    reviewsCount: 34,
    openFrom: 8,
    openTo: 23,
    discountPercent: 20,
  ),
  Partner(
    id: 'v2',
    name: 'Coffee Lab',
    category: 'Qəhvə',
    address: 'Ə. Haqverdiyev 12, Nərimanov',
    city: 'Bakı',
    deal: '2+1 çay təklifi',
    hours: '09:00–22:00',
    image: imgCoffee,
    lat: 40.3922,
    lng: 49.8522,
    priceRange: '₼',
    autoAccept: true,
    rating: 4.6,
    reviewsCount: 51,
    openFrom: 9,
    openTo: 22,
    discountPercent: 15,
  ),
  Partner(
    id: 'v3',
    name: 'Techno Hub',
    category: 'Texnologiya',
    address: 'Nərimanov rayonu',
    city: 'Bakı',
    deal: 'İlk ziyarət pulsuz',
    hours: '10:00–20:00',
    image: imgTech,
    lat: 40.3903,
    lng: 49.8556,
    priceRange: '₼₼',
    autoAccept: true,
    rating: 4.4,
    reviewsCount: 19,
    openFrom: 10,
    openTo: 20,
    discountPercent: 0,
  ),
  Partner(
    id: 'v4',
    name: 'Dostlar Bağı',
    category: 'Piknik',
    address: 'Şıx düzü',
    city: 'Bakı',
    deal: 'Qruplara -10%',
    hours: '11:00–23:00',
    image: imgFriends,
    lat: 40.3292,
    lng: 49.7689,
    priceRange: '₼',
    autoAccept: true,
    rating: 4.7,
    reviewsCount: 27,
    openFrom: 11,
    openTo: 23,
    discountPercent: 10,
  ),
  // D-026 (L.2): obyektə-məxsus Endirim XP bazası demo-su üçün iki obyekt.
  Partner(
    id: 'v5',
    name: 'Xankəndi Restoranı',
    category: 'Restoran',
    address: 'Şəfəq qəsəbəsi, Nərimanov',
    city: 'Bakı',
    deal: 'XP kartı ilə hesabda endirim',
    hours: '10:00–23:00',
    image: imgFriends,
    lat: 40.3842,
    lng: 49.8641,
    priceRange: '₼₼',
    autoAccept: true,
    rating: 4.5,
    reviewsCount: 68,
    openFrom: 10,
    openTo: 23,
    discountPercent: 0,
  ),
  Partner(
    id: 'v6',
    name: 'Kokaltı Kafesi',
    category: 'Kafe',
    address: 'Kokaltı küçəsi 8',
    city: 'Bakı',
    deal: 'Yeni açılış — XP qazanmaq üçün aktivite keçir',
    hours: '09:00–22:00',
    image: imgCoffee,
    lat: 40.3781,
    lng: 49.8469,
    priceRange: '₼',
    autoAccept: true,
    rating: 4.3,
    reviewsCount: 9,
    openFrom: 9,
    openTo: 22,
    discountPercent: 0,
  ),
];

/// Status etiketləri — web onboarding.ts statuses.
const statusLabels = <String, String>{
  'school': 'Məktəbli',
  'student': 'Tələbə',
  'working': 'İşləyirəm',
  'job_seeking': 'İş axtarıram',
  'entrepreneur': 'Sahibkaram',
  'freelancer': 'Frilanserəm',
  'other': 'Digər',
};

String statusLabel(String? id) => id == null ? '' : (statusLabels[id] ?? '');

/// Onboarding seçimləri — web onboarding.ts.
const statuses = <String>['school', 'student', 'working', 'job_seeking', 'entrepreneur', 'freelancer', 'other'];
const universities = <String>[
  'Bakı Dövlət Universiteti', 'ADA Universiteti',
  'Azərbaycan Dövlət İqtisad Universiteti (UNEC)', 'Azərbaycan Texniki Universiteti',
  'Xəzər Universiteti', 'Qafqaz Universiteti', 'Bakı Mühəndislik Universiteti',
  'Azərbaycan Dillər Universiteti', 'Azərbaycan Tibb Universiteti', 'Digər',
];
const languageOptions = <String>[
  'Azərbaycan dili', 'İngilis dili', 'Rus dili', 'Türk dili', 'Alman dili',
  'Fransız dili', 'Ərəb dili', 'Fars dili', 'Çin dili', 'İspan dili',
  'Gürcü dili', 'Koreya dili', 'Yapon dili', 'İtalyan dili',
];
const languageLevels = <String>['Başlanğıc', 'Orta', 'Yaxşı', 'Ana dili'];
const goals = <String>[
  'Yeni dostlar tapmaq', 'İdman yoldaşı', 'Networking və karyera', 'Dil mübadiləsi',
  'Hobbi qrupu', 'Birlikdə öyrənmək', 'Səyahət yoldaşı', 'Könüllülük',
  'Şəhəri kəşf etmək', 'Startap komandası',
];
const availabilityOptions = <String>[
  'Həftəiçi səhər', 'Həftəiçi gündüz', 'Həftəiçi axşam', 'Şənbə', 'Bazar',
];
const groupSizes = <String>['Təkbətək', 'Kiçik qrup (3–6)', 'Orta qrup (7–15)', 'Böyük qrup (15+)'];
const budgets = <String>['Yalnız pulsuz', '10 AZN-ə qədər', '30 AZN-ə qədər', 'Fərq etmir'];

const traitLabels = <String, String>{
  'openness': 'Açıqlıq',
  'conscientiousness': 'Məsuliyyətlilik',
  'extraversion': 'Ekstroversiya',
  'agreeableness': 'Xoş münasibət',
  'stability': 'Emosional sabitlik',
};

const personalityQuestions = <MapEntry<String, MapEntry<String, bool>>>[
  MapEntry('Yeni insanlarla tanış olmaq mənə enerji verir.', MapEntry('extraversion', false)),
  MapEntry('Böyük məclislərdə özümü yorğun hiss edirəm.', MapEntry('extraversion', true)),
  MapEntry('Söhbəti adətən mən başladıram.', MapEntry('extraversion', false)),
  MapEntry('Yeni və qeyri-adi şeyləri sınamağı xoşlayıram.', MapEntry('openness', false)),
  MapEntry('İncəsənət və musiqi məni dərindən maraqlandırır.', MapEntry('openness', false)),
  MapEntry('Planlarımı vaxtında və dəqiq yerinə yetirirəm.', MapEntry('conscientiousness', false)),
  MapEntry('Görüşlərə adətən gecikirəm.', MapEntry('conscientiousness', true)),
  MapEntry('Başqalarının hisslərinə qarşı həssasam.', MapEntry('agreeableness', false)),
  MapEntry('Mübahisədə güzəştə getməyi bacarıram.', MapEntry('agreeableness', false)),
  MapEntry('Kiçik stresslərdən asanlıqla özümü topluyam.', MapEntry('stability', false)),
];

const onboardingSteps = <String>[
  'Əsas məlumat',
  'Təhsil və iş',
  'Dillər',
  'Maraqlar',
  'Məqsədlər',
  'Şəxsiyyət testi',
  'Açıq suallar',
  'Üstünlüklər',
];

/// Başlanğıc qrup söhbəti mesajları (mock).
const initialEventMessages = <String, List<ChatMessage>>{
  'e1': [
    ChatMessage(sender: 'Leyla Məmmədova', body: 'Salam hamıya! Şənbə saat 10:00-da Coffee Lab-da görüşürük.'),
    ChatMessage(sender: 'Rəşad Əliyev', body: 'Yarışıq, yerlər tükənməsin.'),
    ChatMessage(sender: 'Nigar Quliyeva', body: 'Yeni gələnlər üçün masa rezervasiya etdim, narahat olmayın.'),
  ],
  'e5': [
    ChatMessage(sender: 'Aysel İsmayılova', body: 'Bu gün 19:30-da İçərişəhərdəyik. Giriş kodu: 1994.'),
    ChatMessage(sender: 'Orxan Həsənov', body: 'Karbon turniri üçün 2 nəfər axtarıram, kim qoşulur?'),
  ],
};

/// Başlanğıc dost söhbəti (Rəşad — qəbul edilmiş dostluq).
const initialFriendMessages = <String, List<ChatMessage>>{
  'p2': [
    ChatMessage(sender: 'Rəşad Əliyev', body: 'Salam! Padel görüşünə qoşuldun, əla seçimdir.'),
    ChatMessage(sender: 'Sən', body: 'Salam, bəli! İlk dəfə sınayıram.', mine: true),
  ],
};

// ═══════════════════════════════════════════════════════════
// ROL (docs/product-spec.md) mock genişlənmələri — UI-2 (v0.3.0)
// ═══════════════════════════════════════════════════════════

/// ROL B Addım 4: 9 maraq kateqoriyası (min 3, max 8 seçilir).
const rolCategories = <String>[
  'Əyləncə və Gecə həyatı',
  'Kafe və Yemək',
  'İdman və Aktivlik',
  'Təbiət və Səyahət',
  'Mədəniyyət və İncəsənət',
  'Təhsil və Karyera',
  'Oyun və Hobbi',
  'Musiqi',
  'Könüllülük və Sosial',
];

/// ROL B: kateqoriya alt-başlıqları (nümunə etiketlər).
const rolCategoryHints = <String, String>{
  'Əyləncə və Gecə həyatı': 'klub, bar, karaoke',
  'Kafe və Yemək': 'kafe, restoran, kofe görüşləri',
  'İdman və Aktivlik': 'futbol, tennis, qaçış, velosiped, fitnes',
  'Təbiət və Səyahət': 'hiking, kamp, Qəbələ, Şahdağ',
  'Mədəniyyət və İncəsənət': 'teatr, muzey, konsert, sərgi',
  'Təhsil və Karyera': 'dil klubları, networking, seminar',
  'Oyun və Hobbi': 'stolüstü oyunlar, PlayStation, kitab klubu, foto',
  'Musiqi': 'canlı musiqi, jam session',
  'Könüllülük və Sosial': 'xeyriyyə, təmizlik aksiyaları',
};

/// Məkanların rezerv saat slotları (ROL E: tarix → saat → nəfər).
const venueSlotTimes = <String, List<String>>{
  'v1': ['10:00', '12:00', '16:00', '18:00', '20:00'],
  'v2': ['09:00', '11:00', '13:00', '15:00', '17:00', '19:00', '21:00'],
  'v3': ['10:00', '12:00', '14:00', '16:00', '18:00'],
  'v4': ['12:00', '14:00', '16:00', '18:00', '20:00'],
};

/// Növbəti 7 günün etiketləri (Bu gün / Sabah / Ay Gün).
List<String> nextDateLabels({int count = 7}) {
  final now = DateTime.now();
  const months = [
    'Yan', 'Fev', 'Mar', 'Apr', 'May', 'İyn', 'İyl', 'Avq', 'Sen', 'Okt', 'Noy', 'Dek'
  ];
  return List.generate(count, (i) {
    final d = now.add(Duration(days: i));
    if (i == 0) return 'Bu gün';
    if (i == 1) return 'Sabah';
    return '${months[d.month - 1]} ${d.day}';
  });
}

/// Məkan rəyləri (ROL E: məkan detal ekranı).
class VenueReview {
  final String author;
  final int stars;
  final String text;
  const VenueReview(this.author, this.stars, this.text);
}

const mockVenueReviews = <String, List<VenueReview>>{
  'v1': [
    VenueReview('Elnur K.', 5, 'Meydanlar əla, səhər saatları endirim həqiqətən işləyir.'),
    VenueReview('Sara M.', 4, 'Təşkilat yaxşıdır, yalnız qaraj yer axtarmaq lazım oldu.'),
  ],
  'v2': [
    VenueReview('Aysu R.', 5, 'Sərin qəhvə, mehriban kollektiv. 2+1 çay təklifi əladır.'),
    VenueReview('Kamran S.', 4, 'Bir az kalabalıq olur axşam, amma atmosfer xoşdur.'),
    VenueReview('Nərmin A.', 5, 'İlk görüş üçün ideal sakit yer.'),
  ],
  'v3': [
    VenueReview('Rəşad Ə.', 5, 'Meetup-lar üçün ən yaxşı məkan, proyektor və board var.'),
  ],
  'v4': [
    VenueReview('Günay T.', 4, 'Qrupla piknik üçün çox rahat, çay dəstəyi yaxşı qurulub.'),
    VenueReview('Vüqar H.', 5, 'Axşam canlı musiqi olur, ailəvi şərait.'),
  ],
};

/// Mock endirim təklifləri (ROL G): səviyyə-avto + XP ilə alınan.
const mockOffers = <CouponOffer>[
  CouponOffer(
    id: 'o1',
    venueId: 'v2',
    venueName: 'Coffee Lab',
    label: '15% endirim',
    percent: 15,
    minRankIndex: 0,
    xpPrice: 200,
  ),
  CouponOffer(
    id: 'o2',
    venueId: 'v1',
    venueName: 'Padel Arena',
    label: '20% endirim',
    percent: 20,
    minRankIndex: 2, // Aktiv
    xpPrice: 300,
  ),
  CouponOffer(
    id: 'o3',
    venueId: 'v4',
    venueName: 'Dostlar Bağı',
    label: '10% endirim',
    percent: 10,
    minRankIndex: 0,
    xpPrice: 100,
    validityDays: 30,
  ),
  // Səviyyəyə görə avtomatik açılanlar (ROL G-a):
  CouponOffer(
    id: 'o4',
    venueId: 'v2',
    venueName: 'Coffee Lab',
    label: '5% avtomatik endirim',
    percent: 5,
    minRankIndex: 2, // Aktiv səviyyə → bütün partnyor kafelərdə 5%
    xpPrice: 0,
    autoByLevel: true,
  ),
  CouponOffer(
    id: 'o5',
    venueId: 'v3',
    venueName: 'Techno Hub',
    label: 'İlk ziyarət pulsuz',
    fixedAzn: 5,
    minRankIndex: 1, // Kəşfiyyatçı
    xpPrice: 0,
    autoByLevel: true,
  ),
];

/// İlkin bildirişlər (mock) — ROL H tipləri.
final initialNotifications = <AppNotification>[
  AppNotification(
    id: 'n1',
    type: NotifType.nearbyEvent,
    title: 'Yaxınlıqda uyğun tədbir',
    body: 'Şənbə saat 10:00-da Coffee Lab-da qəhvə söhbəti — maraqlarına uyğundur.',
    at: DateTime.now().subtract(const Duration(hours: 2)),
  ),
  AppNotification(
    id: 'n2',
    type: NotifType.marketing,
    title: 'Qoshul-a xoş gəlmisən!',
    body: 'Profilini tamamla, +30 XP qazan və ilk tədbirinə qoşul.',
    at: DateTime.now().subtract(const Duration(days: 1)),
    read: true,
  ),
  AppNotification(
    id: 'n3',
    type: NotifType.eventReminder,
    title: 'Tədbirə 24 saat qalıb',
    body: 'Padel oyunu — bu gün 18:00, Padel Arena.',
    at: DateTime.now().subtract(const Duration(days: 1, hours: 3)),
    read: true,
  ),
];

/// D-026 (L.2): ilkin obyektə-məxsus Endirim XP hesabları (mock).
/// Ayrı baza qaydası: Xankəndi-də qazanılan XP yalnız Xankəndiyə aiddir.
/// Kokaltı 0 göstərilir — "bu obyektdə hələ XP yoxdur" halını nümayiş üçün.
Map<String, VenueXpAccount> initialVenueXp() => <String, VenueXpAccount>{
      'v5': VenueXpAccount(
        venueId: 'v5',
        venueName: 'Xankəndi Restoranı',
        balance: 325,
        config: const VenueXpConfig(
          xpPerAzn: 20, // 100 XP = 5 AZN
          maxDiscountPercent: 25,
          minBillAzn: 10,
        ),
      )..ledger.addAll(<XpLedgerEntry>[
          XpLedgerEntry(
            id: 'xl1',
            venueId: 'v5',
            grant: true,
            amount: 50,
            balanceAfter: 50,
            note: 'Təşkilatçı mükafatı — "Mafiya oyunu gecəsi"',
            at: DateTime(2026, 10, 24, 23, 10),
          ),
          XpLedgerEntry(
            id: 'xl2',
            venueId: 'v5',
            grant: true,
            amount: 275,
            balanceAfter: 325,
            note: '11 taq edilən iştirakçı · aktivite şəkli paylaşılıb',
            at: DateTime(2026, 10, 24, 23, 12),
          ),
        ]),
      'v2': VenueXpAccount(
        venueId: 'v2',
        venueName: 'Coffee Lab',
        balance: 120,
        config: const VenueXpConfig(
          xpPerAzn: 25, // 100 XP = 4 AZN
          maxDiscountPercent: 20,
          minBillAzn: 0,
        ),
      )..ledger.addAll(<XpLedgerEntry>[
          XpLedgerEntry(
            id: 'xl3',
            venueId: 'v2',
            grant: true,
            amount: 120,
            balanceAfter: 120,
            note: 'Təşkilatçı + taq edilən iştirakçılar — "Səhər qəhvə klubu"',
            at: DateTime(2026, 10, 18, 10, 5),
          ),
        ]),
      'v6': VenueXpAccount(
        venueId: 'v6',
        venueName: 'Kokaltı Kafesi',
        balance: 0,
        config: const VenueXpConfig(
          xpPerAzn: 20,
          maxDiscountPercent: 15,
          minBillAzn: 0,
        ),
      ),
      'v1': VenueXpAccount(
        venueId: 'v1',
        venueName: 'Padel Arena',
        balance: 340,
        config: const VenueXpConfig(
          xpPerAzn: 20, // 100 XP = 5 AZN
          maxDiscountPercent: 30,
          minBillAzn: 0,
        ),
      )..ledger.addAll(<XpLedgerEntry>[
          XpLedgerEntry(
            id: 'xl4',
            venueId: 'v1',
            grant: true,
            amount: 340,
            balanceAfter: 340,
            note: 'Təşkilatçı + taq edilən iştirakçılar — "Padel turniri"',
            at: DateTime(2026, 10, 11, 21, 40),
          ),
        ]),
      'v4': VenueXpAccount(
        venueId: 'v4',
        venueName: 'Dostlar Bağı',
        balance: 150,
        config: const VenueXpConfig(
          xpPerAzn: 30, // 100 XP ≈ 3.33 AZN
          maxDiscountPercent: 20,
          minBillAzn: 0,
        ),
      )..ledger.addAll(<XpLedgerEntry>[
          XpLedgerEntry(
            id: 'xl5',
            venueId: 'v4',
            grant: true,
            amount: 150,
            balanceAfter: 150,
            note: 'Təşkilatçı + 4 taq edilən iştirakçı — "Piknik günü"',
            at: DateTime(2026, 10, 4, 17, 20),
          ),
        ]),
    };
