# Backend (NestJS) — Mərhələ 2+

Mərhələ 1-də ancaq qovluq skeleti var (`.gitkeep`). Mərhələ 2-də skelet qurulur:

```
backend/src/
├── main.ts               # bootstrap + Swagger (OpenAPI) quraşdırması
├── app.module.ts
├── common/               # guards, interceptors, filters, pipes, DTO bazaları
├── config/               # typed env validasiyası (fail-fast)
├── database/             # Drizzle konfiqurasiyası + schema/ + migrations/
│                         #   (PostGIS extension custom migration — m2)
├── queue/                # BullMQ qeydiyyatı (m6+)
└── modules/<ad>/         # auth, profile, match, events, chat, notifications, assistant
    ├── dto/              # + input validasiya
    ├── <ad>.controller.ts
    ├── <ad>.service.ts
    └── <ad>.spec.ts      # Vitest (qayda 5)
```

Testlər: Vitest (Jest yerinə — monorepo-nun tək test runner-ı).
