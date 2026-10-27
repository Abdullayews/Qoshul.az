import 'package:flutter/material.dart';

import '../../../core/data/mock_data.dart';
import '../../../core/models/models.dart';
import '../../../core/state/app_state.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/brand.dart';
import '../../../core/widgets/ui.dart';

/// Onboarding — ROL (docs/product-spec.md, bölmə B) axını:
/// 0) 3 slaydlıq tanışlıq (Kəşf et / Qoşul / Qazan) + "Keç"
/// 1) Profil yaradılması: ad, doğum tarixi (18+), cins (ist.), şəkil (1-6), bio (150)
/// 2) Kateqoriya seçimi: 9 maraq, min 3, max 8
/// 3) İcazələr: məkan + bildirişlər (rdd edilə bilər)
/// Bitiş: +50 XP (qeydiyyat bonusu).
class OnboardingScreen extends StatefulWidget {
  final AppState state;
  final VoidCallback onDone;
  const OnboardingScreen({super.key, required this.state, required this.onDone});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  int step = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Q.background,
      body: SafeArea(
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 200),
          child: KeyedSubtree(
            key: ValueKey(step),
            child: step == 0
                ? _WelcomeStep(onNext: () => setState(() => step = 1))
                : step == 1
                    ? _ProfileStep(
                        state: widget.state,
                        onNext: () => setState(() => step = 2),
                      )
                    : step == 2
                        ? _InterestsStep(
                            state: widget.state,
                            onNext: () => setState(() => step = 3),
                          )
                        : _PermissionsStep(
                            state: widget.state,
                            onDone: () {
                              widget.state.finishOnboarding();
                              widget.onDone();
                            },
                          ),
          ),
        ),
      ),
    );
  }
}

// ───────────────────────── Addım 0: tanışlıq slaydları ─────────────────────────

class _WelcomeStep extends StatefulWidget {
  final VoidCallback onNext;
  const _WelcomeStep({required this.onNext});

  @override
  State<_WelcomeStep> createState() => _WelcomeStepState();
}

class _WelcomeStepState extends State<_WelcomeStep> {
  int page = 0;
  final ctrl = PageController();

  static const _slides = <(IconData, String, String)>[
    (Icons.explore, 'Kəşf et',
        'Yaxınlığındakı insanları, tədbirləri və məkanları kəşf et. Ortaq maraqlar sizi birləşdirir.'),
    (Icons.group_add, 'Qoşul',
        'Tədbirlərə qoşul, stol rezerv et, qrup söhbətlərində tanış olduğun insanlarla plan et.'),
    (Icons.emoji_events, 'Qazan',
        'Hər addımda XP qazan. XP ilə səviyyə qalx və partnyor məkanlarda endirim kuponları al.'),
  ];

