#!/bin/bash
# D-169 canlı yoxlama: join → leave → save/unsave
set -euo pipefail
ENV_FILE=~/Qoshul.az/infra/selfhost/.env
ANON=$(grep ^ANON_KEY= "$ENV_FILE" | cut -d= -f2)
API=http://localhost:8000
EMAIL="e2els_$(date +%s)@qoshul.com"
J='Content-Type: application/json'

echo "══ SIGNUP: $EMAIL"
R=$(curl -s -m 15 -X POST "$API/auth/v1/signup" -H "apikey: $ANON" -H "$J" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Test12345!\",\"data\":{\"full_name\":\"Leave Save\"}}")
TOKEN=$(echo "$R" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("access_token",""))')
[ -n "$TOKEN" ] || { echo "FAIL: $R"; exit 1; }
AUTH="apikey: $ANON
Authorization: Bearer $TOKEN"
sleep 1

echo "══ CREATE EVENT"
EID=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_create_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" \
  -d '{"p_title":"LS Test","p_category":"qehve","p_city":"Bakı","p_location":"X","p_event_date":"2026-12-25","p_event_time":"18:00","p_capacity":4,"p_description":"t"}' | tr -d '"')
echo "   event: $EID"

echo "══ SAVE (toggle → true)"
S1=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_save_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d "{\"p_event_id\":\"$EID\"}")
echo "   save: $S1 (true olmalı)"
[ "$S1" = "true" ] || { echo "✗ SAVE FAIL"; exit 1; }

echo "══ SAVED SIYAHISI (saved_events)"
SV=$(curl -s -m 15 "$API/rest/v1/saved_events?select=event_id" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   cavab: $SV (EID olmalı)"
echo "$SV" | grep -q "$EID" || { echo "✗ SAVED LIST FAIL"; exit 1; }

echo "══ UNSAVE (toggle → false)"
S2=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_save_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d "{\"p_event_id\":\"$EID\"}")
echo "   unsave: $S2 (false olmalı)"
[ "$S2" = "false" ] || { echo "✗ UNSAVE FAIL"; exit 1; }

echo "══ JOIN (host öz eventinə)"
J1=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_join_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d "{\"p_event_id\":\"$EID\"}")
echo "   join: $J1"

echo "══ LEAVE"
L1=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_leave_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d "{\"p_event_id\":\"$EID\"}")
echo "   leave: $L1 (ok:true olmalı)"
echo "$L1" | grep -q '"ok":true' || { echo "✗ LEAVE FAIL"; exit 1; }

echo "══ İKRAİ LEAVE (NOT_JOINED olmalı)"
L2=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_leave_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d "{\"p_event_id\":\"$EID\"}")
echo "   ikinci leave: $L2"
echo "$L2" | grep -q 'NOT_JOINED' || { echo "✗ DEDUP FAIL"; exit 1; }

echo "══ YENİDƏN JOIN (LEFT-dən sonra mümkün olmalı)"
J2=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_join_event" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d "{\"p_event_id\":\"$EID\"}")
echo "   rejoin: $J2"
echo "$J2" | grep -q '"ok":true' || { echo "✗ REJOIN FAIL"; exit 1; }

echo "══ RLS — ANON saved_events görə bilməz"
SA=$(curl -s -m 15 "$API/rest/v1/saved_events?select=event_id" -H "apikey: $ANON" -H "Authorization: Bearer $ANON")
echo "   anon: $SA (boş olmalı)"
[ "$SA" = "[]" ] || { echo "✗ RLS FAIL"; exit 1; }

echo "══ TƏMİZLİK"
docker exec supabase-db psql -U supabase_admin -d postgres -q -c "delete from auth.users where email='$EMAIL'"
echo ""
echo "✅ LEAVE/SAVE E2E KEÇDİ"
