# Backup (Mərhələ 10) — Oracle Object Storage

Gündəlik `pg_dump` → zstd → **OCI OSS** private bucket (`qoshul-backup`), rclone S3 compat API ilə, **14 gün rotasiya** (D-018: 20GB cəmi limitin büdcəsi — media ~15GB, backup ~3GB).

- İcra yeri: **VM1** (client → VM2 :5432) — eyni zamanda idle-reclaim 20% qaydasına qarşı gecəlik real yük (runbook §5)
- rclone konfiqi (m10-da `rclone.conf` script ilə generasiya olunur, açarlar `.env`-dən):
  ```
  [qoshul-oss]
  type = s3
  provider = Other
  endpoint = https://{OCI_NAMESPACE}.compat.objectstorage.{OCI_REGION}.oraclecloud.com
  access_key_id = $OCI_ACCESS_KEY_ID
  secret_access_key = $OCI_SECRET_ACCESS_KEY
  ```
- Cron/sistem timer + uğursuzluqda alert (Prometheus alertmanager — D-021)
- Ayda 1 bərpa sınağı (lokal/staging)
- Skript + cron unit bu qovluğa m10-da gəlir
