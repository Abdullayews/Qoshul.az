#!/usr/bin/env python3
"""Qoshul.az — test seed generatoru (D-147, MOCK→SUPABASE keçidi üçün).

Çıxış: supabase/testdata/0006_test_users_seed.sql — serverdə əl ilə tətbiq
olunur (migrate_selfhost.sh zəncirinə DAXİL DEYİL — 0003 prinsipi: demo
data migration zəncirinə düşmür; test məqsədlidir).

100 dolu user: auth.users (bcrypt parol) + profiles (tam dolu) + interests +
notification_settings (trigger) + xp_events + login_streak; əlavə 30 event,
iştiraklar (hər user 2-5 tədbirdə), mesajlar (event + şəxsi), swipe/match,
review-lər, invite-lər, 10 venue. BOŞ HESAB YOXDUR (verify bloku axırda).

Bütün parollar: QoshulTest!2026. Deterministik: random.seed(20261124).

İstifadə:  python3 scripts/gen_test_seed.py   (bcrypt lazımdır: pip install)
Tətbiq:   serverdə psql -v ON_ERROR_STOP=1 -f 0006_test_users_seed.sql
Təmizlə:  truncate public.* cascade; delete from auth.users; (D-147 README)
"""
import uuid
import random
import bcrypt
import json
from datetime import date, datetime, timedelta, timezone

random.seed(20261124)  # reproduklənən seed

N_USERS = 100
PASSWORD = "QoshulTest!2026"

# — Azərbaycan adları (real PII yox — ümumi adlar) —
FIRST_M = ["Elvin", "Rəşad", "Orxan", "Kamran", "Nihat", "Tural", "Anar",
           "Elnur", "Murad", "Samir", "Ruslan", "Vüsal", "Elcan", "Zaur",
           "İlqar", "Nurlan", "Ramin", "Fuad", "Aysel"[:0] or "Babək", "Şahin"]
FIRST_F = ["Leyla", "Aysel", "Nigar", "Günay", "Səbinə", "Türkan", "Aytən",
           "Xəzər", "Nərmin", "Səadət", "Dilarə", "Könül", "Zərnişan",
           "Fidan", "Mirvarí"[:0] or "Solmaz", "Ulviyyə", "Gülşən", "Aytac"]
LAST = ["Əliyev", "Məmmədov", "Həsənov", "Quliyeva", "İsmayılova", "Rəhimov",
        "Səfiyev", "Tağıyeva", "Nəbiyev", "Əhmədova", "Kazımov", "Oraqlı",
        "Şirinov", "Yusifova", "Cəbrayılov", "Mürşüdlü", "Bəhmənova"]

CITIES = [("Bakı", 40.4093, 49.8671), ("Gəncə", 40.6828, 46.3606),
          ("Sumqayıt", 40.5855, 49.6317), ("Şəki", 41.1933, 47.1560),
          ("Lənkəran", 38.7529, 48.8485), ("Quba", 41.3620, 48.5147),
          ("Mingəçevir", 40.7700, 47.0489), ("Naxçıvan", 39.2089, 45.4060)]

STATUSES = ["student", "worker", "entrepreneur", "freelancer", "pupil"]
UNIS = ["BDU", "ADNSU", "ADA", "UNEC", "ADMİU", "Xəzər Universiteti", "UFAZ", ""]
WORK = ["IT", "marketinq", "maliyyə", "təhsil", "tıb", "dizayn", "logistika",
        "turizm", "aqrar", "xidmət", ""]
LANGS_POOL = [["Azərbaycan dili"], ["Azərbaycan dili", "İngilis dili"],
              ["Azərbaycan dili", "Rus dili"],
              ["Azərbaycan dili", "İngilis dili", "Rus dili"],
              ["Azərbaycan dili", "Türk dili"]]
GOALS_POOL = [" dost tapmaq", "tədbirlərə qoşulmaq", "dil praktikası",
              "idman partnyoru", "networking", "hobbi paylaşmaq",
              "səyahət partnyoru", "tələbə çevrəsi"]
