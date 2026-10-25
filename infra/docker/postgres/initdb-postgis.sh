#!/bin/bash
# docker-entrypoint-initdb.d skripti — YALNIZ boş yeni bazada işə düşür.
# (Mövcud volume üzərində ikinci dəfə işləmir — normal Postgres davranışı.)
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS postgis;
EOSQL
