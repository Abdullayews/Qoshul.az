-- ═══════════════════════════════════════════════════════════════════════════
-- Qoshul.az — 0004: KRİTİK audit düzəlişləri (2026-11-14 error axtarışı)
--
-- Mənbə: repo audit hesabatı — 25 tapıntıdan SQL-ə dəyən kritiklər:
--   F1  fn_award_xp authenticated-ə açıq idi → self-award XP
--   F2  fn_grant_venue_xp self-grant → öz hesabına limitsiz XP (pul)
--   F5  xatırlatma notification-ları üçün service-only yazı funksiyası yox idi
--   F6  kupon KODU ilə rezerv (klient kod göndərir, funksiya ID gözləyirdi)
--   F8  profiles.xp/restricted_until/no_show_count/... RLS-də qorunmur idi
--   F10 ulid() 20 simvol qaytarırdı (spec: 26) + 90 bayt entropiya istehsalı
--   F16 0002:1027 "grant back" öz revoke-ını ləğv edirdi
--
-- Prinsip (Supabase sənədi — Database Functions security):
--   "revoke execute for the functions you want to protect, from both public
--    and the role you're restricting" + "an execute grant controls which
--    roles can call the function. It doesn't limit the rows".
--   SECURITY DEFINER sahibi (postgres) adından daxili ağac çağırışları
--   grant tələb etmir — yalnız birbaşa PostgREST çağırışları üçün grant
--   lazımdır; ona görə whitelist yanaşması daxili istifadəni sındırmır.
--
-- Qeyd: deploy HƏLƏ YOXDUR (boş DB) — create or replace / drop if exists
-- ilə tam idempotent yazılıb; 0001-0003 əvvəl tətbiq olunmuş sayılır.
-- ═══════════════════════════════════════════════════════════════════════════

-- ── F10: ulid() — 26 simvol (48-bit ms + 80-bit CSPRNG), tək blob ──────────
create or replace function public.ulid() returns text
language plpgsql stable as $$
declare
  ts_ms bigint; enc text; rnd text; g bytea;
  alphabet text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  i int;
begin
  ts_ms := (extract(epoch from clock_timestamp()) * 1000)::bigint;
  enc := '';
  for i in reverse 47..0 by 5 loop
    enc := substr(alphabet, ((ts_ms >> i) & 31)::int + 1, 1) || enc;
  end loop;                            -- 10 simvol / 48 bit
  g := gen_random_bytes(16);           -- tək çağırış: 90 → 16 bayt istehsal
  rnd := '';
  for i in 1..16 loop                  -- 16 simvol / 80 bit (əvvəl 10 idi)
    rnd := rnd || substr(alphabet, (get_byte(g, i - 1) & 31)::int + 1, 1);
  end loop;
  return enc || rnd;                   -- 26 simvol (github.com/ulid/spec)
end;
$$;
comment on function public.ulid() is
  'ULID (Crockford base32, 26 simvol / 128 bit): 48-bit ms timestamp + 80-bit CSPRNG. F10 düzəlişi: rnd 10→16 simvol, tək gen_random_bytes(16).';

-- ── F1/F2/F16: icra hüquqları — qlobal grant ləğv, whitelist geri qaytar ────
revoke execute on all functions in schema public from authenticated;

-- Whitelist: klientin birbaşa çağırdığı RPC-lər (SupabaseSync.mapEvent pariteti)
grant execute on function public.fn_daily_login() to authenticated;
grant execute on function public.fn_record_swipe(uuid, boolean) to authenticated;
grant execute on function public.fn_preview_spend(text, int, numeric) to authenticated;
grant execute on function public.fn_spend_venue_xp(text, int, numeric, text, text)
  to authenticated;
grant execute on function public.fn_buy_coupon(text) to authenticated;
grant execute on function public.fn_issue_xp_card_nonce(text) to authenticated;
grant execute on function public.fn_consume_xp_card_nonce(text) to authenticated;
grant execute on function public.fn_create_event(
  text, text, text, text, date, text, int, text, int, text, text, text, text,
  boolean, text, double precision, double precision, text, text) to authenticated;
grant execute on function public.fn_join_event(text) to authenticated;
grant execute on function public.fn_approve_participant(text, uuid) to authenticated;
grant execute on function public.fn_check_in_event(text, double precision, double precision)
  to authenticated;
grant execute on function public.fn_create_reservation(
  text, date, text, int, text, text, text, text) to authenticated;
grant execute on function public.fn_confirm_reservation(text) to authenticated;
grant execute on function public.fn_use_reservation(text) to authenticated;
grant execute on function public.fn_cancel_reservation(text) to authenticated;
grant execute on function public.fn_mark_no_show(text) to authenticated;
grant execute on function public.fn_claim_share(text) to authenticated;
grant execute on function public.fn_add_review(text, int, text) to authenticated;
grant execute on function public.fn_report(text, uuid, text, text, text) to authenticated;
grant execute on function public.fn_block_user(uuid) to authenticated;
grant execute on function public.fn_unblock_user(uuid) to authenticated;
grant execute on function public.fn_send_message(text, text, uuid, text) to authenticated;
grant execute on function public.fn_match_candidates(int) to authenticated;
grant execute on function public.fn_create_invite() to authenticated;
grant execute on function public.fn_register_invite(text) to authenticated;
grant execute on function public.fn_set_notif_pref(text, boolean) to authenticated;
grant execute on function public.fn_update_location(double precision, double precision)
  to authenticated;
