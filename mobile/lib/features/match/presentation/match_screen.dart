// ignore_for_file: deprecated_member_use
import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/moderation.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/glass.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/ui.dart';

/// "Tanış ol" — ROL (product-spec D): kart dəsti.
/// Şəkil, ad, yaş, ~məsafə, ortaq maraqlar; "Maraqlıyam" / "Keç".
/// Qarşılıqlı maraq → match + bildiriş + chat + XP (gündə max 3).
///
/// v0.4.0 (D-025): real swipe dəsti — dartılma (drag) kartı çevirir,
/// buraxanda ya uçub gedir (→ swipe), ya yay ilə geri qayıdır.
/// Fəaliyyət düymələri şüşə həblər kimi şəklin ÜSTÜNDƏ üzür.
class MatchScreen extends StatefulWidget {
  final AppState state;
  const MatchScreen({super.key, required this.state});

  @override
  State<MatchScreen> createState() => _MatchScreenState();
}

class _MatchScreenState extends State<MatchScreen> {
  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return Scaffold(
      backgroundColor: Q.background,
      appBar: AppBar(
        backgroundColor: Q.card,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.chevron_left, size: 26),
          color: Q.primary,
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text('Tanış ol', style: T.h2.copyWith(fontSize: 18)),
        actions: [
          IconButton(
            tooltip: 'Təhlükəsizlik məsləhətləri',
            icon: const Icon(Icons.health_and_safety_outlined, size: 20),
            onPressed: () => _safetyDialog(context),
          ),
        ],
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          final candidates = state.matchCandidates;
          if (candidates.isEmpty) {
            return EmptyState(
              icon: Icons.favorite_outline,
              text: state.swipes.isEmpty
                  ? 'Hazırda yeni kart yoxdur.\nYaxınlıqdakı insanlar burada görünəcək.'
                  : 'Bugünlük kartlar bitdi.\nYeni insanlar üçün sabah yenidən bax!',
              action: state.swipes.isNotEmpty
                  ? OutlinedButton.icon(
                      onPressed: () => state.resetSwipes(),
                      icon: const Icon(Icons.refresh, size: 15),
                      label: const Text('Dəsti yenilə (demo)'),
                    )
                  : null,
            );
          }
          final deck = candidates.take(3).toList(growable: false);
          return Column(
            children: [
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      // — arxa kartlar: miqyas + qalxma ilə dəst dərinliyi —
                      for (var i = deck.length - 1; i >= 1; i--)
                        _DeckDepth(
                          depth: i,
                          child: _CardFace(
                            person: deck[i],
                            state: state,
                            showActions: false,
                            onReport: () => _reportSheet(context, state, deck[i]),
                            onBlock: () {
                              state.blockUser(deck[i].id);
                              if (mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                        content: Text(
                                            '${deck[i].fullName} bloklandı — siz bir-birinizi görməzsiniz.')));
                              }
                            },
                          ),
                        ),
                      // — üst kart: dartılabilir, swipe fizikası —
                      _SwipeCard(
                        key: ValueKey(deck.first.id),
                        person: deck.first,
                        state: state,
                        onSwiped: (interested) =>
                            _swipe(context, state, deck.first, interested),
                        onReport: () =>
                            _reportSheet(context, state, deck.first),
                        onBlock: () {
                          state.blockUser(deck.first.id);
                          if (mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                                content: Text(
                                    '${deck.first.fullName} bloklandı — siz bir-birinizi görməzsiniz.')));
                          }
                        },
                      ),
                    ],
                  ),
                ),
              ),
              if (candidates.length > 3)
                Padding(
                  padding: const EdgeInsets.only(top: 4, bottom: 2),
                  child: Text(
                    '...və daha ${candidates.length - 3} insan növbəti kartlarda',
                    textAlign: TextAlign.center,
                    style: T.meta,
                  ),
                ),
              const Padding(
                padding: EdgeInsets.only(bottom: 16),
                child: SafetyNote(),
              ),
            ],
          );
        },
      ),
    );
  }

  void _swipe(BuildContext context, AppState state, Person p, bool interested) {
    final matched = state.swipe(p.id, interested);
    if (matched && mounted) {
      showDialog<void>(
        context: context,
        builder: (d) => AlertDialog(
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(Q.rLg)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Match ürəyi pop + halqa vurğusu — bayram anı (D-025).
              PopIn(
                child: Container(
                  width: 68,
                  height: 68,
                  decoration: BoxDecoration(
                      color: Q.secondary, shape: BoxShape.circle),
                  child: const Icon(Icons.favorite, size: 32, color: Q.primary),
                ),
              ),
              const SizedBox(height: 14),
              const Text('Match!',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 20,
                      fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(
                '${p.fullName} də sənə maraq göstərdi. Söhbəti başla '
                '(+10 XP, gündə max 3).',
                textAlign: TextAlign.center,
                style: const TextStyle(
                    fontFamily: T.family, fontSize: 11, height: 1.7),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(d),
              child: const Text('Davam et'),
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(d);
                Navigator.of(context).pop(); // → çatlara qayıt
              },
              child: const Text('Söhbətə keç'),
            ),
          ],
        ),
      );
    }
  }

  void _safetyDialog(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('İlk görüş məsləhətləri'),
        content: SizedBox(
          width: double.maxFinite,
          child: ListView(
            shrinkWrap: true,
            children: [
              for (final (title, text) in safetyTips)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 12,
                              fontWeight: FontWeight.w700)),
                      const SizedBox(height: 3),
                      Text(text,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 11,
                              height: 1.6,
                              color: Q.mutedFg)),
                    ],
                  ),
                ),
            ],
          ),
        ),
        actions: [
          FilledButton(
              onPressed: () => Navigator.pop(d), child: const Text('Bağla')),
        ],
      ),
    );
  }

  void _reportSheet(BuildContext context, AppState state, Person p) {
    String reason = reportReasons.first;
    showModalBottomSheet<void>(
      context: context,
      builder: (sheetCtx) => StatefulBuilder(
        builder: (sheetCtx, setSheet) => SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Report et',
                    style: TextStyle(
                        fontFamily: T.family,
                        fontSize: 16,
                        fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text('${p.fullName} haqqında bildiriş. Səbəb seç:',
                    style: T.subtitle),
                const SizedBox(height: 12),
                for (final r in reportReasons)
                  RadioListTile<String>(
                    value: r,
                    groupValue: reason,
                    onChanged: (v) => setSheet(() => reason = v!),
                    title: Text(r,
                        style: const TextStyle(
                            fontFamily: T.family, fontSize: 12)),
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                    activeColor: Q.primary,
                  ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () {
                      state.reportUser(p.id, reason);
                      Navigator.pop(sheetCtx);
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                          content: Text(
                              'Report göndərildi. Moderasiya komandası baxacaq.')));
                    },
                    child: const Text('Göndər'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Dartılabilir üst kart — swipe fizikası (D-025).
/// Pan → sürüşmə + fırlanma; burax → ya uçur (110px hədd), ya yay geri.
class _SwipeCard extends StatefulWidget {
  final Person person;
  final AppState state;
  final ValueChanged<bool> onSwiped;
  final VoidCallback onReport;
  final VoidCallback onBlock;

  const _SwipeCard({
    super.key,
    required this.person,
    required this.state,
    required this.onSwiped,
    required this.onReport,
    required this.onBlock,
  });

  @override
  State<_SwipeCard> createState() => _SwipeCardState();
}

class _SwipeCardState extends State<_SwipeCard>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: Motion.d2);

  Offset _drag = Offset.zero;
  double _angle = 0;
  Offset _from = Offset.zero;
  Offset _to = Offset.zero;
  double _angleFrom = 0;
  double _angleTo = 0;
  Curve _curve = Motion.ease;
  VoidCallback? _after;

  static const _threshold = 110.0; // swipe həddi (px)

  @override
  void initState() {
    super.initState();
    _c.addListener(_tick);
    _c.addStatusListener((s) {
      if (s == AnimationStatus.completed) {
        final done = _after;
        _after = null;
        done?.call();
      }
    });
  }

  void _tick() {
    final t = _curve.transform(_c.value);
    setState(() {
      _drag = Offset.lerp(_from, _to, t)!;
      _angle = lerpDouble(_angleFrom, _angleTo, t)!;
    });
  }

  void _animateTo(Offset end, double angleEnd, Curve curve, VoidCallback? after) {
    _from = _drag;
    _to = end;
    _angleFrom = _angle;
    _angleTo = angleEnd;
    _curve = curve;
    _after = after;
    _c.forward(from: 0);
  }

  void _flyOut(bool right) {
    _animateTo(
      Offset((right ? 1 : -1) * 560, -70),
      (right ? 1 : -1) * 0.55,
      Curves.easeOutCubic,
      () => widget.onSwiped(right),
    );
  }

  void _springBack() {
    _animateTo(Offset.zero, 0, Motion.spring, null);
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final dragRight = _drag.dx > 12;
    final dragLeft = _drag.dx < -12;
    final likeOpacity =
        (_drag.dx / _threshold).clamp(0.0, 1.0);
    final passOpacity =
        (-_drag.dx / _threshold).clamp(0.0, 1.0);

    return GestureDetector(
      onPanUpdate: (d) => setState(() {
        _drag += d.delta;
        _angle = (_drag.dx / 22).clamp(-0.5, 0.5);
      }),
      onPanEnd: (_) => _drag.dx.abs() > _threshold
          ? _flyOut(_drag.dx > 0)
          : _springBack(),
      child: Transform.translate(
        offset: _drag,
        child: Transform.rotate(
          angle: _angle,
          child: Stack(
            fit: StackFit.expand,
            children: [
              _CardFace(
                person: widget.person,
                state: widget.state,
                onReport: widget.onReport,
                onBlock: widget.onBlock,
                onPass: () => _flyOut(false),
                onInterested: () => _flyOut(true),
              ),
              // — swipe möhürləri: istiqamət göründürür —
              Positioned(
                top: 34,
                left: 18,
                child: Opacity(
                  opacity: dragRight ? likeOpacity : 0,
                  child: Transform.rotate(
                    angle: -0.18,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        border:
                            Border.all(color: Q.primary, width: 2.5),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text('MARAQLIYAM',
                          style: TextStyle(
                              fontFamily: T.family,
                              fontSize: 13,
                              fontWeight: FontWeight.w800,
                              color: Q.primary)),
                    ),
                  ),
                ),
              ),
              Positioned(
                top: 34,
                right: 18,
                child: Opacity(
                  opacity: dragLeft ? passOpacity : 0,
                  child: Transform.rotate(
                    angle: 0.18,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        border:
                            Border.all(color: Q.destructive, width: 2.5),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text('KEÇ',
                          style: TextStyle(
                              fontFamily: T.family,
                              fontSize: 13,
                              fontWeight: FontWeight.w800,
                              color: Q.destructive)),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Dəst dərinliyi — arxa kartlar kiçilir və yuxarı sürüşür.
class _DeckDepth extends StatelessWidget {
  final int depth;
  final Widget child;

  const _DeckDepth({required this.depth, required this.child});

  @override
  Widget build(BuildContext context) {
    return Transform.translate(
      offset: Offset(0, -12.0 * depth),
      child: Transform.scale(
        scale: 1 - 0.05 * depth,
        child: child,
      ),
    );
  }
}

/// Kart üzü — şəkil + məlumat + şüşə fəaliyyət həbləri (D-025 səth #5).
class _CardFace extends StatelessWidget {
  final Person person;
  final AppState state;
  final VoidCallback onReport;
  final VoidCallback onBlock;
  final VoidCallback? onPass;
  final VoidCallback? onInterested;

  /// Arxa (dərinlik) kartlarda fəaliyyət həbləri gizlidir — yalnız üst kartda.
  final bool showActions;

  const _CardFace({
    required this.person,
    required this.state,
    required this.onReport,
    required this.onBlock,
    this.onPass,
    this.onInterested,
    this.showActions = true,
  });

  @override
  Widget build(BuildContext context) {
    final p = person;
    final shared = p.rolInterests
        .where((i) => state.interests.contains(i))
        .toList(growable: false);
    return Container(
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rXl),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        fit: StackFit.expand,
        children: [
          // — şəkil + kölgə —
          Image.asset(p.image, fit: BoxFit.cover),
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.bottomCenter,
                end: Alignment.topCenter,
                stops: const [0, 0.62],
                colors: [
                  Q.photoShade.withValues(alpha: 0.92),
                  Q.photoShade.withValues(alpha: 0),
                ],
              ),
            ),
          ),
          // — uyğunluq çipi (şüşə, şəkil üzərində) —
          Positioned(
            top: 12,
            right: 12,
            child: GlassPill(
              dense: true,
              padding:
                  const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
              child: Text(
                '${p.match}% uyğunluq',
                style: glassText(size: 9, w: FontWeight.w600, color: Colors.white),
              ),
            ),
          ),
          // — report/blok menyusu —
          Positioned(
            top: 4,
            left: 4,
            child: PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert,
                  size: 18, color: Colors.white),
              onSelected: (v) => v == 'report' ? onReport() : onBlock(),
              itemBuilder: (_) => const [
                PopupMenuItem(
                    value: 'report',
                    child: Text('Report et',
                        style:
                            TextStyle(fontFamily: T.family, fontSize: 12))),
                PopupMenuItem(
                    value: 'block',
                    child: Text('Blokla',
                        style:
                            TextStyle(fontFamily: T.family, fontSize: 12))),
              ],
            ),
          ),
          // — məlumat + şüşə fəaliyyət həbləri —
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(14, 0, 14, 14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '${p.firstName}, ${p.age}',
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 19,
                        fontWeight: FontWeight.w800,
                        color: Colors.white),
                  ),
                  const SizedBox(height: 3),
                  Row(
                    children: [
                      const Icon(Icons.location_on,
                          size: 11, color: Colors.white70),
                      const SizedBox(width: 3),
                      Text(
                        state.showDistance
                            ? '${p.distanceLabel} · ${p.city}'
                            : p.city,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 10,
                            color: Colors.white70),
                      ),
                      const SizedBox(width: 10),
                      Text('${statusLabel(p.status)} · ${p.org}',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10,
                              color: Colors.white70)),
                    ],
                  ),
                  if (p.rolInterests.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        for (final i in p.rolInterests.take(4))
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 9, vertical: 5),
                            decoration: BoxDecoration(
                              color: shared.contains(i)
                                  ? Q.primary.withValues(alpha: 0.32)
                                  : Colors.white.withValues(alpha: 0.14),
                              borderRadius: BorderRadius.circular(5),
                              border: Border.all(
                                  color: shared.contains(i)
                                      ? Q.primary.withValues(alpha: 0.65)
                                      : Q.glassBorder),
                            ),
                            child: Text(
                              i,
                              style: TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 9,
                                  fontWeight: shared.contains(i)
                                      ? FontWeight.w700
                                      : FontWeight.w500,
                                  color: Colors.white),
                            ),
                          ),
                      ],
                    ),
                  ],
                  const SizedBox(height: 6),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(Icons.auto_awesome,
                          size: 12, color: Colors.white70),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(p.reason,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 10,
                                height: 1.5,
                                color: Colors.white70)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // — şüşə fəaliyyət həbləri (yalnız üst kartda; D-025 səth #5) —
                  if (showActions)
                    Row(
                      children: [
                        Expanded(
                          child: ScaleTap(
                            onTap: onPass,
                            child: GlassPill(
                              padding:
                                  const EdgeInsets.symmetric(vertical: 11),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(Icons.close,
                                      size: 16, color: Q.foreground),
                                  const SizedBox(width: 6),
                                  Text('Keç', style: glassText(size: 12)),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          flex: 2,
                          child: ScaleTap(
                            onTap: onInterested,
                            child: GlassPill(
                              padding:
                                  const EdgeInsets.symmetric(vertical: 11),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(Icons.favorite,
                                      size: 16, color: Q.primary),
                                  const SizedBox(width: 6),
                                  Text('Maraqlıyam',
                                      style: glassText(size: 12)),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
