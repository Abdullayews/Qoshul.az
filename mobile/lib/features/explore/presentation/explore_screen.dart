import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../events/presentation/event_card.dart';

/// Kəşf et — web ExploreScreen portu.
/// Yaxınlıqda (xəritə + siyahı) / Populyar / İnsanlar.
class ExploreScreen extends StatefulWidget {
  final AppState state;
  const ExploreScreen({super.key, required this.state});

  @override
  State<ExploreScreen> createState() => _ExploreScreenState();
}

class _ExploreScreenState extends State<ExploreScreen> {
  int view = 0; // 0 Yaxınlıqda, 1 Populyar, 2 İnsanlar
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

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final upcoming = state.upcoming.toList();
        final nearby = [...upcoming]..sort((a, b) => a.distance.compareTo(b.distance));
        final center = findCity(state.city);

        return ListView(
          padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
          children: [
            const ViewHeading(
                title: 'Yeni planlar kəşf et',
                subtitle: 'Yaxınındakı görüşlər, sənə yaxın insanlar.'),
            Segmented(
              labels: const ['Yaxınlıqda', 'Populyar', 'İnsanlar'],
              active: view,
              onChanged: (i) => setState(() => view = i),
            ),
            if (view == 0) ...[
              Row(
                children: [
                  const Expanded(
                    child: Row(
                      children: [
                        Icon(Icons.location_on, size: 13, color: Q.primary),
                        SizedBox(width: 6),
                        Flexible(
                          child: Text(
                            'Şəhər mərkəzinə ən yaxın tədbirlər',
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                                fontFamily: T.family,
                                fontSize: 13,
                                fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                  ),
                  FilledButton.icon(
                    onPressed: () {},
                    style: FilledButton.styleFrom(
                      backgroundColor: Q.secondary,
                      foregroundColor: Q.primary,
                      minimumSize: const Size(0, 30),
                      padding: const EdgeInsets.symmetric(horizontal: 10),
                    ),
                    icon: const Icon(Icons.my_location, size: 14),
                    label: const Text('Mənim yerim',
                        style: TextStyle(
                            fontFamily: T.family,
                            fontSize: 10,
                            fontWeight: FontWeight.w600)),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              _MapPlaceholder(
                events: nearby.take(6).toList(),
                center: center,
                onEventTap: (e) {},
              ),
              const SizedBox(height: 6),
              const Row(
                children: [
                  Icon(Icons.near_me, size: 12, color: Q.mutedFg),
                  SizedBox(width: 6),
                  Text('Nişana toxun və "Yol göstər" ilə məkana get.',
                      style: TextStyle(
                          fontFamily: T.family,
                          fontSize: 11,
                          color: Q.mutedFg)),
                ],
              ),
              const SizedBox(height: 14),
              _browser(state, nearby),
            ] else if (view == 1)
              _browser(
                state,
                [...state.cityEvents]..sort((a, b) =>
                    (b.participants / b.capacity).compareTo(a.participants / a.capacity)),
              )
            else
              _people(state),
          ],
        );
      },
    );
  }

  /// Axtarış + kateqoriyalar + tədbir siyahısı (web EventBrowser).
  Widget _browser(AppState state, List<QoshulEvent> events) {
    final filtered = events.where((e) {
      final catOk = category == 'Hamısı' || groupOf(e.category) == category;
      final q = searchCtrl.text.toLowerCase();
      final textOk = q.isEmpty ||
          '${e.title} ${e.location} ${e.category} ${e.city}'
              .toLowerCase()
              .contains(q);
      return catOk && textOk;
    }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SearchBarField(
          controller: searchCtrl,
          hint: 'Tədbir, maraq və ya məkan axtar...',
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
      ],
    );
  }

  /// İnsanlar tab — web AiPeople portu.
  Widget _people(AppState state) {
    return Column(
      children: [
        AiBox(
          child: Row(
            children: [
              const Expanded(
                child: Text(
                  'AI profilinə, maraqlarına və şəxsiyyətinə görə qiymətləndirir.',
                  style: TextStyle(fontFamily: T.family, fontSize: 12),
                ),
              ),
              const SizedBox(width: 8),
              FilledButton.icon(
                onPressed: () {},
                style: FilledButton.styleFrom(
                  minimumSize: const Size(0, 30),
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                ),
                icon: const Icon(Icons.refresh, size: 14),
                label: const Text('Yenilə',
                    style: TextStyle(
                        fontFamily: T.family,
                        fontSize: 10,
                        fontWeight: FontWeight.w600)),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        for (final p in mockPeople) ...[
          _PersonCard(person: p, state: state),
          const SizedBox(height: 12),
        ],
        const Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.verified_user, size: 13, color: Q.primary),
            SizedBox(width: 6),
            Text('Şəxsi məlumatların yalnız razılığınla görünür.',
                style: TextStyle(
                    fontFamily: T.family, fontSize: 10, color: Q.mutedFg)),
          ],
        ),
      ],
    );
  }
}

/// .person-card — web people-grid kartı.
class _PersonCard extends StatelessWidget {
  final Person person;
  final AppState state;

  const _PersonCard({required this.person, required this.state});

  @override
  Widget build(BuildContext context) {
    final f = state.friendships[person.id];
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rLg),
        boxShadow: [Q.softShadow],
      ),
      child: Column(
        children: [
          CircleAvatar(
            radius: 36,
            backgroundImage: AssetImage(person.image),
          ),
          const SizedBox(height: 12),
          Text(person.fullName, style: T.personName),
          const SizedBox(height: 7),
          Text(
            '${person.city} · ${statusLabel(person.status)} · ${person.org}',
            textAlign: TextAlign.center,
            style: T.meta,
          ),
          const SizedBox(height: 15),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.auto_awesome, size: 11, color: Q.primary),
              const SizedBox(width: 4),
              Text('${person.match}% AI uyğunluq', style: const TextStyle(
                  fontFamily: T.family,
                  fontSize: 9,
                  fontWeight: FontWeight.w700,
                  color: Q.primary)),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            person.reason,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontFamily: T.family, fontSize: 8, color: Q.mutedFg, height: 1.6),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: AnimatedBuilder(
              animation: state,
              builder: (context, _) {
                if (f == null) {
                  return FilledButton.icon(
                    onPressed: () {
                      state.requestFriend(person.id);
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                            content: Text('Dostluq sorğusu göndərildi.')),
                      );
                    },
                    icon: const Icon(Icons.add, size: 14),
                    label: const Text('Dostluq sorğusu',
                        style: TextStyle(
                            fontFamily: T.family,
                            fontSize: 11,
                            fontWeight: FontWeight.w600)),
                  );
                }
                if (f == 'accepted') {
                  return FilledButton(
                    onPressed: () {},
                    style: FilledButton.styleFrom(
                        backgroundColor: Q.secondary,
                        foregroundColor: Q.primary),
                    child: const Text('Dostsunuz — söhbət et',
                        style: TextStyle(
                            fontFamily: T.family,
                            fontSize: 11,
                            fontWeight: FontWeight.w600)),
                  );
                }
                if (f == 'received') {
                  return FilledButton.icon(
                    onPressed: () => state.acceptFriend(person.id),
                    icon: const Icon(Icons.check, size: 14),
                    label: const Text('Sorğunu qəbul et',
                        style: TextStyle(
                            fontFamily: T.family,
                            fontSize: 11,
                            fontWeight: FontWeight.w600)),
                  );
                }
                return FilledButton(
                  onPressed: null,
                  style: FilledButton.styleFrom(
                      backgroundColor: Q.secondary,
                      disabledBackgroundColor: Q.secondary,
                      disabledForegroundColor: Q.primary),
                  child: const Text('Sorğu göndərildi',
                      style: TextStyle(
                          fontFamily: T.family,
                          fontSize: 11,
                          fontWeight: FontWeight.w600)),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

/// Xəritə plitələri placeholder-i (UI mərhələsi, D-023).
/// Real xəritə (flutter_map + OSM) backend mərhələsində gələcək.
/// Web-dəki .event-map ölçüsü: 320px hündürlük, radius 18.
class _MapPlaceholder extends StatelessWidget {
  final List<QoshulEvent> events;
  final City center;
  final ValueChanged<QoshulEvent> onEventTap;

  const _MapPlaceholder({
    required this.events,
    required this.center,
    required this.onEventTap,
  });

  /// lat/lng -> x/y (düzbucaqlı yaxınlaşdırma, yerli miqyas).
  Offset _project(double lat, double lng, Size size) {
    const span = 0.09; // ~10 km
    final dx = (lng - center.lng) / span + 0.5;
    final dy = 0.5 - (lat - center.lat) / span;
    return Offset(dx * size.width, dy * size.height);
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, constraints) {
      final size = Size(constraints.maxWidth, 320);
      return Container(
        height: 320,
        width: double.infinity,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(Q.rMap),
          border: Border.all(color: Q.border),
        ),
        clipBehavior: Clip.antiAlias,
        child: CustomPaint(
          painter: _MapPainter(center: center, project: _project),
          child: Stack(
            children: [
              for (final e in events)
                Builder(builder: (context) {
                  final pos = _project(e.lat, e.lng, size);
                  if (pos.dx < 8 ||
                      pos.dx > size.width - 8 ||
                      pos.dy < 8 ||
                      pos.dy > size.height - 8) {
                    return const SizedBox.shrink();
                  }
                  return Positioned(
                    left: pos.dx - 17,
                    top: pos.dy - 34,
                    child: GestureDetector(
                      onTap: () => onEventTap(e),
                      child: _Pin(label: '${e.participants}'),
                    ),
                  );
                }),
              Positioned(
                left: size.width / 2 - 7,
                top: size.height / 2 - 7,
                child: Container(
                  width: 14,
                  height: 14,
                  decoration: BoxDecoration(
                    color: const Color(0xFF2F7DF6).withValues(alpha: 0.6),
                    shape: BoxShape.circle,
                    border: Border.all(color: const Color(0xFF2F7DF6), width: 2),
                  ),
                ),
              ),
            ],
          ),
        ),
      );
    });
  }
}

/// Web .map-pin — 34px, yaşıl, aşağı sivri, içində iştirakçı sayı.
class _Pin extends StatelessWidget {
  final String label;
  const _Pin({required this.label});

  @override
  Widget build(BuildContext context) {
    return Transform.rotate(
      angle: -45 * math.pi / 180,
      child: Container(
        width: 34,
        height: 34,
        decoration: BoxDecoration(
          color: Q.primary,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(17),
            topRight: Radius.circular(17),
            bottomLeft: Radius.circular(17),
          ),
          boxShadow: [Q.softShadow],
        ),
        child: Center(
          child: Transform.rotate(
            angle: 45 * math.pi / 180,
            child: Text(
              label,
              style: const TextStyle(
                fontFamily: T.family,
                fontSize: 11,
                fontWeight: FontWeight.w700,
                color: Q.primaryFg,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Xəritə fonu — küçə şəbəkəsi, park və dəniz ləkələri (stilləşdirilmiş).
class _MapPainter extends CustomPainter {
  final City center;
  final Offset Function(double, double, Size) project;

  _MapPainter({required this.center, required this.project});

  @override
  void paint(Canvas canvas, Size size) {
    // fon
    final bg = Paint()..color = const Color(0xFFEDF1F4);
    canvas.drawRect(Offset.zero & size, bg);

    // dəniz (Bakı üçün aşağı-sağ künc)
    final sea = Paint()..color = const Color(0xFFD3E5F5);
    final seaPath = Path()
      ..moveTo(size.width, size.height * 0.55)
      ..quadraticBezierTo(size.width * 0.7, size.height * 0.75,
          size.width * 0.35, size.height)
      ..lineTo(size.width, size.height)
      ..close();
    canvas.drawPath(seaPath, sea);

    // park ləkələri
    final park = Paint()..color = const Color(0xFFDFEDDD);
    canvas.drawCircle(Offset(size.width * 0.22, size.height * 0.28), 38, park);
    canvas.drawCircle(Offset(size.width * 0.68, size.height * 0.18), 26, park);

    // küçə şəbəkəsi
    final street = Paint()
      ..color = Colors.white
      ..strokeWidth = 3;
    for (var i = 1; i < 6; i++) {
      canvas.drawLine(
          Offset(0, size.height * i / 6), Offset(size.width, size.height * i / 6), street);
    }
    for (var i = 1; i < 4; i++) {
      canvas.drawLine(
          Offset(size.width * i / 4, 0), Offset(size.width * i / 4, size.height), street);
    }
    // əsas prospekt
    final main = Paint()
      ..color = const Color(0xFFFFF6DE)
      ..strokeWidth = 6;
    canvas.drawLine(Offset(0, size.height * 0.5),
        Offset(size.width, size.height * 0.42), main);

    // şəbəkə adları (kiçik mətn)
    final tp = TextPainter(
      text: const TextSpan(
        text: 'Xəzər dənizi',
        style: TextStyle(
            fontFamily: T.family, fontSize: 8, color: Color(0xFF9AA7B2)),
      ),
      textDirection: TextDirection.ltr,
    );
    tp.layout();
    tp.paint(canvas, Offset(size.width - tp.width - 10, size.height - tp.height - 10));
  }

  @override
  bool shouldRepaint(covariant _MapPainter old) => old.center != center;
}
