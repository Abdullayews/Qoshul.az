#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════
# e2e_test.sh — D-167 canlı uçuş yoxlaması (serverdən, loopback)
#
# Axın: signup → profil trigger yoxlaması → events siyahısı (authenticated)
#       → fn_create_event → fn_join_event → iştirakçı sayı → RLS (anon)
# İstifadə: bash e2e_test.sh   (db konteyneri + .env lazımdır)
# ═══════════════════════════════════════════════════════════════════════
set -euo pipefail

ENV_FILE=~/Qoshul.az/infra/selfhost/.env
ANON=$(grep ^ANON_KEY= "$ENV_FILE" | cut -d= -f2)
API=http://localhost:8000
EMAIL="e2e_$(date +%s)@qoshul.com"
PASS="Test12345!"
J='Content-Type: application/json'

# D-180: test qalığı qarşısı — hər çıxış yolunda (uğurlu exit, set -e
# xətası, Ctrl-C) test useri cascade silinir (əvvəl yalnız addım 9-da
# silinirdi; ortada exit = DB-də "E2E Testov" profili qalırdı).
cleanup() {
  docker exec supabase-db psql -U supabase_admin -d postgres -q -c \
    "delete from auth.users where email='$EMAIL'" 2>/dev/null || true
}
trap cleanup EXIT

echo "══ 1. SIGNUP: $EMAIL"
SIGNUP=$(curl -s -m 15 -X POST "$API/auth/v1/signup" -H "apikey: $ANON" -H "$J" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"data\":{\"full_name\":\"E2E Testov\"}}")
TOKEN=$(echo "$SIGNUP" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("access_token",""))')
if [ -z "$TOKEN" ]; then echo "SIGNUP FAIL: $SIGNUP"; exit 1; fi
echo "   token alındı (${#TOKEN} simvol)"

echo "══ 2. PROFİL TRIGGERİ (0008: full_name → first_name)"
sleep 1
FIRST=$(docker exec supabase-db psql -U supabase_admin -tAc \
  "select first_name || ' ' || last_name from public.profiles where id = (select id from auth.users where email='$EMAIL')")
echo "   profil adı: $FIRST (E2E Testov olmalı)"
[ "$FIRST" = "E2E Testov" ] || { echo "   ✗ TRIGGER XƏTASI"; exit 1; }

echo "══ 3. EVENTS SIYAHISI (authenticated, boş olmalı — heç kim yaratmayıb)"
EV=$(curl -s -m 15 "$API/rest/v1/events?select=id,title" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   cavab: $EV"

echo "══ 4. EVENT YARAT (fn_create_event RPC)"
CREATE=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_create_event" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" \
  -d '{"p_title":"E2E Test Tədbiri","p_category":"qehve","p_city":"Bakı",
       "p_location":"Test kafе","p_event_date":"2026-12-20","p_event_time":"18:00",
       "p_capacity":5,"p_description":"E2E test"}')
EVENT_ID=$(echo "$CREATE" | tr -d '"')
echo "   event id: $EVENT_ID"
[ ${#EVENT_ID} -ge 10 ] || { echo "   ✗ CREATE FAIL: $CREATE"; exit 1; }

echo "══ 5. JOIN (fn_join_event RPC)"
JOIN=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_join_event" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" \
  -d "{\"p_event_id\":\"$EVENT_ID\"}")
echo "   join: $JOIN"
echo "$JOIN" | grep -q '"ok":true' || { echo "   ✗ JOIN FAIL"; exit 1; }

echo "══ 6. İKRAİ QOŞULMA (ALREADY_JOINED olmalı)"
JOIN2=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_join_event" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" \
  -d "{\"p_event_id\":\"$EVENT_ID\"}")
echo "   ikinci join: $JOIN2"
echo "$JOIN2" | grep -q 'ALREADY_JOINED' || { echo "   ✗ DEDUP FAIL"; exit 1; }

echo "══ 7. EVENTS ARTIQ GÖRÜNÜR (authenticated)"
EV2=$(curl -s -m 15 "$API/rest/v1/events?select=title&title=like.E2E*" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   cavab: $EV2"
echo "$EV2" | grep -q "E2E Test" || { echo "   ✗ LIST FAIL"; exit 1; }

echo "══ 8. RLS — ANON events görə BİLMƏZ (events_select yalnız authenticated)"
EV_ANON=$(curl -s -m 15 "$API/rest/v1/events?select=title&title=like.E2E*" -H "apikey: $ANON" -H "Authorization: Bearer $ANON")
echo "   anon cavab: $EV_ANON (boş olmalı)"
[ "$EV_ANON" = "[]" ] || { echo "   ✗ RLS FAIL"; exit 1; }

echo "══ 9. TƏMİZLİK — test user + event sil (trap EXIT ilə)"
echo "   (cascade: profile + event + iştirak silinir — hər çıxışda)"

echo ""
echo "✅ E2E KEÇDİ — signup/trigger/create/join/dedup/list/RLS hamısı sağlam"
