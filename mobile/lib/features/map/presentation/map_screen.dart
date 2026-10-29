import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/models/xp_economy.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/glass.dart';
import '../../../core/widgets/motion.dart';
import '../../events/presentation/event_detail_sheet.dart';
import '../../match/presentation/match_screen.dart';
import '../../venues/presentation/reserve_sheet.dart';
import '../../venues/presentation/venue_detail_screen.dart';
import 'tile_map.dart';

/// Xəritə tabı — ROL (product-spec C, TAB 2).
/// v0.6.0 (D-030): REAL xəritə — OpenStreetMap raster tile-ları (öz
/// renderer, sıfır xarici asılılıq). Pin-lər real lat/lng koordinatlarında:
/// məkanlar, tədbirlər, (razılıq verən) insanlar + "mənim yerim".
///
/// v0.4.0 (D-025): filtr paneli və alt kart Liquid Glass — xəritənin
/// üzərində üzən funksional qat (Apple HIG). Pin-lər staggered drop,
/// kart seçiləndə slide-up, GlassPageRoute keçidləri.
class MapScreen extends StatefulWidget {
  final AppState state;
  const MapScreen({super.key, required this.state});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  String category = 'Hamısı';
  int distanceKm = 10;
  bool openNow = false;
  bool withDiscount = false;
  _MapPin? selected;

  final TileMapController _mapCtrl = TileMapController();

  // Bakı mərkəzi — mock istifadəçi mövqeyi (m5+ GPS).
  static const _myLat = 40.4093;
  static const _myLng = 49.8671;

  static const _categoryChips = <String, IconData>{
    'Hamısı': Icons.public,
    'İdman': Icons.sports_volleyball,
    'Qəhvə': Icons.local_cafe,
    'Texnologiya': Icons.memory,
    'Piknik': Icons.park_outlined,
  };

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    final hour = DateTime.now().hour;

    // — filtrlənmiş məkan pinləri —
    final venuePins = <_MapPin>[
      for (final v in mockPartners)
        if (v.city == state.city &&
            (category == 'Hamısı' || v.category == category) &&
            (!openNow || v.isOpenAt(hour)) &&
            (!withDiscount || v.hasDiscount))
          _MapPin.venue(v),
    ];
    // — tədbir pinləri (yaxınlaşan, məsafə filtri) —
    final eventPins = <_MapPin>[
      for (final e in state.cityEvents)
        if (category == 'Hamısı' || e.category == category)
          if (e.distance <= distanceKm) _MapPin.event(e),
    ];
    // — insan pinləri (ROL: yalnız razılıq verənlər; bloklananlar gizli) —
    // D-027: görünüş 3 seçimlidir — Hamı (bütün razılıq verənlər),
    // Yalnız dostlar (yalnız qəbul olunmuş dostların yeri görünür),
    // Gizli (insan pinləri tam gizli).
    final peoplePins = <_MapPin>[
      for (final p in state.matchCandidates)
        if (p.distanceKm <= distanceKm)
          if (state.mapVisibility == MapVisibility.everyone ||
              (state.mapVisibility == MapVisibility.friends &&
                  state.friendships[p.id] == 'accepted'))
            _MapPin.person(p),
    ];
    final pins = [...venuePins, ...eventPins, ...peoplePins];

