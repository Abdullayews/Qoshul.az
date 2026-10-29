# Qoshul.az — Məhsul Spektrumu (Product Spec)

> **Mənbə:** istifadəçinin ROL sənədi (2026-10-28). Bu sənəd YALNIZ məhsul məntiqini müəyyən edir: rollar, axınlar, ekranlar, XP/kupon/rezerv qaydaları, moderasiya.
>
> **Texniki stack burada QÜVVƏDƏN DÜŞÜB** (istifadəçi direktivi: "burda verilən tech stack unut"). Implementasiya stack-i `decisions.md` (D-001..D-024) ilə müəyyən olunur. Xəritələnmə: R2 → **OCI OSS** (D-019) · hostinq → **hibrid Oracle** (D-022) · FCM/SMTP/SMS/OAuth → xarici (D-020) · qalan stack (Flutter, NestJS, PG/PostGIS, Redis+BullMQ, Socket.IO) dəyişməz.
>
> **Gedişat qaydası (istifadəçi direktivi):** UI tam testdən keçənə qədər **backend PLACEHOLDER-dır** (mock data ilə simulyasiya). Ödənişlər və backend UI təsdiqindən SONRA əlavə olunacaq.

---

## A. İstifadəçi rolları

1. **Qonaq:** yalnız onboarding və giriş ekranlarını görür.
2. **İstifadəçi:** əsas rol, bütün funksiyalardan istifadə edir.
3. **Partnyor (məkan sahibi):** məkanını, rezerv slotlarını və endirim təkliflərini idarə edir (veb panel və ya mobil daxilində ayrıca bölmə: **[TƏSDİQ GÖZLƏYİR]**).
4. **Moderator/Admin:** report-ları, şəkilləri, bloklamanı idarə edir.

## B. İlk giriş axını (Onboarding)

**Addım 1.** Splash → 3 slaydlıq tanışlıq (Kəşf et, Qoşul, Qazan). "Keç" düyməsi var.

**Addım 2.** Qeydiyyat/Giriş ekranı:
- "Google ilə davam et", "Apple ilə davam et" (iOS-da məcburi), "Email ilə qeydiyyat".
- Email qeydiyyatı: email + parol → 6 rəqəmli təsdiq kodu emailə gedir → təsdiq.
- Telefon nömrəsi qeydiyyat zamanı MƏCBURİ DEYİL (SMS qiyməti yüksəkdir). Sonradan Profil → "Hesabı təsdiqlə" bölməsində isteğe bağlı əlavə olunur və təsdiqlənmiş nişan (badge) + bonus XP verir.

**Addım 3.** Profil yaradılması:
- Ad, doğum tarixi (18+ məhdudiyyəti: **[TƏSDİQ GÖZLƏYİR]**), cins (isteğe bağlı), şəkil (1 məcburi, 6-ya qədər), qısa bio (150 simvol).
- Şəkil yüklənir → storage (OSS) → moderasiya növbəsi (BullMQ). Təsdiqlənənə qədər profil "yoxlanılır" statusundadır.

**Addım 4.** Kateqoriya seçimi (maraqlar), minimum 3, maksimum 8:
1. Əyləncə və Gecə həyatı (klub, bar, karaoke)
2. Kafe və Yemək (kafe, restoran, kofe görüşləri)
3. İdman və Aktivlik (futbol, tennis, qaçış, velosiped, fitnes)
4. Təbiət və Səyahət (hiking, kamp, günlük səyahət, Qəbələ, Şahdağ və s.)
5. Mədəniyyət və İncəsənət (teatr, muzey, konsert, sərgi)
6. Təhsil və Karyera (dil klubları, networking, seminar)
7. Oyun və Hobbi (stolüstü oyunlar, PlayStation, kitab klubu, foto)
8. Musiqi (canlı musiqi, jam session)
9. Könüllülük və Sosial (xeyriyyə, təmizlik aksiyaları)

Seçilmiş kateqoriyalar: kəşf feed-ini, match sırasını və bildirişləri müəyyən edir. Sonradan Profil → Maraqlar bölməsindən dəyişdirilə bilər.

**Addım 5.** İcazələr (hər biri ayrıca izah ekranı ilə, rədd edilsə tətbiq işləməyə davam edir):
- Məkan (yaxınlıq üçün): "tətbiq istifadə olunarkən". Dəqiq koordinat başqa istifadəçilərə göstərilmir, yalnız təxmini məsafə (məs. "~2 km").
- Bildirişlər (FCM).

