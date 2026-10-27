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
  });

  String get fullName => '$firstName $lastName';
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
  });
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

/// XP qaydaları — web rules.ts portları (birebir).
class XpRules {
  XpRules._();

  static const int participationXp = 50;
  static const int hostingXp = 100;
  static const int partnerHostingXp = 150;
  static const int reviewXp = 20;

  static const List<int> rankMax = [200, 600, 1500, 3500, 7000];
  static const List<String> rankNames = [
    'Yeni',
    'Kəşfiyyatçı',
    'Aktiv',
    'Veteran',
    'Ultras',
    'Legend',
  ];

  static String rankForXp(int xp) {
    for (var i = 0; i < rankMax.length; i++) {
      if (xp <= rankMax[i]) return rankNames[i];
    }
    return rankNames.last;
  }

  /// {nextName, toNext, pct} — web xpProgress port.
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
}

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
