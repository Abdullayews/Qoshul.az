import 'package:flutter/material.dart';

import 'core/state/app_state.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/presentation/auth_screen.dart';
import 'features/onboarding/presentation/onboarding_screen.dart';
import 'features/shell/app_shell.dart';

/// Qoshul.az — mobil tətbiq (UI mərhələsi, D-023).
///
/// Axın: Auth → Onboarding → AppShell (4 tab).
/// Bütün data mock-dur; backend inteqrasiyasında API ilə əvəz olunacaq.
void main() {
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
