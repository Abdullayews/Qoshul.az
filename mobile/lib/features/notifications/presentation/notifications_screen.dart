import 'package:flutter/material.dart';

import '../../../core/models/notification.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/ui.dart';

/// Bildiriş mərkəzi + tip tercihləri — ROL (product-spec H):
/// hər tip ayrıca söndürülür; marketinq 23:00-08:00 göndərilmir.
class NotificationsScreen extends StatefulWidget {
  final AppState state;
  const NotificationsScreen({super.key, required this.state});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  bool showPrefs = false;

  static const _icons = <NotifType, IconData>{
    NotifType.match: Icons.favorite,
    NotifType.message: Icons.chat_bubble_outline_rounded,
    NotifType.eventJoin: Icons.group_add,
    NotifType.eventApproval: Icons.check_circle_outline,
    NotifType.eventReminder: Icons.event,
    NotifType.resvUpdate: Icons.table_restaurant,
    NotifType.resvReminder: Icons.schedule,
    NotifType.levelUp: Icons.emoji_events,
    NotifType.couponExpiring: Icons.confirmation_number,
    NotifType.nearbyEvent: Icons.near_me,
    NotifType.marketing: Icons.campaign_outlined,
  };

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return Scaffold(
      backgroundColor: Q.background,
      appBar: AppBar(
        backgroundColor: Q.card,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.chevron_left, size: 26),
          color: Q.primary,
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text('Bildirişlər', style: T.h2.copyWith(fontSize: 18)),
        actions: [
          TextButton(
            onPressed: () => setState(() => showPrefs = !showPrefs),
            child: Text(showPrefs ? 'Siyahı' : 'Tercihlər',
                style: const TextStyle(fontSize: 11)),
          ),
        ],
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          if (showPrefs) return _prefs(state);
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
            children: [
              StaggerIn(
                index: 0,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('${state.unreadCount} oxunmamış',
                        style: T.subtitle),
                    if (state.unreadCount > 0)
                      TextButton(
                        onPressed: state.markAllNotificationsRead,
                        child: const Text('Hamısını oxunmuş et',
                            style: TextStyle(fontSize: 11)),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              if (state.notifications.isEmpty)
                const EmptyState(
                    icon: Icons.notifications_none,
                    text: 'Hələ bildiriş yoxdur.')
              else
                for (final n in state.notifications)
                  StaggerIn(
                    index: state.notifications.indexOf(n),
                    child: Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.all(13),
                    decoration: BoxDecoration(
                      color: n.read ? Q.card : Q.secondary,
                      border: Border.all(
                          color: n.read ? Q.border : Q.primary,
                          width: n.read ? 1 : 1.2),
                      borderRadius: BorderRadius.circular(Q.rLg),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(_icons[n.type] ?? Icons.notifications,
                            size: 18, color: Q.primary),
                        const SizedBox(width: 11),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(n.title,
                                        style: const TextStyle(
                                            fontFamily: T.family,
                                            fontSize: 12,
                                            fontWeight: FontWeight.w700)),
                                  ),
                                  Text(
                                      '${n.at.hour.toString().padLeft(2, '0')}:'
                                      '${n.at.minute.toString().padLeft(2, '0')}',
                                      style: T.metaSm),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(n.body,
                                  style: const TextStyle(
                                      fontFamily: T.family,
                                      fontSize: 10,
                                      height: 1.6,
                                      color: Q.mutedFg)),
                              const SizedBox(height: 4),
                              Text(n.type.label, style: T.metaSm),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  ),
            ],
          );
        },
      ),
    );
  }

  Widget _prefs(AppState state) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
      children: [
        const ViewHeading(
          title: 'Bildiriş tercihləri',
          subtitle: 'Hər tip ayrıca söndürülə bilər. Marketinq bildirişləri '
              'gecə 23:00-08:00 göndərilmir.',
        ),
        const SizedBox(height: 10),
        for (final t in NotifType.values)
          Container(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.only(left: 12, right: 4),
            decoration: BoxDecoration(
              color: Q.card,
              border: Border.all(color: Q.border),
              borderRadius: BorderRadius.circular(Q.rLg),
            ),
            child: Row(
              children: [
                Icon(_icons[t] ?? Icons.notifications,
                    size: 16, color: Q.mutedFg),
                const SizedBox(width: 11),
                Expanded(
                  child: Text(t.label,
                      style: const TextStyle(
                          fontFamily: T.family, fontSize: 12)),
                ),
                Switch(
                  value: state.notifPrefs.isEnabled(t),
                  onChanged: (v) => state.toggleNotifPref(t, v),
                  activeThumbColor: Q.primary,
                ),
              ],
            ),
          ),
      ],
    );
  }
}
