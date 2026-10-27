import 'package:flutter/foundation.dart';

import '../data/mock_data.dart';
import '../models/models.dart';

/// Tətbiq state-i — UI mərhələsi (D-023).
/// Sadə ChangeNotifier; backend inteqrasiyasında (m8) feature-first BLoC/Cubit
/// layihəsinə parçalanacaq. İnterfeys dili dəyişməyəcək şəkildə saxlanılıb.
class AppState extends ChangeNotifier {
  // — auth/onboarding —
  bool signedIn = false;
  bool onboarded = false;
  String profileName = 'Dost';
  String firstName = 'Dost';
  String lastName = '';
  String city = 'Bakı';
  String status = 'student';
  String university = '';
  String workField = '';
  List<String> languages = ['Azərbaycan dili'];
  List<String> interests = <String>[];
  List<String> goals = <String>[];
  PersonalityResult? personality;
  List<String> availability = <String>[];
  String groupSize = 'Kiçik qrup (3–6)';
  String budget = '10 AZN-ə qədər';

  // — gameplay —
  int xp = 320;

  // — events —
  final List<QoshulEvent> events = [...mockEvents];
  final Set<String> joined = {'e0'}; // keçmiş qaçış klubu
  final Set<String> pending = <String>{};
  final Set<String> saved = <String>{};

  // — insanlar / dostluqlar —
  /// personId -> none | sent | received | accepted
  final Map<String, String> friendships = {'p2': 'accepted'};

  // — söhbətlər —
  final Map<String, List<ChatMessage>> eventMessages = {...initialEventMessages};
  final Map<String, List<ChatMessage>> friendMessages = {...initialFriendMessages};

  // — assistant —
  final List<ChatMessage> assistantMsgs = <ChatMessage>[];
  bool assistantBusy = false;

  // — köməkçi getterlər —
  Iterable<QoshulEvent> get upcoming sync* {
    for (final e in events) {
      if (!e.past) yield e;
    }
  }

  List<QoshulEvent> get cityEvents =>
      upcoming.where((e) => e.city == city).toList(growable: false);

  String get rank => XpRules.rankForXp(xp);

  Map<String, dynamic> get xpProgress => XpRules.xpProgress(xp);

  int get joinedUpcomingCount => joined.where((id) => _byId(id)?.past == false).length;

  QoshulEvent? _byId(String id) {
    for (final e in events) {
      if (e.id == id) return e;
    }
    return null;
  }

  QoshulEvent? eventById(String id) => _byId(id);

  // — auth —
  void signIn() {
    signedIn = true;
    notifyListeners();
  }

  void signOut() {
    signedIn = false;
    notifyListeners();
  }

  // — onboarding —
  void completeOnboarding({
    required String first,
    required String last,
    required String city,
    required String status,
    String university = '',
    String workField = '',
    List<String>? langs,
    List<String>? intr,
    List<String>? gls,
    PersonalityResult? pers,
    List<String>? avail,
    String? gSize,
    String? bud,
  }) {
    firstName = first;
    lastName = last;
    profileName = '$first $last'.trim();
    this.city = city;
    this.status = status;
    this.university = university;
    this.workField = workField;
    if (langs != null) languages = langs;
    if (intr != null) interests = intr;
    if (gls != null) goals = gls;
    personality = pers;
    if (avail != null) availability = avail;
    if (gSize != null) groupSize = gSize;
    if (bud != null) budget = bud;
    onboarded = true;
    notifyListeners();
  }

  // — profil —
  void setProfileName(String name) {
    profileName = name;
    final parts = name.trim().split(RegExp(r'\s+'));
    firstName = parts.isNotEmpty ? parts.first : name;
    lastName = parts.length > 1 ? parts.sublist(1).join(' ') : '';
    notifyListeners();
  }

  void setCity(String name) {
    city = name;
    notifyListeners();
  }

  // — tədbir əməliyyatları —
  bool join(String id) {
    final e = _byId(id);
    if (e == null || e.past || e.isFull || joined.contains(id)) return false;
    if (e.approval) {
      pending.add(id);
    } else {
      joined.add(id);
      xp += XpRules.participationXp;
    }
    notifyListeners();
    return true;
  }

