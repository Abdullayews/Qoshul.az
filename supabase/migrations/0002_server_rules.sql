-- ═══════════════════════════════════════════════════════════════════════════
-- Qoshul.az M1 — server qaydaları (0002): SECURITY DEFINER funksiyalar
-- D-077 (2026-11-07). Paritet mənbəyi: AppEngine.kt (M0, v0.16.0) —
-- hər qayda Kotlin versiyası ilə eyni semantikada port edilib.
--
-- Qayda 8/9 (product-spec): XP, kupon və rezerv məntiqi YALNIZ burada
-- hesablanır; bütün pul/XP təsirli əməliyyatlar idempotency açarı ilə icra
-- olunur (xp_events.unique(profile_id, key)).
--
-- Xəta kodları — A1 SpendError pariteti (typed, string açarların "səssiz
-- udulması" təkrarı mümkün deyil):
--   NO_ACCOUNT, INVALID_INPUT, BAD_CONFIG, INSUFFICIENT_BALANCE,
--   BILL_BELOW_MIN, CAP_EXCEEDED, OFFER_NOT_FOUND, AUTO_ONLY, LEVEL_LOCKED
--   + axın xüsusi: FULL, ALREADY_JOINED, PAST, TOO_FAR, NO_LOCATION,
--     RESTRICTED, RATE_LIMITED, DUPLICATE, BLOCKED, NOT_PARTICIPANT, OK
-- ═══════════════════════════════════════════════════════════════════════════

-- ── Profil yaradılması (auth.users → profiles) ────────────────────────────
create or replace function public.fn_ensure_profile() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id) values (new.id)
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

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.fn_ensure_profile();

-- ── Səviyyə indeksi (XpRules.rankIndexForXp pariteti) ─────────────────────
create or replace function public.fn_rank_index(xp int) returns int
language sql immutable as $$
  select case
    when xp <= 99 then 0 when xp <= 299 then 1 when xp <= 699 then 2
    when xp <= 1499 then 3 when xp <= 2999 then 4 else 5 end;
$$;

-- ── İdemponent XP mükafatı (award(key) — F.12 hadisə hamısı) ──────────────
create or replace function public.fn_award_xp(
  p_profile_id uuid, p_key text, p_amount int, p_note text default ''
) returns boolean
language plpgsql security definer set search_path = public as $$
begin
  insert into public.xp_events (profile_id, key, amount, note)
  values (p_profile_id, p_key, p_amount, p_note);
  update public.profiles set xp = xp + p_amount where id = p_profile_id;
  return true;
exception when unique_violation then
  return false;   -- idempotensiya: eyni açar ikinci dəfə sayılmır
end;
$$;

-- ═══════════════════ B2/D-073: günlük giriş seriyası ═════════════════════
-- AppEngine.dailyLogin pariteti (fayl:sətir — AppEngine.kt:351-368):
--   missed<=0 → ++; missed<=2 (FREEZE) → dəyər yerində; missed>=3 → 1-dən.
create or replace function public.fn_daily_login() returns table(
  ok boolean, streak int, awarded_login_xp boolean, awarded_bonus_xp boolean
)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  today_day bigint;
  last_day bigint;
  missed int;
  streak_v int;
  login_key text;
  bonus_key text;
  a_login boolean;
  a_bonus boolean := false;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  today_day := (extract(epoch from current_date) / 86400)::bigint;
  select last_login_day, login_streak into last_day, streak_v
    from public.profiles where id = me for update;
  if last_day is not null and today_day <= last_day then
    -- bu gün artıq sayılıb / saat geri — idempotent
    return; -- boş nəticə: çağıran "ok=false" kəşf edir
  end if;
  missed := case when last_day is null then 0 else (today_day - last_day - 1)::int end;
  streak_v := case
    when missed <= 0 then streak_v + 1                      -- ardıcıl gün
    when missed <= 2 then streak_v                          -- DONDURULMA (1-2 gün)
    else 1 end;                                             -- 3+ gün → SIFIR
  login_key := 'login-' || to_char(current_date, 'YYYY-MM-DD');
  a_login := public.fn_award_xp(me, login_key, 5, 'Günlük giriş');
  if a_login and streak_v = 7 then
    bonus_key := 'streak-7-' || to_char(current_date, 'YYYY-MM-DD');
    a_bonus := public.fn_award_xp(me, bonus_key, 30, '7 günlük seriya bonusu');
    streak_v := 1;                                          -- dövrə yenidən başlayır
  end if;
  update public.profiles
    set login_streak = streak_v, last_login_day = today_day
    where id = me;
  return query select true, streak_v, a_login, a_bonus;
end;
$$;