**Addım 6.** Başlanğıc XP: qeydiyyatı tamamlayana **+50 XP**.

## C. Əsas naviqasiya (alt tab bar, 5 tab)

**TAB 1: KƏŞF (Ana səhifə)**
- Yuxarıda: salam, cari XP, səviyyə, bildiriş ikonu.
- Kateqoriya çipləri (seçilmiş maraqlara görə, filtr kimi işləyir).
- Bölmələr: "Bu axşam", "Yaxınında olan tədbirlər", "Populyar məkanlar", "Sənə uyğun insanlar" (match), "Endirimli təkliflər".
- Axtarış: məkan, tədbir, istifadəçi adı.
- Hər kartdan detal ekranına keçid.

**TAB 2: XƏRİTƏ**
- PostGIS əsaslı: yaxınlıqdakı məkanlar, tədbirlər və (razılıq verənlər üçün) insanlar pin kimi göstərilir.
- Filtrlər: kateqoriya, məsafə (1/3/5/10 km), "indi açıqdır", "endirimi var".
- Pin-ə toxunanda alt kart açılır: ad, qiymət aralığı, endirim, "Ətraflı" və "Rezerv et" düymələri.
- İstifadəçi öz görünürlüyünü Profil → Məxfilik bölməsində söndürə bilər (xəritədə və match-də görünməz olur).

**TAB 3: TƏDBİRLƏR**
- Siyahı: "Yaxınlaşan", "Mənim tədbirlərim", "Yaratdığım".
- Tədbir yaratmaq: ad, kateqoriya, məkan (xəritədən və ya partnyor məkan seçimi), tarix-saat, maksimum iştirakçı sayı, təsvir, şəkil, açıq/təsdiqlə-qoşul rejimi.
- Qoşulmaq: "Qoşul" → (açıqdırsa dərhal, təsdiqlə rejimdirsə təşkilatçının təsdiqi) → tədbirin qrup chat-inə avtomatik əlavə olunma.
- Tədbirdən 24 saat və 2 saat əvvəl xatırlatma bildirişi.
- Tədbirdə iştirak: məkanda QR/yerləşmə ilə check-in → XP.
- Tədbirdən sonra: iştirakçıları qiymətləndirmə (1-5 ulduz + qısa rəy), şikayət etmə imkanı.

**TAB 4: CHAT**
- Bölmələr: "Şəxsi" və "Qruplar" (tədbir chat-ləri).
- Şəxsi chat başlamaq: yalnız match olunmuş istifadəçilərlə (qarşılıqlı "Maraqlıyam") və ya eyni tədbir iştirakçıları ilə.
- Mətn, şəkil, emoji; oxunma statusu ("çatdırıldı", "oxundu"); yazır... göstəricisi.
- Hər söhbətdə: Report et, Blokla, Söhbəti sil.
- Mesaj göndərmə rate limit-i və spam filtri var.
- Bloklanan istifadəçi nə mesaj yaza, nə profili görə bilər.

**TAB 5: PROFİL**
- Öz profilim: şəkillər, bio, maraqlar, XP və səviyyə, nişanlar (badge), rezervlərim, endirim kuponlarım, tədbir tarixçəm.
- Ayarlar: məxfilik (xəritədə görünmə, məsafə göstərmə), bildirişlər, dil, hesabı təsdiqlə (telefon), bloklananlar siyahısı, hesabı silmək (GDPR/yerli qanun üzrə məlumatın silinməsi).

## D. Match (yaxınlığa görə tanışlıq)

- Kəşf-də və ya ayrıca "Tanış ol" ekranında kartlar: şəkil, ad, yaş, məsafə, ortaq maraqlar.
- Hərəkətlər: "Maraqlıyam" / "Keç".
- Hər iki tərəf "Maraqlıyam" bassa → match + bildiriş + chat açılır + hər ikisinə **+10 XP** (gündə maksimum 3 match XP-si).
- Sıralama: ortaq kateqoriya sayı + məsafə + son aktivlik. Gündəlik "Maraqlıyam" limiti: **[TƏSDİQ GÖZLƏYİR]**.
- Sorğu: ST_DWithin + gist index, nəticələr səhifələnir.

## E. Məkanlar və rezerv

