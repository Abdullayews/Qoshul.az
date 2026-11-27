#!/usr/bin/env bash
# migrate_selfhost.sh — supabase/migrations/*.sql fayllarını self-host Supabase
# db konteynerinə SIRA İLƏ tətbiq edir (M5, D-086).
#
# Xüsusiyyətlər:
#   * İDEMPOTENT: tətbiq olunan fayllar `_qoshul_migrations` cədvəlində
#     qeyd olunur — təkrar işə salınca SKIP edilir.
#   * SƏRT: psql -v ON_ERROR_STOP=1 — bir xəta = dərhal dayanır (yarıçıq
#     vəziyyət yaranmır; növbəti fayl başlamaz).
#   * Sıra: ls ilə leksikoqrafik (0001 → 0002 → 0003).
#
# İstifadə (repo kökündən):
#   scripts/migrate_selfhost.sh                    # normal işlətmə (PRODUCTION)
#   scripts/migrate_selfhost.sh --with-testdata    # + supabase/testdata seed
#   DRY_RUN=1 scripts/migrate_selfhost.sh          # yalnız statusu göstər
#
# D-177 (ikinci dalğa): supabase/testdata/ TEST seed-ləri (100 dolu demo
# istifadəçi) İSTEHSAŁ axınında İCRA EDİLMİR — yalnız --with-testdata
# bayrağı ilə (dev/test serverləri üçün). Migrations qovluğu təmiz qalır.
#
# Ön şərt: core stack işə düşüb (docker compose up -d), .env mövcuddur.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/infra/selfhost/docker-compose.yml"
ENV_FILE="${REPO_ROOT}/infra/selfhost/.env"
MIG_DIR="${REPO_ROOT}/supabase/migrations"
TESTDATA_DIR="${REPO_ROOT}/supabase/testdata"

# — D-177: --with-testdata bayrağı (default: YOX — production təmizdir) —
WITH_TESTDATA=0
for arg in "$@"; do
  case "${arg}" in
    --with-testdata) WITH_TESTDATA=1 ;;
    *) echo "NAMƏLUM arqument: ${arg} (yalnız --with-testdata icazəlidir)" >&2; exit 1 ;;
  esac
done

if [ ! -f "${ENV_FILE}" ]; then
  echo "XƏTA: ${ENV_FILE} tapılmadı — əvvəl 'cp infra/selfhost/.env.example infra/selfhost/.env' edin." >&2
  exit 1
fi
if [ ! -d "${MIG_DIR}" ]; then
  echo "XƏTA: ${MIG_DIR} tapılmadı." >&2
  exit 1
fi

# Docker kompozisiya əmri (hər çağırışda eyni arqumentlər)
DC="docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE}"
PSQL="${DC} exec -T db psql -v ON_ERROR_STOP=1 -U postgres -d postgres"

# DB konteyneri sağlamdırmı?
if ! ${DC} exec -T db pg_isready -U postgres -d postgres -q 2>/dev/null; then
  echo "XƏTA: db konteyneri cavab vermir — 'docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} up -d' işə salın." >&2
  exit 1
fi

# Registry cədvəli (idempotensiya üçün)
${PSQL} -q <<'SQL'
CREATE TABLE IF NOT EXISTS _qoshul_migrations (
  filename   text PRIMARY KEY,
  applied_at timestamptz NOT NULL DEFAULT now()
);
SQL

echo "── Qoshul.az migration statusu ──"
applied_count=$(${PSQL} -tAc "SELECT count(*) FROM _qoshul_migrations")
echo "Tətbiq olunub: ${applied_count} fayl"
echo ""

shopt -s nullglob
files=("${MIG_DIR}"/*.sql)
shopt -u nullglob

if [ ${#files[@]} -eq 0 ]; then
  echo "XƏTA: ${MIG_DIR} içində .sql faylı yoxdur." >&2
  exit 1
fi

for f in "${files[@]}"; do
  name="$(basename "${f}")"
  applied=$(${PSQL} -tAc "SELECT 1 FROM _qoshul_migrations WHERE filename = '${name}'")

  if [ "${applied}" = "1" ]; then
    echo "  SKIP  ${name} (artıq tətbiq olunub)"
    continue
  fi

  if [ "${DRY_RUN:-0}" = "1" ]; then
    echo "  DRY   ${name} (tətbiq olunacaq)"
    continue
  fi

  echo "  APPLY ${name} ..."
  if ${PSQL} -q -f - < "${f}"; then
    ${PSQL} -q -c "INSERT INTO _qoshul_migrations (filename) VALUES ('${name}')"
    echo "  OK    ${name}"
  else
    echo "XƏTA: ${name} tətbiq olunmadı — ON_ERROR_STOP ilə dayandırıldı." >&2
    exit 1
  fi
done

# ── D-177: TEST SEED-LƏR (yalnız --with-testdata; production DEFAULT YOX) ──
# supabase/testdata/ faylları _qoshul_migrations registry-sinə 'testdata/'
# prefiksi ilə yazılır — idempotensiya eyni qaydada işləyir.
if [ "${WITH_TESTDATA}" = "1" ]; then
  echo ""
  echo "── Test seed-ləri (--with-testdata; İSTEHSAL ÜÇÜN DEYİL) ──"
  if [ ! -d "${TESTDATA_DIR}" ]; then
    echo "XƏTA: ${TESTDATA_DIR} tapılmadı." >&2
    exit 1
  fi
  shopt -s nullglob
  td_files=("${TESTDATA_DIR}"/*.sql)
  shopt -u nullglob
  if [ ${#td_files[@]} -eq 0 ]; then
    echo "  (testdata qovluğunda .sql faylı yoxdur)"
  fi
  for f in "${td_files[@]}"; do
    name="testdata/$(basename "${f}")"
    applied=$(${PSQL} -tAc "SELECT 1 FROM _qoshul_migrations WHERE filename = '${name}'")
    if [ "${applied}" = "1" ]; then
      echo "  SKIP  ${name} (artıq tətbiq olunub)"
      continue
    fi
    if [ "${DRY_RUN:-0}" = "1" ]; then
      echo "  DRY   ${name} (tətbiq olunacaq)"
      continue
    fi
    echo "  APPLY ${name} ..."
    if ${PSQL} -q -f - < "${f}"; then
      ${PSQL} -q -c "INSERT INTO _qoshul_migrations (filename) VALUES ('${name}')"
      echo "  OK    ${name}"
    else
      echo "XƏTA: ${name} tətbiq olunmadı." >&2
      exit 1
    fi
  done
fi

echo ""
if [ "${DRY_RUN:-0}" = "1" ]; then
  echo "DRY_RUN: heç nə yazılmadı."
else
  echo "Bitdi. Yoxlama (cədvəl sayı — 0001-dən sonra 29+ olmalıdır):"
  ${PSQL} -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'"
fi
