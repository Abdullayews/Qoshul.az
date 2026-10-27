import 'dart:ui';

import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Liquid Glass — minimal interpretasiya (D-025).
///
/// Mənbə: Apple iOS 26 "Liquid Glass" (WWDC25) — 4 prinsip:
///   1. lensing        — arxadakı məzmun şüşədən keçərkən yumşalır (blur)
///   2. işıq qatı      — üzərdən gələn işıq qradiyenti (yuxarı-aşağı ağ alfa)
///   3. spekulyar kənar — kənarda 1px ağ kontur (işıq əks olunması)
///   4. funksional qat — şüşə YALNIZ naviqasiya/kontrol üzərində üzən səthlərdə,
///                       heç vaxt məzmun kartlarında (Apple HIG: kontent birinci)
///
/// Bu layihədə cəmi 5 səth növü (D-025 "minimal" qərarı):
///   alt nav pill · Yarat FAB · xəritə filtr çipləri · xəritə alt kartı ·
///   match fəaliyyət düymələri.
/// Kartlar/siyahılar/formalar web-parity düz qalır (D-023 birebir).
///
/// Performans qaydası: bir ekranda max ~4 BackdropFilter (hər biri ~1.3ms,
/// Impeller Android-də çoxlu filtr xərclidir) → blur yalnız real üst-üstə
/// düşmə olan yerlərdə işlənir.

/// Qlobal şüşə rejimi — testlərdə və köhnə cihaz fallback-ində blur söndürülür.
class GlassQuality {
  GlassQuality._();

  /// blur = false → ucuz variant: yarı-şəffaf düz rəng, BackdropFilter yox.
  static bool blur = true;
}

/// Əsas şüşə səthi: blur + işıq qradiyenti + spekulyar kənar.
class GlassSurface extends StatelessWidget {
  final Widget child;
  final BorderRadius? radius;
  final EdgeInsetsGeometry? padding;
  final double sigma;

  /// Ağ işıq qatının gücü (0–1). Apple "Clear/Tinted" rejimlərinin qarşılığı.
  final double luminance;

  final List<BoxShadow>? shadows;

  const GlassSurface({
    super.key,
    required this.child,
    this.radius,
    this.padding,
    this.sigma = 14,
    this.luminance = 0.5,
    this.shadows,
  });

  @override
  Widget build(BuildContext context) {
    final r = radius ?? BorderRadius.circular(18);
    final body = padding == null ? child : Padding(padding: padding!, child: child);

    // Ucuz fallback: blur'suz yarı-şəffaf "şüşə görünüşü".
    if (!GlassQuality.blur) {
      return Container(
        padding: padding,
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: luminance + 0.34),
          borderRadius: r,
          border: Border.all(color: Colors.white.withValues(alpha: 0.6)),
          boxShadow: shadows,
        ),
        child: child,
      );
    }

    final surface = ClipRRect(
      borderRadius: r,
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: sigma, sigmaY: sigma),
        child: DecoratedBox(
          decoration: BoxDecoration(
            // Işık qatı: sol-üstdən sağ-alta — Apple-in "ambient light" oxunuşu.
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Colors.white.withValues(alpha: luminance + 0.10),
                Colors.white.withValues(alpha: luminance - 0.14),
              ],
            ),
            border: Border.all(color: Colors.white.withValues(alpha: 0.55)),
            borderRadius: r,
          ),
          child: body,
        ),
      ),
    );

    if (shadows == null) return surface;
    return Container(
      decoration: BoxDecoration(borderRadius: r, boxShadow: shadows),
      child: surface,
    );
  }
}

/// Şüşə həblər (çip/düymə) — xəritə filtr çipləri və match düymələri üçün.
class GlassPill extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final VoidCallback? onTap;
  final bool dense;

  const GlassPill({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
    this.onTap,
    this.dense = false,
  });

  @override
  Widget build(BuildContext context) {
    final pill = GlassSurface(
      radius: BorderRadius.circular(dense ? 10 : 14),
      sigma: 12,
      luminance: dense ? 0.34 : 0.4,
      padding: padding,
      child: child,
    );
    if (onTap == null) return pill;
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: pill,
    );
  }
}

/// Şüşə üzərində mətn — oxunaqlılıq üçün qalın tünd mətn.
TextStyle glassText({double size = 11, FontWeight w = FontWeight.w700, Color? color}) {
  return TextStyle(
    fontFamily: T.family,
    fontSize: size,
    fontWeight: w,
    color: color ?? Q.foreground,
    height: 1.4,
  );
}
