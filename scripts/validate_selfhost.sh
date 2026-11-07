#!/usr/bin/env bash
# validate_selfhost.sh — self-host Supabase qurğusunun statik yoxlaması (M5, CI).
#
# Nə yoxlayır:
#   1. bash sintaksisi (migrate_selfhost.sh, generate-keys.sh, docker-entrypoint.sh)
#   2. docker compose config — core + hər profil ayrıca (docker varsa; yoxsa SKIP)
#   3. generate-keys.sh → ANON_KEY/SERVICE_ROLE_KEY HS256 imzasının python3
#      stdlib ilə doğrulanması (JWT_SECRET ilə) + claim yoxlaması
#
# Docker olmadan da işləyir (sandbox/lokal) — compose hissəsi ötürülür.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SELFHOST="${REPO_ROOT}/infra/selfhost"
ENV_EXAMPLE="${SELFHOST}/.env.example"

pass() { echo "  OK   $1"; }
fail() { echo "  FAIL $1" >&2; exit 1; }

echo "── 1. bash sintaksisi ──"
for s in \
  "${REPO_ROOT}/scripts/migrate_selfhost.sh" \
  "${SELFHOST}/generate-keys.sh" \
  "${SELFHOST}/volumes/api/envoy/docker-entrypoint.sh"
do
  bash -n "${s}" || fail "${s}"
  pass "${s##*/}"
done

echo "── 2. docker compose config ──"
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  PROFILES=("" "edge" "dashboard" "storage" "realtime" "functions" "pooler")
  for p in "${PROFILES[@]}"; do
    if [ -z "${p}" ]; then
      label="core"
      args=()
    else
      label="${p}"
      args=(--profile "${p}")
    fi
    docker compose -f "${SELFHOST}/docker-compose.yml" --env-file "${ENV_EXAMPLE}" \
      "${args[@]}" config --quiet || fail "compose config: ${label}"
    pass "compose config: ${label}"
  done
else
  echo "  SKIP: docker yoxdur (yalnız statik yoxlamalar aparıldı)"
fi

echo "── 3. JWT self-test (generate-keys.sh çıxışı) ──"
TMP="$(mktemp -d)"
trap 'rm -rf "${TMP}"' EXIT

( cd "${TMP}" && sh "${SELFHOST}/generate-keys.sh" ) > "${TMP}/keys.txt" 2>/dev/null

JWT_SECRET="$(sed -n 's/^JWT_SECRET=//p' "${TMP}/keys.txt")"
ANON_KEY="$(sed -n 's/^ANON_KEY=//p' "${TMP}/keys.txt")"
SERVICE_ROLE_KEY="$(sed -n 's/^SERVICE_ROLE_KEY=//p' "${TMP}/keys.txt")"
POSTGRES_PASSWORD="$(sed -n 's/^POSTGRES_PASSWORD=//p' "${TMP}/keys.txt")"

[ -n "${JWT_SECRET}" ] || fail "JWT_SECRET generasiya olunmadı"
[ -n "${ANON_KEY}" ] || fail "ANON_KEY generasiya olunmadı"
[ -n "${SERVICE_ROLE_KEY}" ] || fail "SERVICE_ROLE_KEY generasiya olunmadı"
[ -n "${POSTGRES_PASSWORD}" ] || fail "POSTGRES_PASSWORD generasiya olunmadı"
pass "4 sirə generasiya olundu (uzunluq: jwt=${#JWT_SECRET}, pg=${#POSTGRES_PASSWORD})"

JWT_SECRET="${JWT_SECRET}" ANON_KEY="${ANON_KEY}" SERVICE_ROLE_KEY="${SERVICE_ROLE_KEY}" \
python3 - "${TMP}/keys.txt" <<'PY'
import base64, hashlib, hmac, json, os, sys

def b64url_decode(s: str) -> bytes:
    s += "=" * (-len(s) % 4)
    return base64.urlsafe_b64decode(s)

secret = os.environ["JWT_SECRET"].encode()

for name, key, want_role in (
    ("ANON_KEY", os.environ["ANON_KEY"], "anon"),
    ("SERVICE_ROLE_KEY", os.environ["SERVICE_ROLE_KEY"], "service_role"),
):
    h, p, sig = key.split(".")
    header = json.loads(b64url_decode(h))
    payload = json.loads(b64url_decode(p))
    assert header == {"alg": "HS256", "typ": "JWT"}, f"{name}: başlıq {header}"
    assert payload["role"] == want_role, f"{name}: role={payload['role']}"
    assert payload["iss"] == "supabase", f"{name}: iss={payload['iss']}"
    assert payload["exp"] > payload["iat"] > 0, f"{name}: vaxt claim-ləri"
    calc = hmac.new(secret, f"{h}.{p}".encode(), hashlib.sha256).digest()
    assert hmac.compare_digest(calc, b64url_decode(sig)), f"{name}: imza uyğunsuz"
    print(f"  OK   {name}: HS256 imza + role={want_role} doğrulandı")

print("  OK   JWT self-test keçdi")
PY

echo ""
echo "BÜTÜN yoxlamalar keçdi ✓"
