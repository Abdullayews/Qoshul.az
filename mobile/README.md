# Qoshul.az — Mobil (Flutter)

Status: **UI mərhələsi (D-023)** — tam arayüz, mock data, backend gözləyir.
Branch: `feature/apk-pipeline` · Versiya: `0.2.0+2`

## Nə var indi (v0.2.0)

Web tətbiqi ilə (Amin099015/qoshul) **birebir eyni stildə** tam arayüz:

| Ekran | Məzmun |
|---|---|
| Auth | Daxil ol / Qeydiyyat / Şifrəni unut (mock) + Google düyməsi |
| Onboarding | 8 addım: əsas məlumat, təhsil/iş, dillər, maraqlar, məqsədlər, şəxsiyyət testi, açıq suallar, üstünlüklər → Big Five nəticəsi |
| Ana səhifə | Salamlaşma + XP/rank, hero şəkli, hekayələr, "Sənin üçün" AI tövsiyələri, cəmiyyət banneri, axtarış + kateqoriya çipləri + tədbir kartları |
| Kəşf et | Yaxınlıqda (xəritə + siyahı) / Populyar / İnsanlar (AI uyğunluq kartları, dostluq sorğuları) |
| Söhbətlər | Tədbir qrup söhbətləri + dost söhbətləri, tam çat görünüşü (mock cavablarla) |
| Köməkçi | AI söhbət (yazı axını simulyasiyası), [TƏDBİR] təklif kartı → "Bu tədbiri yarat" |
| Profil | Avatar, maraqlar, şəxsiyyət nəticəsi, statistika, XP, 3 tab, ad redaktəsi |
| Tərəfdaşlar | Məkan kartları + endirimlər + "Burada tədbir yarat" |
| Ayarlar | Hesab, bildirişlər, təhlükəsizlik, haqqında, çıxış |
| Tədbir detalları | Bottom sheet: şəkil, faktlar, qoşul/çıx, rəy forması (keçmiş üçün) |
| Tədbir yaratma | Tam forma: AI doldurma (mock), kateqoriya, format, şəhər, məkan, tarix/saat, ödəniş, teqlər, təsdiq rejimi |

Dizayn sistemi (`lib/core/theme/app_theme.dart`): web `styles.css` tokenlərinin
OKLCH→sRGB çevirməsi ilə birebir portu — `#00A63E` yaşıl, `#FE9A00` narıncı,
Plus Jakarta Sans (Azərbaycan qlifləri ilə, 400–800 çəkilər), kiçik radiuslar (4–8px).

## Struktur

```
mobile/lib/
├── main.dart                 # Auth → Onboarding → Shell axını
├── core/
│   ├── theme/app_theme.dart  # Q (rənglər) + T (tipografiya) + ThemeData
│   ├── models/models.dart    # QoshulEvent, Person, Partner, XpRules, [TƏDBİR] parser
│   ├── data/mock_data.dart   # 13 tədbir, 5 insan, 4 məkan, 12 kateqoriya qrupu, 25 şəhər
│   ├── state/app_state.dart  # ChangeNotifier (mock) — m8-də BLoC-a parçalanacaq
│   └── widgets/              # brand (wordmark), ui (banner, çip, XP, AI-box, bubble...)
└── features/
    ├── shell/                # header + bottom nav (4 tab + Yarat)
    ├── feed/ explore/ chats/ assistant/ profile/ partners/
    ├── auth/ onboarding/ settings/
    └── events/               # EventCard + detail sheet + create sheet + city picker
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