  @override
  void dispose() {
    ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: widget.onNext,
            child: const Text('Keç', style: TextStyle(fontFamily: T.family, fontSize: 12)),
          ),
        ),
        Expanded(
          child: PageView(
            controller: ctrl,
            onPageChanged: (i) => setState(() => page = i),
            children: [
              for (final (icon, title, text) in _slides)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 34),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 84,
                        height: 84,
                        decoration: BoxDecoration(
                          color: Q.secondary,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(icon, size: 38, color: Q.primary),
                      ),
                      const SizedBox(height: 28),
                      Text(title, style: T.hero),
                      const SizedBox(height: 12),
                      Text(text,
                          style: T.subtitle.copyWith(fontSize: 13, height: 1.8)),
                    ],
                  ),
                ),
            ],
          ),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var i = 0; i < _slides.length; i++)
              Container(
                width: i == page ? 18 : 6,
                height: 6,
                margin: const EdgeInsets.symmetric(horizontal: 3),
                decoration: BoxDecoration(
                  color: i == page ? Q.primary : Q.border,
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
          ],
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(28, 24, 28, 22),
          child: SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: page < _slides.length - 1
                  ? () => ctrl.nextPage(
                      duration: const Duration(milliseconds: 250),
                      curve: Curves.easeInOut)
                  : widget.onNext,
              child: Text(
                page < _slides.length - 1 ? 'İrəli' : 'Başlayaq!',
                style: T.button,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

// ───────────────────────── Addım 1: profil yaradılışı ─────────────────────────

class _ProfileStep extends StatefulWidget {
  final AppState state;
  final VoidCallback onNext;
  const _ProfileStep({required this.state, required this.onNext});

  @override
  State<_ProfileStep> createState() => _ProfileStepState();
}

class _ProfileStepState extends State<_ProfileStep> {
  final nameCtrl = TextEditingController();
  final bioCtrl = TextEditingController();
  DateTime? birth;
  String gender = '';
  int photos = 1;
  String? error;

  @override
  void dispose() {
    nameCtrl.dispose();
    bioCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickBirth() async {
    final now = DateTime.now();
    final d = await showDatePicker(
      context: context,
      initialDate: DateTime(now.year - 22),
      firstDate: DateTime(now.year - 90),
      lastDate: now,
      helpText: 'Doğum tarixi',
    );
    if (d != null) setState(() => birth = d);
  }

  void _next() {
    final parts = nameCtrl.text.trim().split(RegExp(r'\s+'));
    if (nameCtrl.text.trim().isEmpty) {
      setState(() => error = 'Adını yaz.');
      return;
    }
    if (birth == null) {
      setState(() => error = 'Doğum tarixini seç.');
      return;
    }
    if (!XpRules.isAdult(birth!)) {
      setState(() => error = 'Qoshul 18+ platformadır. (Rəsmi yaş həddi təsdiq gözləyir)');
      return;
    }
    widget.state.setProfileCreation(
      first: parts.first,
      last: parts.length > 1 ? parts.sublist(1).join(' ') : '',
      birth: birth!,
      gdr: gender,
      bioText: bioCtrl.text.trim(),
      photos: photos,
    );
    widget.onNext();
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
      children: [
        const Wordmark(fontSize: 22),
        const SizedBox(height: 14),
        const Text('Profilini yarad', style: T.h1),
        const SizedBox(height: 4),
        const Text('Səni tanımağa başlayaq — sonra maraqlarını seçəcəyik.',
            style: T.subtitle),
        FormFieldX(
          label: 'Ad və soyad',
          child: TextField(
            controller: nameCtrl,
            maxLength: 60,
            style: T.input,
            decoration: const InputDecoration(hintText: 'Məs: Aysel Əliyeva', counterText: ''),
          ),
        ),
        FormFieldX(
          label: 'Doğum tarixi (18+)',
          hint: 'Yaş həddi yoxlaması aparılır',
          child: OutlinedButton.icon(
            onPressed: _pickBirth,
            icon: const Icon(Icons.cake, size: 16),
            label: Text(
              birth == null
                  ? 'Tarix seç'
                  : '${birth!.day.toString().padLeft(2, '0')}.'
                      '${birth!.month.toString().padLeft(2, '0')}.${birth!.year}',
              style: T.button,
            ),
          ),
        ),
        FormFieldX(
          label: 'Cins (isteğe bağlı)',
          child: Wrap(
            spacing: 8,
            children: [
              for (final g in const ['Kişi', 'Qadın', 'Digər'])
                CategoryChip(
                  label: g,
                  icon: g == 'Kişi'
                      ? Icons.male
                      : g == 'Qadın'
                          ? Icons.female
                          : Icons.more_horiz,
                  active: gender == g,
                  onTap: () => setState(() =>
                      gender = gender == g ? '' : g),
                ),
            ],
          ),
        ),
        FormFieldX(
          label: 'Şəkil ($photos/${XpRules.maxPhotos})',
          hint: 'Azı 1 şəkil məcburidir — yüklənəndə moderasiyaya düşür',
          child: Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (var i = 1; i <= XpRules.maxPhotos; i++)
                GestureDetector(
                  onTap: () => setState(() => photos = i),
                  child: Container(
                    width: 46,
                    height: 46,
                    decoration: BoxDecoration(
                      color: i <= photos ? Q.secondary : Q.card,
                      border: Border.all(
                          color: i <= photos ? Q.primary : Q.border, width: 1.4),
                      borderRadius: BorderRadius.circular(Q.rMd),
                    ),
                    child: Icon(
                      i <= photos ? Icons.check : Icons.add_photo_alternate_outlined,
                      size: 19,
                      color: i <= photos ? Q.primary : Q.mutedFg,
                    ),
                  ),
                ),
            ],
          ),
        ),
        FormFieldX(
          label: 'Qısa bio (${bioCtrl.text.length}/${XpRules.bioLimit})',
          child: TextField(
            controller: bioCtrl,
            maxLength: XpRules.bioLimit,
            maxLines: 3,
            style: T.input,
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(
              hintText: 'Özün haqqında bir-iki cümlə...',
            ),
          ),
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(error!,
                style: const TextStyle(
                    fontFamily: T.family, fontSize: 11, color: Q.destructive)),
          ),
        const SizedBox(height: 18),
        SizedBox(
          width: double.infinity,
          child: FilledButton(onPressed: _next, child: const Text('Davam et')),
        ),
      ],
    );
  }
}