  void leave(String id) {
    joined.remove(id);
    notifyListeners();
  }

  void toggleSave(String id) {
    if (!saved.remove(id)) saved.add(id);
    notifyListeners();
  }

  void createEvent({
    required String title,
    required String category,
    required String city,
    required String location,
    required String date,
    required String time,
    required int capacity,
    required String description,
    int price = 0,
    String ageLimit = 'Hamı üçün',
    String language = 'Azərbaycan dili',
    String level = 'Hamı üçün',
    String format = 'Oflayn',
    bool approval = false,
    String recurring = 'Bir dəfəlik',
    List<String> tags = const [],
    double? lat,
    double? lng,
  }) {
    final c = findCity(city);
    final id = 'c${events.length + 1}-${DateTime.now().millisecondsSinceEpoch % 100000}';
    events.insert(
      0,
      QoshulEvent(
        id: id,
        title: title,
        category: category,
        image: imageForCategory(category),
        city: city,
        location: format == 'Onlayn' ? 'Onlayn görüş' : location,
        lat: lat ?? c.lat,
        lng: lng ?? c.lng,
        distance: 0,
        time: time,
        date: date,
        past: false,
        capacity: capacity,
        participants: 1,
        host: profileName,
        description: description,
        price: price > 0 ? price : null,
        ageLimit: ageLimit,
        language: language,
        level: level,
        format: format,
        approval: approval,
        recurring: recurring,
        tags: tags,
      ),
    );
    joined.add(id);
    xp += XpRules.hostingXp;
    notifyListeners();
  }

  // — rəy (keçmiş tədbir) —
  void addReview(String eventId, int rating, String comment) {
    xp += XpRules.reviewXp;
    notifyListeners();
  }

  // — söhbətlər —
  void sendEventMessage(String eventId, String text) {
    final list = eventMessages.putIfAbsent(eventId, () => <ChatMessage>[]);
    list.add(ChatMessage(sender: 'Sən', body: text, mine: true));
    // Mock cavab: 1.2 saniyə sonra iştirakçıdan qısa reaksiya.
    _mockReply(eventMessages[eventId]!, eventId);
    notifyListeners();
  }

  void sendFriendMessage(String personId, String text) {
    final list = friendMessages.putIfAbsent(personId, () => <ChatMessage>[]);
    list.add(ChatMessage(sender: 'Sən', body: text, mine: true));
    notifyListeners();
  }

  void _mockReply(List<ChatMessage> list, String eventId) {
    Future.delayed(const Duration(milliseconds: 1200), () {
      if (!hasListeners) return;
      final e = _byId(eventId);
      const replies = [
        'Əla, görüşərik!',
        'Super, gözləyirəm.',
        'Razıyam, saat dəqiqdir.',
      ];
      list.add(ChatMessage(
        sender: e?.host ?? 'İştirakçı',
        body: replies[DateTime.now().second % replies.length],
      ));
      notifyListeners();
    });
  }

  // — insanlar —
  void requestFriend(String personId) {
    friendships[personId] = 'sent';
    notifyListeners();
  }

  void acceptFriend(String personId) {
    friendships[personId] = 'accepted';
    notifyListeners();
  }

  List<Person> get acceptedFriends => mockPeople
      .where((p) => friendships[p.id] == 'accepted')
      .toList(growable: false);

  // — assistant (mock AI) —
  /// Süni intellekt simulyasiyası: canlı cavablar mock məntiqlə.
  /// Backend fazasında streamAi API çağırışına çevriləcək.
  void sendAssistant(String text) {
    if (text.trim().isEmpty || assistantBusy) return;
    assistantMsgs.add(ChatMessage(sender: 'Sən', body: text.trim(), mine: true));
    assistantBusy = true;
    notifyListeners();

    final reply = _assistantReplyFor(text.trim());

    // Yazı simulyasiyası: hər 24 ms-da 2 simvol.
    final list = assistantMsgs;
    var shown = '';
    list.add(const ChatMessage(sender: 'Köməkçi', body: ''));
    void tick() {
      if (!assistantBusy) return;
      if (shown.length >= reply.length) {
        assistantBusy = false;
        notifyListeners();
        return;
      }
      shown = reply.substring(0, shown.length + 2);
      list.last = ChatMessage(sender: 'Köməkçi', body: shown);
      notifyListeners();
      Future.delayed(const Duration(milliseconds: 24), tick);
    }

    Future.delayed(const Duration(milliseconds: 400), tick);
  }