GROUP_SIZES = ["Kiçik qrup (3–6)", "Orta qrup (7–12)", "Böyük qrup (12+)"]
BUDGETS = ["10 AZN-ə qədər", "10–30 AZN", "30–60 AZN", "60+ AZN"]
BIOS = [
    "Həftə sonlarında dağ yürüşlərinə gedirəm, yeni dostlarla tanış olmaq istəyirəm.",
    "Şahmat və kviz gecələri sevirəm. Kofe içə-içə söhbət — əla başlanğıcdır.",
    "Proqramlaşdırma öyrənirəm, hakatonlarda iştirak edirəm.",
    "Padel və voleybol oynayıram. İdman partnyoru axtarıram.",
    "Foto-gezentilərə çıxıram, şəhərin gizli küncələrini kəşf edirəm.",
    "Dil mübadiləsi üçün söhbət partnyoru axtarıram — ingilis dili B2.",
    "Musiqi ilə məşğulam, cem sessiyalara qatılam.",
    "Xeyriyyə aksiyalarında könüllü oluram, heyvanlara yardım sevirəm.",
    "Yeni kafelər sınaqdan keçirirəm, kulinariya dərslərinə gedirəm.",
    "Startap ideyaları üzərində işləyirəm, networking tədbirləri axtarıram.",
    "Kitab klubuna yazılmışam, poeziya gecələrini sevirəm.",
    "Yoga və fitnes ilə məşğulam, sağlam həyat tərzi tərəfdarıyam.",
    "Stolüstü oyunlar gecələri təşkil edirəm — maifa və kviz üzrə komanda.",
    "Səyahət etməyi sevirəm, Azərbaycanın hər guşəsini gəzmək hədəfimdir.",
    "Tələbəyəm, imtahanlara birlikdə hazırlaşacaq dostlar axtarıram.",
]

# 85 kateqoriya slug (serverdən oxunan siyahı)
CATS = ["təmizlik-aksiyası", "muzey", "rəsm", "yurus", "stolüstü-oyunlar",
        "hakaton", "qehve", "networking", "şəhər-turu", "konsert", "futbol",
        "dil-mübadiləsi", "karyera", "proqramlaşdırma", "səfər", "xeyriyyə",
        "kemping", "foto-gezinti", "teatr", "cem-sessiya", "basketbol",
        "kibersport", "cay-sohbeti", "seminar", "kino", "heyvanlara-yardım",
        "süni-intellekt", "karaoke", "ailə", "kviz", "mühazirə",
        "sahibkarlıq", "sam-yemeyi", "alpinizm", "voleybol", "əl-işləri",
        "rəqs", "sergi", "padel", "imtahana-hazirliq", "mafiya", "ağac-əkmə",
        "startap", "seher-yemeyi", "mugam", "sağlamlıq", "marketinq",
        "baliqciliq", "nerd", "tennis", "piknik", "kitab-klubu", "caz",
        "kulinariya-dərsi", "birlikdə-dərs", "oyun-hazırlama",
        "maliyyə-savadlılığı", "xalçaçılıq", "qan-donorluğu",
        "mənəvi-inkişaf", "elm-sohbeti", "dizayn", "valideynlər", "keramika",
        "dequstasiya", "stolustu-tennis", "poeziya-gecəsi", "video-oyunlar",
        "müsahibəyə-hazırlıq", "dj-gecəsi", "at-surme", "kuçə-yeməkləri",
        "musiqi-aləti-dərsi", "tarix-turu", "escape-room", "qus-musahidesi",
        "yazı-emalatxanası", "tələbələr", "uzguculuk", "robototexnika",
        "qacis", "digər", "velosiped", "yoga", "fitnes", "doyus-idmani",
        "gules", "sahmat", "boulinq"]

