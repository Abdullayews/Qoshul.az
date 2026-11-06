-- ═══════════════════════════════════════════════════════════════════════════
-- Qoshul.az M1 — seed (0003): səviyyələr, kateqoriyalar, obyektlər, təkliflər
-- Mənbə pariteti: FakeData.kt (main @ M0) — dəyərlər birebir köçürülüb.
-- Demo tədbirlər/bildirişlər SEED EDİLMİR: tarix-nisbilik (N10) serverdə
-- mənasızdır; tədbirlər istifadəçilər tərəfindən yaradılır (fn_create_event).
-- ═══════════════════════════════════════════════════════════════════════════

-- ── Səviyyələr (XpRules.rankNames + rankMax pariteti) ─────────────────────
insert into public.levels (rank_index, name, rank_max, badge) values
  (0, 'Yeni',        99,     ''),
  (1, 'Kəşfiyyatçı',  299,    ''),
  (2, 'Aktiv',        699,    ''),
  (3, 'Sosial',       1499,   ''),
  (4, 'Lider',        2999,   ''),
  (5, 'Əfsanə',       9999999, '')  -- terminal: yuxarı hədd yoxdur (göstərici nominal)
on conflict (rank_index) do nothing;

-- ── Kateqoriyalar (FakeData.categoryGroups — 12 qrup) ─────────────────────
insert into public.categories (slug, title, group_title, sort) values
  -- İdman
  ('futbol','Futbol','İdman',1), ('basketbol','Basketbol','İdman',2),
  ('voleybol','Voleybol','İdman',3), ('padel','Padel','İdman',4),
  ('tennis','Tennis','İdman',5), ('stolustu-tennis','Stolüstü tennis','İdman',6),
  ('uzguculuk','Üzgüçülük','İdman',7), ('qacis','Qaçış','İdman',8),
  ('velosiped','Velosiped','İdman',9), ('yoga','Yoga','İdman',10),
  ('fitnes','Fitnes','İdman',11), ('doyus-idmani','Döyüş idmanı','İdman',12),
  ('gules','Güləş','İdman',13), ('sahmat','Şahmat','İdman',14),
  ('boulinq','Boulinq','İdman',15),
  -- Təbiət
  ('yurus','Yürüş','Təbiət',1), ('kemping','Kemping','Təbiət',2),
  ('alpinizm','Alpinizm','Təbiət',3), ('baliqciliq','Balıqçılıq','Təbiət',4),
  ('piknik','Piknik','Təbiət',5), ('at-surme','At sürmə','Təbiət',6),
  ('qus-musahidesi','Quş müşahidəsi','Təbiət',7),
  -- Mədəniyyət
  ('muzey','Muzey','Mədəniyyət',1), ('teatr','Teatr','Mədəniyyət',2),
  ('kino','Kino','Mədəniyyət',3), ('sergi','Sərgi','Mədəniyyət',4),
  ('kitab-klubu','Kitab klubu','Mədəniyyət',5), ('poeziya-gecəsi','Poeziya gecəsi','Mədəniyyət',6),
  ('tarix-turu','Tarix turu','Mədəniyyət',7),
  -- Musiqi
  ('konsert','Konsert','Musiqi',1), ('cem-sessiya','Cem-sessiya','Musiqi',2),
  ('karaoke','Karaoke','Musiqi',3), ('mugam','Muğam','Musiqi',4),
  ('caz','Caz','Musiqi',5), ('dj-gecəsi','DJ gecəsi','Musiqi',6),
  ('musiqi-aləti-dərsi','Musiqi aləti dərsi','Musiqi',7),
  -- Yemək və içki
  ('qehve','Qəhvə','Yemək və içki',1), ('cay-sohbeti','Çay söhbəti','Yemək və içki',2),
  ('sam-yemeyi','Şam yeməyi','Yemək və içki',3), ('seher-yemeyi','Səhər yeməyi','Yemək və içki',4),
  ('kulinariya-dərsi','Kulinariya dərsi','Yemək və içki',5),
  ('dequstasiya','Dequstasiya','Yemək və içki',6), ('kuçə-yeməkləri','Küçə yeməkləri','Yemək və içki',7),
  -- Təhsil
  ('dil-mübadiləsi','Dil mübadiləsi','Təhsil',1), ('seminar','Seminar','Təhsil',2),
  ('mühazirə','Mühazirə','Təhsil',3), ('imtahana-hazirliq','İmtahana hazırlıq','Təhsil',4),
  ('birlikdə-dərs','Birlikdə dərs','Təhsil',5), ('elm-sohbeti','Elm söhbəti','Təhsil',6),
  -- Texnologiya
  ('hakaton','Hakaton','Texnologiya',1), ('proqramlaşdırma','Proqramlaşdırma','Texnologiya',2),
  ('süni-intellekt','Süni intellekt','Texnologiya',3), ('startap','Startap','Texnologiya',4),
  ('oyun-hazırlama','Oyun hazırlama','Texnologiya',5), ('dizayn','Dizayn','Texnologiya',6),
  ('robototexnika','Robototexnika','Texnologiya',7),
  -- Biznes və karyera
  ('networking','Networking','Biznes və karyera',1), ('karyera','Karyera','Biznes və karyera',2),
  ('sahibkarlıq','Sahibkarlıq','Biznes və karyera',3), ('marketinq','Marketinq','Biznes və karyera',4),
  ('maliyyə-savadlılığı','Maliyyə savadlılığı','Biznes və karyera',5),
  ('müsahibəyə-hazırlıq','Müsahibəyə hazırlıq','Biznes və karyera',6),
  -- Oyunlar
  ('stolüstü-oyunlar','Stolüstü oyunlar','Oyunlar',1), ('kibersport','Kibersport','Oyunlar',2),
  ('kviz','Kviz','Oyunlar',3), ('mafiya','Mafiya','Oyunlar',4), ('nerd','Nərd','Oyunlar',5),
  ('video-oyunlar','Video oyunlar','Oyunlar',6), ('escape-room','Escape room','Oyunlar',7),
  -- İncəsənət
  ('rəsm','Rəsm','İncəsənət',1), ('foto-gezinti','Foto-gəzinti','İncəsənət',2),
  ('əl-işləri','Əl işləri','İncəsənət',3), ('rəqs','Rəqs','İncəsənət',4),
  ('xalçaçılıq','Xalçaçılıq','İncəsənət',5), ('keramika','Keramika','İncəsənət',6),
  ('yazı-emalatxanası','Yazı emalatxanası','İncəsənət',7),
  -- Könüllülük
  ('təmizlik-aksiyası','Təmizlik aksiyası','Könüllülük',1), ('xeyriyyə','Xeyriyyə','Könüllülük',2),
  ('heyvanlara-yardım','Heyvanlara yardım','Könüllülük',3), ('ağac-əkmə','Ağac əkmə','Könüllülük',4),
  ('qan-donorluğu','Qan donorluğu','Könüllülük',5),
  -- Səyahət və digər
  ('şəhər-turu','Şəhər turu','Səyahət və digər',1), ('səfər','Səfər','Səyahət və digər',2),
  ('ailə','Ailə','Səyahət və digər',3), ('sağlamlıq','Sağlamlıq','Səyahət və digər',4),
  ('mənəvi-inkişaf','Mənəvi inkişaf','Səyahət və digər',5),
  ('valideynlər','Valideynlər','Səyahət və digər',6), ('tələbələr','Tələbələr','Səyahət və digər',7),
  ('digər','Digər','Səyahət və digər',8)