**Məkan detal ekranı:** şəkillər, ünvan və xəritə, iş saatları, kateqoriya, qiymət aralığı, rəylər, aktiv endirim, "Rezerv et" və "Yol tap" düymələri.

**Rezerv axını:**
1. Tarix seç → saat slotu seç → nəfər sayı seç (məkanın tutumuna görə).
2. İstəyə görə qeyd (ad günü, pəncərə kənarı və s.).
3. XP ilə endirim tətbiq etmək istəyirsə: "Kupon seç" → mövcud kuponlardan biri seçilir.
4. "Təsdiqlə" → status: GÖZLƏYİR (partnyor təsdiqi tələb edən məkanlarda) və ya TƏSDİQLƏNDİ (avtomatik təsdiq məkanlarında).
5. Bildirişlər: rezerv təsdiqi, 24 saat və 2 saat əvvəl xatırlatma.
6. Rezerv QR kodu yaranır. Məkanda partnyor QR-i skan edir → status: İSTİFADƏ OLUNDU → XP və endirim aktivləşir.
7. Ləğv: məkanın qaydasına görə (məs. 2 saat əvvələ qədər pulsuz). Gəlməmə (no-show) 3 dəfə təkrarlansa rezerv hüququ müvəqqəti məhdudlaşır (7 gün).

**Rezerv statusları:** GÖZLƏYİR → TƏSDİQLƏNDİ → İSTİFADƏ OLUNDU | LƏĞV EDİLDİ | GƏLMƏDİ | RƏDD EDİLDİ.

**Ödəniş:** Bu mərhələdə onlayn ödəniş YOXDUR, rezerv yalnız yer ayırmadır. **[TƏSDİQ GÖZLƏYİR: depozit/onlayn ödəniş lazımdırmı?]**

**Qrupla rezerv:** istifadəçi rezervi tədbirə və ya chat-dəki dostlarına paylaşa bilər, onlar "Qoşul" basır.

## F. XP və səviyyə sistemi

**XP qazanma** (başlanğıc təklif, admin paneldən dəyişdirilə bilər):

| Hadisə | XP | Limit |
|---|---|---|
| Qeydiyyatı tamamlama | +50 | bir dəfə |
| Profil 100% doldurma (şəkil, bio, maraqlar) | +30 | bir dəfə |
| Telefon/hesab təsdiqi | +40 | bir dəfə |
| Günlük giriş (ardıcıl seriya) | +5 | 7 günlük seriya bonusu +30 |
| Match | +10 | gündə max 3 |
| Tədbirə qoşulma | +10 | — |
| Tədbirdə check-in (faktiki iştirak) | +40 | — |
| Tədbir yaratmaq (≥3 iştirakçı gəldikdə) | +60 | — |
| Rezervdən istifadə (QR skan) | +30 | — |
| Məkana rəy yazmaq (moderasiyadan keçən) | +15 | həftədə max 3 |
| Dost dəvət etmək (dəvətli qeydiyyatı bitirib ilk tədbirə gələndə) | +100 | — |

**Qorunma:** hər XP hadisəsi idempotent (təkrar sayılmır), serverdə hesablanır, gündəlik limit, saxta check-in-ə qarşı yerləşmə yoxlaması (məkandan max 200 m) və QR-in birdəfəlik olması.

**Səviyyələr (təklif):**

| Səviyyə | Ad | XP |
|---|---|---|
| 1 | Yeni | 0-99 |
| 2 | Kəşfiyyatçı | 100-299 |
| 3 | Aktiv | 300-699 |
| 4 | Sosial | 700-1499 |
| 5 | Lider | 1500-2999 |
| 6 | Əfsanə | 3000+ |

**Səviyyə artımı:** bildiriş + nişan + yeni endirim imkanı açılır.
**Nişanlar (badge):** "İlk tədbir", "10 tədbir", "Səyyah", "Gecə quşu", "Təsdiqlənmiş".

## G. Endirim və kupon sistemi

- Partnyor məkan endirim təklifi yaradır: faiz (məs. 10%/15%/20%) və ya sabit məbləğ, tələb olunan minimum səviyyə, XP qiyməti, qüvvədə olma tarixi, günlük/ümumi limit.
- İstifadəçi "Endirimlər" bölməsində (Kəşf və Profil-də) təklifləri görür:
  - a) **Səviyyəyə görə avtomatik açılan** endirimlər (məs. Aktiv səviyyə → bütün partnyor kafelərdə 5%).
  - b) **XP ilə "al":** istifadəçi XP xərcləyib kupon alır (məs. 200 XP → 15% kupon). XP balansı və ümumi qazanılmış XP ayrı saxlanılır (səviyyə xərcləməklə düşmür).