-- ═══════════════════ ROL D: swipe → qarşılıqlı match ═════════════════════
-- D-074: hərəkət LIMITSİZ; XP mükafatı gündə max 3 (MATCH_XP_DAILY_CAP).
create or replace function public.fn_record_swipe(
  p_target_id uuid, p_interested boolean
) returns table(matched boolean, awarded_xp boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  mutual boolean;
  today date := current_date;
  today_xp_count int;
  awarded boolean := false;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if p_target_id = me then raise exception 'INVALID_TARGET'; end if;

  insert into public.swipes (swiper_id, target_id, action, day)
  values (me, p_target_id, case when p_interested then 'LIKE' else 'PASS' end, today)
  on conflict (swiper_id, target_id, day) do nothing;

  if not p_interested then
    return query select false, false; return;
  end if;
  -- (aşağıda: qarşılıqlı maraq yoxdursa da false/false qayıdır)

  -- qarşılıqlı maraq: hədəf əvvəl LIKE edibmi?
  mutual := exists (
    select 1 from public.swipes s
    where s.swiper_id = p_target_id and s.target_id = me and s.action = 'LIKE');

  if not mutual then
    return query select false, false; return;
  end if;

  -- match cütü (kanonik a<b)
  insert into public.matches (a_id, b_id)
  values (least(me, p_target_id), greatest(me, p_target_id))
  on conflict do nothing;

  -- XP: gündə max 3 (swipes.xp_awarded bu gün)
  select count(*) into today_xp_count
    from public.swipes
    where swiper_id = me and day = today and xp_awarded;
  if today_xp_count < 3 then
    awarded := public.fn_award_xp(me, 'match-' || p_target_id::text, 10, 'Yeni match');
    if awarded then
      update public.swipes set xp_awarded = true
        where swiper_id = me and target_id = p_target_id and day = today;
    end if;
  end if;

  -- hər iki tərəfə bildiriş
  insert into public.notifications (profile_id, notif_type, title, body)
  select x, 'MATCH', 'Yeni match',
         'Qarşılıqlı maraq yarandı — söhbəti başlat!'
  from unnest(array[me, p_target_id]) x
  where not exists (select 1 from public.notification_settings ns
                    where ns.profile_id = x and ns.notif_type = 'MATCH' and not ns.enabled);

  return query select true, awarded;
end;
$$;

-- ═══════════════════ L.4: xərcləmə önizləməsi (A1 typed) ════════════════
create or replace function public.fn_preview_spend(
  p_venue_id text, p_xp_amount int, p_bill_azn numeric
) returns table(ok boolean, error text, discount numeric, cap numeric)
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  bal int; per_azn int; min_bill int; max_pct int;
  disc numeric; cap_v numeric;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select a.balance, v.xp_per_azn, v.min_bill_azn, v.max_discount_percent
    into bal, per_azn, min_bill, max_pct
    from public.venue_xp_accounts a
    join public.venues v on v.id = a.venue_id
    where a.profile_id = me and a.venue_id = p_venue_id;

  if bal is null then
    return query select false, 'NO_ACCOUNT'::text, 0::numeric, null::numeric; return;
  end if;
  if p_xp_amount <= 0 or p_bill_azn <= 0 then
    return query select false, 'INVALID_INPUT'::text, 0::numeric, null::numeric; return;
  end if;
  if per_azn <= 0 then
    return query select false, 'BAD_CONFIG'::text, 0::numeric, null::numeric; return;
  end if;
  if bal < p_xp_amount then
    return query select false, 'INSUFFICIENT_BALANCE'::text, 0::numeric, null::numeric; return;
  end if;
  if min_bill > 0 and p_bill_azn < min_bill then
    return query select false, 'BILL_BELOW_MIN'::text, 0::numeric, null::numeric; return;
  end if;
  disc := p_xp_amount::numeric / per_azn;
  cap_v := p_bill_azn * max_pct / 100;
  if disc > cap_v + 0.001 then
    return query select false, 'CAP_EXCEEDED'::text, 0::numeric, cap_v; return;
  end if;
  return query select true, 'OK'::text, disc, cap_v;
end;
$$;

-- ═══════════════════ L.4-5: atomik xərcləmə (B6: mənfi qadağan) ══════════
create or replace function public.fn_spend_venue_xp(
  p_venue_id text, p_xp_amount int, p_bill_azn numeric,
  p_note text default 'Kassada XP endirimi',
  p_idempotency_key text default null
) returns table(ok boolean, error text, discount numeric, new_balance int)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  acct_id text; new_bal int;
  p record;
  key text := coalesce(p_idempotency_key, 'spend-' || public.ulid());
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  p := public.fn_preview_spend(p_venue_id, p_xp_amount, p_bill_azn);
  if not p.ok then
    return query select false, p.error, 0::numeric, 0; return;
  end if;

  -- atomik: balance >= amount şərti ilə tək UPDATE (double-spend qadağan)
  update public.venue_xp_accounts
    set balance = balance - p_xp_amount
    where profile_id = me and venue_id = p_venue_id and balance >= p_xp_amount
    returning id, balance into acct_id, new_bal;
  if acct_id is null then
    return query select false, 'INSUFFICIENT_BALANCE'::text, 0::numeric, 0; return;
  end if;

  insert into public.venue_xp_ledger
    (account_id, is_grant, amount, balance_after, note, idempotency_key)
  values (acct_id, false, p_xp_amount, new_bal, p_note, key);

  return query select true, 'OK'::text, p.discount, new_bal;
end;
$$;

-- L.2: obyekt bazasına grant (təşkilatçı bitirmə axını — M4 job deyil, host özü)
create or replace function public.fn_grant_venue_xp(
  p_venue_id text, p_amount int, p_note text
) returns table(ok boolean, new_balance int)
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); acct_id text; new_bal int;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  update public.venue_xp_accounts
    set balance = balance + p_amount
    where profile_id = me and venue_id = p_venue_id
    returning id, balance into acct_id, new_bal;
  if acct_id is null then
    return query select false, 0; return;
  end if;
  insert into public.venue_xp_ledger
    (account_id, is_grant, amount, balance_after, note, idempotency_key)
  values (acct_id, true, p_amount, new_bal, p_note, 'grant-' || public.ulid());
  return query select true, new_bal;
