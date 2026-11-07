# Self-host Supabase — Oracle Always Free (M5)

> **Status:** infra hazırdır (D-083…D-086). Deploy Oracle Always Free VM-də —
> `VM.Standard.A1.Flex` (ARM64 Ampere A1). Cloud Supabase **istifadə olunmur**;
> bütöv Supabase stack-i öz serverimizdə işləyir (istifadəçi direktivi, 2026-11-07).
>
> **İstifadəçidən gözlənilən dəyərlər (🔶):** §5-də siyahı — repo-da heç biri
> yazılmır, hamisi serverdə `.env` / `local.properties` içində qalır (D-079).

## 1. Oracle Always Free — cari limitlər (D-083)

| Resurs | Limit | Qeyd |
|---|---|---|
| Ampere A1 (ARM64) | **2 OCPU / 12 GB RAM** | 2026-06/07-də azaldıldı (əvvəl 4/24 idi) |
| A1 instans sayı | 1–2 (total 2 OCPU) | 1× (2 OCPU, 12 GB) tövsiyə olunur |
| Block storage | **200 GB** (boot + block) | 100 GB boot + 100 GB data kifayətdir |
| Şəbəkə | 10 TB/ay trafik (egress) | Bolluqda |

RAM büdcəsi (12 GB — core stack ~1 GB-dən az):

| Servis | Təxmini RAM |
|---|---|
| db (postgres 17) | 300–500 MB |
| auth (gotrue) | ~50 MB |
| rest (postgrest) | ~50 MB |
| api-gw (envoy) | ~50 MB |
| caddy (TLS) | ~30 MB |
| M4 servisi (axum) | ~40 MB |
| **Core cəmi** | **~0.8 GB** (+ OS ~0.5 GB) |
| dashboard profili (studio+meta) | + ~600 MB |
| storage / realtime / functions | + hər biri ~150–300 MB |

## 2. Topologiya

```
İnternet
   │  :443 TLS (Let's Encrypt)
   ▼
┌──────────┐      ┌──────────────────────┐
│  caddy   │─────▶│ api-gw (envoy)       │  127.0.0.1:8000
│ (edge)   │      │  /rest/v1 ───────────┼──▶ rest (postgrest :3000)
└──────────┘      │  /auth/v1  ──────────┼──▶ auth (gotrue :9999)
                  │  /dashboard ─────────┼──▶ studio (:3000, profil)
                  └──────────────────────┘
                            │ docker şəbəkəsi (port xaricə ÇIXMIR)
                            ▼
                  ┌──────────────────────┐
                  │ db (postgres 17:5432)│  postgis + pgcrypto daxili
                  └──────────────────────┘  migrationlar: docker compose exec
```

Tətbiq (supabase-kt) və M4 servisi (service_role) eyni API səthini görür —
kod DƏYİŞMİR, yalnız URL/açar dəyişir.

## 3. Oracle VM hazırlığı (bir dəfə)

1. **VM yarat**: Compute → Create instance → Shape `VM.Standard.A1.Flex`,
   2 OCPU / 12 GB; image **Ubuntu 24.04 ARM** (aarch64); boot volume 100 GB.
2. **Security List**: VCN → Subnet → Security List → Add Ingress Rules:
   `TCP 80` və `TCP 443` (mənbə `0.0.0.0/0`). **5432/8000 AÇMA!**
3. **Instance iptables** (Oracle image-ləri default olaraq hər şeyi bloklayır —
   Ubuntu image-də də):
   ```bash
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save
   ```
4. **Swap** (A1 VM-lərində default yoxdur; 12 GB RAM-da ehtiyat üçün 2 GB):
   ```bash
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
   sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
5. **Docker** (Docker CE aarch64):
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker $USER   # çıxıb-girin
   ```

## 4. İlk quraşdırma

```bash
# repo
git clone https://github.com/Abdullayews/Qoshul.az.git && cd Qoshul.az/infra/selfhost

# .env + sirlər (POSTGRES_PASSWORD, JWT_SECRET, ANON_KEY, SERVICE_ROLE_KEY,
# DASHBOARD_PASSWORD və s. avtomatik yaranır — openssl əsaslı)
cp .env.example .env
sh generate-keys.sh --update-env

# 🔶 domen dəyərlərini əl ilə redaktə et (.env):
#   SUPABASE_PUBLIC_URL=https://supabase.<domain>
#   API_EXTERNAL_URL=https://supabase.<domain>/auth/v1
#   PROXY_DOMAIN=supabase.<domain>
#   SITE_URL=https://qoshul.az
nano .env

# core + TLS (production minimumu)
docker compose --profile edge up -d
docker compose ps          # db/auth/rest/api-gw/caddy healthy olmalıdır

# Qoshul.az sxemi + qaydalar + seed (repo kökündən)
cd ../..
scripts/migrate_selfhost.sh
#   → "OK 0001_schema.sql / 0002_server_rules.sql / 0003_seed.sql"
#   → public cədvəl sayı: 29+

# sağlamlıq yoxlaması
curl -s https://supabase.<domain>/rest/v1/ -H "apikey: $ANON_KEY" | head -c 200
curl -s https://supabase.<domain>/auth/v1/health   # {"name":"GoTrue",...}
```

