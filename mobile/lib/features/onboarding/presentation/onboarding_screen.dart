import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/ui.dart';
import '../../events/presentation/city_picker_sheet.dart';

/// Qeydiyyat anketi — web onboarding.tsx portu (8 addım).
/// UI mərhələsi: məlumatlar yalnız AppState-də saxlanılır (mock).
class OnboardingScreen extends StatefulWidget {
  final AppState state;
  final VoidCallback onDone;
  const OnboardingScreen({super.key, required this.state, required this.onDone});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  int step = 0;
  final firstCtrl = TextEditingController();
  final lastCtrl = TextEditingController();
  final birthCtrl = TextEditingController();
  final openAnswer1 = TextEditingController();
  final openAnswer2 = TextEditingController();

  String gender = 'Kişi';
  String city = 'Bakı';
  String status = 'student';
  String university = universities.first;
  String workField = '';
  final List<String> langs = ['Azərbaycan dili'];
  final List<String> langLevels = [];
  final List<String> interests = <String>[];
  final List<String> goals = <String>[];
  final Map<int, int> personalityAnswers = {}; // sual indexi -> 1..5
  final List<String> availability = <String>[];
  String groupSize = groupSizes[1];
  String budget = budgets[1];
  PersonalityResult? result;

  @override
  void dispose() {
    firstCtrl.dispose();
    lastCtrl.dispose();
    birthCtrl.dispose();
    openAnswer1.dispose();
    openAnswer2.dispose();
    super.dispose();
  }

  bool get _canNext {
    switch (step) {
      case 0:
        return firstCtrl.text.trim().isNotEmpty;
      case 1:
        return true;
      case 2:
        return langs.isNotEmpty;
      case 3:
        return interests.length >= 3;
      case 4:
        return goals.isNotEmpty;
      case 5:
        return personalityAnswers.length >= 6;
      case 6:
        return true;
      default:
        return true;
    }
  }

  void _finish() {
    final pers = _computePersonality();
    widget.state.completeOnboarding(
      first: firstCtrl.text.trim(),
      last: lastCtrl.text.trim(),
      city: city,
      status: status,
      university: status == 'student' ? university : workField,
      langs: langs,
      intr: interests,
      gls: goals,
      pers: pers,
      avail: availability,
      gSize: groupSize,
      bud: budget,
    );
    widget.onDone();
  }

  /// Mock şəxsiyyət hesablaması — Big Five orta dəyərləri (web AI analizinin simulyasiyası).
  PersonalityResult _computePersonality() {
    final sums = <String, List<int>>{
      'extraversion': [],
      'openness': [],
      'conscientiousness': [],
      'agreeableness': [],
      'stability': [],
    };
    personalityQuestions.asMap().forEach((i, q) {
      final trait = q.value.key;
      final reverse = q.value.value;
      var v = personalityAnswers[i] ?? 3;
      if (reverse) v = 6 - v;
      sums[trait]?.add(v);
    });
    final traits = <String, int>{};
    sums.forEach((k, v) {
      if (v.isNotEmpty) {
        traits[k] = ((v.reduce((a, b) => a + b) / v.length) * 20).round();
      }
    });
    final ext = traits['extraversion'] ?? 50;
    final open = traits['openness'] ?? 50;
    String type;
    if (ext >= 60 && open >= 60) {
      type = 'Sosial Kəşfiyyatçı';
    } else if (ext >= 60) {
      type = 'Enerjik Təşkilatçı';
    } else if (open >= 60) {
      type = 'Sakit Yaradıcı';
    } else {
      type = 'Etibarlı Dost';
    }
    return PersonalityResult(
      type: type,
      summary:
          '$type tipi qrup görüşlərində ${ext >= 60 ? 'aktiv ünsiyyət' : 'dərin söhbət'} axtarır. '
          'AI bu profili yaxın insanları tövsiyə etmək üçün işlədəcək.',
      traits: traits,
    );
  }

