import 'package:flutter/material.dart';

import '../../core/state/app_state.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/brand.dart';
import '../../core/widgets/ui.dart';
import '../assistant/presentation/assistant_screen.dart';
import '../chats/presentation/chats_screen.dart';
import '../events/presentation/create_event_sheet.dart';
import '../explore/presentation/explore_screen.dart';
import '../feed/presentation/feed_screen.dart';
import '../partners/presentation/partners_screen.dart';
import '../profile/presentation/profile_screen.dart';

/// Əsas tətbiq qabığı — web .app-shell portu:
/// header (wordmark + Tərəfdaşlar/AI/Bell) + 4 tab + mərkəzdə "Yarat" düyməsi.
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
    final s = widget.state;
    showModalBottomSheet<void>(
      context: context,
      builder: (_) => _NotificationsSheet(state: s),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      FeedScreen(state: widget.state),
      ExploreScreen(state: widget.state),
      ChatsScreen(state: widget.state),
      ProfileScreen(state: widget.state),
    ];

    return Scaffold(
      backgroundColor: Q.background,
      body: Column(
        children: [
          _Header(
            onPartners: () => _push(PartnersScreen(state: widget.state)),
            onAssistant: () => _push(AssistantScreen(state: widget.state)),
            onBell: openNotifications,
          ),
          Expanded(child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 200),
            child: KeyedSubtree(key: ValueKey(index), child: pages[index]),
          )),
        ],
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

/// Bildirişlər paneli — web Shell-dəki Dialog porta.
class _NotificationsSheet extends StatelessWidget {
  final AppState state;
  const _NotificationsSheet({required this.state});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 36,
              height: 4,
              margin: const EdgeInsets.only(bottom: 14),
              decoration: BoxDecoration(
                  color: Q.border, borderRadius: BorderRadius.circular(2)),
            ),
            const Text('Bildirişlər',
                style: TextStyle(
                    fontFamily: T.family,
                    fontSize: 18,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text('Yeni görüşlər, yeni imkanlar.', style: T.subtitle),
            const CommunityBanner(
              icon: Icons.verified_user,
              title: 'Qoshul.az-a xoş gəlmisən!',
              subtitle: 'Şəhərindəki insanlarla tanış olmaq üçün ilk tədbirinə qoşul.',
            ),
            if (state.joined.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  '${state.joinedUpcomingCount} tədbirə qoşulmusan. Qrup söhbətlərində iştirakçılarla tanış ola bilərsən.',
                  style: const TextStyle(
                      fontFamily: T.family,
                      fontSize: 12,
                      height: 1.8,
                      color: Q.mutedFg),
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

  const _Header({required this.onPartners, required this.onAssistant, required this.onBell});

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
          TextButton(
            onPressed: onPartners,
            style: TextButton.styleFrom(
              foregroundColor: Q.mutedFg,
              padding: const EdgeInsets.symmetric(horizontal: 4),
              minimumSize: Size.zero,
            ),
            child: Row(
              children: const [
                Icon(Icons.handshake, size: 13),
                SizedBox(width: 4),
                Text('Tərəfdaşlar',
                    style: TextStyle(fontFamily: T.family, fontSize: 11)),
              ],
            ),
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
              Positioned(
                top: 9,
                right: 10,
                child: Container(
                  width: 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: Q.accent,
                    shape: BoxShape.circle,
                    border: Border.all(color: Q.card, width: 1),
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

/// .bottom-nav — 77px, blur-a yaxın ağ, üst haşiyə, mərkəzdə Yarat.
class _BottomNav extends StatelessWidget {
  final int index;
  final ValueChanged<int> onChanged;
  final VoidCallback onCreate;

  const _BottomNav({required this.index, required this.onChanged, required this.onCreate});

  static const _items = [
    (Icons.home_outlined, 'Ana səhifə'),
    (Icons.explore_outlined, 'Kəşf et'),
    (Icons.chat_bubble_outline_rounded, 'Söhbətlər'),
    (Icons.person_outline_rounded, 'Profil'),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 77,
      padding: const EdgeInsets.only(left: 7, right: 7, bottom: 8),
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
        padding: const EdgeInsets.symmetric(horizontal: 10),
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
              size: 21,
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