// ───────────────────────── Addım 2: maraqlar (min 3, max 8) ─────────────────────────

class _InterestsStep extends StatefulWidget {
  final AppState state;
  final VoidCallback onNext;
  const _InterestsStep({required this.state, required this.onNext});

  @override
  State<_InterestsStep> createState() => _InterestsStepState();
}

class _InterestsStepState extends State<_InterestsStep> {
  final Set<String> selected = <String>{};

  static const _icons = <String, IconData>{
    'Əyləncə və Gecə həyatı': Icons.nightlife,
    'Kafe və Yemək': Icons.local_cafe,
    'İdman və Aktivlik': Icons.sports_volleyball,
    'Təbiət və Səyahət': Icons.terrain,
    'Mədəniyyət və İncəsənət': Icons.palette,
    'Təhsil və Karyera': Icons.business_center,
    'Oyun və Hobbi': Icons.casino,
    'Musiqi': Icons.music_note,
    'Könüllülük və Sosial': Icons.volunteer_activism,
  };

  void _toggle(String c) {
    setState(() {
      if (selected.contains(c)) {
        selected.remove(c);
      } else if (selected.length < XpRules.maxInterests) {
        selected.add(c);
      }
    });
  }

  void _next() {
    if (selected.length < XpRules.minInterests) return;
    widget.state.setInterests(selected.toList(growable: false));
    widget.onNext();
  }

  @override
  Widget build(BuildContext context) {
    final ok = selected.length >= XpRules.minInterests;
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
      children: [
        const Wordmark(fontSize: 22),
        const SizedBox(height: 14),
        const Text('Maraqlarını seç', style: T.h1),
        const SizedBox(height: 4),
        Text(
          'Ən azı ${XpRules.minInterests}, ən çoxu ${XpRules.maxInterests} kateqoriya. '
          'Bunlar kəşf axınını, match sırasını və bildirişləri müəyyən edir.',
          style: T.subtitle,
        ),
        const SizedBox(height: 8),
        Text('Seçilib: ${selected.length}/${XpRules.maxInterests}',
            style: const TextStyle(
                fontFamily: T.family, fontSize: 11, fontWeight: FontWeight.w700)),
        const SizedBox(height: 14),
        for (final c in rolCategories)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: GestureDetector(
              onTap: () => _toggle(c),
              behavior: HitTestBehavior.opaque,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 120),
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
                decoration: BoxDecoration(
                  color: selected.contains(c) ? Q.secondary : Q.card,
                  border: Border.all(
                      color: selected.contains(c) ? Q.primary : Q.border,
                      width: selected.contains(c) ? 1.4 : 1),
                  borderRadius: BorderRadius.circular(Q.rLg),
                ),
                child: Row(
                  children: [
                    Icon(
                      _icons[c] ?? Icons.circle,
                      size: 19,
                      color: selected.contains(c) ? Q.primary : Q.mutedFg,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(c,
                              style: const TextStyle(
                                  fontFamily: T.family,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w700)),
                          const SizedBox(height: 2),
                          Text(rolCategoryHints[c] ?? '',
                              style: T.meta),
                        ],
                      ),
                    ),
                    if (selected.contains(c))
                      const Icon(Icons.check_circle, size: 19, color: Q.primary),
                  ],
                ),
              ),
            ),
          ),
        const SizedBox(height: 8),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: ok ? _next : null,
            child: Text(ok
                ? 'Davam et (${selected.length} maraq)'
                : 'Ən azı ${XpRules.minInterests} maraq seç'),
          ),
        ),
      ],
    );
  }
}

// ───────────────────────── Addım 3: icazələr ─────────────────────────

class _PermissionsStep extends StatefulWidget {
  final AppState state;
  final VoidCallback onDone;
  const _PermissionsStep({required this.state, required this.onDone});

  @override
  State<_PermissionsStep> createState() => _PermissionsStepState();
}

