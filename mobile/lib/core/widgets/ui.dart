import 'package:flutter/material.dart';

import '../models/models.dart';
import '../theme/app_theme.dart';

/// Ortaq UI komponentləri — web styles.css siniflərinin Flutter ekvivalentləri.

/// .section-heading — başlıq + sağda link
class SectionHeading extends StatelessWidget {
  final String title;
  final IconData? icon;
  final Widget? trailing;

  const SectionHeading({super.key, required this.title, this.icon, this.trailing});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14, top: 4),
      child: Row(
        children: [
          Expanded(
            child: Row(
              children: [
                if (icon != null) ...[
                  Icon(icon, size: 18, color: Q.primary),
                  const SizedBox(width: 7),
                ],
                Flexible(
                  child: Text(
                    title,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: T.family,
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (trailing != null) trailing!,
        ],
      ),
    );
  }
}

/// .section-link — kiçik yaşıl link
class SectionLink extends StatelessWidget {
  final String text;
  final IconData? icon;
  final VoidCallback? onTap;

  const SectionLink({super.key, required this.text, this.icon, this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 11, color: Q.primary),
            const SizedBox(width: 3),
          ],
          Text(
            text,
            style: const TextStyle(
              fontFamily: T.family,
              fontSize: 10,
              fontWeight: FontWeight.w600,
              color: Q.primary,
            ),
          ),
        ],
      ),
    );
  }
}

/// .rank-badge — narıncı çip (Trophy + səviyyə adı)
class RankBadge extends StatelessWidget {
  final String label;
  const RankBadge({super.key, required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
      decoration: BoxDecoration(
        color: Q.rankBadgeBg,
        borderRadius: BorderRadius.circular(Q.rMd),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.emoji_events, size: 12, color: Q.accentFg),
          const SizedBox(width: 4),
          Text(
            label,
            style: const TextStyle(
              fontFamily: T.family,
              fontSize: 10,
              fontWeight: FontWeight.w700,
              color: Q.accentFg,
            ),
          ),
        ],
      ),
    );
  }
}

/// .xp-line + .xp-track — XP progress zolağı
class XpTrack extends StatelessWidget {
  final int xp;
  const XpTrack({super.key, required this.xp});

  @override
  Widget build(BuildContext context) {
    final prog = XpRules.xpProgress(xp);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('$xp XP',
                style: const TextStyle(
                    fontFamily: T.family,
                    fontSize: 10,
                    fontWeight: FontWeight.w600)),
            Text(
              prog['nextName'] == null
                  ? 'Ən yüksək səviyyədəsən!'
                  : '${prog['nextName']} səviyyəsinə ${prog['toNext']} XP',
              style: T.meta,
            ),
          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(2),
          child: SizedBox(
            height: 4,
            child: LinearProgressIndicator(
              value: (prog['pct'] as int) / 100,
              minHeight: 4,
              backgroundColor: Q.border,
              valueColor: const AlwaysStoppedAnimation<Color>(Q.primary),
            ),
          ),
        ),
      ],
    );
  }
}

/// .segmented — 3 hissəli tab seçici (web .segmented)
class Segmented extends StatelessWidget {
  final List<String> labels;
  final int active;
  final ValueChanged<int> onChanged;

