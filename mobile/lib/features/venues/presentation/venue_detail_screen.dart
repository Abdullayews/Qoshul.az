import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';
import 'reserve_sheet.dart';

/// Məkan detal ekranı — ROL (product-spec E):
/// şəkillər, ünvan, iş saatları, kateqoriya, qiymət aralığı, rəylər,
/// aktiv endirim, "Rezerv et" və "Yol tap" düymələri.
class VenueDetailScreen extends StatelessWidget {
  final AppState state;
  final Partner venue;
  const VenueDetailScreen({super.key, required this.state, required this.venue});

  @override
  Widget build(BuildContext context) {
    final v = venue;
    final reviews = mockVenueReviews[v.id] ?? const <VenueReview>[];
    final hour = DateTime.now().hour;
    final openNow = v.isOpenAt(hour);

    return Scaffold(
      backgroundColor: Q.background,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 230,
            pinned: true,
            backgroundColor: Q.card,
            surfaceTintColor: Colors.transparent,
            leading: Padding(
              padding: const EdgeInsets.all(6),
              child: CircleAvatar(
                backgroundColor: Q.glass,
                child: IconButton(
                  icon: const Icon(Icons.chevron_left, size: 24),
                  color: Colors.white,
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ),
            ),
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                fit: StackFit.expand,
                children: [
                  Image.asset(v.image, fit: BoxFit.cover),
                  DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.bottomCenter,
                        end: Alignment.topCenter,
                        stops: const [0, 0.6],
                        colors: [
                          Q.photoShade.withValues(alpha: 0.6),
                          Q.photoShade.withValues(alpha: 0),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: ListView(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(v.name, style: T.h1.copyWith(fontSize: 22)),
                    ),
                    if (v.hasDiscount)
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 7),
                        decoration: BoxDecoration(
                          color: Q.rankBadgeBg,
                          borderRadius: BorderRadius.circular(Q.rMd),
                        ),
                        child: Text(
                          '-${v.discountPercent}% endirim',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 11,
                              fontWeight: FontWeight.w800,
                              color: Q.accentFg),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    RatingStars(rating: v.rating, size: 15),
                    const SizedBox(width: 6),
                    Text(
                      '${v.rating.toStringAsFixed(1)} (${v.reviewsCount} rəy)',
                      style: T.meta,
                    ),
                    const SizedBox(width: 10),
                    Text('${v.category} · ${v.priceRange}', style: T.meta),
                  ],
                ),
                const SizedBox(height: 14),
                _infoCard(context, v, openNow),
                const SizedBox(height: 14),
                if (v.deal != null)
                  CommunityBanner(
                    icon: Icons.percent,
                    title: 'Aktiv təklif: ${v.deal}',
                    subtitle: v.autoAccept
                        ? 'Rezerv avtomatik təsdiqlənir.'
                        : 'Rezerv partnyorun təsdiqini gözləyir (GÖZLƏYİR).',
                  ),
                const SectionHeading(title: 'Rəylər', icon: Icons.reviews),
                if (reviews.isEmpty)
                  const EmptyState(
                      icon: Icons.reviews, text: 'Hələ rəy yazılmayıb.')
                else
                  for (final r in reviews)
                    Container(
                      margin: const EdgeInsets.only(bottom: 10),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Q.card,
                        border: Border.all(color: Q.border),
                        borderRadius: BorderRadius.circular(Q.rLg),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Text(r.author,
                                  style: const TextStyle(
                                      fontFamily: T.family,
                                      fontSize: 12,
                                      fontWeight: FontWeight.w700)),
                              const Spacer(),
                              RatingStars(rating: r.stars.toDouble(), size: 11),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(r.text,
                              style: const TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 11,
                                  height: 1.6,
                                  color: Q.mutedFg)),
                        ],
                      ),
                    ),
                const SizedBox(height: 6),
                // — sabit olmayan alt düymələr —
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                                content: Text(
                                    'Yol tapmaq xəritə backend mərhələsində açılacaq.')),
                          );
                        },
                        icon: const Icon(Icons.near_me, size: 16),
                        label: Text('Yol tap', style: T.button),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      flex: 2,
                      child: FilledButton.icon(
                        onPressed: () => showReserveSheet(context, state, v),
                        icon: const Icon(Icons.table_restaurant, size: 16),
                        label: Text('Rezerv et', style: T.button),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Text(
                  'Onlayn ödəniş yoxdur — rezerv yalnız yer ayırmadır. '
                  '(Depozit məsələsi təsdiq gözləyir)',
                  textAlign: TextAlign.center,
                  style: T.meta,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _infoCard(BuildContext context, Partner v, bool openNow) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rLg),
      ),
      child: Column(
        children: [
          _row(Icons.location_on, '${v.address}, ${v.city}'),
          const Divider(height: 14, color: Q.border),
          _row(Icons.schedule, '${v.hours} · ${openNow ? 'İndi açıqdır' : 'Bağlıdır'}',
              valueColor: openNow ? Q.primary : Q.mutedFg),
          const Divider(height: 14, color: Q.border),
          _row(Icons.payments, 'Qiymət aralığı: ${v.priceRange}'),
          const Divider(height: 14, color: Q.border),
          _row(
            Icons.workspace_premium,
            'Rezerv: ${v.autoAccept ? 'avtomatik təsdiq' : 'partnyor təsdiqi ilə'}',
          ),
        ],
      ),
    );
  }

  Widget _row(IconData icon, String text, {Color? valueColor}) {
    return Row(
      children: [
        Icon(icon, size: 15, color: Q.mutedFg),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            text,
            style: TextStyle(
                fontFamily: T.family,
                fontSize: 12,
                color: valueColor ?? Q.foreground),
          ),
        ),
      ],
    );
  }
}