    // Xəritə tam sahəni doldurur; filtr/və kart üzərində şüşə qat kimi üzür.
    return Stack(
      clipBehavior: Clip.hardEdge,
      children: [
        // — REAL xəritə (OSM tile-lar, D-030) —
        Positioned.fill(
          child: TileMapView(
            controller: _mapCtrl,
            initLat: _myLat,
            initLng: _myLng,
            initZoom: 13,
            markers: [
              TileMarker(
                lat: _myLat,
                lng: _myLng,
                builder: (_) => const _MyLocationDot(),
              ),
              for (var i = 0; i < pins.length; i++)
                TileMarker(
                  lat: pins[i].lat,
                  lng: pins[i].lng,
                  onTap: () => setState(() => selected = pins[i]),
                  // Pin-lər xəritəyə damla kimi düşür — staggered pop.
                  builder: (_) => PopIn(
                    delay: Motion.staggerStep * (i % 12),
                    child: _pinWidget(pins[i], selected == pins[i]),
                  ),
                ),
            ],
            onTap: () => setState(() => selected = null),
          ),
        ),
        // — şüşə filtr paneli (D-025 səth #2) —
        Positioned(
          top: 8,
          left: 12,
          right: 12,
          child: SafeArea(
            top: false,
            bottom: false,
            child: GlassSurface(
              radius: BorderRadius.circular(16),
              sigma: 16,
              luminance: 0.5,
              padding: const EdgeInsets.fromLTRB(10, 10, 10, 10),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        for (final e in _categoryChips.entries)
                          Padding(
                            padding: const EdgeInsets.only(right: 7),
                            child: _GlassFilterChip(
                              label: e.key,
                              icon: e.value,
                              active: category == e.key,
                              onTap: () => setState(() => category = e.key),
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        for (final km in const [1, 3, 5, 10])
                          Padding(
                            padding: const EdgeInsets.only(right: 7),
                            child: _GlassFilterChip(
                              label: '$km km',
                              icon: Icons.near_me,
                              active: distanceKm == km,
                              onTap: () => setState(() => distanceKm = km),
                            ),
                          ),
                        const SizedBox(width: 6),
                        _GlassFilterChip(
                          label: 'indi açıqdır',
                          icon: Icons.schedule,
                          active: openNow,
                          onTap: () => setState(() => openNow = !openNow),
                        ),
                        const SizedBox(width: 7),
                        _GlassFilterChip(
                          label: 'endirimi var',
                          icon: Icons.percent,
                          active: withDiscount,
                          onTap: () =>
                              setState(() => withDiscount = !withDiscount),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        // — zoom idarəsi (şüşə həblər, D-030) —
        Positioned(
          top: 170,
          right: 12,
          child: Column(
            children: [
              GlassPill(
                onTap: _mapCtrl.zoomIn,
                dense: true,
                padding: const EdgeInsets.all(7),
                child: const Icon(Icons.add, size: 16, color: Q.foreground),
              ),
              const SizedBox(height: 8),
              GlassPill(
                onTap: _mapCtrl.zoomOut,
                dense: true,
                padding: const EdgeInsets.all(7),
                child: const Icon(Icons.remove, size: 16, color: Q.foreground),
              ),
            ],
          ),
        ),
        // — sayğac (şüşə həb) —
        Positioned(
          bottom: 12,
          left: 12,
          child: GlassPill(
            dense: true,
            padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
            child: Text(
              '${venuePins.length} məkan · ${eventPins.length} tədbir · '
              '${peoplePins.length} insan',
              style: glassText(size: 10, w: FontWeight.w600, color: Q.foreground),
            ),
          ),
        ),
        // — D-027: görünüş rejimi göstəricisi (şüşə həb) —
        if (state.mapVisibility != MapVisibility.everyone)
          Positioned(
            bottom: 12,
            right: 12,
            child: GlassPill(
              dense: true,
              padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    state.mapVisibility == MapVisibility.friends
                        ? Icons.group
                        : Icons.visibility_off,
                    size: 12,
                    color: Q.primary,
                  ),
                  const SizedBox(width: 5),
                  Text(
                    state.mapVisibility == MapVisibility.friends
                        ? 'Yalnız dostlar görünür'
                        : 'Gizli rejim',
                    style: glassText(
                        size: 10, w: FontWeight.w600, color: Q.foreground),
                  ),
                ],
              ),
            ),
          ),
        // — seçilmiş pinin şüşə alt kartı (D-025 səth #3) —
        if (selected != null)
          Align(
            alignment: Alignment.bottomCenter,
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                child: _SlideUp(child: _bottomCard(selected!)),
              ),
            ),
          ),
      ],
    );
  }

