# Qoshul.az — Mobil (Flutter)

Status: **UI mərhələsi (D-023/D-025)** — tam arayüz + dizayn qatı, mock data, backend gözləyir.
Branch: `main` · Versiya: `0.5.0+5`

## Nə var indi (v0.5.0 — İkitərəfli XP iqtisadiyyatı)

D-026 (product-spec L bölməsi) — tam UX dəsti, mock data ilə:

- **İki XP**: Peşəkarlıq XP (səviyyə, xərclənmir; Kəşf peşəkar sıralaması) +
  Endirim XP (obyektə-məxsus ayrı baza: Xankəndi XP-si yalnız Xankəndidə işləyir)
- **XP Kartı** (Profil → XP kartım): obyekt balansları + 60 saniyəlik tək-istifadəlik
  dinamik QR + 6 rəqəmli fallback kod + xp_config tənzimləmələri + ledger tarixçəsi
- **Biznes rejimi** (Tərəfdaşlar → Biznes rejimi): işçi skanı → müştəri balansı →
  hesab məbləği → canlı endirim hesablanması (məzənnə/maks %/min hesab) → receipt
- **Təşkilatçı bitirmə axını**: obyektin statik QR-ı → şəkil paylaş (yalnız təşkilatçı,
  biznes hesabına düşür) → iştirakçı taq (+50 təşkilatçı, +25 hər taq)
- **Kəşf**: "Sənin səviyyənə görə" bölməsi — peşəkar aktiviteler önə düşür (L.1)
- **Xəritə görünüşü**: 3 seçim — Hamı / Yalnız dostlar / Gizli (D-027)
- **Kuponlar**: XP ilə alış obyektin öz Endirim XP bazasından (L.6)
- XP müddəti yoxdur; balans = qazanma − xərcləmə (ledger)

Əvvəlki qatlar: v0.2.0 web-parity UI · v0.3.0 ROL strukturu · v0.4.0 Liquid Glass
minimal + bütün bölmələrə hərəkət dili.



D-025 (docs/design-spec.md): Apple iOS 26 "Liquid Glass" dilindən **minimal**
interpretasiya + bütün bölmələrə vahid hərəkət dili:

- **Şüşə — cəmi 5 səth** (yalnız məzmunun real axdığı yerlərdə):
  üzən alt nav pill (`extendBody`), xəritə filtr paneli, xəritə alt kartı,
  sayğac həbi, match fəaliyyət həbləri — kartlar/siyahılar web-parity düz qalır
- **Hərəkət tokenləri:** d1-d4 (150-550ms), Apple-yumşaq əyrilər
  (easeOutCubic/easeOutBack/elasticOut), 45ms stagger, `disableAnimations` hörməti
- **Hər yerdə animasiya:** staggered siyahı girişləri, kart toxunuş miqyası,
  tab fade-through, seçilmiş ikon bounce, XP zolağının animasiyalı dolğusu,
  rəqəm sayğacları (XP/stat), nişan pop-ları, pin drop, xəritə kartı slide-up,
  **real match swipe dəsti** (drag→fırlanma→uçuş/geri qayıdış + MARAQLIYAM/KEÇ
  möhürləri), çat "yazır..." nöqtə dalğası, QR pop, onboarding nöqtə morph
- **Sıfır xarici asılılıq qorunur** — glass və motion tam Flutter SDK ilə
  (BackdropFilter + qradiyent + kənar; AnimationController primitivləri)

## Nə var idi (v0.3.0 — ROL UI)

ROL (docs/product-spec.md) strukturunun tam tətbiqi — web vizual stili qorunaraq (D-023):

- **5 tab:** Kəşf · Xəritə · Tədbirlər · Söhbətlər · Profil (+ mərkəzdə Yarat)
- **Onboarding (ROL B):** 3 slayd → profil (18+ yoxlaması, şəkil 1-6, bio 150) →
  9 maraq kateqoriyası (min 3, max 8) → icazələr (məkan/bildiriş, rdd mümkün) → +50 XP
