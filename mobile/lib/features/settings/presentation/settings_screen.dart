import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/models/xp_economy.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/motion.dart';
import '../../notifications/presentation/notifications_screen.dart';
import '../../safety/presentation/safety_screen.dart';

/// Ayarlar — ROL (product-spec C, TAB 5):
/// məxfilik (xəritədə görünmə, məsafə göstərmə), bildirişlər, dil,
/// hesabı təsdiqlə (telefon), bloklananlar, təhlükəsizlik, hesabı silmək.
class SettingsScreen extends StatefulWidget {
  final AppState state;
  const SettingsScreen({super.key, required this.state});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
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
        centerTitle: false,
        leading: IconButton(
          icon: const Icon(Icons.chevron_left, size: 26),
          color: Q.primary,
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text('Ayarlar', style: T.h2.copyWith(fontSize: 18)),
      ),
      body: AnimatedBuilder(
        animation: state,
        builder: (context, _) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
          children: [
            _section('Hesab'),
            _card([
              _row(Icons.person, 'Ad', state.profileName),
              _divider(),
              _row(Icons.location_city, 'Şəhər', state.city),
              _divider(),
              _row(Icons.workspace_premium, 'Səviyyə', state.rank),
              _divider(),
              _row(
                Icons.verified_user,
                'Hesabı təsdiqlə (telefon)',
                state.phoneVerified
                    ? 'Təsdiqlənmiş ✓ (+${XpRules.phoneVerifyXp} XP)'
                    : 'Təsdiq edilməyib',
                valueColor: state.phoneVerified ? Q.primary : null,
                onTap: state.phoneVerified ? null : _verifyPhoneDialog,
              ),
            ]),
            _section('Məxfilik'),
            _card([
              // D-027: 3-seçimli görünüş — Hamı / Yalnız dostlar / Gizli.
              _navRow(Icons.map, 'Xəritədə görünmə', _visibilityDialog,
                  subtitle: '${state.mapVisibility.label} — ${state.mapVisibility.hint}'),
              _divider(),
              _switchRow(
                Icons.near_me,
                'Məsafəmi göstər (~2 km)',
                state.showDistance,
                state.setShowDistance,
              ),
            ]),
            Text(
              'Dəqiq koordinatın heç kimə göstərilmir — yalnız təxmini məsafə. XP-nin müddəti yoxdur — balans = qazanma − xərcləmə.',
              style: const TextStyle(
                  fontFamily: T.family, fontSize: 11.5, height: 1.6, color: Q.mutedFg),
            ),
            _section('Bildirişlər'),
            _card([
              _navRow(Icons.tune, 'Bildiriş tercihleri', () {
                Navigator.of(context).push(GlassPageRoute(
                    builder: (_) => NotificationsScreen(state: state)));
              }, subtitle: 'Hər tip ayrıca; marketinq 23:00-08:00 sükut'),
            ]),
            _section('Təhlükəsizlik və dəstək'),
            _card([
              _navRow(Icons.health_and_safety_outlined, 'İlk görüş məsləhətləri',
                  () {
                Navigator.of(context).push(
                    GlassPageRoute(builder: (_) => const SafetyScreen()));
              }),
              _divider(),
              _navRow(Icons.block, 'Bloklananlar (${state.blocked.length})',
                  state.blocked.isEmpty ? null : _blockedDialog,
                  subtitle: state.blocked.isEmpty
                      ? 'Bloklanan istifadəçi yoxdur'
                      : 'Bəzi istifadəçilər bloklanıb'),
              _divider(),
              _navRow(Icons.language, 'Dil', null, subtitle: 'Azərbaycan dili'),
              _divider(),
              _navRow(Icons.info_outline, 'Haqqında (v0.3.0)', null,
                  subtitle: 'UI mərhələsi — mock data ilə'),
              _divider(),
              _navRow(Icons.description, 'Şərtlər və məxfilik', null),
            ]),
            const SizedBox(height: 14),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: Q.destructive,
                  side: const BorderSide(color: Q.destructive),
                ),
                onPressed: _deleteAccountDialog,
                icon: const Icon(Icons.delete_outline, size: 16),
                label: Text('Hesabı silmək', style: T.button),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () {
                  widget.state.signOut();
                  Navigator.of(context).popUntil((route) => route.isFirst);
                },
                icon: const Icon(Icons.logout, size: 16),
                label: Text('Çıxış et', style: T.button),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // — telefon təsdiqi (ROL B: sonradan, badge + XP) —
  Future<void> _verifyPhoneDialog() async {
    final ctrl = TextEditingController();
    await showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('Hesabı təsdiqlə'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Telefon nömrəni əlavə et və SMS kodunu yaz. '
              'Təsdiq "Təsdiqlənmiş" nişanı + ${XpRules.phoneVerifyXp} XP verir.',
              style: const TextStyle(
                  fontFamily: T.family, fontSize: 12.5, height: 1.7),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: ctrl,
              keyboardType: TextInputType.phone,
              maxLength: 6,
              decoration: const InputDecoration(
                  hintText: '6 rəqəmli kod (demo: istənilən)', counterText: ''),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(d),
            child: const Text('Ləğv et'),
          ),
          FilledButton(
            onPressed: () {
              if (ctrl.text.trim().length == 6) {
                widget.state.verifyPhone();
                Navigator.pop(d);
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                    content: Text(
                        'Hesab təsdiqləndi! +${XpRules.phoneVerifyXp} XP və "Təsdiqlənmiş" nişanı qazandın.')));
              }
            },
            child: const Text('Təsdiqlə'),
          ),
        ],
      ),
    );
    ctrl.dispose();
  }

  void _blockedDialog() {
    final state = widget.state;
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('Bloklananlar'),
        content: SizedBox(
          width: double.maxFinite,
          child: ListView(
            shrinkWrap: true,
            children: [
              for (final id in state.blocked)
                FutureBuilder<String>(
                  future: Future.value(_personName(id)),
                  builder: (context, snap) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                    title: Text(snap.data ?? id,
                        style: const TextStyle(fontFamily: T.family, fontSize: 12)),
                    trailing: TextButton(
                      onPressed: () {
                        state.unblockUser(id);
                        Navigator.pop(d);
                      },
                      child: const Text('Blokdan çıxar',
                          style: TextStyle(fontSize: 12.5)),
                    ),
                  ),
                ),
            ],
          ),
        ),
        actions: [
          FilledButton(onPressed: () => Navigator.pop(d), child: const Text('Bağla')),
        ],
      ),
    );
  }

  String _personName(String id) {
    for (final p in mockPeople) {
      if (p.id == id) return p.fullName;
    }
    return id;
  }

  /// D-027: xəritə görünüşü seçimi — 3 variant (dialog).
  Future<void> _visibilityDialog() async {
    await showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('Xəritədə görünmə'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final v in MapVisibility.values)
              InkWell(
                onTap: () {
                  widget.state.setMapVisibility(v);
                  Navigator.pop(d);
                },
                borderRadius: BorderRadius.circular(Q.rMd),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
                  child: Row(
                    children: [
                      Icon(
                        widget.state.mapVisibility == v
                            ? Icons.radio_button_checked
                            : Icons.radio_button_off,
                        size: 19,
                        color: widget.state.mapVisibility == v
                            ? Q.primary
                            : Q.mutedFg,
                      ),
                      const SizedBox(width: 11),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(v.label,
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w700)),
                            const SizedBox(height: 2),
                            Text(v.hint,
                                style: const TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 11.5,
                                    height: 1.4,
                                    color: Q.mutedFg)),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  void _deleteAccountDialog() {
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('Hesabı silmək?'),
        content: const Text(
          'Bütün məlumatların (profil, tədbirlər, söhbətlər, XP və kuponlar) '
          'GDPR və yerli qanunvericiliyə əsasən silinəcək. Bu əməliyyat geri '
          'qaytarıla bilməz.\n\nDemo: backend mərhələsində aktivləşəcək.',
          style: TextStyle(fontFamily: T.family, fontSize: 12, height: 1.7),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(d),
            child: const Text('Imtina'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
                backgroundColor: Q.destructive, foregroundColor: Colors.white),
            onPressed: () {
              Navigator.pop(d);
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                  content:
                      Text('Demo: hesab silmə backend mərhələsində işə düşəcək.')));
            },
            child: const Text('Sil'),
          ),
        ],
      ),
    );
  }

  Widget _section(String title) => Padding(
        padding: const EdgeInsets.only(left: 4, bottom: 8, top: 6),
        child: Text(title,
            style: const TextStyle(
                fontFamily: T.family,
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
                color: Q.mutedFg)),
      );

  Widget _card(List<Widget> children) => Container(
        decoration: BoxDecoration(
          color: Q.card,
          border: Border.all(color: Q.border),
          borderRadius: BorderRadius.circular(Q.rLg),
        ),
        clipBehavior: Clip.antiAlias,
        margin: const EdgeInsets.only(bottom: 14),
        child: Column(children: children),
      );

  Widget _divider() => const Divider(height: 1, color: Q.border);

  Widget _row(IconData icon, String label, String value,
      {Color? valueColor, VoidCallback? onTap}) {
    final content = Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      child: Row(
        children: [
          Icon(icon, size: 17, color: Q.mutedFg),
          const SizedBox(width: 12),
          Expanded(
            child: Text(label,
                style: const TextStyle(fontFamily: T.family, fontSize: 12)),
          ),
          Text(value,
              style: T.meta.copyWith(
                  fontSize: 12.5, color: valueColor ?? Q.mutedFg)),
          if (onTap != null) ...[
            const SizedBox(width: 4),
            Icon(Icons.chevron_right, size: 16, color: Q.mutedFg),
          ],
        ],
      ),
    );
    return onTap == null
        ? content
        : InkWell(onTap: onTap, child: content);
  }

  Widget _switchRow(
      IconData icon, String label, bool value, ValueChanged<bool> onChanged) {
    return Padding(
      padding: const EdgeInsets.only(left: 14, right: 4),
      child: Row(
        children: [
          Icon(icon, size: 17, color: Q.mutedFg),
          const SizedBox(width: 12),
          Expanded(
            child: Text(label,
                style: const TextStyle(fontFamily: T.family, fontSize: 12)),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            activeThumbColor: Q.primary,
          ),
        ],
      ),
    );
  }

  Widget _navRow(IconData icon, String label, VoidCallback? onTap,
      {String? subtitle}) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        child: Row(
          children: [
            Icon(icon, size: 17, color: Q.mutedFg),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(label,
                      style: const TextStyle(
                          fontFamily: T.family, fontSize: 12)),
                  if (subtitle != null) ...[
                    const SizedBox(height: 3),
                    Text(subtitle,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 11.5,
                            color: Q.mutedFg)),
                  ],
                ],
              ),
            ),
            if (onTap != null)
              Icon(Icons.chevron_right, size: 18, color: Q.mutedFg),
          ],
        ),
      ),
    );
  }
}
