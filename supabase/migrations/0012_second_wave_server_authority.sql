-- ═══════════════════════════════════════════════════════════════════════
-- 0012_second_wave_server_authority.sql (D-175..D-179, ikinci dalğa)
--
-- D-175 AI SERVER AXINI: fn_assistant_reply — „Qoşa“ köməkçisinin cavabı
--   SERVERDƏ yığılır (tədbir sorğuları + [TƏDBİR] JSON təklifi). Klient
--   tərəfli şablon generatoru silindi (qayda 8: məntiq serverdə).
--
-- D-176 MATCH AVTORİTETİ: read_swipes artıq TARGET LIKE-larını da qaytarır
--   (RPS-safe); fn_request_friend / fn_respond_friend — dostluq RPC-ləri.
--   fn_record_swipe öz imzasını qoruyur (matched/awarded_xp) — matched
--   bayrağı klientdə YEGƏNƏ match qərarıdır (likesBack modeli silindi).
--
-- D-177 VENUE IMAGE_URL: venues.image_url (nullable, full URL) — paket
--   şəkil açarından (venues.image, asset) ayrıqı. Seed-lərdə NULL.
--
-- D-178 VENUE RƏY RPC: fn_add_venue_review — məkan rəyi serverə yazılır
--   (PENDING status, moderasiyadan sonra görünür; ulduz 1-5 CHECK onsuz
--   da var). Klientdə lokal rəy siyahısı mənbəyi ləğv.
--
-- D-179 AI TÖVSİYƏ SKORU: fn_recommend_events — „AI seçimi“ zolağının
--   skorlaması SERVERDƏ (maraq uyğunluğu + yer qalığı; 2-bit lokal skor
--   silindi). RPC olmayanda zolaq gizlədilir (klient fallback yoxdur).
-- ═══════════════════════════════════════════════════════════════════════

-- ── D-177: venues.image_url (nullable full URL; NULL → kateqoriya ikonlu
--    placeholder göstərilir — brend asset açarı venues.image-da qalır) ──
alter table public.venues
  add column if not exists image_url text;

comment on column public.venues.image_url is
  'D-177: tam URL (https://...) — paket asset açarı (image) deyil. NULL = kateqoriya ikonlu placeholder. Seed-lər üçün NULL.';

