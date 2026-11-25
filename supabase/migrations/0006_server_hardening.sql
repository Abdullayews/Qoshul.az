-- 0006_server_hardening.sql — D-151 (2026-11-25)
-- Məqsəd: server-only rejim üçün təhlükəsizlik düzəlişləri + oxu yolu dəstəyi.
-- Audit tapıntıları (C1, C3, H3) + app oxu yolu üçün zəruri dəyişikliklər.

-- ═══ 1. fn_award_xp özünə-xidmət deşiyi bağlanır (C1) ═══
-- Əvvəl: hər hansı authenticated istənilən profile_id-yə istənilən XP yaza bilirdi.
-- Yeni: yalnız öz profilinə (p_profile_id = auth.uid()) və ya service_role.
create or replace function public.fn_award_xp(
    p_profile_id uuid,
    p_key text,
    p_amount integer,
    p_note text default null
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
    -- C1: self-award yalnız ÖZ profilinə; service_role (jwt claim) isə sərbəst
    if p_profile_id <> me and coalesce(
        current_setting('request.jwt.claims', true)::jsonb ->> 'role', '') <> 'service_role'
    then
        raise exception 'FORBIDDEN: yalnız öz profilinə XP yaza bilərsən';
    end if;

    insert into xp_events (profile_id, key, amount, note)
    values (p_profile_id, p_key, p_amount, coalesce(p_note, p_key));
    update public.profiles set xp = xp + p_amount where id = p_profile_id;
    return true;
exception when unique_violation then
    return false;   -- idempotensiya: eyni açar ikinci dəfə sayılmır
end;
$function$;

-- ═══ 2. profiles_update_self həssas sütunları qorunur (C3/H2) ═══
-- xp, restricted_until, no_show_count, phone_verified, login_streak, role
-- birbaşa UPDATE ilə dəyişə bilməz — yalnız RPC-lər (fn_award_xp və s.)
-- security definer kimi yaza bilər.
drop policy if exists profiles_update_self on public.profiles;
create policy profiles_update_self on public.profiles
    for update to authenticated
    using (id = auth.uid())
    with check (
        id = auth.uid()
        -- dəyişməz server-idarə sütunları (C3): köhnə dəyərlə eyni qalmalı
        and xp = (select p.xp from public.profiles p where p.id = auth.uid())
        and restricted_until is not distinct from (
            select p.restricted_until from public.profiles p where p.id = auth.uid())
        and no_show_count = (
            select p.no_show_count from public.profiles p where p.id = auth.uid())
        and phone_verified is not distinct from (
            select p.phone_verified from public.profiles p where p.id = auth.uid())
        and login_streak = (
            select p.login_streak from public.profiles p where p.id = auth.uid())
        and role = (select p.role from public.profiles p where p.id = auth.uid())
    );

-- ═══ 3. execute hüquqları sıradan çıxarılır (H3): default revoke + whitelist ═══
revoke execute on function public.fn_award_xp(uuid, text, integer, text) from public, anon;
revoke execute on function public.fn_grant_venue_xp(text, integer, text) from public, anon, authenticated;
revoke execute on function public.fn_moderation_queue() from public, anon, authenticated;
revoke execute on function public.fn_moderate_review(text, boolean) from public, anon, authenticated;
grant execute on function public.fn_award_xp(uuid, text, integer, text) to authenticated;
-- service_role-a hamısı (superuser özü də işlədir)

-- ═══ 4. Oxu yolu: venues/offers/coupons_offers anon SELECT (app ilk açılış) ═══
-- (events artıq authenticated-only; venues anon açıq idi — offer-lar da açılır)
drop policy if exists coupons_offers_select on public.coupons_offers;
create policy coupons_offers_select on public.coupons_offers
    for select to authenticated using (active = true);

-- ═══ 5. Notifications RLS: yalnız öz bildirişlərin ═══
drop policy if exists notifications_select_self on public.notifications;
create policy notifications_select_self on public.notifications
    for select to authenticated using (profile_id = auth.uid());

-- ═══ 6. venue_reviews: author_name oxu yolu üçün (APPROVED hamıya görünür) ═══
drop policy if exists venue_reviews_select on public.venue_reviews;
create policy venue_reviews_select on public.venue_reviews
    for select to authenticated using (status = 'APPROVED' or author_id = auth.uid());

-- ═══ 7. user_coupons: venue_name/label oxuşu asanlaşdır (app join-siz oxuyur) ═══
alter table public.user_coupons
    add column if not exists venue_name text default '',
    add column if not exists label text default '';

-- ═══ 8. reservations: venue_name oxuşu (app join-siz) ═══
alter table public.reservations
    add column if not exists venue_name text default '',
    add column if not exists venue_image text default 'friends';

-- ═══ 9. fn_create_event host_name əlavəsi (app listdə göstərir) ═══
create or replace function public.fn_create_event(
    p_title text, p_category text, p_city text, p_location text,
    p_event_date date, p_event_time text, p_capacity integer,
    p_description text default '', p_price_azn integer default null,
    p_age_limit text default null, p_language text default null,
    p_level text default null, p_format text default 'Oflayn',
    p_approval boolean default false, p_recurring text default 'Bir dəfəlik',
    p_lat double precision default null, p_lng double precision default null,
    p_venue_id text default null, p_image text default 'friends'
) returns text
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
    me uuid := auth.uid();
    new_id text;
begin
    if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
    if p_capacity < 2 or p_capacity > 100 then
        raise exception 'INVALID_INPUT: capacity 2-100';
    end if;
    new_id := 'evt' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 10);
    insert into events (
        id, title, category, city, location, location_geo,
        epoch_day, event_time, event_date, capacity, description,
        price_azn, age_limit, language, level, format, approval, recurring,
        image, host_profile_id, venue_id
    ) values (
        new_id, p_title, p_category, p_city, p_location,
        case when p_lat is not null and p_lng is not null
            then st_setsrid(st_makepoint(p_lng, p_lat), 4326)::geography end,
        (extract(epoch from p_event_date) / 86400)::integer,
        p_event_time, p_event_date, p_capacity, p_description,
        p_price_azn, p_age_limit, p_language, p_level, p_format, p_approval, p_recurring,
        p_image, me, p_venue_id
    );
    return new_id;
end;
$function$;
