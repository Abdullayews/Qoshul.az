# Runbook — Oracle Cloud Always Free (Ampere A1, 2 VM)

## 1. Provisioning

| Resurs | Dəyər |
|---|---|
| Shape | 2× `VM.Standard.A1.Flex` (1 OCPU, 6 GB RAM hər biri) — cəmi 2 OCPU / 12 GB (Always Free limiti daxilində) |
| Boot volume-lar | 47 GB × 2 (cəmi 200 GB limitə daxil) |
| Block volume | qalan ~106 GB → VM2-yə, mount: `/mnt/qoshul-pgdata` |
| OS | Ubuntu LTS (arm64) — Oracle rəsmi imaj siyahısından |
| Ehtiyat | 2× `VM.Standard.E2.1.Micro` (1/8 OCPU, 1 GB) Always Free-də mövcuddur — bastion/monitorinq ehtiyatı (D-018; idle-reclaim-ə həssas) |

### OCI Always Free limitləri (rəsmi — decisions.md D-018)

| Resurs | Limit |
|---|---|
| Object Storage (Standard + Infrequent Access + Archive cəmi) | **20 GB** |
| Object Storage API sorğuları | **50,000 / ay** |
| Outbound data transfer | **10 TB / ay** |
| Block Volume | 200 GB (+5 volume backup) |
| Flexible Load Balancer | 1× (10 Mbps) — istifadə olunmur (Caddy birbaşa) |

Block volume hazırlığı (VM2):

```bash
sudo mkfs.ext4 -L pgdata /dev/oracleoci/oraclevdb   # disk adını OCI konsolundan dəqiqləşdir
sudo mkdir -p /mnt/qoshul-pgdata
echo 'LABEL=pgdata /mnt/qoshul-pgdata ext4 defaults,noatime 0 2' | sudo tee -a /etc/fstab
sudo mount -a && df -h /mnt/qoshul-pgdata
```

## 2. Şəbəkə (NSG / Security List)

| VM | Port | Mənbə | İzah |
|---|---|---|---|
| VM1 | 80, 443 | 0.0.0.0/0 | Caddy — yeganə publik giriş |
| VM1 | 22 | yalnız admin IP | SSH (yalnız açar; parol bağlı) |
| VM2 | 5432 | **yalnız VM1 private IP** | PostgreSQL — heç vaxt publik |
| VM2 | 22 | yalnız admin IP | SSH |

VCN private subnet: VM1 ↔ VM2 trafiki sərbəst. `PG_BIND_IP` = VM2 private IP — compose 5432-ni yalnız bu IP-yə bind edir (ikinci qoruma qatı).

## 3. `.env` yerləşdirilməsi

- **VM1:** tam `.env` (app + caddy + redis + `DATABASE_URL` → VM2 private IP)
- **VM2:** yalnız `POSTGRES_USER/PASSWORD/DB`, `PG_BIND_IP`, `PGDATA_VOLUME`
- `chmod 600 .env` · heç vaxt koda/git-ə (qayda 3) · fərqlər README-də deyil, burada sənədləşir

## 4. Deploy

```bash
# VM1:
git clone <repo> /opt/qoshul && cd /opt/qoshul   # .env yerləşdir
make vm1-up    # Caddy + backend + Redis (backend imajı m2-dən sonra)

# VM2:
git clone <repo> /opt/qoshul && cd /opt/qoshul   # .env yerləşdir (§3)
make vm2-up    # PostgreSQL build + start (ilk dəfə ~2-3 dəq)
```

m10-dan sonra: CI imajları GHCR-ə push edir, VM-lər `git pull` + `docker compose pull` + `up -d`.

## 5. Idle-reclaim qaydası (20% / 7 gün) — SƏNƏDLƏŞDƏRME

**Qayda:** Always Free instansı 7 gün ərzində CPU (95-ci percentile) **VƏ** şəbəkə **VƏ** yaddaş istifadəsinin hamısı 20%-dən aşağı olsa geri alınır. Vacib: şərt **"hamısı birlikdə aşağı"**dır — bir göstərici 20% üstü olsa, instans idle sayılmır.

**Ölçmə semantikası:** Oracle-un dəqiq ölçmə metrikası (page cache sayılır mı və s.) rəsmi sənədlərdə qeyri-müəyyəndir — status: **təsdiqlənməyib** (decisions.md D-011). Əvvəlki sessiyada faktiki müşahidə: geri alınma = **dayandırma (stop)**, silinmə deyil; əvvəlcə email xəbərdarlığı gəlir; DNS-only instans 1 aydır sağ qalır. Dizayn buna güvənmir — aşağıdakı büdcə ən pis hal üçündür.

### VM2 yaddaş büdcəsi (6 GB) — deterministik qoruma ✅

| Komponent | Rezident | % |
|---|---|---|
| OS + Docker daemon | ~0.7–0.9 GB | 12–15% |
| PostgreSQL `shared_buffers=1GB` — **sabit, heç vaxt buraxılmır** | 1.0 GB | 17% |
| bağlantılar (`work_mem`×50 max) + autovacuum + WAL | ~0.1–0.3 GB | 2–5% |
| **Cəmi rezident** | **~1.8–2.2 GB** | **30–37%** |

