# Qoshul.az — Supabase backend (M1)

> **Status:** M1 sxem + server qaydaları hazırdır (D-076/D-077).
> Tətbiq hələ **MOCK rejimində** işləyir (D-050 ikili rejim) — Supabase
> layihəsi qurulanda `BACKEND_MODE=SUPABASE` ilə keçilir.
>
> **İstifadəçidən gözlənilən dəyərlər (PLACEHOLDER):** aşağıda 🔶 ilə
> işarələnib — tətbiq tərəfində hələ `PLACEHOLDER_…` makrosları durur
> (D-079). Real dəyərlər gələndə yalnız `android-app/local.properties`
> dəyişdirilir, kod YENİDƏN YAZILMIR.

## 1. Fayllar

| Fayl | Məzmun |
|---|---|
| `migrations/0001_schema.sql` | 30 cədvəl + FK + indekslər (PostGIS GiST) + RLS siyasətləri + ULID |
| `migrations/0002_server_rules.sql` | SECURITY DEFINER funksiyalar — XP/kupon/rezerv qaydaları (qayda 8/9) |
| `migrations/0003_seed.sql` | Səviyyələr, 12 kateqoriya qrupu, 6 obyekt (v1..v6), 5 təklif (o1..o5) |

## 2. Quraşdırma (Supabase layihəsi yoxdursa)

1. 🔶 **[İSTİFADƏÇİ]** https://supabase.com → New project (region: Frankfurt eu-central-1)
2. SQL Editor → `migrations/0001_schema.sql` → Run *(PostGIS avtomatik aktivləşir)*
3. SQL Editor → `migrations/0002_server_rules.sql` → Run
4. SQL Editor → `migrations/0003_seed.sql` → Run
5. Project Settings → API:
   - 🔶 `Project URL` → `SUPABASE_URL`
   - 🔶 `anon public` açarı → `SUPABASE_ANON_KEY`
   - 🔶 `service_role` açarı → **yalnız M4 mikroservisində** istifadə olunacaq (tətbiqə YAZILMIR)
6. `android-app/local.properties`:
   ```properties
   SUPABASE_URL=https://xxxx.supabase.co
   SUPABASE_ANON_KEY=eyJ...
   BACKEND_MODE=SUPABASE
   ```
   *(boş/olmayan dəyərlər = tətbiq avtomatik MOCK rejimində qalır — crash YOX)*

## 3. Cədvəl xəritəsi (spec J ↔ M1)

| Spec J | M1 | Qeyd |
|---|---|---|
| `users` | `profiles` (+`profile_cards` görünüşü) | koordinat/restriction kartda YOX (ROL I) |
| `user_interests` | `user_interests` | limitsiz (D-074) |
| `categories` | `categories` | 12 qrup seed |
| `venues` | `venues` (+`venue_slots`) | + `xp_config` sütunları (L.3) |
| `reservations` | `reservations` (+`share_claims`) | N8 paylaşma tokeni |
| `events` | `events` (+`event_participants`) | N1: ULID |
| `matches` / `swipes` | `matches` / `swipes` | a<b kanonik; XP cap gündə 3 |
| `xp_events` | `xp_events` | immutable + idempotensiya |
| `xp_balances` | `venue_xp_accounts` + `venue_xp_ledger` | L.2/L.4 |
| `levels` | `levels` | seed (XpRules pariteti) |
| `coupons_offers` | `coupons_offers` | |
| `user_coupons` | `user_coupons` | 14 gün, təkistifadə |
| `reviews` | `reviews` | B4: PENDING → APPROVED → XP |
| `reports` | `reports` (+`admin_actions`) | N9 audit |
| `blocks` | `blocks` | |
| `notifications` | `notifications` (+`notification_settings`, `push_tokens`) | M4 FCM üçün hazır |

## 4. Server qaydaları (yalnız funksiyalar — RLS + revoke)