  @override
  Widget build(BuildContext context) {
    final isLast = step == onboardingSteps.length - 1;
    return Scaffold(
      backgroundColor: Q.background,
      body: SafeArea(
        child: Column(
          children: [
            // — progress başlıq —
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 20, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      if (step > 0)
                        IconButton(
                          onPressed: () => setState(() => step--),
                          icon: const Icon(Icons.chevron_left, size: 24),
                          color: Q.primary,
                          padding: EdgeInsets.zero,
                        ),
                      Expanded(
                        child: Text(
                          'Qeydiyyat anketi',
                          textAlign: step > 0 ? TextAlign.center : TextAlign.left,
                          style: T.h2.copyWith(fontSize: 16),
                        ),
                      ),
                      SizedBox(width: step > 0 ? 40 : 0),
                    ],
                  ),
                  const SizedBox(height: 10),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: SizedBox(
                      height: 4,
                      child: LinearProgressIndicator(
                        value: (step + 1) / onboardingSteps.length,
                        minHeight: 4,
                        backgroundColor: Q.border,
                        valueColor:
                            const AlwaysStoppedAnimation<Color>(Q.primary),
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${step + 1}/${onboardingSteps.length} · ${onboardingSteps[step]}',
                    style: T.meta,
                  ),
                ],
              ),
            ),
            // — məzmun —
            Expanded(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 180),
                child: ListView(
                  key: ValueKey(step),
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                  children: _stepContent(),
                ),
              ),
            ),
            // — naviqasiya —
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _canNext
                      ? () {
                          if (isLast) {
                            _finish();
                          } else {
                            setState(() => step++);
                          }
                        }
                      : null,
                  icon: isLast
                      ? const Icon(Icons.check, size: 16)
                      : const Icon(Icons.chevron_right, size: 16),
                  label: Text(isLast ? 'Bitir' : 'Davam et', style: T.button),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _stepContent() {
    switch (step) {
      case 0:
        return [
          _q('Adın nədir?'),
          FormFieldX(
            label: 'Ad',
            child: TextField(controller: firstCtrl, style: T.input),
          ),
          FormFieldX(
            label: 'Soyad',
            child: TextField(controller: lastCtrl, style: T.input),
          ),
          FormFieldX(
            label: 'Doğum tarixi',
            child: TextField(
              controller: birthCtrl,
              readOnly: true,
              style: T.input,
              onTap: () async {
                final d = await showDatePicker(
                  context: context,
                  initialDate: DateTime(2000),
                  firstDate: DateTime(1940),
                  lastDate: DateTime.now(),
                );
                if (d != null) {
                  birthCtrl.text =
                      '${d.day.toString().padLeft(2, '0')}.${d.month.toString().padLeft(2, '0')}.${d.year}';
                }
              },
              decoration:
                  const InputDecoration(hintText: 'Gün.Ay.İl'),
            ),
          ),
          FormFieldX(
            label: 'Cins',
            child: DropdownButtonFormField<String>(
              initialValue: gender,
              items: const [
                DropdownMenuItem(value: 'Kişi', child: Text('Kişi')),
                DropdownMenuItem(value: 'Qadın', child: Text('Qadın')),
                DropdownMenuItem(value: 'Digər', child: Text('Digər')),
              ],
              onChanged: (v) => setState(() => gender = v ?? gender),
            ),
          ),
          FormFieldX(
            label: 'Şəhər',
            child: InkWell(
              onTap: () => showCityPicker(context, widget.state),
              child: InputDecorator(
                decoration: const InputDecoration(),
                child: Text(city, style: T.input),
              ),
            ),
          ),
        ];
      case 1:
        return [
          _q('Hazırda nə ilə məşğulsan?'),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final s in statuses)
                _chip(
                  statusLabel(s),
                  active: status == s,
                  onTap: () => setState(() => status = s),
                ),
            ],
          ),
          if (status == 'student' || status == 'school')
            FormFieldX(
              label: 'Universitet / məktəb',
              child: DropdownButtonFormField<String>(
                initialValue: university,
                items: [
                  for (final u in universities)
                    DropdownMenuItem(value: u, child: Text(u, style: T.input)),
                ],
                onChanged: (v) =>
                    setState(() => university = v ?? university),
              ),
            ),
          if (const ['working', 'job_seeking', 'entrepreneur', 'freelancer']
              .contains(status))
            FormFieldX(
              label: 'İş sahən',
              child: TextField(
                onChanged: (v) => workField = v,
                style: T.input,
                decoration: const InputDecoration(
                    hintText: 'Məsələn, IT, təhsil, ticarət...'),
              ),
            ),
        ];
      case 2:
        return [
          _q('Hansı dillərdə ünsiyyət qurursan?'),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final l in languageOptions)
                _chip(
                  l,
                  active: langs.contains(l),
                  onTap: () => setState(() {
                    langs.contains(l) ? langs.remove(l) : langs.add(l);
                  }),
                ),
            ],
          ),
        ];
      case 3:
        return [
          _q('Maraqlarını seç (ən azı 3)'),
          Text('Bu, AI-ya sənə uyğun tədbir və insanları tapmağa kömək edir.',
              style: T.subtitle),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final g in categoryGroups)
                for (final item in g.items)
                  _chip(
                    item,
                    active: interests.contains(item),
                    onTap: () => setState(() {
                      interests.contains(item)
                          ? interests.remove(item)
                          : interests.add(item);
                    }),
                  ),
            ],
          ),
        ];
      case 4:
        return [
          _q('Qoshul ilə nə hədəfləyirsən?'),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final g in goals)
                _chip(
                  g,
                  active: this.goals.contains(g),
                  onTap: () => setState(() {
                    this.goals.contains(g)
                        ? this.goals.remove(g)
                        : this.goals.add(g);
                  }),
                ),
            ],
          ),
        ];
      case 5:
        return [
          _q('Şəxsiyyət testi'),
          Text('1 (razı deyiləm) — 5 (tam razıyam). Ən azı 6 suala cavab ver.',
              style: T.subtitle),
          const SizedBox(height: 10),
          for (var i = 0; i < personalityQuestions.length; i++)
            Container(
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Q.card,
                border: Border.all(color: Q.border),
                borderRadius: BorderRadius.circular(Q.rLg),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    personalityQuestions[i].key,
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 12,
                        height: 1.5),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      for (var v = 1; v <= 5; v++)
                        GestureDetector(
                          onTap: () =>
                              setState(() => personalityAnswers[i] = v),
                          child: Container(
                            width: 38,
                            height: 38,
                            decoration: BoxDecoration(
                              color: personalityAnswers[i] == v
                                  ? Q.primary
                                  : Q.muted,
                              shape: BoxShape.circle,
                            ),
                            alignment: Alignment.center,
                            child: Text(
                              '$v',
                              style: TextStyle(
                                fontFamily: T.family,
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: personalityAnswers[i] == v
                                    ? Q.primaryFg
                                    : Q.mutedFg,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
        ];
      case 6:
        return [
          _q('Bir az da özündən danış'),
          FormFieldX(
            label: 'Boş vaxtında nə edirsən?',
            child: TextField(
              controller: openAnswer1,
              maxLines: 3,
              maxLength: 500,
              style: T.input,
              decoration: const InputDecoration(counterText: ''),
            ),
          ),
          FormFieldX(
            label: 'İdeal görüşün necə görünür?',
            child: TextField(
              controller: openAnswer2,
              maxLines: 3,
              maxLength: 500,
              style: T.input,
              decoration: const InputDecoration(counterText: ''),
            ),
          ),
        ];
      default:
        return [
          _q('Üstünlüklərin'),
          _label('Nə vaxt müsaitsən?'),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final a in availabilityOptions)
                _chip(
                  a,
                  active: availability.contains(a),
                  onTap: () => setState(() {
                    availability.contains(a)
                        ? availability.remove(a)
                        : availability.add(a);
                  }),
                ),
            ],
          ),
          FormFieldX(
            label: 'Neçə nəfərlik qrupda rahatsan?',
            child: DropdownButtonFormField<String>(
              initialValue: groupSize,
              items: [
                for (final g in groupSizes)
                  DropdownMenuItem(value: g, child: Text(g, style: T.input)),
              ],
              onChanged: (v) => setState(() => groupSize = v ?? groupSize),
            ),
          ),
          FormFieldX(
            label: 'Tədbir büdcən',
            child: DropdownButtonFormField<String>(
              initialValue: budget,
              items: [
                for (final b in budgets)
                  DropdownMenuItem(value: b, child: Text(b, style: T.input)),
              ],
              onChanged: (v) => setState(() => budget = v ?? budget),
            ),
          ),
        ];
    }
  }

  Widget _q(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Text(text,
            style: T.h2.copyWith(fontSize: 17)),
      );

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(top: 10, bottom: 10),
        child: Text(text,
            style: const TextStyle(
                fontFamily: T.family,
                fontSize: 11,
                fontWeight: FontWeight.w600)),
      );

  Widget _chip(String label, {required bool active, required VoidCallback onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: active ? Q.secondary : Q.card,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: active ? Q.primary : Q.border),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: T.family,
            fontSize: 12,
            color: active ? Q.primary : Q.mutedFg,
          ),
        ),
      ),
    );
  }
}
