import 'package:flutter/material.dart';

/// Qoshul.az dizayn tokenləri — web (Amin099015/qoshul styles.css) ilə birebir.
/// Rənglər OKLCH-dən sRGB-yə çevrilib (scripts/oklch_to_hex.py, 2026-10-28).
class Q {
  Q._();

  // :root tokenləri
  static const background = Color(0xFFF8FAFC); // oklch(0.985 0.003 247.86)
  static const foreground = Color(0xFF0F172B); // oklch(0.208 0.042 265.76)
  static const card = Color(0xFFFFFFFF);
  static const primary = Color(0xFF00A63E); // oklch(0.627 0.194 149.21)
  static const primaryFg = Color(0xFFFFFFFF);
  static const secondary = Color(0xFFE6F7EA); // oklch(0.961 0.025 152)
  static const muted = Color(0xFFEEF2F5); // oklch(0.959 0.006 247)
  static const mutedFg = Color(0xFF687280); // oklch(0.55 0.025 257)
  static const accent = Color(0xFFFE9A00); // oklch(0.769 0.188 70.08)
  static const accentFg = Color(0xFF7A3800); // oklch(0.42 0.115 57)
  static const destructive = Color(0xFFE7000B); // oklch(0.577 0.245 27.32)
  static const border = Color(0xFFE3E8ED); // oklch(0.929 0.009 249)

  // törəmə rənglər
  static const rankBadgeBg = Color(0xFFFEF2DE); // accent 13% + card
  static const overlayDark = Color(0xFF002211); // oklch(0.16 0.033 159)
  static const photoShade = Color(0xFF00140E); // oklch(0.18 0.02 155)
  static const glass = Color(0x21000000); // oklch(1 0 0 / 0.13)
  static const glassBorder = Color(0x2EFFFFFF); // oklch(1 0 0 / 0.18)

  // radiuslar (web --radius-*)
  static const rSm = 4.0;
  static const rMd = 6.0;
  static const rLg = 8.0;
  static const rXl = 8.0;
  static const rMap = 18.0;
  static const rAiBox = 16.0;

  static const softShadow = BoxShadow(
    color: Color(0x0B28301F), // oklch(0.25 0.025 155 / 0.045)
    blurRadius: 18,
    offset: Offset(0, 3),
  );
}

/// Tipografiya — Plus Jakarta Sans.
/// v0.7.0 (D-035): web mobil-breakpoint ölçülərindən MÖBİL TƏTBİQ
/// ölçülərinə keçid — 9-10px mikroyazılar oxunmurdu; minimum 10.5,
/// gövdə 13.5 (Material/mobil standartlarına uyğunlaşdırıldı).
class T {
  T._();

  static const String family = 'Plus Jakarta Sans';

  static const hero = TextStyle(
      fontFamily: family, fontSize: 27, fontWeight: FontWeight.w800, height: 1.3);
  static const h1 = TextStyle(
      fontFamily: family, fontSize: 24, fontWeight: FontWeight.w800, height: 1.35);
  static const h2 = TextStyle(
      fontFamily: family, fontSize: 19, fontWeight: FontWeight.w800, height: 1.4);
  static const h3 = TextStyle(
      fontFamily: family, fontSize: 13, fontWeight: FontWeight.w800, height: 1.5);
  static const eventTitle = TextStyle(
      fontFamily: family, fontSize: 16, fontWeight: FontWeight.w800, height: 1.5);
  static const personName = TextStyle(
      fontFamily: family, fontSize: 15, fontWeight: FontWeight.w800, height: 1.4);
  static const subtitle = TextStyle(
      fontFamily: family,
      fontSize: 13,
      fontWeight: FontWeight.w400,
      height: 1.7,
      color: Q.mutedFg);
  static const body = TextStyle(fontFamily: family, fontSize: 13.5, height: 1.7);
  static const bodySm = TextStyle(fontFamily: family, fontSize: 12.5, height: 1.6);
  static const meta = TextStyle(
      fontFamily: family, fontSize: 12, height: 1.5, color: Q.mutedFg);
  static const metaSm = TextStyle(
      fontFamily: family, fontSize: 10.5, height: 1.5, color: Q.mutedFg);
  static const category = TextStyle(
      fontFamily: family, fontSize: 10.5, fontWeight: FontWeight.w700, color: Q.primary);
  static const chip = TextStyle(
      fontFamily: family, fontSize: 11.5, fontWeight: FontWeight.w500, color: Q.mutedFg);
  static const chipActive = TextStyle(
      fontFamily: family, fontSize: 11.5, fontWeight: FontWeight.w700, color: Q.primaryFg);
  static const button = TextStyle(
      fontFamily: family, fontSize: 13, fontWeight: FontWeight.w600);
  static const label = TextStyle(
      fontFamily: family, fontSize: 12, fontWeight: FontWeight.w600);
  static const input = TextStyle(fontFamily: family, fontSize: 13.5, fontWeight: FontWeight.w400);
  static const stat = TextStyle(
      fontFamily: family, fontSize: 22, fontWeight: FontWeight.w800);
}

/// ThemeData — Material 3, Qoshul tokenləri ilə.
ThemeData buildQoshulTheme() {
  final base = ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    fontFamily: T.family,
    colorScheme: const ColorScheme.light(
      primary: Q.primary,
      onPrimary: Q.primaryFg,
      secondary: Q.secondary,
      onSecondary: Q.primary,
      surface: Q.card,
      onSurface: Q.foreground,
      surfaceContainerHighest: Q.muted,
      error: Q.destructive,
      outline: Q.border,
      outlineVariant: Q.border,
    ),
    scaffoldBackgroundColor: Q.background,
    splashFactory: InkSparkle.splashFactory,
    dividerColor: Q.border,
  );

  return base.copyWith(
    textTheme: base.textTheme.apply(
      bodyColor: Q.foreground,
      displayColor: Q.foreground,
      fontFamily: T.family,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: Q.primary,
        foregroundColor: Q.primaryFg,
        minimumSize: const Size(64, 36),
        padding: const EdgeInsets.symmetric(horizontal: 14),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Q.rMd)),
        textStyle: T.button,
        elevation: 0,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        backgroundColor: Q.card,
        foregroundColor: Q.foreground,
        minimumSize: const Size(64, 36),
        side: const BorderSide(color: Q.border),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Q.rMd)),
        textStyle: T.button,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: Q.primary,
        textStyle: T.button,
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Q.card,
      contentPadding: const EdgeInsets.symmetric(horizontal: 12),
      constraints: const BoxConstraints(minHeight: 42),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(Q.rMd),
        borderSide: const BorderSide(color: Q.border),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(Q.rMd),
        borderSide: const BorderSide(color: Q.border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(Q.rMd),
        borderSide: const BorderSide(color: Q.primary, width: 1.4),
      ),
      hintStyle: T.input.copyWith(color: Q.mutedFg),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: Q.card,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Q.rLg)),
      titleTextStyle: T.h2,
    ),
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: Q.card,
      modalBackgroundColor: Q.card,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(Q.rLg)),
      ),
      showDragHandle: false,
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: Q.foreground,
      contentTextStyle: T.body.copyWith(color: Q.card),
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Q.rMd)),
    ),
    checkboxTheme: CheckboxThemeData(
      fillColor: WidgetStateProperty.resolveWith((states) =>
          states.contains(WidgetState.selected) ? Q.primary : Q.card),
      side: const BorderSide(color: Q.border, width: 1.4),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Q.rSm)),
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(color: Q.primary),
  );
}
