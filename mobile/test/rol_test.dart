import 'package:flutter_test/flutter_test.dart';

import 'package:qoshul/core/data/mock_data.dart';
import 'package:qoshul/core/models/models.dart';
import 'package:qoshul/core/models/notification.dart';
import 'package:qoshul/core/models/reservation.dart';
import 'package:qoshul/core/state/app_state.dart';

/// ROL məntiqi testləri — docs/product-spec.md (D-F-G-H-I bölmələri).
/// Bütün qaydalar klientdə simulyasiya olunub; backend fazasında eyni
/// qaydalar serverdə təkrarlanacaq (qayda 8-9).
void main() {
  group('XpRules — ROL F səviyyələri', () {
    test('səviyyə hədləri: Yeni→Əfsanə', () {
      expect(XpRules.rankForXp(0), 'Yeni');
      expect(XpRules.rankForXp(99), 'Yeni');
      expect(XpRules.rankForXp(100), 'Kəşfiyyatçı');
      expect(XpRules.rankForXp(299), 'Kəşfiyyatçı');
      expect(XpRules.rankForXp(300), 'Aktiv');
      expect(XpRules.rankForXp(700), 'Sosial');
      expect(XpRules.rankForXp(1500), 'Lider');
      expect(XpRules.rankForXp(3000), 'Əfsanə');
      expect(XpRules.rankForXp(99999), 'Əfsanə');
    });

    test('rankIndexForXp — kupon min səviyyəsi üçün', () {
      expect(XpRules.rankIndexForXp(50), 0);
      expect(XpRules.rankIndexForXp(320), 2); // Aktiv
      expect(XpRules.rankIndexForXp(5000), 5);
    });

    test('xpProgress — ROL hədləri ilə', () {
      final p = XpRules.xpProgress(150);
      expect(p['nextName'], 'Aktiv');
      expect(p['toNext'], 300 - 150);
      expect(p['pct'], 26); // (150-99)/(299-99)*100 = 25.5 → 26
      expect(XpRules.xpProgress(3000)['nextName'], isNull);
    });

    test('isAdult — 18+ yoxlaması (ROL B)', () {
      final now = DateTime(2026, 10, 28);
      expect(XpRules.isAdult(DateTime(2008, 1, 1), now), isTrue); // 18
      expect(XpRules.isAdult(DateTime(2008, 12, 1), now), isFalse); // ad günü hələ keçməyib → 17
      expect(XpRules.isAdult(DateTime(2010, 5, 15), now), isFalse); // 16
    });

    test('ROL cədvəl dəyərləri', () {
      expect(XpRules.registrationXp, 50);
      expect(XpRules.joinXp, 10);
      expect(XpRules.checkInXp, 40);
      expect(XpRules.hostingXp, 60);
      expect(XpRules.reservationXp, 30);
      expect(XpRules.reviewXp, 15);
      expect(XpRules.matchXp, 10);
      expect(XpRules.matchXpDailyCap, 3);
      expect(XpRules.minInterests, 3);
      expect(XpRules.maxInterests, 8);
      expect(XpRules.bioLimit, 150);
    });
  });

  group('AppState — XP mühərriki (idempotent)', () {
    test('qeydiyyat bonusu +50 yalnız bir dəfə', () {
      final s = AppState();
      final before = s.xp;
      s.completeRegistration();
      expect(s.xp, before + 50);
      s.completeRegistration();
      expect(s.xp, before + 50); // idempotent
    });

    test('telefon təsdiqi +40 + nişan (ROL B/F)', () {
      final s = AppState();
      final before = s.xp;
      s.verifyPhone();
      expect(s.phoneVerified, isTrue);
      expect(s.earnedBadges.contains('verified'), isTrue);
      expect(s.xp, before + 40);
      s.verifyPhone();
      expect(s.xp, before + 40);
    });

    test('profil 100% +30 yalnız tam profildə', () {
      final s = AppState();
      s.setProfileCreation(
          first: 'Aysel', last: 'Əliyeva', birth: DateTime(2000, 5, 5), bioText: 'Salam', photos: 2);
      s.setInterests(['İdman və Aktivlik', 'Kafe və Yemək', 'Musiqi']);
      final before = s.xp;
      s.completeProfileFull();
      expect(s.xp, before + 30);
      expect(s.profileCompleted, isTrue);
    });

    test('tədbirə qoşulma +10 (ROL F)', () {
      final s = AppState();
      final before = s.xp;
      expect(s.join('e1'), isTrue); // e1: açıq qoşulma
      expect(s.xp, before + 10);
      expect(s.join('e1'), isFalse); // ikinci dəfə yox
      expect(s.xp, before + 10);
    });

    test('təsdiqli tədbir → pending, XP yoxdur', () {
      final s = AppState();
      final before = s.xp;
      expect(s.join('e3'), isTrue); // e3: approval
      expect(s.pending.contains('e3'), isTrue);
      expect(s.joined.contains('e3'), isFalse);
      expect(s.xp, before);
    });

    test('check-in +40 idempotent + İlk tədbir nişanı', () {
      final s = AppState();
      s.join('e1');
      final before = s.xp;
      expect(s.checkInEvent('e1'), isTrue);
      expect(s.xp, before + 40);
      expect(s.earnedBadges.contains('firstEvent'), isTrue);
      expect(s.checkInEvent('e1'), isFalse); // təkrar yox
      expect(s.xp, before + 40);
    });

    test('rəy +15, həftədə max 3', () {
      final s = AppState();
      var xp = s.xp;
      for (var i = 0; i < 3; i++) {
        expect(s.addReview('x$i', 5, ''), isTrue);
        xp += 15;
        expect(s.xp, xp);
      }
      expect(s.addReview('x3', 5, ''), isFalse); // limit
      expect(s.xp, xp);
    });

    test('günlük giriş +5 (bir dəfə)', () {
      final s = AppState();
      final before = s.xp;
      s.dailyLogin();
      expect(s.xp, before + 5);
      s.dailyLogin();
      expect(s.xp, before + 5);
    });
  });

  group('AppState — match (ROL D)', () {
    test('qarşılıqlı maraq → match + chat + +10 XP', () {
      final s = AppState();
      final before = s.xp;
      expect(s.swipe('p1', true), isTrue); // p1 likesBack
      expect(s.matches.contains('p1'), isTrue);
      expect(s.friendships['p1'], 'accepted');
      expect(s.friendMessages.containsKey('p1'), isTrue);
      expect(s.xp, before + 10);
      expect(s.notifications.any((n) => n.type == NotifType.match), isTrue);
    });

    test('keçmə → match yoxdur, XP yoxdur', () {
      final s = AppState();
      final before = s.xp;
      expect(s.swipe('p4', false), isFalse);
      expect(s.matches.contains('p4'), isFalse);
      expect(s.xp, before);
      expect(s.matchCandidates.any((p) => p.id == 'p4'), isFalse);
    });

    test('qarşılıqsız maraq → match yoxdur', () {
      final s = AppState();
      expect(s.swipe('p3', true), isFalse); // p3 likesBack = false
      expect(s.matches.contains('p3'), isFalse);
    });

    test('gündəlik match XP limiti 3', () {
      final s = AppState();
      final before = s.xp;
      s.swipe('p1', true); // match +10
      s.swipe('p5', true); // match +10
      s.swipe('p3', true); // likesBack yox → +0
      s.resetSwipes();
      s.swipe('p1', true); // yenidən — idempotent açar, +0
      expect(s.xp, before + 20); // yalnız 2 real match
    });
  });

  group('AppState — rezerv (ROL E)', () {
    test('avto-təsdiq məkanı → TƏSDİQLƏNDİ', () {
      final s = AppState();
      final r = s.createReservation(
          venue: mockPartners[1], // v2 Coffee Lab autoAccept
          date: 'Bu gün',
          time: '11:00',
          party: 3);
      expect(r, isNotNull);
      expect(r!.status, ResvStatus.confirmed);
      expect(s.notifications.any((n) => n.type == NotifType.resvUpdate), isTrue);
    });

    test('manuel təsdiq məkanı → GÖZLƏYİR', () {
      final s = AppState();
      final r = s.createReservation(
          venue: mockPartners[0], // v1 Padel Arena manual
          date: 'Bu gün',
          time: '10:00',
          party: 2);
      expect(r!.status, ResvStatus.pending);
    });

    test('check-in → İSTİFADƏ OLUNDU +30 XP', () {
      final s = AppState();
      final r = s.createReservation(
          venue: mockPartners[1], date: 'Bu gün', time: '11:00', party: 2);
      final before = s.xp;
      expect(s.checkInReservation(r!.id), isTrue);
      expect(r.status, ResvStatus.used);
      expect(s.xp, before + 30);
      expect(s.checkInReservation(r.id), isFalse); // bir dəfəlik QR
    });

    test('ləğv → LƏĞV EDİLDİ', () {
      final s = AppState();
      final r = s.createReservation(
          venue: mockPartners[1], date: 'Bu gün', time: '11:00', party: 2);
      s.cancelReservation(r!.id);
      expect(r.status, ResvStatus.cancelled);
    });

    test('gəlməmə 3× → 7 günlük məhdudiyyət', () {
      final s = AppState();
      for (var i = 0; i < 3; i++) {
        final r = s.createReservation(
            venue: mockPartners[1], date: 'Bu gün', time: '11:00', party: 2);
        s.markNoShow(r!.id);
      }
      expect(s.reservationRestricted, isTrue);
      expect(
        s.createReservation(
            venue: mockPartners[1], date: 'Bu gün', time: '11:00', party: 2),
        isNull,
      );
    });

    test('başqa məkan kuponu rezervə tətbiq olunmur', () {
      final s = AppState();
      s.xp = 500;
      s.xpBalance = 500;
      final c = s.buyCoupon('o3'); // Dostlar Bağı (v4)
      expect(c, isNotNull);
      final r = s.createReservation(
          venue: mockPartners[1], // Coffee Lab — kupon v4 üçündür
          date: 'Bu gün',
          time: '11:00',
          party: 2,
          couponCode: c!.code);
      expect(r, isNull); // kupon məkan uyğunsuzluğu → rədd
    });
  });

  group('AppState — kuponlar (ROL G)', () {
    test('XP ilə al: balans düşür, ümumi XP dəyişmir', () {
      final s = AppState();
      final totalBefore = s.xp;
      final balBefore = s.xpBalance;
      final c = s.buyCoupon('o1'); // 200 XP
      expect(c, isNotNull);
      expect(s.xpBalance, balBefore - 200);
      expect(s.xp, totalBefore); // ümumi XP düşmür
      expect(c!.code.startsWith('QSHUL-'), isTrue);
      expect(c.isActive, isTrue);
    });

    test('balans çatmırsa → null', () {
      final s = AppState();
      s.xpBalance = 100;
      expect(s.buyCoupon('o1'), isNull); // 200 XP lazım
    });

    test('səviyyə çatmırsa → null', () {
      final s = AppState();
      s.xp = 150; // Kəşfiyyatçı
      s.xpBalance = 500;
      expect(s.buyCoupon('o2'), isNull); // Aktiv+ tələb edir
    });

    test('səviyyə-avto təkliflər görünürlüyü', () {
      final s = AppState();
      s.xp = 320; // Aktiv
      expect(s.visibleOffers.any((o) => o.id == 'o4'), isTrue); // Aktiv+
      s.xp = 100; // Kəşfiyyatçı
      expect(s.visibleOffers.any((o) => o.id == 'o4'), isFalse);
    });

    test('kupon rezerv check-in-də İSTİFADƏ OLUNDU olur', () {
      final s = AppState();
      s.xp = 500;
      s.xpBalance = 500;
      final c = s.buyCoupon('o3'); // v4
      final r = s.createReservation(
          venue: mockPartners[3], date: 'Bu gün', time: '12:00', party: 4,
          couponCode: c!.code);
      expect(r, isNotNull);
      s.checkInReservation(r!.id);
      expect(c.used, isTrue);
      expect(c.statusLabel, 'İSTİFADƏ OLUNDU');
    });
  });

  group('Bildirişlər (ROL H)', () {
    test('gecə sükut saatları 23:00-08:00', () {
      expect(isQuietHours(DateTime(2026, 1, 1, 23, 0)), isTrue);
      expect(isQuietHours(DateTime(2026, 1, 1, 23, 30)), isTrue);
      expect(isQuietHours(DateTime(2026, 1, 1, 7, 59)), isTrue);
      expect(isQuietHours(DateTime(2026, 1, 1, 8, 0)), isFalse);
      expect(isQuietHours(DateTime(2026, 1, 1, 12, 0)), isFalse);
      expect(isQuietHours(DateTime(2026, 1, 1, 22, 59)), isFalse);
    });

    test('tipi söndürülmüş bildiriş gəlmir', () {
      final s = AppState();
      s.toggleNotifPref(NotifType.levelUp, false);
      final before = s.notifications.length;
      s.xp = 100; // Aktiv səviyyəsinə keçid tetikləmək üçün
      s.completeRegistration(); // +50 → 150 (hələ Aktiv yox)
      s.xp = 690; // səviyyə atlayacaq
      s.verifyPhone(); // +40 → 730 > 699 → levelUp — amma tip bağlıdır
      expect(s.notifications.length, before);
      expect(s.unreadCount, s.notifications.where((n) => !n.read).length);
    });

    test('səviyyə artımı bildirişi gəlir (tip açıqdursa)', () {
      final s = AppState();
      final before = s.notifications.length;
      s.xp = 690;
      s.verifyPhone(); // +40 → 730: Sosial səviyyə
      expect(s.notifications.length, before + 1);
      expect(
          s.notifications.first.type == NotifType.levelUp ||
              s.notifications.any((n) => n.type == NotifType.levelUp),
          isTrue);
    });

    test('hamısını oxunmuş et', () {
      final s = AppState();
      s.markAllNotificationsRead();
      expect(s.unreadCount, 0);
    });
  });

  group('Moderasiya (ROL I)', () {
    test('blok: qarşılıqlı görünməzlik', () {
      final s = AppState();
      s.blockUser('p1');
      expect(s.blocked.contains('p1'), isTrue);
      expect(s.matchCandidates.any((p) => p.id == 'p1'), isFalse);
      expect(s.acceptedFriends.any((p) => p.id == 'p1'), isFalse);
      s.unblockUser('p1');
      expect(s.blocked.contains('p1'), isFalse);
    });

    test('3+ report → avtomatik məhdudlaşdırma', () {
      final s = AppState();
      s.reportUser('p3', 'Saxta profil');
      s.reportUser('p3', 'Spam və reklam');
      expect(s.restrictedUsers.contains('p3'), isFalse);
      s.reportUser('p3', 'Uyğunsuz məzmun');
      expect(s.restrictedUsers.contains('p3'), isTrue);
      expect(s.matchCandidates.any((p) => p.id == 'p3'), isFalse);
    });

    test('blok match-i ləğv edir', () {
      final s = AppState();
      s.swipe('p1', true);
      expect(s.matches.contains('p1'), isTrue);
      s.blockUser('p1');
      expect(s.matches.contains('p1'), isFalse);
      expect(s.friendMessages.containsKey('p1'), isFalse);
    });
  });

  group('Onboarding qaydaları (ROL B)', () {
    test('18+ yoxlaması profildə tətbiq olunur', () {
      final s = AppState();
      s.setProfileCreation(
          first: 'Nəbi',
          last: 'Əliyev',
          birth: DateTime(2010, 3, 3),
          bioText: 'test');
      expect(XpRules.isAdult(s.birthDate!), isFalse);
    });

    test('maraq limitləri: min 3, max 8', () {
      expect(XpRules.minInterests, 3);
      expect(XpRules.maxInterests, 8);
      expect(rolCategories.length, 9); // ROL B Addım 4: 9 kateqoriya
    });

    test('finishOnboarding +50 XP verir', () {
      final s = AppState();
      final before = s.xp;
      s.finishOnboarding();
      expect(s.onboarded, isTrue);
      expect(s.xp, before + 50 + 5); // qeydiyyat + ilk günlük giriş
    });
  });
}
