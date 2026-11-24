-- ═══════════════════════════════════════════════════════════════════════════
-- Qoshul.az — 0005: fn_match_candidates tip uyğunsuzluğu (F17, 2026-11-24)
--
-- XƏTA: PostgREST-dən hər çağırış 42804 ilə partlayırdı:
--   {"code":"42804","message":"structure of query does not match function
--    result type","details":"Returned type double precision does not match
--    expected type numeric in column 4."}
-- SƏBƏB: st_distance() double precision qaytarır, funksiyanın RETURNS TABLE
-- sütunu distance_m numeric gözləyirdi (0002:933-dən bəri). PL/pgSQL-in
-- return query uyğunluq yoxlaması yalnız İCRA ZAMANI düşür — DDL uğurlu
-- keçirdi, funksiya heç vaxt canlı çağırılmadığı üçün xəta gizli qaldı.
-- HƏLL: st_distance(...)::numeric — davranış dəyişmir (numeric ← double
-- lossless çevrilir; JS tərəfində əvvəl də JSON number idi).
--
-- Test (2026-11-24, canlı server):
--   POST /rest/v1/rpc/fn_match_candidates {"p_radius_m":50000}
--   → 200, mesafə-artsı sıralı namizədlər.
-- Idempotent: create or replace (deploy-larda təhlükəsiz təkrar).
-- ═══════════════════════════════════════════════════════════════════════════

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
           st_distance(my_geo, p.last_location)::numeric   -- F17: double → numeric
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

-- grant 0004-də var (whitelist), təkrar zərərsizdir:
grant execute on function public.fn_match_candidates(int) to authenticated;