  Widget _pinWidget(_MapPin pin, bool active) {
    final isVenue = pin.type == _PinType.venue;
    final isEvent = pin.type == _PinType.event;
    final color = isVenue
        ? Q.primary
        : isEvent
            ? Q.accent
            : Q.foreground;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        AnimatedContainer(
          duration: Motion.d1,
          curve: Motion.enter,
          width: active ? 40 : 34,
          height: active ? 40 : 34,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 2.5),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.35),
                blurRadius: 8,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          child: Center(
            child: isVenue
                ? const Icon(Icons.storefront, size: 16, color: Colors.white)
                : isEvent
                    ? const Icon(Icons.event, size: 17, color: Colors.white)
                    : Text(
                        pin.person!.firstName[0],
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 13,
                            fontWeight: FontWeight.w800,
                            color: Colors.white),
                      ),
          ),
        ),
        AnimatedContainer(
          duration: Motion.d1,
          width: 2.5,
          height: active ? 14 : 11,
          color: color,
        ),
      ],
    );
  }

  /// Pin-ə toxunanda alt kart (ROL TAB 2): ad, qiymət aralığı, endirim,
  /// "Ətraflı" və "Rezerv et" düymələri — şüşə üzərində.
  Widget _bottomCard(_MapPin pin) {
    final state = widget.state;
    Widget card;
    if (pin.type == _PinType.venue) {
      final v = pin.venue!;
      card = GlassSurface(
        radius: BorderRadius.circular(Q.rXl),
        sigma: 18,
        luminance: 0.58,
        padding: const EdgeInsets.all(14),
        shadows: const [
          BoxShadow(
            color: Color(0x1E28301F),
            blurRadius: 26,
            offset: Offset(0, 8),
          ),
        ],
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(Q.rMd),
                  child: Image.asset(v.image,
                      width: 56, height: 56, fit: BoxFit.cover),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(v.name, style: glassText(size: 14)),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Icon(Icons.star, size: 12, color: Q.accent),
                          const SizedBox(width: 3),
                          Text('${v.rating.toStringAsFixed(1)} · ${v.priceRange}',
                              style: T.meta),
                          if (v.hasDiscount) ...[
                            const SizedBox(width: 6),
                            Text('-${v.discountPercent}%',
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 10,
                                    fontWeight: FontWeight.w800,
                                    color: Q.accentFg)),
                          ],
                        ],
                      ),
                      const SizedBox(height: 3),
                      Text(v.address, style: T.metaSm),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () =>
                        Navigator.of(context).push(GlassPageRoute(
                            builder: (_) =>
                                VenueDetailScreen(state: state, venue: v))),
                    child: const Text('Ətraflı', style: T.button),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: FilledButton(
                    onPressed: () => showReserveSheet(context, state, v),
                    child: const Text('Rezerv et', style: T.button),
                  ),
                ),
              ],
            ),
          ],
        ),
      );
    } else if (pin.type == _PinType.event) {
      final e = pin.event!;
      card = GlassSurface(
        radius: BorderRadius.circular(Q.rXl),
        sigma: 18,
        luminance: 0.58,
        padding: const EdgeInsets.all(14),
        shadows: const [
          BoxShadow(
            color: Color(0x1E28301F),
            blurRadius: 26,
            offset: Offset(0, 8),
          ),
        ],
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(e.title, style: glassText(size: 14)),
            const SizedBox(height: 4),
            Text('${e.category} · ${e.date}, ${e.time} · ~${e.distance} km',
                style: T.meta),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => showEventDetailSheet(context, state, e),
                child: const Text('Ətraflı', style: T.button),
              ),
            ),
          ],
        ),
      );
    } else {
      final p = pin.person!;
      card = GlassSurface(
        radius: BorderRadius.circular(Q.rXl),
        sigma: 18,
        luminance: 0.58,
        padding: const EdgeInsets.all(14),
        shadows: const [
          BoxShadow(
            color: Color(0x1E28301F),
            blurRadius: 26,
            offset: Offset(0, 8),
          ),
        ],
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(backgroundImage: AssetImage(p.image), radius: 22),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${p.firstName}, ${p.age}', style: glassText(size: 14)),
                      const SizedBox(height: 3),
                      Text('${p.distanceLabel} · ${p.match}% uyğunluq',
                          style: T.meta),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => Navigator.of(context).push(GlassPageRoute(
                    builder: (_) => MatchScreen(state: state))),
                child: const Text('Tanış ol', style: T.button),
              ),
            ),
          ],
        ),
      );
    }
    return card;
  }
}