-- ── D-175: fn_assistant_reply — „Qoşa“ server cavabı ──────────────────
-- Giriş: p_query (istifadəçi sualı). Çıxış: reply text — client tərəfində
-- [TƏDBİR]{...} bloku parse olunur (parseAssistantEvent, Models.kt).
-- Məntiq köhnə assistantReplyFor Kotlin portunun SQL qarşılığıdır:
--   • „fikir/təklif/plan/şənbə/bazar/təşkil“ → gələcək şənbə üçün
--     [TƏDBİR] JSON təklifi (Yürüş kateqoriyası, Qala qalası);
--   • „pulsuz/ödənişsiz“ → şəhərdəki pulsuz tədbirlər (max 3);
--   • „idman/spor“ → idman/təbiət qrupu tədbirləri (max 3);
--   • əks halda → tövsiyələr (şəhər tədbirlərindən max 2).
create or replace function public.fn_assistant_reply(
  p_query text
) returns text
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  my_city text;
  q text := lower(coalesce(p_query, ''));
  wants_proposal boolean;
  free_rows record;
  sport_rows record;
  rec_rows record;
  parts text;
  proposal jsonb;
  next_saturday date;
  d date;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;

  select city into my_city from public.profiles where id = me;

  wants_proposal := q ~* '(fikri|fikir|təklif|teklif|plan|şənbə|senbe|bazar|təşkil|teskil)';

  if wants_proposal then
    -- gələcək şənbə (bugündən sonra ilk şənbə; məhdud dövr qoruması)
    d := current_date;
    for i in 1..8 loop
      exit when extract(dow from d) = 6 and d > current_date;
      d := d + 1;
    end loop;
    if not (extract(dow from d) = 6 and d > current_date) then
      d := current_date + 7;  -- məntiq xətası halında təhlükəsiz fallback
    end if;
    next_saturday := d;
    proposal := jsonb_build_object(
      'title', 'Şənbə səhəri Qala yürüşü',
      'category', 'Yürüş',
      'date', to_char(next_saturday, 'YYYY-MM-DD'),
      'time', '09:00',
      'capacity', 10,
      'location', 'Qala qalası, Bakı',
      'description', 'Qala qalası ətrafında 5 km asan yürüş + qala divarlarında fotopauza. Sonrasında yaxınlıqdakı kafedə çay.',
      'partner', ''
    );
    return 'Şənbə üçün hazırladığım plan: səhər yürüş, sonra birlikdə çay. ' ||
           'Təklifimi bir düymə ilə tədbirə çevirə bilərsən.' || chr(10) ||
           '[TƏDBİR]' || proposal::text;
  end if;

  if q ~* '(pulsuz|ödənişsiz|odenisiz)' then
    parts := '';
    for free_rows in
      select title, date_label, event_time, location
      from (
        select e.title,
               to_char(e.event_date, 'DD.MM.YYYY') as date_label,
               e.event_time, e.location
        from public.events e
        where e.price_azn is null
          and e.epoch_day >= (extract(epoch from current_date)/86400)::bigint
          and (my_city is null or my_city = '' or e.city = my_city)
        order by e.epoch_day asc
        limit 3
      ) t
    loop
      parts := parts || '• ' || free_rows.title || ' — ' || free_rows.date_label ||
               ', ' || free_rows.event_time || ' (' || free_rows.location || ')' || chr(10);
    end loop;
    if parts = '' then
      return 'Bu şəhərdə pulsuz tədbir yoxdur — sənin üçün birini təşkil edim! ' ||
             'Məsələn, parkda səhər qaçışı və ya bulvarda yürüş planlaya bilərəm. ' ||
             '"Fikir al" yaz, hazırlayım.';
    end if;
    return 'Pulsuz tədbirlər bunlardır:' || chr(10) || chr(10) ||
           rtrim(parts, chr(10)) || chr(10) || chr(10) ||
           'Hansını seçsən, dərhal qoşulmağını təşkil edirəm.';
  end if;

  if q ~* '(idman|spor)' then
    parts := '';
    for sport_rows in
      select title, date_label, event_time, participants, capacity
      from (
        select e.title,
               to_char(e.event_date, 'DD.MM.YYYY') as date_label,
               e.event_time, e.capacity,
               (select count(*)::int from public.event_participants ep
                 where ep.event_id = e.id and ep.status = 'JOINED') as participants
        from public.events e
        join public.categories c on c.title = e.category
        where c.group_title in ('İdman', 'Təbiət')
          and e.epoch_day >= (extract(epoch from current_date)/86400)::bigint
        order by e.epoch_day asc
        limit 3
      ) t
    loop
      parts := parts || '• ' || sport_rows.title || ' — ' || sport_rows.date_label ||
               ' ' || sport_rows.event_time || ', ' || sport_rows.participants ||
               '/' || sport_rows.capacity || ' nəfər' || chr(10);
    end loop;
    if parts = '' then
      return 'Aktiv idman tədbiri tapmadım — sənin üçün idman görüşü quraşdırım! ' ||
             'Padel, qaçış və ya velosiped — hansı maraqlıdır? Dey, dəvətnaməni hazırlayım.';
    end if;
    return 'Aktiv idman tədbirləri:' || chr(10) || chr(10) || rtrim(parts, chr(10));
  end if;

  -- default: şəhər tövsiyələri (max 2)
  parts := '';
  for rec_rows in
    select title, category, date_label, event_time
    from (
      select e.title, e.category,
             to_char(e.event_date, 'DD.MM.YYYY') as date_label,
             e.event_time
      from public.events e
      where e.epoch_day >= (extract(epoch from current_date)/86400)::bigint
        and (my_city is null or my_city = '' or e.city = my_city)
      order by e.epoch_day asc
      limit 2
    ) t
  loop
    parts := parts || '• ' || rec_rows.title || ' (' || rec_rows.category || ') — ' ||
             rec_rows.date_label || ' ' || rec_rows.event_time || chr(10);
  end loop;
  if parts = '' then
    return 'Bu şəhərdə hələ tədbir yoxdur — birlikdə ilkini təşkil edək! ' ||
           'Maraqlarını yaz (məs. "futbol, qəhvə"), mən də tədbiri qurub dəvətnamə göndərim.';
  end if;
  return 'Sənin üçün tövsiyələrim:' || chr(10) || chr(10) ||
         rtrim(parts, chr(10)) || chr(10) || chr(10) ||
         'Hər hansı birini seçsən, qoşulmanı dərhal edərəm. "Sənin üçün" bölməsindəki AI seçimlərinə də bax.';
end;
$$;

grant execute on function public.fn_assistant_reply(text) to authenticated;

-- ── D-179: fn_recommend_events — AI seçimi skoru serverdə ─────────────
-- Skor: hər maraq qrupu üçün +2 (maraq uyğunluğu), yer qalıbsa +1.
-- Nəticə: (event_id, score) — maksimum 3, skor azalan sırada.
create or replace function public.fn_recommend_events()
returns table(event_id text, score int)
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;

  return query
  with my_groups as (
    select distinct c.group_title
    from public.user_interests ui
    join public.categories c on c.slug = ui.category_slug
    where ui.profile_id = me
  ),
  scored as (
    select e.id as eid,
           (case when exists (
              select 1 from public.categories c2
              where c2.title = e.category
                and c2.group_title in (select group_title from my_groups)
            ) then 2 else 0 end
            + case when (select count(*) from public.event_participants ep
                         where ep.event_id = e.id and ep.status = 'JOINED')
                   < e.capacity then 1 else 0 end) as sc
    from public.events e
    where e.epoch_day >= (extract(epoch from current_date)/86400)::bigint
  )
  select eid, sc
  from scored
  where sc > 0
  order by sc desc, eid
  limit 3;
end;
$$;

grant execute on function public.fn_recommend_events() to authenticated;