| Funksiya | Qayda (paritet: AppEngine.kt M0) |
|---|---|
| `fn_daily_login` | D-073: 1-2 gün dondurulma, 3+ sıfırlanma; +5; 7-də +30 |
| `fn_award_xp` | idempotent (`xp_events.unique(profile_id,key)`) |
| `fn_record_swipe` | D-074: hərəkət limitsiz; XP gündə max 3; qarşılıqlı → `matches` |
| `fn_preview_spend`/`fn_spend_venue_xp` | A1 typed xətalar; B6 mənfi qadağan; atomik UPDATE |
| `fn_buy_coupon` | D-072: balansda var olandan; REDD (CLAMP yox) |
| `fn_issue_xp_card_nonce`/`fn_consume_xp_card_nonce` | B5: CSPRNG (`gen_random_bytes`); 60 s TTL; təkistifadə |
| `fn_check_in_event` | B1: `ST_DWithin(200m)`; hosting ≥3 iştirakçı; B3 dəvət XP ilk check-in-də |
| `fn_create_event`/`fn_join_event`/`fn_approve_participant` | N1 ULID; approval axını; FULL/PAST yoxlamaları |
| `fn_create_reservation`/`fn_confirm_reservation`/`fn_use_reservation`/`fn_mark_no_show` | ROL E 6-status; QR server-generated; no-show ×3 → 7 gün restrict |
| `fn_claim_share` | N8: "Qoşul" iddiası |
| `fn_add_review`/`fn_moderate_review` | B4: moderasiyadan sonra XP; həftədə max 3 |
| `fn_report` | 3+ müxtəlif reporter → avto-restrict (N9) |
| `fn_send_message` | N6: saatlıq 20; 60 s dublikat spam filtri; blok cütləri |
| `fn_match_candidates` | D-070: `ST_DWithin` + GiST; görünüş/blok/restricted filtri (B7) |

## 5. Realtime (M1 → M4-də klient kodu)

Cədvəllər hazırdır (`notifications`, `messages`, `reservations` + `push_tokens`).
Klient tərəfdə abunə kodu (`realtime-kt`) **istifadə kodu gələndə** əlavə
olunacaq (M4 push fazası); Supabase Dashboard → Database → Replication →
həmin 3 cədvəl üçün "Send to realtime" aktivləşdirilir.

## 6. Storage (M1 — şəkil moderasiyası infra)

Bucket: `event-photos` (public read, write yalnız auth). Dashboard → Storage →
New bucket. Moderasiya işi (şəkli gizlətmə) M4 mikroservisində gəlir;
`storage-kt` 3.9.0-da JVM artifaktı YOXDUR (repo1 404) — app tərəfli yükləmə
inteqrasiyası M4-də qiymətləndiriləcək.

## 7. M4 mikroservisi (`service/`)

| Env | Default (PLACEHOLDER) | Nə vaxt lazımdır |
|---|---|---|
| `SUPABASE_URL` | `PLACEHOLDER` | deployda |
| `SUPABASE_SERVICE_ROLE_KEY` | `PLACEHOLDER` | deployda — 🔶 YALNIZ burada; tətbiqə YAZILMIR |
| `FCM_SERVICE_ACCOUNT_JSON` | `PLACEHOLDER` | push aktivləşəndə (service-account JSON bütöv) |
| `ADMIN_TOKEN` | `PLACEHOLDER` | admin API istifadəsi üçün (X-Admin-Token başlığı) |
| `PORT` | `8080` | — |
| `JOBS_INTERVAL_SECS` | `60` | — |

```bash
cd service && cargo run --release
curl http://localhost:8080/health
```

Joblar: `POST /jobs/reminders/run` (24s/2s — DB sütunları ilə idempotent),
`POST /jobs/moderation/run` (stale report-lar DISMISSED; 7+ günlük PENDING
review-lər avtomatik APPROVE → XP SQL-də). Admin API: `/admin/*` (D-082).
