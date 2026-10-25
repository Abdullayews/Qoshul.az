# Texniki qərarlar registeri (qayda 2)

Hər texniki iddia burada mənbə ilə qeyd olunur. Yoxlanmayanlar **təsdiqlənməyib** statusu daşıyır və bağlı mərhələdə yoxlanılır. Yoxlama tarixi: 2026-10-25.

## Təsdiqlənənlər

| ID | İddia / qərar | Status | Mənbə |
|---|---|---|---|
| D-001 | `postgres:17` və `postgres:17-alpine` — `linux/arm64` mövcuddur | ✅ yoxlanılıb | Docker Hub API `hub.docker.com/v2/repositories/library/postgres/tags/17` (2026-10-25) |
| D-002 | `postgis/postgis` rəsmi imajı **yalnız amd64**-dir: `17-3.5`, `17-3.5-alpine`, `18-3.6`, `18-3.6-alpine` — 4/4 tag-də arm64 YOXDUR → öz imajımız (`infra/docker/postgres/`): `postgres:17` + PGDG paketi | ✅ yoxlanılıb (4 tag) | Docker Hub API, hər tag birbaşa lookup (2026-10-25) |
| D-003 | PGDG-də `postgresql-17-postgis-3` paketi `arm64` üçün mövcuddur (bookworm + trixie) | ✅ yoxlanılıb | `apt.postgresql.org` `dists/{bookworm,trixie}-pgdg/main/binary-arm64/Packages.gz` (2026-10-25) |
| D-004 | `redis:7-alpine` — `linux/arm64` mövcuddur (son push 2026-10-22, yenilənir) | ✅ yoxlanılıb | Docker Hub API (2026-10-25) |
| D-005 | `caddy:2-alpine` — `linux/arm64` mövcuddur (son push 2026-10-22) | ✅ yoxlanılıb | Docker Hub API (2026-10-25) |
| D-006 | `node:24-alpine` — `linux/arm64` mövcuddur (son push 2026-10-22) | ✅ yoxlanılıb | Docker Hub API (2026-10-25) |
| D-007 | Node 24: LTS 2025-10-28, Maintenance 2026-10-20-dən, EOL 2028-04-30. Node 26: LTS 2026-10-28-dən başlayır (m2-də yenidən baxıla bilər) | ✅ yoxlanılıb | `raw.githubusercontent.com/nodejs/Release/main/schedule.json` |
| D-008 | Redis 8 seçilmədi: `8-alpine` arm64 mövcuddur, amma BullMQ-Redis 8 uyğunluğu hələ yoxlanmayıb; 7-alpine aktiv şəkildə yenilənir → mərhələli keçid m6-da mümkün | ✅ (fakt hissəsi) / ⬜ (BullMQ hissəsi — m6) | Docker Hub API + m6-da bullmq.io |

## Qərarlar (mühəndislik seçimi)

| ID | Qərar | Əsas |
|---|---|---|
| D-009 | PG 18 yerinə **PG 17** | 2024-cü ildən stable, 2029-a dəstək, extension ekosistemi daha çox sınaqdan keçib; keçmiş layihə sxemi PG mənşəlidir |
| D-010 | VM2: `shared_buffers=1GB` (6 GB RAM) — sabit rezident → idle-reclaim 20% qaydasına deterministik qoruma | runbook §5 |
| D-011 | Oracle idle-reclaim ölçmə semantikası (page cache sayılır mı və s.) | **təsdiqlənməyib** — rəsmi sənədlərdə qeyri-müəyyən; əvvəlki sessiyada müşahidə: stop-not-delete, email əvvəlcə |
| D-012 | Redis `maxmemory 768mb` + container limit 1 GB + `noeviction` (VM1) | noeviction BullMQ üçün tövsiyə olunur — **təsdiqlənməyib**, m6-da bullmq.io sənədi ilə yoxlanacaq |
| D-013 | Compose-da `deploy.resources.limits.memory` istifadəsi | Compose spesifikasiyası; VM-də runtime davranışı m10 monitorinqi ilə təsdiqlənəcək |
| D-014 | İmaj tag siyasəti: major/minor-bounded rolling (`2-alpine`, `7-alpine`, `24-alpine`) — patch-lar avtomatik, major sabit | yenilənə bilən + reproducible balansı |

## Açıq maddələr (növbəti mərhələlərdə)

| Mövzu | Nə vaxt | Mənbə |
|---|---|---|
| MAU rəqəmi (bağlantı hovuzu + yaddaş tuning üçün) | istifadəçidən gözlənilir | — |
| NestJS cari versiyası + Node 24/26 uyğunluğu | m2 | docs.nestjs.com |
| Drizzle ORM PG dialekti + PostGIS `geometry` dəstəyi | m2 | orm.drizzle.team |
| BullMQ ↔ Redis 7/8 rəsmi dəstək matrisi | m6 | docs.bullmq.io |
| `Caddyfile` runtime validate (`caddy validate`) — sandbox-da docker yoxdur | m2 (VM/lokal) | caddyserver.com |
| Socket.IO redis adapter versiya uyğunluğu | m5 | socket.io docs |
| FCM (v1 HTTP API) + service account axını | m6 | firebase.google.com |
| Oracle A1 idle-reclaim dəqiq metrikası | m10 (monitorinq qurulanda müşahidə) | Oracle Console |
