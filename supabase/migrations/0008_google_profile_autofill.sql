-- ═══════════════════════════════════════════════════════════════════════
-- 0008_google_profile_autofill.sql (D-167)
--
-- İstifadəçi direktivi: „hesab artıq varsa yenidən giriş etdikdə özü
-- haqqında məlumat istəməsi kimi şeyləri əngəllə“.
--
-- PROBLEM: Google ilə ilk girişdə GoTrue raw_user_meta_data-ya
-- full_name/name (Google profilindən) yazır, amma fn_ensure_profile
-- yalnız boş profiles sətri yaradır → first_name = 'Dost' default.
-- Nəticə: onboarding-də ad soruşulur (istifadəçi onsuz da GOOGLE adını
-- verib — ikinci dəfə soruşmaq əngəllənməlidir).
--
-- HƏLL: fn_ensure_profile artıq raw_user_meta_data-dan ad/soyad/
-- şəkil URL-u oxuyur. Google IDToken axını full_name göndərir;
-- supabase-kt signInWith(IDToken) data blokunda full_name qeyd
-- edir (mövzudursa). Ad varsa → onboarding-in ad addımı artıq
-- DOLDURULMUŞ gəlir (ProfileStep initial dəyərini profildən oxuyur).
--
-- Qeyd: 0002-dəki fn_ensure_profile-in tam əvəzidir (drop + create).
-- ═══════════════════════════════════════════════════════════════════════

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
begin
  -- adın hissələri: "Ad Soyad" → first/last (tək söz → yalnız ad)
  if v_full_name is not null and btrim(v_full_name) <> '' then
    v_first := split_part(btrim(v_full_name), ' ', 1);
    v_last  := nullif(
                 btrim(substr(btrim(v_full_name), length(v_first) + 1)), '');
  end if;

  insert into public.profiles (id, first_name, last_name)
  values (
    new.id,
    coalesce(v_first, 'Dost'),
    coalesce(v_last, '')
  )
  on conflict (id) do nothing;

  insert into public.notification_settings (profile_id, notif_type)
  select new.id, t from unnest(array[
    'MATCH','MESSAGE','EVENT_JOIN','EVENT_APPROVAL','EVENT_REMINDER',
    'RESV_UPDATE','RESV_REMINDER','LEVEL_UP','COUPON_EXPIRING',
    'NEARBY_EVENT','MARKETING']) as t
  on conflict do nothing;
  return new;
end;
$$;

-- Trigger 0002-də yaradılıb; funksiya əvəz olunduğu üçün trigger-in
-- yenidən qurulması lazım deyil (funksiya referansı canlıdır).
-- Mövcudluq yoxlaması (assert funksiyası yoxdur — DO bloku):
do $$
begin
  if not exists (
    select 1 from pg_trigger
    where tgname = 'on_auth_user_created'
      and tgrelid = 'auth.users'::regclass
      and not tgisinternal
  ) then
    raise exception 'on_auth_user_created triggeri yoxdur — 0002 pozulub';
  end if;
end
$$;
