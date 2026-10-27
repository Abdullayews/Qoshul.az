import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../events/presentation/event_detail_sheet.dart';
import '../../match/presentation/match_screen.dart';
import '../../venues/presentation/reserve_sheet.dart';
import '../../venues/presentation/venue_detail_screen.dart';

/// Xəritə tabı — ROL (product-spec C, TAB 2).
/// PostGIS əsaslı xəritənin UI placeholder-i (D-023): stilləşdirilmiş
/// CustomPaint. Pin-lər: məkanlar, tədbirlər, (razılıq verən) insanlar.
/// Filtrlər: kateqoriya, məsafə (1/3/5/10 km), "indi açıqdır", "endirimi var".
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
    final peoplePins = <_MapPin>[
      for (final p in state.matchCandidates)
        if (p.distanceKm <= distanceKm) _MapPin.person(p),
    ];
    final pins = [...venuePins, ...eventPins, ...peoplePins];

    return Column(
      children: [
        // — filtrlər —
        Container(
          color: Q.card,
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 10),
          child: Column(
            children: [
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    for (final e in _categoryChips.entries)
                      Padding(
                        padding: const EdgeInsets.only(right: 7),
                        child: CategoryChip(
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
                        child: CategoryChip(
                          label: '$km km',
                          icon: Icons.near_me,
                          active: distanceKm == km,
                          onTap: () => setState(() => distanceKm = km),
                        ),
                      ),
                    const SizedBox(width: 6),
                    CategoryChip(
                      label: 'indi açıqdır',
                      icon: Icons.schedule,
                      active: openNow,
                      onTap: () => setState(() => openNow = !openNow),
                    ),
                    const SizedBox(width: 7),
                    CategoryChip(
                      label: 'endirimi var',
                      icon: Icons.percent,
                      active: withDiscount,
                      onTap: () => setState(() => withDiscount = !withDiscount),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        // — xəritə sahəsi —
        Expanded(
          child: LayoutBuilder(builder: (context, constraints) {
            final mapW = constraints.maxWidth;
            final mapH = constraints.maxHeight;
            return Stack(
              clipBehavior: Clip.hardEdge,
              children: [
                GestureDetector(
                  onTap: () => setState(() => selected = null),
                  child: CustomPaint(
                    painter: _MapPainter(),
                    size: Size(mapW, mapH),
                  ),
                ),
                for (final pin in pins)
                  Positioned(
                    left: pin.fx * mapW - 22,
                    top: pin.fy * mapH - 46,
                    width: 44,
                    height: 56,
                    child: GestureDetector(
                      onTap: () => setState(() => selected = pin),
                      behavior: HitTestBehavior.opaque,
                      child: _pinWidget(pin, selected == pin),
                    ),
                  ),
                if (selected != null)
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: _bottomCard(selected!),
                  ),
                Positioned(
                  top: 10,
                  left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 9, vertical: 6),
                    decoration: BoxDecoration(
                      color: Q.card,
                      border: Border.all(color: Q.border),
                      borderRadius: BorderRadius.circular(Q.rMd),
                    ),
                    child: Text(
                      '${venuePins.length} məkan · ${eventPins.length} tədbir · '
                      '${peoplePins.length} insan',
                      style: T.meta,
                    ),
                  ),
                ),
              ],
            );
          }),
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
      children: [
        Container(
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
        Container(
          width: 2.5,
          height: active ? 14 : 11,
          color: color,
        ),
      ],
    );
  }

  /// Pin-ə toxunanda alt kart (ROL TAB 2): ad, qiymət aralığı, endirim,
  /// "Ətraflı" və "Rezerv et" düymələri.
  Widget _bottomCard(_MapPin pin) {
    final state = widget.state;
    Widget card;
    if (pin.type == _PinType.venue) {
      final v = pin.venue!;
      card = Container(
        margin: const EdgeInsets.all(12),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rXl),
          boxShadow: const [Q.softShadow],
        ),
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
                      Text(v.name,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 14,
                              fontWeight: FontWeight.w800)),
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
                    onPressed: () => Navigator.of(context).push(MaterialPageRoute(
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
      card = Container(
        margin: const EdgeInsets.all(12),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rXl),
          boxShadow: const [Q.softShadow],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(e.title,
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 14,
                    fontWeight: FontWeight.w800)),
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
      card = Container(
        margin: const EdgeInsets.all(12),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rXl),
          boxShadow: const [Q.softShadow],
        ),
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
                      Text('${p.firstName}, ${p.age}',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 14,
                              fontWeight: FontWeight.w800)),
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
                onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => MatchScreen(state: state))),
                child: const Text('Tanış ol', style: T.button),
              ),
            ),
          ],
        ),
      );
    }
    return SafeArea(child: card);
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

  // Xəritə koordinat transformu (Bakı bounding box → 0..1).
  static const _minLat = 40.30;
  static const _maxLat = 40.46;
  static const _minLng = 49.72;
  static const _maxLng = 49.90;

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

  double _norm(double v, double min, double max) =>
      ((v - min) / (max - min)).clamp(0.08, 0.92);

  // Placeholder sahədə nisbi mövqe (0..1) — LayoutBuilder ölçüsü ilə vurulur.
  double get fx => _norm(lng, _minLng, _maxLng);
  double get fy => 1 - _norm(lat, _minLat, _maxLat);
}

// ───────────────────────── Xəritə rəssamı ─────────────────────────

class _MapPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    // fon
    final bg = Paint()..color = const Color(0xFFEDF3EF);
    canvas.drawRect(Offset.zero & size, bg);

    // su (Xəzər — aşağı/sağ künc)
    final water = Paint()..color = const Color(0xFFD6E8F5);
    final waterPath = Path()
      ..moveTo(size.width, size.height * 0.55)
      ..quadraticBezierTo(
          size.width * 0.8, size.height * 0.7, size.width * 0.92, size.height)
      ..lineTo(size.width, size.height)
      ..close();
    canvas.drawPath(waterPath, water);

    // parklar
    final park = Paint()..color = const Color(0xFFDDEEDD);
    canvas.drawCircle(Offset(size.width * 0.22, size.height * 0.28), 46, park);
    canvas.drawCircle(Offset(size.width * 0.7, size.height * 0.2), 34, park);

    // bloklar
    final block = Paint()..color = const Color(0xFFE2E9E4);
    const rows = 7, cols = 5;
    for (var r = 0; r < rows; r++) {
      for (var c = 0; c < cols; c++) {
        if ((r * 5 + c * 3) % 4 == 0) continue;
        final w = size.width / cols - 14;
        final h = size.height / rows - 14;
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            Rect.fromLTWH(8 + c * (w + 16) + ((r % 2) * 10),
                8 + r * (h + 18), w, h),
            const Radius.circular(6),
          ),
          block,
        );
      }
    }

    // yollar
    final road = Paint()
      ..color = Colors.white
      ..strokeWidth = 5
      ..style = PaintingStyle.stroke;
    canvas.drawLine(Offset(0, size.height * 0.42),
        Offset(size.width, size.height * 0.36), road);
    canvas.drawLine(Offset(size.width * 0.3, 0),
        Offset(size.width * 0.38, size.height), road);
    canvas.drawLine(Offset(size.width * 0.72, 0),
        Offset(size.width * 0.66, size.height), road);
  }

  @override
  bool shouldRepaint(covariant _MapPainter old) => false;
}
