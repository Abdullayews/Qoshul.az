import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';

/// "Birlikdə yeni plan quraq" — web CreateEventDialog-un mobil portu.
/// [initial] — Assistant [TƏDBİR] təklifindən gələn prefill.
Future<void> showCreateEventSheet(
  BuildContext context,
  AppState state, {
  EventProposal? initial,
  String presetCity = '',
  Partner? venue,
}) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    builder: (_) => _CreateSheet(
      state: state,
      initial: initial,
      presetCity: presetCity,
      venue: venue,
    ),
  );
}

class _CreateSheet extends StatefulWidget {
  final AppState state;
  final EventProposal? initial;
  final String presetCity;
  final Partner? venue;

  const _CreateSheet({
    required this.state,
    this.initial,
    this.presetCity = '',
    this.venue,
  });

  @override
  State<_CreateSheet> createState() => _CreateSheetState();
}

class _CreateSheetState extends State<_CreateSheet> {
  final _formKey = GlobalKey<FormState>();
  final titleCtrl = TextEditingController();
  final ideaCtrl = TextEditingController();
  final locationCtrl = TextEditingController();
  final descCtrl = TextEditingController();
  final tagsCtrl = TextEditingController();
  final priceCtrl = TextEditingController();

  late String city;
  String category = 'Qəhvə';
  String customCategory = '';
  String format = 'Oflayn';
  DateTime date = DateTime.now().add(const Duration(days: 1));
  TimeOfDay time = const TimeOfDay(hour: 18, minute: 0);
  int capacity = 8;
  bool paid = false;
  bool approval = false;
  bool aiBusy = false;

  String? _aiLocation;

  @override
  void initState() {
    super.initState();
    city = widget.presetCity.isNotEmpty
        ? widget.presetCity
        : widget.venue?.city ?? widget.state.city;
    if (widget.venue != null) {
      locationCtrl.text = '${widget.venue!.name}, ${widget.venue!.address}';
    }
    final p = widget.initial;
    if (p != null) {
      titleCtrl.text = p.title;
      if (allCategories.contains(p.category)) category = p.category;
      descCtrl.text = p.description;
      if (RegExp(r'^\d{2}:\d{2}$').hasMatch(p.time)) {
        final parts = p.time.split(':');
        time = TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
      }
      if (RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(p.date)) {
        final d = DateTime.tryParse(p.date);
        if (d != null) date = d;
      }
      if (p.capacity >= 2 && p.capacity <= 500) capacity = p.capacity;
      if (p.location.isNotEmpty) _aiLocation = p.location;
    }
  }

  @override
  void dispose() {
    titleCtrl.dispose();
    ideaCtrl.dispose();
    locationCtrl.dispose();
    descCtrl.dispose();
    tagsCtrl.dispose();
    priceCtrl.dispose();
    super.dispose();
  }

  String get _dateLabel =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  String get _timeLabel =>
      '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';