/// "Mənim yerim" — göy dalğa + ağ halqa (mock GPS: Bakı mərkəzi).
class _MyLocationDot extends StatelessWidget {
  const _MyLocationDot();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 44,
      height: 50,
      child: Center(
        child: Container(
          width: 20,
          height: 20,
          decoration: BoxDecoration(
            color: const Color(0xFF2D7FF9),
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 3),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF2D7FF9).withValues(alpha: 0.4),
                blurRadius: 10,
                offset: const Offset(0, 2),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Alt kartın slide-up + fade girişi — pin seçiləndə "sürüşüb gəlir".
class _SlideUp extends StatefulWidget {
  final Widget child;
  const _SlideUp({required this.child});

  @override
  State<_SlideUp> createState() => _SlideUpState();
}

class _SlideUpState extends State<_SlideUp>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: Motion.d3);
  late final Animation<double> _t = CurvedAnimation(parent: _c, curve: Motion.ease);

  @override
  void initState() {
    super.initState();
    _c.forward();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!Motion.animationsOn(context)) return widget.child;
    return AnimatedBuilder(
      animation: _t,
      builder: (context, child) => FractionalTranslation(
        translation: Offset(0, 0.35 * (1 - _t.value)),
        child: Opacity(opacity: _t.value, child: child),
      ),
      child: widget.child,
    );
  }
}

/// Şüşə filtr çipi — xəritə üzərində üzən seçim həbi (D-025 səth #2).
class _GlassFilterChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool active;
  final VoidCallback onTap;

  const _GlassFilterChip({
    required this.label,
    required this.icon,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ScaleTap(
      onTap: onTap,
      child: AnimatedContainer(
        duration: Motion.d1,
        curve: Motion.ease,
        height: 31,
        padding: const EdgeInsets.symmetric(horizontal: 11),
        decoration: BoxDecoration(
          // Aktiv: brend yaşıl, qatılıq 88% — şüşə kimiliksə də aydın seçim.
          color: active
              ? Q.primary.withValues(alpha: GlassQuality.blur ? 0.88 : 1)
              : Colors.white.withValues(alpha: 0.42),
          border: Border.all(
            color: active ? Q.primary : Colors.white.withValues(alpha: 0.6),
            width: active ? 1.2 : 1,
          ),
          borderRadius: BorderRadius.circular(Q.rMd),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 13, color: active ? Q.primaryFg : Q.foreground),
            const SizedBox(width: 5),
            Text(
              label,
              style: TextStyle(
                fontFamily: T.family,
                fontSize: 10,
                fontWeight: active ? FontWeight.w700 : FontWeight.w500,
                color: active ? Q.primaryFg : Q.foreground,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ───────────────────────── Pin modeli ─────────────────────────

enum _PinType { venue, event, person }

class _MapPin {
  final _PinType type;
  final Partner? venue;
  final QoshulEvent? event;
  final Person? person;
  final double lat;
  final double lng;

  _MapPin.venue(Partner v)
      : type = _PinType.venue,
        venue = v,
        event = null,
        person = null,
        lat = v.lat,
        lng = v.lng;

  _MapPin.event(QoshulEvent e)
      : type = _PinType.event,
        venue = null,
        event = e,
        person = null,
        lat = e.lat,
        lng = e.lng;

  _MapPin.person(Person p)
      : type = _PinType.person,
        venue = null,
        event = null,
        person = p,
        lat = 40.37 + (p.match % 7) * 0.012, // mock: insan paylanması
        lng = 49.83 + (p.age % 7) * 0.007;
}
