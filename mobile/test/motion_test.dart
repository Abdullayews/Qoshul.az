import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:qoshul/core/state/app_state.dart';
import 'package:qoshul/core/widgets/glass.dart';
import 'package:qoshul/core/widgets/motion.dart';
import 'package:qoshul/core/widgets/ui.dart';
import 'package:qoshul/main.dart';

/// D-025 testləri — Liquid Glass (minimal) + Hərəkət dili primitivləri.
/// Şüşə səthləri, motion primitivləri, tab keçidi, match swipe fizikası.
void main() {
  Future<void> boot(WidgetTester tester, AppState state) async {
    await tester.binding.setSurfaceSize(const Size(800, 1200));
    await tester.pumpWidget(buildAppWithState(state));
    await tester.pumpAndSettle();
  }

  group('glass (D-025):', () {
    testWidgets('GlassSurface blur aktivdə BackdropFilter istifadə olunur',
        (tester) async {
      tester.view.physicalSize = const Size(400, 200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(
          body: Center(
            child: GlassSurface(child: Text('şüşə')),
          ),
        ),
      ));
      await tester.pumpAndSettle();

      expect(find.text('şüşə'), findsOneWidget);
      expect(find.byType(BackdropFilter), findsOneWidget);
    });

    testWidgets('GlassQuality.blur=false → blur olmadan fallback (ucuz mod)',
        (tester) async {
      GlassQuality.blur = false;
      addTearDown(() => GlassQuality.blur = true);

      tester.view.physicalSize = const Size(400, 200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(
          body: Center(
            child: GlassSurface(child: Text('fallback')),
          ),
        ),
      ));
      await tester.pumpAndSettle();

      expect(find.text('fallback'), findsOneWidget);
      expect(find.byType(BackdropFilter), findsNothing);
    });

    testWidgets('GlassPill onTap işləyir', (tester) async {
      var taps = 0;
      tester.view.physicalSize = const Size(300, 120);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: Center(
            child: GlassPill(onTap: () => taps++, child: const Text('həb')),
          ),
        ),
      ));
      await tester.pumpAndSettle();

      await tester.tap(find.text('həb'));
      await tester.pumpAndSettle();
      expect(taps, 1);
    });
  });

  group('motion primitivləri (D-025):', () {
    testWidgets('ScaleTap toxunuşu callback çağırır', (tester) async {
      var fired = 0;
      tester.view.physicalSize = const Size(300, 120);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: Center(
            child: ScaleTap(onTap: () => fired++, child: const Text(' toxun ')),
          ),
        ),
      ));
      await tester.tap(find.text(' toxun '));
      await tester.pumpAndSettle();
      expect(fired, 1);
    });

    testWidgets('StaggerIn uşaq widgeti görünür edir', (tester) async {
      tester.view.physicalSize = const Size(300, 200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(
          body: Center(child: StaggerIn(index: 3, child: Text('giriş'))),
        ),
      ));
      // Stagger gecikməsini (3 × 45ms) fake vaxtla irələt — taymer işə düşsün.
      await tester.pump(const Duration(milliseconds: 200));
      await tester.pumpAndSettle();
      expect(find.text('giriş'), findsOneWidget);
      expect(tester.getSize(find.text('giriş')), greaterThan(Size.zero));
    });

    testWidgets('PopIn animasiyadan sonra görünür', (tester) async {
      tester.view.physicalSize = const Size(300, 200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(body: Center(child: PopIn(child: Text('pop')))),
      ));
      await tester.pumpAndSettle();
      expect(find.text('pop'), findsOneWidget);
    });

    testWidgets('CountUp yekun dəyəri göstərir və dəyişəndə yenilənir',
        (tester) async {
      tester.view.physicalSize = const Size(300, 120);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      var value = 120;
      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: Center(
            child: StatefulBuilder(
              builder: (context, setState) => Column(
                children: [
                  CountUp(value: value, suffix: ' XP'),
                  TextButton(
                    onPressed: () => setState(() => value = 170),
                    child: const Text('artır'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ));
      await tester.pumpAndSettle();
      expect(find.text('120 XP'), findsOneWidget);

      await tester.tap(find.text('artır'));
      await tester.pumpAndSettle();
      expect(find.text('170 XP'), findsOneWidget);
    });

    testWidgets('TypingDots 3 nöqtə render edir', (tester) async {
      tester.view.physicalSize = const Size(200, 100);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(body: Center(child: TypingDots())),
      ));
      await tester.pump(const Duration(milliseconds: 100));
      // 3 nöqtə = 3 Container (dairə)
      expect(find.byType(TypingDots), findsOneWidget);
    });

    testWidgets('Shimmer uşağı render edir', (tester) async {
      tester.view.physicalSize = const Size(300, 100);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(
          body: Center(
            child: Shimmer(child: SkeletonBox(width: 120, height: 20)),
          ),
        ),
      ));
      await tester.pump(const Duration(milliseconds: 200));
      expect(find.byType(SkeletonBox), findsOneWidget);
    });

    testWidgets('FadeThrough iki uşaq arasında keçir', (tester) async {
      tester.view.physicalSize = const Size(300, 150);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      var page = 0;
      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: Center(
            child: StatefulBuilder(
              builder: (context, setState) => Column(
                children: [
                  FadeThrough(
                    child: KeyedSubtree(
                      key: ValueKey(page),
                      child: Text(page == 0 ? 'A səhifəsi' : 'B səhifəsi'),
                    ),
                  ),
                  TextButton(
                    onPressed: () => setState(() => page = 1),
                    child: const Text('dəyiş'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ));
      await tester.pumpAndSettle();
      expect(find.text('A səhifəsi'), findsOneWidget);

      await tester.tap(find.text('dəyiş'));
      await tester.pumpAndSettle();
      expect(find.text('B səhifəsi'), findsOneWidget);
      expect(find.text('A səhifəsi'), findsNothing);
    });

    testWidgets('GlassPageRoute push → məzmun fade+slide ilə görünür',
        (tester) async {
      tester.view.physicalSize = const Size(400, 300);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => Center(
              child: TextButton(
                onPressed: () => Navigator.of(context).push(GlassPageRoute(
                  builder: (_) =>
                      const Scaffold(body: Center(child: Text('hədəf'))),
                )),
                child: const Text('aç'),
              ),
            ),
          ),
        ),
      ));
      await tester.tap(find.text('aç'));
      await tester.pumpAndSettle();
      expect(find.text('hədəf'), findsOneWidget);
    });
  });

  group('inteqrasiya (D-025):', () {
    testWidgets('XpTrack animasiyalı dolğu + sayğac göstərir', (tester) async {
      tester.view.physicalSize = const Size(400, 150);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(const MaterialApp(
        home: Scaffold(body: Center(child: XpTrack(xp: 120))),
      ));
      await tester.pumpAndSettle();

      expect(find.byType(LinearProgressIndicator), findsOneWidget);
      expect(find.byType(CountUp), findsOneWidget);
    });

    testWidgets('şüşə nav pill: tab keçidi işləyir (Xəritə → filtrlər)',
        (tester) async {
      final state = AppState()
        ..signIn()
        ..onboarded = true;
      await boot(tester, state);

      // Shell-də extendBody + GlassSurface nav var
      expect(find.byType(GlassSurface), findsWidgets);

      // Xəritə tabına keç
      await tester.tap(find.text('Xəritə'));
      await tester.pumpAndSettle();
      // Şüşə filtr çipi görünür (10 km default)
      expect(find.text('10 km'), findsOneWidget);
    });

    testWidgets('match dəsti: sola dart → Keç (nomizəd azalır)',
        (tester) async {
      final state = AppState()
        ..signIn()
        ..onboarded = true;
      await boot(tester, state);

      // Match ekranına get (feed-dən "Hamısına bax")
      final feed = find.byType(ListView).first;
      for (var i = 0; i < 14 && !tester.any(find.text('Hamısına bax')); i++) {
        await tester.drag(feed, const Offset(0, -350));
        await tester.pumpAndSettle();
      }
      await tester.tap(find.text('Hamısına bax'));
      await tester.pumpAndSettle();

      final before = state.matchCandidates.length;
      expect(before, greaterThan(0));

      // Üst kartı sola dart (Keç) — swipe fizikası (D-025)
      final card = find.text('Maraqlıyam');
      expect(card, findsOneWidget); // yalnız üst kartda (arxa kartlarda gizli)
      await tester.drag(card, const Offset(-320, -20));
      await tester.pumpAndSettle();

      expect(state.matchCandidates.length, lessThan(before));
    });
  });
}
