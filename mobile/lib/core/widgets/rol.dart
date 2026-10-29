import 'package:flutter/material.dart';

import '../models/models.dart';
import '../models/reservation.dart';
import '../theme/app_theme.dart';
import 'motion.dart';

/// ROL UI-2 (v0.3.0) widgetları — rezerv/kupon/match/bildiriş üçün ortaq hissələr.
/// Vizual dil D-023 web stili ilə eynidir (Q tokenləri).

/// Rezerv status çipi — rəng ROL E statusuna görə.
class ResvStatusChip extends StatelessWidget {
  final ResvStatus status;
  const ResvStatusChip({super.key, required this.status});

  (Color, Color) get _colors {
    switch (status) {
      case ResvStatus.pending:
        return (Q.rankBadgeBg, Q.accentFg);
      case ResvStatus.confirmed:
        return (Q.secondary, Q.primary);
      case ResvStatus.used:
        return (Q.muted, Q.mutedFg);
      case ResvStatus.cancelled:
        return (Q.muted, Q.mutedFg);
      case ResvStatus.noShow:
      case ResvStatus.rejected:
        return (const Color(0xFFFDE2E3), Q.destructive);
    }
  }

  @override
  Widget build(BuildContext context) {
    final (bg, fg) = _colors;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(Q.rMd),
      ),
      child: Text(
        resvStatusLabels[status] ?? '',
        style: TextStyle(
            fontFamily: T.family, fontSize: 10.5, fontWeight: FontWeight.w700, color: fg),
      ),
    );
  }
}

/// QR placeholder — koddan deterministik naxış (real QR backend fazasında).
class QrPlaceholder extends StatelessWidget {
  final String code;
  final double size;
  const QrPlaceholder({super.key, required this.code, this.size = 132});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rLg),
      ),
      child: CustomPaint(painter: _QrPainter(code), size: Size.square(size - 16)),
    );
  }
}

class _QrPainter extends CustomPainter {
  final String code;
  _QrPainter(this.code);

  int _cell(int x, int y) {
    // Sadə deterministik hash — eyni kod eyni naxış verir.
    return (code.codeUnitAt((x * 7 + y * 13) % code.length) + x * 31 + y * 17) % 5 == 0
        ? 1
        : 0;
  }

  @override
  void paint(Canvas canvas, Size size) {
    const n = 11.0;
    final cell = size.width / n;
    final paint = Paint()..color = Q.foreground;
    // künc işarələri (QR gözü)
    for (final (ox, oy) in const [(0, 0), (1, 0), (0, 1)]) {
      final bx = ox == 0 ? 0 : n - 3;
      final by = oy == 0 ? 0 : n - 3;
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(bx * cell, by * cell, 3 * cell, 3 * cell),
          const Radius.circular(2),
        ),
        paint,
      );
      final inner = Paint()..color = Colors.white;
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH((bx + 0.6) * cell, (by + 0.6) * cell, 1.8 * cell, 1.8 * cell),
          const Radius.circular(1),
        ),
        inner,
      );
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH((bx + 1.05) * cell, (by + 1.05) * cell, 0.9 * cell, 0.9 * cell),
          const Radius.circular(1),
        ),
        paint,
      );
    }
    for (var x = 0; x < n; x++) {
      for (var y = 0; y < n; y++) {
        final inEye = (x < 4 && y < 4) || (x > n - 5 && y < 4) || (x < 4 && y > n - 5);
        if (inEye) continue;
        if (_cell(x, y) == 1) {
          canvas.drawRRect(
            RRect.fromRectAndRadius(
              Rect.fromLTWH(x * cell + 1, y * cell + 1, cell - 2, cell - 2),
              const Radius.circular(1),
            ),
            paint,
          );
        }
      }
    }
  }

  @override
  bool shouldRepaint(covariant _QrPainter old) => old.code != code;
}

/// Ulduz reytingi (yalnız göstəriş).
class RatingStars extends StatelessWidget {
  final double rating;
  final double size;
  const RatingStars({super.key, required this.rating, this.size = 14});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (var i = 1; i <= 5; i++)
          Icon(
            Icons.star,
            size: size,
            color: i <= rating.round() ? Q.accent : Q.border,
          ),
      ],
    );
  }
}

/// Nişan çipi (ROL F badges) — qazanılıb/yox.
class BadgeChip extends StatelessWidget {
  final BadgeDef badge;
  final bool earned;
  const BadgeChip({super.key, required this.badge, required this.earned});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
      decoration: BoxDecoration(
        color: earned ? Q.secondary : Q.muted,
        borderRadius: BorderRadius.circular(Q.rMd),
        border: Border.all(color: earned ? Q.primary : Q.border, width: 1),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            earned ? Icons.military_tech : Icons.military_tech_outlined,
            size: 15.5,
            color: earned ? Q.primary : Q.mutedFg,
          ),
          const SizedBox(width: 6),
          Text(
            badge.name,
            style: TextStyle(
              fontFamily: T.family,
              fontSize: 11.5,
              fontWeight: FontWeight.w700,
              color: earned ? Q.primary : Q.mutedFg,
            ),
          ),
        ],
      ),
    );
  }
}

