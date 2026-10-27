import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../events/presentation/create_event_sheet.dart';

/// Tərəfdaşlar — web PartnersScreen portu (AppShell-dən push olunur).
class PartnersScreen extends StatelessWidget {
  final AppState state;
  const PartnersScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Q.background,
      appBar: AppBar(
        backgroundColor: Q.card,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        leading: IconButton(
          icon: const Icon(Icons.chevron_left, size: 26),
          color: Q.primary,
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text('Tərəfdaşlar', style: T.h2.copyWith(fontSize: 18)),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
        children: [
          const ViewHeading(
              title: 'Sevdiyin məkanlar',
              subtitle: 'Görüşlərə ev sahibliyi edən tərəfdaşlarımız.'),
          CommunityBanner(
            icon: Icons.storefront,
            title: 'Öz məkanını əlavə et',
            subtitle: 'Biznes kimi qeydiyyatdan keç, yeni müştərilər qazan.',
            trailing: Icon(Icons.north_east, size: 20, color: Q.primary),
            onTap: () {},
          ),
          const CommunityBanner(
            icon: Icons.handshake,
            title: 'Görüş təşkil et, +150 XP qazan',
            subtitle: 'Tərəfdaş məkanlarda daha çox fürsət.',
          ),
          for (final p in mockPartners) ...[
            _partnerCard(context, p),
            const SizedBox(height: 20),
          ],
        ],
      ),
    );
  }

  Widget _partnerCard(BuildContext context, Partner p) {
    return Container(
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rLg),
        boxShadow: [Q.softShadow],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Image.asset(p.image, height: 200, width: double.infinity, fit: BoxFit.cover),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.category, size: 11, color: Q.primary),
                    const SizedBox(width: 4),
                    Text(p.category,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 9,
                            fontWeight: FontWeight.w700,
                            color: Q.primary)),
                  ],
                ),
                const SizedBox(height: 8),
                Text(p.name, style: T.h2),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.location_on, size: 12, color: Q.mutedFg),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text('${p.address}, ${p.city}', style: T.meta),
                    ),
                  ],
                ),
                if (p.deal != null) ...[
                  const SizedBox(height: 15),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Q.secondary,
                      borderRadius: BorderRadius.circular(5),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(p.deal!,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 11,
                                fontWeight: FontWeight.w800,
                                color: Q.primary)),
                        Text(p.hours,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 11,
                                color: Q.primary)),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: () => showCreateEventSheet(context, state, venue: p),
                    icon: const Icon(Icons.add, size: 16),
                    label: Text('Burada tədbir yarat', style: T.button),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
