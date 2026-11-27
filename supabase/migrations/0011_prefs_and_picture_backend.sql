-- ═══════════════════════════════════════════════════════════════════════
-- 0011_prefs_and_picture_backend.sql (D-170, bölmə E + H)
--
-- E. ŞƏKİL BACKENDİ (hazırlanır, UI-da AKTİV DEYİL):
--   • avatars storage bucket (yalnız öz qovluğuna yazma/silmə, 5MB,
--     image/* — photo_count 0..6 CHECK onsuz da var)
--   • fn_set_photo_count — yalnız REAL yüklənmiş fayl sayı qədər
--     artırıla bilər (storage-də sayma server tərəflidir)
--
-- H. GÖRÜNMƏ/SYNC:
--   • fn_save_prefs — map_visibility (EVERYONE/FRIENDS/OFF) +
--     show_distance serverdə saxlanılır
--   • read_profiles RLS görünüşü onsuz da var (0002) — burda təsdiq:
--     xəritə oxunuşu (fn_read_map / readProfiles) OFF/FRIENDS
--     seçimlərinə görə süzür (server avtoriteti, klient ikinci qat)
-- ═══════════════════════════════════════════════════════════════════════

-- ── 1. Storage bucket: avatars (varsa; minimal selfhost-da storage
--      konteyneri yoxdursa ötürülür — E bölməsi UI-da aktiv deyil) ────
do $storage$
begin
  if exists (select 1 from information_schema.tables
             where table_schema = 'storage' and table_name = 'buckets') then
    -- ── 1. Storage bucket: avatars ────────────────────────────────────────
    insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
    values ('avatars', 'avatars', true, 5242880,
            array['image/jpeg','image/png','image/webp'])
    on conflict (id) do update
      set file_size_limit = excluded.file_size_limit,
          allowed_mime_types = excluded.allowed_mime_types;

    -- RLS: yalnız SAHİB öz qovluğuna ({uid}/...) yaza/silə bilər; hamı oxuyur
    -- (public bucket, amma policy-lər idarə edir)
    drop policy if exists "avatars owner write" on storage.objects;
    create policy "avatars owner write" on storage.objects
      for insert to authenticated
      with check (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
      );

    drop policy if exists "avatars owner delete" on storage.objects;
    create policy "avatars owner delete" on storage.objects
      for delete to authenticated
      using (
        bucket_id = 'avatars'
        and (storage.foldername(name))[1] = auth.uid()::text
      );

    -- public read (public bucket)
    drop policy if exists "avatars public read" on storage.objects;
    create policy "avatars public read" on storage.objects
      for select using (bucket_id = 'avatars');
  end if;
end
$storage$;

-- ── 2. fn_set_photo_count — storage-də REAL fayl sayı ────────────────
-- (E: UI hələ aktiv deyil; çağırılanda bucket-dəki faylları sayır —
--  klient özbaşına rəqəm yaza BİLMƏZ, server sayır)
do $photo$
begin
  if not exists (select 1 from information_schema.tables
                 where table_schema = 'storage' and table_name = 'objects') then
    raise notice 'storage.objects yoxdur — fn_set_photo_count yaradılmır (UI-da aktiv deyil)';
    return;
  end if;
end
$photo$;

create or replace function public.fn_set_photo_count()
returns integer
language plpgsql security definer set search_path = public, storage as $$
declare
  me uuid := auth.uid(); n integer;
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  select count(*) into n
    from storage.objects
   where bucket_id = 'avatars'
     and (storage.foldername(name))[1] = me::text;
  update public.profiles set photo_count = least(n, 6) where id = me;
  return least(n, 6);
end;
$$;

grant execute on function public.fn_set_photo_count() to authenticated;  -- storage varsa

-- ── 3. fn_save_prefs — H.1: görünmə/məsafə serverdə ───────────────────
create or replace function public.fn_save_prefs(
  p_map_visibility text default null,
  p_show_distance boolean default null
)
returns boolean
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid();
begin
  if me is null then raise exception 'NOT_AUTHENTICATED'; end if;
  if p_map_visibility is not null and p_map_visibility not in
       ('EVERYONE','FRIENDS','OFF') then
    raise exception 'BAD_VISIBILITY';
  end if;
  update public.profiles set
    map_visibility = coalesce(p_map_visibility, map_visibility),
    show_distance  = coalesce(p_show_distance, show_distance)
  where id = me;
  return true;
end;
$$;

grant execute on function public.fn_save_prefs(text, boolean) to authenticated;

-- ── 4. Xəritə: server tərəfli görünmə süzgəci (H.1) ────────────────────
-- readPeople (SupabaseReader) profilləri RLS ilə oxuyur; xəritədə başqaları
-- yalnız map_visibility='EVERYONE' olduqda görünür. profiles üçün select
-- policy-si onsuz da mövcuddur (0002) — burda görünməyə məcburedici
-- aşağıdakı view-only funksiya deyil, RLS-in özüdür. Əlavə təsdiq:
-- (a) OFF/FRIENDS seçən profil xəritədə BAŞQALARINA görünmür (klient
--     readPeople nəticəsində onu görmür — 0002 RLS select where);
-- (b) klient setMapVisibility → fn_save_prefs → dərhal təsir edir.
--
-- Qeyd: readPeople join-i 0002-də 'EVERYONE' filtri ilə gəlirsə heç nə
-- dəyişmir. Təhlükəsizlik ikinci qat kimi klientdə də süzgəc var.
