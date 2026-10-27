import 'package:flutter/material.dart';

import '../../core/models/notification.dart';
import '../../core/state/app_state.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/brand.dart';
import '../../core/widgets/ui.dart';
import '../assistant/presentation/assistant_screen.dart';
import '../chats/presentation/chats_screen.dart';
import '../events/presentation/create_event_sheet.dart';
import '../events/presentation/events_screen.dart';
import '../feed/presentation/feed_screen.dart';
import '../map/presentation/map_screen.dart';
import '../notifications/presentation/notifications_screen.dart';
import '../partners/presentation/partners_screen.dart';
import '../profile/presentation/profile_screen.dart';

/// Əsas tətbiq qabığı — ROL (product-spec C): 5 tab alt paneli.
/// Kəşf · Xəritə · Tədbirlər · [Yarat] · Söhbətlər · Profil
/// Header: wordmark + Tərəfdaşlar + AI köməkçi + bildiriş zili.
class AppShell extends StatefulWidget {
  final AppState state;
  const AppShell({super.key, required this.state});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int index = 0;

  void openCreate() {
    showCreateEventSheet(context, widget.state);
  }

  void openNotifications() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _NotificationsSheet(state: widget.state),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      FeedScreen(state: widget.state),
      MapScreen(state: widget.state),
      EventsScreen(state: widget.state),
      ChatsScreen(state: widget.state),
      ProfileScreen(state: widget.state),
    ];

    return Scaffold(
      backgroundColor: Q.background,
      body: AnimatedBuilder(
        animation: widget.state,
        builder: (context, _) => Column(
          children: [
            _Header(
              onPartners: () => _push(PartnersScreen(state: widget.state)),
              onAssistant: () => _push(AssistantScreen(state: widget.state)),
              onBell: openNotifications,
              unread: widget.state.unreadCount,
            ),
            Expanded(child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 200),
              child: KeyedSubtree(key: ValueKey(index), child: pages[index]),
            )),
          ],
        ),
      ),
      bottomNavigationBar: _BottomNav(
        index: index,
        onChanged: (i) => setState(() => index = i),
        onCreate: openCreate,
      ),
    );
  }

  void _push(Widget screen) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
  }
}

