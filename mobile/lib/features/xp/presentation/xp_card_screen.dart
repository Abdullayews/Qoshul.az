import 'dart:async';

import 'package:flutter/material.dart';

import '../../../core/models/models.dart';
import '../../../core/models/xp_economy.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';

/// XP Kartı — D-026 (product-spec L.4): müştərinin dinamik QR-ı.
///
/// İki XP görünür: Peşəkarlıq XP (səviyyə — xərclənmir) və obyektə-məxsus
/// Endirim XP bazaları (Xankəndi-də qazanılan yalnız Xankəndidə işləyir).
/// Obyekt seçiləndə: 60 saniyəlik tək-istifadəlik dinamik QR + 6 rəqəmli
/// fallback kod + tənzimləmələr + ledger tarixçəsi. XP-nin müddəti yoxdur.
class XpCardScreen extends StatefulWidget {
  final AppState state;
  const XpCardScreen({super.key, required this.state});

  @override
  State<XpCardScreen> createState() => _XpCardScreenState();
}

class _XpCardScreenState extends State<XpCardScreen> {
  String? selectedVenueId;
  Timer? _ticker;

  @override
  void initState() {
    super.initState();
    // Dinamik QR 60 saniyəlikdir — saniyə tik-tiki ilə geri sayım yenilənir.
    _ticker = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  void _openVenue(String venueId) {
    setState(() => selectedVenueId = venueId);
    // Hər açılışda yeni nonce (L.4: tək-istifadəlik, screenshot qorunması).
    widget.state.regenerateXpCard(venueId);
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return Scaffold(
      backgroundColor: Q.background,
      appBar: AppBar(
        backgroundColor: Q.background,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        leading: IconButton(
          icon: const Icon(Icons.chevron_left, size: 26),
          color: Q.primary,
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text('XP kartım', style: T.h2.copyWith(fontSize: 18)),
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          final accounts = state.venueXp.values.toList()
            ..sort((a, b) => b.balance.compareTo(a.balance));
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 21, 20, 32),
            children: [
              StaggerIn(
                index: 0,
                child: const ViewHeading(
                  title: 'XP kartım',
                  subtitle:
                      'Endirim XP-ni obyektdə göstər, kassa endirimi alsın. '
                      'Hər obyektin XP bazası ayrıdır.',
                ),
              ),
              const SizedBox(height: 16),

              // — 1-ci XP: Peşəkarlıq (xərclənmir, səviyyə üçün) —
              StaggerIn(
                index: 1,
                child: Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Q.secondary,
                    borderRadius: BorderRadius.circular(Q.rLg),
                    border: Border.all(color: Q.border),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.workspace_premium,
                          size: 20, color: Q.primary),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Peşəkarlıq XP: ${state.xp} · ${state.rank}',
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 12,
                                    fontWeight: FontWeight.w700,
                                    color: Q.primary)),
                            const SizedBox(height: 2),
                            Text(
                              'Səviyyə üçün istifadə olunur — xərclənmir. '
                              'Səviyyən artdıqca Kəşf-də sənə peşəkar aktiviteler önə düşür.',
                              style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 18),

              // — 2-ci XP: obyekt bazaları —
              for (var i = 0; i < accounts.length; i++) ...[
                StaggerIn(
                  index: i + 2,
                  child: ScaleTap(
                    onTap: () => _openVenue(accounts[i].venueId),
                    child: _AccountCard(
                      account: accounts[i],
                      state: state,
                      expanded: selectedVenueId == accounts[i].venueId,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
              ],

              const SizedBox(height: 6),
              Text(
                'XP-nin müddəti yoxdur — balans qazanma − xərcləmədir. '
                'Kamera işləməsə QR altındakı 6 rəqəmli kodu de.',
                style: T.meta.copyWith(fontSize: 11.5, height: 1.6),
                textAlign: TextAlign.center,
              ),
            ],
          );
        },
      ),
    );
  }
}

/// Bir obyektin balans kartı — açılan zaman dinamik QR bölməsi göstərilir.
class _AccountCard extends StatelessWidget {
  final VenueXpAccount account;
  final AppState state;
  final bool expanded;

