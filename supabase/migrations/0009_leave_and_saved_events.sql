-- ═══════════════════════════════════════════════════════════════════════
-- 0009_leave_and_saved_events.sql (D-169)
--
-- D-167 auditinin funksionallıq tapıntıları: app-da join/leave/toggleSave
-- YALNIZ LOKAL state dəyişdirirdi — serverə heç bir RPC getmirdi.
-- DB tərəfdə bu üç əməliyyatdan ikisi (leave, save) üçün funksiya YOX idi.
--
-- Bu migration:
--   1. fn_leave_event — tədbirdən çıxış (status LEFT, XP geri ALINMIR:
--      qoşulma XP-si verildiyi anda qazanılıb — leave qaytarılmır,
--      amma gələcək check-in/finish mükafatları bloklanır);
--   2. saved_events cədvəli + fn_save_event / unsave (toggle) —
--      „saxla“ (bookmark) funksiyası serverdə;
--   3. fn_join_event-ə LEFT statusu əlavə (yenidən qoşulma idarəsi).
-- ═══════════════════════════════════════════════════════════════════════

-- ── 0. Status CHECK-inə LEFT əlavəsi (0001-dəki constraint PENDING/
--    JOINED/DECLINED qəbul edirdi — LEFT pozulurdu) ─────────────────────
DO $$
BEGIN
  IF EXISTS (
    select 1 from pg_constraint
    where conrelid = 'public.event_participants'::regclass
      and conname = 'event_participants_status_check'
  ) THEN
    ALTER TABLE public.event_participants
      DROP CONSTRAINT event_participants_status_check;
    ALTER TABLE public.event_participants
      ADD CONSTRAINT event_participants_status_check
      CHECK (status IN ('PENDING','JOINED','DECLINED','LEFT'));
  END IF;
END
$$;

-- ── 1. Tədbirdən çıxış ─────────────────────────────────────────────────
-- status = 'LEFT': tarixçə qorunur (hansı tədbirlərdə olub), amma
-- aktiv iştirak sayına düşmür (fn_join_event yalnız JOINED/PENDING
-- sayır — ALREADY_JOINED yoxlaması LEFT-ə icazə verir → yenidən
-- qoşulma mümkündür).
create or replace function public.fn_leave_event(p_event_id text)
returns table(ok boolean, error text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  update public.event_participants
     set status = 'LEFT'
   where event_id = p_event_id and profile_id = me
     and status in ('JOINED','PENDING');
  if not found then
    return query select false, 'NOT_JOINED'::text; return;
  end if;
  return query select true, 'OK'::text;
end;
$$;

grant execute on function public.fn_leave_event(text) to authenticated;

-- fn_join_event-in ALREADY_JOINED yoxlaması JOINED/PENDING üzərindədir —
-- LEFT-dən yenidən qoşulma avtomatik işləyir (yeni sətir insert olunur,
-- köhnə LEFT qalır). Bu bilərəkdən belədir (tarixçə).

-- ── 2. Saxlanılan tədbirlər (bookmark) ────────────────────────────────
create table if not exists public.saved_events (
  profile_id uuid not null references public.profiles(id) on delete cascade,
  event_id   text not null references public.events(id) on delete cascade,
  saved_at   timestamptz not null default now(),
  primary key (profile_id, event_id)
);

comment on table public.saved_events is
  'İstifadəçinin saxladığı (bookmark) tədbirlər — D-169: toggleSave artıq serverdə.';

create index if not exists saved_events_profile_idx
  on public.saved_events(profile_id);

alter table public.saved_events enable row level security;

-- yalnız SAHİBİ görə/dəyişə bilər
drop policy if exists saved_events_owner on public.saved_events;
create policy saved_events_owner on public.saved_events
  for all to authenticated
  using (auth.uid() = profile_id)
  with check (auth.uid() = profile_id);

grant select, insert, delete on public.saved_events to authenticated;

-- ── 3. fn_save_event — toggle RPC (idempotent: varsa silir, yoxdursa
--    yaradır; server tərəfli səhvə yol vermir) ─────────────────────────
create or replace function public.fn_save_event(p_event_id text)
returns boolean  -- true = SAXLANILDI, false = SİLİNDİ
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if not exists (select 1 from public.events where id = p_event_id) then
    raise exception 'EVENT_NOT_FOUND';
  end if;
  delete from public.saved_events
   where profile_id = me and event_id = p_event_id;
  if found then return false; end if;
  insert into public.saved_events (profile_id, event_id) values (me, p_event_id);
  return true;
end;
$$;

grant execute on function public.fn_save_event(text) to authenticated;

-- ── 4. Reader dəstəyi: saved id-lər (SupabaseReader saved_events oxuyur) ─
comment on function public.fn_save_event is
  'D-169: toggleSave serverdə — true=saxla, false=sil. App restart-da itmir.';

-- ── 5. fn_join_event: LEFT-dən yenidən qoşulma ─────────────────────────
-- PK (event_id, profile_id) LEFT sətirini saxladığı üçün adi INSERT
-- duplicate key atırdı. Yenidən qoşulmada köhnə LEFT sətri silinir
-- (iştirak tarixçəsi XP jurnalında — xp_events — qalır).
create or replace function public.fn_join_event(p_event_id text)
returns table(ok boolean, error text, awarded_xp boolean)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid(); e record; joined_count int;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select * into e from public.events where id = p_event_id;
  if e.id is null then
    return query select false, 'OFFER_NOT_FOUND'::text, false; return;
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
  -- D-169: LEFT qalığı varsa sil (PK toqquşmasın)
  delete from public.event_participants
   where event_id = p_event_id and profile_id = me and status = 'LEFT';
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

grant execute on function public.fn_join_event(text) to authenticated;