/// Bildirişlər paneli — ROL H: tiplər üzrə, oxunmamış sayı, tercih keçidi.
class _NotificationsSheet extends StatelessWidget {
  final AppState state;
  const _NotificationsSheet({required this.state});

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
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  width: 36,
                  height: 4,
                  margin: const EdgeInsets.only(bottom: 14),
                  decoration: BoxDecoration(
                      color: Q.border, borderRadius: BorderRadius.circular(2)),
                ),
                TextButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                    Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => NotificationsScreen(state: state)));
                  },
                  child: const Text('Tercihlər', style: TextStyle(fontSize: 11)),
                ),
              ],
            ),
            const Text('Bildirişlər',
                style: TextStyle(
                    fontFamily: T.family,
                    fontSize: 18,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text('Yeni match-lər, xatırlatmalar, rezerv yenilikləri.',
                style: T.subtitle),
            const SizedBox(height: 12),
            SizedBox(
              height: 320,
              child: state.notifications.isEmpty
                  ? const EmptyState(
                      icon: Icons.notifications_none,
                      text: 'Hələ bildiriş yoxdur.')
                  : ListView.separated(
                      shrinkWrap: true,
                      itemCount: state.notifications.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, i) {
                        final n = state.notifications[i];
                        return Container(
                          padding: const EdgeInsets.all(12),
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
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(n.title,
                                        style: const TextStyle(
                                            fontFamily: T.family,
                                            fontSize: 12,
                                            fontWeight: FontWeight.w700)),
                                    const SizedBox(height: 3),
                                    Text(n.body,
                                        style: const TextStyle(
                                            fontFamily: T.family,
                                            fontSize: 10,
                                            height: 1.6,
                                            color: Q.mutedFg)),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: state.unreadCount == 0
                    ? null
                    : () {
                        state.markAllNotificationsRead();
                        Navigator.of(context).pop();
                      },
                child: const Text('Hamısını oxunmuş et'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// .app-header — 67px, ağ, alt haşiyə.
class _Header extends StatelessWidget {
  final VoidCallback onPartners;
  final VoidCallback onAssistant;
  final VoidCallback onBell;
  final int unread;

  const _Header({
    required this.onPartners,
    required this.onAssistant,
    required this.onBell,
    required this.unread,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 67,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      decoration: const BoxDecoration(
        color: Q.card,
        border: Border(bottom: BorderSide(color: Q.border)),
      ),
      child: Row(
        children: [
          const Wordmark(fontSize: 25),
          const Spacer(),
          IconButton(
            onPressed: onPartners,
            icon: const Icon(Icons.handshake, size: 19),
            color: Q.foreground,
            tooltip: 'Tərəfdaşlar',
          ),
          IconButton(
            onPressed: onAssistant,
            icon: const Icon(Icons.smart_toy, size: 20),
            color: Q.foreground,
            tooltip: 'AI köməkçi',
          ),
          Stack(
            children: [
              IconButton(
                onPressed: onBell,
                icon: const Icon(Icons.notifications_none, size: 20),
                color: Q.foreground,
                tooltip: 'Bildirişlər',
              ),
              if (unread > 0)
                Positioned(
                  top: 8,
                  right: 8,
                  child: Container(
                    padding: const EdgeInsets.all(3),
                    decoration: BoxDecoration(
                      color: Q.accent,
                      shape: BoxShape.circle,
                      border: Border.all(color: Q.card, width: 1),
                    ),
                    child: Text(
                      '$unread',
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 7,
                          fontWeight: FontWeight.w800,
                          color: Colors.white),
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// .bottom-nav — ROL 5 tab + mərkəzdə Yarat.
class _BottomNav extends StatelessWidget {
  final int index;
  final ValueChanged<int> onChanged;
  final VoidCallback onCreate;

  const _BottomNav({required this.index, required this.onChanged, required this.onCreate});

  static const _items = [
    (Icons.home_outlined, 'Kəşf'),
    (Icons.map_outlined, 'Xəritə'),
    (Icons.event_outlined, 'Tədbirlər'),
    (Icons.chat_bubble_outline_rounded, 'Söhbətlər'),
    (Icons.person_outline_rounded, 'Profil'),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 77,
      padding: const EdgeInsets.only(left: 4, right: 4, bottom: 8),
      decoration: BoxDecoration(
        color: Q.card.withValues(alpha: 0.96),
        border: const Border(top: BorderSide(color: Q.border)),
        boxShadow: [
          BoxShadow(
            color: const Color(0x0A28301F),
            blurRadius: 24,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: Row(
        children: [
          for (var i = 0; i < _items.length; i++) ...[
            if (i == 2) _createButton(),
            Expanded(child: _navItem(i)),
          ],
        ],
      ),
    );
  }

  Widget _createButton() {
    return GestureDetector(
      onTap: onCreate,
      behavior: HitTestBehavior.opaque,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Transform.translate(
              offset: const Offset(0, -5),
              child: Container(
                width: 43,
                height: 43,
                decoration: BoxDecoration(
                  color: Q.primary,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: Q.primary.withValues(alpha: 0.2),
                      blurRadius: 10,
                      offset: const Offset(0, 3),
                    ),
                  ],
                ),
                child: const Icon(Icons.add, color: Q.primaryFg, size: 22),
              ),
            ),
            Transform.translate(
              offset: const Offset(0, -1),
              child: Text('Yarat',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 9,
                      color: Q.mutedFg)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _navItem(int i) {
    final active = index == i;
    final (icon, label) = _items[i];
    return GestureDetector(
      onTap: () => onChanged(i),
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon,
              size: 20,
              color: active ? Q.primary : Q.mutedFg,
              weight: active ? 700 : 400),
          const SizedBox(height: 5),
          Stack(
            clipBehavior: Clip.none,
            children: [
              Text(
                label,
                style: TextStyle(
                  fontFamily: T.family,
                  fontSize: 9,
                  fontWeight: active ? FontWeight.w700 : FontWeight.w400,
                  color: active ? Q.primary : Q.mutedFg,
                ),
              ),
              if (active)
                Positioned(
                  bottom: -5,
                  left: 0,
                  right: 0,
                  child: Center(
                    child: Container(
                      width: 4,
                      height: 4,
                      decoration: BoxDecoration(
                          color: Q.primary, shape: BoxShape.circle),
                    ),
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
