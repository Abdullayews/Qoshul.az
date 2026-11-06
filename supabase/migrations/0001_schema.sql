-- ═══════════════════════════════════════════════════════════════════════════
-- Qoshul.az M1 — Supabase sxemi (0001): cədvəllər + FK + indekslər + RLS
-- D-076 (2026-11-07): M1 miqrasiya qatının sxem hissəsi.
--
-- Dizayn qaydaları:
--  * ID-lər: profiles.id = auth.users (Supabase konvensiyası); bütün digər
--    cədvəllərdə ULID (DEFAULT ulid()) — N1 həlli: "size+1" üslublu kolliziya-
--    yaradan klient ID-ləri aradan qalxdı; ULID vaxt-sıralı, 26 simvol Crockford.
--  * XP həssas cədvəllərə (xp_events, venue_xp_accounts, venue_xp_ledger,
--    xp_card_nonces, rate_limit_buckets) birbaşa INSERT/UPDATE/DELETE YOXDUR —
--    yalnız SECURITY DEFINER funksiyalar (0002) yazır (qayda 8/9).
--  * RLS: hər cədvəldə aktiv; istifadəçi öz sətirlerini, partnyor öz obyektini,
--    moderator/admin report növbəsini oxuyur.
--  * PostGIS (D-070): geography(Point,4326) + GiST; məsafə sorğuları ST_DWithin.
--  * Xəritə görünüşü (B7): profiles.select siyasəti map_visibility-i SERVER
--    tərəfindən zorlanır (FRIENDS = yalnız matches cütü, OFF = yalnız özü).
--  * Məxfilik (ROL I): last_location yalnız sahibinə qaytarılır (I: "dəqiq
--    koordinat heç vaxt API cavabında başqa istifadəçiyə qaytarılmır") —
--    profile_cards görünüşü (security_invoker) o sütunu açmir.
-- ═══════════════════════════════════════════════════════════════════════════

create extension if not exists postgis;
create extension if not exists pgcrypto;

-- ── ULID generatoru (N1 həlli) ─────────────────────────────────────────────
create or replace function public.ulid() returns text
language plpgsql stable as $$
declare
  ts_ms bigint;
  enc text;
  alphabet text := '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
  rnd text;
  i int;
begin
  ts_ms := (extract(epoch from clock_timestamp()) * 1000)::bigint;
  enc := '';
  for i in reverse 47..0 by 5 loop
    enc := substr(alphabet, ((ts_ms >> i) & 31)::int + 1, 1) || enc;
  end loop;
  rnd := '';
  for i in 1..10 loop
    rnd := rnd || substr(alphabet, (get_byte(gen_random_bytes(10), i - 1) & 31)::int + 1, 1);
  end loop;
  return enc || rnd;
end;
$$;
comment on function public.ulid() is
  'ULID (Crockford base32, 26 simvol): 48-bit ms timestamp + 80-bit CSPRNG. N1: klient "size+1" ID kolliziyalarının server həlli.';

-- ── updated_at tetikleyicisi ──────────────────────────────────────────────
create or replace function public.fn_touch_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

-- ═══════════════════════════ 1. Profil ════════════════════════════════════

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  role text not null default 'USER'
    check (role in ('USER','PARTNER','MODERATOR','ADMIN')),
  first_name text not null default 'Dost',
  last_name text not null default '',
  birth_date date,
  gender text not null default '',
  bio text not null default '' check (char_length(bio) <= 150),
  photo_count int not null default 1 check (photo_count between 0 and 6),
  photo_urls jsonb not null default '[]',
  phone_verified boolean not null default false,
  city text not null default 'Bakı',
  status text not null default 'student',
  university text not null default '',
  work_field text not null default '',
  languages jsonb not null default '["Azərbaycan dili"]',
  goals jsonb not null default '[]',
  personality jsonb,
  availability jsonb not null default '[]',
  group_size text not null default 'Kiçik qrup (3–6)',
  budget text not null default '10 AZN-ə qədər',
  -- məxfilik (D-027 / ROL TAB 5)
  map_visibility text not null default 'EVERYONE'
    check (map_visibility in ('EVERYONE','FRIENDS','OFF')),
  show_distance boolean not null default true,
  -- XP 1 (peşəkarlıq — xərclənmir)
  xp int not null default 0 check (xp >= 0),
  login_streak int not null default 0,
  last_login_day bigint,              -- epochDay (D-073 gap semantikası)
  -- moderasiya
  restricted_until timestamptz,
  restriction_reason text not null default '',
  no_show_count int not null default 0 check (no_show_count >= 0),
  -- coğrafiya (yalnız sahib oxuyur — profile_cards açmir)
  last_location geography(Point, 4326),
  location_updated_at timestamptz,
  onboarded boolean not null default false,
  profile_completed boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
comment on table public.profiles is
  'auth.users uzantısı. XP/rezerv/kupon qaydaları yalnız funksiyalarla dəyişir (0002).';

create trigger profiles_touch_updated_at
  before update on public.profiles
  for each row execute function public.fn_touch_updated_at();

create index profiles_location_gist on public.profiles using gist (last_location);
create index profiles_city_idx on public.profiles (city);
create index profiles_xp_idx on public.profiles (xp desc);

-- Başqalarına görünən kart — dəqiq koordinat/restriction daxil deyil (ROL I).
create view public.profile_cards
  with (security_invoker = true) as
select
  p.id, p.first_name, p.last_name, p.city, p.status, p.university,
  p.work_field, p.languages, p.goals, p.personality, p.availability,
  p.group_size, p.budget, p.bio, p.photo_count, p.map_visibility,
  p.show_distance, p.xp, p.role, p.onboarded, p.created_at
from public.profiles p;

-- ── Bildiriş tercihleri (ROL H: hər tip ayrıca) ──────────────────────────
create table public.notification_settings (
  profile_id uuid not null references public.profiles(id) on delete cascade,
  notif_type text not null check (notif_type in (
    'MATCH','MESSAGE','EVENT_JOIN','EVENT_APPROVAL','EVENT_REMINDER',
    'RESV_UPDATE','RESV_REMINDER','LEVEL_UP','COUPON_EXPIRING',
    'NEARBY_EVENT','MARKETING')),
  enabled boolean not null default true,
  primary key (profile_id, notif_type)
);

-- ── Kateqoriyalar (taksonomiya — seed 0003) ───────────────────────────────
create table public.categories (
  slug text primary key,
  title text not null,
  group_title text not null,
  sort int not null default 0
);
create index categories_group_idx on public.categories (group_title, sort);

create table public.user_interests (
  profile_id uuid not null references public.profiles(id) on delete cascade,
  category_slug text not null references public.categories(slug) on delete cascade,
  primary key (profile_id, category_slug)
);
comment on table public.user_interests is
  'D-074: maraq seçimi LIMITSİZ — heç bir say həddi yoxdur (CHECK yoxdur).';

-- ═══════════════════════════ 2. Obyektlər ════════════════════════════════

create table public.venues (
  id text primary key,                       -- sabit kod (v1..v6) və ya ULID
  name text not null,
  category text not null,
  address text not null default '',
  city text not null default 'Bakı',
  deal text,
  hours text not null default '',
  image text not null default 'friends',
  price_range text not null default '₼₼',
  auto_accept boolean not null default true,
  rating numeric(2,1) not null default 4.6,
  reviews_count int not null default 12,
  open_from int not null default 9 check (open_from between 0 and 23),
  open_to int not null default 22 check (open_to between 1 and 24),
  discount_percent int not null default 0 check (discount_percent between 0 and 100),
  location geography(Point, 4326) not null,
  -- L.3: xp_config (obyekt başına)
  xp_per_azn int not null default 20 check (xp_per_azn > 0),
  max_discount_percent int not null default 25 check (max_discount_percent between 1 and 100),
  min_bill_azn int not null default 0 check (min_bill_azn >= 0),
  -- partnyor sahibi (biznes rejimi — ROL A)
  managed_by uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
comment on table public.venues is
  'Obyektlər (Partnyor modeli). xp_per_azn/max_discount_percent/min_bill_azn = L.3 xp_config (D-061: 0 bölmə qadağan — CHECK > 0).';
create index venues_location_gist on public.venues using gist (location);
create index venues_city_idx on public.venues (city);
create index venues_managed_by_idx on public.venues (managed_by);

create trigger venues_touch_updated_at
  before update on public.venues
  for each row execute function public.fn_touch_updated_at();

-- Rezerv saat slotları (ROL E)
create table public.venue_slots (
  id text primary key default public.ulid(),
  venue_id text not null references public.venues(id) on delete cascade,
  slot_date date not null,
  slot_time text not null,
  capacity int not null default 8 check (capacity > 0),
  unique (venue_id, slot_date, slot_time)
);

-- Obyekt rəyləri (ictimai — moderasiyalı)
create table public.venue_reviews (
  id text primary key default public.ulid(),
  venue_id text not null references public.venues(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  stars int not null check (stars between 1 and 5),
  text text not null default '',
  status text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED')),
  created_at timestamptz not null default now()
);
create index venue_reviews_venue_idx on public.venue_reviews (venue_id, status);

-- ═══════════════════════════ 3. XP (ikitərəfli — D-026) ══════════════════

-- XP 1: peşəkarlıq — immutable jurnal + idempotensiya açarı
create table public.xp_events (
  id text primary key default public.ulid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  key text not null,                    -- 'join-e1', 'login-2026-11-07'...
  amount int not null check (amount > 0),
  note text not null default '',
  at timestamptz not null default now(),
  unique (profile_id, key)              -- award(key) idempotensiyası
);
comment on table public.xp_events is
  'Immutable jurnal. Yazmaq yalnız fn_award_xp (0002) — birbaşa DML revok olunub.';

-- Səviyyələr (seed 0003; ROL F)
create table public.levels (
  rank_index int primary key check (rank_index between 0 and 5),
  name text not null,
  rank_max int not null,                -- 99/299/699/1499/2999 (5. = terminal)
  badge text not null default ''
);

-- XP 2: obyektə-məxsus endirim bazası (L.2 — TTL yoxdur)
create table public.venue_xp_accounts (
  id text primary key default public.ulid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  venue_id text not null references public.venues(id) on delete cascade,
  balance int not null default 0 check (balance >= 0),   -- B6: mənfi qadağan
  unique (profile_id, venue_id)
);
comment on table public.venue_xp_accounts is
  'B6: balance >= 0 CHECK — DB səviyyəsində mənfi balans mümkün deyil.';

-- L.4: mübahisə sübutu — immutable ledger
create table public.venue_xp_ledger (
  id text primary key default public.ulid(),
  account_id text not null references public.venue_xp_accounts(id) on delete cascade,
  is_grant boolean not null,
  amount int not null check (amount > 0),
  balance_after int not null check (balance_after >= 0),
  note text not null default '',
  idempotency_key text not null unique,
  at timestamptz not null default now()
);
create index venue_xp_ledger_account_idx on public.venue_xp_ledger (account_id, at desc);

-- L.4-1: dinamik XP kartı — server CSPRNG nonce (B5 həlli)
create table public.xp_card_nonces (
  nonce text primary key,                       -- 'QSHUL-XP-<6 rəqəm>-<chk>'
  profile_id uuid not null references public.profiles(id) on delete cascade,
  venue_id text not null references public.venues(id) on delete cascade,
  fallback_code text not null,                  -- 6 rəqəm (L.5)
  issued_at timestamptz not null default now(),
  expires_at timestamptz not null,              -- TTL 60 s (XP_CARD_TTL_SECONDS)
  used_at timestamptz,
  check (expires_at > issued_at)
);
create index xp_card_nonces_profile_idx on public.xp_card_nonces (profile_id, issued_at desc);

-- ═══════════════════════════ 4. Kuponlar (ROL G) ═════════════════════════

create table public.coupons_offers (
  id text primary key,                       -- 'o1'.. və ya ULID
  venue_id text not null references public.venues(id) on delete cascade,
  label text not null,
  percent int not null default 0 check (percent between 0 and 100),
  fixed_azn int not null default 0 check (fixed_azn >= 0),
  min_rank_index int not null default 0 check (min_rank_index between 0 and 5),
  xp_price int not null default 0,           -- 0 = yalnız səviyyə ilə avtomatik
  validity_days int not null default 14 check (validity_days > 0),
  auto_by_level boolean not null default false,
  active boolean not null default true,
  created_at timestamptz not null default now()
);
create index coupons_offers_venue_idx on public.coupons_offers (venue_id, active);

create table public.user_coupons (
  id text primary key default public.ulid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  offer_id text not null references public.coupons_offers(id) on delete cascade,
  venue_id text not null references public.venues(id) on delete cascade,
  code text not null unique,                 -- 'QSHUL-<ulid12>'
  status text not null default 'ACTIVE' check (status in ('ACTIVE','USED','EXPIRED')),
  purchased_at timestamptz not null default now(),
  expires_at timestamptz not null,
  used_at timestamptz,
  check (expires_at > purchased_at)
);
create index user_coupons_profile_idx on public.user_coupons (profile_id, status);
comment on table public.user_coupons is
  'L.4/G: 14 gün, təkistifadə, XP qayıtmır (kupon kodu serverdə yaranır — fn_buy_coupon).';

-- ═══════════════════════════ 5. Tədbirlər (ROL TAB 3) ════════════════════

create table public.events (
  id text primary key default public.ulid(),
  title text not null,
  category text not null,
  city text not null default 'Bakı',
  location text not null default '',
  location_geo geography(Point, 4326),
  epoch_day bigint not null,                 -- keçmiş filtri (A6)
  event_time text not null,                  -- '18:00'
  event_date date not null,
  capacity int not null default 10 check (capacity > 0),
  description text not null default '',
  price_azn int,                            -- null = pulsuz
  age_limit text not null default 'Hamı üçün',
  language text not null default 'Azərbaycan dili',
  level text not null default 'Hamı üçün',
  format text not null default 'Oflayn' check (format in ('Oflayn','Onlayn')),
  approval boolean not null default false,
  recurring text not null default 'Bir dəfəlik',
  tags jsonb not null default '[]',
  image text not null default 'friends',     -- ImageKey
  host_profile_id uuid not null references public.profiles(id) on delete cascade,
  venue_id text references public.venues(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index events_location_gist on public.events using gist (location_geo);
create index events_epoch_day_idx on public.events (epoch_day);
create index events_city_idx on public.events (city, epoch_day);
create index events_host_idx on public.events (host_profile_id);

create trigger events_touch_updated_at
  before update on public.events
  for each row execute function public.fn_touch_updated_at();

create table public.event_participants (
  event_id text not null references public.events(id) on delete cascade,
  profile_id uuid not null references public.profiles(id) on delete cascade,
  status text not null default 'JOINED' check (status in ('PENDING','JOINED','DECLINED')),
  joined_at timestamptz not null default now(),
  checked_in_at timestamptz,
  checkin_xp_awarded boolean not null default false,
  primary key (event_id, profile_id)
);
create index event_participants_profile_idx on public.event_participants (profile_id);

-- ═══════════════════════════ 6. Rezervlər (ROL E) ════════════════════════

create table public.reservations (
  id text primary key default public.ulid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  venue_id text not null references public.venues(id) on delete cascade,
  event_id text references public.events(id) on delete set null,
  resv_date date not null,
  resv_time text not null,
  party int not null check (party > 0),
  note text not null default '',
  coupon_id text references public.user_coupons(id) on delete set null,
  status text not null default 'PENDING' check (status in (
    'PENDING','CONFIRMED','USED','CANCELLED','NO_SHOW','REJECTED')),
  qr_code text not null unique,             -- 'QSH-r-<ulid>' — server-generated (N1)
  share_token text unique,                  -- N8: qrupla paylaşma
  reminder_24h_sent_at timestamptz,         -- M4 idempotent job
  reminder_2h_sent_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index reservations_profile_idx on public.reservations (profile_id, status);
create index reservations_venue_idx on public.reservations (venue_id, resv_date);
create index reminders_idx on public.reservations (resv_date, resv_time)
  where status in ('PENDING','CONFIRMED');

create trigger reservations_touch_updated_at
  before update on public.reservations
  for each row execute function public.fn_touch_updated_at();

-- N8: paylaşma linki ilə "Qoşul" iddiaları
create table public.share_claims (
  id text primary key default public.ulid(),
  reservation_id text not null references public.reservations(id) on delete cascade,
  claimed_by uuid not null references public.profiles(id) on delete cascade,
  claimed_at timestamptz not null default now(),
  unique (reservation_id, claimed_by)
);

-- ═══════════════════════════ 7. Sosial (ROL D) ═══════════════════════════

-- Match cütləri — a_id < b_id kanonik (unikallıq)
create table public.matches (
  id text primary key default public.ulid(),
  a_id uuid not null references public.profiles(id) on delete cascade,
  b_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  check (a_id < b_id),
  unique (a_id, b_id)
);
create index matches_a_idx on public.matches (a_id);
create index matches_b_idx on public.matches (b_id);

create table public.swipes (
  id text primary key default public.ulid(),
  swiper_id uuid not null references public.profiles(id) on delete cascade,
  target_id uuid not null references public.profiles(id) on delete cascade,
  action text not null check (action in ('LIKE','PASS')),
  day date not null default current_date,
  xp_awarded boolean not null default false,   -- D-074: hərəkət limitsiz, XP gündə max 3
  created_at timestamptz not null default now(),
  unique (swiper_id, target_id, day)
);
create index swipes_swiper_idx on public.swipes (swiper_id, day);

create table public.blocks (
  blocker_id uuid not null references public.profiles(id) on delete cascade,
  blocked_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  check (blocker_id <> blocked_id)
);

-- Tədbir rəyləri (B4: moderasiyalı — PENDING → APPROVED → XP)
create table public.reviews (
  id text primary key default public.ulid(),
  event_id text not null references public.events(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  rating int not null check (rating between 1 and 5),
  body text not null default '',
  status text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED')),
  xp_awarded boolean not null default false,
  created_at timestamptz not null default now(),
  moderated_at timestamptz
);
create index reviews_event_idx on public.reviews (event_id, status);

-- ═══════════════════════════ 8. Söhbət (N6 rate limit) ══════════════════

create table public.messages (
  id text primary key default public.ulid(),
  kind text not null check (kind in ('EVENT','PERSONAL')),
  event_id text references public.events(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  recipient_id uuid references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 2000),
  created_at timestamptz not null default now(),
  check ((kind = 'EVENT' and event_id is not null) or
         (kind = 'PERSONAL' and recipient_id is not null))
);
create index messages_event_idx on public.messages (event_id, created_at);
create index messages_personal_idx on public.messages (sender_id, recipient_id, created_at);
create index messages_created_idx on public.messages (created_at);

-- ═══════════════════════════ 9. Moderasiya (ROL I) ═══════════════════════

create table public.reports (
  id text primary key default public.ulid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  target_type text not null check (target_type in ('USER','EVENT','VENUE','REVIEW')),
  target_profile_id uuid references public.profiles(id) on delete cascade,
  target_event_id text references public.events(id) on delete cascade,
  target_venue_id text references public.venues(id) on delete set null,
  target_review_id text references public.reviews(id) on delete set null,
  reason text not null,
  note text not null default '',
  status text not null default 'OPEN' check (status in ('OPEN','RESOLVED','DISMISSED')),
  created_at timestamptz not null default now()
);
create index reports_target_idx on public.reports (target_type, status);
create index reports_reporter_idx on public.reports (reporter_id);

create table public.admin_actions (
  id text primary key default public.ulid(),
  actor_id uuid not null references public.profiles(id) on delete cascade,
  action text not null,
  target_type text not null,
  target_id text not null,
  note text not null default '',
  created_at timestamptz not null default now()
);
comment on table public.admin_actions is 'N9: moderator/admin audit jurnalı (M4 panel API oxuyur).';

-- Dəvət zənciri (B3: XP birinci check-in-dən SONRA)
create table public.invites (
  code text primary key,                      -- 'QSH-INV-<ulid12>'
  inviter_id uuid not null references public.profiles(id) on delete cascade,
  invitee_id uuid references public.profiles(id) on delete set null,
  registered_at timestamptz,
  first_checkin_at timestamptz,
  xp_awarded boolean not null default false,
  created_at timestamptz not null default now()
);
create index invites_invitee_idx on public.invites (invitee_id);

-- ═════════════════════════ 10. Infra (N6 / ROL H / M4) ═══════════════════

create table public.rate_limit_buckets (
  bucket_key text primary key,               -- 'msg:<profile>:<saat-pəncərəsi>'
  window_start timestamptz not null,
  window_end timestamptz not null,
  hit_count int not null default 0,
  updated_at timestamptz not null default now()
);
comment on table public.rate_limit_buckets is
  'N6: mesaj/spam rate limit — yalnız fn_send_message (0002) toxunur.';

create table public.notifications (
  id text primary key default public.ulid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  notif_type text not null check (notif_type in (
    'MATCH','MESSAGE','EVENT_JOIN','EVENT_APPROVAL','EVENT_REMINDER',
    'RESV_UPDATE','RESV_REMINDER','LEVEL_UP','COUPON_EXPIRING',
    'NEARBY_EVENT','MARKETING')),
  title text not null,
  body text not null,
  read boolean not null default false,
  created_at timestamptz not null default now()
);
create index notifications_profile_idx on public.notifications (profile_id, created_at desc);

-- FCM cihaz tokenləri (M4 push üçün; M1-də cədvəl hazır)
create table public.push_tokens (
  id text primary key default public.ulid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  token text not null,
  platform text not null default 'android' check (platform in ('android','ios','web')),
  updated_at timestamptz not null default now(),
  unique (profile_id, token)
);

-- ═════════════════════════════════════════════════════════════════════════
-- RLS siyasətləri
-- ═════════════════════════════════════════════════════════════════════════

alter table public.profiles enable row level security;
alter table public.notification_settings enable row level security;
alter table public.categories enable row level security;
alter table public.user_interests enable row level security;
alter table public.venues enable row level security;
alter table public.venue_slots enable row level security;
alter table public.venue_reviews enable row level security;
alter table public.xp_events enable row level security;
alter table public.levels enable row level security;
alter table public.venue_xp_accounts enable row level security;
alter table public.venue_xp_ledger enable row level security;
alter table public.xp_card_nonces enable row level security;
alter table public.coupons_offers enable row level security;
alter table public.user_coupons enable row level security;
alter table public.events enable row level security;
alter table public.event_participants enable row level security;
alter table public.reservations enable row level security;
alter table public.share_claims enable row level security;
alter table public.matches enable row level security;
alter table public.swipes enable row level security;
alter table public.blocks enable row level security;
alter table public.reviews enable row level security;
alter table public.messages enable row level security;
alter table public.reports enable row level security;
alter table public.admin_actions enable row level security;
alter table public.invites enable row level security;
alter table public.rate_limit_buckets enable row level security;
alter table public.notifications enable row level security;
alter table public.push_tokens enable row level security;

-- Köməkçi: idarəçi rolu
create or replace function public.is_staff() returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce(
    (select p.role in ('MODERATOR','ADMIN') from public.profiles p where p.id = auth.uid()),
    false);
$$;

-- ── profiles: B7 xəritə görünüşü SERVER-də zorlanır ───────────────────────
create policy profiles_select on public.profiles for select to authenticated
using (
  id = auth.uid()
  or (
    (restricted_until is null or restricted_until <= now())
    and map_visibility <> 'OFF'
    and (map_visibility = 'EVERYONE' or exists (
      select 1 from public.matches m
      where (m.a_id = profiles.id and m.b_id = auth.uid())
         or (m.b_id = profiles.id and m.a_id = auth.uid())))
    and not exists (
      select 1 from public.blocks b
      where (b.blocker_id = profiles.id and b.blocked_id = auth.uid())
         or (b.blocked_id = profiles.id and b.blocker_id = auth.uid()))
  )
);
create policy profiles_update_self on public.profiles for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid() and role = (select role from public.profiles where id = auth.uid()));
-- insert: signup trigger yaradır (0002 fn_ensure_profile); istifadəçi birbaşa insert edə bilmir.

create policy notif_settings_rw on public.notification_settings for all to authenticated
  using (profile_id = auth.uid()) with check (profile_id = auth.uid());

-- Taksonomiya: hamı oxuyur
create policy categories_select on public.categories for select to authenticated using (true);
create policy levels_select on public.levels for select to authenticated using (true);
create policy user_interests_rw on public.user_interests for all to authenticated
  using (profile_id = auth.uid()) with check (profile_id = auth.uid());

-- Obyektlər: oxu hamı (anon daxil), yazı partnyor/staff
create policy venues_select on public.venues for select using (true);
create policy venues_write on public.venues for all to authenticated
  using (managed_by = auth.uid() or public.is_staff())
  with check (managed_by = auth.uid() or public.is_staff());
create policy venue_slots_select on public.venue_slots for select using (true);
create policy venue_slots_write on public.venue_slots for all to authenticated
  using (exists (select 1 from public.venues v where v.id = venue_id and (v.managed_by = auth.uid() or public.is_staff())))
  with check (exists (select 1 from public.venues v where v.id = venue_id and (v.managed_by = auth.uid() or public.is_staff())));
create policy venue_reviews_select on public.venue_reviews for select using (true);
create policy venue_reviews_insert on public.venue_reviews for insert to authenticated with check (author_id = auth.uid());
create policy venue_reviews_moderate on public.venue_reviews for update to authenticated
  using (author_id = auth.uid() or public.is_staff()) with check (true);

-- XP cədvəlləri: oxu sahib; yazı YOXDUR (yalnız funksiyalar — aşağıda revoke)
create policy xp_events_select on public.xp_events for select to authenticated using (profile_id = auth.uid());
create policy venue_xp_accounts_select on public.venue_xp_accounts for select to authenticated using (profile_id = auth.uid());
create policy venue_xp_ledger_select on public.venue_xp_ledger for select to authenticated
  using (exists (select 1 from public.venue_xp_accounts a where a.id = account_id and a.profile_id = auth.uid()));
create policy xp_card_nonces_select on public.xp_card_nonces for select to authenticated using (profile_id = auth.uid());

-- Kuponlar
create policy offers_select on public.coupons_offers for select using (true);
create policy offers_write on public.coupons_offers for all to authenticated
  using (exists (select 1 from public.venues v where v.id = venue_id and (v.managed_by = auth.uid() or public.is_staff())))
  with check (exists (select 1 from public.venues v where v.id = venue_id and (v.managed_by = auth.uid() or public.is_staff())));
create policy user_coupons_select on public.user_coupons for select to authenticated using (profile_id = auth.uid());

-- Tədbirlər
create policy events_select on public.events for select to authenticated using (true);
create policy events_insert on public.events for insert to authenticated with check (host_profile_id = auth.uid());
create policy events_write on public.events for all to authenticated
  using (host_profile_id = auth.uid() or public.is_staff()) with check (host_profile_id = auth.uid() or public.is_staff());
create policy event_participants_select on public.event_participants for select to authenticated
  using (profile_id = auth.uid()
    or exists (select 1 from public.events e where e.id = event_id and e.host_profile_id = auth.uid()));
create policy event_participants_insert on public.event_participants for insert to authenticated
  with check (profile_id = auth.uid());

-- Rezervlər: sahib + obyekt meneceri
create policy reservations_select on public.reservations for select to authenticated
  using (profile_id = auth.uid()
    or exists (select 1 from public.venues v where v.id = venue_id and (v.managed_by = auth.uid() or public.is_staff())));
create policy share_claims_select on public.share_claims for select to authenticated
  using (claimed_by = auth.uid()
    or exists (select 1 from public.reservations r join public.venues v on v.id = r.venue_id
               where r.id = reservation_id and (v.managed_by = auth.uid() or public.is_staff())));

-- Sosial
create policy matches_select on public.matches for select to authenticated using (a_id = auth.uid() or b_id = auth.uid());
create policy swipes_select on public.swipes for select to authenticated using (swiper_id = auth.uid());
create policy blocks_select on public.blocks for select to authenticated using (blocker_id = auth.uid() or blocked_id = auth.uid());
create policy blocks_insert on public.blocks for insert to authenticated with check (blocker_id = auth.uid());
create policy blocks_delete on public.blocks for delete to authenticated using (blocker_id = auth.uid());
create policy reviews_select on public.reviews for select to authenticated
  using (author_id = auth.uid() or public.is_staff()
    or exists (select 1 from public.event_participants ep where ep.event_id = reviews.event_id and ep.profile_id = auth.uid() and ep.status = 'JOINED' and ep.checked_in_at is not null));
create policy reviews_insert on public.reviews for insert to authenticated with check (author_id = auth.uid());

-- Söhbət
create policy messages_select on public.messages for select to authenticated
  using (
    sender_id = auth.uid() or recipient_id = auth.uid()
    or exists (select 1 from public.event_participants ep where ep.event_id = messages.event_id and ep.profile_id = auth.uid() and ep.status = 'JOINED'));

-- Moderasiya
create policy reports_insert on public.reports for insert to authenticated with check (reporter_id = auth.uid());
create policy reports_select on public.reports for select to authenticated using (public.is_staff());
create policy reports_update on public.reports for update to authenticated using (public.is_staff()) with check (true);
create policy admin_actions_select on public.admin_actions for select to authenticated using (public.is_staff());

-- Dəvətlər
create policy invites_select on public.invites for select to authenticated using (inviter_id = auth.uid() or invitee_id = auth.uid());

-- rate_limit_buckets: istifadəçiyə birbaşa giriş YOXDUR (policy yoxdur = deny)

-- Bildirişlər + push
create policy notifications_select on public.notifications for select to authenticated using (profile_id = auth.uid());
create policy notifications_update on public.notifications for update to authenticated using (profile_id = auth.uid()) with check (profile_id = auth.uid());
create policy push_tokens_all on public.push_tokens for all to authenticated
  using (profile_id = auth.uid()) with check (profile_id = auth.uid());

-- XP-həssas cədvəllərdə birbaşa DML-i tam bağla (qayda 8: yalnız server hesablayır)
revoke insert, update, delete on public.xp_events from authenticated;
revoke insert, update, delete on public.venue_xp_accounts from authenticated;
revoke insert, update, delete on public.venue_xp_ledger from authenticated;
revoke insert, update, delete on public.xp_card_nonces from authenticated;
revoke insert, update, delete on public.rate_limit_buckets from authenticated;
revoke insert, update, delete on public.venue_xp_accounts from anon;
revoke insert, update, delete on public.xp_events from anon;
revoke insert, update, delete on public.venue_xp_ledger from anon;
revoke insert, update, delete on public.xp_card_nonces from anon;
