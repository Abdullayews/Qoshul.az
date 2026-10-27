import 'package:flutter/material.dart';

import '../../../core/models/models.dart';
import '../../../core/models/reservation.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/rol.dart';
import '../../../core/widgets/ui.dart';

/// "Rezervlərim" — ROL (product-spec E): statuslar, QR, check-in, ləğv.
class ReservationsScreen extends StatelessWidget {
  final AppState state;
  const ReservationsScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
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
        title: Text('Rezervlərim', style: T.h2.copyWith(fontSize: 18)),
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) {
          if (state.reservationRestricted) {
            return const EmptyState(
              icon: Icons.block,
              text: 'Rezerv hüquğun müvəqqəti məhdudlaşdırıb — 3 gəlməmə '
                  'qeydə alındı. 7 gün sonra yenidən açılacaq.',
            );
          }
          if (state.reservations.isEmpty) {
            return const EmptyState(
              icon: Icons.table_restaurant,
              text: 'Hələ rezervin yoxdur.\nXəritədən məkan seç, stol ayır.',
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
            children: [
              Text(
                'Gəlməmə sayı: ${state.noShowCount}/3 — 3-dən sonra 7 günlük '
                'məhdudiyyət yaranır.',
                style: T.subtitle,
              ),
              const SizedBox(height: 12),
              for (final r in state.reservations) ...[
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Q.card,
                    border: Border.all(color: Q.border),
                    borderRadius: BorderRadius.circular(Q.rLg),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(Q.rMd),
                            child: Image.asset(r.venueImage,
                                width: 52, height: 52, fit: BoxFit.cover),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(r.venueName,
                                    style: const TextStyle(
                                        fontFamily: T.family,
                                        fontSize: 14,
                                        fontWeight: FontWeight.w800)),
                                const SizedBox(height: 4),
                                Text(
                                    '${r.date}, ${r.time} · ${r.party} nəfər'
                                    '${r.couponCode != null ? ' · kupon: ${r.couponCode}' : ''}',
                                    style: T.meta),
                              ],
                            ),
                          ),
                          ResvStatusChip(status: r.status),
                        ],
                      ),
                      if (r.note.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Text('Qeyd: ${r.note}', style: T.metaSm),
                      ],
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: () => _qrDialog(context, r),
                              icon: const Icon(Icons.qr_code_2, size: 16),
                              label: const Text('QR', style: T.button),
                            ),
                          ),
                          if (r.canCheckIn) ...[
                            const SizedBox(width: 8),
                            Expanded(
                              flex: 2,
                              child: FilledButton.icon(
                                onPressed: () {
                                  final ok = state.checkInReservation(r.id);
                                  if (ok && context.mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(content: Text(
                                          'Check-in uğurlu! +${XpRules.reservationXp} XP'
                                          '${r.couponCode != null ? ' · kupon istifadə olundu' : ''}')),
                                    );
                                  }
                                },
                                icon: const Icon(Icons.where_to_vote_outlined,
                                    size: 16),
                                label: const Text('Check-in et', style: T.button),
                              ),
                            ),
                          ],
                          if (r.canCancel) ...[
                            const SizedBox(width: 8),
                            IconButton(
                              tooltip: 'Ləğv et',
                              onPressed: () => state.cancelReservation(r.id),
                              icon: const Icon(Icons.close, size: 18),
                              color: Q.destructive,
                            ),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
              ],
              const SafetyNote(),
            ],
          );
        },
      ),
    );
  }

  void _qrDialog(BuildContext context, Reservation r) {
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(r.venueName,
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 15,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text('${r.date}, ${r.time} · ${r.party} nəfər', style: T.meta),
            const SizedBox(height: 14),
            QrPlaceholder(code: r.qrCode),
            const SizedBox(height: 8),
            Text(r.qrCode,
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 12,
                    fontWeight: FontWeight.w700)),
          ],
        ),
        actions: [
          FilledButton(onPressed: () => Navigator.pop(d), child: const Text('Bağla')),
        ],
      ),
    );
  }
}
