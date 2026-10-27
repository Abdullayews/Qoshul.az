import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';

/// "Şəhərini seç" — web CityPicker dialoqunun mobil portu.
Future<void> showCityPicker(BuildContext context, AppState state) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _CityPickerSheet(state: state),
  );
}

class _CityPickerSheet extends StatefulWidget {
  final AppState state;
  const _CityPickerSheet({required this.state});

  @override
  State<_CityPickerSheet> createState() => _CityPickerSheetState();
}

class _CityPickerSheetState extends State<_CityPickerSheet> {
  final searchCtrl = TextEditingController();
  String query = '';

  @override
  void dispose() {
    searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final list = cities
        .where((c) => c.name.toLowerCase().contains(query.toLowerCase()))
        .toList();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.7,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
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
            const Text('Şəhərini seç',
                style: TextStyle(
                    fontFamily: T.family,
                    fontSize: 18,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text(
              'Azərbaycanın ${cities.length} şəhər və rayon mərkəzindən birini seç.',
              style: T.subtitle,
            ),
            const SizedBox(height: 12),
            Container(
              height: 43,
              decoration: BoxDecoration(
                color: Q.card,
                border: Border.all(color: Q.border),
                borderRadius: BorderRadius.circular(7),
              ),
              child: Row(
                children: [
                  const SizedBox(width: 12),
                  const Icon(Icons.search, size: 16, color: Q.mutedFg),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: searchCtrl,
                      style: T.input,
                      onChanged: (v) => setState(() => query = v),
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        isDense: true,
                        hintText: 'Şəhər axtar...',
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: list.isEmpty
                  ? Center(child: Text('Şəhər tapılmadı.', style: T.subtitle))
                  : ListView.builder(
                      itemCount: list.length,
                      itemBuilder: (context, i) {
                        final c = list[i];
                        final active = widget.state.city == c.name;
                        return InkWell(
                          onTap: () {
                            widget.state.setCity(c.name);
                            Navigator.of(context).pop();
                          },
                          borderRadius: BorderRadius.circular(12),
                          child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 2),
                            padding: const EdgeInsets.symmetric(
                                horizontal: 12, vertical: 10),
                            decoration: BoxDecoration(
                              color: active ? Q.secondary : Colors.transparent,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  c.name,
                                  style: TextStyle(
                                    fontFamily: T.family,
                                    fontSize: 14,
                                    fontWeight: active
                                        ? FontWeight.w700
                                        : FontWeight.w400,
                                    color: active ? Q.primary : Q.foreground,
                                  ),
                                ),
                                Text(
                                  c.region,
                                  style: const TextStyle(
                                      fontFamily: T.family,
                                      fontSize: 12,
                                      color: Q.mutedFg),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }
}