  const Segmented({super.key, required this.labels, required this.active, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 16, bottom: 22),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Q.muted,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Row(
        children: [
          for (var i = 0; i < labels.length; i++)
            Expanded(
              child: GestureDetector(
                onTap: () => onChanged(i),
                behavior: HitTestBehavior.opaque,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 150),
                  height: 33,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: i == active ? Q.card : Colors.transparent,
                    borderRadius: BorderRadius.circular(5),
                    boxShadow: i == active ? [Q.softShadow] : null,
                  ),
                  child: Text(
                    labels[i],
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontFamily: T.family,
                      fontSize: 10,
                      fontWeight: i == active ? FontWeight.w700 : FontWeight.w500,
                      color: i == active ? Q.primary : Q.mutedFg,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// .search-bar — axtarış zolağı (+ sağda filtr düyməsi)
class SearchBarField extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final VoidCallback? onFilter;

  const SearchBarField({
    super.key,
    required this.controller,
    required this.hint,
    this.onFilter,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 43,
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: Q.border),
        borderRadius: BorderRadius.circular(7),
      ),
      child: Row(
        children: [
          const SizedBox(width: 12),
          Icon(Icons.search, size: 16, color: Q.mutedFg),
          const SizedBox(width: 8),
          Expanded(
            child: TextField(
              controller: controller,
              style: T.input,
              decoration: InputDecoration(
                border: InputBorder.none,
                isDense: true,
                hintText: hint,
                counterText: '',
              ),
            ),
          ),
          if (onFilter != null)
            SizedBox(
              width: 36,
              height: 30,
              child: IconButton(
                onPressed: onFilter,
                padding: EdgeInsets.zero,
                icon: Icon(Icons.tune, size: 16, color: Q.mutedFg),
              ),
            ),
        ],
      ),
    );
  }
}

/// .category-chip — kateqoriya filtri
class CategoryChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool active;
  final VoidCallback onTap;

  const CategoryChip({
    super.key,
    required this.label,
    required this.icon,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        height: 31,
        padding: const EdgeInsets.symmetric(horizontal: 11),
        decoration: BoxDecoration(
          color: active ? Q.primary : Q.card,
          border: Border.all(color: active ? Q.primary : Q.border),
          borderRadius: BorderRadius.circular(Q.rMd),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 13, color: active ? Q.primaryFg : Q.mutedFg),
            const SizedBox(width: 5),
            Text(
              label,
              style: active ? T.chipActive : T.chip,
            ),
          ],
        ),
      ),
    );
  }
}

/// .community-banner — sol yaşıl zolaqlı bildiriş kartı
class CommunityBanner extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback? onTap;
  final Widget? trailing;

  const CommunityBanner({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    this.onTap,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final content = Container(
      margin: const EdgeInsets.symmetric(vertical: 12),
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 17),
      decoration: BoxDecoration(
        color: Q.secondary,
        border: const Border(left: BorderSide(color: Q.primary, width: 3)),
      ),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: const BoxDecoration(color: Q.card, shape: BoxShape.circle),
            child: Icon(icon, size: 18, color: Q.primary),
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 12,
                        fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text(
                  subtitle,
                  style: const TextStyle(
                      fontFamily: T.family,
                      fontSize: 10,
                      height: 1.6,
                      color: Q.mutedFg),
                ),
              ],
            ),
          ),
          if (trailing != null) trailing!,
        ],
      ),
    );
    return onTap == null
        ? content
        : GestureDetector(onTap: onTap, behavior: HitTestBehavior.opaque, child: content);
  }
}

/// .ai-box — kəsik yaşıl haşiyəli AI konteyneri
class AiBox extends StatelessWidget {
  final Widget child;
  const AiBox({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF1FBF4), // primary 6% + card
        borderRadius: BorderRadius.circular(Q.rAiBox),
        border: Border.all(color: Q.primary, width: 1.2),
      ),
      child: child,
    );
  }
}

/// .empty-state — mərkəzləşdirilmiş boşluq bildirişi
class EmptyState extends StatelessWidget {
  final IconData icon;
  final String text;
  final Widget? action;

  const EmptyState({super.key, required this.icon, required this.text, this.action});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 45),
      child: Column(
        children: [
          Icon(icon, size: 34, color: Q.primary),
          const SizedBox(height: 12),
          Text(
            text,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontFamily: T.family, fontSize: 12, height: 1.8, color: Q.mutedFg),
          ),
          if (action != null) ...[const SizedBox(height: 18), action!],
        ],
      ),
    );
  }
}