end;
$$;

-- ═══════════════════ ROL G: kupon alışı (D-072 REDD + balans) ════════════
create or replace function public.fn_buy_coupon(
  p_offer_id text
) returns table(ok boolean, error text, coupon_id text, coupon_code text, coupon_expires_at timestamptz)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  o record; a record; rank_v int; code_v text; new_bal int;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into o from public.coupons_offers
    where id = p_offer_id and active;
  if o.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, null, null, null; return;
  end if;
  if o.auto_by_level or o.xp_price <= 0 then
    return query select false, 'AUTO_ONLY'::text, null, null, null; return;
  end if;
  select xp into rank_v from public.profiles where id = me;
  if public.fn_rank_index(rank_v) < o.min_rank_index then
    return query select false, 'LEVEL_LOCKED'::text, null, null, null; return;
  end if;
  select a.id, a.balance into a
    from public.venue_xp_accounts a where a.profile_id = me and a.venue_id = o.venue_id;
  if a.id is null then
    return query select false, 'NO_ACCOUNT'::text, null, null, null; return;
  end if;
  if a.balance < o.xp_price then
    -- D-072: yalnız hesabda VAR OLANDAN; kupon diskret alışdır → REDD
    return query select false, 'INSUFFICIENT_BALANCE'::text, null, null, null; return;
  end if;

  update public.venue_xp_accounts
    set balance = balance - o.xp_price
    where id = a.id and balance >= o.xp_price
    returning balance into new_bal;
  if new_bal is null then
    return query select false, 'INSUFFICIENT_BALANCE'::text, null, null, null; return;
  end if;

  insert into public.venue_xp_ledger
    (account_id, is_grant, amount, balance_after, note, idempotency_key)
  values (a.id, false, o.xp_price, new_bal, 'Kupon alındı: ' || o.label,
          'coupon-' || public.ulid());

  code_v := 'QSHUL-' || substr(public.ulid(), 1, 12);
  insert into public.user_coupons (profile_id, offer_id, venue_id, code, expires_at)
  values (me, o.id, o.venue_id, code_v, now() + make_interval(days => o.validity_days))
  returning id, code, expires_at into coupon_id, coupon_code, coupon_expires_at;

  insert into public.notifications (profile_id, notif_type, title, body)
  values (me, 'COUPON_EXPIRING', 'Kupon alındı: ' || o.label,
          o.label || ' · ' || o.validity_days || ' gün etibarlıdır. Vaxtı bitərsə XP geri qaytarılmır.');

  return query select true, 'OK'::text, coupon_id, coupon_code, coupon_expires_at;
end;
$$;

-- Kupon statusu (24s yoxlama işi M4-də EXPIRED edir; burada istifadə olunanda)
create or replace function public.fn_expire_coupons() returns int
language sql security definer set search_path = public as $$
  with updated as (
    update public.user_coupons
      set status = 'EXPIRED'
      where status = 'ACTIVE' and now() > expires_at
      returning 1)
  select count(*) from updated;
$$;

-- ═══════════════════ L.4-1: XP kartı nonce (B5: CSPRNG) ═════════════════
create or replace function public.fn_issue_xp_card_nonce(
  p_venue_id text
) returns table(card_nonce text, card_code text, card_expires_at timestamptz)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  raw int; code_v text; nonce_v text; chk int;
  exp timestamptz := now() + interval '60 seconds';
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  -- CSPRNG: gen_random_bytes (pgcrypto) — B5: köhnə nano/1000 mock deyil
  raw := (get_byte(gen_random_bytes(4), 0) << 24 |
          get_byte(gen_random_bytes(4), 1) << 16 |
          get_byte(gen_random_bytes(4), 2) << 8 |
          get_byte(gen_random_bytes(4), 3)) % 1000000;
  raw := (raw + 1000000) % 1000000;
  code_v := lpad(raw::text, 6, '0');            -- L.5: 6 rəqəm fallback
  chk := raw % 97;                              -- mock nonce formatı pariteti
  nonce_v := 'QSHUL-XP-' || code_v || '-' || chk;
  exp := now() + interval '60 seconds';
  insert into public.xp_card_nonces (nonce, profile_id, venue_id, fallback_code, expires_at)
  values (nonce_v, me, p_venue_id, code_v, exp)
  on conflict (nonce) do update
    set expires_at = excluded.expires_at, used_at = null;
  return query select nonce_v, code_v, exp;