- **Kupon:** unikal kod + QR, qüvvədə olma müddəti (məs. 14 gün), yalnız bir dəfə istifadə olunur, yalnız seçilmiş məkanda keçərlidir.
- **İstifadə:** rezerv zamanı tətbiq olunur və ya məkanda QR göstərilir, partnyor skan edir və kupon "İSTİFADƏ OLUNDU" olur.
- Müddəti bitən istifadə olunmamış kupon üçün XP geri qaytarılmır (istifadəçiyə ekranda xəbərdarlıq göstərilir).

## H. Bildirişlər

**Tiplər:** yeni match, yeni mesaj, tədbirə qoşulma sorğusu/təsdiqi, tədbir xatırlatması, rezerv təsdiqi/ləğvi/xatırlatma, XP və səviyyə artımı, kupon vaxtı bitir, yaxınlıqda uyğun tədbir.

- Ayarlarda hər tip ayrıca söndürülə bilər.
- Gecə saatlarında (23:00-08:00) marketinq tipli bildirişlər göndərilmir.
- Real-vaxt: Socket.IO, arxa planda: FCM (BullMQ növbəsi ilə).

## I. Təhlükəsizlik və moderasiya

- Hər profil, tədbir və məkan üzərində "Report et" (səbəb siyahısı + qeyd).
- Blok: qarşılıqlı görünməzlik, chat-ın bağlanması.
- Şəkil moderasiyası: yükləmədən sonra asinxron yoxlama (BullMQ), uyğunsuz şəkil gizlədilir.
- Eyni istifadəçiyə qarşı 3+ report → avtomatik müvəqqəti məhdudlaşdırma və admin növbəsi.
- İlk görüşlər üçün təhlükəsizlik məsləhətləri ekranı (ictimai məkanda görüş, yaxın adamı xəbərdar etmə).
- Dəqiq ev ünvanı/koordinat heç vaxt API cavabında başqa istifadəçiyə qaytarılmır.

## J. Backend-ə təsir (backend mərhələsi üçün — UI fazasında placeholder)

Mövcud modullara əlavə: Interests (kateqoriyalar), Venues (məkanlar, slotlar), Reservations, Xp (hadisə jurnalı, balans, səviyyə), Coupons (təkliflər, kuponlar, istifadə), Reports/Moderation, Reviews, Partners.

Əsas cədvəllər (Drizzle): `users`, `user_interests`, `categories`, `venues`, `venue_slots`, `reservations`, `events`, `event_participants`, `matches`, `swipes`, `xp_events` (immutable jurnal), `xp_balances`, `levels`, `coupons_offers`, `user_coupons`, `reviews`, `reports`, `blocks`, `notifications`.

Hər yeni endpoint üçün: DTO, validasiya, rate limit, test və OpenAPI təsviri məcburidir.

## K. Günün əsas istifadəçi ssenarisi (nümunə)

Aysel qeydiyyatdan keçir (Google) → profil doldurur → İdman, Kafe, Musiqi seçir → +50 XP → Kəşf-də "Bu axşam yaxınlıqda canlı musiqi" tədbirini görür → "Qoşul" → qrup chat-ə düşür → eyni zamanda tədbirin keçirildiyi kafedə 4 nəfərlik stol rezerv edir → rezerv QR alır → axşam məkanda check-in edir (+40 XP, +30 XP) → tədbirdə tanış olduğu Elnur ilə match olur (+10 XP) → chat-də yazışırlar → sonradan Aysel rəy yazır (+15 XP) → XP 200-ü keçir → 15% kupon alır → növbəti rezervdə tətbiq edir.

---

## İş qaydaları (əlavə, 8-10)

8. XP, kupon və rezerv məntiqi YALNIZ serverdə hesablanır, klientə etibar edilmir. *(UI fazasında: mock simulyasiya, backend fazasında məcburi.)*
9. Hər pul/XP təsiri olan əməliyyat tranzaksiya və idempotency key ilə icra olunur. *(Backend fazası.)*
10. Aşağıdakı sualların cavabı olmadan backend Mərhələ 4-dən sonrasına keçilmir. *(UI fazası bloklanmır — mock.)*

