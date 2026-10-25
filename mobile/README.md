# Mobile (Flutter) — Mərhələ 7/8

Mərhələ 1-də ancaq qovluq skeleti var (`.gitkeep`). Tətbiq mərhələ 7-də `flutter create` ilə yaradılır — mövcud qovluqlar qorunur.

Struktur — feature-first Clean Architecture:

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

Sinhronizasiya: backend `/openapi.json` → client generasiyası → `core/network/` (əl ilə yazılan API client YOXDUR).
