import 'dart:convert';

/// Domain modelləri — web (repo-qoshul) tiplərinin Flutter portları.
/// UI mərhələsi (D-023): sadə tutum; backend fazasında DTO-lara çevriləcək.

class QoshulEvent {
  final String id;
  final String title;
  final String category;
  final String image;
  final String city;
  final String location;
  final double lat;
  final double lng;
  final double distance;
  final String time;
  final String date;
  final bool past;
  final int capacity;
  final int participants;
  final String host;
  final String description;
  final int? price;
  final String? ageLimit;
  final String? language;
  final String? level;
  final String format;
  final bool approval;
  final String recurring;
  final List<String> tags;

  const QoshulEvent({
    required this.id,
    required this.title,
    required this.category,
    required this.image,
    required this.city,
    required this.location,
    required this.lat,
    required this.lng,
    required this.distance,
    required this.time,
    required this.date,
    required this.past,
    required this.capacity,
    required this.participants,
    required this.host,
    required this.description,
    this.price,
    this.ageLimit,
    this.language,
    this.level,
    this.format = 'Oflayn',
    this.approval = false,
    this.recurring = 'Bir dəfəlik',
    this.tags = const [],
  });

  int get spotsLeft => capacity - participants < 0 ? 0 : capacity - participants;
  bool get isFull => participants >= capacity;
}

class Person {
  final String id;
  final String firstName;
  final String lastName;
  final String image;
  final String city;
  final String status; // status id: student / working / ...
  final String org; // universitet və ya iş sahəsi
  final String interest;
  final int match; // 0-100
  final String reason;
  final int age; // ROL D: kartda yaş göstərilir
  final double distanceKm; // "~2 km" (dəqiq koordinat YOX)
  final List<String> rolInterests; // ROL kateqoriyaları (ortaq maraq üçün)
  final bool likesBack; // mock: qarşılıqlı "Maraqlıyam" simulyasiyası

  const Person({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.image,
    required this.city,
    required this.status,
    required this.org,
    required this.interest,
    required this.match,
    required this.reason,
    this.age = 24,
    this.distanceKm = 1.5,
    this.rolInterests = const [],
    this.likesBack = false,
  });

  String get fullName => '$firstName $lastName';

  /// ROL: yalnız təxmini məsafə göstərilir.
  String get distanceLabel =>
      distanceKm < 1 ? '~500 m' : '~${distanceKm.round()} km';
}

class Partner {
  final String id;
  final String name;
  final String category;
  final String address;
  final String city;
  final String? deal;
  final String hours;
  final String image;
  final double lat;
  final double lng;
  // ROL E genişlənmələri (məkan detal + rezerv axını)
  final String priceRange; // ₼ / ₼₼ / ₼₼₼
  final bool autoAccept; // true → TƏSDİQLƏNDİ, false → GÖZLƏYİR
  final double rating; // 0-5
  final int reviewsCount;
  final int openFrom; // saat ("indi açıqdır" filtri üçün)
  final int openTo;
  final int discountPercent; // aktiv endirim (0 = yox)

  const Partner({
    required this.id,
    required this.name,
    required this.category,
    required this.address,
    required this.city,
    this.deal,
    required this.hours,
    required this.image,
    required this.lat,
    required this.lng,
    this.priceRange = '₼₼',
    this.autoAccept = true,
    this.rating = 4.6,
    this.reviewsCount = 12,
    this.openFrom = 9,
    this.openTo = 22,
    this.discountPercent = 0,
  });

  bool isOpenAt(int hour) => hour >= openFrom && hour < openTo;

  bool get hasDiscount => discountPercent > 0;
}

class City {
  final String name;
  final String region;
  final double lat;
  final double lng;

  const City(this.name, this.region, this.lat, this.lng);
}

class ChatMessage {
  final String sender;
  final String body;
  final bool mine;

  const ChatMessage({required this.sender, required this.body, this.mine = false});
}

class CategoryGroupData {
  final String group;
  final List<String> items;
  const CategoryGroupData(this.group, this.items);
}

class PersonalityResult {
  final String type;
  final String summary;
  final Map<String, int> traits;

  const PersonalityResult({required this.type, required this.summary, required this.traits});
}

/// XP qaydaları — ROL (docs/product-spec.md, bölmə F) dəyərləri.
/// UI fazası: mock simulyasiya; backend fazasında server tərəfində hesablanacaq
/// (qayda 8) və bu sinif DTO qaydalarına çevriləcək.
class XpRules {
  XpRules._();

  // — XP hadisələri (ROL F cədvəli) —
  static const int registrationXp = 50; // qeydiyyatı tamamlama
  static const int profileCompleteXp = 30; // profil 100%
  static const int phoneVerifyXp = 40; // telefon/hesab təsdiqi
  static const int dailyLoginXp = 5; // günlük giriş
  static const int streakBonusXp = 30; // 7 günlük seriya bonusu
  static const int matchXp = 10; // match (gündə max 3)
  static const int joinXp = 10; // tədbirə qoşulma
  static const int checkInXp = 40; // tədbirdə faktiki iştirak
  static const int hostingXp = 60; // tədbir yaratma (≥3 iştirakçı gələndə)
  static const int reservationXp = 30; // rezervdən istifadə (QR skan)
  static const int reviewXp = 15; // məkana rəy (həftədə max 3)
  static const int inviteXp = 100; // dost dəvəti

