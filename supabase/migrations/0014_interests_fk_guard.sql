-- ═══════════════════════════════════════════════════════════════════════
-- 0014_interests_fk_guard.sql (D-181)
--
-- İstifadəçi xətası (2026-11-27): „Sinxronizasiya xətası — dəyişikliklər
-- geri alındı (insert or update on table "user_interests" violates
-- foreign key constraint …)".
--
-- KÖK SƏBƏB: onboarding maraq seçimi QRUPLARI göstərilir („İdman və
-- Aktivlik“ və s.); app qrup adını slug-a çevirib göndərirdi
-- („idman-və-aktivlik“) — bu slug categories.cədvəlində YOXDUR →
-- user_interests_category_slug_fkey POZULUR → bütün SaveProfileIntent
-- rollback olunur (ad, şəhər, onboarded də itir).
--
-- HƏLL (iki qat):
--   1. APP (Taxonomy.slugsForSelection): qrup → alt kateqoriya slüqları
--      (yalnız DB-də mövcud slüqlar göndərilir). Bu migration ilə gəlir.
--   2. SERVER (bu fayl): fn_save_profile naməlum slüqləri SƏSSİCƏ
--      atır (insert ... select where exists categories) — köhnə APK
--      versiyalarından gələn zərərli slüqlər artıq bütün əməliyyatı
--      partlatmır; qalan sahələr (ad, şəhər, onboarded) YAZILIR.
--
-- Paritet: app tərəfdə slugsForSelection + EngineSyncTest; DB tərəfdə
-- bu migrationın özü. İki qat da olsa, app tərəf təkbaşına kifayətdir —
-- server qatı köhnə APK-lar üçün təhlükəsizlik torudur.
-- ═══════════════════════════════════════════════════════════════════════

create or replace function public.fn_save_profile(
    p_first_name     text default null,
    p_last_name      text default null,
    p_birth_date     date default null,
    p_gender         text default null,
    p_bio            text default null,
    p_photo_count    integer default null,
    p_city           text default null,
    p_status         text default null,
    p_university     text default null,
    p_work_field     text default null,
    p_languages      jsonb default null,
    p_goals          jsonb default null,
    p_personality    jsonb default null,
    p_availability   jsonb default null,
    p_group_size     text default null,
    p_budget         text default null,
    p_onboarded      boolean default null,
    p_profile_completed boolean default null,
    p_interests      jsonb default null
) returns boolean
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
    me uuid := auth.uid();
begin
    if me is null then
        raise exception 'NOT_AUTHENTICATED';
    end if;

    insert into public.profiles (id) values (me) on conflict (id) do nothing;

    update public.profiles set
        first_name    = coalesce(p_first_name, first_name),
        last_name     = coalesce(p_last_name, last_name),
        birth_date    = coalesce(p_birth_date, birth_date),
        gender        = coalesce(p_gender, gender),
        bio           = coalesce(p_bio, bio),
        photo_count   = coalesce(p_photo_count, photo_count),
        city          = coalesce(p_city, city),
        status        = coalesce(p_status, status),
        university    = coalesce(p_university, university),
        work_field    = coalesce(p_work_field, work_field),
        languages     = coalesce(p_languages, languages),
        goals         = coalesce(p_goals, goals),
        personality   = coalesce(p_personality, personality),
        availability  = coalesce(p_availability, availability),
        group_size    = coalesce(p_group_size, group_size),
        budget        = coalesce(p_budget, budget),
        -- onboarded yalnız IRƏLİYİR (true) — heç vaxt geri düşmür
        onboarded     = onboarded or coalesce(p_onboarded, false),
        profile_completed = profile_completed or coalesce(p_profile_completed, false),
        updated_at    = now()
    where id = me;

    -- Maraqlar — atomik əvəzetmə (p_interests null-dursa toxunmur).
    -- D-181 FK GUARD: yalnız categories-də MÖVCUD slüqlar yazılır —
    -- naməlum slüq (köhnə APK fallback-i, böyük/kiçik hərf fərqi,
    -- decompose „i̇“) SƏSSİCƏ atılır; əməliyyat rollback olunmur.
    if p_interests is not null then
        delete from public.user_interests where profile_id = me;
        insert into public.user_interests (profile_id, category_slug)
        select me, t.slug
        from jsonb_array_elements_text(p_interests) as t(slug)
        join public.categories c on c.slug = t.slug
        on conflict do nothing;
    end if;

    return true;
end;
$function$;

-- ── Təmizlik: tarixdə yaranmış zərərli slüqləri sil (FK-ya zərər yox,
-- amma user_interests-də „boş“ maraqların qalıqları) ──
delete from public.user_interests ui
where not exists (select 1 from public.categories c where c.slug = ui.category_slug);
