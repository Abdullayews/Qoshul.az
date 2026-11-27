#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════
# reset_selfhost.sh — DB-ni TAM sıfırlayıb 0001→0008 migration-larla
# yenidən qurur (D-167 playbook — serverdə bir dəfə əl ilə edilən
# addımların avtomatlaşdırması; ilk wipe zamanı tapılan tələslər daxildir).
#
# Nə edir:
#   0. pg_dump backup (~/backups/pre_reset_<vaxt>.sql.gz)
#   1. drop schema public cascade + graphql_public; truncate auth.users
#   2. create schema public + postgis (public-də!) + standart grant-lar
#   3. 0001..0008 migration-ları SIRA ilə (ON_ERROR_STOP=1)
#   4. anon/authenticated cədvəl+funksiya grant-ları (RLS qoruyur)
#   5. pgrst reload + registry (_qoshul_migrations) doldurulması
#
# İstifadə: bash scripts/reset_selfhost.sh   (repo kökündən, db konteyneri işlərkən)
# ═══════════════════════════════════════════════════════════════════════
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/infra/selfhost/.env"
MIG_DIR="${REPO_ROOT}/supabase/migrations"

[ -f "$ENV_FILE" ] || { echo "XƏTA: $ENV_FILE yoxdur"; exit 1; }

PSQL="docker exec -i supabase-db psql -U supabase_admin -d postgres"

echo "── 0. BACKUP"
mkdir -p ~/backups
BK=~/backups/pre_reset_$(date +%Y%m%d_%H%M).sql.gz
docker exec supabase-db pg_dump -U supabase_admin -d postgres --clean --if-exists 2>/dev/null | gzip > "$BK"
echo "   $BK"

echo "── 1. WIPE (public + graphql_public + auth.users)"
$PSQL -q <<'SQL'
drop schema if exists public cascade;
drop schema if exists graphql_public cascade;
truncate auth.users cascade;
delete from auth.refresh_tokens;
delete from auth.flow_state;
SQL

echo "── 2. İLKİLLƏŞDİRMƏ (public + postgis-in public-də olması ŞƏRTDIR —
       0001 geography(Point,4326) gözləyir; wipe edilməzsə postgis auth
       sxeminə düşür və bütün funksiyalar xəta verir)"
$PSQL -q <<'SQL'
create schema public;
create schema graphql_public;
grant usage on schema public to postgres, supabase_admin, supabase_auth_admin,
  supabase_storage_admin, authenticator, authenticated, anon, service_role;
grant all on schema public to postgres, supabase_admin, anon, authenticated, service_role;
grant usage on schema graphql_public to postgres, supabase_admin, supabase_auth_admin,
  supabase_storage_admin, authenticator, authenticated, anon, service_role;
comment on schema graphql_public is 'Supabase GraphQL schema reference';
create extension if not exists postgis schema public;
create extension if not exists pgcrypto;
SQL

echo "── 3. MİQRASİYALAR 0001→0008"
for f in "${MIG_DIR}"/*.sql; do
  echo "   $(basename "$f")"
  $PSQL -v ON_ERROR_STOP=1 -q < "$f" > /dev/null
done

echo "── 4. ROL GRANT-LARI (RLS sətirləri filtr edir — burada yalnız "
echo "   sxem/obyekt səviyyəsi icazələri)"
$PSQL -q <<'SQL'
grant select on all tables in schema public to anon, authenticated;
grant execute on all functions in schema public to anon, authenticated;
grant usage, select on all sequences in schema public to anon, authenticated;
alter default privileges in schema public grant select on tables to anon, authenticated;
alter default privileges in schema public grant execute on functions to anon, authenticated;
alter default privileges in schema public grant usage, select on sequences to anon, authenticated;
SQL

echo "── 5. POSTGREST SCHEMA CACHE + REGISTRY"
$PSQL -q <<'SQL'
notify pgrst, 'reload schema';
SQL
$PSQL -q <<'SQL'
create table if not exists public._qoshul_migrations (
  filename text primary key, applied_at timestamptz not null default now());
insert into public._qoshul_migrations (filename)
select f from (
  select '0001_schema.sql' f union all select '0002_server_rules.sql' union all
  select '0003_seed.sql' union all select '0004_audit_fixes.sql' union all
  select '0005_fn_match_candidates_fix.sql' union all select '0006_server_hardening.sql' union all
  select '0007_profile_persistence.sql' union all select '0008_google_profile_autofill.sql' union all
  select '0009_leave_and_saved_events.sql' union all
  select '0010_remove_static_defaults.sql' union all
  select '0011_prefs_and_picture_backend.sql'
) t
where f <= (select max(filename) from (
  select regexp_replace(basename, '.*\/', '') as filename from (
    select unnest(string_to_array(current_setting('a'), ',')) as basename
  ) x
) y)
on conflict do nothing;
SQL

echo "── STATUS"
$PSQL -tAc "select 'cədvəllər: ' || count(*) from pg_tables where schemaname='public'"
$PSQL -tAc "select 'kateqoriyalar: ' || count(*) from public.categories"
$PSQL -tAc "select 'məkanlar: ' || count(*) from public.venues"
$PSQL -tAc "select 'istifadəçilər: ' || count(*) from auth.users"

echo "✅ RESET TAMAM — rest/v1 siyahıları dərhal cavab vermirsə:"
echo "   docker compose -f infra/selfhost/docker-compose.yml restart rest"
