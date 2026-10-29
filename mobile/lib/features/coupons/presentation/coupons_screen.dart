import 'package:flutter/material.dart';

import '../../../core/models/coupon.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';

/// Endirimlər və kuponlar — ROL (product-spec G):
/// a) səviyyəyə görə avtomatik açılan; b) XP ilə "al".
/// XP balansı və ümumi XP ayrı saxlanılır (balans xərclənir, səviyyə düşmür).
class CouponsScreen extends StatefulWidget {
  final AppState state;
  const CouponsScreen({super.key, required this.state});

  @override
  State<CouponsScreen> createState() => _CouponsScreenState();
}

class _CouponsScreenState extends State<CouponsScreen> {
  int tab = 0;

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
        title: Text('Endirimlər', style: T.h2.copyWith(fontSize: 18)),
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: Q.secondary,
                borderRadius: BorderRadius.circular(Q.rLg),
              ),
              child: Row(
                children: [
                  const Icon(Icons.toll, size: 18, color: Q.primary),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Kuponlar obyektin Endirim XP balansından alınır — D-026 ayrı baza qaydası (Xankəndi XP-si yalnız Xankəndidə işləyir)',
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 11,
                          height: 1.5,
                          fontWeight: FontWeight.w600,
                          color: Q.primary),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
            Segmented(
              labels: const ['Endirimlər', 'Kuponlarım'],
              active: tab,
              onChanged: (i) => setState(() => tab = i),
            ),
            if (tab == 0) _offers(context, state) else _myCoupons(state),
            const SizedBox(height: 10),
            Text(
              'Vaxtı bitən istifadə olunmamış kupon üçün XP geri qaytarılmır.',
              textAlign: TextAlign.center,
              style: T.meta,
            ),
          ],
        ),
      ),
    );
  }

  Widget _offers(BuildContext context, AppState state) {
    final visible = state.visibleOffers;
    if (visible.isEmpty) {
      return const EmptyState(
          icon: Icons.percent, text: 'Hazırda təklif yoxdur.');
    }
    return Column(
      children: [
        for (final o in visible)
          // Təklif kartları sırayla girir (D-025).
          StaggerIn(
            index: visible.indexOf(o),
            child: OfferCard(
            venueName: o.venueName,
            label: o.label,
            requirement: o.autoByLevel
                ? 'Səviyyə: ${XpRules.rankNames[o.minRankIndex]}+ (avtomatik aktiv)'
                : '${o.xpPrice} XP · balansın: ${state.venueXp[o.venueId]?.balance ?? 0} XP',
            autoByLevel: o.autoByLevel,
            unlocked: o.unlockedFor(state.rankIndex),
            action: o.autoByLevel
                ? (o.unlockedFor(state.rankIndex)
                    ? const Icon(Icons.check_circle, size: 20, color: Q.primary)
                    : null)
                : FilledButton(
                    onPressed: (o.unlockedFor(state.rankIndex) &&
                            (state.venueXp[o.venueId]?.balance ?? 0) >= o.xpPrice)
                        ? () => _buy(context, state, o)
                        : null,
                    style: FilledButton.styleFrom(
                        minimumSize: const Size(0, 32),
                        padding: const EdgeInsets.symmetric(horizontal: 12)),
                    child: const Text('XP ilə al', style: T.button),
                  ),
            ),
          ),
      ],
    );
  }

  void _buy(BuildContext context, AppState state, CouponOffer offer) {
    final c = state.buyCoupon(offer.id);
    if (c == null) return;
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(
                'Kupon alındı: ${c.label} (${c.code}) — ${offer.validityDays} gün etibarlıdır')),
      );
    }
  }

  Widget _myCoupons(AppState state) {
    if (state.coupons.isEmpty) {
      return const EmptyState(
        icon: Icons.confirmation_number,
        text: 'Hələ kuponun yoxdur.\n"Endirimlər" siyahısından XP ilə al.',
      );
    }
    return Column(
      children: [
        for (final c in state.coupons)
          Container(
            margin: const EdgeInsets.only(bottom: 12),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Q.card,
              border: Border.all(
                  color: c.isActive ? Q.primary : Q.border,
                  width: c.isActive ? 1.2 : 1),
              borderRadius: BorderRadius.circular(Q.rLg),
            ),
            child: Row(
              children: [
                Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: c.isActive ? Q.rankBadgeBg : Q.muted,
                    borderRadius: BorderRadius.circular(Q.rMd),
                  ),
                  child: Icon(
                    c.isActive ? Icons.confirmation_number : Icons.block,
                    size: 21,
                    color: c.isActive ? Q.accentFg : Q.mutedFg,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(c.label,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 13,
                              fontWeight: FontWeight.w800)),
                      const SizedBox(height: 4),
                      Text(
                          '${c.venueName} · ${c.code}\n'
                          '${c.statusLabel}'
                          '${c.isActive ? ' · ${c.daysLeft} gün qalıb' : ''}',
                          style: T.meta),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                TextButton(
                  onPressed: c.isActive
                      ? () => showDialog<void>(
                          context: context,
                          builder: (d) => AlertDialog(
                            content: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(c.venueName,
                                    style: const TextStyle(
                                        fontFamily: T.family,
                                        fontSize: 14,
                                        fontWeight: FontWeight.w800)),
                                const SizedBox(height: 10),
                                QrPlaceholder(code: c.code),
                                const SizedBox(height: 8),
                                Text(c.code, style: T.meta),
                              ],
                            ),
                            actions: [
                              FilledButton(
                                  onPressed: () => Navigator.pop(d),
                                  child: const Text('Bağla')),
                            ],
                          ),
                        )
                      : null,
                  child: const Text('QR göstər'),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