end;
$$;

create or replace function public.fn_consume_xp_card_nonce(p_nonce text)
returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); consumed boolean;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  update public.xp_card_nonces
    set used_at = now()
    where nonce = p_nonce and profile_id = me and used_at is null
      and now() < expires_at
    returning true into consumed;
  return coalesce(consumed, false);   -- təkistifadə (L.4)
end;
$$;

-- ═══════════════════ ROL TAB 3: tədbir axını ════════════════════════════
create or replace function public.fn_create_event(
  p_title text, p_category text, p_city text, p_location text,
  p_event_date date, p_event_time text, p_capacity int, p_description text,
  p_price_azn int default null, p_age_limit text default 'Hamı üçün',
  p_language text default 'Azərbaycan dili', p_level text default 'Hamı üçün',
  p_format text default 'Oflayn', p_approval boolean default false,
  p_recurring text default 'Bir dəfəlik', p_tags jsonb default '[]',
  p_lat double precision default null, p_lng double precision default null,
  p_venue_id text default null, p_image text default 'friends'
) returns text
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); ev_id text; geo geography;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if p_capacity <= 0 then raise exception 'INVALID_INPUT'; end if;
  -- N1 həlli: ID serverdə yaranır (ULID) — "c${size+1}-${ms}" kolliziyası yox
  ev_id := public.ulid();
  geo := case when p_lat is not null and p_lng is not null
    then st_setsrid(st_makepoint(p_lng, p_lat), 4326)::geography
    else null end;
  insert into public.events (
    id, title, category, city, location, location_geo, epoch_day, event_time,
    event_date, capacity, description, price_azn, age_limit, language, level,
    format, approval, recurring, tags, image, host_profile_id, venue_id)
  values (
    ev_id, p_title, p_category, p_city,
    case when p_format = 'Onlayn' then 'Onlayn görüş' else p_location end,
    geo, (extract(epoch from p_event_date) / 86400)::bigint, p_event_time,
    p_event_date, p_capacity, p_description, p_price_azn, p_age_limit, p_language,
    p_level, p_format, p_approval, p_recurring, p_tags, p_image, me, p_venue_id);
  -- yaratdı = avtomatik qoşuldu (Kotlin createEvent: joinedV.add)
  insert into public.event_participants (event_id, profile_id, status)
  values (ev_id, me, 'JOINED');
  return ev_id;
end;
$$;

create or replace function public.fn_join_event(p_event_id text)
returns table(ok boolean, error text, awarded_xp boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); e record; joined_count int;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into e from public.events where id = p_event_id;
  if e.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, false; return; -- tapılmadı (event yoxdur)
  end if;
  if e.epoch_day < (extract(epoch from current_date) / 86400)::bigint then
    return query select false, 'PAST'::text, false; return;
  end if;
  if exists (select 1 from public.event_participants
             where event_id = p_event_id and profile_id = me
               and status in ('JOINED','PENDING')) then
    return query select false, 'ALREADY_JOINED'::text, false; return;
  end if;
  select count(*) into joined_count from public.event_participants
    where event_id = p_event_id and status = 'JOINED';
  if joined_count >= e.capacity then
    return query select false, 'FULL'::text, false; return;
  end if;
  if e.approval then
    insert into public.event_participants (event_id, profile_id, status)
    values (p_event_id, me, 'PENDING');
    insert into public.notifications (profile_id, notif_type, title, body)
    values (e.host_profile_id, 'EVENT_JOIN', 'Tədbirə qoşulma sorğusu',
            e.title || ' — sorğu gözləyir');
    return query select true, 'OK'::text, false; return;
  end if;
  insert into public.event_participants (event_id, profile_id, status)
  values (p_event_id, me, 'JOINED');
  return query select true, 'OK'::text,
    public.fn_award_xp(me, 'join-' || p_event_id, 10, 'Tədbirə qoşuldu');
end;
$$;

-- Host: PENDING iştirakçını təsdiq et → JOINED + join XP
create or replace function public.fn_approve_participant(p_event_id text, p_profile_id uuid)
returns boolean
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); host uuid;
begin
  select host_profile_id into host from public.events where id = p_event_id;
  if me is null or host is null or (host <> me and not public.is_staff()) then
    raise exception 'FORBIDDEN';
  end if;
  update public.event_participants
    set status = 'JOINED'
    where event_id = p_event_id and profile_id = p_profile_id and status = 'PENDING';
  if not found then return false; end if;
  perform public.fn_award_xp(p_profile_id, 'join-' || p_event_id, 10, 'Tədbirə qoşuldu');
  insert into public.notifications (profile_id, notif_type, title, body)
  values (p_profile_id, 'EVENT_APPROVAL', 'Qoşulma təsdiqi',
          'Sorğun təsdiqləndi — görəşərik!');
  return true;
