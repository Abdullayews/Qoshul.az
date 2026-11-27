# Test data (MOCK→SUPABASE keçidi, D-147)

Bu qovluq **yalnız test məqsədli** məlumat saxlayır. `supabase/migrations/`
zəncirinə DAXİL DEYİL — `migrate_selfhost.sh` burdan heç nə oxumur (0003
prinsipi: demo data migration zəncirinə düşmür; tədbirlər istifadəçilər
tərəfindən yaradılır). Serverə əl ilə, fərqində olaraq tətbiq olunur.

## `0006_test_users_seed.sql` — 100 dolu test user

APK-nın SUPABASE rejimində real login/məlumat axını sınamaq üçün
(2026-11-24, canlı serverdə tətbiq olunub və doğrulanıb):

- **Giriş:** `testuser001@qoshul.com` … `testuser100@qoshul.com`
- **Parol (hamısı eyni):** `QoshulTest!2026`
- **Məzmun:** hər hesabda tam profil (bio, maraq, lokasiya, XP, streak),
  2–5 tədbir iştirakı, event + şəxsi mesajlar, bildirişlər; 30 tədbir,
  10 venue, 60 swipe, 25 match, 25 review, 20 invite. Boş hesab YOXDUR
  (aşağıdakı audit sorğularının hamısı 0 qaytarır).
- Parollar bcrypt (cost 10) — GoTrue uyğun; PII yoxdur (ümumi Azərbaycan
  adları, `@qoshul.com` test domeni, saxta koordinatlar).

### Tətbiq (yalnız test serverində!)

```bash
# 1) mövcud test məlumatını təmizlə (prod-a ASMAYIN):
sudo docker compose -f ~/Qoshul/infra/selfhost/docker-compose.yml \
  exec -T db psql -U supabase_admin -d postgres -c \
  "truncate public.reviews, public.matches, public.swipes, public.invites,
   public.notifications, public.xp_events, public.messages,
   public.event_participants, public.events, public.user_interests,
   public.notification_settings, public.profiles, public.venues cascade;
   delete from auth.users;"

# 2) seed-i tətbiq et:
sudo docker compose -f ~/Qoshul/infra/selfhost/docker-compose.yml \
  exec -T db psql -U supabase_admin -d postgres \
  -v ON_ERROR_STOP=1 -f /dev/stdin < 0006_test_users_seed.sql
```

### Boş-hesab audit sorğuları (hamısı 0 olmalı)

```sql
select count(*) from public.profiles where not onboarded or coalesce(bio,'')='';
select count(*) from public.profiles p where not exists
  (select 1 from public.user_interests ui where ui.profile_id=p.id);
select count(*) from public.profiles p where not exists
  (select 1 from public.event_participants ep where ep.profile_id=p.id)
  and not exists (select 1 from public.events e where e.host_profile_id=p.id);
select count(*) from public.profiles p where not exists
  (select 1 from public.messages m where m.sender_id=p.id);
```

## `scripts/gen_test_seed.py` — generator

Seed-i yenidən istehsal edir (deterministik: `random.seed(20261124)`).
Asılılıq: `bcrypt` (`pip install bcrypt`). Çıxış birbaşa bu qovluğa yazılır.
