# Qoshul.az

Sosial platforma — istifadəçi profilləri, canlı çat, yaxınlıq (match) sistemi, xəritə, real-vaxt bildirişlər, tədbirlər, AI köməkçi. Hədəf region: Azərbaycan · İnterfeys dili: Azərbaycan dili.

## Stack (dəyişməz)

| Qat | Texnologiya |
|---|---|
| Mobil | Flutter (Dart, Clean Architecture + BLoC, feature-first, freezed, json_serializable) |
| API kontraktı | OpenAPI sxemi → Dart client generasiyası |
| Backend | NestJS (TypeScript) — modul monolit (mikroservis/gRPC yoxdur) |
| Realtime | Socket.IO + Redis adapter |
| Baza | PostgreSQL 17 + PostGIS 3 (Drizzle ORM; koordinatlar `geometry(Point)`, geo əməliyyatlar xam SQL) |
| Keş/növbə | Redis + BullMQ |
| Auth | Custom JWT (access + refresh), Google/Apple ID token doğrulaması, email təsdiqi; SMS OTP opsional (sərt rate limit) |
| Media | Oracle Cloud Object Storage (S3 Compatibility API) — publik bucket · CDN yox (beta; m10 opsiyası) |
| Push | FCM |
| Proxy | Caddy |
| CI/CD | GitHub Actions — lint, Vitest, build, multi-arch Docker (arm64 məcburi), APK |
| Monitorinq | Prometheus + Grafana (VM1-də, m10) · Sentry (opsional, xarici) |

## Hostinq topologiyası — Oracle Cloud Always Free (Ampere A1)

```
                 ┌──────────── Oracle A1 (ARM, 2 OCPU / 12 GB cəmi) ────────────┐
                 │ VM1 (1 OCPU / 6 GB)              VM2 (1 OCPU / 6 GB)         │
Flutter (APK) ──▶│  Caddy :443 (TLS/ACME)           PostgreSQL 17 + PostGIS     │
                 │   └─ NestJS :4000 (modullar)       :5432 — YALNIZ VM1-dən    │
                 │   └─ Redis :6379 (keş/BullMQ)    (private VCN; NSG izahı:   │
                 │        private şəbəkə ──────────▶  runbook.md §2)            │
                 └────────────────────────────┬─────────────────────────────────┘
                                              │ gündəlik pg_dump (VM1 vasitəsilə)
                                              ▼
                                   Oracle Object Storage (eyni tenancy)
                                   · qoshul-media (publik) — media
                                   · qoshul-backup (private) — backup
                                   (S3 compat API · 20GB · 50K req/ay · 10TB egress)
```

Block volume: 200 GB cəmi = 2×47 GB boot + ~106 GB VM2-yə (`/mnt/qoshul-pgdata`).

**Xarici asılılıqlar** (Oracle-a köçürülməsi mümkün deyil — decisions.md D-020):
GitHub (kod+CI) · FCM (Android push) · Google/Apple OAuth · AI provayder · SMTP provayder (OCI port 25 bloklu) · DNS (pulsuz registrar/Cloudflare).

## Struktur

```
qoshul-v2/
├── README.md · Makefile · .env.example · .gitignore · .editorconfig
├── docs/            architecture.md · runbook.md · decisions.md
├── mobile/          Flutter (m7+) — core/ + features/<ad>/{data,domain,presentation}
├── backend/         NestJS (m2+) — common · config · database · queue · modules/
└── infra/
    ├── docker/      postgres/ (PG17+PostGIS, arm64) · backend.Dockerfile (m2) · caddy/
    ├── compose/     dev.yml · vm1.yml · vm2.yml (host-agnostik — Hetznerə köç = .env + DNS)
    ├── backup/      pg_dump → R2 (m10)
    ├── monitoring/  Prometheus/Grafana/Sentry (m10)
    └── ci/          GitHub Actions multi-arch (m10)
```

## Mərhələ statusu

| # | Mərhələ | Status |
|---|---|---|
| 1 | Monorepo + Docker Compose (arm64) + .env.example + README | ✅ |
| 2 | Backend skeleti: NestJS, Drizzle, PostGIS migration, health, Swagger | ⬜ |
| 3 | Auth: qeydiyyat, giriş, refresh, Google/Apple, email təsdiqi | ⬜ |
| 4 | Profil + Match (PostGIS yaxınlıq) + Events | ⬜ |
| 5 | Chat: Socket.IO + Redis adapter, oxunma statusu | ⬜ |
| 6 | Bildirişlər: FCM + BullMQ | ⬜ |
| 7 | Flutter: skelet, OpenAPI client, auth + profil ekranları | ⬜ |
| 8 | Flutter: xəritə, çat, bildirişlər | ⬜ |
| 9 | AI köməkçi proxy (istifadəçi başına limit) | ⬜ |
| 10 | Infra: Caddy prod, CI multi-arch, pg_dump→R2, monitoring | ⬜ |

## Quickstart (lokal dev)

```bash
cp .env.example .env      # POSTGRES_PASSWORD doldur (lokal üçün kifayətdir)
make dev                  # PG17+PostGIS (build) + Redis qalxır
# PG: 127.0.0.1:5432 · Redis: 127.0.0.1:6379
```

> ARM maclarda da işləyir (bütün imajlar linux/arm64 yoxlanılıb — `docs/decisions.md`).
> **Diqqət:** rəsmi `postgis/postgis` imajı amd64-only olduğu üçün öz imajımızı
> qururuq (`infra/docker/postgres/`) — səbəb: `docs/decisions.md D-002`.

## Sənədlər

- `docs/architecture.md` — istək axını, modullar, növbələr, kontrakt
- `docs/runbook.md` — provisioning, NSG, deploy, **idle-reclaim 20% qaydası**, backup, Hetznerə köç
- `docs/decisions.md` — bütün texniki iddiaların mənbələri (qayda 2 registri)

## İş qaydaları (qısa)

1. Sirllər yalnız `.env` (chmod 600) və CI secrets — heç vaxt kodda.
2. Hər texniki iddia `docs/decisions.md`-də mənbə ilə; yoxlanmayan "təsdiqlənməyib".
3. Hər endpoint: DTO + input validasiya + rate limit. Hər modul: Vitest.
4. Şəkil moderasiyası + report/block mexanizmi nəzərə alınır (m4+).