end;
$$;

-- ═══════════════════ ROL F: check-in (B1: 200 m ST_DWithin) ═════════════
create or replace function public.fn_check_in_event(
  p_event_id text, p_lat double precision default null, p_lng double precision default null
) returns table(ok boolean, error text, distance_m numeric, hosting_awarded boolean, invite_awarded boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  e record; my_geo geography; dist numeric := null;
  participants int; hosting_v boolean := false; invite_v boolean := false;
  inv record;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into e from public.events where id = p_event_id;
  if e.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, null::numeric, false, false; return;
  end if;
  if not exists (select 1 from public.event_participants
                 where event_id = p_event_id and profile_id = me
                   and status = 'JOINED' and checked_in_at is null) then
    return query select false, 'ALREADY_JOINED'::text, null::numeric, false, false; return;
  end if;
  if e.location_geo is null then
    return query select false, 'NO_LOCATION'::text, null::numeric, false, false; return;
  end if;
  select last_location into my_geo from public.profiles where id = me;
  if my_geo is null and (p_lat is null or p_lng is null) then
    return query select false, 'NO_LOCATION'::text, null::numeric, false, false; return;
  end if;
  my_geo := coalesce(my_geo, st_setsrid(st_makepoint(p_lng, p_lat), 4326)::geography);

  -- B1: 200 m yaxınlıq (CHECK_IN_MAX_DISTANCE_M — artıq ölü sabit deyil)
  dist := st_distance(my_geo, e.location_geo);
  if not st_dwithin(my_geo, e.location_geo, 200) then
    return query select false, 'TOO_FAR'::text, dist, false, false; return;
  end if;

  update public.event_participants
    set checked_in_at = now(), checkin_xp_awarded = true
    where event_id = p_event_id and profile_id = me;
  perform public.fn_award_xp(me, 'checkin-' || p_event_id, 40, 'Check-in');

  -- ROL F: öz tədbirimdə ≥3 iştirakçı → hosting XP
  select count(*) into participants from public.event_participants
    where event_id = p_event_id and status = 'JOINED';
  if e.host_profile_id = me and participants >= 3 then
    hosting_v := public.fn_award_xp(me, 'hosting-' || p_event_id, 60, 'Təşkilatçı');
  end if;

  -- B3 düzəlişi: dəvət XP-i İLK CHECK-IN-dən sonra (köhnə: düyməyə basanda)
  select * into inv from public.invites
    where invitee_id = me and first_checkin_at is null limit 1;
  if inv.code is not null then
    update public.invites set first_checkin_at = now()
      where code = inv.code and first_checkin_at is null;
    invite_v := public.fn_award_xp(
      inv.inviter_id, 'invite-' || inv.code, 100, 'Dəvətli ilk check-in etdi');
    if invite_v then
      update public.invites set xp_awarded = true where code = inv.code;
      insert into public.notifications (profile_id, notif_type, title, body)
      values (inv.inviter_id, 'LEVEL_UP', '+100 XP qazandın',
              'Dəvətli qeydiyyatı bitirib ilk tədbirə gəldi.');
    end if;
  end if;

  return query select true, 'OK'::text, dist, hosting_v, invite_v;
end;
$$;

-- ═══════════════════ ROL E: rezerv axını ════════════════════════════════
create or replace function public.fn_create_reservation(
  p_venue_id text, p_resv_date date, p_resv_time text, p_party int,
  p_note text default '', p_coupon_id text default null, p_event_id text default null
) returns table(ok boolean, error text, id text, qr_code text, share_token text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  v record; c record; r_id text; qr text; tok text; status_v text;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if p_party <= 0 then raise exception 'INVALID_INPUT'; end if;
  if exists (select 1 from public.profiles
             where id = me and restricted_until is not null and restricted_until > now()) then
    return query select false, 'RESTRICTED'::text, null, null, null; return;
  end if;
  select * into v from public.venues where id = p_venue_id;
  if v.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, null, null, null; return;
  end if;
  if p_coupon_id is not null then
    select * into c from public.user_coupons
      where id = p_coupon_id and profile_id = me and venue_id = p_venue_id;
    if c.id is null or c.status <> 'ACTIVE' or now() > c.expires_at then
      return query select false, 'INVALID_INPUT'::text, null, null, null; return;
    end if;
  end if;
  status_v := case when v.auto_accept then 'CONFIRMED' else 'PENDING' end;
  r_id := public.ulid();
  qr := 'QSH-r-' || substr(public.ulid(), 1, 12);      -- N1: server-generated
  tok := 'shr-' || substr(public.ulid(), 1, 12);       -- N8: paylaşma tokeni
  insert into public.reservations (
    id, profile_id, venue_id, event_id, resv_date, resv_time, party, note,
    coupon_id, status, qr_code, share_token)
  values (r_id, me, p_venue_id, p_event_id, p_resv_date, p_resv_time, p_party,
          p_note, p_coupon_id, status_v, qr, tok);
  if c.id is not null then
    update public.user_coupons set status = 'USED', used_at = now() where id = c.id;
  end if;
  insert into public.notifications (profile_id, notif_type, title, body)
  values (me, 'RESV_UPDATE',
          case when v.auto_accept then 'Rezerv təsdiqləndi' else 'Rezerv gözləyir' end,
          v.name || ' · ' || p_resv_date || ' ' || p_resv_time || ' · ' || p_party ||
          ' nəfər' || case when v.auto_accept then '' else ' — partnyor təsdiqini gözləyir' end);
  return query select true, 'OK'::text, r_id, qr, tok;
end;
$$;

-- Partnyor skanı: PENDING → CONFIRMED (təsdiq) — yalnız obyekt meneceri
create or replace function public.fn_confirm_reservation(p_qr_code text)
returns table(ok boolean, error text, new_status text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); r record;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into r from public.reservations where qr_code = p_qr_code;
  if r.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, null; return;
  end if;
  if not exists (select 1 from public.venues v
                 where v.id = r.venue_id and (v.managed_by = me or public.is_staff())) then
    raise exception 'FORBIDDEN';
  end if;
  if r.status <> 'PENDING' then
    return query select false, 'INVALID_INPUT'::text, r.status; return;
  end if;
  update public.reservations set status = 'CONFIRMED' where id = r.id;
  insert into public.notifications (profile_id, notif_type, title, body)
  values (r.profile_id, 'RESV_UPDATE', 'Rezerv təsdiqləndi',
          'Partnyor rezervini təsdiqlədi.');
  return query select true, 'OK'::text, 'CONFIRMED'::text;
end;
$$;

-- Partnyor skanı: CONFIRMED → USED (+30 XP, kupon təkistifadə)
create or replace function public.fn_use_reservation(p_qr_code text)
returns table(ok boolean, error text, new_status text, awarded_xp boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); r record; awarded boolean;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into r from public.reservations where qr_code = p_qr_code;
  if r.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, null, false; return;
  end if;
  if not exists (select 1 from public.venues v
                 where v.id = r.venue_id and (v.managed_by = me or public.is_staff())) then
    raise exception 'FORBIDDEN';
  end if;
  if r.status <> 'CONFIRMED' then
    return query select false, 'INVALID_INPUT'::text, r.status, false; return;
  end if;
  update public.reservations set status = 'USED' where id = r.id;
  if r.coupon_id is not null then
    update public.user_coupons set status = 'USED', used_at = now() where id = r.coupon_id;
  end if;
  awarded := public.fn_award_xp(r.profile_id, 'resv-' || r.id, 30, 'Rezerv istifadə olundu');
  insert into public.notifications (profile_id, notif_type, title, body)
  values (r.profile_id, 'RESV_UPDATE', 'Rezerv istifadə olundu',
          'Məkanda check-in edildi — +' || 30 || ' XP.');
  return query select true, 'OK'::text, 'USED'::text, awarded;
end;
$$;

create or replace function public.fn_cancel_reservation(p_id text)
returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); r record;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into r from public.reservations where id = p_id and profile_id = me;
  if r.id is null then return false; end if;
  if r.status not in ('PENDING','CONFIRMED') then return false; end if;
  update public.reservations set status = 'CANCELLED' where id = p_id;
  insert into public.notifications (profile_id, notif_type, title, body)
  values (me, 'RESV_UPDATE', 'Rezerv ləğv edildi', 'Rezerv ləğv olundu.');
  return true;
end;
$$;

create or replace function public.fn_mark_no_show(p_id text)
returns table(ok boolean, restricted boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); r record; ns int; restricted_v boolean := false;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into r from public.reservations where id = p_id;
  if r.id is null then return query select false, false; return; end if;
  if not exists (select 1 from public.venues v
                 where v.id = r.venue_id and (v.managed_by = me or public.is_staff())) then
    raise exception 'FORBIDDEN';
  end if;
  if r.status not in ('PENDING','CONFIRMED') then
    return query select false, false; return;
  end if;
  update public.reservations set status = 'NO_SHOW' where id = p_id;
  update public.profiles set no_show_count = no_show_count + 1
    where id = r.profile_id returning no_show_count into ns;
  -- ROL E-7: 3 dəfə → 7 gün məhdudiyyət
  if ns >= 3 then
    update public.profiles
      set restricted_until = now() + interval '7 days',
          restriction_reason = '3 dəfə gəlməmə (no-show)'
      where id = r.profile_id and (restricted_until is null or restricted_until <= now());
    if found then
      restricted_v := true;
      insert into public.notifications (profile_id, notif_type, title, body)
      values (r.profile_id, 'RESV_UPDATE', 'Rezerv hüququ müvəqqəti məhdudlaşdırıldı',
              '3 dəfə gəlməmə qeydə alındı — 7 gün sonra rezerv yenidən açılacaq.');
      insert into public.admin_actions (actor_id, action, target_type, target_id, note)
      values (me, 'AUTO_RESTRICT', 'PROFILE', r.profile_id::text, 'no-show ×3');
    end if;
  end if;
  return query select true, restricted_v;
end;
$$;

-- N8: qrupla rezerv paylaşması — "Qoşul" iddiası
create or replace function public.fn_claim_share(p_share_token text)
returns table(ok boolean, error text, joined_event_id text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); r record;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into r from public.reservations where share_token = p_share_token;
  if r.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, null; return;
  end if;
  if r.status not in ('PENDING','CONFIRMED') then
    return query select false, 'INVALID_INPUT'::text, null; return;
  end if;
  insert into public.share_claims (reservation_id, claimed_by)
  values (r.id, me) on conflict do nothing;
  if r.event_id is not null then
    insert into public.event_participants (event_id, profile_id, status)
    values (r.event_id, me, 'JOINED') on conflict do nothing;
  end if;
  insert into public.notifications (profile_id, notif_type, title, body)
  values (r.profile_id, 'RESV_UPDATE', 'Qrupla rezerv',
          'Dostun rezervinə qoşuldu — QR kodu birlikdə istifadə edin.');
  return query select true, 'OK'::text, r.event_id;
end;
$$;

-- ═══════════════════ B4: rəy moderasiyası ═══════════════════════════════
create unique index if not exists reviews_one_per_event_author
  on public.reviews (event_id, author_id);

create or replace function public.fn_add_review(
  p_event_id text, p_rating int, p_body text
) returns table(ok boolean, error text, review_id text)
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); r_id text;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if p_rating not between 1 and 5 then raise exception 'INVALID_INPUT'; end if;
  if not exists (select 1 from public.events where id = p_event_id) then
    return query select false, 'OFFER_NOT_FOUND'::text, null; return; -- N3: naməlum eventId rədd
  end if;
  if not exists (select 1 from public.event_participants
                 where event_id = p_event_id and profile_id = me
                   and status = 'JOINED' and checked_in_at is not null) then
    return query select false, 'NOT_PARTICIPANT'::text, null; return;
  end if;
  r_id := public.ulid();
  insert into public.reviews (id, event_id, author_id, rating, body)
  values (r_id, p_event_id, me, p_rating, p_body);
  return query select true, 'OK'::text, r_id;   -- XP yalnız moderasiyadan sonra