on conflict (slug) do nothing;

-- ── Obyektlər (FakeData.partners v1..v6 + initialVenueXp xp_config) ───────
insert into public.venues (
  id, name, category, address, city, deal, hours, image, price_range,
  auto_accept, rating, reviews_count, open_from, open_to, discount_percent,
  location, xp_per_azn, max_discount_percent, min_bill_azn)
values
  ('v1','Padel Arena','İdman','Böyük Şor şossesi 15','Bakı',
   'Səhər saatları -20%','08:00–12:00','padel','₼₼₼',
   false, 4.8, 34, 8, 23, 20,
   st_setsrid(st_makepoint(49.8236, 40.4236), 4326)::geography,
   20, 30, 0),
  ('v2','Coffee Lab','Qəhvə','Ə. Haqverdiyev 12, Nərimanov','Bakı',
   '2+1 çay təklifi','09:00–22:00','coffee','₼',
   true, 4.6, 51, 9, 22, 15,
   st_setsrid(st_makepoint(49.8522, 40.3922), 4326)::geography,
   25, 20, 0),
  ('v3','Techno Hub','Texnologiya','Nərimanov rayonu','Bakı',
   'İlk ziyarət pulsuz','10:00–20:00','tech','₼₼',
   true, 4.4, 19, 10, 20, 0,
   st_setsrid(st_makepoint(49.8556, 40.3903), 4326)::geography,
   20, 15, 0),
  ('v4','Dostlar Bağı','Piknik','Şıx düzü','Bakı',
   'Qruplara -10%','11:00–23:00','friends','₼',
   true, 4.7, 27, 11, 23, 10,
   st_setsrid(st_makepoint(49.7689, 40.3292), 4326)::geography,
   30, 20, 0),
  ('v5','Xankəndi Restoranı','Restoran','Şəfəq qəsəbəsi, Nərimanov','Bakı',
   'XP kartı ilə hesabda endirim','10:00–23:00','friends','₼₼',
   true, 4.5, 68, 10, 23, 0,
   st_setsrid(st_makepoint(49.8641, 40.3842), 4326)::geography,
   20, 25, 10),
  ('v6','Kokaltı Kafesi','Kafe','Kokaltı küçəsi 8','Bakı',
   'Yeni açılış — XP qazanmaq üçün aktivite keçir','09:00–22:00','coffee','₼',
   true, 4.3, 9, 9, 22, 0,
   st_setsrid(st_makepoint(49.8469, 40.3781), 4326)::geography,
   20, 15, 0)
on conflict (id) do nothing;

-- ── Kupon təklifləri (FakeData.offers o1..o5) ─────────────────────────────
insert into public.coupons_offers (
  id, venue_id, label, percent, fixed_azn, min_rank_index, xp_price,
  validity_days, auto_by_level, active)
values
  ('o1','v2','15% endirim',15, 0, 0, 200, 14, false, true),
  ('o2','v1','20% endirim',20, 0, 2, 300, 14, false, true),
  ('o3','v4','10% endirim',10, 0, 0, 100, 30, false, true),
  ('o4','v2','5% avtomatik endirim', 5, 0, 2,   0, 14, true,  true),
  ('o5','v3','İlk ziyarət pulsuz',   0, 5, 1,   0, 14, true,  true)
on conflict (id) do nothing;

-- ── Rezerv slotları (demo: bu gün + sabah, 3 vaxt) ───────────────────────
insert into public.venue_slots (venue_id, slot_date, slot_time, capacity)
select v.id, current_date, t, 8
from public.venues v
cross join (values ('12:00'),('15:00'),('18:00')) t
on conflict do nothing;
insert into public.venue_slots (venue_id, slot_date, slot_time, capacity)
select v.id, current_date + 1, t, 8
from public.venues v
cross join (values ('12:00'),('15:00'),('18:00')) t
on conflict do nothing;
