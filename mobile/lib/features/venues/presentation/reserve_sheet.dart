import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/models/reservation.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';

/// Rezerv axını — ROL (product-spec E):
/// tarix → saat slotu → nəfər sayı → qeyd → kupon seç → təsdiqlə → status + QR.
/// Onlayn ödəniş YOXDUR (ROL E: [TƏSDİQ GÖZLƏYİR]).
Future<void> showReserveSheet(BuildContext context, AppState state, Partner venue) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _ReserveSheet(state: state, venue: venue),
  );
}

class _ReserveSheet extends StatefulWidget {
  final AppState state;
  final Partner venue;
  const _ReserveSheet({required this.state, required this.venue});

  @override
  State<_ReserveSheet> createState() => _ReserveSheetState();
}

class _ReserveSheetState extends State<_ReserveSheet> {
  int step = 0; // 0 tarix, 1 saat, 2 nəfər, 3 qeyd+kupon, 4 nəticə
  String? date;
  String? time;
  int party = 2;
  final noteCtrl = TextEditingController();
  String? couponCode;
  Reservation? result;

  @override
  void dispose() {
    noteCtrl.dispose();
    super.dispose();
  }

  Partner get v => widget.venue;
  AppState get state => widget.state;

  void _confirm() {
    final r = state.createReservation(
      venue: v,
      date: date!,
      time: time!,
      party: party,
      note: noteCtrl.text.trim(),
      couponCode: couponCode,
    );
    if (r == null) {
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text(
              'Rezerv hüquğun müvəqqəti məhdudlaşdırıb (3 gəlməmə). 7 gün sonra yenidən açılacaq.')));
      return;
    }
    setState(() {
      result = r;
      step = 4;
    });
  }

  @override
  Widget build(BuildContext context) {
    final dates = nextDateLabels();
    final slots = venueSlotTimes[v.id] ?? const <String>[];
    final myCoupons = state.activeCouponsFor(v.id);
    final canConfirm = date != null && time != null;

    return DraggableScrollableSheet(
      initialChildSize: 0.78,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      expand: false,
      builder: (context, scrollController) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: ListView(
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
            Text('Rezerv — ${v.name}',
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 18,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text(
              step == 4
                  ? 'Rezerv yaradıldı.'
                  : 'Addım ${step + 1}/4 — ${['tarix', 'saat', 'nəfər sayı', 'qeyd və kupon'][step]}',
              style: T.subtitle,
            ),
            const SizedBox(height: 16),
            if (step == 0) ...[
              const Text('Tarix seç',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 13,
                      fontWeight: FontWeight.w700)),
              const SizedBox(height: 10),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final d in dates)
                    CategoryChip(
                      label: d,
                      icon: Icons.calendar_today,
                      active: date == d,
                      onTap: () => setState(() {
                        date = d;
                        step = 1;
                      }),
                    ),
                ],
              ),
            ] else if (step == 1) ...[
              const Text('Saat slotu seç',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 13,
                      fontWeight: FontWeight.w700)),
              const SizedBox(height: 10),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final s in slots)
                    CategoryChip(
                      label: s,
                      icon: Icons.schedule,
                      active: time == s,
                      onTap: () => setState(() {
                        time = s;
                        step = 2;
                      }),
                    ),
                ],
              ),
              const SizedBox(height: 12),
              SectionLink(
                  text: '← tarixə qayıt',
                  onTap: () => setState(() => step = 0)),
            ] else if (step == 2) ...[
              const Text('Nəfər sayı',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 13,
                      fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Text('Məkanın tutumuna görə (maks. 8 nəfər)', style: T.meta),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton.filled(
                    onPressed: party > 1
                        ? () => setState(() => party--)
                        : null,
                    icon: const Icon(Icons.remove, size: 18),
                  ),
                  Container(
                    width: 76,
                    alignment: Alignment.center,
                    child: Text('$party nəfər',
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 18,
                            fontWeight: FontWeight.w800)),
                  ),
                  IconButton.filled(
                    onPressed: party < 8
                        ? () => setState(() => party++)
                        : null,
                    icon: const Icon(Icons.add, size: 18),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => setState(() => step = 3),
                  child: const Text('Davam et'),
                ),
              ),
              const SizedBox(height: 12),
              SectionLink(
                  text: '← saata qayıt',
                  onTap: () => setState(() => step = 1)),
            ] else if (step == 3) ...[
              FormFieldX(
                label: 'Qeyd (isteğe bağlı)',
                hint: 'Ad günü, pəncərə kənarı və s.',
                child: TextField(
                  controller: noteCtrl,
                  maxLines: 2,
                  maxLength: 200,
                  style: T.input,
                  decoration: const InputDecoration(counterText: ''),
                ),
              ),
              const SizedBox(height: 6),
              const Text('Kupon seç',
                  style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 13,
                      fontWeight: FontWeight.w700)),
              const SizedBox(height: 10),
              if (myCoupons.isEmpty)
                Text(
                  'Bu məkan üçün aktiv kuponun yoxdur. '
                  '"Endirimlər" bölməsindən XP ilə ala bilərsən.',
                  style: T.subtitle,
                )
              else
                Column(
                  children: [
                    _couponTile(null, 'Kuponsuz rezerv'),
                    for (final c in myCoupons) _couponTile(c.code, '${c.label} · ${c.code}'),
                  ],
                ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: canConfirm ? _confirm : null,
                  child: const Text('Təsdiqlə'),
                ),
              ),
              const SizedBox(height: 12),
              SectionLink(
                  text: '← nəfər sayına qayıt',
                  onTap: () => setState(() => step = 2)),
            ] else ...[
              // — nəticə: status + QR —
              Center(
                child: Column(
                  children: [
                    ResvStatusChip(status: result!.status),
                    const SizedBox(height: 14),
                    // QR uğur nəticəsində pop şəklində görünür (D-025).
                    PopIn(
                      child: QrPlaceholder(code: result!.qrCode),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      result!.qrCode,
                      style: const TextStyle(
                          fontFamily: T.family,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Q.card,
                  border: Border.all(color: Q.border),
                  borderRadius: BorderRadius.circular(Q.rLg),
                ),
                child: Column(
                  children: [
                    _factRow('${result!.venueName} · ${result!.date}, ${result!.time}'),
                    _factRow('${result!.party} nəfər'
                        '${result!.couponCode != null ? ' · kupon: ${result!.couponCode}' : ''}'),
                    _factRow(result!.status == ResvStatus.confirmed
                        ? 'Avtomatik təsdiqləndi — TƏSDİQLƏNDİ'
                        : 'Partnyor təsdiqi gözlənilir — GÖZLƏYİR'),
                    _factRow('Xatırlatmalar: 24 saat və 2 saat əvvəl bildiriş'),
                    _factRow('Məkanda QR-i göstər — partnyor skan edəndə +${XpRules.reservationXp} XP'),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Hazırdır'),
                ),
              ),
            ],
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _factRow(String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.check, size: 13, color: Q.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Text(text,
                style: const TextStyle(
                    fontFamily: T.family, fontSize: 11, height: 1.6)),
          ),
        ],
      ),
    );
  }

  Widget _couponTile(String? code, String label) {
    final active = couponCode == code;
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GestureDetector(
        onTap: () => setState(() => couponCode = code),
        behavior: HitTestBehavior.opaque,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
          decoration: BoxDecoration(
            color: active ? Q.secondary : Q.card,
            border: Border.all(color: active ? Q.primary : Q.border,
                width: active ? 1.4 : 1),
            borderRadius: BorderRadius.circular(Q.rMd),
          ),
          child: Row(
            children: [
              Icon(
                active ? Icons.check_circle : Icons.confirmation_number,
                size: 16,
                color: active ? Q.primary : Q.mutedFg,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(label,
                    style: const TextStyle(
                        fontFamily: T.family, fontSize: 12)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
