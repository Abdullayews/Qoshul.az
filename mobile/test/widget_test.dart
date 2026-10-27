import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:qoshul/core/data/mock_data.dart';
import 'package:qoshul/core/models/models.dart';
import 'package:qoshul/core/state/app_state.dart';
import 'package:qoshul/main.dart';

void main() {
  group('XpRules (web rules.ts portu)', () {
    test('rankForXp sərhədləri', () {
      expect(XpRules.rankForXp(0), 'Yeni');
      expect(XpRules.rankForXp(200), 'Yeni');
      expect(XpRules.rankForXp(201), 'Kəşfiyyatçı');
      expect(XpRules.rankForXp(320), 'Kəşfiyyatçı');
      expect(XpRules.rankForXp(7000), 'Ultras');
      expect(XpRules.rankForXp(99999), 'Legend');
    });

    test('xpProgress hesablanması', () {
      final p = XpRules.xpProgress(320);
      expect(p['nextName'], 'Aktiv');
      expect(p['toNext'], 281);
      expect(p['pct'], 30); // (320-200)/(600-200) = 30%
    });
  });

  group('parseAssistantEvent ([TƏDBİR] parser)', () {
    test('təklifsiz mətn olduğu kimi qalır', () {
      final r = parseAssistantEvent('Salam, necə kömək edə bilərəm?');
      expect(r.proposal, isNull);
      expect(r.text, 'Salam, necə kömək edə bilərəm?');
    });

    test('təklif düzgün ayrışdırılır', () {
      const raw =
          'Şənbə üçün plan:\n[TƏDBİR]{"title":"Yürüş","category":"Yürüş","date":"2026-11-01","time":"09:00","capacity":10,"location":"Qala","description":"Asan marşrut","partner":""}';
      final r = parseAssistantEvent(raw);
      expect(r.text, 'Şənbə üçün plan:');
      expect(r.proposal, isNotNull);
      expect(r.proposal!.title, 'Yürüş');
      expect(r.proposal!.capacity, 10);
      expect(r.proposal!.date, '2026-11-01');
    });
  });

  group('AppState (mock məntiq)', () {
    test('qoşulma XP artırır və joined-a salır', () {
      final s = AppState();
      final before = s.xp;
      expect(s.join('e1'), isTrue);
      expect(s.joined.contains('e1'), isTrue);
      expect(s.xp, before + 50);
      // təkrar qoşulma yox
      expect(s.join('e1'), isFalse);
    });

    test('təsdiqli tədbir pending-ə düşür', () {
      final s = AppState();
      expect(s.join('e3'), isTrue); // e3 approval=true
      expect(s.pending.contains('e3'), isTrue);
      expect(s.joined.contains('e3'), isFalse);
    });

    test('tədbir yaratma XP + başlıq əlavə edir', () {
      final s = AppState();
      final before = s.xp;
      s.createEvent(
        title: 'Test görüşü',
        category: 'Qəhvə',
        city: 'Bakı',
        location: 'Test kafe',
        date: '2026-11-01',
        time: '10:00',
        capacity: 5,
        description: 'Test',
      );
      expect(s.events.first.title, 'Test görüşü');
      expect(s.xp, before + 100);
      expect(s.joined.contains(s.events.first.id), isTrue);
    });

    test('dostluq sorğu axını', () {
      final s = AppState();
      expect(s.friendships['p2'], 'accepted'); // Rəşad əvvəlcədən dost
      s.requestFriend('p1');
      expect(s.friendships['p1'], 'sent');
      s.acceptFriend('p1');
      expect(s.friendships['p1'], 'accepted');
      expect(s.acceptedFriends.length, 2);
    });
  });

  group('AuthScreen', () {
    testWidgets('başlıq, Google düyməsi və form render olunur', (tester) async {
      await tester.pumpWidget(const QoshulApp());
      await tester.pumpAndSettle();
      expect(find.text('Xoş gəldin!'), findsOneWidget);
      expect(find.text('Google ilə davam et'), findsOneWidget);
      expect(find.text('Hesabın yoxdur? Qeydiyyatdan keç'), findsOneWidget);
      // wordmark Text.rich-dır — textContaining ilə axtarılır
      expect(find.textContaining('qoshul.az'), findsOneWidget);
    });

    testWidgets('Google düyməsi sign-in edir → onboarding açılır', (tester) async {
      await tester.pumpWidget(const QoshulApp());
      await tester.pumpAndSettle();
      await tester.tap(find.text('Google ilə davam et'));
      await tester.pump(const Duration(milliseconds: 100));
      await tester.pumpAndSettle();
      expect(find.text('Qeydiyyat anketi'), findsOneWidget);
      expect(find.textContaining('Əsas məlumat'), findsWidgets);
    });
  });

  group('AppShell (tam istifadəçi axını)', () {
    AppState readyState() {
      final s = AppState()
        ..signedIn = true
        ..onboarded = true
        ..setProfileName('Kənan Səmədov');
      return s;
    }

    testWidgets('feed: salamlaşma, XP, hero, tədbir kartları', (tester) async {
      // ListView lazy-dir — bütün məzmunun build olması üçün hündür səth.
      tester.view.physicalSize = const Size(800, 12000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      expect(find.text('Salam, Kənan!'), findsOneWidget);
      expect(find.text('Bu gün yeni bir hekayəyə qoşul.'), findsOneWidget);
      expect(find.text('Şəhər səninlə\ndaha maraqlıdır.'), findsOneWidget);
      expect(find.text('Yaxınlıqda nə var?'), findsOneWidget);
      // mock tədbirlərdən biri görünür (tövsiyə + grid — 2 yer)
      expect(find.text('Şənbə səhəri qəhvə və söhbət'), findsWidgets);
      expect(find.text('Qoşul'), findsWidgets);
    });

    testWidgets('bottom nav: Kəşf et tabına keçid', (tester) async {
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Kəşf et'));
      await tester.pumpAndSettle();
      expect(find.text('Yeni planlar kəşf et'), findsOneWidget);
      expect(find.text('Yaxınlıqda'), findsOneWidget);
    });

    testWidgets('explore: İnsanlar tab — AI uyğunluq kartları', (tester) async {
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Kəşf et'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('İnsanlar'));
      await tester.pumpAndSettle();
      expect(find.text('96% AI uyğunluq'), findsOneWidget);
      expect(find.text('Dostluq sorğusu'), findsWidgets);
      expect(find.text('Leyla Məmmədova'), findsOneWidget);
    });

    testWidgets('çatlar: tədbir söhbəti + dost söhbəti', (tester) async {
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Söhbətlər'));
      await tester.pumpAndSettle();
      expect(find.text('Tədbir söhbətləri'), findsOneWidget);
      expect(find.text('Dost söhbətləri'), findsOneWidget);
      expect(find.text('Keçən həftə qaçış klubu'), findsOneWidget);
      expect(find.text('Rəşad Əliyev'), findsOneWidget);
    });

    testWidgets('tədbir qoşulması XP artırır (snackbar)', (tester) async {
      tester.view.physicalSize = const Size(800, 12000);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      final joinButtons = find.text('Qoşul');
      expect(joinButtons, findsWidgets);
      await tester.tap(joinButtons.first);
      await tester.pump(); // snackbar
      expect(find.byType(SnackBar), findsOneWidget);
      expect(s.joined.contains('e1'), isTrue);
    });

    testWidgets('assistant: sual → mock cavab axını', (tester) async {
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      // header-dəki Bot ikonu ilə köməkçiyə keç
      await tester.tap(find.byIcon(Icons.smart_toy));
      await tester.pumpAndSettle();
      expect(find.text('Qoshul köməkçisi'), findsOneWidget);
      // təklif çipinə toxun
      await tester.tap(find.text('Şənbə üçün tədbir fikri ver'));
      await tester.pump(const Duration(milliseconds: 600)); // yazı simulyasiyası başlayır
      await tester.pumpAndSettle();
      // cavab gəldi və [TƏDBİR] təklif kartı göründü
      expect(find.text('Bu tədbiri yarat'), findsOneWidget);
      expect(s.assistantMsgs.length, 2); // sual + cavab
    });

    testWidgets('profil: statistika və tablar', (tester) async {
      final s = readyState();
      await tester.pumpWidget(buildAppWithState(s));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Profil'));
      await tester.pumpAndSettle();
      expect(find.text('Kənan Səmədov'), findsOneWidget);
      expect(find.text('XP xalı'), findsOneWidget);
      expect(find.text('Tədbirlərim'), findsOneWidget);
      expect(find.text('Yadda saxlanılanlar'), findsOneWidget);
      expect(find.text('Nailiyyətlər'), findsOneWidget);
    });
  });

  group('Mock data tutarlılığı', () {
    test('kateqoriya → qrup xəritələməsi düzgündür', () {
      expect(groupOf('Qəhvə'), 'Yemək və içki');
      expect(groupOf('Futbol'), 'İdman');
      expect(groupOf('Yürüş'), 'Təbiət');
      expect(groupOf('Hakaton'), 'Texnologiya');
      expect(groupOf('Şəhər turu'), 'Səyahət və digər'); // həmin qrupun öz üzvü
      expect(groupOf('Mövcud olmayan'), 'Səyahət və digər'); // fallback
    });

    test('şəkil xəritələmə boş deyil', () {
      for (final g in categoryGroups) {
        expect(imageForCategory(g.items.first), isNotEmpty);
      }
      expect(imageForCategory('Yürüş'), imgPadel);
      expect(imageForCategory('Qəhvə'), imgCoffee);
      expect(imageForCategory('Hakaton'), imgTech);
    });

    test('hər mock tədbir lazımi sahələrə malikdir', () {
      for (final e in mockEvents) {
        expect(e.title, isNotEmpty);
        expect(e.location, isNotEmpty);
        expect(e.capacity, greaterThanOrEqualTo(e.participants));
      }
    });
  });
}
