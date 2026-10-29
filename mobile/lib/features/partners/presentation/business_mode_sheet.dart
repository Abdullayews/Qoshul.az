import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/models/xp_economy.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';

/// Biznes rejimi — D-026 (product-spec L.4, addım 2-6): obyekt işçisi tərəfi.
///
/// Simmetrik QR modeli: qazanma obyektin statik QR-ı ilə (təşkilatçı oxuyur),
/// xərcləmə müştərinin dinamik QR-ı ilə (işçi oxuyur). Axın:
/// skan → müştəri balansı → hesab məbləği → endirim hesablanır (məzənnə +
/// limitlər) → atomik düşüm → iki tərəfə receipt (ledger).
///
/// Demo qeydi: tester həm müştəri, həm işçi rolunu oynayır — "skan" cari
/// istifadəçinin XP kartındakı kodu oxuyur. Real sistemdə işçi modu ayrıca
/// biznes hesabı ilə daxil olunur (ROL J: Partners).
Future<void> showBusinessModeSheet(BuildContext context, AppState state) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _BusinessModeSheet(state: state),
  );
}

class _BusinessModeSheet extends StatefulWidget {
  final AppState state;
  const _BusinessModeSheet({required this.state});

  @override
  State<_BusinessModeSheet> createState() => _BusinessModeSheetState();
}

class _BusinessModeSheetState extends State<_BusinessModeSheet> {
  int step = 0; // 0: obyekt+skan · 1: balans · 2: hesab+endirim · 3: receipt
  String venueId = 'v5';
  bool scanFailed = false;
  final billCtrl = TextEditingController();
  final xpCtrl = TextEditingController(text: '100');