## Açıq suallar (təsdiq lazımdır)

| # | Sual | Status |
|---|---|---|
| a | Partnyor paneli ayrı veb tətbiqdir, yoxsa mobildə ayrıca rol? | **[TƏSDİQ GÖZLƏYİR]** — UI-da placeholder bölmə |
| b | Onlayn ödəniş/depozit olacaqmı? | **[TƏSDİQ GÖZLƏYİR]** — bu mərhələdə ödəniş YOXDUR |
| c | Minimum yaş həddi 18-dirmi? | **[TƏSDİQ GÖZLƏYİR]** — UI default: 18+ yoxlaması aktiv |
| d | Qeydiyyat tələbi: aylıq aktiv istifadəçi sayı | **Cavablanıb** → D-022 (beta ≤4 tester → launch ölkə miqyası) |

## Mərhələlər (UI-first qaydası ilə yenidən sıralanıb)

Mövcud vəziyyət (D-023/D-024): **UI fazası birinci** — backend m2+ UI tam testdən keçəndən sonra.

| Mərhələ | Məzmun | Status |
|---|---|---|
| UI-1 (v0.2.0) | Web stili birebir: tema, font, əsas ekranlar | ✅ çatdırılıb (APK) |
| UI-2 (v0.3.0) | ROL strukturu: 5 tab, onboarding, match, rezerv, kupon, XP, bildiriş, moderasiya | ✅ çatdırılıb (APK) |
| UI-3 (v0.4.0) | Dizayn qatı: Liquid Glass minimal (5 səth) + hərəkət dili bütün bölmələrə (D-025, docs/design-spec.md) | ✅ çatdırılıb (APK + beta release) |
| UI-4 (v0.5.0) | İkitərəfli XP iqtisadiyyatı: XP Kartı, Biznes rejimi, təşkilatçı bitirmə axını, Kəşf peşəkar sıralaması, xəritə 3-seçimli görünüş (D-026/D-027, bölmə L) | ✅ çatdırılıb (APK + beta release) |
| UI-test | 4 tester tam yoxlama → düzəlişlər | — |
| **"UI hazır" təsdiqi** | — | **qapı: bundan sonra backend** |
| m2 | NestJS nüvə | gözləyir |
| 4 | Profil + Interests + Match (backend) | gözləyir |
| 4B | Venues + Reservations (slotlar, QR, statuslar) | gözləyir |
| 4C | XP + Coupons modulu | gözləyir |
| 5 | Chat (şəxsi + tədbir qrupları) | gözləyir |
| 6 | Bildirişlər | gözləyir |
| 7-8 | Flutter: real backend qoşulması | gözləyir (UI hazır olduğundan) |
| 9-10 | Launch hazırlığı, monitorinq | gözləyir |

## L. İkitərəfli XP iqtisadiyyatı (D-026 — istifadəçi əlavəsi)

Platformada **iki növ XP** var. Bu bölmə F (XP/səviyyə) və G (endirim/kupon)
bölmələrinin strukturunu dəyişir — ziddiyyət yarananda **bu bölmə üstün tutulur**.

### L.1. 1-ci XP — Peşəkarlıq XP (xərclənməz)

- Məqsəd: istifadəçinin aktivlikdə peşəkarlıq səviyyəsi. Çox oynayan insanın
  oyuna dair anlayışı artır; aşağı səviyyəli insanlarla oyunlar maraqsız keçir.
- Səviyyə pillələri F bölməsindəki kimi saxlanılır (Yeni → Əfsanə).
- **Xərclənmir** — yalnız səviyyə, nişan və Kəşf sıralaması üçün istifadə olunur.
- **Kəşf qaydası**: istifadəçinin səviyyəsi nə qədər yüksəkdirsə, Kəşf-də ona
  önə çıxan aktiviteler bir o qədər peşəkar olur (səviyyə uyğunluğu ilə sıralama).

### L.2. 2-ci XP — Endirim XP (obyektə-məxsus, xərclənən)

- Hər obyektin (restoran, kafe, otel, şirkət) **özünə məxsus ayrı XP bazası**
  var (İnstagram-model biznes hesabı ilə əlaqəli).