end;
$$;

create or replace function public.fn_moderate_review(
  p_review_id text, p_approve boolean
) returns table(ok boolean, awarded_xp boolean, cap_reached boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); r record; weekly int; awarded_v boolean := false;
begin
  if me is null or not public.is_staff() then raise exception 'FORBIDDEN'; end if;
  select * into r from public.reviews where id = p_review_id;
  if r.id is null or r.status <> 'PENDING' then
    return query select false, false, false; return;
  end if;
  update public.reviews
    set status = case when p_approve then 'APPROVED' else 'REJECTED' end,
        moderated_at = now()
    where id = p_review_id;
  if not p_approve then
    return query select true, false, false; return;
  end if;
  -- həftədə max 3 (REVIEW_XP_WEEKLY_CAP) — ISO həftə; N3 pariteti:
  -- slot yalnız REAL award-dan sonra yanır
  select count(*) into weekly from public.reviews
    where author_id = r.author_id and status = 'APPROVED' and xp_awarded
      and date_trunc('week', created_at) = date_trunc('week', now());
  if weekly >= 3 then
    return query select true, false, true; return;   -- rəy dərc olundu, XP bu həftə dolub
  end if;
  awarded_v := public.fn_award_xp(r.author_id, 'review-' || r.event_id, 15, 'Tədbir rəyi');
  if awarded_v then
    update public.reviews set xp_awarded = true where id = p_review_id;
  end if;
  return query select true, awarded_v, false;
