import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/models/xp_economy.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';

/// Təşkilatçı bitirmə axını — D-026 (product-spec L.3).
///
/// Aktivite bitəndə: obyektin STATİK QR-ı oxunur (çıxışda) → şəkilləri
/// yalnız təşkilatçı paylaşır (obyektin biznes hesabına düşür) →
/// yalnız iştirakçılar taq edilir → təşkilatçı +50, hər taq edilən +25 XP.
/// Taq edilməyən iştirakçı XP almır. Məbləğlər xp_config ilə tənzimlənəndir.
Future<void> showFinishActivitySheet(
    BuildContext context, AppState state, QoshulEvent event) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _FinishSheet(state: state, event: event),
  );
}

class _FinishSheet extends StatefulWidget {
  final AppState state;
  final QoshulEvent event;

  const _FinishSheet({required this.state, required this.event});

  @override
  State<_FinishSheet> createState() => _FinishSheetState();
}

class _FinishSheetState extends State<_FinishSheet> {
  int step = 0; // 0: statik QR · 1: şəkil · 2: taq · 3: nəticə
  final Set<String> tagged = <String>{};
  final Set<int> sharedPhotos = <int>{};
  ActivityXpResult? result;

  /// Mock iştirakçı siyahısı — real sistemdə event_participants cədvəlindən.
  List<Person> get participants {
    final n = widget.event.participants.clamp(1, mockPeople.length);
    return mockPeople.take(n).toList(growable: false);
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    final acct = state.venueXp[widget.event.venueId ?? ''];
    return DraggableScrollableSheet(
      initialChildSize: 0.75,
      minChildSize: 0.4,
      maxChildSize: 0.95,
      expand: false,
      builder: (context, scrollController) {
        return AnimatedBuilder(
          animation: state,
          builder: (context, _) => ListView(
            controller: scrollController,
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
            children: [
              Center(
                child: Container(
                  width: 36,
                  height: 4,
                  margin: const EdgeInsets.only(bottom: 14),
                  decoration: BoxDecoration(
                      color: Q.border, borderRadius: BorderRadius.circular(2)),
                ),
              ),
              FadeThrough(
                child: KeyedSubtree(
                  key: ValueKey(step),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Aktiviteyi bitir — "${widget.event.title}"',
                          style: T.h2.copyWith(fontSize: 15)),
                      const SizedBox(height: 4),
                      Text(
                        'Obyektin statik QR-ı → şəkil paylaş → iştirakçıları taq et → '
                        'Endirim XP paylanır (təşkilatçı +${XpRules.organizerXp}, '
                        'hər taq +${XpRules.taggedParticipantXp}).',
                        style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
                      ),
                      const SizedBox(height: 16),
                      if (acct == null)
                        const EmptyState(
                            icon: Icons.error_outline,
                            text: 'Bu tədbir partnyor obyektinə bağlı deyil.')
                      else if (step == 0) _stepQr(acct),
                      if (acct != null && step == 1) _stepPhotos(),
                      if (acct != null && step == 2) _stepTag(),
                      if (acct != null && step == 3) _stepResult(acct),
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  // — Addım 1: obyektin STATİK QR-ı (çıxışda təşkilatçı oxuyur) —
  Widget _stepQr(VenueXpAccount acct) {
    return Column(
      children: [
        Center(
          child: PopIn(
            child: QrPlaceholder(
                code: 'STATIC-${acct.venueId}-${acct.venueName}', size: 148),
          ),
        ),
        const SizedBox(height: 12),
        Center(
          child: Text(
            '${acct.venueName} · obyektin statik QR-ı.\n'
            'Kassada və ya çıxışda oxu — aktivite obyektlə əlaqələnir.',
            textAlign: TextAlign.center,
            style: T.meta.copyWith(fontSize: 11.5, height: 1.6),
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: Q.primary),
            onPressed: () => setState(() => step = 1),
            icon: const Icon(Icons.qr_code_scanner, size: 16),
            label: const Text('QR-ı oxudum', style: T.button),
          ),
        ),
      ],
    );
  }

  // — Addım 2: şəkil paylaşımı (yalnız təşkilatçı!) —
  Widget _stepPhotos() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Şəkilləri paylaş',
            style: T.meta.copyWith(fontSize: 12.5, fontWeight: FontWeight.w700)),
        const SizedBox(height: 4),
        Text(
          'Şəkli yalnız SƏN (təşkilatçı) paylaşa bilərsən — Aktivite şəkilləri '
          'obyektin biznes hesabına düşür (İnstagram-model).',
          style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            for (var i = 0; i < 3; i++)
              Expanded(
                child: ScaleTap(
                  onTap: () => setState(() {
                    if (!sharedPhotos.remove(i)) sharedPhotos.add(i);
                  }),
                  child: AnimatedContainer(
                    duration: Motion.d3,
                    curve: Motion.ease,
                    margin: EdgeInsets.only(right: i < 2 ? 10 : 0),
                    height: 92,
                    decoration: BoxDecoration(
                      color: sharedPhotos.contains(i)
                          ? Q.secondary
                          : Q.muted,
                      borderRadius: BorderRadius.circular(Q.rMd),
                      border: Border.all(
                          color: sharedPhotos.contains(i)
                              ? Q.primary
                              : Q.border,
                          width: sharedPhotos.contains(i) ? 2 : 1),
                    ),
                    child: sharedPhotos.contains(i)
                        ? const Icon(Icons.check_circle,
                            size: 30, color: Q.primary)
                        : Icon(Icons.add_photo_alternate_outlined,
                            size: 26, color: Q.mutedFg.withValues(alpha: 0.7)),
                  ),
                ),
              ),
          ],
        ),
        const SizedBox(height: 14),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(
                backgroundColor: Q.primary,
                disabledBackgroundColor: Q.muted),
            onPressed: sharedPhotos.isEmpty
                ? null
                : () => setState(() => step = 2),
            icon: const Icon(Icons.share, size: 16),
            label: Text(
                sharedPhotos.isEmpty
                    ? 'Ən azı bir şəkil seç'
                    : '${sharedPhotos.length} şəkli biznes hesabına paylaş',
                style: T.button),
          ),
        ),
      ],
    );
  }

  // — Addım 3: iştirakçıları taq et (yalnız taq edilənlər XP alır) —
  Widget _stepTag() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('İştirakçıları taq et',
            style: T.meta.copyWith(fontSize: 12.5, fontWeight: FontWeight.w700)),
        const SizedBox(height: 4),
        Text(
          'Taq edilməyən iştirakçı XP almır. Hər taq edilənə '
          '+${XpRules.taggedParticipantXp} XP, sənə təşkilatçı kimi '
          '+${XpRules.organizerXp} XP.',
          style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
        ),
        const SizedBox(height: 12),
        for (final p in participants)
          ScaleTap(
            onTap: () => setState(() {
              if (!tagged.remove(p.id)) tagged.add(p.id);
            }),
            child: AnimatedContainer(
              duration: Motion.d3,
              curve: Motion.ease,
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: tagged.contains(p.id) ? Q.secondary : Q.card,
                borderRadius: BorderRadius.circular(Q.rMd),
                border: Border.all(
                    color: tagged.contains(p.id) ? Q.primary : Q.border,
                    width: tagged.contains(p.id) ? 2 : 1),
              ),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 16,
                    backgroundColor: Q.muted,
                    child: Text(p.firstName[0],
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 12,
                            fontWeight: FontWeight.w800)),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(p.fullName,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 12,
                                fontWeight: FontWeight.w700)),
                        Text('${p.distanceLabel} · ortaq maraq',
                            style: T.meta.copyWith(fontSize: 10.5)),
                      ],
                    ),
                  ),
                  AnimatedSwitcher(
                    duration: Motion.d3,
                    child: Icon(
                      tagged.contains(p.id)
                          ? Icons.check_circle
                          : Icons.circle_outlined,
                      key: ValueKey(tagged.contains(p.id)),
                      size: 21,
                      color: tagged.contains(p.id) ? Q.primary : Q.mutedFg,
                    ),
                  ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 10),
        // — canlı XP paylanma önizləməsi —
        AnimatedContainer(
          duration: Motion.d3,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Q.secondary,
            borderRadius: BorderRadius.circular(Q.rMd),
            border: Border.all(color: Q.border),
          ),
          child: Column(
            children: [
              _xpRow('Sən (təşkilatçı)', '+${XpRules.organizerXp} XP',
                  Icons.emoji_events),
              const SizedBox(height: 5),
              _xpRow(
                  'Taq edilən ${tagged.length} iştirakçı',
                  '+${tagged.length * XpRules.taggedParticipantXp} XP',
                  Icons.group),
              const Divider(height: 14, color: Q.border),
              _xpRow(
                  'Cəmi paylanacaq',
                  '+${XpRules.organizerXp + tagged.length * XpRules.taggedParticipantXp} XP',
                  Icons.toll),
            ],
          ),
        ),
        const SizedBox(height: 14),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(
                backgroundColor: Q.primary,
                disabledBackgroundColor: Q.muted),
            onPressed: tagged.isEmpty
                ? null
                : () {
                    final r =
                        widget.state.finishActivity(widget.event, tagged.toList());
                    setState(() {
                      result = r;
                      step = 3;
                    });
                  },
            icon: const Icon(Icons.workspace_premium, size: 16),
            label: Text(
                tagged.isEmpty ? 'Ən azı bir iştirakçı taq et' : 'Bitir və XP payla',
                style: T.button),
          ),
        ),
      ],
    );
  }

  Widget _xpRow(String label, String value, IconData icon) => Row(
        children: [
          Icon(icon, size: 14, color: Q.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Text(label, style: T.meta.copyWith(fontSize: 12.5)),
          ),
          Text(value,
              style: const TextStyle(
                  fontFamily: T.family,
                  fontSize: 12,
                  fontWeight: FontWeight.w800,
                  color: Q.primary)),
        ],
      );

  // — Addım 4: nəticə —
  Widget _stepResult(VenueXpAccount acct) {
    final r = result;
    if (r == null) {
      return const EmptyState(
          icon: Icons.error_outline, text: 'XP paylanmadı — yenidən cəhd et.');
    }
    return PopIn(
      child: Column(
        children: [
          Container(
            width: 62,
            height: 62,
            decoration: const BoxDecoration(
                color: Q.primary, shape: BoxShape.circle),
            child:
                const Icon(Icons.emoji_events, size: 32, color: Colors.white),
          ),
          const SizedBox(height: 14),
          Text('Aktivite bitdi!', style: T.h2.copyWith(fontSize: 15)),
          const SizedBox(height: 6),
          Text(
            '${sharedPhotos.length} şəkil ${r.venueName}-in biznes hesabına düşdü. '
            '${r.taggedCount} iştirakçı taq edildi — hər birinə '
            '+${r.perParticipantXp} Endirim XP yazıldı.',
            textAlign: TextAlign.center,
            style: T.meta.copyWith(fontSize: 11.5, height: 1.6),
          ),
          const SizedBox(height: 16),
          _kv('Sənə (təşkilatçı)', '+${r.organizerXp} XP · ${r.venueName} bazası'),
          _kv('İştirakçılara', '+${r.participantsTotal} XP (hərəsinə ${r.perParticipantXp})'),
          _kv('Yeni balansın', '${acct.balance} XP'),
          const SizedBox(height: 14),
          Text(
            'XP yalnız ${r.venueName} bazasında keçərlidir — digər obyektdən '
            'endirim almaq olmaz (ayrı baza qaydası).',
            textAlign: TextAlign.center,
            style: T.meta.copyWith(fontSize: 10.5, height: 1.5),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              style: FilledButton.styleFrom(backgroundColor: Q.primary),
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Əla!', style: T.button),
            ),
          ),
        ],
      ),
    );
  }

  Widget _kv(String k, String v) => Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(child: Text(k, style: T.meta.copyWith(fontSize: 12.5))),
            const SizedBox(width: 10),
            Text(v,
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 12.5,
                    fontWeight: FontWeight.w700,
                    color: Q.primary)),
          ],
        ),
      );
}