- **Ayrı baza qaydası**: Xankəndi-də qazanılan XP yalnız Xankəndiyə aiddir;
  Kokaltı kafesindən endirim almaq olmaz — bir obyekt qazanarkən digərinin
  ziyanına düşməsinin qarşısını almaq üçün.
- **XP müddəti YOXDUR** (TTL silinmə işi lazım deyil; balans = bütün grantlar −
  xərcləmələr, ledger cəmi; limitsiz yığılma biznes qərarı kimi qəbul edilib).

### L.3. Qazanma axını (statik QR — təşkilatçı tərəfi)

1. Aktivite bitəndə təşkilatçı obyektdən çıxışda **obyektin statik QR-ını oxuyur**.
2. Şəkilləri **yalnız təşkilatçı paylaşa bilər** → obyektin biznes hesabına düşür.
3. Paylaşarkən təşkilatçı **yalnız iştirakçıları taq edir**:
   - Təşkilatçı: **+50 XP** (başlanğıc default)
   - Hər taq edilən iştirakçı: **+25 XP**
   - Taq edilməyən iştirakçı: XP almır
4. XP məbləğləri sabit deyil → `xp_config` cədvəli (obyekt başına), işə
   düşdükdən sonra statistikaya görə tənzimlənəcək.

### L.4. Xərcləmə axını (dinamik QR — müştəri tərəfi, 6 addım)

Prinsip — **iki QR, simmetrik istiqamət**: qazanma obyektin statik QR-ı ilə,
xərcləmə müştərinin dinamik QR-ı ilə (bilet/loyallıq kartı modeli).

1. Müştəri app-də **"XP kartı"**nı açır → dinamik QR (**60 saniyəlik, tək
   istifadəlik nonce**; screenshot/kopyalama fırıldaqçılığı qarşısını alır).
2. Obyekt işçisi (app-in biznes rejimi) QR-ı oxuyur → obyekt bazasında
   müştərinin balansı görünür.
3. İşçi hesabın məbləğini yazır.
4. Sistem endirimi hesablayır (`xp_config` məzənnəsi ilə, məs. 100 XP = 5 AZN).
5. DB transaksiyası: balans **atomik** yoxlanır və düşülür → double-spend
   imkansız.
6. Hər iki tərəfə anında **receipt** (ledger qeydi).

**Obyekt başına konfiqurasiya (xp_config):** məzənnə (obyekt təyin edir),
maksimum endirim = hesabın X%-i (obyekt ziyanından qorunma), minimum hesab
məbləği (isteğe bağlı). MVP: müştəri təsdiqsiz, receipt push dərhal; yüksək
məbləğ üçün obyekt təsdiq tələbi sonradan açıla bilər.

### L.5. Kanal qaydaları

- QR = fiziki obyektlərdə əsas kanal. **Fallback**: QR altında 6 rəqəmli kod
  (kamera işləməyəndə). Platformadaxili xərclər (gələcək: biletlər) QR-sız,
  app içi. **NFC → MVP-də yoxdur.**

### L.6. G-b bölməsinə dəyişiklik (kupon alışı)

Köhnə qayda ("ümumi XP balansı ilə kupon al") ləğv olunur: XP ilə alınan
kuponlar **həmin obyektin Endirim XP balansından** alınır (ayrı baza qaydası).
Səviyyə-avto endirimlər (G-a) dəyişməz qalır — Peşəkarlıq XP-si ilə açılır.

### L.7. Xəritə görünüşü seçimi (D-027)

ROL TAB 2 məxfilik genişlənməsi — görünüş 3 seçimli olur:
**Hamı / Yalnız dostlar / Gizli** (köhnə ikili açar əvəzinə). "Yalnız dostlar"
rejimində yerin yalnız səninlə dost olan istifadəçilərə görünür.

### UI-4 (v0.5.0) əhatəsi

XP Kartı (balanslar + dinamik QR + 6 rəqəmli fallback + tənzimləmələr +
ledger), Biznes rejimi (işçi: skan → balans → məbləğ → endirim → receipt),
təşkilatçı bitirmə axını (statik QR → şəkil paylaş + taq → 50/25 paylanma),
Kəşf peşəkar sıralaması, xəritə 3-seçimli görünüş, profildə ikili XP göstərişi.
Hər şey mock-data — backend qaydaları (server-only hesab, idempotency,
atomiklik) m2+ fazasında.