end;
$$;

-- ═══════════════════ ROL I: report + avto-restrict (N9) ══════════════════
create or replace function public.fn_report(
  p_target_type text, p_target_profile_id uuid default null,
  p_target_event_id text default null, p_target_venue_id text default null,
  p_reason text, p_note text default ''
) returns table(ok boolean, auto_restricted boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); r_id text; cnt int; restricted_v boolean := false;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  r_id := public.ulid();
  insert into public.reports (
    id, reporter_id, target_type, target_profile_id, target_event_id,
    target_venue_id, reason, note)
  values (r_id, me, p_target_type, p_target_profile_id, p_target_event_id,
          p_target_venue_id, p_reason, p_note);
  if p_target_type = 'USER' and p_target_profile_id is not null then
    -- eyni istifadəçiyə 3+ MÜXTƏLİF reporter → avtomatik restrict (ROL I)
    select count(distinct reporter_id) into cnt from public.reports
      where target_type = 'USER' and target_profile_id = p_target_profile_id
        and created_at > now() - interval '30 days';
    if cnt >= 3 then
      update public.profiles
        set restricted_until = now() + interval '7 days',
            restriction_reason = 'Report: ' || p_reason
        where id = p_target_profile_id
          and (restricted_until is null or restricted_until <= now());
      if found then
        restricted_v := true;
        insert into public.admin_actions (actor_id, action, target_type, target_id, note)
        values (me, 'AUTO_RESTRICT', 'PROFILE', p_target_profile_id::text,
                'reports: ' || cnt);
      end if;
    end if;
  end if;
  return query select true, restricted_v;