-- ── D-176: dostluq RPC-ləri — friendships cədvəli (0001-də yox idi!) ──
create table if not exists public.friendships (
  requester_id uuid not null references public.profiles(id) on delete cascade,
  addressee_id uuid not null references public.profiles(id) on delete cascade,
  status text not null default 'PENDING'
    check (status in ('PENDING','ACCEPTED','DECLINED')),
  created_at timestamptz not null default now(),
  primary key (requester_id, addressee_id)
);
create index if not exists friendships_addressee_idx
  on public.friendships (addressee_id, status);

alter table public.friendships enable row level security;

drop policy if exists friendships_select on public.friendships;
create policy friendships_select on public.friendships
  for select to authenticated
  using (requester_id = auth.uid() or addressee_id = auth.uid());

-- Yalnız RPC-lər yazır (birbaşa DML yoxdur — qayda 8)
revoke insert, update, delete on public.friendships from authenticated;

comment on table public.friendships is
  'D-176: dostluq sorğu/cavabı. Yazmaq yalnız fn_request_friend / fn_respond_friend.';

-- fn_request_friend — sorğu göndər (PENDING; özünə/blokluya/əvvəl varsa rədd)
create or replace function public.fn_request_friend(
  p_target_id uuid
) returns table(ok boolean, error text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if p_target_id = me then
    return query select false, 'SELF_REQUEST'::text; return;
  end if;
  if exists (select 1 from public.blocks b
             where (b.blocker_id = p_target_id and b.blocked_id = me)
                or (b.blocker_id = me and b.blocked_id = p_target_id)) then
    return query select false, 'BLOCKED'::text; return;
  end if;
  if exists (select 1 from public.friendships f
             where (f.requester_id = me and f.addressee_id = p_target_id)
                or (f.requester_id = p_target_id and f.addressee_id = me)) then
    return query select false, 'ALREADY_EXISTS'::text; return;
  end if;
  insert into public.friendships (requester_id, addressee_id, status)
  values (me, p_target_id, 'PENDING');
  insert into public.notifications (profile_id, notif_type, title, body)
  values (p_target_id, 'MATCH', 'Dostluq sorğusu',
          'Yeni dostluq sorğusu aldıq — cavab ver!');
  return query select true, 'OK'::text;
end;
$$;

grant execute on function public.fn_request_friend(uuid) to authenticated;

-- fn_respond_friend — sorğuya cavab (ACCEPTED/DECLINED; yalnız addressee)
create or replace function public.fn_respond_friend(
  p_requester_id uuid,
  p_accept boolean
) returns table(ok boolean, error text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if not exists (select 1 from public.friendships f
                 where f.requester_id = p_requester_id
                   and f.addressee_id = me and f.status = 'PENDING') then
    return query select false, 'NOT_FOUND'::text; return;
  end if;
  update public.friendships
    set status = case when p_accept then 'ACCEPTED' else 'DECLINED' end
    where requester_id = p_requester_id and addressee_id = me;
  if p_accept then
    insert into public.notifications (profile_id, notif_type, title, body)
    values (p_requester_id, 'MATCH', 'Dostluq qəbul edildi',
            'Dostluq sorğunuz qəbul olundu — söhbəti başladın!');
  end if;
  return query select true, 'OK'::text;
end;
$$;

grant execute on function public.fn_respond_friend(uuid, boolean) to authenticated;

-- ── D-176: read swipes — LIKE targets da görünür (klient bərpası üçün) ──
-- RLS swipes_select yalnız swiper_id=me qaytarırdı; match mexanikası üçün
-- hədəfin LIKE-ları lazımdır. Gücləndirilmiş oxu RPC (security definer):
create or replace function public.fn_read_swipes()
returns table(person_id uuid, interested boolean)
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  -- mənim hərəkətlərim (həm LIKE, həm PASS — kartın keçilib-keçilməməsi)
  return query
  select s.target_id, (s.action = 'LIKE')
  from public.swipes s
  where s.swiper_id = me;
end;
$$;

grant execute on function public.fn_read_swipes() to authenticated;

-- ── D-178: fn_add_venue_review — məkan rəyi serverə ───────────────────
create or replace function public.fn_add_venue_review(
  p_venue_id text, p_stars int, p_body text
) returns table(ok boolean, error text, review_id text)
language plpgsql security definer set search_path = public as $$
declare
  me uuid := auth.uid();
  v_id text;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select id into v_id from public.venues where id = p_venue_id;
  if v_id is null then
    return query select false, 'VENUE_NOT_FOUND'::text, null::text; return;
  end if;
  if p_stars is null or p_stars not between 1 and 5 then
    return query select false, 'INVALID_STARS'::text, null::text; return;
  end if;
  if exists (select 1 from public.venue_reviews
             where venue_id = p_venue_id and author_id = me) then
    return query select false, 'ALREADY_REVIEWED'::text, null::text; return;
  end if;
  insert into public.venue_reviews (venue_id, author_id, stars, text, status)
  values (p_venue_id, me, p_stars, coalesce(p_body, ''), 'PENDING')
  returning id into v_id;
  return query select true, 'OK'::text, v_id;
end;
$$;

grant execute on function public.fn_add_venue_review(text, int, text) to authenticated;
