import 'package:flutter/material.dart';

/// Hərəkət sistemi (D-025) — Apple "yumşaldılmış" əyrilərindən ilhamlanmış
/// vahid token dəsti. Bütün ekranlar eyni ritmdə danışır:
///
///   d1  150ms  mikro      — çip seçimi, ikon bounce, toxunuş geri bildirimi
///   d2  250ms  standart   — tab keçidi, konteyner morph, rəng dəyişimi
///   d3  400ms  vurğulu    — sheet, kart girişi, bayram pop
///   d4  550ms  səhnə      — səhifə keçidləri, böyük girişlər
///
/// Əyrilər: `ease` standart hər şey, `enter` girişlər (kiçik overshoot),
/// `spring` vurğulu anlar (MATCH, level-up), `decel` siyahı item-ləri.
/// `disableAnimations` hörmət olunur — bütün vidgetlər 0ms-a düşür.
class Motion {
  Motion._();

  static const d1 = Duration(milliseconds: 150);
  static const d2 = Duration(milliseconds: 250);
  static const d3 = Duration(milliseconds: 400);
  static const d4 = Duration(milliseconds: 550);

  static const Curve ease = Curves.easeOutCubic;
  static const Curve enter = Curves.easeOutBack;
  static const Curve spring = Curves.elasticOut;
  static const Curve decel = Curves.decelerate;

  /// Siyahı stagger addımı — hər item 45ms gecikir.
  static const staggerStep = Duration(milliseconds: 45);

  static bool animationsOn(BuildContext context) =>
      !MediaQuery.disableAnimationsOf(context);
}

/// Staggered giriş — siyahı/seqsion item-ləri üçün fade + 14px sürüşmə.
/// [index] sıraya görə gecikmə verir (max 8 addım — uzun siyahıda da ritm qurur).
class StaggerIn extends StatefulWidget {
  final int index;
  final Widget child;
  final Duration step;

  const StaggerIn({
    super.key,
    required this.index,
    required this.child,
    this.step = Motion.staggerStep,
  });

  @override
  State<StaggerIn> createState() => _StaggerInState();
}

class _StaggerInState extends State<StaggerIn>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: Motion.d3);
  late final Animation<double> _t = CurvedAnimation(parent: _c, curve: Motion.decel);

  @override
  void initState() {
    super.initState();
    final n = widget.index.clamp(0, 8);
    Future.delayed(widget.step * n, () {
      if (mounted) _c.forward();
    });
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!Motion.animationsOn(context)) return widget.child;
    return AnimatedBuilder(
      animation: _t,
      builder: (context, child) => Opacity(
        opacity: _t.value,
        child: Transform.translate(
          offset: Offset(0, 14 * (1 - _t.value)),
          child: child,
        ),
      ),
      child: widget.child,
    );
  }
}

/// Pop-in — badge / bayram / MATCH pəncərəsi üçün bounce miqyası.
class PopIn extends StatefulWidget {
  final Widget child;
  final Duration delay;

  const PopIn({super.key, required this.child, this.delay = Duration.zero});

  @override
  State<PopIn> createState() => _PopInState();
}

class _PopInState extends State<PopIn>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: Motion.d3);
  late final Animation<double> _t = CurvedAnimation(parent: _c, curve: Motion.enter);

  @override
  void initState() {
    super.initState();
    Future.delayed(widget.delay, () {
      if (mounted) _c.forward();
    });
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!Motion.animationsOn(context)) return widget.child;
    return AnimatedBuilder(
      animation: _t,
      builder: (context, child) => Transform.scale(
        scale: 0.6 + 0.4 * _t.value,
        child: Opacity(opacity: _t.value.clamp(0.0, 1.0), child: child),
      ),
      child: widget.child,
    );
  }
}

/// Toxunuş geri bildirimi — basılanda 0.97 miqyas. Bütün kartlar/düymələr üçün.
class ScaleTap extends StatefulWidget {
  final VoidCallback? onTap;
  final Widget child;
  final double pressed;

  const ScaleTap({
    super.key,
    required this.onTap,
    required this.child,
    this.pressed = 0.97,
  });

  @override
  State<ScaleTap> createState() => _ScaleTapState();
}

class _ScaleTapState extends State<ScaleTap> {
  bool _down = false;

  @override
  Widget build(BuildContext context) {
    final animated = Motion.animationsOn(context);
    return GestureDetector(
      onTapDown: widget.onTap == null ? null : (_) => setState(() => _down = true),
      onTapCancel: () => setState(() => _down = false),
      onTapUp: (_) => setState(() => _down = false),
      onTap: widget.onTap,
      behavior: HitTestBehavior.opaque,
      child: animated
          ? AnimatedScale(
              scale: _down ? widget.pressed : 1,
              duration: const Duration(milliseconds: 90),
              child: widget.child,
            )
          : widget.child,
    );
  }
}

/// Rəqəm sayğacı — XP / statistika dəyərləri animasiyalı artır.
class CountUp extends StatefulWidget {
  final int value;
  final TextStyle? style;
  final String prefix;
  final String suffix;

  const CountUp({
    super.key,
    required this.value,
    this.style,
    this.prefix = '',
    this.suffix = '',
  });

