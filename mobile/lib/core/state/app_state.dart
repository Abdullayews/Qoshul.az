import 'package:flutter/foundation.dart';

import '../data/mock_data.dart';
import '../models/coupon.dart';
import '../models/models.dart';
import '../models/moderation.dart';
import '../models/notification.dart';
import '../models/reservation.dart';

/// Tətbiq state-i — UI mərhələsi (D-023/D-024).
///
/// v0.3.0: ROL məhsul məntiqi (XP/kupon/rezerv/match/bildiriş/moderasiya)
/// mock-data ilə simulyasiya olunur. Server-only qaydalar (ROL 8-9:
/// idempotency, gündəlik limitlər, balans≠ümumi) klient tərəfində da
/// eyni qaydalarla modelləşdirilib — backend fazasında eyni məntiq
/// serverə köçürüləcək (feature-first BLoC/Cubit parçalanması ilə).
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

  // — ROL B: profil yaradılması —
  DateTime? birthDate;
  String gender = ''; // isteğe bağlı (ROL B Addım 3)
  String bio = '';
  int photoCount = 1; // mock: 1 məcburi, 6-ya qədər
  bool phoneVerified = false; // ROL B: sonradan, badge + XP ilə
  bool profileCompleted = false; // 100% bonus XP üçün

  // — ROL TAB 5: məxfilik —
  bool visibleOnMap = true; // xəritədə görünmə
  bool showDistance = true; // məsafə göstərilməsi

  // — XP (ROL F): ümumi qazanılan ≠ xərclənə bilən balans —
  int xp = 320; // ümumi qazanılmış XP (səviyyə bununla hesablanır, düşmür)
  int xpBalance = 320; // kupon almaq üçün balans
  final Set<String> _xpEventIds = <String>{}; // idempotency (ROL F qorunma)
  final Set<String> earnedBadges = <String>{};
  int _matchXpToday = 0;
  DateTime _matchXpDay = DateTime.now();
  int _reviewXpThisWeek = 0;
  int loginStreak = 1;
  bool _dailyLoginDone = false;

  // — tədbirlər —
  final List<QoshulEvent> events = [...mockEvents];
  final Set<String> joined = {'e0'}; // keçmiş qaçış klubu
  final Set<String> pending = <String>{};
  final Set<String> saved = <String>{};
  final Set<String> checkedIn = <String>{};

  // — insanlar / dostluqlar —
  /// personId -> none | sent | received | accepted
  final Map<String, String> friendships = {'p2': 'accepted'};

  // — ROL D: match —
  /// personId -> true (Maraqlıyam) | false (Keç)
  final Map<String, bool> swipes = <String, bool>{};
  final Set<String> matches = <String>{};

  // — ROL E: rezervlər —
  final List<Reservation> reservations = <Reservation>[];
  int noShowCount = 0;
  DateTime? restrictedUntil; // no-show 3x → 7 gün (ROL E-7)

  // — ROL G: kuponlar —
  final List<CouponOffer> offers = [...mockOffers];
  final List<UserCoupon> coupons = <UserCoupon>[];
  int _couponSeq = 1;

  // — ROL H: bildirişlər —
  final List<AppNotification> notifications = [...initialNotifications];
  final NotificationPrefs notifPrefs = NotificationPrefs();

  // — ROL I: moderasiya —
  final Set<String> blocked = <String>{};
  final Map<String, int> reportCounts = <String, int>{};
  final Set<String> restrictedUsers = <String>{}; // 3+ report → gizlədilir
  final List<ReportEntry> reports = <ReportEntry>[];

  // — söhbətlər —
  final Map<String, List<ChatMessage>> eventMessages = {...initialEventMessages};
  final Map<String, List<ChatMessage>> friendMessages = {...initialFriendMessages};

  // — assistant —
  final List<ChatMessage> assistantMsgs = <ChatMessage>[];
  bool assistantBusy = false;

  // ═══════════ XP mühərriki (ROL F: idempotent + server qaydaları) ═══════════

  /// İdemponent XP mükafatı — eyni açar ikinci dəfə sayılmır (ROL F).
  bool _award(String key, int amount) {
    if (_xpEventIds.contains(key)) return false;
    _xpEventIds.add(key);
    final beforeRank = rankIndex;
    xp += amount;
    xpBalance += amount;
    _maybeLevelUp(beforeRank);
    return true;
  }

  void _maybeLevelUp(int beforeIndex) {
    final after = rankIndex;
    if (after > beforeIndex) {
      _notify(
        NotifType.levelUp,
        'Yeni səviyyə: ${XpRules.rankNames[after]}',
        'Təbriklər! Səviyyə artımı yeni endirim imkanları açır.',
      );
    }
  }

  /// ROL B Addım 6: qeydiyyatı tamamlayana +50 XP (bir dəfə).
  void completeRegistration() {
    _award('registration', XpRules.registrationXp);
  }

  /// ROL F: profil 100% (şəkil + bio + maraqlar) → +30 (bir dəfə).
  void completeProfileFull() {
    if (photoCount >= 1 && bio.trim().isNotEmpty && interests.length >= XpRules.minInterests) {
      if (_award('profile-full', XpRules.profileCompleteXp)) {
        profileCompleted = true;
      }
    }
    notifyListeners();
  }

  /// ROL B: telefon təsdiqi → badge + +40 XP (bir dəfə).
  void verifyPhone() {
    if (phoneVerified) return;
    phoneVerified = true;
    earnedBadges.add('verified');
    _award('phone-verify', XpRules.phoneVerifyXp);
    notifyListeners();
  }

  /// ROL F: günlük giriş +5; 7 günlük seriya bonusu +30.
  void dailyLogin() {
    if (_dailyLoginDone) return;
    _dailyLoginDone = true;
    _award('login-${DateTime.now().toIso8601String().substring(0, 10)}',
        XpRules.dailyLoginXp);
    if (loginStreak == 7) {
      _award('streak-7-${DateTime.now().toIso8601String().substring(0, 10)}',
          XpRules.streakBonusXp);
      loginStreak = 1;
    } else {
      loginStreak++;
    }
    notifyListeners();
  }

  // ═══════════ Köməkçi getterlər ═══════════

  Iterable<QoshulEvent> get upcoming sync* {
    for (final e in events) {
      if (!e.past) yield e;
    }
  }

  List<QoshulEvent> get cityEvents =>
      upcoming.where((e) => e.city == city).toList(growable: false);

  String get rank => XpRules.rankForXp(xp);

  int get rankIndex => XpRules.rankIndexForXp(xp);

  Map<String, dynamic> get xpProgress => XpRules.xpProgress(xp);

  int get joinedUpcomingCount => joined.where((id) => _byId(id)?.past == false).length;

  QoshulEvent? _byId(String id) {
    for (final e in events) {
      if (e.id == id) return e;
    }
    return null;
  }

  QoshulEvent? eventById(String id) => _byId(id);

  // ═══════════ Auth ═══════════

  void signIn() {
    signedIn = true;
    notifyListeners();
  }

  void signOut() {
    signedIn = false;
    notifyListeners();
  }

  // ═══════════ Onboarding (ROL B) ═══════════

  void setBirthDate(DateTime d) {
    birthDate = d;
    notifyListeners();
  }

  void setProfileCreation({
    required String first,
    required String last,
    required DateTime birth,
    String gdr = '',
    String bioText = '',
    int photos = 1,
  }) {
    firstName = first;
    lastName = last;
    profileName = '$first $last'.trim();
    birthDate = birth;
    gender = gdr;
    bio = bioText;
    photoCount = photos;
    notifyListeners();
  }

  void setInterests(List<String> intr) {
    interests = intr;
    notifyListeners();
  }

  /// ROL B axınının sonu: +50 XP və onboarded.
  void finishOnboarding() {
    completeRegistration();
    onboarded = true;
    dailyLogin();
    notifyListeners();
  }

  // Eski imza saxlanılır (yalnız testlər/əvvəlki ekranlar üçün).
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

  // ═══════════ Profil ═══════════

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

  void setVisibleOnMap(bool v) {
    visibleOnMap = v;
    notifyListeners();
  }

  void setShowDistance(bool v) {
    showDistance = v;
    notifyListeners();
  }

  // ═══════════ Tədbir əməliyyatları (ROL TAB 3) ═══════════

  bool join(String id) {
    final e = _byId(id);
    if (e == null || e.past || e.isFull || joined.contains(id)) return false;
    if (e.approval) {
      pending.add(id);
    } else {
      joined.add(id);
      _award('join-$id', XpRules.joinXp); // ROL F: qoşulma +10
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

  /// ROL TAB 3: məkanda QR/yerləşmə ilə check-in → +40 XP (ROL F).
  /// Mock: yerləşmə yoxlaması (max 200 m — XpRules.checkInMaxDistanceM)
  /// həmişə uğurludur; backend fazasında həqiqi geocheck olacaq.
  bool checkInEvent(String id) {
    final e = _byId(id);
    if (e == null || !joined.contains(id) || checkedIn.contains(id)) return false;
    checkedIn.add(id);
    _award('checkin-$id', XpRules.checkInXp);
    // Nişanlar (ROL F)
    if (!earnedBadges.contains('firstEvent')) {
      earnedBadges.add('firstEvent');
    } else if (checkedIn.length >= 10) {
      earnedBadges.add('tenEvents');
    }
    if (e.city != city) earnedBadges.add('traveler');
    final hour = DateTime.now().hour;
    if (hour >= 20) earnedBadges.add('nightOwl');
    // ROL F: tədbir yaratma XP — öz tədbirimdə ≥3 iştirakçı gəlibsə
    if (e.host == profileName && e.participants >= XpRules.hostingMinParticipants) {
      _award('hosting-$id', XpRules.hostingXp);
    }
    notifyListeners();
    return true;
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
    // ROL F: yaratma XP dərhal YOX — ≥3 iştirakçı check-in-də verilir.
    notifyListeners();
  }

  /// ROL F: rəy +15, həftədə max 3 (vaxtı gələn tədbir + məkan rəyi).
  bool addReview(String eventId, int rating, String comment) {
    if (_reviewXpThisWeek >= XpRules.reviewXpWeeklyCap) return false;
    _reviewXpThisWeek++;
    _award('review-$eventId', XpRules.reviewXp);
    notifyListeners();
    return true;
  }

  // ═══════════ Match (ROL D) ═══════════

  Person? _personById(String id) {
    for (final p in mockPeople) {
      if (p.id == id) return p;
    }
    return null;
  }

  /// Match namizədləri: swipe edilməyib, bloklanıb YOX, məhdudlaşdırılıb YOX.
  List<Person> get matchCandidates => mockPeople
      .where((p) =>
          !swipes.containsKey(p.id) &&
          !blocked.contains(p.id) &&
          !restrictedUsers.contains(p.id))
      .toList(growable: false);

  /// ROL D: "Maraqlıyam" / "Keç".
  /// Qaytarır: true → qarşılıqlı match (mock: likesBack).
  bool swipe(String personId, bool interested) {
    if (swipes.containsKey(personId) || blocked.contains(personId)) return false;
    swipes[personId] = interested;
    if (!interested) {
      notifyListeners();
      return false;
    }
    final p = _personById(personId);
    if (p == null || !p.likesBack) {
      notifyListeners();
      return false;
    }
    // Match! → bildiriş + chat + XP (gündə max 3) (ROL D)
    matches.add(personId);
    friendships[personId] = 'accepted';
    friendMessages.putIfAbsent(personId, () => <ChatMessage>[]);
    _notify(
      NotifType.match,
      'Yeni match: ${p.fullName}',
      'Qarşılıqlı maraq yarandı — söhbəti başlat!',
    );
    final today = DateTime.now();
    if (_matchXpDay.day != today.day) {
      _matchXpDay = today;
      _matchXpToday = 0;
    }
    if (_matchXpToday < XpRules.matchXpDailyCap) {
      _matchXpToday++;
      _award('match-$personId', XpRules.matchXp);
    }
    notifyListeners();
    return true;
  }

  /// Demo üçün: kart dəstini sıfırla (yalnız mock).
  void resetSwipes() {
    swipes.clear();
    notifyListeners();
  }

  // ═══════════ Rezerv (ROL E) ═══════════

  bool get reservationRestricted =>
      restrictedUntil != null && DateTime.now().isBefore(restrictedUntil!);

  /// ROL E axını: tarix → slot → nəfər → qeyd → kupon → təsdiq.
  /// status: GÖZLƏYİR (manuel təsdiq məkanı) və ya TƏSDİQLƏNDİ (avto).
  /// Qaytarır: yaranan rezerv; məhdudiyyət varsa null.
  Reservation? createReservation({
    required Partner venue,
    required String date,
    required String time,
    required int party,
    String note = '',
    String? couponCode,
  }) {
    if (reservationRestricted) return null;
    // Kupon yoxlaması (ROL G): aktiv + bu məkana aiddir
    UserCoupon? coupon;
    if (couponCode != null) {
      for (final c in coupons) {
        if (c.code == couponCode) {
          coupon = c;
          break;
        }
      }
      if (coupon == null || !coupon.isActive || coupon.venueId != venue.id) {
        return null;
      }
    }
    final id = 'r${reservations.length + 1}';
    final resv = Reservation(
      id: id,
      venueId: venue.id,
      venueName: venue.name,
      venueImage: venue.image,
      date: date,
      time: time,
      party: party,
      note: note,
      couponCode: coupon?.code,
      qrCode: 'QSH-$id-${DateTime.now().millisecondsSinceEpoch % 10000}',
      createdAt: DateTime.now(),
      status: venue.autoAccept ? ResvStatus.confirmed : ResvStatus.pending,
    );
    reservations.insert(0, resv);
    _notify(
      NotifType.resvUpdate,
      venue.autoAccept ? 'Rezerv təsdiqləndi' : 'Rezerv gözləyir',
      '${venue.name} · $date $time · $party nəfər'
          '${venue.autoAccept ? '' : ' — partnyor təsdiqini gözləyir'}',
    );
    notifyListeners();
    return resv;
  }

  /// ROL E-6: partnyor QR-i skan edir → İSTİFADƏ OLUNDU + XP + kupon istifadə.
  bool checkInReservation(String id) {
    Reservation? resv;
    for (final r in reservations) {
      if (r.id == id) resv = r;
    }
    if (resv == null || !resv.canCheckIn) return false;
    resv.status = ResvStatus.used;
    _award('resv-$id', XpRules.reservationXp);
    if (resv.couponCode != null) {
      for (final c in coupons) {
        if (c.code == resv.couponCode) c.used = true;
      }
    }
    notifyListeners();
    return true;
  }

  /// ROL E-7: ləğv (mock: 2 saatdan əvvəl həmişə pulsuz).
  void cancelReservation(String id) {
    for (final r in reservations) {
      if (r.id == id && r.canCancel) {
        r.status = ResvStatus.cancelled;
        _notify(
          NotifType.resvUpdate,
          'Rezerv ləğv edildi',
          '${r.venueName} · ${r.date} ${r.time} rezervi ləğv olundu.',
        );
      }
    }
    notifyListeners();
  }

  /// ROL E-7: gəlməmə — 3 dəfə → 7 gün rezerv hüququ məhdudiyyəti.
  void markNoShow(String id) {
    for (final r in reservations) {
      if (r.id == id && r.canCancel) {
        r.status = ResvStatus.noShow;
        noShowCount++;
        if (noShowCount >= XpRules.noShowLimit) {
          restrictedUntil =
              DateTime.now().add(const Duration(days: XpRules.noShowRestrictionDays));
          _notify(
            NotifType.resvUpdate,
            'Rezerv hüququ müvəqqəti məhdudlaşdırıldı',
            '3 dəfə gəlməmə qeydə alındı — ${XpRules.noShowRestrictionDays} '
                'gün sonra rezerv yenidən açılacaq.',
          );
        }
      }
    }
    notifyListeners();
  }

  // ═══════════ Kuponlar (ROL G) ═══════════

  /// Bu istifadəçiyə görünən təkliflər: səviyyə-avto (açılıb) + XP ilə alınanlar.
  List<CouponOffer> get visibleOffers => offers
      .where((o) => o.autoByLevel ? o.unlockedFor(rankIndex) : true)
      .toList(growable: false);

  /// ROL G-b: XP ilə kupon al — balans xərclənir, ümumi XP düşmür.
  /// Qaytarır: alınan kupon; balans/və ya səviyyə çatmırsa null.
  UserCoupon? buyCoupon(String offerId) {
    CouponOffer? offer;
    for (final o in offers) {
      if (o.id == offerId) offer = o;
    }
    if (offer == null || offer.autoByLevel) return null;
    if (!offer.unlockedFor(rankIndex)) return null; // səviyyə yetmir
    if (xpBalance < offer.xpPrice) return null; // balans yetmir
    xpBalance -= offer.xpPrice; // ümumi XP (xp) DƏYİŞMİR (ROL G)
    final coupon = UserCoupon(
      id: 'uc$_couponSeq',
      offerId: offer.id,
      code: 'QSHUL-${_couponSeq.toString().padLeft(4, '0')}-${offer.venueId.toUpperCase()}',
      venueId: offer.venueId,
      venueName: offer.venueName,
      label: offer.label,
      expiresAt: DateTime.now().add(Duration(days: offer.validityDays)),
      purchasedAt: DateTime.now(),
    );
    _couponSeq++;
    coupons.insert(0, coupon);
    _notify(
      NotifType.couponExpiring,
      'Kupon alındı: ${offer.label}',
      '${offer.venueName} · ${offer.validityDays} gün etibarlıdır. '
          'Vaxtı bitərsə XP geri qaytarılmır.',
    );
    notifyListeners();
    return coupon;
  }

  /// Bu məkanda rezervə tətbiq edilə bilən aktiv kuponlar (ROL E-3).
  List<UserCoupon> activeCouponsFor(String venueId) =>
      coupons.where((c) => c.isActive && c.venueId == venueId).toList(growable: false);

  // ═══════════ Bildirişlər (ROL H) ═══════════

  int get unreadCount => notifications.where((n) => !n.read).length;

  void _notify(NotifType type, String title, String body) {
    if (!notifPrefs.isEnabled(type)) return; // hər tip ayrıca söndürülür
    final now = DateTime.now();
    if (type.isMarketing && isQuietHours(now)) return; // 23:00-08:00 sükut
    notifications.insert(
      0,
      AppNotification(id: 'n${notifications.length + 1}-${now.millisecondsSinceEpoch % 100000}',
          type: type, title: title, body: body, at: now),
    );
    if (notifications.length > 50) notifications.removeLast();
  }

  void markAllNotificationsRead() {
    for (final n in notifications) {
      n.read = true;
    }
    notifyListeners();
  }

  void toggleNotifPref(NotifType t, bool v) {
    notifPrefs.setEnabled(t, v);
    notifyListeners();
  }

  /// ROL H: tədbir xatırlatması (24s / 2s əvvəl) — mock tetikleyici.
  void eventReminder(QoshulEvent e, String when) {
    _notify(
      NotifType.eventReminder,
      'Tədbir $when',
      '${e.title} — ${e.date}, ${e.time} · ${e.location}',
    );
    notifyListeners();
  }

  // ═══════════ Moderasiya (ROL I) ═══════════

  /// ROL I: blok — qarşılıqlı görünməzlik, chat bağlanır.
  void blockUser(String personId) {
    blocked.add(personId);
    friendships.remove(personId);
    friendMessages.remove(personId);
    matches.remove(personId);
    notifyListeners();
  }

  void unblockUser(String personId) {
    blocked.remove(personId);
    notifyListeners();
  }

  /// ROL I: report — eyni istifadəçiyə 3+ → avtomatik məhdudlaşdırma (gizlədilir).
  void reportUser(String personId, String reason, {String note = ''}) {
    reports.add(ReportEntry(
      targetType: 'user',
      targetId: personId,
      reason: reason,
      note: note,
      at: DateTime.now(),
    ));
    final count = (reportCounts[personId] ?? 0) + 1;
    reportCounts[personId] = count;
    if (count >= 3 && !restrictedUsers.contains(personId)) {
      restrictedUsers.add(personId);
    }
    notifyListeners();
  }

  // ═══════════ Söhbətlər ═══════════

  void sendEventMessage(String eventId, String text) {
    final list = eventMessages.putIfAbsent(eventId, () => <ChatMessage>[]);
    list.add(ChatMessage(sender: 'Sən', body: text, mine: true));
    _mockReply(eventMessages[eventId]!, eventId);
    notifyListeners();
  }

  void sendFriendMessage(String personId, String text) {
    final list = friendMessages.putIfAbsent(personId, () => <ChatMessage>[]);
    list.add(ChatMessage(sender: 'Sən', body: text, mine: true));
    notifyListeners();
  }

  /// ROL TAB 4: "Söhbəti sil" — şəxsi söhbəti bağlayır (mock).
  void deleteChat(String personId) {
    friendMessages.remove(personId);
    friendships.remove(personId);
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
      .where((p) =>
          friendships[p.id] == 'accepted' &&
          !blocked.contains(p.id) &&
          !restrictedUsers.contains(p.id))
      .toList(growable: false);

  // ═══════════ Assistant (mock AI) ═══════════

  void sendAssistant(String text) {
    if (text.trim().isEmpty || assistantBusy) return;
    assistantMsgs.add(ChatMessage(sender: 'Sən', body: text.trim(), mine: true));
    assistantBusy = true;
    notifyListeners();

    final reply = _assistantReplyFor(text.trim());

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
