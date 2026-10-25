# PostgreSQL 17 + PostGIS 3 (PGDG) — Oracle A1 (arm64) üçün.
#
# SƏBƏB: rəsmi postgis/postgis imajı YALNIZ amd64-dir — 17-3.5, 17-3.5-alpine,
# 18-3.6, 18-3.6-alpine tag-lərinin hamısında linux/arm64 yoxdur
# (yoxlanılıb — docs/decisions.md D-002). Ona görə eyni resepti (rəsmi
# postgres baza + PGDG paketi — postgis/postgis-in öz üsulu) biz özümüz
# qururuq, amma multi-arch.
#
# Baza: rəsmi postgres:17 (Debian; PGDG repo imajda hazır konfiqurəlidir;
# linux/arm64 təsdiqlənib — D-001).
# Paket: postgresql-17-postgis-3 (arm64: bookworm + trixie-də təsdiqlənib — D-003).
#
# Build: docker compose build (dev / VM2) və ya CI multi-arch (m10).
FROM postgres:17

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-17-postgis-3 \
    && rm -rf /var/lib/apt/lists/*

# İlk init-də default bazada CREATE EXTENSION postgis
COPY initdb-postgis.sh /docker-entrypoint-initdb.d/10-postgis.sh
RUN chmod 755 /docker-entrypoint-initdb.d/10-postgis.sh
