# Backup (Mərhələ 10)

Gündəlik `pg_dump` → gzip → Cloudflare R2 private bucket (`qoshul-backup`), 30 gün rotasiya.

- İcra yeri: **VM1** (client → VM2 :5432) — eyni zamanda idle-reclaim 20% qaydasına qarşı gecəlik real yük (runbook §5)
- Cron/sistem timer + uğursuzluqda Sentry alert
- Ayda 1 bərpa sınağı (lokal/staging)
- Skript + cron unit bu qovluğa m10-da gəlir