`shared_buffers` Postgres tərəfindən birbaşa shared memory kimi ayrılır və proses yaşadıqca rezident qalır → VM2 yaddaş istifadəsi struktura görə **heç vaxt 20%-nin altına düşə bilməz**. Bu, compose-da sərt kodlanmışdır (`docker-compose.vm2.yml`).

### VM1 yaddaş büdcəsi (6 GB) — aktiv komponentlər + gecəlik real iş

| Komponent | Rezident (idle) | % |
|---|---|---|
| OS + Docker daemon | ~0.7–1.0 GB | 12–17% |
| NestJS backend (Node, `--max-old-space-size=1536`, idle baseline) | 0.15–0.25 GB | 2.5–4% |
| Redis (AOF, maxmemory 768mb limit; idle ~50–100 MB) | ~0.1 GB | 1.5% |
| Caddy | ~0.05 GB | 0.8% |
| **Baseline cəmi** | **~1.0–1.4 GB** | **17–23%** (sərhəd) |

VM1-də `shared_buffers` ekvivalenti yoxdur → sərhəddi keçərək aşağı düşə bilər. Ona görə **hər gecə real iş** planlaşdırılır (saxtı yük generatoru YAZILMIR — baxım siyasəti baxımından riskli və dəyər yaratmır):

1. **Backup relay (m10):** `pg_dump` VM1-də icra olunur (client → VM2 :5432), zstd sıxılır, OCI OSS `qoshul-backup` bucket-inə yüklənir (rclone, S3 compat) → CPU (sıxma) + şəbəkə (tam DB oxunu + upload) + yaddaş (page cache) — üç metricdə birdən real aktivlik.
2. **nightly-maintenance (BullMQ):** köhnə token/OTP təmizliyi, statistika hesablanması → CPU + DB trafiki.

Nəticə: "7 gün hamısı 20%-dən aşağı" şərti heç vaxt ardıcıl tamamlanmır. Real istifadəçi trafiki (hətta az) bunu daha da gücləndirir.

**Beta fazası (D-022 — ≤4 tester):** istifadəçi trafigi ~0 olacaq → yuxarıdakı mitigasiyalar bu fazada **yeganə qoruyucudur**; gecəlik backup relay + nightly-maintenance işləri heç vaxt bağlanmamalıdır.

### Monitorinq

```bash
free -m                        # VM səviyyəsi
docker stats --no-stream       # konteyner səviyyəsi
```

Oracle Console → Instance → Metrics: CPU / Network / Memory qrafikləri. m10-dan sonra Prometheus + Grafana (`infra/monitoring/`) + alert: hər hansı VM-də yaddaş < 25% olsa xəbərdarlıq.

## 6. Backup / bərpa (m10-da avtomatlaşır)

- **Gündəlik:** VM1-də `pg_dump -Fc` → zstd → rclone (S3 compat API) → OCI OSS private bucket (`qoshul-backup`) · **14 gün rotasiya** (20GB cəmi limitin büdcəsi: media ~15GB, backup ~3GB — architecture.md Media)
- **Bərpa sınağı:** ayda 1 dəfə (staging/lokal) — backup-un backup olduğu yox, bərpa edə bildiyi sınaqla müəyyən olunur
- **Bucket siyasəti:** `qoshul-media` publik oxunuş (UUID açarlar), `qoshul-backup` private; açarlar Customer Secret Keys (.env — qayda 3)
- Bucket yaratmaq: OCI Console → Object Storage → Create Bucket (region = `OCI_REGION`)

## 7. Fəlakət və Hetznerə köç

- **Idle-reclaim baş verərsə:** email gəlir → instansı start et → `make vm1-up && make vm2-up` → ~1 saatlıq əməliyyat (data diskdə qalır — əvvəlki sessiyada doğrulanmış fakt).
- **Hetznerə köç:** bütün fərq = `.env` (yeni IP/domen) + DNS rekordları. Compose faylları host-agnostikdir.

## 8. Təhlükəsizlik təqvimi

| Hadisə | Tezlik |
|---|---|
| İmaj yeniləməsi (base image patch) | ayda 1 (m10-dan sonra CI) |
| `.env` açar rotasiyası (JWT secrets, OSS Customer Secret Keys) | 6 ayda 1 |
| SSH: yalnız açar, parol bağlı, admin IP filtri | quraşdırma zamanı |
| Backup bərpa sınağı | ayda 1 |

## 9. Xarici asılılıqlar (Oracle-dan kənar — D-020)

| Xidmət | Səbəb |
|---|---|
| GitHub (kod + CI) | pulsuz; APK pipeline istənilən; self-hosted runner VM1 RAM bölgüsünə görə m10-da qiymətləndiriləcək |
| FCM | Android push yalnız Google infrastrukturu ilə |
| Google/Apple OAuth | istifadəçi hesabları — xarici IdP təbiətinə görə |
| SMTP provayder | OCI outbound **port 25 blokludur** → self-host SMTP mümkün deyil; pulsuz tier provayder (email təsdiqi m3) |
| SMS OTP gateway | operator infrastrukturu (opsional, m3) |
| AI provayder | xarici LLM API (m9, proxy arxasında) |
| DNS | pulsuz (registrar/Cloudflare); OCI DNS Always Free daxil deyil — təsdiqlənməyib |