class _PermissionsStepState extends State<_PermissionsStep> {
  bool location = false;
  bool notifications = false;
  bool asked = false;

  void _askLocation() {
    // Mock icazə dialoqu — rdd edilə bilər (ROL B Addım 5).
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('Məkan icazəsi'),
        content: const Text(
          'Yaxınlığındakı insanları və tədbirləri göstərmək üçün məkan '
          '(tətbiq istifadə olunarkən) icazəsi lazımdır.\n\n'
          'Dəqiq koordinatın heç kimə göstərilmir — yalnız təxmini məsafə '
          '(məs. "~2 km").',
          style: TextStyle(fontFamily: T.family, fontSize: 12, height: 1.7),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(d),
            child: const Text('Rədd et'),
          ),
          FilledButton(
            onPressed: () {
              setState(() => location = true);
              Navigator.pop(d);
            },
            child: const Text('İcazə ver'),
          ),
        ],
      ),
    );
    setState(() => asked = true);
  }

  void _askNotifications() {
    showDialog<void>(
      context: context,
      builder: (d) => AlertDialog(
        title: const Text('Bildirişlər'),
        content: const Text(
          'Match-lər, mesajlar, tədbir xatırlatmaları və rezerv yenilikləri '
          'üçün push bildirişlər. Hər tip sonra ayarlardan ayrıca söndürülə bilər.',
          style: TextStyle(fontFamily: T.family, fontSize: 12, height: 1.7),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(d),
            child: const Text('Rədd et'),
          ),
          FilledButton(
            onPressed: () {
              setState(() => notifications = true);
              Navigator.pop(d);
            },
            child: const Text('İcazə ver'),
          ),
        ],
      ),
    );
    setState(() => asked = true);
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 18, 24, 24),
      children: [
        const Wordmark(fontSize: 22),
        const SizedBox(height: 14),
        const Text('İcazələr', style: T.h1),
        const SizedBox(height: 4),
        const Text(
          'Hər ikisi isteğe bağlıdır — rdd etsən də tətbiq işləməyə davam edir.',
          style: T.subtitle,
        ),
        const SizedBox(height: 18),
        _permCard(
          icon: Icons.location_on,
          title: 'Məkan',
          subtitle: 'Yaxınlıq üçün · "tətbiq istifadə olunarkən"\n'
              'Dəqiq koordinat yalnız sənə görünür, başqalarına ~məsafə.',
          granted: location,
          onAsk: _askLocation,
        ),
        _permCard(
          icon: Icons.notifications,
          title: 'Bildirişlər',
          subtitle: 'Match, mesaj, xatırlatma və rezerv yenilikləri (FCM).',
          granted: notifications,
          onAsk: _askNotifications,
        ),
        const SizedBox(height: 16),
        CommunityBanner(
          icon: Icons.emoji_events,
          title: 'Hazırsan!',
          subtitle: 'Qeydiyyatı tamamladıqda +${XpRules.registrationXp} XP '
              'qazanacaqsan. Sonra profilini 100% doldurub əlavə '
              '+${XpRules.profileCompleteXp} XP qazana bilərsən.',
        ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          child: FilledButton(onPressed: widget.onDone, child: const Text('Qoshul-a başla')),
        ),
      ],
    );
  }

  Widget _permCard({
    required IconData icon,
    required String title,
    required String subtitle,
    required bool granted,
    required VoidCallback onAsk,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Q.card,
        border: Border.all(color: granted ? Q.primary : Q.border,
            width: granted ? 1.4 : 1),
        borderRadius: BorderRadius.circular(Q.rLg),
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: granted ? Q.secondary : Q.muted,
              borderRadius: BorderRadius.circular(Q.rMd),
            ),
            child: Icon(icon, size: 20, color: granted ? Q.primary : Q.mutedFg),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 13,
                        fontWeight: FontWeight.w800)),
                const SizedBox(height: 4),
                Text(subtitle,
                    style: const TextStyle(
                        fontFamily: T.family,
                        fontSize: 10,
                        height: 1.6,
                        color: Q.mutedFg)),
              ],
            ),
          ),
          const SizedBox(width: 8),
          granted
              ? const Icon(Icons.check_circle, size: 22, color: Q.primary)
              : TextButton(onPressed: onAsk, child: const Text('İcazə ver')),
        ],
      ),
    );
  }
}