  @override
  State<CountUp> createState() => _CountUpState();
}

class _CountUpState extends State<CountUp>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: Motion.d4);
  late int _from = widget.value;

  @override
  void didUpdateWidget(CountUp old) {
    super.didUpdateWidget(old);
    if (old.value != widget.value) {
      _from = old.value;
      _c.forward(from: 0);
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!Motion.animationsOn(context)) {
      return Text('${widget.prefix}${widget.value}${widget.suffix}',
          style: widget.style);
    }
    return AnimatedBuilder(
      animation: _c,
      builder: (context, _) {
        final v = (_from + (widget.value - _from) * Curves.easeOutCubic.transform(_c.value))
            .round();
        return Text('${widget.prefix}$v${widget.suffix}', style: widget.style);
      },
    );
  }
}

/// Yazır göstəricisi — söhbətlərdə 3 nöqtə dalğası.
class TypingDots extends StatefulWidget {
  final Color color;
  final double size;

  const TypingDots({super.key, this.color = const Color(0xFF687280), this.size = 5});

  @override
  State<TypingDots> createState() => _TypingDotsState();
}

class _TypingDotsState extends State<TypingDots>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 1100))
        ..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!Motion.animationsOn(context)) {
      return Row(mainAxisSize: MainAxisSize.min, children: List.generate(3, (_) => dot(0)));
    }
    return AnimatedBuilder(
      animation: _c,
      builder: (context, _) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 0; i < 3; i++) ...[
            if (i > 0) const SizedBox(width: 4),
            dot(_wavePhase(i)),
          ],
        ],
      ),
    );
  }

  /// Nöqtə i üçün dalğa fazası — 0..1 arası dövrən yuxarı sürüşür.
  double _wavePhase(int i) {
    final t = (_c.value - i * 0.18) % 1.0;
    return Curves.easeOutQuad.transform((t + 1.0) % 1.0);
  }

  Widget dot(double t) => Transform.translate(
        offset: Offset(0, -3 * t),
        child: Container(
          width: widget.size,
          height: widget.size,
          decoration: BoxDecoration(
            color: widget.color.withValues(alpha: 0.35 + 0.65 * t),
            shape: BoxShape.circle,
          ),
        ),
      );
}

/// Shimmer — yüklənmə iskeleti üçün işıq zolağı.
class Shimmer extends StatefulWidget {
  final Widget child;

  const Shimmer({super.key, required this.child});

  @override
  State<Shimmer> createState() => _ShimmerState();
}

class _ShimmerState extends State<Shimmer>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 1300))
        ..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!Motion.animationsOn(context)) return widget.child;
    return AnimatedBuilder(
      animation: _c,
      builder: (context, child) => ShaderMask(
        blendMode: BlendMode.srcATop,
        shaderCallback: (bounds) => LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          stops: [0.4 * _c.value, 0.4 * _c.value + 0.3, 0.4 * _c.value + 0.6]
              .map((s) => s.clamp(0.0, 1.0))
              .toList(),
          colors: const [
            Color(0xFFEEF2F5),
            Color(0xFFFAFCFE),
            Color(0xFFEEF2F5),
          ],
        ).createShader(bounds),
        child: child,
      ),
      child: widget.child,
    );
  }
}

/// Iskelet kartı — statikplaceholder blokları.
class SkeletonBox extends StatelessWidget {
  final double width;
  final double height;
  final double radius;

  const SkeletonBox({
    super.key,
    required this.width,
    required this.height,
    this.radius = 6,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: const Color(0xFFEEF2F5),
        borderRadius: BorderRadius.circular(radius),
      ),
    );
  }
}

/// Fade-through — tab/keçid məzmunu üçün Material "fade through" hərəkəti.
class FadeThrough extends StatelessWidget {
  final Widget child;

  const FadeThrough({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: Motion.d2,
      switchInCurve: Motion.ease,
      switchOutCurve: Curves.easeInCubic.flipped,
      transitionBuilder: (child, anim) => FadeTransition(
        opacity: anim,
        child: ScaleTransition(scale: Tween(begin: 0.94, end: 1.0).animate(anim), child: child),
      ),
      child: child,
    );
  }
}

/// Səhifə keçidi — şüşə dili: fade + 18px yuxarı sürüşmə + 0.98→1 miqyas.
/// Bütün `MaterialPageRoute` çağırışlarının əvəzinə keçir (D-025).
class GlassPageRoute<T> extends PageRouteBuilder<T> {
  GlassPageRoute({required WidgetBuilder builder, super.settings})
      : super(
          transitionDuration: Motion.d4,
          reverseTransitionDuration: Motion.d2,
          pageBuilder: (context, _, __) => builder(context),
          transitionsBuilder: (context, anim, _, child) {
            final t = CurvedAnimation(parent: anim, curve: Motion.ease);
            return FadeTransition(
              opacity: Tween(begin: 0.0, end: 1.0).animate(t),
              child: SlideTransition(
                position: Tween(
                  begin: const Offset(0, 0.028),
                  end: Offset.zero,
                ).animate(t),
                child: ScaleTransition(
                  scale: Tween(begin: 0.98, end: 1.0).animate(t),
                  child: child,
                ),
              ),
            );
          },
        );
}