/// Məkan mini kartı (Kəşf "Populyar məkanlar" / Xəritə alt kartı).
class VenueMiniCard extends StatelessWidget {
  final String image;
  final String name;
  final String category;
  final String priceRange;
  final double rating;
  final int? discount;
  final VoidCallback? onTap;

  const VenueMiniCard({
    super.key,
    required this.image,
    required this.name,
    required this.category,
    required this.priceRange,
    required this.rating,
    this.discount,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ScaleTap(
      onTap: onTap,
      child: Container(
        width: 174,
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rLg),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              height: 98,
              width: double.infinity,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  Image.asset(image, fit: BoxFit.cover),
                  if (discount != null && discount! > 0)
                    Positioned(
                      top: 8,
                      left: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Q.rankBadgeBg,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          '-$discount%',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10.5,
                              fontWeight: FontWeight.w800,
                              color: Q.accentFg),
                        ),
                      ),
                    ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(11),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 13.5,
                          fontWeight: FontWeight.w800)),
                  const SizedBox(height: 5),
                  Row(
                    children: [
                      Icon(Icons.star, size: 12.5, color: Q.accent),
                      const SizedBox(width: 3),
                      Text('${rating.toStringAsFixed(1)}',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 11.5,
                              fontWeight: FontWeight.w600)),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          '$category · $priceRange',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: T.meta,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Şəxs mini kartı (Kəşf "Sənə uyğun insanlar").
class PersonMiniCard extends StatelessWidget {
  final String image;
  final String name;
  final int age;
  final String distanceLabel;
  final int matchPercent;
  final VoidCallback? onTap;

  const PersonMiniCard({
    super.key,
    required this.image,
    required this.name,
    required this.age,
    required this.distanceLabel,
    required this.matchPercent,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ScaleTap(
      onTap: onTap,
      child: Container(
        width: 132,
        padding: const EdgeInsets.all(11),
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rLg),
        ),
        child: Column(
          children: [
            Stack(
              clipBehavior: Clip.none,
              children: [
                CircleAvatar(
                  backgroundImage: AssetImage(image),
                  radius: 36,
                  backgroundColor: Q.secondary,
                ),
                Positioned(
                  bottom: -4,
                  right: -4,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3.5),
                    decoration: BoxDecoration(
                      color: Q.primary,
                      borderRadius: BorderRadius.circular(5),
                    ),
                    child: Text(
                      '$matchPercent%',
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 10.5,
                          fontWeight: FontWeight.w800,
                          color: Q.primaryFg),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text('$name, $age',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                    fontFamily: T.family, fontSize: 12.5, fontWeight: FontWeight.w800)),
            const SizedBox(height: 3),
            Text(distanceLabel, style: T.meta),
          ],
        ),
      ),
    );
  }
}

/// Təklif kartı (Endirimlər bölməsi).
class OfferCard extends StatelessWidget {
  final String venueName;
  final String label;
  final String requirement; // 'Səviyyə: Aktiv+' / '200 XP'
  final bool autoByLevel;
  final bool unlocked;
  final Widget? action;

  const OfferCard({
    super.key,
    required this.venueName,
    required this.label,
    required this.requirement,
    required this.autoByLevel,
    required this.unlocked,
    this.action,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: unlocked ? Q.border : Q.muted),
        borderRadius: BorderRadius.circular(Q.rLg),
      ),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: unlocked ? Q.rankBadgeBg : Q.muted,
              borderRadius: BorderRadius.circular(Q.rMd),
            ),
            child: Icon(
              autoByLevel ? Icons.workspace_premium : Icons.confirmation_number,
              size: 22,
              color: unlocked ? Q.accentFg : Q.mutedFg,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 6,
                  runSpacing: 3,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text(
                      label,
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 14.5,
                          fontWeight: FontWeight.w800),
                    ),
                    if (autoByLevel)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2.5),
                        decoration: BoxDecoration(
                          color: Q.secondary,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: const Text(
                          'Səviyyən ilə açıq',
                          style: TextStyle(
                              fontFamily: T.family,
                              fontSize: 9.5,
                              fontWeight: FontWeight.w700,
                              color: Q.primary),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  '$venueName · $requirement',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: T.meta,
                ),
              ],
            ),
          ),
          if (action != null) ...[const SizedBox(width: 8), action!],
        ],
      ),
    );
  }
}