  const _AccountCard({
    required this.account,
    required this.state,
    required this.expanded,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: Motion.d3,
      curve: Motion.ease,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Q.card,
        borderRadius: BorderRadius.circular(Q.rLg),
        border: Border.all(color: expanded ? Q.primary : Q.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: Q.secondary,
                  borderRadius: BorderRadius.circular(Q.rMd),
                ),
                child: const Icon(Icons.storefront, size: 19, color: Q.primary),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(account.venueName,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 13,
                            fontWeight: FontWeight.w800)),
                    const SizedBox(height: 2),
                    Text(account.config.describe(),
                        style: T.meta.copyWith(fontSize: 11.5)),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  CountUp(
                    value: account.balance,
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 17,
                        fontWeight: FontWeight.w800,
                        color: Q.primary),
                    suffix: ' XP',
                  ),
                  Text('Endirim XP', style: T.meta.copyWith(fontSize: 10.5)),
                ],
              ),
            ],
          ),
          if (expanded) ...[
            const SizedBox(height: 14),
            const Divider(height: 1, color: Q.border),
            const SizedBox(height: 14),
            _DynamicQrSection(account: account, state: state),
            const SizedBox(height: 14),
            // — ledger tarixçəsi (L.4-6: hər əməliyyat izlənəndir) —
            Text('Hərəkətlər (ledger)',
                style: T.meta.copyWith(
                    fontSize: 11.5, fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            if (account.recentLedger.isEmpty)
              Text('Hərəkət yoxdur — aktivite keçir, XP qazan.',
                  style: T.meta.copyWith(fontSize: 11.5))
            else
              for (final e in account.recentLedger.take(4))
                Padding(
                  padding: const EdgeInsets.only(bottom: 7),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        e.grant ? Icons.add_circle : Icons.remove_circle,
                        size: 14,
                        color: e.grant ? Q.primary : Q.destructive,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(e.note,
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 12.5,
                                    height: 1.4)),
                            Text(
                              '${e.grant ? '+' : '−'}${e.amount} XP · '
                              '${_fmtDate(e.at)} · qalıq ${e.balanceAfter}',
                              style: T.meta.copyWith(fontSize: 10.5),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
          ],
        ],
      ),
    );
  }

  String _fmtDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}.${d.month.toString().padLeft(2, '0')} '
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
}

/// Dinamik QR bölməsi — 60 saniyəlik ömür, geri sayım zolağı,
/// tək-istifadəlik nonce, 6 rəqəmli fallback kod.
class _DynamicQrSection extends StatelessWidget {
  final VenueXpAccount account;
  final AppState state;

  const _DynamicQrSection({required this.account, required this.state});

  @override
  Widget build(BuildContext context) {
    final card = state.xpCard;
    final valid =
        card != null && state.xpCardVenueId == account.venueId && card.isValid;
    final seconds = card?.secondsLeft ?? 0;
    final pct = seconds / XpRules.xpCardTtlSeconds;

    return Column(
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // — QR —
            Stack(
              alignment: Alignment.center,
              children: [
                PopIn(
                  key: ValueKey(card?.nonce ?? 'yox'),
                  child: Opacity(
                    opacity: valid ? 1 : 0.35,
                    child: QrPlaceholder(
                        code: card?.nonce ?? 'expired', size: 124),
                  ),
                ),
                if (!valid)
                  Icon(Icons.block,
                      size: 40,
                      color: Q.destructive.withValues(alpha: 0.7)),
              ],
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Dinamik XP kodu',
                      style: T.meta.copyWith(
                          fontSize: 11.5, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 4),
                  Text(
                    valid
                        ? 'Kassaya göstər — $seconds saniyədən sonra yenilənir. '
                          'Kod tək istifadəlikdir: skan ediləndə bir daha işləmir.'
                        : 'Kodun vaxtı bitdi və ya işlənib — yenilə.',
                    style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
                  ),
                  const SizedBox(height: 8),
                  // — fallback: 6 rəqəmli kod (kamera işləməyəndə) —
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: Q.muted,
                      borderRadius: BorderRadius.circular(Q.rMd),
                      border: Border.all(color: Q.border),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.dialpad, size: 12, color: Q.mutedFg),
                        const SizedBox(width: 6),
                        Text(
                          card?.fallbackCode ?? '------',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 13,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 2),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        // — vaxt zolağı + yenilə düyməsi —
        Row(
          children: [
            Expanded(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(2),
                child: SizedBox(
                  height: 5,
                  child: LinearProgressIndicator(
                    value: valid ? pct : 0,
                    minHeight: 5,
                    backgroundColor: Q.border,
                    valueColor: const AlwaysStoppedAnimation<Color>(Q.primary),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 10),
            ScaleTap(
              onTap: () => state.regenerateXpCard(account.venueId),
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                decoration: BoxDecoration(
                  color: Q.primary,
                  borderRadius: BorderRadius.circular(Q.rMd),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.refresh, size: 13, color: Colors.white),
                    const SizedBox(width: 5),
                    Text('Yenilə',
                        style: T.button.copyWith(color: Colors.white)),
                  ],
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
