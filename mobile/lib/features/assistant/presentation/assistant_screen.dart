import 'package:flutter/material.dart';

import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../events/presentation/create_event_sheet.dart';

/// AI köməkçi — web AssistantScreen portu.
/// Təklif çipləri, axın simulyasiyalı mesajlar, [TƏDBİR] təklif kartı.
class AssistantScreen extends StatefulWidget {
  final AppState state;
  const AssistantScreen({super.key, required this.state});

  @override
  State<AssistantScreen> createState() => _AssistantScreenState();
}

class _AssistantScreenState extends State<AssistantScreen> {
  final ctrl = TextEditingController();

  @override
  void dispose() {
    ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    final suggestions = [
      'Bu həftə sonu ${state.city}da nə var?',
      'Pulsuz idman tədbirləri hansılardır?',
      'Gəncədə tanışlıq üçün nə tövsiyə edərsən?',
      'Şənbə üçün tədbir fikri ver',
    ];

    return Scaffold(
      backgroundColor: Q.background,
      appBar: AppBar(
        backgroundColor: Q.card,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        leading: IconButton(
          icon: const Icon(Icons.chevron_left, size: 26),
          color: Q.primary,
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Row(
          children: [
            Icon(Icons.smart_toy, size: 20, color: Q.primary),
            const SizedBox(width: 8),
            Text('Qoshul köməkçisi',
                style: T.h2.copyWith(fontSize: 18)),
          ],
        ),
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          final msgs = state.assistantMsgs;
          // Son AI cavabından təklif çıxar (web assistantEvent).
          EventProposal? proposal;
          for (final m in msgs.reversed) {
            if (!m.mine) {
              proposal = parseAssistantEvent(m.body).proposal;
              break;
            }
          }

          return Column(
            children: [
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                  children: [
                    if (msgs.isEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 4, bottom: 8),
                        child: Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            for (final s in suggestions)
                              OutlinedButton(
                                onPressed: () => state.sendAssistant(s),
                                style: OutlinedButton.styleFrom(
                                  backgroundColor: Q.secondary,
                                  side: BorderSide.none,
                                  foregroundColor: Q.primary,
                                  minimumSize: const Size(0, 34),
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 12),
                                  shape: RoundedRectangleBorder(
                                    borderRadius:
                                        BorderRadius.circular(Q.rMd),
                                  ),
                                ),
                                child: Text(s,
                                    style: const TextStyle(
                                        fontFamily: T.family,
                                        fontSize: 11)),
                              ),
                          ],
                        ),
                      ),
                    for (final m in msgs)
                      ChatBubble(
                        sender: m.mine ? 'Sən' : 'Köməkçi',
                        body: parseAssistantEvent(m.body).text,
                        mine: m.mine,
                      ),
                    if (state.assistantBusy)
                      const Padding(
                        padding: EdgeInsets.only(top: 14),
                        child: Align(
                          alignment: Alignment.centerLeft,
                          child: SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          ),
                        ),
                      ),
                    if (proposal != null && !state.assistantBusy) ...[
                      AiBox(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Təsdiqləndi: ${proposal.title}',
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600)),
                            const SizedBox(height: 6),
                            Text(
                              '${proposal.date} ${proposal.time} · ${proposal.location} · ${proposal.capacity} nəfər',
                              style: const TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 11,
                                  color: Q.mutedFg),
                            ),
                            const SizedBox(height: 10),
                            FilledButton.icon(
                              onPressed: () => showCreateEventSheet(
                                context,
                                state,
                                initial: proposal,
                              ),
                              icon: const Icon(Icons.add, size: 14),
                              label: const Text('Bu tədbiri yarat',
                                  style: TextStyle(
                                      fontFamily: T.family,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w600)),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              // — yazma zolağı —
              Container(
                padding: const EdgeInsets.fromLTRB(20, 10, 20, 16),
                decoration: const BoxDecoration(
                  color: Q.card,
                  border: Border(top: BorderSide(color: Q.border)),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: ctrl,
                        style: T.input,
                        maxLength: 1000,
                        onSubmitted: (_) {
                          state.sendAssistant(ctrl.text);
                          ctrl.clear();
                        },
                        decoration: const InputDecoration(
                            hintText: 'Sualını yaz...', counterText: ''),
                      ),
                    ),
                    const SizedBox(width: 8),
                    SizedBox(
                      width: 42,
                      height: 42,
                      child: FilledButton(
                        onPressed: state.assistantBusy || ctrl.text.trim().isEmpty
                            ? null
                            : () {
                                state.sendAssistant(ctrl.text);
                                ctrl.clear();
                              },
                        style: FilledButton.styleFrom(
                            padding: EdgeInsets.zero, minimumSize: Size.zero),
                        child: Icon(
                          state.assistantBusy ? Icons.stop : Icons.send,
                          size: 16,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