/// .safety-note — "Birlikdə daha güvənli..." alt yazısı
class SafetyNote extends StatelessWidget {
  const SafetyNote({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 22),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.verified_user, size: 13, color: Q.primary),
          const SizedBox(width: 6),
          Text(
            'Birlikdə daha güvənli, birlikdə daha gözəl.',
            style: const TextStyle(fontFamily: T.family, fontSize: 10, color: Q.mutedFg),
          ),
        ],
      ),
    );
  }
}

/// .form-field + .form-input — vahid forma sahəsi
class FormFieldX extends StatelessWidget {
  final String label;
  final Widget child;
  final String? hint;

  const FormFieldX({super.key, required this.label, required this.child, this.hint});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: T.label),
          const SizedBox(height: 7),
          child,
          if (hint != null) ...[
            const SizedBox(height: 6),
            Text(hint!, style: const TextStyle(
                fontFamily: T.family, fontSize: 11, color: Q.mutedFg)),
          ],
        ],
      ),
    );
  }
}

/// .view-heading — səhifə başlığı + alt yazı
class ViewHeading extends StatelessWidget {
  final String title;
  final String subtitle;
  final Widget? leading;

  const ViewHeading({super.key, required this.title, required this.subtitle, this.leading});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            if (leading != null) ...[leading!, const SizedBox(width: 8)],
            Text(title, style: T.h1),
          ],
        ),
        const SizedBox(height: 4),
        Text(subtitle, style: T.subtitle),
      ],
    );
  }
}

/// .chat-message — söhbə balonu
/// [status]: 'çatdırıldı' / 'oxundu' (ROL TAB 4 oxunma statusu; yalnız mənimsə).
class ChatBubble extends StatelessWidget {
  final String sender;
  final String body;
  final bool mine;
  final String? status;

  const ChatBubble({super.key, required this.sender, required this.body, this.mine = false, this.status});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(top: 15),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.8,
        ),
        decoration: BoxDecoration(
          color: mine ? Q.secondary : Q.muted,
          borderRadius: BorderRadius.circular(Q.rLg),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              sender,
              style: const TextStyle(
                fontFamily: T.family,
                fontSize: 9,
                fontWeight: FontWeight.w700,
                color: Q.primary,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              body,
              style: const TextStyle(fontFamily: T.family, fontSize: 12, height: 1.7),
            ),
            if (mine && status != null) ...[
              const SizedBox(height: 3),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    status == 'oxundu' ? Icons.done_all : Icons.done,
                    size: 11,
                    color: status == 'oxundu' ? Q.primary : Q.mutedFg,
                  ),
                  const SizedBox(width: 3),
                  Text(
                    status!,
                    style: const TextStyle(
                        fontFamily: T.family, fontSize: 8, color: Q.mutedFg),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// .live-badge — şüşə effektlə canlı göstərici (hero şəkli üzərində)
class LiveBadge extends StatelessWidget {
  final String text;
  const LiveBadge({super.key, required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: BoxDecoration(
        color: Q.glass,
        borderRadius: BorderRadius.circular(5),
        border: Border.all(color: Q.glassBorder),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 5,
            height: 5,
            decoration: const BoxDecoration(color: Q.accent, shape: BoxShape.circle),
          ),
          const SizedBox(width: 6),
          Text(
            text,
            style: const TextStyle(
                fontFamily: T.family, fontSize: 9,
                fontWeight: FontWeight.w500, color: Colors.white),
          ),
        ],
      ),
    );
  }
}

/// Kənarları .photo-shade qradiyentli şəkil (event-card şəkli)
class ShadedImage extends StatelessWidget {
  final String asset;
  final double height;
  final BoxFit fit;

  const ShadedImage({super.key, required this.asset, required this.height, this.fit = BoxFit.cover});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      width: double.infinity,
      child: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset(asset, fit: fit),
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.bottomCenter,
                end: Alignment.topCenter,
                stops: const [0, 0.6],
                colors: [
                  Q.photoShade.withValues(alpha: 0.5),
                  Q.photoShade.withValues(alpha: 0),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