EVENT_TEMPLATES = [
    ("Şənbə səhəri yürüş — Şahdağ ətəklərində", "yurus", "Təbiət"),
    ("Java hakatonu 48 saat", "hakaton", "Texnologiya"),
    ("Kviz gecəsi — komandalar 4 nəfər", "kviz", "Oyunlar"),
    ("Padel turniri yeni başlayanlar üçün", "padel", "İdman"),
    ("Söhbət klubu: ingilis dili B1-B2", "dil-mübadiləsi", "Təhsil"),
    ("Klassik musiqi axşamı", "konsert", "Musiqi"),
    ("Stolüstü oyunlar gecəsi: maifa + əlaqə", "mafiya", "Oyunlar"),
    ("Kənd turları — Lahıc səfəri", "səfər", "Səyahət və digər"),
    ("Şəhər foto-gezentisi: İçərişəhər", "foto-gezinti", "İncəsənət"),
    ("Kofe və networking — startupçılar üçün", "networking", "Biznes və karyera"),
    ("Yoga səhər seansı — Bulvar", "yoga", "İdman"),
    ("Kitab klubu: ayın kitabı üzrə müzakirə", "kitab-klubu", "Mədəniyyət"),
    ("Xeyriyyə aksiyası: Bağça təmizliyi", "təmizlik-aksiyası", "Könüllülük"),
    ("Kulinariya dərsi: xəmir yeməkləri", "kulinariya-dərsi", "Yemək və içki"),
    ("Caz gecəsi — canlı ifa", "caz", "Musiqi"),
    ("Futbol maçları 5x5 — həftəlik", "futbol", "İdman"),
    ("Seminariya: CV və müsahibə hazırlığı", "müsahibəyə-hazırlıq", "Biznes və karyera"),
    ("Kempinq hafta sonu — Qəbələ", "kemping", "Təbiət"),
    ("Robototexnika emalatxanası", "robototexnika", "Texnologiya"),
    ("Çay söhbəti — tanışlıq axşamı", "cay-sohbeti", "Yemək və içki"),
    ("Voleybol açıq havada", "voleybol", "İdman"),
    ("Muzey turu: Milli İncəsənət Muzeyi", "muzey", "Mədəniyyət"),
    ("Əl işləri emalatxanası: keramika", "keramika", "İncəsənət"),
    ("Şahmat blits turniri", "sahmat", "İdman"),
    ("Kino gecəsi — klassiklər", "kino", "Mədəniyyət"),
    ("Alpinizm: ilk addımlar (dərs)", "alpinizm", "Təbiət"),
    ("Escape-room komanda oyunu", "escape-room", "Oyunlar"),
    ("Poexiya gecəsi — açıq mikrofon", "poeziya-gecəsi", "Mədəniyyət"),
    ("Süni intellekt üzrə mühazirə", "süni-intellekt", "Texnologiya"),
    ("Velosiped turu — Şamaxı yolu", "velosiped", "İdman"),
]

VENUES = [
    ("Coffee Lab", "qehve", "Nərimanov, Bakı", "Bakı", "Hər kupa üçün 5% endirim", "08:00–22:00", "qehve", "₼₼", 40.4055, 49.8352, 10, 15, 5),
    ("Padel Arena", "padel", "Xətai, Bakı", "Bakı", "Saatlıq oyun 10% endirim", "09:00–23:00", "padel", "₼₼₼", 40.3833, 49.8517, 15, 20, 10),
    ("Chess Hub", "sahmat", "Mərkəz, Bakı", "Bakı", "Giriş 2 dəfədən sonra 1 dəfə pulsuz", "10:00–21:00", "diger", "₼", 40.4093, 49.8671, 8, 10, 5),
    ("Book Corner", "kitab-klubu", "İçərişəhər, Bakı", "Bakı", "Klub üzvlərinə 10% endirim", "09:00–20:00", "diger", "₼", 40.3637, 49.8346, 8, 10, 5),
    ("Yoga Loft", "yoga", "Səbail, Bakı", "Bakı", "İlk dərslə 20% endirim", "07:00–21:00", "yoga", "₼₼", 40.3688, 49.8411, 12, 15, 8),
    ("Gəncə Tech Mərkəzi", "hakaton", "Mərkəz, Gəncə", "Gəncə", "Tədbir iştirakçılarına endirim", "09:00–19:00", "diger", "₼₼", 40.6828, 46.3606, 10, 12, 5),
    ("Sumqayıt Sport Zalı", "voleybol", "Mərkəz, Sumqayıt", "Sumqayıt", "Ay üzvlük 10% XP bonusu", "08:00–22:00", "diger", "₼₼", 40.5855, 49.6317, 15, 20, 10),
    ("Quba Restoranı", "sam-yemeyi", "Mərkəz, Quba", "Quba", "Səhər yeməyi kompleksinə 8% endirim", "08:00–23:00", "diger", "₼₼", 41.3620, 48.5147, 10, 12, 5),
    ("Lənkəran Kafe", "cay-sohbeti", "Sahil, Lənkəran", "Lənkəran", "Çay söhbəti gecələri 10% endirim", "09:00–22:00", "diger", "₼", 38.7529, 48.8485, 8, 10, 5),
    ("Şəki Turizm Mərkəzi", "şəhər-turu", "Mərkəz, Şəki", "Şəki", "Qrup turlarına 12% endirim", "09:00–18:00", "diger", "₼₼", 41.1933, 47.1560, 10, 15, 8),
]