Admin panel lazımdırsa: `docker compose --profile edge --profile dashboard up -d`
→ `https://supabase.<domain>/dashboard` (basic auth: `DASHBOARD_USERNAME/PASSWORD`).

## 5. 🔶 İstifadəçidən gözlənilən dəyərlər (D-079 siyahısı)

| # | Dəyər | Harada | Status |
|---|---|---|---|
| 1 | Oracle Always Free VM (2 OCPU/12 GB, Ubuntu ARM) | §3 | 🔶 server |
| 2 | Domen + DNS A qeydi (məs. `supabase.qoshul.az` → VM IP) | `.env`: `PROXY_DOMAIN`, `SUPABASE_PUBLIC_URL`, `API_EXTERNAL_URL` | 🔶 domen |
| 3 | `SITE_URL` (tətbiq domeni) | `.env` | 🔶 domen |
| 4 | `SUPABASE_URL` + `SUPABASE_ANON_KEY` (self-host) | `android-app/local.properties` | 🔶 deploy sonrası |
| 5 | `SUPABASE_SERVICE_ROLE_KEY` (YALNIZ M4 servisdə) | service env | 🔶 deploy sonrası |
| 6 | `ADMIN_TOKEN` (M4 admin API) | service env | 🔶 deploy sonrası |
| 7 | `FCM_SERVICE_ACCOUNT_JSON` (M4 push) | service env | 🔶 push aktivləşəndə |
| 8 | SMTP (real email təsdiqi istəsən) | `.env` SMTP bölməsi | 🔶 istəyə bağlı |

Tətbiq tərəfi (`android-app/local.properties`):

```properties
SUPABASE_URL=https://supabase.<domain>
SUPABASE_ANON_KEY=<.env içindəki ANON_KEY — generate-keys.sh çıxışı>
BACKEND_MODE=SUPABASE
```

M4 mikroservisi (eyni hostda, docker şəbəkəsi daxilindən):

```bash
SUPABASE_URL=http://api-gw:8000 \
SUPABASE_SERVICE_ROLE_KEY=<SERVICE_ROLE_KEY> \
ADMIN_TOKEN=<openssl rand -hex 32> \
cargo run --release   # service/ qovluğundan — yaxud öz konteynerində
```

## 6. Gündəlik əməliyyatlar

```bash
# status / loglar
docker compose ps
docker compose logs -f api-gw auth rest

# yeniləmə (imaj pinləri dəyişəndə)
git pull && docker compose --profile edge up -d

# migration əlavə olunanda (yalnız yeni fayllar tətbiq olunur)
scripts/migrate_selfhost.sh

# ehtiyat nüsxə (günlük cron üçün)
docker compose exec -T db pg_dumpall -U postgres | gzip > backup-$(date +%F).sql.gz

# bərpa (testdə yoxla!)
gunzip -c backup-2026-11-07.sql.gz | docker compose exec -T db psql -U postgres
```

## 7. Təhlükəsizlik qeydləri

- **5432 heç vaxt xaricə açılmır** (db port publish olunmur; pooler profili
  belə 127.0.0.1-ə bağlı — D-085).
- `SERVICE_ROLE_KEY` yalnız M4 mikroservisində; tətbiqə heç vaxt düşmür (qayda 3).
- `.env` `.gitignore`-dadır (`/infra/selfhost/.env`); repo-ya heç vaxt düşmür.
- RLS + SECURITY DEFINER qaydalar (D-076/D-077) self-host-da eyni işləyir —
  sxem fərqi yoxdur, çünki `supabase/postgres` imajı eyni `auth.users` /
  `auth.uid()` səthini verir.
- Caddy: HSTS + TLS avtomatik yeniləmə.

## 8. Faylların mənşəyi (vendored)

| Fayl | Mənbə | Lisenziya |
|---|---|---|
| `volumes/api/envoy/*` (4 fayl) | supabase/supabase@`396358c` docker/volumes/api/envoy | Apache-2.0 |
| `volumes/db/*.sql` (7 fayl) | eyni SHA, docker/volumes/db | Apache-2.0 |
| `volumes/pooler/pooler.exs` | eyni SHA, docker/volumes/pooler | Apache-2.0 |
| `generate-keys.sh` | eyni SHA, docker/utils (Inder Singh setup.sh əsasında) | Apache-2.0 |
| `docker-compose.yml` | eyni SHA-dan SLIM-ləşdirilib (profil strukturu D-085) | — |
| `volumes/api/caddy/Caddyfile` | Qoshul.az | — |

ARM64 (aarch64) dəstəyi — bütün imajlar Docker Hub API ilə yoxlanılıb
(2026-11-07, D-084): supabase/postgres:17.6.1.136, supabase/gotrue:v2.197.0,
postgrest/postgrest:v14.18, envoyproxy/envoy:v1.39.6, supabase/studio:2026.11.02,
supabase/postgres-meta:v0.100.0, supabase/storage-api:v1.89.12,
darthsim/imgproxy:v4.0.21, supabase/realtime:v2.148.3,
supabase/edge-runtime:v1.77.9, supabase/supavisor:2.9.13, caddy:2-alpine.
