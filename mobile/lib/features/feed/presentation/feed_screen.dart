import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';
import '../../coupons/presentation/coupons_screen.dart';
import '../../match/presentation/match_screen.dart';
import '../../venues/presentation/venue_detail_screen.dart';

/// Kəşf (ana səhifə) — ROL (product-spec C, TAB 1), v0.6.0 (D-028):
/// Instagram-paradigmalı "Sən üçün" axını — tədbir siyahıları bu tabdan
/// ÇIXARILDI (tədbirlər ana menyunun öz tabındadır, dublikat yoxdur).
/// Qalan: salam + XP + səviyyə, "Bu axşam" keçid kartı, populyar məkanlar,
/// sənə uyğun insanlar, endirimli təkliflər.
///
/// Axtarış (məkan / insan) səhifənin ƏN ÜSTÜNDƏ sabitdir — scroll ilə
/// itmir (D-029: "search bölmələrinin yeri" direktivi, Instagram-vari).
class FeedScreen extends StatefulWidget {
  final AppState state;

  /// "Bu axşam" kartından Tədbirlər ana tabına keçid (shell-dən ötürülür).
  final VoidCallback? onOpenEvents;
  const FeedScreen({super.key, required this.state, this.onOpenEvents});

  @override
  State<FeedScreen> createState() => FeedScreenState();
}

class FeedScreenState extends State<FeedScreen> {
  final searchCtrl = TextEditingController();

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
    final name = state.profileName.split(' ').first;
    final q = searchCtrl.text.toLowerCase();

    bool searchOk(String text) => q.isEmpty || text.toLowerCase().contains(q);

    // D-028: tədbir siyahıları YOXDUR — axtarış məkan + insan üzrə.
    final venues = mockPartners
        .where((v) =>
            v.city == state.city &&
            searchOk('${v.name} ${v.category} ${v.address}'))
        .toList(growable: false);
    final people = state.matchCandidates
        .where((p) => searchOk('${p.firstName} ${p.lastName}'))
        .toList(growable: false);
    final discountOffers = state.visibleOffers
        .where((o) => !o.autoByLevel || o.percent > 0)
        .toList();

    // "Bu axşam" keçid kartı üçün say (ROL TAB 3-ə körpü).
    final tonightCount =
        state.cityEvents.where((e) => e.date == 'Bu gün').length;

    return AnimatedBuilder(
      animation: state,
      builder: (context, _) => Column(
        children: [
          // — sabit axtarış (D-029): scroll ilə itmir, həmişə görünür —
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 6),
            child: SearchBarField(
              controller: searchCtrl,
              hint: 'Məkan və ya insan axtar...',
            ),
          ),
          Expanded(
            child: ListView(
              // alt 128px: məzmun şüşə nav pillin altından axır (extendBody, D-025)
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 128),
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
                const SizedBox(height: 10),
                // — "Bu axşam": Tədbirlər ana tabına körpü (D-028) —
                if (tonightCount > 0)
                  StaggerIn(
                    index: 2,
                    child: CommunityBanner(
                      icon: Icons.nightlight_round,
                      title: 'Bu axşam $tonightCount tədbir var',
                      subtitle:
                          'Görüşlər, idman, sənət — hamısı Tədbirlər tabında.',
                      trailing:
                          Icon(Icons.north_east, size: 20, color: Q.primary),
                      onTap: widget.onOpenEvents,
                    ),
                  ),
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
                            builder: (_) => VenueDetailScreen(
                                state: state, venue: venues[i]))),
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
                  EmptyState(
                    icon: Icons.search,
                    text: q.isEmpty
                        ? 'Yaxınlıqda yeni insan yoxdur.\nGörüşlərə qoşul, tanış ol!'
                        : '"$q" üzrə insan tapılmadı.',
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
                          onTap: () => Navigator.of(context).push(
                              GlassPageRoute(
                                  builder: (_) =>
                                      CouponsScreen(state: state))),
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
                  trailing:
                      Icon(Icons.north_east, size: 20, color: Q.primary),
                  onTap: () => Navigator.of(context).push(GlassPageRoute(
                      builder: (_) => MatchScreen(state: state))),
                ),
                const SafetyNote(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
