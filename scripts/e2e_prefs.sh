#!/bin/bash
set -euo pipefail
ENV_FILE=~/Qoshul.az/infra/selfhost/.env
ANON=$(grep ^ANON_KEY= "$ENV_FILE" | cut -d= -f2)
API=http://localhost:8000
EMAIL="e2eprefs_$(date +%s)@qoshul.com"
J='Content-Type: application/json'
R=$(curl -s -m 15 -X POST "$API/auth/v1/signup" -H "apikey: $ANON" -H "$J" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Test12345!\",\"data\":{\"full_name\":\"Prefs Test\"}}")
TOKEN=$(echo "$R" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("access_token",""))')
[ -n "$TOKEN" ] || { echo "FAIL signup: $R"; exit 1; }
sleep 1

echo "── fn_save_prefs(OFF, false)"
R1=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_save_prefs" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d '{"p_map_visibility":"OFF","p_show_distance":false}')
echo "   cavab: $R1 (true olmalı)"
[ "$R1" = "true" ] || { echo "✗ FAIL"; exit 1; }

echo "── profil geri oxu (map_visibility=OFF olmalı)"
P=$(curl -s -m 15 "$API/rest/v1/profiles?select=map_visibility,show_distance" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   cavab: $P"
echo "$P" | grep -q '"OFF"' && echo "$P" | grep -q false || { echo "✗ PERSIST FAIL"; exit 1; }

echo "── YANLIŞ dəyər rədd (BAD_VISIBILITY)"
R2=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_save_prefs" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d '{"p_map_visibility":"HACKER"}')
echo "   cavab: ${R2:0:80}"
echo "$R2" | grep -q 'BAD_VISIBILITY' || { echo "✗ VALIDATION FAIL"; exit 1; }

echo "── FRIENDS + true"
R3=$(curl -s -m 15 -X POST "$API/rest/v1/rpc/fn_save_prefs" -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "$J" -d '{"p_map_visibility":"FRIENDS","p_show_distance":true}')
[ "$R3" = "true" ] || { echo "✗ FRIENDS FAIL"; exit 1; }
echo "   OK"

docker exec supabase-db psql -U supabase_admin -d postgres -q -c "delete from auth.users where email='$EMAIL'"
echo "✅ PREFS E2E KEÇDİ"
