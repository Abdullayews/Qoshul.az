#!/usr/bin/env bash
# loop_metrics.sh — avtomatik dövr üçün metrika toplayıcı (Master Prompt, P4 metrik loopu).
#
# Ölçür:
#   1) Son release APK ölçüsü (GitHub API asset.size — bayt və MB; lokal endirmə lazım deyil)
#   2) Son CI run statusu (main budağı)
#   3) Audit test sayı (DirectivesAuditTest) və ümumi unit test sayı (repo grep)
#   4) Soyuq başlanğıc / login vaxtı — REAL CİHAZ TƏLƏB EDİR: həmişə "ölçülmədi" qeyd olunur,
#      heç bir halda uydurma rəqəm yazılmır.
#
# Nəticə: docs/LOOP_LOG.md sonuna tarixli qeyd əlavə edir (append — köhnə qeydlər saxlanılır).
# İstifadə: bash scripts/loop_metrics.sh          (yalnız ölç + qeyd)
#          bash scripts/loop_metrics.sh --print   (qeydi yalnız stdout-a yaz, fayla toxunma)
#
# Token git remote-undan oxunur (make_release.py pattern) — heç vaxt çap olunmur.
set -euo pipefail
cd "$(dirname "$0")/.."

PRINT_ONLY=0
[ "${1:-}" = "--print" ] && PRINT_ONLY=1

# ── Token git remote-dan (x-access-token:TOKEN@ və ya TOKEN@ formatları) ──
TOKEN=$(git config --get remote.origin.url | sed -E 's#https://[^:]+:([^@]+)@.*#\1#; t; s#https://([^@]+)@.*#\1#')
if [ -z "$TOKEN" ]; then
  echo "XƏTA: remote.origin.url-də token yoxdur" >&2; exit 1
fi
AUTH="Authorization: Bearer $TOKEN"
API="https://api.github.com/repos/Abdullayews/Qoshul.az"
ACCEPT="Accept: application/vnd.github+json"

# ── 1) Son release APK ölçüsü ──
REL_JSON=$(curl -sf -H "$AUTH" -H "$ACCEPT" "$API/releases/latest")
REL_TAG=$(printf '%s' "$REL_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["tag_name"])')
APK_SIZE=$(printf '%s' "$REL_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["assets"][0]["size"])')
APK_MB=$(python3 -c "print(f'{$APK_SIZE/1048576:.1f}')")

# ── 2) Son CI run (main) ──
CI_JSON=$(curl -sf -H "$AUTH" -H "$ACCEPT" "$API/actions/runs?per_page=1&branch=main")
CI_SHA=$(printf '%s' "$CI_JSON" | python3 -c 'import json,sys; r=json.load(sys.stdin)["workflow_runs"][0]; print(r["head_sha"][:7])')
CI_CONCL=$(printf '%s' "$CI_JSON" | python3 -c 'import json,sys; r=json.load(sys.stdin)["workflow_runs"][0]; print(r["conclusion"] or r["status"])')

# ── 3) Test sayı (repo grep) ──
AUDIT_N=$(grep -c '@Test' android-app/app/src/test/java/az/qoshul/app/DirectivesAuditTest.kt || echo 0)
UNIT_N=$(grep -rc '@Test' android-app/app/src/test --include='*.kt' 2>/dev/null | awk -F: '{s+=$2} END {print s+0}')

# ── 4) Qeyd ──
STAMP=$(date '+%Y-%m-%d %H:%M')
ENTRY="### Metrik qeydi — $STAMP (avtomatik)

- Release: $REL_TAG · APK $APK_SIZE bayt ($APK_MB MB)
- CI (main, $CI_SHA): $CI_CONCL
- Testlər: $UNIT_N unit (o cümlədən $AUDIT_N audit)
- Soyuq başlanğıc / login vaxtı: ölçülmədi — real cihaz tələb olunur
"

if [ "$PRINT_ONLY" = "1" ]; then
  printf '%s\n' "$ENTRY"
else
  # LOOP_LOG.md sonu "avtomatik metrikalar" bölməsinə append
  if ! grep -q '^## Avtomatik metrikalar' docs/LOOP_LOG.md; then
    printf '\n---\n\n## Avtomatik metrikalar (loop_metrics.sh qeydləri)\n' >> docs/LOOP_LOG.md
  fi
  printf '%s\n' "$ENTRY" >> docs/LOOP_LOG.md
  echo "LOOP_LOG.md-ə qeyd əlavə olundu ($STAMP)"
fi
