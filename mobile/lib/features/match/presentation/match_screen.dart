// ignore_for_file: deprecated_member_use
import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/moderation.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';

/// "Tanış ol" — ROL (product-spec D): kart dəsti.
/// Şəkil, ad, yaş, ~məsafə, ortaq maraqlar; "Maraqlıyam" / "Keç".
/// Qarşılıqlı maraq → match + bildiriş + chat + XP (gündə max 3).
class MatchScreen extends StatefulWidget {
  final AppState state;
  const MatchScreen({super.key, required this.state});

  @override
  State<MatchScreen> createState() => _MatchScreenState();
}

class _MatchScreenState extends State<MatchScreen> {
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
        title: Text('Tanış ol', style: T.h2.copyWith(fontSize: 18)),
        actions: [
          IconButton(
            tooltip: 'Təhlükəsizlik məsləhətləri',
            icon: const Icon(Icons.health_and_safety_outlined, size: 20),
            onPressed: () => _safetyDialog(context),
          ),
        ],
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          final candidates = state.matchCandidates;
          if (candidates.isEmpty) {
            return EmptyState(
              icon: Icons.favorite_outline,
              text: state.swipes.isEmpty
                  ? 'Hazırda yeni kart yoxdur.\nYaxınlıqdakı insanlar burada görünəcək.'
                  : 'Bugünlük kartlar bitdi.\nYeni insanlar üçün sabah yenidən bax!',
              action: state.swipes.isNotEmpty
                  ? OutlinedButton.icon(
                      onPressed: () => state.resetSwipes(),
                      icon: const Icon(Icons.refresh, size: 15),
                      label: const Text('Dəsti yenilə (demo)'),
                    )
                  : null,
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            children: [
              for (final p in candidates.take(3)) ...[
                _MatchCard(
                  person: p,
                  state: state,
                  onInterested: () => _swipe(context, state, p, true),
                  onPass: () => state.swipe(p.id, false),
                  onReport: () => _reportSheet(context, state, p),
                  onBlock: () {
                    state.blockUser(p.id);
                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content:
                            Text('${p.fullName} bloklandı — siz bir-birinizi görməzsiniz.')));
                  },
                ),
                const SizedBox(height: 16),
              ],
              if (candidates.length > 3)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    '...və daha ${candidates.length - 3} insan növbəti kartlarda',
                    textAlign: TextAlign.center,
                    style: T.meta,
                  ),
                ),
              const SafetyNote(),
            ],
          );
        },
      ),
    );
  }

  void _swipe(BuildContext context, AppState state, Person p, bool interested) {
    final matched = state.swipe(p.id, interested);
    if (matched && mounted) {
      showDialog<void>(
        context: context,
        builder: (d) => AlertDialog(
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(Q.rLg)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 68,
                height: 68,
                decoration: const BoxDecoration(
                    color: Q.secondary, shape: BoxShape.circle),
                child: const Icon(Icons.favorite, size: 32, color: Q.primary),
              ),
              const SizedBox(height: 14),
              const Text('Match!',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 20,
                      fontWeight: FontWeight.w800)),
              const SizedBox(height: 6),
              Text(
                '${p.fullName} də sənə maraq göstərdi. Söhbəti başla '
                '(+10 XP, gündə max 3).',
                textAlign: TextAlign.center,
                style: const TextStyle(
                    fontFamily: T.family, fontSize: 11, height: 1.7),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(d),
              child: const Text('Davam et'),
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(d);
                Navigator.of(context).pop(); // → çatlara qayıt
              },
              child: const Text('Söhbətə keç'),
            ),
          ],
        ),
      );
    }
  }

  void _safetyDialog(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('İlk görüş məsləhətləri'),
        content: SizedBox(
          width: double.maxFinite,
          child: ListView(
            shrinkWrap: true,
            children: [
              for (final (title, text) in safetyTips)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 12,
                              fontWeight: FontWeight.w700)),
                      const SizedBox(height: 3),
                      Text(text,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 11,
                              height: 1.6,
                              color: Q.mutedFg)),
                    ],
                  ),
                ),
            ],
          ),
        ),
        actions: [
          FilledButton(
              onPressed: () => Navigator.pop(d), child: const Text('Bağla')),
        ],
      ),
    );
  }

  void _reportSheet(BuildContext context, AppState state, Person p) {
    String reason = reportReasons.first;
    showModalBottomSheet<void>(
      context: context,
      builder: (sheetCtx) => StatefulBuilder(
        builder: (sheetCtx, setSheet) => SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Report et',
                    style: TextStyle(
                        fontFamily: T.family,
                        fontSize: 16,
                        fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text('${p.fullName} haqqında bildiriş. Səbəb seç:',
                    style: T.subtitle),
                const SizedBox(height: 12),
                for (final r in reportReasons)
                  RadioListTile<String>(
                    value: r,
                    groupValue: reason,
                    onChanged: (v) => setSheet(() => reason = v!),
                    title: Text(r,
                        style: const TextStyle(
                            fontFamily: T.family, fontSize: 12)),
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                    activeColor: Q.primary,
                  ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () {
                      state.reportUser(p.id, reason);
                      Navigator.pop(sheetCtx);
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                          content: Text(
                              'Report göndərildi. Moderasiya komandası baxacaq.')));
                    },
                    child: const Text('Göndər'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _MatchCard extends StatelessWidget {
  final Person person;
  final AppState state;
  final VoidCallback onInterested;
  final VoidCallback onPass;
  final VoidCallback onReport;
  final VoidCallback onBlock;

  const _MatchCard({
    required this.person,
    required this.state,
    required this.onInterested,
    required this.onPass,
    required this.onReport,
    required this.onBlock,
  });

  @override
  Widget build(BuildContext context) {
    final p = person;
    final shared = p.rolInterests
        .where((i) => state.interests.contains(i))
        .toList(growable: false);
    return Container(
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(Q.rXl),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // — şəkil + overlay —
          SizedBox(
            height: 240,
            width: double.infinity,
            child: Stack(
              fit: StackFit.expand,
              children: [
                Image.asset(p.image, fit: BoxFit.cover),
                DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.bottomCenter,
                      end: Alignment.topCenter,
                      stops: const [0, 0.55],
                      colors: [
                        Q.photoShade.withValues(alpha: 0.75),
                        Q.photoShade.withValues(alpha: 0),
                      ],
                    ),
                  ),
                ),
                Positioned(
                  top: 10,
                  right: 10,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 5),
                    decoration: BoxDecoration(
                      color: Q.glass,
                      borderRadius: BorderRadius.circular(5),
                      border: Border.all(color: Q.glassBorder),
                    ),
                    child: Text(
                      '${p.match}% uyğunluq',
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 9,
                          color: Colors.white,
                          fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
                Positioned(
                  left: 14,
                  bottom: 12,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${p.firstName}, ${p.age}',
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 19,
                            fontWeight: FontWeight.w800,
                            color: Colors.white),
                      ),
                      const SizedBox(height: 3),
                      Row(
                        children: [
                          const Icon(Icons.location_on,
                              size: 11, color: Colors.white70),
                          const SizedBox(width: 3),
                          Text(
                            state.showDistance
                                ? '${p.distanceLabel} · ${p.city}'
                                : p.city,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 10,
                                color: Colors.white70),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                Positioned(
                  top: 8,
                  left: 8,
                  child: PopupMenuButton<String>(
                    icon: const Icon(Icons.more_vert,
                        size: 18, color: Colors.white),
                    onSelected: (v) =>
                        v == 'report' ? onReport() : onBlock(),
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
                    ],
                  ),
                ),
              ],
            ),
          ),
          // — məlumat —
          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${statusLabel(p.status)} · ${p.org}',
                    style: T.meta),
                const SizedBox(height: 8),
                if (p.rolInterests.isNotEmpty)
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: [
                      for (final i in p.rolInterests)
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 9, vertical: 5),
                          decoration: BoxDecoration(
                            color: shared.contains(i) ? Q.secondary : Q.muted,
                            borderRadius: BorderRadius.circular(5),
                          ),
                          child: Text(
                            i,
                            style: TextStyle(
                                fontFamily: T.family,
                                fontSize: 9,
                                fontWeight: shared.contains(i)
                                    ? FontWeight.w700
                                    : FontWeight.w500,
                                color: shared.contains(i)
                                    ? Q.primary
                                    : Q.mutedFg),
                          ),
                        ),
                    ],
                  ),
                const SizedBox(height: 10),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.auto_awesome, size: 12, color: Q.primary),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(p.reason,
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 10,
                              height: 1.6,
                              color: Q.mutedFg)),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                // — hərəkətlər —
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: onPass,
                        style: OutlinedButton.styleFrom(
                          minimumSize: const Size(0, 42),
                        ),
                        icon: const Icon(Icons.close, size: 16),
                        label: const Text('Keç', style: T.button),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      flex: 2,
                      child: FilledButton.icon(
                        onPressed: onInterested,
                        style: FilledButton.styleFrom(
                            minimumSize: const Size(0, 42)),
                        icon: const Icon(Icons.favorite, size: 16),
                        label: const Text('Maraqlıyam', style: T.button),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