MSG_POOL = [
    "Salam! Bu tədbirə qatılırsan?",
    "Yarın orda olacağam, görüşək.",
    "Yerlər tükənir, tələs.",
    "Saat dəqiq desən, gələrəm.",
    "Komandamızda 1 yer var, qoşulursan?",
    "Super — sözə görə!",
    "Əla idi, təşəkkür təşkilatçılara!",
    "Növbəti dəfə də qatılacağam.",
    "Yol xərçini necə bölünür?",
    "Giriş kodu: hamı üçün eynidir.",
]
DM_POOL = [
    "Salam! Profilindəki maraqlara baxdım, ortaq maraqlarımız var.",
    "Hər hansı bir tədbirə gedirsən bu həftə?",
    "Padel oynayırsansa, bir oyun yazılaşaq.",
    "Yeni şəhərə köçdüm, dost tapmaq istəyirəm.",
    "Dil mübadiləsi üçün partnyor axtarırsan?",
    "Növbəti yürüşdə görüşək!",
]
NOTIF_TYPES = ["MATCH", "MESSAGE", "EVENT_JOIN", "EVENT_APPROVAL",
               "EVENT_REMINDER", "RESV_UPDATE", "RESV_REMINDER", "LEVEL_UP",
               "COUPON_EXPIRING", "NEARBY_EVENT", "MARKETING"]


def sq(s: str) -> str:
    """SQL string literal (tək dırnaq qaçışı)."""
    return "'" + s.replace("'", "''") + "'"


def j(obj) -> str:
    return sq(json.dumps(obj, ensure_ascii=False))


def geo(lat: float, lng: float) -> str:
    return f"ST_SetSRID(ST_MakePoint({lng:.5f},{lat:.5f}),4326)::geography"


