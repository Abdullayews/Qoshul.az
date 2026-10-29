import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:qoshul/core/data/mock_data.dart';
import 'package:qoshul/core/models/notification.dart';
import 'package:qoshul/core/models/xp_economy.dart';
import 'package:qoshul/core/state/app_state.dart';
import 'package:qoshul/core/widgets/glass.dart';
import 'package:qoshul/core/widgets/rol.dart';
import 'package:qoshul/features/events/presentation/event_detail_sheet.dart';
import 'package:qoshul/features/map/presentation/tile_map.dart';
import 'package:qoshul/features/venues/presentation/reserve_sheet.dart';
import 'package:qoshul/main.dart';

/// Widget testləri — ROL UI-2 (v0.3.0): auth axını, onboarding, 5 tab,
/// match, rezerv, kupon, ayarlar. Səth: 800×1200; lazy ListView bölmələri
/// scrollUntilVisible ilə yoxlanılır.
void main() {
  Future<void> boot(WidgetTester tester, AppState state) async {
    await tester.binding.setSurfaceSize(const Size(800, 1200));
    await tester.pumpWidget(buildAppWithState(state));
    await tester.pumpAndSettle();
  }

  /// Lazy ListView içində hədəfi tapana qədər sürüşdür (drag əsaslı).
  Future<void> dragUntilFound(
      WidgetTester tester, Finder target, Finder scrollable,
      {int max = 14, bool up = false}) async {
    for (var i = 0; i < max; i++) {
      if (tester.any(target)) return;
      await tester.drag(scrollable, Offset(0, up ? 400 : -350));
      await tester.pumpAndSettle();
    }
  }

  /// Hədəfi həm GÖRÜNƏN, həm şüşə nav pill-in ALTINA düşməyən mövqedə
  /// üzə çıxar və toxun (D-026 feed uzandığı üçün mövqe nəzarəti lazımdır).
  Future<void> revealAndTap(
      WidgetTester tester, Finder target, Finder scrollable) async {
    for (var i = 0; i < 26; i++) {
      if (tester.any(target)) {
        final dy = tester.getTopLeft(target.first).dy;
        if (dy >= 0 && dy < 1060) {
          await tester.tap(target.first);
          await tester.pumpAndSettle();
          return;
        }
        await tester.drag(
            scrollable, Offset(0, dy < 0 ? 150 : -150));
      } else {
        await tester.drag(scrollable, const Offset(0, -150));
      }
      await tester.pumpAndSettle();
    }
    fail('revealAndTap: hədəf tapılmadı');
  }

  /// Uzağa scroll olmuş siyahını yuxarı qaytar (TextField-lər geri gəlsin).
  ///
  /// v0.6.0: pinned axtarış (D-029) sayəsində artıq feed testlərinə lazım
  /// deyil — gələcək testlər üçün saxlanılır.
  // ignore: unused_element
  Future<void> scrollTop(WidgetTester tester, Finder scrollable) async {
    for (var i = 0; i < 20; i++) {
      await tester.drag(scrollable, const Offset(0, 500));
      await tester.pumpAndSettle(const Duration(milliseconds: 30));
    }
  }

  testWidgets('auth: email qeydiyyatı → 6 rəqəmli kod addımı (ROL B)',
      (tester) async {
    final state = AppState();
    await boot(tester, state);
    expect(find.text('Xoş gəldin!'), findsOneWidget);

    await tester.tap(find.text('Hesabın yoxdur? Qeydiyyatdan keç'));
    await tester.pumpAndSettle();
    expect(find.text('Hesab yarat'), findsOneWidget);

    // TextField-lər sırayla: ad, e-poçt, şifrə, təkrar
    final fields = find.byType(TextField);
    expect(fields, findsNWidgets(4));
    await tester.enterText(fields.at(0), 'Aysel Əliyeva');
    await tester.enterText(fields.at(1), 'aysel@test.az');
    await tester.enterText(fields.at(2), 'parol12345');
    await tester.enterText(fields.at(3), 'parol12345');
    await tester.pump();

    await tester.tap(find.text('Qeydiyyatdan keç'));
    await tester.pumpAndSettle();

    // Kod mərhələsi (ROL B Addım 2)
    expect(find.text('E-poçtunu təsdiqlə'), findsOneWidget);

    // Yanlış kod (5 rəqəm) → xəta
    await tester.enterText(find.byType(TextField).first, '12345');
    await tester.tap(find.text('Təsdiqlə'));
    await tester.pumpAndSettle();
    expect(find.textContaining('6 rəqəmdən'), findsOneWidget);

    // Düz kod → giriş
    await tester.enterText(find.byType(TextField).first, '123456');
    await tester.tap(find.text('Təsdiqlə'));
    await tester.pumpAndSettle();
    expect(state.signedIn, isTrue);
  });

  testWidgets('onboarding: profil → maraqlar (min 3) → icazə → +50 XP (ROL B)',
      (tester) async {
    final state = AppState()..signIn();
    await boot(tester, state);

    // Slaydları keç
    await tester.tap(find.text('Keç'));
    await tester.pumpAndSettle();
    expect(find.text('Profilini yarad'), findsOneWidget);

    // Ad (ilk TextField; ikincisi bio-dur)
    await tester.enterText(find.byType(TextField).first, 'Aysel Əliyeva');
    await tester.pump();

    // Doğum tarixi (18+ keçərli — initialDate = 22 yaş)
    await tester.tap(find.text('Tarix seç'));
    await tester.pumpAndSettle();
    final okBtn = find.byType(TextButton).last; // dialoqda OK
    await tester.tap(okBtn);
    await tester.pumpAndSettle();

    await tester.tap(find.text('Davam et'));
    await tester.pumpAndSettle();
    expect(find.text('Maraqlarını seç'), findsOneWidget);

    // 2 maraq → davam bağlı; 3 maraq → açılır (ROL B Addım 4)
    await tester.tap(find.text('İdman və Aktivlik'));
    await tester.pump();
    await tester.tap(find.text('Kafe və Yemək'));
    await tester.pump();
    expect(find.text('Ən azı 3 maraq seç'), findsOneWidget);

    await tester.tap(find.text('Musiqi'));
    await tester.pump();
    expect(find.text('Davam et (3 maraq)'), findsOneWidget);
    await tester.tap(find.text('Davam et (3 maraq)'));
    await tester.pumpAndSettle();

    // İcazələr (ROL B Addım 5)
    expect(find.text('İcazələr'), findsOneWidget);
    final before = state.xp;
    await tester.tap(find.text('Qoshul-a başla'));
    await tester.pumpAndSettle();

    expect(state.onboarded, isTrue);
    expect(state.interests.length, 3);
    expect(state.xp, before + 50 + 5); // qeydiyyat + günlük giriş

    // 5 tab shell (ROL C)
    expect(find.text('Kəşf'), findsWidgets);
    expect(find.text('Xəritə'), findsOneWidget);
    expect(find.text('Tədbirlər'), findsOneWidget);
    expect(find.text('Söhbətlər'), findsOneWidget);
    expect(find.text('Profil'), findsOneWidget);
  });

  testWidgets('shell: 5 tab naviqasiyası (ROL C)', (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    // TAB 2: Xəritə — filtrlər
    await tester.tap(find.text('Xəritə'));
    await tester.pumpAndSettle();
    expect(find.text('indi açıqdır'), findsOneWidget);
    expect(find.text('endirimi var'), findsOneWidget);
    expect(find.text('10 km'), findsOneWidget);

    // TAB 3: Tədbirlər — 3 siyahı + kateqoriya çipləri + peşəcar sıralama
    await tester.tap(find.text('Tədbirlər'));
    await tester.pumpAndSettle();
    expect(find.text('Yaxınlaşan'), findsOneWidget);
    expect(find.text('Mənim tədbirlərim'), findsOneWidget);
    expect(find.text('Yaratdığım'), findsOneWidget);
    // v0.6.0 (D-026 L.1 + D-028): peşəkar sıralama və kateqoriya çipləri
    // Kəşf feed-indən Tədbirlər tabına köçürüldü.
    expect(find.text('Sənin səviyyənə görə'), findsOneWidget);
    expect(find.text('Hamısı'), findsOneWidget);

    // TAB 4: Söhbətlər — Şəxsi/Qruplar
    await tester.tap(find.text('Söhbətlər'));
    await tester.pumpAndSettle();
    expect(find.text('Qruplar'), findsWidgets);
    expect(find.text('Şəxsi'), findsWidgets);

    // TAB 5: Profil — nişanlar + keçidlər
    await tester.tap(find.text('Profil'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Nişanlar'), 200,
        scrollable: find.byType(Scrollable).first);
    expect(find.text('Rezervlərim'), findsOneWidget);
    expect(find.text('Kuponlarım'), findsOneWidget);
  });

  testWidgets('xəritə: real OSM tile renderer + zoom idarəsi (D-030)',
      (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    await tester.tap(find.text('Xəritə'));
    await tester.pumpAndSettle();

    // Öz renderer (sıfır xarici asılılıq) qurulub
    expect(find.byType(TileMapView), findsOneWidget);
    // OSM atribusiyası görünür (istifadə qaydası — məcburi kredit)
    expect(find.text('© OpenStreetMap contributors'), findsOneWidget);
    // Zoom həbləri (şüşə) — Yarat düyməsindən fərqli
    expect(find.widgetWithIcon(GlassPill, Icons.add), findsOneWidget);
    expect(find.widgetWithIcon(GlassPill, Icons.remove), findsOneWidget);

    // Zoom dəyişməsi state-i sındırmır
    await tester.tap(find.widgetWithIcon(GlassPill, Icons.add));
    await tester.pumpAndSettle();
    expect(find.byType(TileMapView), findsOneWidget);
    await tester.tap(find.widgetWithIcon(GlassPill, Icons.remove));
    await tester.pumpAndSettle();
    // Pin toxunuşu → şüşə alt kart (venue pin, görünürsə)
    final pin = find.byIcon(Icons.storefront);
    if (tester.any(pin)) {
      await tester.tap(pin.first, warnIfMissed: false);
      await tester.pumpAndSettle();
    }
  });

  testWidgets('feed: Kəşf = "Sən üçün" axını + pinned axtarış (ROL TAB 1, v0.6.0)',
      (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    expect(find.text('Salam, Dost!'), findsOneWidget);

    // D-029: axtarış scroll ETMƏDƏN görünür — səhifənin ən üstündə sabit.
    expect(find.byType(TextField), findsOneWidget);
    // D-028: tədbir siyahıları Kəşfdən çıxarılıb (Tədbirlər ana tabında).
    expect(find.text('Yaxınında olan tədbirlər'), findsNothing);
    expect(find.text('Bütün tədbirlər'), findsNothing);
    expect(find.text('Sənin səviyyənə görə'), findsNothing);

    final feed = find.byType(ListView).first;
    await dragUntilFound(tester, find.text('Populyar məkanlar'), feed);
    expect(find.text('Populyar məkanlar'), findsOneWidget);

    await dragUntilFound(tester, find.text('Sənə uyğun insanlar'), feed);
    expect(find.text('Sənə uyğun insanlar'), findsOneWidget);

    await dragUntilFound(tester, find.text('Endirimli təkliflər'), feed);
    expect(find.text('Endirimli təkliflər'), findsOneWidget);

    // Axtarış: insan adı ilə (pinned olduğu üçün scroll-back lazım DEYİL)
    await tester.enterText(find.byType(TextField).first, 'Leyla');
    await tester.pumpAndSettle();
    expect(find.text('Sənə uyğun insanlar'), findsOneWidget);

    // Axtarışı təmizlə → "Bu axşam" keçid kartı → Tədbirlər tabı (D-028)
    await tester.enterText(find.byType(TextField).first, '');
    await tester.pumpAndSettle();
    final banner = find.textContaining('Bu axşam');
    if (tester.any(banner)) {
      await tester.tap(banner.first, warnIfMissed: false);
      await tester.pumpAndSettle();
      expect(find.text('Yaxınlaşan'), findsOneWidget);
    }
  });

  testWidgets('match: Maraqlıyam → match dialoqu (ROL D)', (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    final feed = find.byType(ListView).first;
    await revealAndTap(tester, find.text('Hamısına bax'), feed);
    expect(find.text('Tanış ol'), findsWidgets);
    // 3 namizəd kart var (ROL D) — hamısında eyni düymə
    expect(find.text('Maraqlıyam'), findsWidgets);

    final before = state.xp;
    await tester.tap(find.text('Maraqlıyam').first);
    await tester.pumpAndSettle();
    // p1 (Leyla, likesBack) → match dialoqu + XP (ROL D)
    expect(find.text('Match!'), findsOneWidget);
    expect(state.xp, before + 10);
    await tester.tap(find.text('Davam et'));
    await tester.pumpAndSettle();
  });

  testWidgets('rezerv axını: slot → təsdiq → GÖZLƏYİR + QR (ROL E)',
      (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    // Scaffold konteksti (Localizations altında)
    final ctx = tester.element(find.byType(Scaffold).first);
    showReserveSheet(ctx, state, mockPartners.first); // v1: manuel təsdiq
    await tester.pumpAndSettle();

    await tester.tap(find.text('Bu gün'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('10:00'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Davam et'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Təsdiqlə'));
    await tester.pumpAndSettle();

    // v1 manuel → GÖZLƏYİR + QR (ROL E)
    expect(find.text('GÖZLƏYİR'), findsOneWidget);
    expect(find.byType(QrPlaceholder), findsOneWidget);
    expect(state.reservations.length, 1);
    expect(state.reservations.first.statusLabel, 'GÖZLƏYİR');
  });

  testWidgets('kupon: XP ilə al → Kuponlarım (ROL G)', (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    // D-026: kupon obyektin öz bazasından alınır — Coffee Lab balansı
    // boot-dan əvvəl seed edilir ki, düymə enabled qurulsun.
    state.venueXp['v2']!.balance = 400;
    await boot(tester, state);

    await tester.tap(find.text('Profil'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Kuponlarım'), 200,
        scrollable: find.byType(Scrollable).first);
    await tester.tap(find.text('Kuponlarım'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Endirim XP balansından'), findsOneWidget);
    final balBefore = state.venueXp['v2']!.balance;
    final totalBefore = state.xp;

    // o1 Coffee Lab 15% (200 XP) — v2 bazasından alınır
    await tester.tap(find.text('XP ilə al').first);
    await tester.pumpAndSettle();
    expect(state.venueXp['v2']!.balance, balBefore - 200);
    expect(state.xp, totalBefore); // Peşəkarlıq XP-si düşmür (D-026)

    // Kuponlarım tabına keç
    await tester.tap(find.text('Kuponlarım').first);
    await tester.pumpAndSettle();
    expect(find.textContaining('AKTİV'), findsWidgets);
    expect(find.textContaining('QSHUL-'), findsWidgets);
  });

  testWidgets('tədbir: qoşul (+10) → check-in (+40) (ROL F)', (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    final ctx = tester.element(find.byType(Scaffold).first);
    final event = state.cityEvents.first; // e1: açıq qoşulma
    showEventDetailSheet(ctx, state, event);
    await tester.pumpAndSettle();

    final before = state.xp;
    await tester.tap(find.text('Tədbirə qoşul'));
    await tester.pumpAndSettle();
    expect(state.joined.contains(event.id), isTrue);
    expect(state.xp, before + 10);

    await dragUntilFound(tester, 
        find.textContaining('Yerində check-in et'), find.byType(ListView).last);
    await tester.tap(find.textContaining('Yerində check-in et').first);
    await tester.pumpAndSettle();
    expect(state.checkedIn.contains(event.id), isTrue);
    expect(state.xp, before + 50); // +10 +40
    expect(state.earnedBadges.contains('firstEvent'), isTrue);
  });

  testWidgets('ayarlar: məxfilik switch-ləri state-ə bağlı (ROL TAB 5)',
      (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    await tester.tap(find.text('Profil'));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.settings));
    await tester.pumpAndSettle();

    // D-027: 3-seçimli görünüş — Hamı / Yalnız dostlar / Gizli.
    expect(find.text('Xəritədə görünmə'), findsOneWidget);
    expect(state.mapVisibility, MapVisibility.everyone);

    // Dialog aç → "Gizli" seç
    await tester.tap(find.text('Xəritədə görünmə'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Gizli'));
    await tester.pumpAndSettle();
    expect(state.mapVisibility, MapVisibility.off);

    // "Yalnız dostlar" seç
    await tester.tap(find.text('Xəritədə görünmə'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Yalnız dostlar'));
    await tester.pumpAndSettle();
    expect(state.mapVisibility, MapVisibility.friends);

    // Məsafə switch-i (indi ilk Switch) söndür
    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();
    expect(state.showDistance, isFalse);
  });

  testWidgets('bildiriş mərkəzi: tercih bölməsi + tiplər (ROL H)',
      (tester) async {
    final state = AppState()
      ..signIn()
      ..onboarded = true;
    await boot(tester, state);

    await tester.tap(find.byIcon(Icons.notifications_none));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tercihlər').first); // sheet-dəki keçid
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tercihlər').last); // ekrandaki toggle
    await tester.pumpAndSettle();

    // Hər tip ayrıca switch (ROL H)
    expect(find.text('Yeni match'), findsOneWidget);
    expect(find.text('Tədbir xatırlatması'), findsOneWidget);
    expect(find.text('Rezerv yeniliyi'), findsOneWidget);
    expect(find.text('Yeniliklər və təkliflər'), findsOneWidget);

    // Tip söndürmə state-ə düşür (ilk switch = Yeni match)
    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();
    expect(state.notifPrefs.isEnabled(NotifType.match), isFalse);
  });
}