grant execute on function public.fn_finish_activity(text, uuid[]) to authenticated;

-- AÇILMAYANLAR (daxili köməkçilər / service-only) — PUBLIC default grantı da ləğv:
revoke execute on function public.fn_award_xp(uuid, text, int, text) from public;
revoke execute on function public.fn_grant_venue_xp(text, int, text) from public;
revoke execute on function public.fn_expire_coupons() from public;
revoke execute on function public.fn_moderate_review(text, boolean) from public;
revoke execute on function public.fn_moderation_queue() from public;
revoke execute on function public.fn_rank_index(int) from public;
revoke execute on function public.fn_push_notification(uuid, text, text, text) from public;
revoke execute on function public.fn_touch_updated_at() from public;
revoke execute on function public.fn_ensure_profile() from public;
revoke execute on function public.fn_guard_profile_columns() from public;

-- ── F5: service-only notification yazıcısı (xatırlatma axını üçün) ─────────
-- jobs.rs artıq fn_set_notif_pref çağırMUR (o, istifadəçinin tərcihini
-- yandırırdı — F5). Əvəzinə bu funksiya: per-type toggle + sükut saatını
-- nəzərə alaraq notifications cədvəlinə yazır; push göndərilib-göndərilmə-
-- məsini qaytarır (FcmSender-enabled yalnız Rus tərəfində).
create or replace function public.fn_push_notification(
  p_profile_id uuid, p_type text, p_title text, p_body text
) returns boolean
language plpgsql security definer set search_path = public as $$
declare quiet boolean;
begin
  -- per-type toggle (ROL H): söndürübsə yazılmır
  if exists (select 1 from public.notification_settings ns
             where ns.profile_id = p_profile_id
               and ns.notif_type = p_type and not ns.enabled) then
    return false;
  end if;
  -- sükut saatı 23:00-08:00 (AZ) — push-u gecikdirir, DB yazısı qalır
  quiet := extract(hour from now() at time zone 'Asia/Baku') >= 23
        or extract(hour from now() at time zone 'Asia/Baku') < 8;
  insert into public.notifications (profile_id, notif_type, title, body)
  values (p_profile_id, p_type, p_title, p_body);
  return not quiet;
end;
$$;
comment on function public.fn_push_notification is
  'F5: M4 servisi üçün notification yazıcısı — tərcih + sükut saatı; yalnız service_role çağırır.';

-- ── F2: təşkilatçı grantı — server tərəfli yoxlamalı fn_finish_activity ───
-- Köhnə fn_grant_venue_xp CLIENT-in amount-u ilə öz hesabını şişirdə bilirdi.
-- Yeni axın: host öz tədbirini bitirir; məbləğ sabit (ORGANIZER_XP=50),
-- idempotensiya açarı grant-<event_id>, host-müəlliflik + keçmişlik yoxlaması.
create or replace function public.fn_finish_activity(
  p_event_id text, p_tagged_ids uuid[] default '{}'
) returns table(ok boolean, organizer_xp int, tagged_xp int)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); e record; acct record;
  granted boolean := false;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into e from public.events where id = p_event_id;
  if e.id is null or e.host_profile_id <> me then
    return query select false, 0, 0; return;          -- yalnız ÖZ tədbiri
  end if;
  if e.venue_id is null then
    return query select false, 0, 0; return;          -- obyekt bağlı deyil
  end if;
  -- tədbir keçməlidir (A6 epoch_day; AZ karqünlüyü üçün -1 günlük tolerans)
  if e.epoch_day >= (extract(epoch from current_date) / 86400)::bigint - 1 then
    return query select false, 0, 0; return;
  end if;

  -- idempotent grant: açar = grant-<event_id> (ikinci dəfə yazılmır)
  insert into public.venue_xp_accounts (profile_id, venue_id, balance)
    values (me, e.venue_id, 0)
    on conflict (profile_id, venue_id) do nothing;
  select * into acct from public.venue_xp_accounts
    where profile_id = me and venue_id = e.venue_id;
  insert into public.venue_xp_ledger
    (account_id, is_grant, amount, balance_after, note, idempotency_key)
  values (acct.id, true, 50, acct.balance + 50, 'Təşkilatçı bitirdi',
          'grant-' || p_event_id);
  update public.venue_xp_accounts set balance = balance + 50
    where profile_id = me and venue_id = e.venue_id;
  granted := true;

  -- taq edilmiş iştirakçılara +25 (L.3 pariteti; onların hesabına)
  if p_tagged_ids is not null and array_length(p_tagged_ids, 1) > 0 then
    foreach me in array p_tagged_ids loop
      continue when me = e.host_profile_id;
      insert into public.venue_xp_accounts (profile_id, venue_id, balance)
        values (me, e.venue_id, 0)
        on conflict (profile_id, venue_id) do nothing;
      insert into public.venue_xp_ledger
        (account_id, is_grant, amount, balance_after, note, idempotency_key)
      select a.id, true, 25, a.balance + 25, 'Tədbirdə iştirak etdi',
             'tagged-' || p_event_id || '-' || me::text
        from public.venue_xp_accounts a
        where a.profile_id = me and a.venue_id = e.venue_id;
      update public.venue_xp_accounts set balance = balance + 25
        where profile_id = me and venue_id = e.venue_id;
    end loop;
  end if;

  return query select granted, 50,
    coalesce(array_length(p_tagged_ids, 1), 0) * 25;
