import 'package:flutter/material.dart';

import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';

/// Ayarlar — web /settings marşrutunun yüngül portu.
class SettingsScreen extends StatefulWidget {
  final AppState state;
  const SettingsScreen({super.key, required this.state});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool notifications = true;
  bool visibility = true;

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
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 21, 20, 24),
        children: [
          _section('Hesab'),
          _card([
            _row(Icons.person, 'Ad', state.profileName),
            _divider(),
            _row(Icons.location_city, 'Şəhər', state.city),
            _divider(),
            _row(Icons.workspace_premium, 'Səviyyə', state.rank),
          ]),
          _section('Bildirişlər'),
          _card([
            _switchRow(Icons.notifications, 'Push bildirişlər', notifications,
                (v) => setState(() => notifications = v)),
            _divider(),
            _switchRow(Icons.visibility, 'Profilim axtarışda görünür', visibility,
                (v) => setState(() => visibility = v)),
          ]),
          _section('Təhlükəsizlik və dəstək'),
          _card([
            _navRow(Icons.verified_user, 'Təhlükəsizlik məsləhətləri', () {}),
            _divider(),
            _navRow(Icons.help_outline, 'Kömək — AI köməkçi', () {
              Navigator.of(context).popUntil((r) => r.isFirst);
            }),
            _divider(),
            _navRow(Icons.info_outline, 'Haqqında (v0.2.0)', () {}),
            _divider(),
            _navRow(Icons.description, 'Şərtlər və məxfilik', () {}),
          ]),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () {
                widget.state.signOut();
                Navigator.of(context)
                    .popUntil((route) => route.isFirst);
              },
              icon: const Icon(Icons.logout, size: 16),
              label: Text('Çıxış et', style: T.button),
            ),
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
                fontSize: 11,
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

  Widget _row(IconData icon, String label, String value) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        child: Row(
          children: [
            Icon(icon, size: 17, color: Q.mutedFg),
            const SizedBox(width: 12),
            Expanded(
              child: Text(label,
                  style: const TextStyle(fontFamily: T.family, fontSize: 12)),
            ),
            Text(value, style: T.meta.copyWith(fontSize: 11)),
          ],
        ),
      );

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

  Widget _navRow(IconData icon, String label, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        child: Row(
          children: [
            Icon(icon, size: 17, color: Q.mutedFg),
            const SizedBox(width: 12),
            Expanded(
              child: Text(label,
                  style: const TextStyle(fontFamily: T.family, fontSize: 12)),
            ),
            Icon(Icons.chevron_right, size: 18, color: Q.mutedFg),
          ],
        ),
      ),
    );
  }
}
