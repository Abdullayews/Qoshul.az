import 'package:flutter/material.dart';

import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';

/// Söhbətlər — web ChatsScreen portu.
/// Tədbir qrup söhbətləri + dost söhbətləri; seçiləndə çat görünüşü.
class ChatsScreen extends StatefulWidget {
  final AppState state;
  const ChatsScreen({super.key, required this.state});

  @override
  State<ChatsScreen> createState() => _ChatsScreenState();
}

class _ChatsScreenState extends State<ChatsScreen> {
  QoshulEvent? activeEvent;
  Person? activeFriend;

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        if (activeEvent != null) {
          return _ChatView(
            title: activeEvent!.title,
            subtitle: '${activeEvent!.participants} iştirakçı',
            image: activeEvent!.image,
            messages: state.eventMessages[activeEvent!.id] ?? const [],
            onSend: (t) => state.sendEventMessage(activeEvent!.id, t),
            onBack: () => setState(() => activeEvent = null),
            calendar: true,
          );
        }
        if (activeFriend != null) {
          return _ChatView(
            title: activeFriend!.fullName,
            subtitle: 'Şəxsi söhbət',
            image: activeFriend!.image,
            messages: state.friendMessages[activeFriend!.id] ?? const [],
            onSend: (t) => state.sendFriendMessage(activeFriend!.id, t),
            onBack: () => setState(() => activeFriend = null),
          );
        }
        return _listView(state);
      },
    );
  }

  Widget _listView(AppState state) {
    final joinedEvents =
        state.events.where((e) => state.joined.contains(e.id)).toList();
    final friends = state.acceptedFriends;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
      children: [
        const ViewHeading(title: 'Söhbətlər', subtitle: 'Görüşdən əvvəl tanış olaq.'),
        if (joinedEvents.isEmpty && friends.isEmpty)
          const EmptyState(
            icon: Icons.chat_bubble_outline_rounded,
            text:
                'İlk söhbətin səni gözləyir.\nTədbirə qoşulanda onun qrup söhbəti, '
                'dostluq qəbul edəndə isə şəxsi söhbət burada görünəcək.',
          )
        else ...[
          if (joinedEvents.isNotEmpty) ...[
            const SectionHeading(title: 'Tədbir söhbətləri', icon: Icons.event),
            for (final e in joinedEvents)
              _chatRow(
                image: e.image,
                title: e.title,
                subtitle:
                    '${e.participants} iştirakçı · Qrup söhbəti${e.past ? ' · keçib' : ''}',
                onTap: () => setState(() => activeEvent = e),
              ),
          ],
          if (friends.isNotEmpty) ...[
            const SizedBox(height: 16),
            const SectionHeading(title: 'Dost söhbətləri', icon: Icons.person),
            for (final p in friends)
              _chatRow(
                image: p.image,
                title: p.fullName,
                subtitle: '${p.city} · Şəxsi söhbət',
                onTap: () => setState(() => activeFriend = p),
              ),
          ],
        ],
      ],
    );
  }

  Widget _chatRow({
    required String image,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 18),
        child: Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(Q.rLg),
              child: Image.asset(image, width: 58, height: 58, fit: BoxFit.cover),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 13,
                          fontWeight: FontWeight.w800)),
                  const SizedBox(height: 5),
                  Text(subtitle,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 11,
                          color: Q.mutedFg)),
                ],
              ),
            ),
            Icon(Icons.chevron_right, size: 18, color: Q.mutedFg),
          ],
        ),
      ),
    );
  }
}

/// Tam çat görünüşü — başlıq + mesajlar + yazma zolağı (web ChatView portu).
class _ChatView extends StatefulWidget {
  final String title;
  final String subtitle;
  final String image;
  final List<ChatMessage> messages;
  final ValueChanged<String> onSend;
  final VoidCallback onBack;
  final bool calendar;

  const _ChatView({
    required this.title,
    required this.subtitle,
    required this.image,
    required this.messages,
    required this.onSend,
    required this.onBack,
    this.calendar = false,
  });

  @override
  State<_ChatView> createState() => _ChatViewState();
}

class _ChatViewState extends State<_ChatView> {
  final ctrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    ctrl.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    ctrl.dispose();
    super.dispose();
  }

  void _send() {
    if (ctrl.text.trim().isEmpty) return;
    widget.onSend(ctrl.text.trim());
    ctrl.clear();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // — çat başlığı —
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: const BoxDecoration(
            color: Q.card,
            border: Border(bottom: BorderSide(color: Q.border)),
          ),
          child: Row(
            children: [
              IconButton(
                onPressed: widget.onBack,
                icon: const Icon(Icons.chevron_left, size: 22),
                color: Q.primary,
              ),
              ClipRRect(
                borderRadius: BorderRadius.circular(Q.rMd),
                child:
                    Image.asset(widget.image, width: 40, height: 40, fit: BoxFit.cover),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(widget.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 13,
                            fontWeight: FontWeight.w800)),
                    Text(widget.subtitle, style: T.metaSm),
                  ],
                ),
              ),
              if (widget.calendar)
                IconButton(
                  onPressed: () {},
                  icon: const Icon(Icons.event, size: 20),
                  color: Q.foreground,
                  tooltip: 'Təqvimə əlavə et',
                ),
            ],
          ),
        ),
        // — mesajlar —
        Expanded(
          child: widget.messages.isEmpty
              ? Center(
                  child: Text('Hələ mesaj yoxdur. İlk mesajı sən yaz!',
                      style: T.subtitle))
              : ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                  children: [
                    for (final m in widget.messages)
                      ChatBubble(sender: m.sender, body: m.body, mine: m.mine),
                  ],
                ),
        ),
        // — yazma zolağı —
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: ctrl,
                  style: T.input,
                  onSubmitted: (_) => _send(),
                  decoration: const InputDecoration(hintText: 'Bir mesaj yaz...'),
                ),
              ),
              const SizedBox(width: 8),
              SizedBox(
                width: 42,
                height: 42,
                child: FilledButton(
                  onPressed: ctrl.text.trim().isEmpty ? null : _send,
                  style: FilledButton.styleFrom(
                      padding: EdgeInsets.zero, minimumSize: Size.zero),
                  child: const Icon(Icons.send, size: 16),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
