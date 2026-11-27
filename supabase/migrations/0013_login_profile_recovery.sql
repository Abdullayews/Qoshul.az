-- ═══════════════════════════════════════════════════════════════════════
-- 0013_login_profile_recovery.sql (D-180)
--
-- İstifadəçi direktivi (2026-11-27): „google ilə giriş edən useri
-- qeydiyyatı yoxdursa hesab yarat“ + „E2E Testov profilini sil“.
--
-- PROBLEM 1: on_auth_user_created triggeri (0002) yalnız INSERT-də işə
-- düşür. Google IDToken axınında GoTrue İLK girişdə user yaradır (INSERT
-- → profil yaranır), amma 0008-dən ƏVVƏL yaradılmış köhnə userlərdə /
-- triggerin sıradan çıxdığı vəziyyətlərdə profiles sətri YOXDUR →
-- login sonrası readMyProfile boş qaytarır, onboarding qərarı etibarsızdır,
-- fn_save_profile `insert … on conflict do nothing` ilə boş sətir yaradır.
--
-- PROBLEM 2: profil yaradılanda Google adı yazılır, amma İSTİFADƏÇİ
-- Google hesabında adını dəyişəndən sonra yenidə giriş etsə profil
-- köhnə adda qalır (raw_user_meta_data yenilənir, profiles yox).
--
-- HƏLL:
--   1. fn_ensure_profile INSERT və UPDATE-də işə düşür (trigger əvəzi).
--   2. Profil YOXDURSA → yaradır (adı metadata-dan); VARSA və ad
--      İSTİFADƏÇİ tərəfindən doldurulmayıbsa (boşdur) → metadata adı
--      yazır. İstifadəçi onboarding-də ad yazıbsa (boş deyil) TOXUNMUR
--      (klient tərəfli ad üstünlük təşkil edir — server üstünə yazmır).
--   3. notification_settings seed-i hər login-də idempotent tamamlanır
--      (on conflict do nothing).
--
-- Ayrıca: e2e_test.sh üçün təmizlik düzəlişi (test qalıqlarının
-- DELETE əmri yalnız uğurlu addımlardan SONRA işə düşməsi üçün trap).
-- ═══════════════════════════════════════════════════════════════════════

-- ── 1. fn_ensure_profile — INSERT+UPDATE, profil-bərpa + ad sinxronu ──
create or replace function public.fn_ensure_profile() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  v_meta        jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
  v_full_name   text  := coalesce(
                           v_meta ->> 'full_name',
                           v_meta ->> 'name'
                         );
  v_first       text;
  v_last        text;
  v_meta_full   text;
begin
  -- adın hissələri: "Ad Soyad" → first/last (tək söz → yalnız ad)
  if v_full_name is not null and btrim(v_full_name) <> '' then
    v_first := split_part(btrim(v_full_name), ' ', 1);
    v_last  := nullif(
                 btrim(substr(btrim(v_full_name), length(v_first) + 1)), '');
  end if;
  v_meta_full := coalesce(v_first, '') || ' ' || coalesce(v_last, '');

  -- (a) profil sətri yoxdursa YARAT (login-time hesab bərpası)
  insert into public.profiles (id, first_name, last_name)
  values (
    new.id,
    coalesce(v_first, ''),
    coalesce(v_last, '')
  )
  on conflict (id) do nothing;

  -- (b) profil VARDIRSA və ad hələ istifadəçi tərəfindən doldurulmayıbsa
  --     (first/last boşdur / default qalığıdır) → metadata adını yaz.
  --     Doldurulmuş ada TOXUNMUR (klient win).
  if v_first is not null or v_last is not null then
    update public.profiles
      set first_name = case when btrim(first_name) = '' then coalesce(v_first, first_name) else first_name end,
          last_name  = case when btrim(last_name)  = '' then coalesce(v_last,  last_name)  else last_name  end,
          updated_at = now()
      where id = new.id
        and (btrim(first_name) = '' or btrim(last_name) = '');
  end if;

  -- (c) bildiriş tercihləri — idempotent seed (hər login-də tamamlanır)
  insert into public.notification_settings (profile_id, notif_type)
  select new.id, t from unnest(array[
    'MATCH','MESSAGE','EVENT_JOIN','EVENT_APPROVAL','EVENT_REMINDER',
    'RESV_UPDATE','RESV_REMINDER','LEVEL_UP','COUPON_EXPIRING',
    'NEARBY_EVENT','MARKETING']) as t
  on conflict do nothing;
  return new;
end;
$$;

-- ── 2. Trigger — INSERT və UPDATE hər ikisində ──
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert or update of raw_user_meta_data, email on auth.users
  for each row execute function public.fn_ensure_profile();

-- ── 3. Mövcud zədələnmiş userlər üçün birdəfəlik bərpa ──
-- auth.users sətirlərinə no-op toxunuş — AFTER UPDATE OF email triggeri
-- hər user üçün fn_ensure_profile-i işə salır (profil yoxdursa yaradır,
-- ad boşdursa metadata-dan doldurur, notification_settings seed edir).
-- (Əvvəlki variant: DO blok içində cursor + UPDATE — plpgsql alias xətası
-- verirdi; no-op UPDATE həm sadə, həm də eyni kod yolunu sınayır.)
update auth.users set email = email;

-- ── 4. Xəritə pinləri üçün yuvarlaqlaşdırılmış koordinat (D-180) ──
-- PROBLEM: profile_cards görünüşünə dəqiq koordinat daxil deyil (ROL I),
-- app isə hamı üçün sabit Bakı fallback (40.37, 49.83) göstərirdi —
-- „null-island“ filtri (0,0) heç vaxt işləmirdi, bütün pinlər eyni
-- nöqtədə üst-üstə düşürdü.
-- HƏLL: profile_cards-ə 2 onluq dəqiqlikdə (~1.1 km grid) koordinat —
-- privacy qorunur (ROL I: dəqiq nöqtə YOX, şəhər içi paylanma BƏLİ),
-- xəritə pinləri real mövqelərə düşür. map_visibility='OFF' → null.
create or replace view public.profile_cards
  with (security_invoker = true) as
select
  p.id, p.first_name, p.last_name, p.city, p.status, p.university,
  p.work_field, p.languages, p.goals, p.personality, p.availability,
  p.group_size, p.budget, p.bio, p.photo_count, p.map_visibility,
  p.show_distance, p.xp, p.role, p.onboarded, p.created_at,
  case
    when p.map_visibility = 'OFF' or p.last_location is null then null
    else round(st_y(p.last_location::geometry)::numeric, 2)
  end as map_lat,
  case
    when p.map_visibility = 'OFF' or p.last_location is null then null
    else round(st_x(p.last_location::geometry)::numeric, 2)
  end as map_lng
from public.profiles p;

-- ── 5. Grant (0002 pariteti — funksiya yenidən yaradıldı) ──
revoke all on function public.fn_ensure_profile() from public, anon;
-- trigger funksiyası birbaşa çağrılmır (yalnız trigger); execution
-- supabase_auth_admin-ə (trigger sahibi konteksti) qalır:
grant execute on function public.fn_ensure_profile() to supabase_auth_admin;
