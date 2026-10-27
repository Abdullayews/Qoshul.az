# Arxitektura

## Baxış

Qoshul.az — modul monolit NestJS backend + Flutter mobil tətbiq. Realtime: Socket.IO + Redis adapter. Baza: PostgreSQL 17 + PostGIS 3. Keş və növbələr: Redis + BullMQ. Media və backup: Oracle Object Storage (S3 Compatibility API) — R2 YOXDUR (D-018). Push: FCM. Reverse proxy: Caddy (avtomatik TLS). Mikroservis və gRPC YOXDUR.

## İstək axını

```
Flutter (APK)
   │  HTTPS (REST: /api/*) · WSS + polling (/socket.io/*)
   ▼
Caddy (VM1 · :80/:443 · ACME TLS · WebSocket avtomatik)
   ▼
NestJS (VM1 · :4000) — modul monolit
   │   modullar: auth · profile · match · events · chat · notifications · assistant
   ├──▶ PostgreSQL 17 + PostGIS 3 (VM2 · private :5432)
   │      Drizzle ORM (PG dialekti) + xam geo SQL
   ├──▶ Redis 7 (VM1 · :6379)
   │      keş · BullMQ növbələri · Socket.IO redis adapter
   ├──▶ Oracle Object Storage (eyni tenancy — S3 compat API)
   │      media: qoshul-media (publik) — birbaşa URL, backup: qoshul-backup (private)
   ├──▶ FCM — push (BullMQ worker-dən)
   └──▶ AI provayder — YALNIZ server tərəfli proxy (açar serverdə)
```

## Backend modulları

| Modul | Mərhələ | Öhdəliklər |
|---|---|---|
| auth | m3 | qeydiyyat, giriş, refresh token, Google/Apple ID token doğrulaması, email təsdiqi, (opsional) SMS OTP — sərt rate limit |
| profile | m4 | profil CRUD, şəkil yükləmə (OCI OSS presigned URL), moderasiya hook |
| match | m4 | yaxınlıq sorğusu — PostGIS `ST_DWithin` + spatial index (xam SQL) |
| events | m4 | tədbir CRUD, iştirak, rezervasiya, tərəfdaşlar |
| chat | m5 | Socket.IO + redis adapter, mesaj saxlama, oxunma statusu |
| notifications | m6 | FCM push, BullMQ vasitəsilə, preference-lar |
| assistant | m9 | AI proxy — istifadəçi başına limit, açar yalnız serverdə |

Hər modulda: `dto/` · `*.controller.ts` · `*.service.ts` · `*.spec.ts` (Vitest). Input validasiya + rate limit + guard-lar `common/`-də mərkəzləşir.

## Növbə işləri (BullMQ)

| Növbə | Məqsəd | Səbəb |
|---|---|---|
| media-processing | şəkil emalı / moderasiya hazırlığı | VM1-də cəmi 1 OCPU — CPU-yönümlü iş sinxron yox, növbədə |
| push-delivery | FCM göndərişləri | uğursuzluqda retry, backoff |
| ai-request | AI çağırışları | istifadəçi başına limit + rate smoothing |
| nightly-maintenance | köhnə token/OTP təmizliyi, statistika | gecəlik real yük (runbook §5 — idle-reclaim qorunması) |

## Realtime (m5)

`@nestjs/websockets` + `@socket.io/redis-adapter`. Tək NestJS instansı olsa da adapter standart qurulur (gələcəkdə 2-ci instans problemi olmur). JWT handshake-də doğrulanır. Otaqlar: event-otaqları + privat çatlar.

## API kontraktı

NestJS Swagger → `/openapi.json` → Dart client generasiyası (m7) → `mobile/lib/core/network/`. Kontrakt dəyişəndə client **yenidən generasiya olunur** — əl ilə yazılan API client YOXDUR. Bu, mobil/web versiya uyğunsuzluğunu aradan qaldırır.

## Data qatı

Drizzle ORM (PostgreSQL dialekti) + drizzle-kit migrations (`backend/src/database/`). PostGIS extension custom migration ilə (m2). Koordinatlar: `geometry(Point, 4326)`; yaxınlıq: `ST_DWithin` + GIST index — xam SQL (Drizzle-dən kənar).

Keçmiş layihədəki (React/Supabase) 11 cədvəlli sxem əsas götürülür — profiles, events, event_participants, saved_events, reviews, friendships, messages, match_cache, user_roles, partners, reservations — PG tipinə geri konvertasiya ilə (m2). 10 triqqerin server funksiyaları artıq mövcuddur (əvvəlki sessiyada yazılıb) — NestJS service-lərinə köçürülür.

## Media (Oracle-native — D-018/D-019)

Flutter → (auth) backend → OSS **presigned PUT URL** (S3 compat) → birbaşa yükləmə OSS-yə → oxunuş **publik bucket URL** ilə (`objectstorage.<region>.oraclecloud.com/n/<ns>/b/qoshul-media/o/<açar>`). CDN yoxdur (beta): tətbiqdə Glide disk cache təkrar sorğuları azaldır; 50K API request/ay büdcəsi Prometheus-də izlənir (m10). Aşım ucuzdur (overage qiyməti — təsdiqlənməyib). m10 opsiyası: VM1 Caddy cache plugin (xcaddy) — OSS custom domain rəsmi dəstəyi yoxdur (təsdiqlənməyib). Object açarları UUID — listing məxfiliyi üçün unguessable. Report/block + şəkil moderasiyası növbəyə düşür (qayda 6).

Yaddaş büdcəsi (Always Free 20GB cəmi): media ~15GB · backup ~3GB · ehtiyat ~2GB.

## Portativlik

Bütün host faktları (IP, domen, URL) `.env`-də. Compose faylları host-agnostik → Hetznerə köçüş = `.env` + DNS (runbook §7).
