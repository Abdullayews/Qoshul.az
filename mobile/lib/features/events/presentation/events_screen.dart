import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../venues/presentation/venue_detail_screen.dart';
import 'event_card.dart';

/// Tədbirlər tabı — ROL (product-spec C, TAB 3):
/// "Yaxınlaşan" / "Mənim tədbirlərim" / "Yaratdığım" siyahıları.
class EventsScreen extends StatefulWidget {
  final AppState state;
  const EventsScreen({super.key, required this.state});

  @override
  State<EventsScreen> createState() => _EventsScreenState();
}

class _EventsScreenState extends State<EventsScreen> {
  int tab = 0;

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final upcoming = state.upcoming.toList(growable: false);
        final mine = state.events
            .where((e) => state.joined.contains(e.id) || state.pending.contains(e.id))
            .toList(growable: false);
        final createdByMe =
            state.events.where((e) => e.host == state.profileName).toList(growable: false);
        final list = tab == 0 ? upcoming : tab == 1 ? mine : createdByMe;

        return ListView(
          padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
          children: [
            const ViewHeading(
              title: 'Tədbirlər',
              subtitle: 'Yaxınlaşan görüşlər, iştirak etdiyin və yaratdıqların.',
            ),
            Segmented(
              labels: const ['Yaxınlaşan', 'Mənim tədbirlərim', 'Yaratdığım'],
              active: tab,
              onChanged: (i) => setState(() => tab = i),
            ),
            if (list.isEmpty)
              EmptyState(
                icon: tab == 2 ? Icons.add_circle_outline : Icons.event,
                text: tab == 0
                    ? 'Hazırda yaxınlaşan tədbir yoxdur.'
                    : tab == 1
                        ? 'Hələ heç bir tədbirə qoşulmamısan.\n"Yaxınlaşan" siyahısından birini seç.'
                        : 'Heç bir tədbir yaratmamısan.\nMərkəzdəki "Yarat" düyməsi ilə başla.',
              )
            else
              for (final e in list) ...[
                EventCard(event: e, state: state),
                const SizedBox(height: 16),
              ],
            if (tab == 0) _venueHint(),
            const SafetyNote(),
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
        Navigator.of(context).push(MaterialPageRoute(
          builder: (_) => VenueDetailScreen(state: s, venue: mockPartners.first),
        ));
      },
    );
  }
}
