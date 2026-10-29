import 'package:flutter_test/flutter_test.dart';

import 'package:qoshul/core/models/models.dart';
import 'package:qoshul/core/models/xp_economy.dart';
import 'package:qoshul/core/state/app_state.dart';

/// D-026 — ikitərəfli XP iqtisadiyyatı testləri (product-spec L bölməsi).
///
/// 1-ci XP (Peşəkarlıq) xərclənmir; 2-ci XP (Endirim) obyektə-məxsusdur:
/// qazanma statik QR + taq axını ilə, xərcləmə dinamik QR (60s TTL) ilə.
/// Ayrı baza qaydası: Xankəndi XP-si yalnız Xankəndidə işləyir.
void main() {
  group('XpCardCode — dinamik QR (L.4)', () {
    test('təzə kod 60 saniyə etibarlıdır, fallback 6 rəqəmdir', () {
      final c = XpCardCode.fresh();
      expect(c.isValid, isTrue);
      expect(c.secondsLeft, lessThanOrEqualTo(60));
      expect(c.secondsLeft, greaterThan(55));
      expect(c.fallbackCode.length, XpRules.fallbackCodeDigits);
      expect(RegExp(r'^\d{6}$').hasMatch(c.fallbackCode), isTrue);
    });

    test('vaxtı bitən kod etibarsızdır (TTL)', () {
      final oldCode = XpCardCode.fresh(
          DateTime.now().subtract(const Duration(seconds: 61)));
      expect(oldCode.isValid, isFalse);
      expect(oldCode.secondsLeft, 0);
    });

    test('işlənən kod təkrar istifadə oluna bilməz (tək-istifadəlik)', () {
      final c = XpCardCode.fresh();
      expect(c.isValid, isTrue);
      c.used = true;
      expect(c.isValid, isFalse); // double-spend qorunması
    });

    test('hər kod yeni nonce daşıyır', () {
      final a = XpCardCode.fresh();
      final b = XpCardCode.fresh();
      expect(a.nonce, isNot(b.nonce));
    });
  });

  group('MapVisibility — 3 seçim (D-027)', () {
    test('label və hint doludur', () {
      for (final v in MapVisibility.values) {
        expect(v.label, isNotEmpty);
        expect(v.hint, isNotEmpty);
      }
    });

    test('state üç rejimi qəbul edir', () {
      final s = AppState();
      expect(s.mapVisibility, MapVisibility.everyone);
      s.setMapVisibility(MapVisibility.friends);
      expect(s.mapVisibility, MapVisibility.friends);
      s.setMapVisibility(MapVisibility.off);
      expect(s.mapVisibility, MapVisibility.off);
    });
  });

  group('VenueXpConfig — obyekt tənzimləmələri (xp_config)', () {
    test('məzənnə təsviri düzgündür', () {
      const cfg = VenueXpConfig(xpPerAzn: 20, maxDiscountPercent: 25, minBillAzn: 10);
      expect(cfg.describe(), contains('100 XP = 5'));
      expect(cfg.describe(), contains('maks 25%'));
      expect(cfg.describe(), contains('min hesab 10'));
      const baza = VenueXpConfig(xpPerAzn: 25, maxDiscountPercent: 20);
      expect(baza.describe(), isNot(contains('min hesab')));
    });
  });

  group('AppState — ilkin obyekt bazaları (L.2 ayrı baza)', () {
    test('Xankəndi balansı var, Kokaltı sıfırdır (demo ayrı baza)', () {
      final s = AppState();
      expect(s.venueXp['v5']!.balance, 325); // Xankəndi Restoranı
      expect(s.venueXp['v6']!.balance, 0); // Kokaltı Kafesi
      expect(s.venueXp['v5']!.venueName, 'Xankəndi Restoranı');
      expect(s.discountXpTotal, greaterThan(0));
      expect(s.discountXpTotal,
          s.venueXp.values.fold(0, (t, a) => t + a.balance));
    });

    test('ledger qeydləri tarixçəsi oxunur', () {
      final s = AppState();
      expect(s.venueXp['v5']!.recentLedger, isNotEmpty);
      expect(s.venueXp['v5']!.recentLedger.first.grant, isTrue);
    });
  });

  group('previewSpend — endirim hesablanması (L.4-4)', () {
    test('uğurlu hal: 100 XP, 48 ₼ hesab → 5 ₼ endirim', () {
      final s = AppState();
      final p = s.previewSpend('v5', 100, 48);
      expect(p['ok'], isTrue);
      expect((p['discount'] as double), closeTo(5.0, 0.01));
    });

    test('balans yetmirsə → balans xətası', () {
      final s = AppState();
      final p = s.previewSpend('v5', 99999, 48);
      expect(p['ok'], isFalse);
      expect(p['error'], 'balans');
    });

    test('maksimum endirim faizi aşılırsa → cap xətası', () {
      final s = AppState();
      // 300 XP / 20 = 15 ₼; 40 ₼ hesabın 25%-i = 10 ₼ → cap aşılır
      final p = s.previewSpend('v5', 300, 40);
      expect(p['ok'], isFalse);
      expect(p['error'], 'cap');
      expect((p['discount'] as double), closeTo(10.0, 0.01));
    });

    test('minimum hesab məbləği altında → minBill xətası', () {
      final s = AppState();
      final p = s.previewSpend('v5', 100, 5); // v5 min 10 ₼
      expect(p['ok'], isFalse);
      expect(p['error'], 'minBill');
    });

    test('Kokaltı bazası sıfırdır → balans xətası', () {
      final s = AppState();
      final p = s.previewSpend('v6', 50, 30);
      expect(p['ok'], isFalse);
      expect(p['error'], 'balans');
    });
  });

  group('spendVenueXp — atomik xərcləmə (L.4-5)', () {
    test('balans düşür, ledger qeydi yaranır, bildiriş gəlir', () {
      final s = AppState();
      final before = s.venueXp['v5']!.balance;
      final ledBefore = s.venueXp['v5']!.ledger.length;
      final notifBefore = s.notifications.length;

      final d = s.spendVenueXp('v5', 100, 48);
      expect(d, closeTo(5.0, 0.01));
      expect(s.venueXp['v5']!.balance, before - 100);
      expect(s.venueXp['v5']!.ledger.length, ledBefore + 1);
      final e = s.venueXp['v5']!.ledger.last;
      expect(e.grant, isFalse);
      expect(e.amount, 100);
      expect(e.balanceAfter, before - 100);
      expect(s.notifications.length, greaterThan(notifBefore)); // receipt
    });

    test('xətalı halda balans dəyişmir', () {
      final s = AppState();
      final before = s.venueXp['v5']!.balance;
      expect(s.spendVenueXp('v5', 500, 40), isNull); // cap
      expect(s.spendVenueXp('v5', 100, 5), isNull); // minBill
      expect(s.spendVenueXp('v6', 50, 30), isNull); // balans 0
      expect(s.venueXp['v5']!.balance, before);
      expect(s.venueXp['v6']!.balance, 0);
    });
  });

  group('dinamik XP kartı state (L.4-1)', () {
    test('regenerate → venue-ə etibarlıdır; işlənəndə etibarsız olur', () {
      final s = AppState();
      expect(s.xpCardValidFor('v5'), isFalse); // hələ kod yoxdur
      s.regenerateXpCard('v5');
      expect(s.xpCardValidFor('v5'), isTrue);
      expect(s.xpCardValidFor('v2'), isFalse); // başqa obyektə yox
      s.markXpCardUsed();
      expect(s.xpCardValidFor('v5'), isFalse); // tək istifadəlik
    });
  });

  group('finishActivity — təşkilatçı axını (L.3)', () {
    test('canFinish: bitmiş + öz tədbirim + obyektli', () {
      final s = AppState();
      s.setProfileName('Aysel İsmayılova');
      final e = s.events.firstWhere((e) => e.id == 'e-mafiya');
      // host demo adına keçirilib
      expect(e.host, 'Aysel İsmayılova');
      expect(s.canFinishActivity(e), isTrue);
      expect(s.isActivityFinished(e.id), isFalse);
    });

    test('bitir → təşkilatçı +50, taq sayına görə nəticə, ikinci dəfə YOX', () {
      final s = AppState();
      s.setProfileName('Aysel İsmayılova');
      final e = s.events.firstWhere((e) => e.id == 'e-mafiya');
      final before = s.venueXp['v5']!.balance;

      final r = s.finishActivity(e, ['p1', 'p2', 'p3']);
      expect(r, isNotNull);
      expect(r!.organizerXp, XpRules.organizerXp); // 50
      expect(r.perParticipantXp, XpRules.taggedParticipantXp); // 25
      expect(r.taggedCount, 3);
      expect(r.venueName, 'Xankəndi Restoranı');
      expect(s.venueXp['v5']!.balance, before + 50);
      expect(s.isActivityFinished(e.id), isTrue);

      // İkinci dəfə çağırıla bilməz (idempotent)
      expect(s.finishActivity(e, ['p1']), isNull);
      expect(s.venueXp['v5']!.balance, before + 50);

      // Ledger-ə təşkilatçı grantı yazıldı
      final last = s.venueXp['v5']!.ledger.last;
      expect(last.grant, isTrue);
      expect(last.amount, 50);
      expect(last.note, contains('Təşkilatçı'));
    });

    test('taq siyahısı boşdursa → null', () {
      final s = AppState();
      s.setProfileName('Aysel İsmayılova');
      final e = s.events.firstWhere((e) => e.id == 'e-mafiya');
      expect(s.finishActivity(e, []), isNull);
    });

    test('başqa host-un tədbiri bitmir', () {
      final s = AppState();
      s.setProfileName('Aysel İsmayılova');
      final e0 = s.events.firstWhere((e) => e.id == 'e0'); // host: Rəşad
      expect(s.canFinishActivity(e0), isFalse);
      expect(s.finishActivity(e0, ['p1']), isNull);
    });
  });

  group('Kəşf peşəkar sıralaması (L.1)', () {
    test('proWeight: Peşəkar > Orta > Başlanğıc > Hamı üçün', () {
      expect(XpRules.proWeight('Peşəkar'), 3);
      expect(XpRules.proWeight('Orta'), 2);
      expect(XpRules.proWeight('Başlanğıc'), 1);
      expect(XpRules.proWeight('Hamı üçün'), 0);
      expect(XpRules.proWeight(null), 0);
    });
  });

  group('XpRules — D-026 sabitləri', () {
    test('təşkilatçı 50, taq 25, TTL 60, fallback 6 rəqəm', () {
      expect(XpRules.organizerXp, 50);
      expect(XpRules.taggedParticipantXp, 25);
      expect(XpRules.xpCardTtlSeconds, 60);
      expect(XpRules.fallbackCodeDigits, 6);
    });
  });
}
