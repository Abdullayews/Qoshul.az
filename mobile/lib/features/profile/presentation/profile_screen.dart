import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';
import '../../coupons/presentation/coupons_screen.dart';
import '../../events/presentation/event_card.dart';
import '../../reservations/presentation/reservations_screen.dart';
import '../../settings/presentation/settings_screen.dart';

/// Profil — web ProfileScreen + MyProfilePanel portu.
class ProfileScreen extends StatefulWidget {
  final AppState state;
  const ProfileScreen({super.key, required this.state});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  int tab = 0; // 0 Tədbirlərim, 1 Yadda saxlanılanlar, 2 Nailiyyətlər

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        final prog = state.xpProgress;
        final mine = state.events
            .where((e) => state.joined.contains(e.id) || state.pending.contains(e.id))
            .toList();
        final saved = state.events
            .where((e) => state.saved.contains(e.id))
            .toList();
        final tabList = tab == 1 ? saved : mine;

        return ListView(
          // alt 128px: şüşə nav pill altından axış (extendBody, D-025)
          padding: const EdgeInsets.fromLTRB(20, 13, 20, 128),
          children: [
            // — redaktə / ayarlar düymələri —
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                IconButton(
                  tooltip: 'Profili redaktə et',
                  onPressed: _editName,
                  icon: const Icon(Icons.edit, size: 20),
                  color: Q.foreground,
                ),
                IconButton(
                  tooltip: 'Ayarlar',
                  onPressed: () => Navigator.of(context).push(
                    GlassPageRoute(builder: (_) => SettingsScreen(state: state)),
                  ),
                  icon: const Icon(Icons.settings, size: 20),
                  color: Q.foreground,
                ),
              ],
            ),
            // — profil başlığı —
            Center(
              child: Column(
                children: [
                  Container(
                    width: 84,
                    height: 84,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(color: Q.card, width: 4),
                      boxShadow: [
                        BoxShadow(
                          color: Q.primary.withValues(alpha: 0.9),
                          spreadRadius: 2,
                        ),
                      ],
                    ),
                    child: CircleAvatar(
                      backgroundImage: const AssetImage(imgCoffee),
                      backgroundColor: Q.secondary,
                      child: state.personality == null
                          ? Text(
                              state.firstName.isNotEmpty
                                  ? state.firstName[0]
                                  : 'Q',
                              style: const TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 28,
                                  fontWeight: FontWeight.w800,
                                  color: Q.primary),
                            )
                          : null,
                    ),
                  ),
                  const SizedBox(height: 15),
                  Text(state.profileName, style: T.h1.copyWith(fontSize: 24)),
                  const SizedBox(height: 9),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      RankBadge(label: state.rank),
                      if (state.phoneVerified) ...[
                        const SizedBox(width: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 9, vertical: 6),
                          decoration: BoxDecoration(
                            color: Q.secondary,
                            borderRadius: BorderRadius.circular(Q.rMd),
                          ),
                          child: const Row(
                            children: [
                              Icon(Icons.verified, size: 12, color: Q.primary),
                              SizedBox(width: 4),
                              Text('Təsdiqlənmiş',
                                  style: TextStyle(
                                      fontFamily: T.family,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w700,
                                      color: Q.primary)),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 9),
                  Text(
                    '${state.city} · ${statusLabel(state.status)}'
                    '${state.university.isNotEmpty ? ' · ${state.university}' : state.workField.isNotEmpty ? ' · ${state.workField}' : ''}',
                    textAlign: TextAlign.center,
                    style: T.subtitle,
                  ),
                ],
              ),
            ),
            // — maraqlar —
            if (state.interests.isNotEmpty) ...[
              const SizedBox(height: 20),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final i in state.interests)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 11, vertical: 7),
                      decoration: BoxDecoration(
                        color: Q.secondary,
                        borderRadius: BorderRadius.circular(5),
                      ),
                      child: Text(i,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10,
                              color: Q.primary)),
                    ),
                ],
              ),
            ],
            // — şəxsiyyət testi nəticəsi —
            if (state.personality != null) ...[
              const SizedBox(height: 16),
              CommunityBanner(
                icon: Icons.auto_awesome,
                title: state.personality!.type,
                subtitle: state.personality!.summary,
              ),
              const SizedBox(height: 10),
              _traitBars(state.personality!),
            ],
            // — nişanlar (ROL F) —
            const SizedBox(height: 18),
            const SectionHeading(title: 'Nişanlar', icon: Icons.military_tech),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final b in badgeDefs)
                  // Nişanlar sırayla pop şəklində doğulur (D-025).
                  PopIn(
                    delay: Motion.staggerStep * badgeDefs.indexOf(b),
                    child: Tooltip(
                      message: b.description,
                      child: BadgeChip(
                          badge: b,
                          earned: state.earnedBadges.contains(b.id)),
                    ),
                  ),
              ],
            ),
            // — tez keçidlər: rezervlərim / kuponlarım (ROL TAB 5) —
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _quickCard(
                    icon: Icons.table_restaurant,
                    title: 'Rezervlərim',
                    value: '${state.reservations.length}',
                    onTap: () => Navigator.of(context).push(GlassPageRoute(
                        builder: (_) => ReservationsScreen(state: state))),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _quickCard(
                    icon: Icons.confirmation_number,
                    title: 'Kuponlarım',
                    value: '${state.coupons.where((c) => c.isActive).length}',
                    onTap: () => Navigator.of(context).push(GlassPageRoute(
                        builder: (_) => CouponsScreen(state: state))),
                  ),
                ),
              ],
            ),
            // — statistika —
            Container(
              margin: const EdgeInsets.only(top: 20),
              decoration: const BoxDecoration(
                border: Border(
                  top: BorderSide(color: Q.border),
                  bottom: BorderSide(color: Q.border),
                ),
              ),
              padding: const EdgeInsets.symmetric(vertical: 20),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  // Statlar sayğacla artır — XP qazanınca canlı dəyişir (D-025).
                  _statCount(state.xp, 'Ümumi XP'),
                  _statCount(state.xpBalance, 'Balans'),
                  _statCount(state.joined.length, 'Tədbir'),
                  _stat(state.rank, 'Səviyyə'),
                ],
              ),
            ),
            const SizedBox(height: 13),
            // — XP progress —
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  prog['nextName'] ?? 'Maksimum',
                  style: const TextStyle(
                      fontFamily: T.family,
                      fontSize: 10,
                      fontWeight: FontWeight.w600),
                ),
                Text(
                  prog['nextName'] != null ? '${prog['toNext']} XP qalıb' : '',
                  style: T.meta,
                ),
              ],
            ),
            const SizedBox(height: 6),
            ClipRRect(
              borderRadius: BorderRadius.circular(2),
              child: SizedBox(
                height: 4,
                child: TweenAnimationBuilder<double>(
                  tween: Tween(begin: 0, end: (prog['pct'] as int) / 100),
                  duration: Motion.d4,
                  curve: Motion.ease,
                  builder: (context, value, _) => LinearProgressIndicator(
                    value: value,
                    minHeight: 4,
                    backgroundColor: Q.border,
                    valueColor:
                        const AlwaysStoppedAnimation<Color>(Q.primary),
                  ),
                ),
              ),
            ),
            // — tablar —
            Segmented(
              labels: const ['Tədbirlərim', 'Yadda saxlanılanlar', 'Nailiyyətlər'],
              active: tab,
              onChanged: (i) => setState(() => tab = i),
            ),
            if (tab == 2)
              Column(
                children: [
                  CommunityBanner(
                    icon: Icons.emoji_events,
                    title: state.rank,
                    subtitle:
                        '${state.xp} XP topladın. Növbəti görüşə qoşul, yeni səviyyəyə yüksəl!',
                  ),
                  const SizedBox(height: 6),
                  for (final b in badgeDefs)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: Row(
                        children: [
                          Icon(
                            state.earnedBadges.contains(b.id)
                                ? Icons.check_circle
                                : Icons.circle_outlined,
                            size: 14,
                            color: state.earnedBadges.contains(b.id)
                                ? Q.primary
                                : Q.mutedFg,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              '${b.name} — ${b.description}',
                              style: TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 11,
                                  height: 1.5,
                                  color: state.earnedBadges.contains(b.id)
                                      ? Q.foreground
                                      : Q.mutedFg),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              )
            else if (tabList.isEmpty)
              const EmptyState(
                icon: Icons.event,
                text: 'Burada hələ tədbir yoxdur.\nYeni bir plana qoşulmağa nə deyirsən?',
              )
            else
              for (final e in tabList) ...[
                EventCard(event: e, state: state),
                const SizedBox(height: 16),
              ],
          ],
        );
      },
    );
  }

  Widget _stat(String value, String label) {
    return Column(
      children: [
        Text(value, style: T.stat),
        const SizedBox(height: 3),
        Text(label, style: T.meta),
      ],
    );
  }

  /// Animasiyalı stat — rəqəm sayğacla artır (D-025).
  Widget _statCount(int value, String label) {
    return Column(
      children: [
        CountUp(value: value, style: T.stat),
        const SizedBox(height: 3),
        Text(label, style: T.meta),
      ],
    );
  }

  /// Tez keçid kartı (Rezervlərim / Kuponlarım).
  Widget _quickCard({
    required IconData icon,
    required String title,
    required String value,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: ScaleTap(
        onTap: onTap,
        child: Container(
        padding: const EdgeInsets.all(13),
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rLg),
        ),
        child: Row(
          children: [
            Icon(icon, size: 19, color: Q.primary),
            const SizedBox(width: 10),
            Expanded(
              child: Text(title,
                  style: const TextStyle(
                      fontFamily: T.family,
                      fontSize: 12,
                      fontWeight: FontWeight.w700)),
            ),
            Text(value, style: T.stat.copyWith(fontSize: 17)),
          ],
        ),
      ),
      ),
    );
  }

  /// TraitBars — web onboarding.ts TraitBars portu.
  Widget _traitBars(p) {
    final traits = p.traits as Map<String, int>;
    return Column(
      children: [
        for (final entry in traitLabels.entries)
          if (traits[entry.key] != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(entry.value,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10,
                              color: Q.mutedFg)),
                      Text('${traits[entry.key]}',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10,
                              fontWeight: FontWeight.w600)),
                    ],
                  ),
                  const SizedBox(height: 4),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: SizedBox(
                      height: 4,
                      child: LinearProgressIndicator(
                        value: (traits[entry.key] ?? 0) / 100,
                        minHeight: 4,
                        backgroundColor: Q.border,
                        valueColor: const AlwaysStoppedAnimation<Color>(Q.primary),
                      ),
                    ),
                  ),
                ],
              ),
            ),
      ],
    );
  }

  void _editName() {
    final ctrl = TextEditingController(text: widget.state.profileName);
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Profili redaktə et'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Səni necə çağıraq?', style: T.subtitle),
            const SizedBox(height: 8),
            TextField(controller: ctrl),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('Ləğv et'),
          ),
          FilledButton(
            onPressed: () {
              if (ctrl.text.trim().isNotEmpty) {
                widget.state.setProfileName(ctrl.text.trim());
              }
              Navigator.of(dialogContext).pop();
            },
            child: const Text('Yadda saxla'),
          ),
        ],
      ),
    );
  }
}
