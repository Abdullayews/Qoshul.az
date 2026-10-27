import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Qoshul.az — mobil tətbiq.
///
/// Bu, APK pipeline-ın (branch: feature/apk-pipeline, D-017) ilk çıxışı üçün
/// hazırlanmış m1 skelet ekranıdır — build-in işlədiyini real ekranda göstərir.
/// m7-də tam arxitektura (core/ + features/, BLoC) ilə əvəz olunacaq.

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
    ),
  );
  runApp(const QoshulApp());
}

class QoshulApp extends StatelessWidget {
  const QoshulApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Qoshul.az',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFFF4D6D),
          brightness: Brightness.dark,
        ),
        scaffoldBackgroundColor: const Color(0xFF0F0F17),
      ),
      home: const SkeletonScreen(),
    );
  }
}

/// Yol xəritəsi sətirləri: (status simvolu, mətn).
const List<(String, String)> kRoadmap = [
  ('✓', 'Monorepo + Docker + arm64 (m1)'),
  ('→', 'Backend nüvəsi: NestJS + Drizzle (m2)'),
  ('·', 'Auth: JWT + Google/Apple (m3)'),
  ('·', 'Profil + Match + Events (m4)'),
  ('·', 'Çat + Bildirişlər (m5–m6)'),
  ('·', 'Tam Flutter tətbiqi (m7–m8)'),
  ('·', 'AI köməkçi + Production (m9–m10)'),
];

class SkeletonScreen extends StatelessWidget {
  const SkeletonScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: const [
              SizedBox(height: 32),
              _Logo(),
              SizedBox(height: 28),
              _Wordmark(),
              SizedBox(height: 8),
              _Tagline(),
              SizedBox(height: 24),
              _StatusChip(),
              SizedBox(height: 32),
              _RoadmapCard(),
              SizedBox(height: 32),
              _Footer(),
            ],
          ),
        ),
      ),
    );
  }
}

class _Logo extends StatelessWidget {
  const _Logo();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 96,
        height: 96,
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFFF4D6D), Color(0xFFFF8A4D)],
          ),
          borderRadius: BorderRadius.circular(28),
          boxShadow: const [
            BoxShadow(
              color: Color(0x59FF4D6D),
              blurRadius: 40,
              offset: Offset(0, 12),
            ),
          ],
        ),
        child: const Center(
          child: Text(
            'q.',
            style: TextStyle(
              fontSize: 46,
              fontWeight: FontWeight.w800,
              color: Colors.white,
            ),
          ),
        ),
      ),
    );
  }
}

class _Wordmark extends StatelessWidget {
  const _Wordmark();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text(
        'qoshul.az',
        textAlign: TextAlign.center,
        style: TextStyle(
          fontSize: 34,
          fontWeight: FontWeight.w800,
          letterSpacing: -0.5,
          color: Color(0xFFF5F5FA),
        ),
      ),
    );
  }
}

class _Tagline extends StatelessWidget {
  const _Tagline();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text(
        'Yaxınlığındakı insanlarla tanış ol',
        textAlign: TextAlign.center,
        style: TextStyle(
          fontSize: 15,
          height: 1.4,
          color: Color(0xFF9B9BAF),
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: const Color(0x1FFF4D6D),
          borderRadius: BorderRadius.circular(999),
        ),
        child: const Text(
          'İLK APK · MƏRHƏLƏ 1 / 10',
          style: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w700,
            letterSpacing: 1.2,
            color: Color(0xFFFF8FA3),
          ),
        ),
      ),
    );
  }
}

class _RoadmapCard extends StatelessWidget {
  const _RoadmapCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF181826),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFF23233A)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Yol xəritəsi',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              letterSpacing: 1.0,
              color: Color(0xFF7A7A94),
            ),
          ),
          const SizedBox(height: 14),
          ...kRoadmap.map((row) => _RoadmapRow(symbol: row.$1, text: row.$2)),
        ],
      ),
    );
  }
}

class _RoadmapRow extends StatelessWidget {
  const _RoadmapRow({required this.symbol, required this.text});

  final String symbol;
  final String text;

  Color get _symbolColor => switch (symbol) {
        '✓' => const Color(0xFF4ADE80),
        '→' => const Color(0xFFFF4D6D),
        _ => const Color(0xFF55556B),
      };

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 26,
            child: Text(
              symbol,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: _symbolColor,
              ),
            ),
          ),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontSize: 14,
                height: 1.35,
                color: Color(0xFFC4C4D4),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Footer extends StatelessWidget {
  const _Footer();

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Column(
        children: [
          Text(
            'v0.1.0 · ilk build',
            style: TextStyle(fontSize: 12, color: Color(0xFF7A7A94)),
          ),
          SizedBox(height: 4),
          Text(
            'CI: GitHub Actions · imza: debug (keystore m7-də)',
            style: TextStyle(fontSize: 11, color: Color(0xFF55556B)),
          ),
        ],
      ),
    );
  }
}
