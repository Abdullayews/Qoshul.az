<div align="center">

# 🤝 Qoshul.az

**The social platform of Azerbaijan — profiles, live chat, nearby matches, maps, events and an AI assistant.**

*“Qoşul” means “join.” Target region: Azerbaijan · Interface language: Azerbaijani.*

[![Flutter](https://img.shields.io/badge/Mobile-Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)](https://flutter.dev)
[![NestJS](https://img.shields.io/badge/Backend-NestJS-E0234E?style=for-the-badge&logo=nestjs&logoColor=white)](https://nestjs.com)
[![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL_17%20%2B%20PostGIS-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Redis](https://img.shields.io/badge/Cache%2FQueue-Redis%20%2B%20BullMQ-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io)
[![Docker](https://img.shields.io/badge/Infra-Docker%20arm64-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com)
[![Caddy](https://img.shields.io/badge/Proxy-Caddy-1F73C1?style=for-the-badge&logo=caddy&logoColor=white)](https://caddyserver.com)

</div>

---

## 🧭 What is this?

**Qoshul.az** is a full-stack social platform built for the Azerbaijani
market. On the surface: user profiles, live chat, a **nearby-people match
system** powered by PostGIS geography, an interactive map, real-time
notifications and events — with an AI assistant on call underneath.

Under the hood it's a carefully-argued architecture: a **Flutter mobile
app** in strict Clean Architecture (BLoC, feature-first), a **NestJS
modular monolith**, **PostgreSQL 17 + PostGIS 3** for data and geo
queries, **Socket.IO over a Redis adapter** for realtime, **BullMQ** for
jobs, and a full **Oracle Cloud Always Free** hosting topology — with
every technical decision written down and sourced in
[`docs/decisions.md`](docs/decisions.md).

## ✨ Product features

| | |
|---|---|
| 👤 **Profiles** | Accounts with verification, photos, interests |
| 📍 **Match & map** | Nearby-people matching on PostGIS `geometry(Point)` + interactive map |
| 💬 **Live chat** | Socket.IO + Redis adapter, read receipts |
| 🔔 **Real-time notifications** | FCM push via BullMQ queues |
| 📅 **Events** | Discover and join local events |
| 🏟️ **Venues & partners** | Browse places and partner spots |
| 🎟️ **Coupons & reservations** | Deals and bookings through partners |
| 🛡️ **Safety** | Reporting & blocking, image moderation *(planned m4+)* |
| 🤖 **AI assistant** | Per-user rate-limited AI proxy |
| 🌐 **AZ-first** | Interface fully in Azerbaijani |

## 🛠️ Tech stack — the immutable set

| Layer | Technology |
|---|---|
| Mobile | **Flutter** (Dart, Clean Architecture + BLoC, feature-first, freezed, json_serializable) |
| API contract | OpenAPI schema → generated Dart client |
| Backend | **NestJS** (TypeScript) — modular monolith (no microservices / gRPC) |
| Realtime | **Socket.IO + Redis adapter** |
| Database | **PostgreSQL 17 + PostGIS 3** (Drizzle ORM; coordinates as `geometry(Point)`, geo ops in raw SQL) |
| Cache / queue | **Redis + BullMQ** |
| Auth | Custom **JWT (access + refresh)**, Google/Apple ID token validation, email verification; optional SMS OTP with hard rate limits |
| Media | Oracle Cloud Object Storage (S3-compatible API) — public bucket, CDN planned later |
| Push | **FCM** |
| Proxy | **Caddy** (TLS/ACME) |
| CI/CD | **GitHub Actions** — lint, Vitest, build, multi-arch Docker (arm64 mandatory), APK |
| Monitoring | Prometheus + Grafana · Sentry (optional) |

## ☁️ Hosting topology — Oracle Cloud Always Free (Ampere A1)

```
             ┌──────────── Oracle A1 (ARM, 2 OCPU / 12 GB total) ────────────┐
             │ VM1 (1 OCPU / 6 GB)              VM2 (1 OCPU / 6 GB)         │
Flutter(APK)─▶│  Caddy :443 (TLS/ACME)           PostgreSQL 17 + PostGIS     │
             │   └─ NestJS :4000 (modules)       :5432 — VM1 only           │
             │   └─ Redis :6379 (cache/BullMQ)  (private VCN; NSG notes:    │
             │        private network ─────────▶   runbook.md §2)            │
             └────────────────────────────┬─────────────────────────────────┘
                                          │ daily pg_dump (via VM1)
                                          ▼
                              Oracle Object Storage (same tenancy)
                              · qoshul-media (public)  — media
                              · qoshul-backup (private) — backups
                              (S3 compat API · 20 GB · 50K req/mo · 10 TB egress)
```

Block volume: **200 GB total** = 2×47 GB boot + ~106 GB on VM2
(`/mnt/qoshul-pgdata`).

**External dependencies** (can't move into Oracle — see
`docs/decisions.md` D-020): GitHub (code+CI) · FCM · Google/Apple OAuth ·
AI provider · SMTP provider (OCI blocks port 25) · DNS.

## 📁 Repository structure

```
Qoshul.az/
├── README.md · Makefile · .env.example · .editorconfig
├── docs/            architecture.md · runbook.md · decisions.md
├── mobile/          Flutter — core/ + features/<name>/{data, domain, presentation}
│                    features: auth · profile · match · map · chats · feed · events
│                             venues · partners · coupons · reservations · safety
│                             notifications · onboarding · settings · assistant · shell
├── backend/         NestJS — common · config · database · queue · modules/
│                    modules: auth · profile · match · map · chat · events
│                            notifications · assistant
└── infra/
    ├── docker/      postgres/ (PG17+PostGIS, arm64) · backend.Dockerfile · caddy/
    ├── compose/     dev.yml · vm1.yml · vm2.yml (host-agnostic — moving to Hetzner = .env + DNS)
    ├── backup/      pg_dump → object storage
    ├── monitoring/  Prometheus / Grafana / Sentry
    └── ci/          GitHub Actions multi-arch
```

## 🚦 Phase status

| # | Phase | Status |
|---|---|---|
| 1 | Monorepo + Docker Compose (arm64) + .env.example + README | ✅ |
| 2 | Backend skeleton: NestJS, Drizzle, PostGIS migration, health, Swagger | ⬜ |
| 3 | Auth: registration, login, refresh, Google/Apple, email verification | ⬜ |
| 4 | Profile + Match (PostGIS proximity) + Events | ⬜ |
| 5 | Chat: Socket.IO + Redis adapter, read receipts | ⬜ |
| 6 | Notifications: FCM + BullMQ | ⬜ |
| 7 | Flutter: skeleton, OpenAPI client, auth + profile screens | ⬜ |
| 8 | Flutter: map, chat, notifications | ⬜ |
| 9 | AI assistant proxy (per-user limits) | ⬜ |
| 10 | Infra: Caddy prod, multi-arch CI, backups, monitoring | ⬜ |

## 🚀 Quickstart (local dev)

```bash
cp .env.example .env      # fill POSTGRES_PASSWORD — enough for local dev
make dev                  # brings up PG17+PostGIS (built) + Redis
# PG: 127.0.0.1:5432 · Redis: 127.0.0.1:6379
```

> Also works on ARM Macs — all images are verified `linux/arm64`
> (`docs/decisions.md`). The official `postgis/postgis` image is
> amd64-only, so this repo builds **its own** PostGIS image
> (`infra/docker/postgres/`) — reasoning in `docs/decisions.md` D-002.

## 📚 Documentation

- [`docs/architecture.md`](docs/architecture.md) — request flow, modules, queues, contracts
- [`docs/runbook.md`](docs/runbook.md) — provisioning, NSG, deploys, the **20% idle-reclaim rule**, backups, moving to Hetzner
- [`docs/decisions.md`](docs/decisions.md) — every technical claim with its source (rule 2 registry)

## 📏 Working rules (short version)

1. Secrets only in `.env` (chmod 600) and CI secrets — never in code.
2. Every technical claim is sourced in `docs/decisions.md`; unsourced = “unproven”.
3. Every endpoint: DTO + input validation + rate limit. Every module: Vitest.
4. Image moderation + report/block flows are designed in from m4+.

## 📄 License

Personal startup project — built in the open, one phase at a time.