def main() -> None:
    pwd_hash = bcrypt.hashpw(PASSWORD.encode(), bcrypt.gensalt(rounds=10)).decode()
    today = date(2026, 11, 24)
    now = datetime(2026, 11, 24, 18, 0, 0, tzinfo=timezone.utc)

    users = []
    for i in range(1, N_USERS + 1):
        female = i % 2 == 0
        first = random.choice(FIRST_F if female else FIRST_M)
        last = random.choice(LAST)
        city, clat, clng = random.choice(CITIES[:5] if i % 3 else CITIES)
        birth = date(random.randint(1988, 2006), random.randint(1, 12),
                     random.randint(1, 28))
        status = random.choice(STATUSES)
        uni = random.choice(UNIS) if status == "student" else ""
        work = random.choice(WORK)
        lat = clat + random.uniform(-0.03, 0.03)
        lng = clng + random.uniform(-0.03, 0.03)
        goals = random.sample(GOALS_POOL, k=random.randint(1, 3))
        availability = random.sample(
            ["səhər", "günorta", "axşam", "həftə sonu"],
            k=random.randint(1, 3))
        personality = {
            "mbti": random.choice(["INTJ", "ENFP", "ISTP", "ESFJ", "INFP",
                                   "ENTJ", "ISFP", "ESTJ"]),
            "energy": random.choice(["introvert", "ekstravert", "ambivert"]),
        }
        xp = random.randint(60, 2400)
        streak = random.randint(1, 14)
        interests = random.sample(CATS, k=random.randint(3, 7))
        users.append({
            "i": i, "id": str(uuid.uuid4()),
            "first": first, "last": last,
            "email": f"testuser{i:03d}@qoshul.az",
            "female": female, "city": city, "birth": birth,
            "status": status, "uni": uni, "work": work,
            "lat": lat, "lng": lng, "goals": goals,
            "availability": availability, "personality": personality,
            "xp": xp, "streak": streak, "interests": interests,
            "photo_count": random.randint(1, 4),
            "bio": BIOS[(i - 1) % len(BIOS)],
        })

    out = ["BEGIN;",
           "-- Qoshul.az test seed — 100 dolu user (2026-11-24)",
           "-- Bütün parollar: QoshulTest!2026",
           ""]

    # — auth.users (trigger profili + notification_settings yaradır) —
    out.append("-- 1) auth.users + trigger-in avtomatik profil/notif yaratması")
    for u in users:
        out.append(
            f"insert into auth.users (id, instance_id, aud, role, email, "
            f"encrypted_password, email_confirmed_at, last_sign_in_at, "
            f"raw_app_meta_data, raw_user_meta_data, created_at, updated_at, "
            f"confirmation_token, recovery_token, email_change, email_change_token_new) values ("
            f"{sq(u['id'])}, '00000000-0000-0000-0000-000000000000', 'authenticated', "
            f"'authenticated', {sq(u['email'])}, {sq(pwd_hash)}, "
            f"'{now.isoformat()}', '{now.isoformat()}', "
            f"'{{\"provider\":\"email\",\"providers\":[\"email\"]}}', "
            f"'{{\"first_name\":\"{u['first']}\",\"last_name\":\"{u['last']}\"}}', "
            f"'{now.isoformat()}', '{now.isoformat()}', '', '', '', '');")

    # — profiles: trigger minimum yaradır, biz DOLDURURUQ —
    out.append("\n-- 2) profiles — tam dolu (bio, maraq, lokasiya, XP, streak)")
    for u in users:
        out.append(
            f"update public.profiles set role='USER', first_name={sq(u['first'])}, "
            f"last_name={sq(u['last'])}, birth_date='{u['birth'].isoformat()}', "
            f"gender={'\'Qadın\'' if u['female'] else '\'Kişi\''}, bio={sq(u['bio'])}, "
            f"photo_count={u['photo_count']}, "
            f"photo_urls={j([f'photo{k+1}' for k in range(u['photo_count'])])}, "
            f"phone_verified={'true' if u['i'] % 4 == 0 else 'false'}, "
            f"city={sq(u['city'])}, status={sq(u['status'])}, university={sq(u['uni'])}, "
            f"work_field={sq(u['work'])}, languages={j(random.choice(LANGS_POOL))}, "
            f"goals={j(u['goals'])}, personality={j(u['personality'])}, "
            f"availability={j(u['availability'])}, "
            f"group_size={sq(random.choice(GROUP_SIZES))}, "
            f"budget={sq(random.choice(BUDGETS))}, "
            f"map_visibility='EVERYONE', show_distance=true, xp={u['xp']}, "
            f"login_streak={u['streak']}, "
            f"last_login_day={(today - timedelta(days=1)).toordinal()}, "
            f"last_location={geo(u['lat'], u['lng'])}, "
            f"location_updated_at='{now.isoformat()}', onboarded=true, "
            f"profile_completed=true where id={sq(u['id'])};")

    # — user_interests —
    out.append("\n-- 3) user_interests — hər userdə 3–7 maraq")
    for u in users:
        vals = ", ".join(f"({sq(u['id'])},{sq(c)})" for c in u["interests"])
        out.append(f"insert into public.user_interests (profile_id, category_slug) "
                   f"values {vals} on conflict do nothing;")

    # — venues —
    out.append("\n-- 4) venues — 10 tərəfdaş məkan")
    for k, (name, cat, addr, city, deal, hours, image, price, lat, lng,
            xpa, maxd, minb) in enumerate(VENUES, 1):
        out.append(
            f"insert into public.venues (id, name, category, address, city, deal, "
            f"hours, image, price_range, auto_accept, rating, reviews_count, "
            f"open_from, open_to, discount_percent, location, xp_per_azn, "
            f"max_discount_percent, min_bill_azn) values ('v{k:02d}', {sq(name)}, "
            f"{sq(cat)}, {sq(addr)}, {sq(city)}, {sq(deal)}, {sq(hours)}, "
            f"{sq(image)}, {sq(price)}, true, {round(random.uniform(4.2, 4.9), 1)}, "
            f"{random.randint(8, 60)}, 8, 22, {random.choice([0, 5, 10])}, "
            f"{geo(lat, lng)}, {xpa}, {maxd}, {minb});")

    # — events — hosts: ilk 30 user —
    out.append("\n-- 5) events — 30 tədbir (host = ilk 30 user)")
    event_ids = []
    for k, (title, cat, _grp) in enumerate(EVENT_TEMPLATES, 1):
        eid = f"evt{k:03d}"
        event_ids.append(eid)
        host = users[(k - 1) % 30]
        days_ahead = random.randint(-10, 30)
        edate = today + timedelta(days=days_ahead)
        city, clat, clng = next(c for c in CITIES if c[0] == host["city"])
        lat = clat + random.uniform(-0.02, 0.02)
        lng = clng + random.uniform(-0.02, 0.02)
        out.append(
            f"insert into public.events (id, title, category, city, location, "
            f"location_geo, epoch_day, event_time, event_date, capacity, "
            f"description, price_azn, age_limit, language, level, format, "
            f"approval, recurring, tags, image, host_profile_id, venue_id) values ("
            f"{sq(eid)}, {sq(title)}, {sq(cat)}, {sq(host['city'])}, "
            f"{sq(random.choice(['Park', 'Mərkəz', 'Kafe', 'Kampus', 'Bulvar']) + ', ' + host['city'])}, "
            f"{geo(lat, lng)}, {edate.toordinal()}, "
            f"'{random.choice(['10:00','14:00','17:00','19:00','19:30'])}', "
            f"'{edate.isoformat()}', {random.choice([6, 8, 10, 12, 16, 20])}, "
            f"{sq('Test tədbiri — ' + title + '. Yeni dostlarla tanış ol, XP qazan.')}, "
            f"{random.choice([0, 0, 5, 10, 15, 20])}, 'Hamı üçün', "
            f"'Azərbaycan dili', 'Hamı üçün', 'Oflayn', "
            f"{'true' if k % 4 == 0 else 'false'}, 'Bir dəfəlik', "
            f"{j([cat, 'test'])}, 'friends', {sq(host['id'])}, "
            f"{sq(f'v{random.randint(1, 10):02d}')});")

    # — event_participants — USER-MƏRKƏZLİ: hər user 2-4 tədbirdə (v2 fix) —
    # Əvvəlki versiya event-mərkəzli idi: ilk ~12 user HƏR tədbirə düşürdü,
    # 70 user isə heç birinə. İndi: hər user öz seçimlərini edir + host özününkü.
    out.append("\n-- 6) event_participants — hər user 2-4 tədbirdə (user-mərkəzli)")
    user_events: dict = {u["id"]: set() for u in users}
    for k, eid in enumerate(event_ids, 1):
        host = users[(k - 1) % 30]
        user_events[host["id"]].add(eid)
    for u in users:
        for eid in random.sample(event_ids, k=random.randint(2, 4)):
            user_events[u["id"]].add(eid)
    for u in users:
        for eid in sorted(user_events[u["id"]]):
            # status check: PENDING | JOINED | DECLINED (DB constraint)
            status = "JOINED" if random.random() < 0.85 else "PENDING"
            checked = ("null" if random.random() > 0.3
                       else f"'{(now - timedelta(days=random.randint(1, 5))).isoformat()}'")
            out.append(
                f"insert into public.event_participants (event_id, profile_id, status, "
                f"joined_at, checked_in_at, checkin_xp_awarded) values ("
                f"{sq(eid)}, {sq(u['id'])}, '{status}', "
                f"'{(now - timedelta(days=random.randint(0, 12))).isoformat()}', "
                f"{checked}, {'true' if checked != 'null' else 'false'});")

    # — messages (event söhbətləri) —
    out.append("\n-- 7) messages — tədbir söhbətləri (hər tədbirdə 5–12 mesaj + hər user 1 mesaj)")
    for k, eid in enumerate(event_ids, 1):
        host = users[(k - 1) % 30]
        participants = [u for u in random.sample(users, k=random.randint(4, 8))]
        if host not in participants:
            participants.append(host)
        n_msg = random.randint(5, 12)
        for m in range(n_msg):
            sender = random.choice(participants)
            out.append(
                f"insert into public.messages (kind, event_id, sender_id, recipient_id, body, created_at) "
                f"values ('EVENT', {sq(eid)}, {sq(sender['id'])}, null, "
                f"{sq(random.choice(MSG_POOL))}, "
                f"'{(now - timedelta(days=random.randint(0, 10), minutes=m * 7)).isoformat()}');")
    # hər user öz iştirak etdiyi tədbirdən ən az 1 mesaj yazır (boş hesab yox)
    for u in users:
        eid = random.choice(sorted(user_events[u["id"]]))
        out.append(
            f"insert into public.messages (kind, event_id, sender_id, recipient_id, body, created_at) "
            f"values ('EVENT', {sq(eid)}, {sq(u['id'])}, null, "
            f"{sq(random.choice(MSG_POOL))}, "
            f"'{(now - timedelta(days=random.randint(0, 9), minutes=41)).isoformat()}');")

    # — DM-lər (kind=PERSONAL) — 100 cüt: hər user həm sender, həm alıcı —
    out.append("\n-- 8) messages — şəxsi söhbətlər (100 cüt, tam örtük)")
    for i in range(N_USERS):
        a, b = users[i], users[(i + 7) % N_USERS]
        for m in range(random.randint(2, 5)):
            out.append(
                f"insert into public.messages (kind, event_id, sender_id, recipient_id, body, created_at) "
                f"values ('PERSONAL', null, {sq(a['id'])}, {sq(b['id'])}, "
                f"{sq(random.choice(DM_POOL))}, "
                f"'{(now - timedelta(days=random.randint(0, 6), minutes=m * 23)).isoformat()}');")

    # — swipes + matches —
    out.append("\n-- 9) swipes + matches — 60 swipe, 25 match")
    matched = set()
    for i in range(60):
        a = users[i % N_USERS]
        b = users[(i * 7 + 13) % N_USERS]
        if a["id"] == b["id"] or (a["id"], b["id"]) in matched:
            continue
        matched.add((a["id"], b["id"]))
        action = "LIKE" if i % 3 else "PASS"
        out.append(
            f"insert into public.swipes (id, swiper_id, target_id, action, day, xp_awarded, created_at) "
            f"values ('sw{i:03d}', {sq(a['id'])}, {sq(b['id'])}, '{action}', "
            f"'{today.isoformat()}', {'true' if action == 'LIKE' else 'false'}, "
            f"'{(now - timedelta(days=random.randint(0, 4))).isoformat()}');")
    pair_seen = set()
    mid = 0
    for (aid, bid) in list(matched)[:25]:
        # matches_check: a_id < b_id (uuid sıralı olmalı)
        lo, hi = sorted([aid, bid])
        key = (lo, hi)
        if key in pair_seen or lo == hi:
            continue
        pair_seen.add(key)
        mid += 1
        out.append(
            f"insert into public.matches (id, a_id, b_id, created_at) values "
            f"('mt{mid:03d}', {sq(lo)}, {sq(hi)}, "
            f"'{(now - timedelta(days=random.randint(0, 5))).isoformat()}');")

    # — reviews — keçmiş tədbirlərə —
    out.append("\n-- 10) reviews — 25 rəy (keçmiş tədbirlərə)")
    for i in range(25):
        eid = event_ids[i % 30]
        author = users[(i * 3 + 11) % N_USERS]
        out.append(
            f"insert into public.reviews (id, event_id, author_id, rating, body, status, xp_awarded, created_at) "
            f"values ('rv{i:03d}', {sq(eid)}, {sq(author['id'])}, "
            f"{random.choice([4, 4, 5, 5, 5, 3])}, "
            f"{sq(random.choice(['Çox yaxşı təşkil olunub, təşəkkürlər!',
                                'Əla atmosfer idi, yenə qatılacağam.',
                                'Yer və vaxt dəqiq seçilib. Tövsiyə edirəm.',
                                'Marqlı tədbir idi, yeni dostlar qazandım.']))}, "
            f"'APPROVED', true, "
            f"'{(now - timedelta(days=random.randint(1, 8))).isoformat()}');")

    # — xp_events — hər user üçün XP tarixçəsi —
    # unikal (profile_id, key) — hər userdə hər key BİR dəfə
    out.append("\n-- 11) xp_events — hər userdə 3–6 XP hadisəsi (unikal key)")
    for u in users:
        keys = random.sample(["event_join", "checkin", "daily_login",
                              "review", "invite", "profile_complete"],
                             k=random.randint(3, 6))
        for k, key in enumerate(keys):
            out.append(
                f"insert into public.xp_events (id, profile_id, key, amount, note, at) "
                f"values ('x{u['i']:03d}{k}', {sq(u['id'])}, '{key}', "
                f"{random.choice([10, 15, 20, 30, 50])}, "
                f"{sq('test seed')}, "
                f"'{(now - timedelta(days=random.randint(0, 30))).isoformat()}');")

    # — invites —
    out.append("\n-- 12) invites — 20 dəvət")
    for i in range(20):
        inviter = users[(i * 5) % N_USERS]
        invitee = users[(i * 5 + 3) % N_USERS]
        out.append(
            f"insert into public.invites (code, inviter_id, invitee_id, registered_at, first_checkin_at, xp_awarded, created_at) "
            f"values ('INV{i:03d}QZ', {sq(inviter['id'])}, {sq(invitee['id'])}, "
            f"'{(now - timedelta(days=random.randint(2, 10))).isoformat()}', "
            f"'{(now - timedelta(days=random.randint(0, 2))).isoformat()}', "
            f"true, '{(now - timedelta(days=random.randint(10, 20))).isoformat()}');")

    # — notifications —
    out.append("\n-- 13) notifications — hər userə 2–4 bildiriş")
    for u in users:
        for k in range(random.randint(2, 4)):
            ntype = random.choice(NOTIF_TYPES)
            titles = {
                "MATCH": "Yeni match!", "MESSAGE": "Yeni mesaj",
                "EVENT_JOIN": "Tədbirə yeni iştirakçı",
                "EVENT_APPROVAL": "İştirakın təsdiqləndi",
                "EVENT_REMINDER": "Tədbir yaxınlaşır",
                "RESV_UPDATE": "Rezervasiya yeniləndi",
                "RESV_REMINDER": "Rezervasiya xatırlatması",
                "LEVEL_UP": "Yeni səviyyə!", "COUPON_EXPIRING": "Kupon bitir",
                "NEARBY_EVENT": "Yaxınlıqda tədbir",
                "MARKETING": "Qoshul-dan yenilik",
            }
            bodies = {
                "MATCH": "Səninlə match oldu — salam yaz!",
                "MESSAGE": "Gələn sandiqda yeni mesajın var.",
                "EVENT_JOIN": "Tədbirinə yeni iştirakçı qoşuldu.",
                "EVENT_APPROVAL": "Təşkilatçı iştirakını təsdiqlədi.",
                "EVENT_REMINDER": "Tədbir 1 saatdan sonra başlayır.",
                "RESV_UPDATE": "Məkan rezervasiyanı təsdiqlədi.",
                "RESV_REMINDER": "Rezervasiyan bu gün saat 19:00-dadır.",
                "LEVEL_UP": "Təbrik! Yeni səviyyəyə keçdin.",
                "COUPON_EXPIRING": "Endirim kuponun 3 gün sonra bitir.",
                "NEARBY_EVENT": "1 km-də yeni tədbir yaradıldı.",
                "MARKETING": "Bu həftənin tədbirlərinə göz at.",
            }
            out.append(
                f"insert into public.notifications (id, profile_id, notif_type, title, body, read, created_at) "
                f"values ('n{u['i']:03d}{k}', {sq(u['id'])}, '{ntype}', "
                f"{sq(titles[ntype])}, {sq(bodies[ntype])}, "
                f"{'true' if k == 0 else 'false'}, "
                f"'{(now - timedelta(days=random.randint(0, 5), hours=k)).isoformat()}');")

    out.append("\nCOMMIT;")
    out.append("-- ============ SEED TAMAM ============")
    out.append(f"-- users={N_USERS}, events={len(event_ids)}, venues={len(VENUES)}")

    import pathlib
    path = str(pathlib.Path(__file__).resolve().parent.parent
               / "supabase" / "testdata" / "0006_test_users_seed.sql")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out) + "\n")
    print(f"OK: {path} — {len(out)} sətir")
    # statistika
    print(f"users={N_USERS} events={len(event_ids)} venues={len(VENUES)}")


if __name__ == "__main__":
    main()
