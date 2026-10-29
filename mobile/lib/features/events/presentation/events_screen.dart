import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/ui.dart';
import '../../venues/presentation/venue_detail_screen.dart';
import 'event_card.dart';

/// Tədbirlər görünüşü — ROL (product-spec C), v0.7.0 (D-034): Kəşf
/// (ana ekran) içindəki "Tədbirlər" daxili tabının məzmunu — sabit
/// axtarış (scroll ilə itmir), kateqoriya çipləri və "Yaxınlaşan /
/// Mənim tədbirlərim / Yaratdığım" siyahıları.
///
/// D-026 (L.1) — "Sənin səviyyənə görə": səviyyən yüksəldikcə peşəkar
/// aktiviteler üstdə önə düşür.
class EventsScreen extends StatefulWidget {
  final AppState state;
  const EventsScreen({super.key, required this.state});

  @override
  State<EventsScreen> createState() => _EventsScreenState();
}

class _EventsScreenState extends State<EventsScreen> {
  int tab = 0;
  final searchCtrl = TextEditingController();
  String category = 'Hamısı';

  static const _groupIcons = <String, IconData>{
    'Hamısı': Icons.explore,
    'İdman': Icons.sports_volleyball,
    'Təbiət': Icons.terrain,
    'Mədəniyyət': Icons.account_balance,
    'Musiqi': Icons.music_note,
    'Yemək və içki': Icons.local_cafe,
    'Təhsil': Icons.menu_book,
    'Texnologiya': Icons.memory,
    'Biznes və karyera': Icons.business_center,
    'Oyunlar': Icons.casino,
    'İncəsənət': Icons.palette,
    'Könüllülük': Icons.volunteer_activism,
    'Səyahət və digər': Icons.flight,
  };

  @override
  void initState() {
    super.initState();
    searchCtrl.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final q = searchCtrl.text.toLowerCase();
        bool searchOk(String text) =>
            q.isEmpty || text.toLowerCase().contains(q);

        final upcoming = state.upcoming
            .where((e) =>
                searchOk('${e.title} ${e.location} ${e.category} ${e.city}'))
            .toList(growable: false);
        final mine = state.events
            .where((e) =>
                (state.joined.contains(e.id) ||
                    state.pending.contains(e.id)) &&
                searchOk('${e.title} ${e.location}'))
            .toList(growable: false);
        final createdByMe = state.events
            .where((e) =>
                e.host == state.profileName &&
                searchOk('${e.title} ${e.location}'))
            .toList(growable: false);
        final base = tab == 0 ? upcoming : tab == 1 ? mine : createdByMe;
        final list = base
            .where((e) =>
                category == 'Hamısı' || groupOf(e.category) == category)
            .toList(growable: false);

        // D-026 (L.1): səviyyə çəkisi ilə sıralanan peşəkar önəçəkilmə.
        final proRanked = [...list]..sort((a, b) =>
            XpRules.proWeight(b.level) * (state.rankIndex + 1) -
            XpRules.proWeight(a.level) * (state.rankIndex + 1));

        return Column(
          children: [
            // — sabit axtarış (D-029): tədbir axtarışı həmişə görünür —
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 4, 20, 6),
              child: SearchBarField(
                controller: searchCtrl,
                hint: 'Tədbir axtar...',
              ),
            ),
            Expanded(
              child: ListView(
                // alt 132px: şüşə nav pill altından axış (extendBody, D-025)
                padding: const EdgeInsets.fromLTRB(20, 4, 20, 132),
                children: [
                  // — kateqoriya çipləri —
                  StaggerIn(
                    index: 0,
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          for (final entry in _groupIcons.entries)
                            Padding(
                              padding: const EdgeInsets.only(right: 7),
                              child: CategoryChip(
                                label: entry.key,
                                icon: entry.value,
                                active: category == entry.key,
                                onTap: () =>
                                    setState(() => category = entry.key),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                  StaggerIn(
                    index: 1,
                    child: Segmented(
                      labels: const [
                        'Yaxınlaşan',
                        'Mənim tədbirlərim',
                        'Yaratdığım'
                      ],
                      active: tab,
                      onChanged: (i) => setState(() => tab = i),
                    ),
                  ),
                  // — "Sənin səviyyənə görə" (D-026 L.1 — burada, v0.6.0) —
                  if (tab == 0 && proRanked.isNotEmpty) ...[
                    SectionHeading(
                      title: 'Sənin səviyyənə görə',
                      icon: Icons.military_tech,
                      trailing: SectionLink(
                          text: state.rank,
                          icon: Icons.workspace_premium),
                    ),
                    for (final e in proRanked.take(2)) ...[
                      EventCard(event: e, state: state, showLevel: true),
                      const SizedBox(height: 14),
                    ],
                    const SizedBox(height: 6),
                  ],
                  // Tab dəyişəndə siyahı fade-through ilə yenilənir (D-025).
                  FadeThrough(
                    child: KeyedSubtree(
                      key: ValueKey(tab),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (list.isEmpty)
                            EmptyState(
                              icon: tab == 2
                                  ? Icons.add_circle_outline
                                  : Icons.event,
                              text: tab == 0
                                  ? (q.isEmpty
                                      ? 'Hazırda yaxınlaşan tədbir yoxdur.'
                                      : '"$q" üzrə tədbir tapılmadı.')
                                  : tab == 1
                                      ? 'Hələ heç bir tədbirə qoşulmamısan.\n"Yaxınlaşan" siyahısından birini seç.'
                                      : 'Heç bir tədbir yaratmamısan.\nMərkəzdəki "Yarat" düyməsi ilə başla.',
                            )
                          else
                            for (final e in list) ...[
                              EventCard(event: e, state: state),
                              const SizedBox(height: 16),
                            ],
                        ],
                      ),
                    ),
                  ),
                  if (tab == 0) _venueHint(),
                  const SafetyNote(),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  /// Tədbirsiz günlər üçün: partnyor məkanlarda stol rezerv xatırlatması (ROL E).
  Widget _venueHint() {
    return CommunityBanner(
      icon: Icons.table_restaurant,
      title: 'Tədbir yoxdur? Məkan rezerv et',
      subtitle: 'Partnyor məkanlarda stol ayır, QR al və iştirakla XP qazan.',
      trailing: Icon(Icons.north_east, size: 20, color: Q.primary),
      onTap: () {
        final s = widget.state;
        Navigator.of(context).push(GlassPageRoute(
          builder: (_) => VenueDetailScreen(state: s, venue: mockPartners.first),
        ));
      },
    );
  }
}