  // — limitlər —
  static const int matchXpDailyCap = 3;
  static const int reviewXpWeeklyCap = 3;
  static const int hostingMinParticipants = 3;
  static const int noShowLimit = 3; // 3 gəlməmə → 7 gün məhdudiyyət
  static const int noShowRestrictionDays = 7;
  static const int couponValidityDays = 14;

  // — onboarding qaydaları (ROL B) —
  static const int minAge = 18;
  static const int minInterests = 3;
  static const int maxInterests = 8;
  static const int bioLimit = 150;
  static const int maxPhotos = 6;

  // — check-in qorunması (ROL F) —
  static const int checkInMaxDistanceM = 200;

  // — səviyyələr (ROL F) —
  static const List<int> rankMax = [99, 299, 699, 1499, 2999];
  static const List<String> rankNames = [
    'Yeni',
    'Kəşfiyyatçı',
    'Aktiv',
    'Sosial',
    'Lider',
    'Əfsanə',
  ];

  static String rankForXp(int xp) {
    for (var i = 0; i < rankMax.length; i++) {
      if (xp <= rankMax[i]) return rankNames[i];
    }
    return rankNames.last;
  }

  /// Səviyyə indeksi (0=Yeni .. 5=Əfsanə) — kuponların min səviyyəsi üçün.
  static int rankIndexForXp(int xp) {
    for (var i = 0; i < rankMax.length; i++) {
      if (xp <= rankMax[i]) return i;
    }
    return rankNames.length - 1;
  }

  /// {nextName, toNext, pct} — web xpProgress portu (ROL hədləri ilə).
  static Map<String, dynamic> xpProgress(int xp) {
    var lower = 0;
    for (var i = 0; i < rankMax.length; i++) {
      final max = rankMax[i];
      if (xp <= max) {
        final span = max - lower;
        final pct = ((xp - lower) / span * 100).round().clamp(0, 100);
        return {
          'nextName': rankNames[i + 1],
          'toNext': max + 1 - xp,
          'pct': pct,
        };
      }
      lower = max;
    }
    return {'nextName': null, 'toNext': 0, 'pct': 100};
  }

  /// ROL B: 18+ yoxlaması (UI default aktiv; rəsmi təsdiq gözləyir).
  static bool isAdult(DateTime birth, [DateTime? now]) {
    final n = now ?? DateTime.now();
    var age = n.year - birth.year;
    final hadBirthday = n.month > birth.month ||
        (n.month == birth.month && n.day >= birth.day);
    if (!hadBirthday) age--;
    return age >= minAge;
  }
}

/// Nişanlar (ROL F): "İlk tədbir", "10 tədbir", "Səyyah", "Gecə quşu", "Təsdiqlənmiş".
class BadgeDef {
  final String id;
  final String name;
  final String description;
  const BadgeDef(this.id, this.name, this.description);
}

const badgeDefs = <BadgeDef>[
  BadgeDef('firstEvent', 'İlk tədbir', 'İlk tədbirinə qoşul'),
  BadgeDef('tenEvents', '10 tədbir', '10 tədbirdə iştirak et'),
  BadgeDef('traveler', 'Səyyah', 'Başqa şəhərdə tədbirə qoşul'),
  BadgeDef('nightOwl', 'Gecə quşu', 'Axşam 20:00-dan sonra check-in'),
  BadgeDef('verified', 'Təsdiqlənmiş', 'Telefon nömrəsini təsdiqlə'),
];

/// Assistant [TƏDBİR] parser — web assistant-event.ts portu.
/// Qaytarır: {text: təmiz mətn, proposal: EventProposal?}
class AssistantParse {
  final String text;
  final EventProposal? proposal;
  const AssistantParse(this.text, this.proposal);
}

AssistantParse parseAssistantEvent(String text) {
  final i = text.indexOf('[TƏDBİR]');
  if (i < 0) return AssistantParse(text, null);
  final clean = text.substring(0, i).trim();
  final raw = text.substring(i + 8).trim();
  final start = raw.indexOf('{');
  final end = raw.lastIndexOf('}');
  if (start < 0 || end <= start) return AssistantParse(clean, null);
  try {
    final map = jsonDecode(raw.substring(start, end + 1));
    if (map is! Map) return AssistantParse(clean, null);
    return AssistantParse(
      clean,
      EventProposal(
        title: map['title'] is String ? map['title'] as String : '',
        category: map['category'] is String ? map['category'] as String : '',
        date: map['date'] is String ? map['date'] as String : '',
        time: map['time'] is String ? map['time'] as String : '',
        capacity: map['capacity'] is int ? map['capacity'] as int : 8,
        location: map['location'] is String ? map['location'] as String : '',
        description: map['description'] is String ? map['description'] as String : '',
        partner: map['partner'] is String ? map['partner'] as String : '',
      ),
    );
  } catch (_) {
    return AssistantParse(clean, null);
  }
}

class EventProposal {
  final String title;
  final String category;
  final String date;
  final String time;
  final int capacity;
  final String location;
  final String description;
  final String partner;

  const EventProposal({
    required this.title,
    required this.category,
    required this.date,
    required this.time,
    required this.capacity,
    required this.location,
    required this.description,
    this.partner = '',
  });
}
