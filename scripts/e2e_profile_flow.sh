#!/usr/bin/env bash
# e2e_profile_flow.sh — D-152: signup → fn_save_profile → fn_ensure_profile →
# profiles/user_interests OXU. İnternetdən real API ilə (APK-nın gördüyü görünüş).
set -euo pipefail

ANON="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzk0ODcwMDY3LCJleHAiOjE5NTI1NTAwNjd9.nyCQfi9ke_yzcWqD8x67m1LEo71OeeKb3F3AFy-RTBU"
URL="https://supabase.qoshul.com"
EMAIL="e2e-flow-$RANDOM@qoshul.az"

echo "── 1) Signup $EMAIL"
R=$(curl -s -X POST "$URL/auth/v1/signup" \
  -H "apikey: $ANON" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"E2eTest123!\",\"data\":{\"full_name\":\"Son Test\"}}")
TOKEN=$(echo "$R" | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")
if [ -z "$TOKEN" ]; then echo "SIGNUP FAIL: $R"; exit 1; fi
echo "   token OK (${#TOKEN} chars)"

echo "── 2) fn_save_profile (onboarding data)"
R2=$(curl -s -X POST "$URL/rest/v1/rpc/fn_save_profile" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"p_first_name":"Son","p_last_name":"Test","p_city":"Bakı","p_onboarded":true,"p_interests":["Futbol","Qəhvə"]}')
echo "   nəticə: $R2"

echo "── 3) fn_daily_login"
R3=$(curl -s -X POST "$URL/rest/v1/rpc/fn_daily_login" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   nəticə: $R3"

echo "── 4) fn_award_xp (registration)"
R4=$(curl -s -X POST "$URL/rest/v1/rpc/fn_award_xp" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" \
  -d "{\"p_profile_id\":null,\"p_key\":\"registration\",\"p_amount\":50}")
echo "   nəticə (p_profile_id=null → auth.uid fallback yoxdursa xəta gözlənilir): $R4"

echo "── 5) profiles OXU (getprofile)"
R5=$(curl -s "$URL/rest/v1/profiles?select=first_name,last_name,city,onboarded,xp" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   profil: $R5"

echo "── 6) events OXU"
R6=$(curl -s "$URL/rest/v1/events?select=id,title&limit=3" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN")
echo "   events: $R6"

echo "── 7) fn_send_message"
R7=$(curl -s -X POST "$URL/rest/v1/rpc/fn_send_message" \
  -H "apikey: $ANON" -H "Authorization: Bearer $TOKEN" \
  -d '{"p_kind":"event","p_event_id":null,"p_body":"salam"}')
echo "   mesaj: $R7"
