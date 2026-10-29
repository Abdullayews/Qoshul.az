import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'core/platform/native_bridge.dart';
import 'core/state/app_state.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/glass.dart';
import 'features/auth/presentation/auth_screen.dart';
import 'features/onboarding/presentation/onboarding_screen.dart';
import 'features/shell/app_shell.dart';

/// Qoshul.az — mobil tətbiq (UI mərhələsi, D-023).
///
/// Axın: Auth → Onboarding → AppShell (5 tab).
/// Bütün data mock-dur; backend inteqrasiyasında API ilə əvəz olunacaq.
///
/// v0.6.0 (D-031): tam ekran = **edge-to-edge** (Instagram paradigması) —
/// məzmun şəffaf status/nav barların altından axır; barlar gizlənmir,
/// üstünə düşmür. Native tərəfdə (Kotlin, D-032) insets güvəncəsi var.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // ── Tam ekran: edge-to-edge (D-031) ──
  // Android 15 (API 35) bu rejimi məcburi edir; köhnə versiyalarda da
  // biz özümüz qaldırırıq → status bar üstəlik kimi YOX, şəffaf qat kimi.
  await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark, // Android: tünd ikonlar
    statusBarBrightness: Brightness.light, // iOS: fon açıq → tünd status
    systemNavigationBarColor: Colors.transparent,
    systemNavigationBarIconBrightness: Brightness.dark,
    systemNavigationBarDividerColor: Colors.transparent,
  ));

  // ── Adaptiv keyfiyyət (D-032): zəif cihazda blur özünü söndürür ──
  // NativePerfInfo (Java) → lowRam/memoryClass → GlassQuality.blur=false.
  // Instagram-ın adaptiv render keyfiyyəti ilə eyni ideya: cihaz nə verirsə,
  // UI o qədər "şüşə" çəkir — 60fps hər yerdə prioritetdir.
  final perf = await NativeBridge.perfInfo();
  if (perf != null) {
    final lowRam = perf['lowRam'] == true;
    final memClass = (perf['memoryClass'] as num?)?.toInt() ?? 256;
    if (lowRam || memClass < 160) {
      GlassQuality.blur = false;
    }
  }

  runApp(const QoshulApp());
}

class QoshulApp extends StatefulWidget {
  const QoshulApp({super.key});

  @override
  State<QoshulApp> createState() => _QoshulAppState();
}

class _QoshulAppState extends State<QoshulApp> {
  final AppState state = AppState();

  @override
  void dispose() {
    state.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Qoshul.az',
      debugShowCheckedModeBanner: false,
      theme: buildQoshulTheme(),
      home: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          if (!state.signedIn) {
            return AuthScreen(onSignedIn: state.signIn);
          }
          if (!state.onboarded) {
            return OnboardingScreen(
              state: state,
              onDone: () {}, // onboarded state dəyişəndə AnimatedBuilder yenidən qurur
            );
          }
          return AppShell(state: state);
        },
      ),
    );
  }
}

/// Widget testləri üçün köməkçi — tətbiqi verilmiş state ilə qurur.
Widget buildAppWithState(AppState state) {
  return MaterialApp(
    title: 'Qoshul.az',
    debugShowCheckedModeBanner: false,
    theme: buildQoshulTheme(),
    home: AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        if (!state.signedIn) {
          return AuthScreen(onSignedIn: state.signIn);
        }
        if (!state.onboarded) {
          return OnboardingScreen(state: state, onDone: () {});
        }
        return AppShell(state: state);
      },
    ),
  );
}