  /// Mock AI doldurma — web "Tədbiri AI ilə doldur" düyməsinin simulyasiyası.
  void _aiFill() {
    setState(() => aiBusy = true);
    Future.delayed(const Duration(milliseconds: 1100), () {
      if (!mounted) return;
      final idea = ideaCtrl.text.toLowerCase();
      setState(() {
        if (idea.contains('dağ') || idea.contains('yürüş')) {
          titleCtrl.text = 'Şənbə səhəri dağa yürüş';
          category = 'Yürüş';
          descCtrl.text =
              'Orta çətinlikdə marşrut, gözəl mənzərəli dayanacaqlar. Su və rahat ayaqqabı yanına al.';
        } else if (idea.contains('qəhvə') || idea.contains('çay')) {
          titleCtrl.text = 'Qəhvə və tanışlıq səhəri';
          category = 'Qəhvə';
          descCtrl.text =
              'Səhər qəhvəsi ətrafında asan söhbət. Yeni insanlarla tanış ol, həftəni planlaşdır.';
        } else {
          titleCtrl.text = 'Şənbə üçün birgə plan';
          category = 'Stolüstü oyunlar';
          descCtrl.text =
              'Maraqlı keçirmək istəyənlər üçün açıq dəvət. Oyunlar, söhbət, yaxşı əhval.';
        }
        capacity = 10;
        aiBusy = false;
      });
    });
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;
    widget.state.createEvent(
      title: titleCtrl.text.trim(),
      category: category == 'Digər' && customCategory.trim().isNotEmpty
          ? customCategory.trim()
          : category,
      city: city,
      location: _aiLocation ?? locationCtrl.text.trim(),
      date: _dateLabel,
      time: _timeLabel,
      capacity: capacity,
      description: descCtrl.text.trim(),
      price: paid ? (int.tryParse(priceCtrl.text) ?? 0) : 0,
      language: 'Azərbaycan dili',
      level: 'Hamı üçün',
      format: format,
      approval: approval,
      tags: tagsCtrl.text
          .split(',')
          .map((t) => t.trim())
          .where((t) => t.isNotEmpty)
          .toList(),
    );
    Navigator.of(context).pop();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Tədbir yaradıldı! +${XpRules.hostingXp} XP')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.only(bottom: bottom),
      child: DraggableScrollableSheet(
        initialChildSize: 0.9,
        minChildSize: 0.5,
        maxChildSize: 0.97,
        expand: false,
        builder: (context, scrollController) => Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: Form(
            key: _formKey,
            child: ListView(
              controller: scrollController,
              children: [
                Center(
                  child: Container(
                    width: 36,
                    height: 4,
                    margin: const EdgeInsets.only(top: 10, bottom: 14),
                    decoration: BoxDecoration(
                        color: Q.border,
                        borderRadius: BorderRadius.circular(2)),
                  ),
                ),
                const Text('Birlikdə yeni plan quraq', style: T.h2),
                const SizedBox(height: 4),
                Text(
                  'İstədiyin formada tədbir yarat və maraqlarını bölüşən insanlarla tanış ol.',
                  style: T.subtitle,
                ),
                // — AI bloku —
                AiBox(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('AI köməkçisi: fikrini qısa yaz',
                          style: TextStyle(
                              fontFamily: T.family,
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600)),
                      const SizedBox(height: 7),
                      Row(
                        children: [
                          Icon(Icons.auto_fix_high, size: 14, color: Q.primary),
                          const SizedBox(width: 6),
                          Expanded(
                            child: TextField(
                              controller: ideaCtrl,
                              style: T.input,
                              decoration: const InputDecoration(
                                hintText: 'Məsələn, şənbə səhəri dağa yürüş',
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      SizedBox(
                        width: double.infinity,
                        child: FilledButton.icon(
                          onPressed: aiBusy || ideaCtrl.text.trim().isEmpty
                              ? null
                              : _aiFill,
                          style: FilledButton.styleFrom(
                            backgroundColor: Q.secondary,
                            foregroundColor: Q.primary,
                            disabledBackgroundColor: Q.muted,
                            disabledForegroundColor: Q.mutedFg,
                          ),
                          icon: aiBusy
                              ? const SizedBox(
                                  width: 14,
                                  height: 14,
                                  child: CircularProgressIndicator(
                                      strokeWidth: 2))
                              : const Icon(Icons.auto_fix_high, size: 16),
                          label: Text(
                              aiBusy ? 'Hazırlanır...' : 'Tədbiri AI ilə doldur',
                              style: T.button),
                        ),
                      ),
                    ],
                  ),
                ),
                // — Forma —
                FormFieldX(
                  label: 'Tədbirin adı',
                  child: TextFormField(
                    controller: titleCtrl,
                    maxLength: 65,
                    style: T.input,
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Adı yaz' : null,
                    decoration:
                        const InputDecoration(hintText: 'Məsələn, şənbə günü qəhvə görüşü', counterText: ''),
                  ),
                ),
                FormFieldX(
                  label: 'Kateqoriya',
                  child: DropdownButtonFormField<String>(
                    initialValue: allCategories.contains(category) ? category : null,
                    items: [
                      for (final g in categoryGroups)
                        DropdownMenuItem<String>(
                          enabled: false,
                          value: null,
                          child: Text(g.group,
                              style: const TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 12.5,
                                  fontWeight: FontWeight.w800,
                                  color: Q.mutedFg)),
                        ),
                      for (final item in allCategories)
                        DropdownMenuItem<String>(
                          value: item,
                          child: Text(item, style: T.input),
                        ),
                    ],
                    onChanged: (v) => setState(() => category = v ?? 'Qəhvə'),
                    decoration: const InputDecoration(),
                  ),
                ),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: FormFieldX(
                        label: 'Format',
                        child: DropdownButtonFormField<String>(
                          initialValue: format,
                          items: const [
                            DropdownMenuItem(value: 'Oflayn', child: Text('Oflayn')),
                            DropdownMenuItem(value: 'Onlayn', child: Text('Onlayn')),
                          ],
                          onChanged: (v) =>
                              setState(() => format = v ?? 'Oflayn'),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: FormFieldX(
                        label: 'Şəhər',
                        child: DropdownButtonFormField<String>(
                          initialValue: city,
                          items: [
                            for (final c in cities)
                              DropdownMenuItem(
                                  value: c.name, child: Text(c.name, style: T.input)),
                          ],
                          onChanged: (v) => setState(() => city = v ?? city),
                        ),
                      ),
                    ),
                  ],
                ),
                if (format == 'Oflayn')
                  FormFieldX(
                    label: 'Məkan və ünvan',
                    child: TextFormField(
                      controller: locationCtrl,
                      style: T.input,
                      validator: (v) =>
                          v == null || v.trim().isEmpty ? 'Məkanı yaz' : null,
                      decoration: const InputDecoration(
                          hintText: 'Məkanın adı və ünvanı'),
                    ),
                  ),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: FormFieldX(
                        label: 'Tarix',
                        child: InkWell(
                          onTap: () async {
                            final d = await showDatePicker(
                              context: context,
                              initialDate: date,
                              firstDate: DateTime.now(),
                              lastDate:
                                  DateTime.now().add(const Duration(days: 365)),
                            );
                            if (d != null) setState(() => date = d);
                          },
                          child: InputDecorator(
                            decoration: const InputDecoration(),
                            child: Text(_dateLabel, style: T.input),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: FormFieldX(
                        label: 'Saat',
                        child: InkWell(
                          onTap: () async {
                            final t = await showTimePicker(
                                context: context, initialTime: time);
                            if (t != null) setState(() => time = t);
                          },
                          child: InputDecorator(
                            decoration: const InputDecoration(),
                            child: Text(_timeLabel, style: T.input),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: FormFieldX(
                        label: 'İştirakçı sayı',
                        child: TextFormField(
                          initialValue: '$capacity',
                          keyboardType: TextInputType.number,
                          style: T.input,
                          validator: (v) {
                            final n = int.tryParse(v ?? '');
                            if (n == null || n < 2 || n > 500) {
                              return '2–500 arası';
                            }
                            return null;
                          },
                          onChanged: (v) =>
                              capacity = int.tryParse(v) ?? capacity,
                          decoration: const InputDecoration(),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: FormFieldX(
                        label: 'Təkrarlanma',
                        child: DropdownButtonFormField<String>(
                          initialValue: 'Bir dəfəlik',
                          items: const [
                            DropdownMenuItem(
                                value: 'Bir dəfəlik',
                                child: Text('Bir dəfəlik')),
                            DropdownMenuItem(
                                value: 'Hər həftə', child: Text('Hər həftə')),
                            DropdownMenuItem(
                                value: 'İki həftədən bir',
                                child: Text('İki həftədən bir')),
                            DropdownMenuItem(
                                value: 'Hər ay', child: Text('Hər ay')),
                          ],
                          onChanged: (_) {},
                        ),
                      ),
                    ),
                  ],
                ),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: FormFieldX(
                        label: 'İştirak haqqı',
                        child: DropdownButtonFormField<String>(
                          initialValue: paid ? '1' : '0',
                          items: const [
                            DropdownMenuItem(value: '0', child: Text('Pulsuz')),
                            DropdownMenuItem(value: '1', child: Text('Ödənişli')),
                          ],
                          onChanged: (v) => setState(() => paid = v == '1'),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: paid
                          ? FormFieldX(
                              label: 'Qiymət (AZN)',
                              child: TextFormField(
                                controller: priceCtrl,
                                keyboardType: TextInputType.number,
                                style: T.input,
                                validator: (v) {
                                  final n = int.tryParse(v ?? '');
                                  if (n == null || n < 1) return 'Min 1 AZN';
                                  return null;
                                },
                                decoration: const InputDecoration(),
                              ),
                            )
                          : FormFieldX(
                              label: 'Yaş həddi',
                              child: DropdownButtonFormField<String>(
                                initialValue: 'Hamı üçün',
                                items: const [
                                  DropdownMenuItem(
                                      value: 'Hamı üçün', child: Text('Hamı üçün')),
                                  DropdownMenuItem(value: '16+', child: Text('16+')),
                                  DropdownMenuItem(value: '18+', child: Text('18+')),
                                  DropdownMenuItem(
                                      value: '18–25', child: Text('18–25')),
                                  DropdownMenuItem(
                                      value: '25–35', child: Text('25–35')),
                                ],
                                onChanged: (_) {},
                              ),
                            ),
                    ),
                  ],
                ),
                FormFieldX(
                  label: 'Teqlər (vergüllə)',
                  child: TextFormField(
                    controller: tagsCtrl,
                    style: T.input,
                    decoration: const InputDecoration(
                        hintText: 'tələbələr, açıq hava, pulsuz'),
                  ),
                ),
                const SizedBox(height: 12),
                GestureDetector(
                  onTap: () => setState(() => approval = !approval),
                  behavior: HitTestBehavior.opaque,
                  child: Row(
                    children: [
                      SizedBox(
                        width: 20,
                        height: 20,
                        child: Checkbox(value: approval, onChanged: (v) => setState(() => approval = v ?? false)),
                      ),
                      const SizedBox(width: 10),
                      const Expanded(
                        child: Text('Qoşulma istəklərini özüm təsdiqləyim',
                            style: TextStyle(fontFamily: T.family, fontSize: 12)),
                      ),
                    ],
                  ),
                ),
                FormFieldX(
                  label: 'Tədbir haqqında',
                  child: TextFormField(
                    controller: descCtrl,
                    maxLines: 4,
                    maxLength: 2000,
                    style: T.input,
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Təsviri yaz' : null,
                    decoration: const InputDecoration(
                        hintText: 'Birlikdə nə edəcəyik?', counterText: ''),
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: _submit,
                    icon: const Icon(Icons.add, size: 16),
                    label: Text('Tədbir yarat', style: T.button),
                  ),
                ),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
