import 'package:flutter/material.dart';

import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import 'finish_activity_sheet.dart';

/// Tədbir detalı — web EventCard-dakı Dialog-un mobil portu (bottom sheet).
Future<void> showEventDetailSheet(
    BuildContext context, AppState state, QoshulEvent event) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _DetailSheet(state: state, event: event),
  );
}

class _DetailSheet extends StatefulWidget {
  final AppState state;
  final QoshulEvent event;

  const _DetailSheet({required this.state, required this.event});

  @override
  State<_DetailSheet> createState() => _DetailSheetState();
}

class _DetailSheetState extends State<_DetailSheet> {
  int rating = 5;
  final commentCtrl = TextEditingController();
  bool reviewSent = false;

  @override
  void dispose() {
    commentCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final event = widget.event;

    return DraggableScrollableSheet(
      initialChildSize: 0.85,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      expand: false,
      builder: (context, scrollController) {
        return AnimatedBuilder(
          animation: widget.state,
          builder: (context, _) {
            final state = widget.state;
            final isJoined = state.joined.contains(event.id);
            final isPending = state.pending.contains(event.id);
            return ListView(
            controller: scrollController,
            children: [
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  margin: const EdgeInsets.only(top: 10, bottom: 14),
                  decoration: BoxDecoration(
                      color: Q.border, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              ClipRRect(
                borderRadius: BorderRadius.circular(Q.rMd),
                child: ShadedImage(asset: event.image, height: 180),
              ),
              const SizedBox(height: 14),
              Text(event.title, style: T.h2),
              const SizedBox(height: 4),
              Text(
                '${event.host} tərəfindən təşkil olunur'
                '${event.approval ? ' · qoşulma təsdiq tələb edir' : ''}',
                style: T.subtitle,
              ),
              const SizedBox(height: 10),
              Text(
                event.description,
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 12,
                    height: 1.8,
                    color: Q.mutedFg),
              ),
              const SizedBox(height: 12),
              _facts(event),
              // D-026 (L.3): təşkilatçı bitirmə axını — statik QR + şəkil + taq.
              if (state.canFinishActivity(event)) ...[
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    style: FilledButton.styleFrom(backgroundColor: Q.primary),
                    onPressed: () {
                      Navigator.of(context).pop();
                      showFinishActivitySheet(context, state, event);
                    },
                    icon: const Icon(Icons.workspace_premium, size: 16),
                    label: Text(
                        'Aktiviteyi bitir və XP payla (+${XpRules.organizerXp})',
                        style: T.button),
                  ),
                ),
              ] else if (state.isActivityFinished(event.id)) ...[
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: null,
                    style: FilledButton.styleFrom(
                      backgroundColor: Q.secondary,
                      disabledBackgroundColor: Q.secondary,
                      disabledForegroundColor: Q.primary,
                    ),
                    icon: const Icon(Icons.check_circle, size: 16),
                    label: Text(
                        'Aktivite bitib — XP paylandı (+${XpRules.organizerXp} təşkilatçı)',
                        style: T.button),
                  ),
                ),
              ],
              if (isJoined && !event.past)
                SizedBox(
                  width: double.infinity,
                  child: state.checkedIn.contains(event.id)
                      ? FilledButton.icon(
                          onPressed: null,
                          style: FilledButton.styleFrom(
                            backgroundColor: Q.secondary,
                            disabledBackgroundColor: Q.secondary,
                            disabledForegroundColor: Q.primary,
                          ),
                          icon: const Icon(Icons.check_circle, size: 16),
                          label: Text(
                              'Check-in edilib (+${XpRules.checkInXp} XP qazanıldı)',
                              style: T.button),
                        )
                      : FilledButton.icon(
                          onPressed: () {
                            final ok = state.checkInEvent(event.id);
                            if (ok) {
                              ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                                content: Text(
                                    'Check-in uğurlu! +${XpRules.checkInXp} XP qazandın.'),
                              ));
                            }
                          },
                          icon: const Icon(Icons.where_to_vote_outlined, size: 16),
                          label: Text(
                              'Yerində check-in et (+${XpRules.checkInXp} XP)',
                              style: T.button),
                        ),
                ),
              if (isJoined && event.past) _reviewForm(event),
              if (event.format != 'Onlayn') ...[
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.near_me, size: 16),
                    label: Text('Yol göstər (${event.distance} km)',
                        style: T.button),
                  ),
                ),
              ],
              const SizedBox(height: 8),
              if (isJoined && !event.past)
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: () {
                      state.leave(event.id);
                      Navigator.of(context).pop();
                    },
                    icon: const Icon(Icons.logout, size: 16),
                    label: Text('Tədbirdən çıx', style: T.button),
                  ),
                ),
              if (!event.past) ...[
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: () {
                    if (isJoined) {
                      return FilledButton.icon(
                        onPressed: () {},
                        icon: const Icon(Icons.north_east, size: 16),
                        label: Text('Qrup söhbətinə keç', style: T.button),
                      );
                    }
                    if (isPending) {
                      return FilledButton.icon(
                        onPressed: null,
                        style: FilledButton.styleFrom(
                          backgroundColor: Q.secondary,
                          disabledBackgroundColor: Q.secondary,
                          disabledForegroundColor: Q.primary,
                        ),
                        icon: const Icon(Icons.hourglass_empty, size: 16),
                        label: Text('Sorğun təşkilatçıya göndərildi',
                            style: T.button),
                      );
                    }
                    if (event.isFull) {
                      return const FilledButton(
                        onPressed: null,
                        child: Text('Yer yoxdur'),
                      );
                    }
                    return FilledButton.icon(
                      onPressed: () {
                        final ok = state.join(event.id);
                        if (ok) {
                          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                            content: Text(event.approval
                                ? 'Sorğun təşkilatçıya göndərildi.'
                                : 'Tədbirə qoşuldun! +${XpRules.joinXp} XP'),
                          ));
                        }
                      },
                      icon: const Icon(Icons.north_east, size: 16),
                      label: Text(
                          event.approval ? 'Qoşulma istəyi göndər' : 'Tədbirə qoşul',
                          style: T.button),
                    );
                  }(),
                ),
              ],
              const SizedBox(height: 24),
            ],
          );
          },
        );
      },
    );
  }

  Widget _facts(QoshulEvent e) {
    final rows = <(IconData, String)>[
      (Icons.event, '${e.date}, ${e.time}'),
      (Icons.group, '${e.spotsLeft} boş yer'),
      (Icons.location_on, e.location),
      (Icons.emoji_events, '+${XpRules.joinXp} XP qoşulma'),
      (Icons.payments, e.price != null ? '${e.price} AZN' : 'Pulsuz'),
      if (e.level != null) (Icons.signal_cellular_alt, e.level!),
      if (e.language != null) (Icons.translate, e.language!),
    ];
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rLg),
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          for (var i = 0; i < rows.length; i += 2)
            Row(
              children: [
                Expanded(child: _fact(rows[i])),
                if (i + 1 < rows.length)
                  Expanded(child: _fact(rows[i + 1])),
              ],
            ),
        ],
      ),
    );
  }

  Widget _fact((IconData, String) r) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: [
          Icon(r.$1, size: 12, color: Q.mutedFg),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              r.$2,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                  fontFamily: T.family,
                  fontSize: 12.5,
                  color: Q.mutedFg),
            ),
          ),
        ],
      ),
    );
  }

  /// Keçmiş + qoşululmuş tədbir üçün rəy forması (web ReviewForm portu).
  Widget _reviewForm(QoshulEvent e) {
      if (reviewSent) {
        return Padding(
          padding: const EdgeInsets.only(top: 14),
          child: Row(
            children: [
              const Icon(Icons.check, size: 12, color: Q.primary),
              const SizedBox(width: 6),
              Text('Rəyin üçün təşəkkürlər! (+${XpRules.reviewXp} XP)',
                  style: const TextStyle(fontFamily: T.family, fontSize: 12.5, color: Q.mutedFg)),
            ],
          ),
        );
      }
    return Padding(
      padding: const EdgeInsets.only(top: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Tədbiri qiymətləndir',
              style: TextStyle(
                  fontFamily: T.family,
                  fontSize: 13,
                  fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Row(
            children: [
              for (var n = 1; n <= 5; n++)
                GestureDetector(
                  onTap: () => setState(() => rating = n),
                  child: Padding(
                    padding: const EdgeInsets.all(3),
                    child: Icon(
                      Icons.star,
                      size: 22,
                      color: n <= rating ? Q.primary : Q.mutedFg,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          TextField(
            controller: commentCtrl,
            maxLines: 2,
            maxLength: 1000,
            style: T.input,
            decoration: const InputDecoration(
              hintText: 'Təcrübən bir cümlə ilə bölüş (istəyə görə)',
            ),
          ),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: () {
              widget.state.addReview(e.id, rating, commentCtrl.text);
              setState(() => reviewSent = true);
            },
            child: const Text('Rəyi göndər'),
          ),
        ],
      ),
    );
  }
}