  @override
  void dispose() {
    billCtrl.dispose();
    xpCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return Padding(
      padding:
          EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: DraggableScrollableSheet(
        initialChildSize: 0.72,
        minChildSize: 0.4,
        maxChildSize: 0.95,
        expand: false,
        builder: (context, scrollController) {
          return AnimatedBuilder(
            animation: state,
            builder: (context, _) {
              final acct = state.venueXp[venueId]!;
              return ListView(
                controller: scrollController,
                padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
                children: [
                  Center(
                    child: Container(
                      width: 36,
                      height: 4,
                      margin: const EdgeInsets.only(bottom: 14),
                      decoration: BoxDecoration(
                          color: Q.border,
                          borderRadius: BorderRadius.circular(2)),
                    ),
                  ),
                  FadeThrough(
                    child: KeyedSubtree(
                      key: ValueKey(step),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Biznes rejimi — XP endirimi',
                              style: T.h2.copyWith(fontSize: 16)),
                          const SizedBox(height: 4),
                          Text(
                            'Müştərinin dinamik XP kodunu oxu, hesabı yaz — '
                            'endirim avtomatik hesablanır.',
                            style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
                          ),
                          const SizedBox(height: 16),
                          if (step == 0) _stepScan(state),
                          if (step == 1) _stepBalance(state, acct),
                          if (step == 2) _stepAmount(state, acct),
                          if (step == 3) _stepReceipt(state, acct),
                        ],
                      ),
                    ),
                  ),
                ],
              );
            },
          );
        },
      ),
    );
  }

  // — Addım 1: obyekt seç + müştəri QR-ını oxu —
  Widget _stepScan(AppState state) {
    final venues = state.venueXp.values.toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Obyekt', style: T.meta.copyWith(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final v in venues)
              ChoiceChip(
                label: Text(v.venueName, style: const TextStyle(
                    fontFamily: T.family, fontSize: 12.5)),
                selected: venueId == v.venueId,
                onSelected: (_) => setState(() {
                  venueId = v.venueId;
                  scanFailed = false;
                }),
                selectedColor: Q.secondary,
                labelStyle: TextStyle(
                    fontFamily: T.family,
                    fontSize: 12.5,
                    color: venueId == v.venueId ? Q.primary : Q.foreground),
              ),
          ],
        ),
        const SizedBox(height: 18),
        if (scanFailed)
          Container(
            margin: const EdgeInsets.only(bottom: 12),
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Q.destructive.withValues(alpha: 0.08),
              borderRadius: BorderRadius.circular(Q.rMd),
              border: Border.all(
                  color: Q.destructive.withValues(alpha: 0.4)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.error_outline,
                    size: 15, color: Q.destructive),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Kod oxunmadı: müştərinin XP kartı bu obyekt üçün etibarlı '
                    'deyil və ya vaxtı bitib. Müştəçi "Profil → XP kartım"-dan '
                    'bu obyekt üçün yeni kod almalıdır (60 saniyəlik, tək istifadəlik).',
                    style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
                  ),
                ),
              ],
            ),
          ),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: Q.primary),
            onPressed: () {
              // Mock skan: cari istifadəçinin kartı bu obyekt üçün etibarımdımı?
              if (state.xpCardValidFor(venueId)) {
                setState(() {
                  scanFailed = false;
                  step = 1;
                });
              } else {
                setState(() => scanFailed = true);
              }
            },
            icon: const Icon(Icons.qr_code_scanner, size: 17),
            label: const Text('Müştəri XP kodunu oxu', style: T.button),
          ),
        ),
      ],
    );
  }

  // — Addım 2: müştəri balansı görünür —
  Widget _stepBalance(AppState state, VenueXpAccount acct) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PopIn(
          child: Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Q.secondary,
              borderRadius: BorderRadius.circular(Q.rLg),
              border: Border.all(color: Q.border),
            ),
            child: Row(
              children: [
                const CircleAvatar(
                    radius: 18,
                    backgroundColor: Q.primary,
                    child: Icon(Icons.person, size: 18, color: Colors.white)),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${state.profileName} (demo müştəri)',
                          style: const TextStyle(
                              fontFamily: T.family,
                              fontSize: 13,
                              fontWeight: FontWeight.w800)),
                      Text('Kod təsdiqləndi · ${acct.venueName} bazası',
                          style: T.meta.copyWith(fontSize: 11.5)),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    CountUp(
                      value: acct.balance,
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 17,
                          fontWeight: FontWeight.w800,
                          color: Q.primary),
                      suffix: ' XP',
                    ),
                    Text('balans', style: T.meta.copyWith(fontSize: 10.5)),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 10),
        Text(
          'Məzənnə: ${acct.config.describe()} · Endirim hesabın maks '
          '${acct.config.maxDiscountPercent}%-i qədər ola bilər.',
          style: T.meta.copyWith(fontSize: 11.5, height: 1.5),
        ),
        const SizedBox(height: 14),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: Q.primary),
            onPressed: () => setState(() => step = 2),
            icon: const Icon(Icons.receipt_long, size: 16),
            label: const Text('Hesabı yaz', style: T.button),
          ),
        ),
      ],
    );
  }

  // — Addım 3: məbləğ + XP → canlı endirim önizləməsi —
  Widget _stepAmount(AppState state, VenueXpAccount acct) {
    final bill = double.tryParse(billCtrl.text.replaceAll(',', '.')) ?? 0;
    final xpSpend = int.tryParse(xpCtrl.text) ?? 0;
    final p = state.previewSpend(venueId, xpSpend, bill);
    final ok = p['ok'] == true;
    final err = p['error'] as String?;

    String errText = switch (err) {
      'balans' => 'Balansda kifayət qədər XP yoxdur (maks $xpSpend-dən az sına).',
      'minBill' =>
        'Hesab minimum ${acct.config.minBillAzn} ₼ olmalıdır (obyekt qaydası).',
      'cap' =>
        'Endirim hesabın ${acct.config.maxDiscountPercent}%-i həddini aşır — '
            '${(p['discount'] as double).toStringAsFixed(2)} ₼-dən çox ola bilməz. '
            'Daha az XP yaz.',
      'hesab' => 'Məbləğ və XP düzgün daxil edilməyib.',
      _ => '',
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _numField(billCtrl, 'Hesab məbləği (₼)', '48.00'),
        const SizedBox(height: 10),
        _numField(xpCtrl, 'Xərclənəcək XP', '100'),
        const SizedBox(height: 14),
        AnimatedContainer(
          duration: Motion.d3,
          padding: const EdgeInsets.all(13),
          decoration: BoxDecoration(
            color: ok ? Q.secondary : Q.destructive.withValues(alpha: 0.07),
            borderRadius: BorderRadius.circular(Q.rMd),
            border: Border.all(
                color: ok ? Q.border : Q.destructive.withValues(alpha: 0.4)),
          ),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Endirim',
                      style: T.meta.copyWith(
                          fontSize: 12.5, fontWeight: FontWeight.w700)),
                  Text(
                    ok
                        ? '− ${(p['discount'] as double).toStringAsFixed(2)} ₼'
                        : '—',
                    style: TextStyle(
                        fontFamily: T.family,
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                        color: ok ? Q.primary : Q.destructive),
                  ),
                ],
              ),
              const SizedBox(height: 5),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Müştəri ödəyir',
                      style: T.meta.copyWith(fontSize: 12.5)),
                  Text(
                    ok ? '${(bill - (p['discount'] as double)).toStringAsFixed(2)} ₼' : '—',
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 12,
                        fontWeight: FontWeight.w700),
                  ),
                ],
              ),
              if (!ok && err != null && errText.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(errText,
                    style: T.meta.copyWith(
                        fontSize: 11.5, height: 1.5, color: Q.destructive)),
              ],
            ],
          ),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: () => setState(() => step = 1),
                child: const Text('Geri', style: T.button),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              flex: 2,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(backgroundColor: Q.primary),
                onPressed: ok
                    ? () {
                        // L.4-5: atomik düşüm + L.4-1: kod işlənib sayılır.
                        final d = state.spendVenueXp(venueId, xpSpend, bill);
                        if (d != null) {
                          state.markXpCardUsed();
                          setState(() => step = 3);
                        }
                      }
                    : null,
                icon: const Icon(Icons.check, size: 16),
                label: const Text('Təsdiqlə və endirim ver', style: T.button),
              ),
            ),
          ],
        ),
      ],
    );
  }

  // — Addım 4: receipt —
  Widget _stepReceipt(AppState state, VenueXpAccount acct) {
    final last = acct.recentLedger.first;
    return PopIn(
      child: Column(
        children: [
          Container(
            width: 62,
            height: 62,
            decoration: const BoxDecoration(
                color: Q.primary, shape: BoxShape.circle),
            child: const Icon(Icons.check_rounded,
                size: 34, color: Colors.white),
          ),
          const SizedBox(height: 14),
          Text('Endirim tətbiq olundu',
              style: T.h2.copyWith(fontSize: 15)),
          const SizedBox(height: 6),
          Text(
            '${acct.venueName} bazasından ${last.amount} XP xərcləndi. '
            'Hər iki tərəfə receipt göndərildi — əməliyyat ledger-də izlənilir.',
            textAlign: TextAlign.center,
            style: T.meta.copyWith(fontSize: 11.5, height: 1.6),
          ),
          const SizedBox(height: 16),
          _kv('İştirak edən XP', '${last.amount} XP'),
          _kv('Qalıq balans', '${last.balanceAfter} XP'),
          _kv('Ledger qeydi', last.id),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              style: FilledButton.styleFrom(backgroundColor: Q.primary),
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Bitir', style: T.button),
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
            Text(k, style: T.meta.copyWith(fontSize: 12.5)),
            Text(v,
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 12.5,
                    fontWeight: FontWeight.w700)),
          ],
        ),
      );

  Widget _numField(TextEditingController ctrl, String label, String hint) {
    return TextField(
      controller: ctrl,
      keyboardType:
          const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [
        FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
      ],
      onChanged: (_) => setState(() {}),
      style: const TextStyle(fontFamily: T.family, fontSize: 13),
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        isDense: true,
        border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(Q.rMd),
            borderSide: const BorderSide(color: Q.border)),
        enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(Q.rMd),
            borderSide: const BorderSide(color: Q.border)),
        focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(Q.rMd),
            borderSide: const BorderSide(color: Q.primary)),
      ),
    );
  }
}
