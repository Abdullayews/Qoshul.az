-- ═══════════════════════════════════════════════════════════════════════
-- 0010_remove_static_defaults.sql (D-170, bölmə A.2)
--
-- İstifadəçi direktivi: bütün STATIC/DEFAULT məlumat silinsən — profil
-- adı "Dost", şəhər "Bakı", status "student" kimi default-lar YOX.
-- Məlumat yalnız qeydiyyatda istifadəçidən alınır (onboarding) və ya
-- serverdən gəlir. Köhnə miqrasiyalar REDAKTƏ EDİLMİR (0010 yeni).
--
-- Nə edir:
--   1. profiles: first_name/city/status default-ları silinir, sütunlar
--      nullable olur (onboarding tamamlanana qədər boş).
--   2. venues/events: city default 'Bakı' silinir (yalnız DEFAULT,
--      not null saxlanılır — bu cədvəllərdə şəhər məcburi sahədir və
--      seed/istifadəçi həmişə göndərir).
--   3. fn_ensure_profile: coalesce(.., 'Dost') silinir — Google adı
--      yoxdursa first_name BOŞ qalır (onboarding soruşur).
--   4. Mövcud 'Dost' sətirləri təmizlənir (canlıda 0-dur, təhlükəsiz).
-- ═══════════════════════════════════════════════════════════════════════

-- ── 1. profiles: default-ları sil, nullable et ─────────────────────────
alter table public.profiles
  alter column first_name drop default,
  alter column first_name drop not null,
  alter column city drop default,
  alter column city drop not null,
  alter column status drop default,
  alter column status drop not null;

comment on column public.profiles.first_name is
  'D-170: default YOXDUR — onboarding-də istifadəçidən alınır (boş = onboarding tamamlanmayıb)';
comment on column public.profiles.city is
  'D-170: default "Bakı" silindi — istifadəçi seçir';
comment on column public.profiles.status is
  'D-170: default "student" silindi — istifadəçi seçir';

-- ── 2. venues / events: city default silinir ───────────────────────────
alter table public.venues
  alter column city drop default;
alter table public.events
  alter column city drop default;

comment on column public.events.city is
  'D-170: default "Bakı" silindi — yaradıcı mütləq göndərir';
comment on column public.venues.city is
  'D-170: default "Bakı" silindi — seed/məlumat daxil edən göndərir';

-- ── 3. fn_ensure_profile: "Dost" fallback silinir ──────────────────────
-- (Google raw_user_meta_data-da ad varsa yazılır; yoxdursa BOŞ —
--  onboarding-in ProfileStep-i soruşur)
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

  -- D-170: coalesce(v_first, 'Dost') SİLİNDİ — ad yoxdursa BOŞ qalır,
  -- onboarding soruşur (A.1 direktivi: default məlumat yoxdur)
  insert into public.profiles (id, first_name, last_name)
  values (
    new.id,
    coalesce(v_first, ''),
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

-- ── 4. Mövcud "Dost" qalıqlarını təmizlə ───────────────────────────────
-- (canlıda 0-dur — 0001-dəki default-la yaranmış köhnə hesablar üçün)
update public.profiles set first_name = '' where first_name = 'Dost';
update public.profiles set city = null where city = 'Bakı'
  and first_name = '' and last_name = '';  -- yalnız tam boş profillər
