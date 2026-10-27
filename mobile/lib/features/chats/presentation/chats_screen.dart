// ignore_for_file: deprecated_member_use
import 'package:flutter/material.dart';

import '../../../core/models/moderation.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/ui.dart';

/// Söhbətlər — ROL (product-spec C, TAB 4):
/// "Şəxsi" (match / dost) + "Qruplar" (tədbir söhbətləri).
/// Şəxsi chat yalnız match olunmuş və ya eyni tədbir iştirakçıları ilə.
/// Hər söhbətdə: Report et, Blokla, Söhbəti sil.
class ChatsScreen extends StatefulWidget {
  final AppState state;
  const ChatsScreen({super.key, required this.state});

  @override
  State<ChatsScreen> createState() => _ChatsScreenState();
}

class _ChatsScreenState extends State<ChatsScreen> {
  QoshulEvent? activeEvent;
  Person? activeFriend;
  String _lastFriendName = '';

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
            friendActions: true,
            onBlock: () {
              state.blockUser(activeFriend!.id);
              setState(() => activeFriend = null);
              ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                  content: Text(
                      '${_lastFriendName} bloklandı — söhbət bağlandı.')));
            },
            onDelete: () {
              state.deleteChat(activeFriend!.id);
              setState(() => activeFriend = null);
            },
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
      // alt 128px: şüşə nav pill altından axış (extendBody, D-025)
      padding: const EdgeInsets.fromLTRB(20, 21, 20, 128),
      children: [
        StaggerIn(
          index: 0,
          child: const ViewHeading(
              title: 'Söhbətlər', subtitle: 'Görüşdən əvvəl tanış olaq.'),
        ),
        if (joinedEvents.isEmpty && friends.isEmpty)
          const EmptyState(
            icon: Icons.chat_bubble_outline_rounded,
            text:
                'İlk söhbətin səni gözləyir.\nTədbirə qoşulanda onun qrup söhbəti, '
                'dostluq qəbul edəndə isə şəxsi söhbət burada görünəcək.',
          )
        else ...[
          if (joinedEvents.isNotEmpty) ...[
            const SectionHeading(title: 'Qruplar', icon: Icons.groups),
            for (final e in joinedEvents)
              StaggerIn(
                index: joinedEvents.indexOf(e),
                child: _chatRow(
                  image: e.image,
                  title: e.title,
                  subtitle:
                      '${e.participants} iştirakçı · Tədbir qrupu${e.past ? ' · keçib' : ''}',
                  onTap: () => setState(() => activeEvent = e),
                ),
              ),
          ],
          if (friends.isNotEmpty) ...[
            const SizedBox(height: 16),
            const SectionHeading(title: 'Şəxsi', icon: Icons.person),
            for (final p in friends)
              StaggerIn(
                index: friends.indexOf(p),
                child: _chatRow(
                  image: p.image,
                  title: p.fullName,
                  subtitle:
                      '${p.city} · ${state.matches.contains(p.id) ? 'Match' : 'Dost'} · Şəxsi',
                  onTap: () => setState(() {
                    activeFriend = p;
                    _lastFriendName = p.fullName;
                  }),
                ),
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

/// Tam çat görünüşü — ROL TAB 4: başlıq + mesajlar (oxunma statusu ilə) +
/// "yazır..." göstəricisi + yazma zolağı + Report/Blok/Sil menyu.
class _ChatView extends StatefulWidget {
  final String title;
  final String subtitle;
  final String image;
  final List<ChatMessage> messages;
  final ValueChanged<String> onSend;
  final VoidCallback onBack;
  final bool calendar;
  final bool friendActions;
  final VoidCallback? onBlock;
  final VoidCallback? onDelete;

  const _ChatView({
    required this.title,
    required this.subtitle,
    required this.image,
    required this.messages,
    required this.onSend,
    required this.onBack,
    this.calendar = false,
    this.friendActions = false,
    this.onBlock,
    this.onDelete,
  });

  @override
  State<_ChatView> createState() => _ChatViewState();
}

class _ChatViewState extends State<_ChatView> {
  final ctrl = TextEditingController();
  bool typing = false;

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
    // "yazır..." göstəricisi (mock: cavab 1.2 s sonra gəlir)
    setState(() => typing = true);
    Future.delayed(const Duration(milliseconds: 1400), () {
      if (mounted) setState(() => typing = false);
    });
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
                    if (typing)
                      // Yazır göstəricisi: canlı nöqtə dalğası (D-025).
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          TypingDots(color: Q.primary, size: 4),
                          const SizedBox(width: 5),
                          Text('yazır...',
                              style: T.metaSm.copyWith(color: Q.primary)),
                        ],
                      ),
                  ],
                ),
              ),
              if (widget.friendActions)
                PopupMenuButton<String>(
                  icon: const Icon(Icons.more_vert, size: 20),
                  color: Q.foreground,
                  onSelected: (v) {
                    if (v == 'report') {
                      _reportDialog();
                    } else if (v == 'block') {
                      widget.onBlock?.call();
                    } else if (v == 'delete') {
                      widget.onDelete?.call();
                    }
                  },
                  itemBuilder: (_) => const [
                    PopupMenuItem(
                        value: 'report',
                        child: Text('Report et',
                            style:
                                TextStyle(fontFamily: T.family, fontSize: 12))),
                    PopupMenuItem(
                        value: 'block',
                        child: Text('Blokla',
                            style:
                                TextStyle(fontFamily: T.family, fontSize: 12))),
                    PopupMenuItem(
                        value: 'delete',
                        child: Text('Söhbəti sil',
                            style:
                                TextStyle(fontFamily: T.family, fontSize: 12))),
                  ],
                )
              else if (widget.calendar)
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
                  padding: const EdgeInsets.symmetric(
                      horizontal: 20, vertical: 8),
                  children: [
                    for (final m in widget.messages)
                      // Yeni mesajlar pop şəklində girir (D-025).
                      StaggerIn(
                        index: widget.messages.indexOf(m),
                        child: ChatBubble(
                          sender: m.sender,
                          body: m.body,
                          mine: m.mine,
                          status: m.mine
                              ? (m == widget.messages.lastOrNull && typing
                                  ? 'çatdırıldı'
                                  : 'oxundu')
                              : null,
                        ),
                      ),
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
  void _reportDialog() {
    String reason = reportReasons.first;
    showDialog<void>(
      context: context,
      builder: (d) => StatefulBuilder(
        builder: (d, setD) => AlertDialog(
          title: const Text('Report et'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (final r in reportReasons)
                RadioListTile<String>(
                  value: r,
                  groupValue: reason,
                  onChanged: (v) => setD(() => reason = v!),
                  title: Text(r,
                      style: const TextStyle(fontFamily: T.family, fontSize: 12)),
                  contentPadding: EdgeInsets.zero,
                  dense: true,
                  activeColor: Q.primary,
                ),
            ],
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(d), child: const Text('Ləğv et')),
            FilledButton(
              onPressed: () {
                Navigator.pop(d);
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                    content: Text(
                        'Report göndərildi — moderasiya komandası baxacaq.')));
              },
              child: const Text('Göndər'),
            ),
          ],
        ),
      ),
    );
  }
}
