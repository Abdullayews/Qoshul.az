import 'package:flutter/material.dart';

import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import 'event_detail_sheet.dart';

/// .event-card — web EventCard portu.
/// Şəkil (kölgə qradiyenti + nişanlar) + info bloku + footer.
class EventCard extends StatelessWidget {
  final QoshulEvent event;
  final AppState state;
  final bool recommended;
  /// D-026 (L.1): peşəkarlıq səviyyəsi çipini göstər (Kəşf sıralaması).
  final bool showLevel;

  const EventCard({
    super.key,
    required this.event,
    required this.state,
    this.recommended = false,
    this.showLevel = false,
  });

  @override
  Widget build(BuildContext context) {
    final isJoined = state.joined.contains(event.id);
    final isPending = state.pending.contains(event.id);
    final isSaved = state.saved.contains(event.id);

    // StaggerIn: kart hər build-də (scroll-da da) yumşaq girir (D-025).
    return StaggerIn(
      index: 0,
      child: ScaleTap(
      onTap: () => showEventDetailSheet(context, state, event),
      child: Container(
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
            // — şəkil + nişanlar —
            Stack(
              children: [
                SizedBox(
                  height: 156,
                  width: double.infinity,
                  child: Image.asset(event.image, fit: BoxFit.cover),
                ),
                Positioned.fill(
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.bottomCenter,
                        end: Alignment.topCenter,
                        stops: const [0, 0.6],
                        colors: [
                          Q.photoShade.withValues(alpha: 0.5),
                          Q.photoShade.withValues(alpha: 0),
                        ],
                      ),
                    ),
                  ),
                ),
                if (recommended && !event.past)
                  Positioned(
                    left: 12,
                    top: 12,
                    child: _whiteBadge(
                      Icons.auto_awesome,
                      'Tədbir təklifi',
                    ),
                  ),
                // D-026 (L.1): peşəkarlıq səviyyəsi — sağ alt çip.
                if (showLevel &&
                    !event.past &&
                    (event.level == 'Peşəkar' ||
                        event.level == 'Orta' ||
                        event.level == 'Başlanğıc'))
                  Positioned(
                    left: 12,
                    bottom: 12,
                    child: _whiteBadge(
                      event.level == 'Peşəkar'
                          ? Icons.military_tech
                          : Icons.school,
                      event.level!,
                    ),
                  ),
                if (event.past)
                  Positioned(
                    left: 12,
                    top: 12,
                    child: _whiteBadge(Icons.event, 'Keçib'),
                  ),
                if (!event.past)
                  Positioned(
                    right: 10,
                    top: 10,
                    child: GestureDetector(
                      onTap: () => state.toggleSave(event.id),
                      child: Container(
                        width: 34,
                        height: 34,
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.9),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          isSaved ? Icons.bookmark : Icons.bookmark_border,
                          size: 18,
                          color: isSaved ? Q.primary : Q.foreground,
                        ),
                      ),
                    ),
                  ),
                if (!event.past)
                  Positioned(
                    left: 12,
                    bottom: 10,
                    child: Row(
                      children: [
                        Icon(Icons.location_on, size: 13.5, color: Colors.white),
                        const SizedBox(width: 4),
                        Text(
                          '${event.distance} km',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10.5,
                              color: Colors.white),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
            // — info —
            Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        event.category == 'Qəhvə'
                            ? Icons.local_cafe
                            : event.category == 'İdman' ||
                                    event.category == 'Padel'
                                ? Icons.emoji_events
                                : Icons.group,
                        size: 13,
                        color: event.category == 'Qəhvə'
                            ? Q.accentFg
                            : Q.primary,
                      ),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          event.category,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontFamily: T.family,
                            fontSize: 10.5,
                            fontWeight: FontWeight.w700,
                            color: event.category == 'Qəhvə'
                                ? Q.accentFg
                                : Q.primary,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 7),
                  Text(
                    event.title,
                    style: T.eventTitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Icon(Icons.location_on, size: 13.5, color: Q.mutedFg),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          event.location,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: T.meta,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 7),
                  Row(
                    children: [
                      Icon(Icons.event, size: 13.5, color: Q.mutedFg),
                      const SizedBox(width: 6),
                      Text('${event.date}, ${event.time}', style: T.meta),
                    ],
                  ),
                  // — footer —
                  Container(
                    margin: const EdgeInsets.only(top: 13),
                    padding: const EdgeInsets.only(top: 12),
                    decoration: const BoxDecoration(
                        border: Border(top: BorderSide(color: Q.border))),
                    child: Row(
                      children: [
                        // avatar stack (mock iştirakçılar)
                        ..._avatarStack(),
                        const SizedBox(width: 9),
                        Expanded(
                          child: Text(
                            '${event.participants} nəfər qoşulub · ${event.spotsLeft} yer qalıb',
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 9.5,
                                color: Q.mutedFg),
                          ),
                        ),
                        _joinButton(context, isJoined, isPending),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
      ),
    );
  }

  Widget _whiteBadge(IconData icon, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5.5),
      decoration: BoxDecoration(
        color: Q.card,
        borderRadius: BorderRadius.circular(5),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13.5, color: Q.primary),
          const SizedBox(width: 4),
          Text(
            label,
            style: const TextStyle(
              fontFamily: T.family,
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: Q.primary,
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _avatarStack() {
    const colors = [Color(0xFF8AD8A8), Color(0xFF6FCB95), Color(0xFFB7E4C7)];
    // Web CSS: margin-left: -3px üst-üstə düşmə — Flutter-da Stack + offset ilə.
    return [
      SizedBox(
        width: 26 + 2 * 23.0,
        height: 26,
        child: Stack(
          children: [
            for (var i = 0; i < 3; i++)
              Positioned(
                left: i * 23.0,
                child: Container(
                  width: 26,
                  height: 26,
                  decoration: BoxDecoration(
                    color: colors[i],
                    shape: BoxShape.circle,
                    border: Border.all(color: Q.card, width: 2),
                  ),
                ),
              ),
          ],
        ),
      ),
    ];
  }

  Widget _joinButton(BuildContext context, bool isJoined, bool isPending) {
    if (event.past) {
      return _btn('Keçib', enabled: false, secondary: true);
    }
    if (isJoined) {
      return _btn('Qoşuldun', icon: Icons.check, secondary: true);
    }
    if (isPending) {
      return _btn('Təsdiq gözləyir', icon: Icons.hourglass_empty, enabled: false, secondary: true);
    }
    if (event.isFull) {
      return _btn('Yer yoxdur', enabled: false, secondary: true);
    }
    return _btn('Qoşul', icon: Icons.north_east, onTap: () {
      final ok = state.join(event.id);
      if (ok) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(event.approval
                ? 'Sorğun təşkilatçıya göndərildi.'
                : 'Tədbirə qoşuldun! +${XpRules.joinXp} XP'),
          ),
        );
      }
    });
  }

  Widget _btn(
    String label, {
    IconData? icon,
    bool enabled = true,
    bool secondary = false,
    VoidCallback? onTap,
  }) {
    return SizedBox(
      height: 34,
      child: FilledButton(
        onPressed: enabled ? onTap ?? () {} : null,
        style: FilledButton.styleFrom(
          backgroundColor: secondary ? Q.secondary : Q.primary,
          foregroundColor: secondary ? Q.primary : Q.primaryFg,
          disabledBackgroundColor: Q.secondary,
          disabledForegroundColor: Q.primary,
          padding: const EdgeInsets.symmetric(horizontal: 14),
          minimumSize: Size.zero,
          textStyle: const TextStyle(fontFamily: T.family,
              fontSize: 11.5, fontWeight: FontWeight.w600),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 13.5),
              const SizedBox(width: 4),
            ],
            Text(label),
          ],
        ),
      ),
    );
  }
}
