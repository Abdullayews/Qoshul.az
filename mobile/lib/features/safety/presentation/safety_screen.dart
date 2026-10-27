import 'package:flutter/material.dart';

import '../../../core/models/moderation.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';

/// Təhlükəsizlik məsləhətləri — ROL (product-spec I):
/// ilk görüşlər üçün məsləhətlər ekranı.
class SafetyScreen extends StatelessWidget {
  const SafetyScreen({super.key});

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
        title:
            Text('Təhlükəsizlik', style: T.h2.copyWith(fontSize: 18)),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
        children: [
          const ViewHeading(
            title: 'İlk görüş məsləhətləri',
            subtitle: 'Yeni tanışlığa hazırlaş — bu sadə qaydalar çox kömək edir.',
          ),
          const SizedBox(height: 10),
          for (var i = 0; i < safetyTips.length; i++)
            Container(
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Q.card,
                border: Border.all(color: Q.border),
                borderRadius: BorderRadius.circular(Q.rLg),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 34,
                    height: 34,
                    decoration: BoxDecoration(
                      color: Q.secondary,
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(
                        '${i + 1}',
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 13,
                            fontWeight: FontWeight.w800,
                            color: Q.primary),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(safetyTips[i].$1,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 13,
                                fontWeight: FontWeight.w700)),
                        const SizedBox(height: 5),
                        Text(safetyTips[i].$2,
                            style: const TextStyle(
                                fontFamily: T.family,
                                fontSize: 11,
                                height: 1.7,
                                color: Q.mutedFg)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          const CommunityBanner(
            icon: Icons.shield_outlined,
            title: 'Dəqiq ünvan heç vaxt paylaşılmır',
            subtitle: 'Başqa istifadəçilər yalnız təxmini məsafəni görür '
                '(məs. "~2 km"). Ev ünvanın və koordinatın API cavablarında gizli qalır.',
          ),
          const SafetyNote(),
        ],
      ),
    );
  }
}