end;
$$;

create or replace function public.fn_block_user(p_target_id uuid) returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  insert into public.blocks (blocker_id, blocked_id) values (me, p_target_id)
    on conflict do nothing;
  return true;
end;
$$;

create or replace function public.fn_unblock_user(p_target_id uuid) returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  delete from public.blocks where blocker_id = me and blocked_id = p_target_id;
  return true;
end;
$$;

-- ═══════════════════ N6: mesaj + rate limit + spam ══════════════════════
create or replace function public.fn_send_message(
  p_kind text, p_event_id text default null, p_recipient_id uuid default null,
  p_body text
) returns table(ok boolean, error text, message_id text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); m_id text; bucket text; hits int;
  window_end timestamptz; dup boolean;
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
  select hit_count, window_end into hits, window_end
    from public.rate_limit_buckets where bucket_key = bucket for update;
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
$$;

-- ═══════════════════ B7/D-070: match namizədləri (ST_DWithin) ════════════
create or replace function public.fn_match_candidates(
  p_radius_m int default 5000
) returns table(profile_id uuid, first_name text, city text, distance_m numeric)
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := auth.uid(); my_geo geography;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select last_location into my_geo from public.profiles where id = me;
  if my_geo is null then return; end if;
  return query
    select p.id, p.first_name, p.city,
           st_distance(my_geo, p.last_location)
    from public.profiles p
    where p.id <> me
      and p.last_location is not null
      and (p.restricted_until is null or p.restricted_until <= now())
      and p.map_visibility <> 'OFF'
      and (p.map_visibility = 'EVERYONE' or exists (
            select 1 from public.matches m
            where (m.a_id = p.id and m.b_id = me) or (m.b_id = p.id and m.a_id = me)))
      and not exists (select 1 from public.blocks b
            where (b.blocker_id = p.id and b.blocked_id = me)
               or (b.blocked_id = p.id and b.blocker_id = me))
      and not exists (select 1 from public.swipes s
            where s.swiper_id = me and s.target_id = p.id)
      and st_dwithin(my_geo, p.last_location, p_radius_m)
    order by st_distance(my_geo, p.last_location);
end;
$$;

-- ═══════════════════ Dəvət zənciri (B3) ═════════════════════════════════
create or replace function public.fn_create_invite() returns text
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); code_v text;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  code_v := 'QSH-INV-' || substr(public.ulid(), 1, 12);
  insert into public.invites (code, inviter_id) values (code_v, me);
  return code_v;   -- XP YOX — birinci check-in-dən sonra (B3 düzəlişi)
end;
$$;

create or replace function public.fn_register_invite(p_code text) returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  update public.invites
    set invitee_id = me, registered_at = now()
    where code = p_code and invitee_id is null;
  return found;
end;
$$;

-- ═══════════════════ Tərcihlər / yerləşmə ═══════════════════════════════
create or replace function public.fn_set_notif_pref(p_notif_type text, p_enabled boolean)
returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  insert into public.notification_settings (profile_id, notif_type, enabled)
  values (me, p_notif_type, p_enabled)
  on conflict (profile_id, notif_type) do update set enabled = excluded.enabled;
  return true;
end;
$$;

create or replace function public.fn_update_location(p_lat double precision, p_lng double precision)
returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  update public.profiles
    set last_location = st_setsrid(st_makepoint(p_lng, p_lat), 4326)::geography,
        location_updated_at = now()
    where id = me;
  return true;
end;
$$;

-- Rəy moderasiyası — şəkil moderasiyası növbəsi (M4 job oxuyur)
create or replace function public.fn_moderation_queue() returns setof public.reports
language sql stable security definer set search_path = public as $$
  select * from public.reports where status = 'OPEN' order by created_at;
$$;

-- ═══════════════════ İcra hüquqları ═════════════════════════════════════
grant execute on function public.ulid() to postgres;
grant execute on all functions in schema public to authenticated;
revoke execute on function public.fn_moderate_review(text, boolean) from authenticated;
revoke execute on function public.fn_moderation_queue() from authenticated;
grant execute on function public.fn_moderate_review(text, boolean) to authenticated; -- daxili is_staff() yoxlayır
comment on function public.fn_daily_login() is
  'D-073 pariteti: 1-2 gün dondurulma, 3+ gün sıfırlanma; +5 hər giriş; 7-də +30.';