end;
$$;
comment on function public.fn_finish_activity is
  'F2: host-müəlliflik + sabit məbləğ (50/25) + idempotensiya (grant-<event_id>). fn_grant_venue_xp artıq client-ə açıq deyil.';

-- ── F6: kupon KODU körpüsü — fn_create_reservation ikili qəbul edir ───────
-- M1 klienti kuponun görüntü kodunu göndərir; ULID id yalnız M2-də gələcək.
create or replace function public.fn_create_reservation(
  p_venue_id text, p_resv_date date, p_resv_time text, p_party int,
  p_note text default '', p_coupon_id text default null,
  p_coupon_code text default null, p_event_id text default null
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
  if p_coupon_id is null and p_coupon_code is not null then
    -- F6 körpüsü: kod ilə axtarış (M1 klienti kod göndərir)
    select * into c from public.user_coupons
      where code = p_coupon_code and profile_id = me and venue_id = p_venue_id;
  elsif p_coupon_id is not null then
    select * into c from public.user_coupons
      where id = p_coupon_id and profile_id = me and venue_id = p_venue_id;
  end if;
  if (p_coupon_id is not null or p_coupon_code is not null)
     and (c.id is null or c.status <> 'ACTIVE' or now() > c.expires_at) then
    return query select false, 'INVALID_INPUT'::text, null, null, null; return;
  end if;
  status_v := case when v.auto_accept then 'CONFIRMED' else 'PENDING' end;
  r_id := public.ulid();
  qr := 'QSH-r-' || substr(public.ulid(), 1, 12);      -- N1: server-generated
  tok := 'shr-' || substr(public.ulid(), 1, 12);       -- N8: paylaşma tokeni
  insert into public.reservations (
    id, profile_id, venue_id, event_id, resv_date, resv_time, party, note,
    coupon_id, status, qr_code, share_token)
  values (r_id, me, p_venue_id, p_event_id, p_resv_date, p_resv_time, p_party,
          p_note, c.id, status_v, qr, tok);
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
comment on function public.fn_create_reservation is
  'F6: p_coupon_code (M1 görüntü kodu) və ya p_coupon_id (ULID) — hər ikisi qəbul olunur.';

-- ── F8: profiles sütun qoruması — guard trigger (column-level security) ────
-- RLS sətir-səviyyəlidir, sütun deyil; 0001-dəki WITH CHECK yalnız rolu
-- qoruyurdu. Trigger authenticated PATCH-lərdə server-idarə sahələrini
-- dondurur.
--
-- Diskriminator: `current_user` — SECURITY DEFINER funksiyası daxilində
-- sahibi (postgres) olur, birbaşa PostgREST PATCH-də isə `authenticated`.
-- (current_setting('role') DEFİNER daxilində də 'authenticated' qalır —
-- ona güvənmək olmaz; auth.uid() da JWT GUC-u olduğundan daxili çağırışlarda
-- dolu qalır.) Service role (M4) və psql (migration) keçir.
create or replace function public.fn_guard_profile_columns() returns trigger
language plpgsql as $$
begin
  if current_user = 'authenticated' then
    if new.xp is distinct from old.xp
       or new.restricted_until is distinct from old.restricted_until
       or new.restriction_reason is distinct from old.restriction_reason
       or new.no_show_count is distinct from old.no_show_count
       or new.phone_verified is distinct from old.phone_verified
       or new.login_streak is distinct from old.login_streak
       or new.last_login_day is distinct from old.last_login_day
       or new.role is distinct from old.role then
      raise exception 'QORUNAN_SUTUN: bu sahələr yalnız server funksiyaları ilə dəyişir';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists profiles_guard_columns on public.profiles;
create trigger profiles_guard_columns
  before update on public.profiles
  for each row execute function public.fn_guard_profile_columns();
comment on function public.fn_guard_profile_columns is
  'F8: authenticated PATCH-lərdə xp/restricted_until/no_show_count/phone_verified/login_streak/last_login_day/role dondurulur (column-level security).';

-- ── F16: 0002:1027-ci sətri ləğv edir (revoke → grant back zənciri) ────────
-- Yuxarıdakı revoke bloku fn_moderate_review-i artıq bağlı saxlayır;
-- 0002-dəki "grant ... to authenticated; -- daxili is_staff() yoxlayır"
-- sətri bu miqrasiyadan sonra təsirsizdir (revoke on ALL sonrakı grant-ı
-- ləğv edir — 0002 əvvəl, 0004 sonra icra olunduğundan sıra düzgündür).
