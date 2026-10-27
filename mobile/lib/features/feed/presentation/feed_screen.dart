import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../events/presentation/create_event_sheet.dart';
import '../../events/presentation/event_card.dart';

/// Ana səhifə — web FeedScreen portu:
/// salamlaşma + XP, hero şəkli, hekayələr, "Sənin üçün" (AI),
/// cəmiyyət banneri, axtarış + kateqoriyalar + tədbir gridi.
class FeedScreen extends StatefulWidget {
  final AppState state;
  const FeedScreen({super.key, required this.state});

  @override
  State<FeedScreen> createState() => FeedScreenState();
}

class FeedScreenState extends State<FeedScreen> {
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
  void dispose() {
    searchCtrl.dispose();
    super.dispose();
  }

  void openCreate() {
    showCreateEventSheet(context, widget.state);
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    final events = state.cityEvents;
    final name = state.profileName.split(' ').first;

    final filtered = events.where((e) {
      final catOk = category == 'Hamısı' || groupOf(e.category) == category;
      final q = searchCtrl.text.toLowerCase();
      final textOk = q.isEmpty ||
          '${e.title} ${e.location} ${e.category} ${e.city}'
              .toLowerCase()
              .contains(q);
      return catOk && textOk;
    }).toList();

    return AnimatedBuilder(
      animation: state,
      builder: (context, _) => ListView(
        padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
        children: [
          // — salamlaşma —
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Salam, $name!', style: T.h1),
                    const SizedBox(height: 4),
                    const Text('Bu gün yeni bir hekayəyə qoşul.',
                        style: T.subtitle),
                  ],
                ),
              ),
              RankBadge(label: state.rank),
            ],
          ),
          const SizedBox(height: 17),
          XpTrack(xp: state.xp),
          const SizedBox(height: 23),
          // — hero şəkli —
          _hero(events.length),
          // — hekayələr —
          _stories(),
          // — Sənin üçün —
          const SectionHeading(title: 'Sənin üçün', icon: Icons.auto_awesome),
          _recommendations(),
          // — cəmiyyət banneri —
          CommunityBanner(
            icon: Icons.school,
            title: 'Eyni maraqlar, yeni tanışlıqlar',
            subtitle: 'Sevdiyin məkanlarda görüş, əlavə XP qazan.',
            trailing: Icon(Icons.north_east, size: 20, color: Q.primary),
          ),
          // — Yaxınlıqda nə var? —
          SectionHeading(
            title: 'Yaxınlıqda nə var?',
            trailing: SectionLink(text: state.city, icon: Icons.location_on),
          ),
          SearchBarField(
            controller: searchCtrl,
            hint: 'Tədbir, maraq və ya məkan axtar...',
            onFilter: () => setState(() {}),
          ),
          const SizedBox(height: 14),
          SingleChildScrollView(
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
                      onTap: () => setState(() => category = entry.key),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          if (filtered.isEmpty)
            const EmptyState(
              icon: Icons.search,
              text: 'Uyğun tədbir tapılmadı.\nBaşqa bir maraq və ya məkan seç.',
            )
          else
            for (final e in filtered) ...[
              EventCard(event: e, state: state),
              const SizedBox(height: 16),
            ],
          const SafetyNote(),
        ],
      ),
    );
  }

  Widget _hero(int eventCount) {
    return Container(
      height: 212,
      decoration: const BoxDecoration(
        borderRadius: BorderRadius.vertical(top: Radius.circular(0)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset(imgFriends, fit: BoxFit.cover),
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.centerLeft,
                end: Alignment.centerRight,
                stops: const [0, 0.75],
                colors: [
                  Q.overlayDark.withValues(alpha: 0.8),
                  Q.overlayDark.withValues(alpha: 0.2),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LiveBadge(text: '$eventCount tədbir səni gözləyir'),
                const SizedBox(height: 10),
                const Text(
                  'Şəhər səninlə\ndaha maraqlıdır.',
                  style: TextStyle(
                    fontFamily: T.family,
                    fontSize: 27,
                    fontWeight: FontWeight.w800,
                    height: 1.3,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 10),
                TextButton(
                  onPressed: () {},
                  style: TextButton.styleFrom(
                    foregroundColor: Colors.white,
                    padding: EdgeInsets.zero,
                    minimumSize: Size.zero,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: const [
                      Text('Öz insanlarını tap',
                          style: TextStyle(
                              fontFamily: T.family,
                              fontSize: 11,
                              color: Colors.white)),
                      SizedBox(width: 4),
                      Icon(Icons.arrow_forward, size: 15, color: Colors.white),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _stories() {
    return SizedBox(
      height: 76,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _story(
            child: Container(
              width: 49,
              height: 49,
              decoration: BoxDecoration(
                color: Q.secondary,
                shape: BoxShape.circle,
                border: Border.all(
                  color: Q.primary.withValues(alpha: 0.4),
                  width: 1.2,
                ),
              ),
              child: Icon(Icons.add, size: 22, color: Q.primary),
            ),
            label: 'Sənin planın',
            onTap: openCreate,
          ),
          for (final p in mockPeople)
            _story(
              child: Container(
                width: 49,
                height: 49,
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(
                  border: Border.all(color: Q.primary, width: 1.5),
                  shape: BoxShape.circle,
                ),
                child: ClipOval(
                  child: Image.asset(p.image, fit: BoxFit.cover),
                ),
              ),
              label: p.firstName,
              onTap: () {},
            ),
        ],
      ),
    );
  }

  Widget _story({
    required Widget child,
    required String label,
    VoidCallback? onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Padding(
        padding: const EdgeInsets.only(right: 20),
        child: Column(
          children: [
            child,
            const SizedBox(height: 6),
            Text(
              label,
              style: const TextStyle(
                  fontFamily: T.family, fontSize: 9, color: Q.mutedFg),
            ),
          ],
        ),
      ),
    );
  }

  /// "Sənin üçün" — AI tövsiyə bloku (web AiRecommendations).
  Widget _recommendations() {
    final state = widget.state;
    final recs = state.aiRecommendations();
    if (recs.isEmpty) {
      return const EmptyState(
        icon: Icons.auto_awesome,
        text: 'Bu şəhərdə hələ tədbir yoxdur. İlk tədbiri sən yarat!',
      );
    }
    return Column(
      children: [
        AiBox(
          child: Row(
            children: [
              const Expanded(
                child: Text(
                  'AI maraqlarına görə seçdi.',
                  style: TextStyle(fontFamily: T.family, fontSize: 12),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton.icon(
                onPressed: () => setState(() {}),
                style: FilledButton.styleFrom(
                  minimumSize: const Size(0, 30),
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                ),
                icon: const Icon(Icons.auto_awesome, size: 14),
                label: const Text('Yenilə',
                    style: TextStyle(
                        fontFamily: T.family,
                        fontSize: 10,
                        fontWeight: FontWeight.w600)),
              ),
            ],
          ),
        ),
        SizedBox(
          height: 490, // EventCard + AI səbəb yazısı + ehtiyat (başlıq/səbəb max 2 sətir)
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: recs.length,
            separatorBuilder: (_, __) => const SizedBox(width: 14),
            itemBuilder: (context, i) {
              final (e, reason) = recs[i];
              return SizedBox(
                width: 265,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    EventCard(event: e, state: state, recommended: true),
                    if (reason.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 10),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(Icons.auto_awesome,
                                size: 11, color: Q.primary),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                reason,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 8,
                                    color: Q.mutedFg),
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
