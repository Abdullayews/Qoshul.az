import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';
import '../../coupons/presentation/coupons_screen.dart';
import '../../events/presentation/events_screen.dart';
import '../../match/presentation/match_screen.dart';
import '../../venues/presentation/venue_detail_screen.dart';

/// Kəşf (ANA EKRAN) — ROL (product-spec C, TAB 1), v0.7.0 (D-034):
/// istifadəçi direktivi — "tədbirlər tabı ana ekranda olmalıdır".
/// Ana ekranda İKİ daxili tab var: "Sənin üçün" (Instagram-vari axın:
/// salam + XP, məkanlar, insanlar, endirimlər) və "Tədbirlər" (tam
/// tədbir kəşfi: axtarış, kateqoriya çipləri, siyahılar).
///
/// Axtarış (məkan / insan / tədbir) hər görünüşdə səhifənin ƏN ÜSTÜNDƏ
/// sabitdir — scroll ilə itmir (D-029).
class FeedScreen extends StatefulWidget {
  final AppState state;
  const FeedScreen({super.key, required this.state});

  @override
  State<FeedScreen> createState() => FeedScreenState();
}

class FeedScreenState extends State<FeedScreen> {
  /// 0 = "Sənin üçün", 1 = "Tədbirlər" (D-034).
  int view = 0;
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
    return Column(
      children: [
        // — daxili tablar (D-034): Tədbirlər ANA EKRANDA — sabit, pinned —
        _ViewTabs(
          active: view,
          onChanged: (i) => setState(() => view = i),
        ),
        Expanded(
          // Fade-through: görünüş dəyişəndə köhnəsi solub kiçilir.
          child: FadeThrough(
            child: KeyedSubtree(
              key: ValueKey(view),
              child: view == 0
                  ? _ForYouView(
                      state: widget.state,
                      searchCtrl: searchCtrl,
                      onOpenEvents: () => setState(() => view = 1),
                    )
                  : EventsScreen(state: widget.state),
            ),
          ),
        ),
      ],
    );
  }
}

/// Ana ekranın daxili tab zolağı — "Sənin üçün | Tədbirlər".
/// Instagram-vari üst tab paradigması: böyük toxunma sahəsi, aktiv
/// seqment ağ kart + yumşaq kölgə, ikon + mətn birlikdə.
class _ViewTabs extends StatelessWidget {
  final int active;
  final ValueChanged<int> onChanged;

  const _ViewTabs({required this.active, required this.onChanged});

  static const _tabs = [
    (Icons.auto_awesome_outlined, Icons.auto_awesome_rounded, 'Sənin üçün'),
    (Icons.event_outlined, Icons.event_rounded, 'Tədbirlər'),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(20, 10, 20, 6),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Q.muted,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: [
          for (var i = 0; i < _tabs.length; i++)
            Expanded(
              child: GestureDetector(
                onTap: () => onChanged(i),
                behavior: HitTestBehavior.opaque,
                child: AnimatedContainer(
                  duration: Motion.d1,
                  curve: Motion.ease,
                  height: 42,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: i == active ? Q.card : Colors.transparent,
                    borderRadius: BorderRadius.circular(10),
                    boxShadow: i == active ? [Q.softShadow] : null,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        i == active ? _tabs[i].$2 : _tabs[i].$1,
                        size: 17,
                        color: i == active ? Q.primary : Q.mutedFg,
                      ),
                      const SizedBox(width: 7),
                      Text(
                        _tabs[i].$3,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontFamily: T.family,
                          fontSize: 13,
                          fontWeight:
                              i == active ? FontWeight.w800 : FontWeight.w500,
                          color: i == active ? Q.primary : Q.mutedFg,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// "Sənin üçün" görünüşü — Instagram-paradigmalı axın (D-028 dilindən
/// gəlir, v0.7.0-da ana ekranın 0-cı daxili tabı).
class _ForYouView extends StatelessWidget {
  final AppState state;
  final TextEditingController searchCtrl;
  final VoidCallback onOpenEvents;

  const _ForYouView({
    required this.state,
    required this.searchCtrl,
    required this.onOpenEvents,
  });

  @override
  Widget build(BuildContext context) {
    final q = searchCtrl.text.toLowerCase();

    bool searchOk(String text) => q.isEmpty || text.toLowerCase().contains(q);

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

    // "Bu axşam" keçid kartı üçün say (Tədbirlər daxili tabına körpü).
    final tonightCount =
        state.cityEvents.where((e) => e.date == 'Bu gün').length;

    return AnimatedBuilder(
      animation: state,
      builder: (context, _) => Column(
        children: [
          // — sabit axtarış (D-029): scroll ilə itmir, həmişə görünür —
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 6),
            child: SearchBarField(
              controller: searchCtrl,
              hint: 'Məkan və ya insan axtar...',
            ),
          ),
          Expanded(
            child: ListView(
              // alt 132px: məzmun şüşə nav pillin altından axır (extendBody, D-025)
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 132),
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
                            Text('Salam, ${state.profileName.split(' ').first}!',
                                style: T.h1),
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
                // — "Bu axşam": Tədbirlər daxili tabına körpü (D-034) —
                if (tonightCount > 0)
                  StaggerIn(
                    index: 2,
                    child: CommunityBanner(
                      icon: Icons.nightlight_round,
                      title: 'Bu axşam $tonightCount tədbir var',
                      subtitle:
                          'Görüşlər, idman, sənət — hamısı Tədbirlər bölməsində.',
                      trailing:
                          Icon(Icons.north_east, size: 20, color: Q.primary),
                      onTap: onOpenEvents,
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
                    height: 184,
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
                    height: 166,
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
                    height: 96,
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
                              width: 224,
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
                                          size: 15, color: Q.accentFg),
                                      const SizedBox(width: 6),
                                      Expanded(
                                        child: Text(o.label,
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                            style: const TextStyle(
                                                fontFamily: T.family,
                                                fontSize: 14.5,
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
