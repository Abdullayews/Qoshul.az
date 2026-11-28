-- ═══════════════════════════════════════════════════════════════════
-- 0015_profile_null_clearing.sql — D-184 (Loop 3, 2026-11-28)
--
-- Problem: fn_save_profile (0007) bütün profil sahələrini coalesce(p_x,
-- saxlanılan) ilə yazır — klient NULL ötürsə dəyər DƏYİŞMİR. Profil
-- redaktə ekranında (D-184/6.4) „Heç nə“ (qrup ölçüsü) və boş büdcə
-- seçimləri DB-də NULL yazmalıdır — istifadəçi seçimini geri götürə
-- bilməlidir.
--
-- Həll: p_group_size / p_budget üçün üçvəziyyətli semantika:
--   NULL (ötürülməyib)  → toxunma (əvvəlki dəyər qalır) — əvvəlki
--                          davranış, bütün mövcud çağırışlar təhlükəsizdir
--   '' (boş string)     → NULL YAZ (dəyəri təmizlə)
--   'mətn'              → dəyəri yaz
--
-- Yalnız bu iki sahə dəyişir — digər sahələrdə köhnə semantika saxlanılır
-- (onboarding axını boş dəyər ötürərək mövcud məlumatı təmizləməməlidir).
-- ═══════════════════════════════════════════════════════════════════

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
set search_path = 'public'
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
        -- D-184: üçvəziyyətli clearing — '' → NULL yaz (seçim geri alınıb);
        -- NULL → toxunma. trim: yalnız boşluq göndərilsə də təmizlə.
        group_size    = case
                            when p_group_size is null then group_size
                            when btrim(p_group_size) = '' then null
                            else p_group_size
                        end,
        budget        = case
                            when p_budget is null then budget
                            when btrim(p_budget) = '' then null
                            else p_budget
                        end,
        -- onboarded yalnız İRƏLİYİR (true) — heç vaxt geri düşmür
        onboarded     = onboarded or coalesce(p_onboarded, false),
        profile_completed = profile_completed or coalesce(p_profile_completed, false),
        updated_at    = now()
    where id = me;

    -- Maraqlar — atomik əvəzetmə (p_interests null-dursa toxunmur)
    if p_interests is not null then
        delete from public.user_interests where profile_id = me;
        insert into public.user_interests (profile_id, category_slug)
        select me, slug
        from jsonb_array_elements_text(p_interests) as t(slug)
        on conflict do nothing;
    end if;

    return true;
end;
$function$;

-- Revokasiya yenidən tətbiq olunmur — 0007-dəki GRANT bərpa olunur
-- (create or replace funksiya imzayı dəyişmirsə grantlar QALIR; imza
-- eynidir — parametr siyahısı dəyişməyib, yalnız gövdə dəyişib).
