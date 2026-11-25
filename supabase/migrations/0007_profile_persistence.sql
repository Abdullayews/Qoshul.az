-- 0007_profile_persistence.sql — D-152 (2026-11-25)
-- Məqsəd: istifadəçi axını tam işləsin — login sonrası refresh (klient),
-- onboarding/profil redaktə PERSISTENSİYASI (bu miqrasiya).
--
-- Diaqnoz (istifadəçi şikayəti: „Supabase düzgün qoşulmayıb“):
--   * server API işləyirdi (signup token qaytarır);
--   * klient login-dən sonra data oxumurdu (refresh hook yoxdur — klient
--     tərəfdə düzəldildi);
--   * onboarding profili (ad/şəhər/maraqlar/onboarded) HEÇ YERƏ yazılmırdı —
--     restart-dan sonra istifadəçi onboarding-ə geri düşürdü.
--
-- Həll: fn_save_profile — profiles cədvəlinə öz sətirinə SECURE upsert;
-- həssas sütunlara (xp, role, restricted_until ...) klient toxunmur.

-- ═══ 1. fn_save_profile — onboarding + profil redaktə (ROL B, D-152) ═══
-- Yalnız öz profilinə; həssas server-idarə sütunları MƏCBURİƏN saxlanılır
-- (funksiya onları heç yazmır). onboarded bir dəfə true olanda geri
-- DÜŞMÜR (bərpa/roll-back qoruması). Maraqlar user_interests-də atomik
-- əvəz olunur (delete+insert bir tranzaksiyada).
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

-- ═══ 2. fn_set_avatar — profil şəkli açarı (D-112 redaktə ekranı) ═══
create or replace function public.fn_set_avatar(
    p_image_key text
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
    update public.profiles
    set photo_urls = to_jsonb(array[p_image_key::text]),
        photo_count = 1,
        updated_at = now()
    where id = me;
    return true;
end;
$function$;

-- ═══ 3. execute hüquqları: yalnız authenticated ═══
revoke execute on function public.fn_save_profile from public, anon;
grant execute on function public.fn_save_profile to authenticated;
revoke execute on function public.fn_set_avatar from public, anon;
grant execute on function public.fn_set_avatar to authenticated;

-- ═══ 4. oxu yolu: öz maraqlarımız (SupabaseReader üçün RLS) ═══
drop policy if exists user_interests_select on public.user_interests;
create policy user_interests_select on public.user_interests
    for select to authenticated using (profile_id = auth.uid());

-- ═══ 5. fn_send_message düzəlişi (D-152): ambiguous window_end ═══
-- Postgres 17: PL/pgSQL variable `window_end` RETURNS TABLE sütunu ilə
-- toqquşurdu → 42702 ambiguous column (şəxsi mesajlarda sınırdı).
-- Rust service (0005) eyni bug-u daşıyır — orada ayrıca düzələcək.
create or replace function public.fn_send_message(
    p_kind text default null,
    p_event_id text default null,
    p_recipient_id uuid default null,
    p_body text default null
) returns table(ok boolean, error text, message_id text)
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
    me uuid := auth.uid(); m_id text; bucket text; hits int;
    rl_window_end timestamptz; dup boolean;
begin
    if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
    if p_body is null or btrim(p_body) = '' then raise exception 'INVALID_INPUT'; end if;
    if char_length(p_body) > 2000 then raise exception 'INVALID_INPUT'; end if;

    if p_kind = 'EVENT' then
        if not exists (select 1 from public.event_participants
                       where event_id = p_event_id and profile_id = me and status = 'JOINED') then
            return query select false, 'NOT_PARTICIPANT'::text, null; return;
        end if;
    elsif p_kind = 'PERSONAL' then
        if p_recipient_id is null then raise exception 'INVALID_INPUT'; end if;
        if exists (select 1 from public.blocks b
                   where (b.blocker_id = p_recipient_id and b.blocked_id = me)
                      or (b.blocker_id = me and b.blocked_id = p_recipient_id)) then
            return query select false, 'BLOCKED'::text, null; return;
        end if;
    else
        raise exception 'INVALID_INPUT';
    end if;

    -- N6: saatlıq pəncərə, 20 mesaj/saat
    bucket := 'msg:' || me::text || ':' || to_char(date_trunc('hour', now()), 'YYYYMMDDHH24');
    select rl.hit_count, rl.window_end into hits, rl_window_end
      from public.rate_limit_buckets rl where rl.bucket_key = bucket for update;
    if hits is null then
        insert into public.rate_limit_buckets (bucket_key, window_start, window_end, hit_count)
        values (bucket, date_trunc('hour', now()), date_trunc('hour', now()) + interval '1 hour', 0)
        on conflict (bucket_key) do nothing;
        hits := 0;
    end if;
    if hits >= 20 then
        return query select false, 'RATE_LIMITED'::text, null; return;
    end if;

    -- spam: eyni mətn 60 saniyə ərzində təkrar
    dup := exists (select 1 from public.messages m
                   where m.sender_id = me and m.body = p_body
                     and m.created_at > now() - interval '60 seconds');
    if dup then
        return query select false, 'DUPLICATE'::text, null; return;
    end if;

    update public.rate_limit_buckets set hit_count = hit_count + 1, updated_at = now()
      where bucket_key = bucket;

    m_id := public.ulid();
    insert into public.messages (id, kind, event_id, sender_id, recipient_id, body)
    values (m_id, p_kind, p_event_id, me,
            case when p_kind = 'PERSONAL' then p_recipient_id else null end, p_body);

    if p_kind = 'PERSONAL' then
        if exists (select 1 from public.notification_settings ns
                   where ns.profile_id = p_recipient_id and ns.notif_type = 'MESSAGE' and ns.enabled) then
            insert into public.notifications (profile_id, notif_type, title, body)
            values (p_recipient_id, 'MESSAGE', 'Yeni mesaj', 'Sənə yeni mesaj gəldi.');
        end if;
    end if;
    return query select true, 'OK'::text, m_id;
end;
$function$;

-- PostgREST schema cache yenilənsin (null-parametrli resolution üçün)
notify pgrst, 'reload schema';

-- ═══ 6. gen_random_bytes public wrap (D-152 — ƏSAS FIX) ═══
-- PostgREST connection-larında extensions şeması search_path-ə düşmürdü →
-- ulid() istifadə edən BÜTÜN RPC-lər (fn_daily_login, fn_send_message,
-- fn_award_xp...) 42883 "function gen_random_bytes does not exist" ilə
-- sınırdı. Bu, „Supabase qoşulmayıb“ şikayətinin SERVER tərəfli səbəbi idi.
-- public.gen_random_bytes → extensions.gen_random_bytes wrap.
create or replace function public.gen_random_bytes(n int) returns bytea
language sql strict volatile as $fn$ select extensions.gen_random_bytes(n) $fn$;
grant execute on function public.gen_random_bytes(int) to public;

-- (env düzəlişi də edildi: PGRST_DB_EXTRA_SEARCH_PATH=public,extensions)