  String _assistantReplyFor(String q) {
    final lower = q.toLowerCase();
    final wantsProposal = ['fikri', 'fikir', 'təklif', 'plan', 'şənbə', 'bazar', 'təşkil']
        .any((w) => lower.contains(w));
    if (wantsProposal) {
      return 'Şənbə üçün əla planım var! Yeni başlayanlar üçün asan yürüş marşrutu, '
          'sonrasında isə birlikdə çay. Təklifim aşağıda — bir düymə ilə tədbirə çevirə bilərsən.\n'
          '[TƏDBİR]{"title":"Şənbə səhəri Qala yürüşü","category":"Yürüş",'
          '"date":"${_nextSaturdayIso()}","time":"09:00","capacity":10,'
          '"location":"Qala qalası, Bakı","description":"Qala qalası ətrafında 5 km asan '
          'yürüş + qala divarlarında fotopauza. Sonrasında yaxınlıqdakı kafedə çay.",'
          '"partner":""}';
    }
    if (lower.contains('pulsuz') || lower.contains('ödənişsiz')) {
      final free = upcoming.where((e) => e.price == null && e.city == city).take(3).toList();
      if (free.isEmpty) return 'Bu şəhərdə hazırda pulsuz tədbir görmürəm. Amma özünüq pulsuz tədbir yarada bilərsən!';
      return 'Pulsuz tədbirlər bunlardır:\n\n'
          + free.map((e) => '• ${e.title} — ${e.date}, ${e.time} (${e.location})').join('\n');
    }
    if (lower.contains('idman') || lower.contains('spor')) {
      final sport = upcoming
          .where((e) => groupOf(e.category) == 'İdman' || groupOf(e.category) == 'Təbiət')
          .take(3)
          .toList();
      if (sport.isEmpty) return 'İdman tədbirləri hələ azdır — ilk idman görüşünü sən yarat!';
      return 'Aktiv idman tədbirləri:\n\n'
          + sport.map((e) => '• ${e.title} — ${e.date} ${e.time}, ${e.participants}/${e.capacity} nəfər').join('\n');
    }
    if (lower.contains('gəncə')) {
      final g = upcoming.where((e) => e.city == 'Gəncə').toList();
      if (g.isEmpty) return 'Gəncədə hazırda plan yoxdur — amma e9 pikniki yaxındadır, bax!';
      return 'Gəncədə olan görüşlər:\n\n'
          + g.map((e) => '• ${e.title} — ${e.date}, ${e.time}').join('\n');
    }
    final rec = cityEvents.take(2).toList();
    if (rec.isEmpty) return 'Bu şəhərdə hələ tədbir yoxdur. İlk tədbiri sən yarat — mən kömək edərəm!';
    return 'Sənin üçün tövsiyələrim:\n\n'
        + rec.map((e) => '• ${e.title} (${e.category}) — ${e.date} ${e.time}').join('\n')
        + '\n\nHər hansı birini seçmək istəsən, "Sənin üçün" bölməsində AI seçimi də sınaya bilərsən.';
  }

  String _nextSaturdayIso() {
    final now = DateTime.now();
    final daysAhead = (DateTime.saturday - now.weekday) % 7;
    final d = now.add(Duration(days: daysAhead == 0 ? 7 : daysAhead));
    return '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  }

  /// "AI ilə seç" (Sənin üçün bölməsi) — mock tövsiyə səbəbləri.
  List<(QoshulEvent, String)> aiRecommendations() {
    final list = cityEvents.take(3).toList();
    const reasons = [
      'Profilindəki maraqlara ən yaxın tədbir.',
      'Yaxın məsafədədir və yerlər az qalıb.',
      'Yeni başlayanlar üçün uyğun səviyyə.',
    ];
    return [
      for (var i = 0; i < list.length; i++) (list[i], i < reasons.length ? reasons[i] : ''),
    ];
  }
}
