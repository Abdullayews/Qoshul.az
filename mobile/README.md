# Qoshul.az — Mobil (Flutter)

Status: **m1 skelet + APK pipeline preview** (branch: `feature/apk-pipeline`).

## Nə var indi

- `lib/main.dart` — m1 skelet ekranı: APK pipeline-ın işlədiyini real ekranda
  göstərən brend placeholder. m7-də tam arxitektura ilə əvəz olunacaq.
- Android platform faylları (`android/`) repoda **yoxdur** — CI-də
  `flutter create` generasiya edir (`docs/decisions.md` D-017).
  m7-də kurasiyalı `android/` commit olunacaq (tətbiq adı, ikon, imza, flavor).

## APK haradan?

GitHub → **Actions** → son «APK» run-u → **Artifacts** → `qoshul-apk.zip`
(içində `app-release.apk`). İlk APK-lar **debug imza** ilədir — yalnız birbaşa
quraşdırma üçün; real keystore m7-də (D-016).

## Lokal run (m7-dən əvvəl, istəyə bağlı)

Flutter SDK qurulu olanda:

```bash
cd mobile
flutter create --platforms=android --org az.qoshul --project-name qoshul .
flutter pub get
flutter run
```

`flutter create` bizim `lib/main.dart` və ya `pubspec.yaml`-ı dəyişsə:
`git checkout -- .` ilə geri qaytar.

## Hədəf arxitektura (m7) — feature-first Clean Architecture

```
mobile/lib/
├── core/
│   ├── config/       # env, konfiq
│   ├── router/       # naviqasiya
│   ├── theme/        # dizayn sistemi
│   ├── storage/      # secure storage (token-lar)
│   └── network/      # OpenAPI-dən generasiya olunan Dart client (m7)
└── features/<ad>/    # auth, profile, match, chat, map, events, notifications, assistant
    ├── data/         # repository impl, DTO-lar
    ├── domain/       # entities, usecases, repository interfeysləri
    └── presentation/ # ekranlar + BLoC
```

Sinxronizasiya: backend `/openapi.json` → client generasiyası → `core/network/`
(əl ilə yazılan API client YOXDUR).
