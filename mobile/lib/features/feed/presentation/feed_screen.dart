import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';
import '../../coupons/presentation/coupons_screen.dart';
import '../../events/presentation/create_event_sheet.dart';
import '../../events/presentation/event_card.dart';
import '../../match/presentation/match_screen.dart';
import '../../venues/presentation/venue_detail_screen.dart';

/// Kəşf (ana səhifə) — ROL (product-spec C, TAB 1):
/// salam + XP + səviyyə, kateqoriya çipləri, "Bu axşam", "Yaxınında olan
/// tədbirlər", "Populyar məkanlar", "Sənə uyğun insanlar", "Endirimli
/// təkliflər", axtarış (məkan / tədbir / istifadəçi).
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
  void initState() {
    super.initState();
    searchCtrl.addListener(() => setState(() {}));
  }

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
    final name = state.profileName.split(' ').first;
    final q = searchCtrl.text.toLowerCase();

    bool searchOk(String text) => q.isEmpty || text.toLowerCase().contains(q);

    final events = state.cityEvents.where((e) => searchOk(
        '${e.title} ${e.location} ${e.category} ${e.city}')).toList();
    final venues = mockPartners
        .where((v) => v.city == state.city && searchOk('${v.name} ${v.category}'))
        .toList(growable: false);
    final people = state.matchCandidates
        .where((p) => searchOk('${p.firstName} ${p.lastName}'))
        .toList(growable: false);

    final filtered = events.where((e) {
      return category == 'Hamısı' || groupOf(e.category) == category;
    }).toList();

    final tonight = filtered.where((e) => e.date == 'Bu gün').toList();
    final nearby = [...filtered]
      ..sort((a, b) => a.distance.compareTo(b.distance));
    final discountOffers =
        state.visibleOffers.where((o) => !o.autoByLevel || o.percent > 0).toList();

    return AnimatedBuilder(
      animation: state,
      builder: (context, _) => ListView(
        // alt 128px: məzmun şüşə nav pillin altından axır (extendBody, D-025)
        padding: const EdgeInsets.fromLTRB(20, 21, 20, 128),
        children: [
          // — salamlaşma (ROL TAB 1: salam, XP, səviyyə) — staggered giriş —
          StaggerIn(
            index: 0,
            child: Row(
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
          ),
          const SizedBox(height: 17),
          StaggerIn(
            index: 1,
            child: XpTrack(xp: state.xp),
          ),
          const SizedBox(height: 20),
          // — axtarış (məkan, tədbir, istifadəçi adı) —
          StaggerIn(
            index: 2,
            child: SearchBarField(
              controller: searchCtrl,
              hint: 'Məkan, tədbir və ya insan axtar...',
              onFilter: () => setState(() {}),
            ),
          ),
          const SizedBox(height: 14),
          // — kateqoriya çipləri (maraqlara görə filtr) —
          StaggerIn(
            index: 3,
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
                        onTap: () => setState(() => category = entry.key),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
          // — Bu axşam —
          if (tonight.isNotEmpty) ...[
            SectionHeading(
              title: 'Bu axşam',
              icon: Icons.nightlight_round,
              trailing: SectionLink(
                  text: '${tonight.length} tədbir', icon: Icons.north_east),
            ),
            for (final e in tonight.take(2)) ...[
              EventCard(event: e, state: state),
              const SizedBox(height: 14),
            ],
            const SizedBox(height: 4),
          ],
          // — Yaxınında olan tədbirlər —
          SectionHeading(
            title: 'Yaxınında olan tədbirlər',
            icon: Icons.near_me,
            trailing: SectionLink(text: state.city, icon: Icons.location_on),
          ),
          if (nearby.isEmpty)
            const EmptyState(
              icon: Icons.search,
              text: 'Bu filtrlə uyğun tədbir yoxdur.\nBaşqa maraq seç.',
            )
          else
            for (final e in nearby.take(3)) ...[
              EventCard(event: e, state: state),
              const SizedBox(height: 14),
            ],
          // — Populyar məkanlar —
          if (venues.isNotEmpty) ...[
            SectionHeading(
              title: 'Populyar məkanlar',
              icon: Icons.storefront,
              trailing: SectionLink(text: '${venues.length} məkan'),
            ),
            SizedBox(
              height: 176,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: venues.length,
                separatorBuilder: (_, __) => const SizedBox(width: 12),
                itemBuilder: (context, i) => VenueMiniCard(
                  image: venues[i].image,
                  name: venues[i].name,
                  category: venues[i].category,
                  priceRange: venues[i].priceRange,
                  rating: venues[i].rating,
                  discount: venues[i].discountPercent,
                  onTap: () => Navigator.of(context).push(GlassPageRoute(
                      builder: (_) =>
                          VenueDetailScreen(state: state, venue: venues[i]))),
                ),
              ),
            ),
            const SizedBox(height: 20),
          ],
          // — Sənə uyğun insanlar (match) —
          SectionHeading(
            title: 'Sənə uyğun insanlar',
            icon: Icons.favorite_outline,
            trailing: SectionLink(
                text: 'Hamısına bax',
                icon: Icons.north_east,
                onTap: () => Navigator.of(context).push(GlassPageRoute(
                    builder: (_) => MatchScreen(state: state)))),
          ),
          if (people.isEmpty)
            const EmptyState(
              icon: Icons.favorite_outline,
              text: 'Yaxınlıqda yeni insan yoxdur.\nGörüşlərə qoşul, tanış ol!',
            )
          else
            SizedBox(
              height: 158,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: people.length,
                separatorBuilder: (_, __) => const SizedBox(width: 12),
                itemBuilder: (context, i) => PersonMiniCard(
                  image: people[i].image,
                  name: people[i].firstName,
                  age: people[i].age,
                  distanceLabel: state.showDistance
                      ? people[i].distanceLabel
                      : people[i].city,
                  matchPercent: people[i].match,
                  onTap: () => Navigator.of(context).push(GlassPageRoute(
                      builder: (_) => MatchScreen(state: state))),
                ),
              ),
            ),
          const SizedBox(height: 20),
          // — Endirimli təkliflər —
          SectionHeading(
            title: 'Endirimli təkliflər',
            icon: Icons.percent,
            trailing: SectionLink(
                text: 'Bütün endirimlər',
                icon: Icons.north_east,
                onTap: () => Navigator.of(context).push(GlassPageRoute(
                    builder: (_) => CouponsScreen(state: state)))),
          ),
          if (discountOffers.isEmpty)
            const EmptyState(
                icon: Icons.percent, text: 'Hazırda endirim təklifi yoxdur.')
          else
            SizedBox(
              height: 92,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: discountOffers.length,
                separatorBuilder: (_, __) => const SizedBox(width: 12),
                itemBuilder: (context, i) {
                  final o = discountOffers[i];
                  return ScaleTap(
                    onTap: () => Navigator.of(context).push(GlassPageRoute(
                        builder: (_) => CouponsScreen(state: state))),
                    child: StaggerIn(
                      index: i,
                      child: Container(
                      width: 210,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Q.card,
                        border: Border.all(color: Q.border),
                        borderRadius: BorderRadius.circular(Q.rLg),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.percent,
                                  size: 14, color: Q.accentFg),
                              const SizedBox(width: 6),
                              Expanded(
                                child: Text(o.label,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                        fontFamily: T.family,
                                        fontSize: 13,
                                        fontWeight: FontWeight.w800)),
                              ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(
                              o.autoByLevel
                                  ? '${o.venueName} · səviyyən ilə açıq'
                                  : '${o.venueName} · ${o.xpPrice} XP',
                              style: T.meta),
                        ],
                      ),
                    ),
                  ),
              );
                },
              ),
            ),
          const SizedBox(height: 16),
          // — cəmiyyət banneri —
          CommunityBanner(
            icon: Icons.school,
            title: 'Eyni maraqlar, yeni tanışlıqlar',
            subtitle: 'Sevdiyin məkanlarda görüş, əlavə XP qazan.',
            trailing: Icon(Icons.north_east, size: 20, color: Q.primary),
            onTap: () => Navigator.of(context).push(GlassPageRoute(
                builder: (_) => MatchScreen(state: state))),
          ),
          // — bütün tədbirlər —
          SectionHeading(
            title: 'Bütün tədbirlər',
            icon: Icons.event,
            trailing: SectionLink(
                text: 'Tədbirlər tabı',
                icon: Icons.north_east,
                onTap: () => setState(() {})),
          ),
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
}