- **Auth (ROL B):** Google / Apple / email + 6 rəqəmli təsdiq kodu (mock)
- **Xəritə (ROL TAB 2):** pin-lər (məkan/tədbir/insan), 4 filtr (kateqoriya,
  1-3-5-10 km, indi açıqdır, endirimi var), alt kart → Rezerv et / Ətraflı
- **Match (ROL D):** kart dəsti (şəkil, yaş, ~məsafə, ortaq maraqlar),
  Maraqlıyam/Keç → match → chat + +10 XP (gündə max 3), report/blok
- **Rezerv (ROL E):** tarix → slot → nəfər → qeyd → kupon → status
  (GÖZLƏYİR/TƏSDİQLƏNDİ) → QR; check-in → İSTİFADƏ OLUNDU + 30 XP;
  ləğv; no-show 3× → 7 gün məhdudiyyət
- **Kuponlar (ROL G):** səviyyə-avto endirimlər + XP ilə al (balans ≠ ümumi XP),
  unikal kod + QR + müddət; vaxtı bitən kupon XP qaytarılmır
- **XP (ROL F):** 11 hadisə ROL cədvəli ilə (idempotent, gündəlik/həftəlik limitlər),
  6 səviyyə (Yeni→Əfsanə), 5 nişan
- **Bildirişlər (ROL H):** 11 tip, hər tip ayrıca söndürülür, marketinq 23:00-08:00 sükut
- **Moderasiya (ROL I):** report (səbərlə), blok (qarşılıqlı görünməzlik),
  3+ report → məhdudlaşdırma; ilk görüş məsləhətləri ekranı
- **Çat (ROL TAB 4):** Şəxsi/Qruplar, oxunma statusu (çatdırıldı/oxundu),
  yazır..., report/blok/sil

Bütün məntiq mock-data ilə klient tərəfində simulyasiya olunur (D-024:
backend placeholder). Ödəniş yoxdur.

## Struktur

```
mobile/lib/
├── main.dart                 # Auth → Onboarding → Shell axını
├── core/
│   ├── theme/app_theme.dart  # Q (rənglər) + T (tipografiya) + ThemeData
│   ├── models/               # models (event/person/partner/XP) + reservation/coupon/notification/moderation
│   ├── data/mock_data.dart   # 13 tədbir, 5 insan, 4 məkan, 12 kateqoriya qrupu, 25 şəhər
│   ├── state/app_state.dart  # ChangeNotifier (mock) — m8-də BLoC-a parçalanacaq
│   └── widgets/              # brand, ui, rol (ROL hissələri),
│                             # glass (D-025 şüşə səthlər), motion (D-025 hərəkət dili)
└── features/                 # 17 qovluq: shell, feed, map, match, events, venues,
                              # reservations, coupons, chats, notifications, profile,
                              # settings, safety, onboarding, auth, partners, assistant
```

**Sıfır xarici asılılıq** — yalnız Flutter SDK (D-023). Backend inteqrasiyasında
http/bloc/paketlər əlavə olunacaq.

## Testlər

```bash
cd mobile && flutter test    # 20 test: XpRules, parser, AppState məntiqi, hər ekranın widget testi
```

## APK haradan?

GitHub → **Actions** → son «APK» run-u → **Artifacts** → `qoshul-apk.zip`
(içində `app-release.apk`). APK-lar **debug imza** ilədir — yalnız birbaşa
quraşdırma üçün (4 tester); real keystore + Play Store D-016 üzrə sonrakı mərhələdə.

## Lokal run

```bash
cd mobile
flutter create --platforms=android --org az.qoshul --project-name qoshul .
git checkout -- .   # create-in toxunduğu faylları geri qaytar
flutter pub get
flutter run         # və ya: flutter test
```

## UI mərhələsinin məhdudiyyətləri (bilinən placeholderlər)

- Xəritə — stilləşdirilmiş CustomPaint (OSM plitələri deyil); real xəritə backend fazasında
- AI cavabları — lokal mock generator; real LLM backend fazasında
- Bütün data yaddaşda (restart-da sıfırlanır); davamlılıq backend fazasında
- Onlayn ödəniş UI elementləri (qiymət sahəsi) var, ödəniş inteqrasiyası yoxdur (sıradan sonra)
