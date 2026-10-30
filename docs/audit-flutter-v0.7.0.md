# Flutter tətbiqinin tam auditi və native yenidən yazma bazası (Phase 1)

**Audit obyekti:** `mobile/` (Flutter, v0.7.0+7, commit `fee9bd4`)
**Audit tarixi:** 2026-10-30 · **Metod:** bütün 42 Dart faylının sətir-sətir oxunması + testlərin + CI-in təhlili
**Məqsəd:** Kotlin + Jetpack Compose native yenidən yazması (D-036) üçün funksional/vizual spesifikasiya + baza ölçmələri

---

## 1. İcra xülasəsi

Flutter tətbiqi **yüksək keyfiyyətli UI-prototip**dir: məhsul məntiqi (ROL B–I + D-026 XP
iqtisadiyyatı) tam simulyasiya olunub, 89 test keçir, vizual dil web ilə birebir (D-023).
Kod təmizdir, arxitektura sadədir (tək `ChangeNotifier` + mock data), sıfır xarici asılılıq var.

**Audit nəticəsi:** yenidən yazma üçün uyğundur — məntik yaxşı sənədləşib və testlə örtülüb;
10 qüsur tapıldı (bölmə 5), onlar native versiyada düzəldiləcək (paritet + düzəliş siyahısı, bölmə 6).

| Metrik | Dəyər (v0.7.0) |
|---|---|
| Dart kodu (`lib/`) | 42 fayl · **14 501 sətir** |
| Testlər (`test/`) | 5 fayl · 1 447 sətir · **89 test** (CI-da keçir) |
| Xarici paketlər | **0** (yalnız Flutter SDK — D-023) |
| Release APK (universal) | **55.6 MB** (3 ABI + flutter_assets; run 40936920295) |
| CI tam run (analyze+test+build+release+web) | ~6 dəq (GitHub-hosted, ölkədaxili şəbəkədən asılı) |
| Soyudma başlanğıcı (cold start) | **ölçülməyib** — cihaz yoxdur (bölmə 7-də açıq qalıır) |
| Kadrsürətli/əməli yaddaş | **ölçülməyib** — cihaz yoxdur |

---

## 2. Struktur inventarı

```
lib/
├── main.dart                      (112) — edge-to-edge (D-031), adaptiv GlassQuality (D-032)
├── core/
│   ├── state/app_state.dart      (1009) — MƏRKƏZİ MƏNTİK: XP, match, rezerv, kupon,
│   │                                      bildiriş, moderasiya, assistant, XP kartı
│   ├── data/mock_data.dart        (997) — tədbirlər, insanlar, məkanlar, şəhərlər,
│   │                                      kateqoriyalar, slotlar, təkliflər, XP bazaları
│   ├── models/                    (851) — models, coupon, reservation, notification,
│   │                                      moderation, xp_economy
│   ├── theme/app_theme.dart       (194) — Q (rəng/radius/kölgə) + T (tipo) tokenləri
│   ├── widgets/                  (1821) — ui (705), rol (477), motion (410),
│   │                                      glass (147), brand (82)
│   └── platform/native_bridge.dart (46) — MethodChannel qoshul/native (perfInfo, immersive)
└── features/ (18 modul, təqdimat qatı)
    feed (400) · shell (476) · events (2055/6 fayl) · map (969/2) · match (734)
    · chats (425) · profile (495) · settings (436) · partners (613/2)
    · venues (610/2) · xp (418) · coupons (232) · reservations (184)
    · assistant (216) · notifications (190) · onboarding (658) · auth (316) · safety (101)
```

Platform qatı: `mobile/platform/android/` — `MainActivity.kt` (Kotlin: edge-to-edge
güvəncəsi + immersive keçidləri) + `NativePerfInfo.java` (memoryClass/lowRam/cores).

---

## 3. Dizayn sistemi (native port üçün mənbə-of-həqiqət)

### 3.1 Rənglər — `Q` (OKLCH→sRGB, D-023)

| Token | HEX | İstifadə |
|---|---|---|
| background | `#F8FAFC` | səhifə fonu |
| foreground | `#0F172B` | əsas mətn |
| card | `#FFFFFF` | kartlar |
| primary | `#00A63E` | brend yaşıl |
| primaryFg | `#FFFFFF` | primary üzərində mətn |
| secondary | `#E6F7EA` | seçilmiş/vurgu fonu |
| muted | `#EEF2F5` | neytral fon |
| mutedFg | `#687280` | ikinci dərəcəli mətn |
| accent | `#FE9A00` | canlı göstərici (nöqtə) |
| accentFg | `#7A3800` | narıncı çip mətni (rankBadge) |
| destructive | `#E7000B` | KEÇ möhürü, ləğv |
| border | `#E3E8ED` | haşiyələr |
| rankBadgeBg | `#FEF2DE` | səviyyə çipi fonu |
| overlayDark / photoShade | `#002211` / `#00140E` | şəkil kölgələri |
| glass | `#21000000` (13% qara) | canlı nişan fonu |
| glassBorder | `#2EFFFFFF` (18% ağ) | canlı nişan haşiyəsi |

Radius: rSm 4 · rMd 6 · rLg 8 · rXl 8 · rMap 18 · rAiBox 16 · axtarış 15 · nav pill 24 ·
kapsul 16 · şüşə həb 14/10. Kölgə: `softShadow` = blur 18, offset (0,3), `#0B28301F`;
nav pill blur 22 offset (0,6) `#1428301F`; alt kart blur 26 offset (0,8) `#1E28301F`.

### 3.2 Tipografiya — `T` (Plus Jakarta Sans 400–800; D-035 mobil şkalası)

hero 27/800 · h1 24/800 · h2 19/800 · h3 13/800 · eventTitle 16/800 · personName 15/800 ·
stat 22/800 · subtitle 13/400 (mutedFg, h1.7) · body 13.5 · bodySm 12.5 · meta 12 (mutedFg) ·
metaSm 10.5 · category 10.5/700 (primary) · chip 11.5/500 · chipActive 11.5/700 (ağ) ·
button 13/600 · label 12/600 · input 13.5 · axtarış hint 13.5 · nav label 10.5 ·
glassText 9–14/700 · möhür 13/800.

### 3.3 Hərəkət dili — `Motion` (D-025)

d1 150ms (mikro) · d2 250ms (standart) · d3 400ms (vurğulu) · d4 550ms (səhnə) ·
stagger 45ms. Əyrilər: ease=easeOutCubic, enter=easeOutBack, spring=elasticOut, decel.
Primitivlər: StaggerIn (fade+14px, max 8 addım), PopIn (0.6→1 bounce), ScaleTap (0.97),
CountUp, TypingDots, Shimmer, FadeThrough (0.94→1 miqyas), GlassPageRoute (fade+18px+0.98→1).
`disableAnimations` hörmət olunur (bütün primitivlər 0ms).

### 3.4 Liquid Glass (D-025 — minimal 5 səth)

blur 12–18 + işıq qradiyenti (luminance 0.34–0.58) + 1px spekulyar kənar:
① alt nav pill (sigma 18, lum 0.52, radius 24, h 68) ② xəritə filtr paneli (sigma 16) ③
xəritə alt kartı (sigma 18, lum 0.58) ④ sayğac/rejim həbləri ⑤ match fəaliyyət həbləri.
`GlassQuality.blur=false` → ucuz fallback (yarı-şəffaf ağ, haşiyə 0.6 ağ).

### 3.5 Naviqasiya (D-034)

Auth → Onboarding → Shell. Shell: yuxarıda 67px başlıq (wordmark + tərəfdaşlar + AI + zəng
badge), aşağıda şüşə pill: **Kəşf · Xəritə · [Yarat mərkəzi] · Söhbətlər · Profil**.
Kəşf daxili tabları: **Sənin üçün | Tədbirlər** (pinned). Push-ekranlar: Tərəfdaşlar, AI
köməkçi, Bildirişlər, Tanış ol, Məkan detal, Endirimlər, Rezervlərim, XP kartım, Ayarlar,
Təhlükəsizlik. Bottom-sheet-lər: tədbir detalı, yaratma, bitirmə, şəhər, rezerv, biznes,
bildirişlər, report.

---

## 4. Məhsul məntiqi kataloqu (paritet üçün məcburi siyahı)

### 4.1 XP mühərriki (ROL F + D-026)
- **Peşəkarlıq XP** (xərclənmir): başlanğıc 320; səviyyələr Yeni≤99 < Kəşfiyyatçı≤299 <
  Aktiv≤699 < Sosial≤1499 < Lider≤2999 < Əfsanə; `rankForXp/rankIndexForXp/xpProgress`.
- İdemponent `_award(key, amount)` (`_xpEventIds`); səviyyə artımı → levelUp bildirişi.
- Hadisələr: qeydiyyat +50, profil-100% +30 (şəkil+bio+≥3 maraq), telefon +40 (badge
  `verified`), günlük giriş +5 (`login-YYYY-MM-DD`), 7-gün seriya +30 (sonra 1-ə düşür),
  qoşulma +10 (`join-$id`), check-in +40 (`checkin-$id`), host (öz tədbiri, ≥3 iştirakçı)
  +60 (`hosting-$id`), rezerv istifadəsi +30 (`resv-$id`), rəy +15 (`review-$id`,
  həftəlik cap 3), match +10 (`match-$pid`, gündəlik cap 3), dəvət +100 (yoxdur UI-da).
- Nişanlar: firstEvent (ilk check-in), tenEvents (≥10), traveler (başqa şəhər),
  nightOwl (saat ≥20), verified (telefon).
- **Endirim XP** (D-026, obyektə-məxsus, xərclənir): venueId → VenueXpAccount{balance,
  config(xpPerAzn, maxDiscountPercent, minBillAzn), ledger}. İlkin: v5=325 (20₼/AZN,
  maks25%, min10), v2=120 (25, maks20), v6=0, v1=340 (maks30), v4=150 (30, maks20).
- `previewSpend`: balans/minBill/cap/hesab yoxlamaları → {ok, discount, error};
  `spendVenueXp`: atomik düşüm + ledger + bildiriş; `discountXpTotal` = Σ balanslar.
- **XP kartı (L.4)**: XpCardCode{nonce `QSHUL-XP-xxxxxx-yy`, 6 rəqəmli fallback, 60s
  TTL, tək-istifadə}; `regenerateXpCard/markXpCardUsed/xpCardValidFor`.
- **Bitirmə axını (L.3)**: `canFinishActivity` = past ∧ venueId≠null ∧ host=mən ∧
  bitməyib; `finishActivity(e, tagged)`: təşkilatçı +50 grant, hər taq +25 (yalnız
  sayğac göstərilir), ActivityXpResult, bildiriş; `finishedActivities` idempotenti.
- **Peşəkar sıralama (L.1)**: `proWeight(level)` Peşəkar3/Orta2/Başlanğıc1/0; Kəşf
  siyahısı çəki = w·(rankIndex+1) üzrə desc.

### 4.2 Match (ROL D)
`matchCandidates` = swipe-olunmamış ∧ bloklanmamış ∧ məhdudlaşdırılmamış. Swipe: Maraqlıyam/
Keç; `likesBack` → match: `matches`+`friendships[accepted]`+boş chat+match bildirişi+XP
(gündə 3). Kart dəsti: üst kart dartılır (110px hədd, bucaq dx/22, maks 0.5), uçur
(±560,-70, 0.55 rad) və ya yay geri; MARAQLIYAM/KEÇ möhürləri; dərinlik: -12px/0.95.
Report: 5 səbəb; Blok: qarşılıqlı görünməzlik (chat+match+dostluq silinir). 3+ report →
`restrictedUsers` (kandidatlar siyahısından yox olur).

### 4.3 Rezerv (ROL E)
Axın: tarix (7 gün: Bu gün/Sabah/Ay Gün) → slot (venueSlotTimes) → nəfər 1–8 → qeyd +
kupon (aktiv, bu məkan) → təsdiq. Status: avto-tesdiq məkan → TƏSDİQLƏNDİ, əks halda
GÖZLƏYİR; QR `QSH-rN-xxxx`. `checkInReservation` (yalnız TƏSDİQLƏNDİ): +30 XP, kupon
İSTİFADƏ OLUNDU. `cancelReservation` (GÖZLƏYİR/TƏSDİQLƏNDİ). `markNoShow` ×3 →
`restrictedUntil` +7 gün (rezerv qadağası, ekranlarda xəbərdarlıq).

### 4.4 Kuponlar (ROL G)
Təkliflər: o1 v2 15%/200XP, o2 v1 20%/300XP (səviyyə≥Aktiv), o3 v4 10%/100XP/30gün,
o4 v2 5% avto (Aktiv+), o5 v3 5₼ avto (Kəşfiyyatçı+). `visibleOffers` = səviyyə-avto
yalnız açılıbsa. `buyCoupon`: səviyyə ∧ obyekt balansı ≥ xpPrice → `_spendLedger`,
kod `QSHUL-NNNN-VENUE`, müddət 14 gün (o3: 30), bildiriş. Statuslar AKTİV/
İSTİFADƏ OLUNDU/VAXTIBİTMİŞ; `daysLeft`; QR dialoqu. Vaxtı bitən üçün XP qayıtmır.

### 4.5 Bildirişlər (ROL H)
11 tip (match, message, eventJoin, eventApproval, eventReminder, resvUpdate, resvReminder,
levelUp, couponExpiring, nearbyEvent, marketing) — hər biri ayrıca söndürülür; marketinq
23:00–08:00 sükut; siyahı maks 50; oxunmamış sayı başlıq badge-də; "Hamısını oxunmuş et".

### 4.6 Tədbirlər (ROL TAB 3)
Join: approval → `pending`, əks halda `joined`+XP; leave; toggleSave; check-in (yalnız
joined, bir dəfə); yaratma: id `cN-ts`, host=profileName, participants=1, avtomatik
joined, imageForCategory (İdman/Təbiət→padel, Yemək→coffee, Texnologiya/Biznes/Təhsil→tech,
əks halda friends); rəy forması (keçmiş+joined): 5 ulduz + şərh, +15 XP.
Siyahılar: Yaxınlaşan (upcoming) / Mənim (joined∪pending) / Yaratdığım (host=mən);
kateqoriya çipləri 13 qrup üzrə; axtarış (başlıq+məkan+kateqoriya+şəhər).

### 4.7 Söhbətlər (ROL TAB 4)
Qruplar: joined tədbirlər (e1/e5 başlanğıc mesajları); Şəxsi: acceptedFriends (p2 Rəşad
ilkin). Mesaj göndər → 1.2s sonra mock cavab (3 variant); "yazır..." 1.4s; oxunma
statusu (son mesajım: çatdırıldı→oxundu). Menyu: Report/Blokla/Söhbəti sil.

### 4.8 Assistant (mock AI)
Təklif çipləri (4 sual). Cavab qaydaları: "fikir/plan/şənbə..." → [TƏDBİR] JSON təklifi
(Qala yürüşü, gələn şənbə ISO); "pulsuz" → şəhərin 3 pulsuz tədbiri; "idman" → İdman/
Təbiət qruplarının 3-ü; "gəncə" → Gəncə tədbirləri; əks halda → şəhərin 2 tövsiyəsi.
Yazı simulyasiyası: 24ms-də 2 simvol. `[TƏDBİR]` parser → AiBox təklif kartı →
"Bu tədbiri yarat" (create sheet prefill).

### 4.9 Xəritə (ROL TAB 2 + D-027/D-030)
Pinlər: məkanlar (şəhər+kateqoriya+açıq+endirim filtrləri), tədbirlər (məsafə filtri),
insanlar (görünüş rejiminə görə: Hamı=hamı razılıq verən / Yalnız dostlar=accepted /
Gizli=heç kim) + "mənim yerim" (Bakı 40.4093,49.8671, göy nöqtə). Filtr paneli (şüşə):
5 kateqoriya çipi + 1/3/5/10 km + "indi açıqdır" + "endirimi var". Sayğac həbi; görünüş
rejimi həbi (friends/off). Zoom həbləri (±). Pin: məkan=yaşıl storefront, tədbir=accent
event, insan=baş hərfi; seçilmiş 34→40px. Alt kartlar (şüşə): məkan (şəkil, reyting,
qiymət, endirim, Ətraflı/Rezerv et), tədbir (Ətraflı), insan (Tanış ol).
Tile renderer: Web Mercator, OSM tile + UA `Qoshul.az/0.6.0 (beta; https://qoshul.az)`,
fallback `#EDF3EF`, pinch commit düsturu, marker qatı sabit pin ölçüsü, atribusiya.

### 4.10 Profil / Ayarlar / Onboarding / Auth / Təhlükəsizlik / Tərəfdaşlar
- **Profil**: redaktə (ad), ayarlar; avatar (şəkil və ya baş hərfi, yaşıl halqa);
  RankBadge + Təsdiqlənmiş; şəhər·status·təhsil maraqlar Wrap; şəxsiyyət testi banneri +
  trait zolaqları; 5 nişan (PopIn stagger); 3 tez keçid (Rezervlərim/XP kartım/Kuponlarım);
  statistika (CountUp: Peşəkar XP, Endirim XP, Tədbir, Səviyyə); XP progress; tablar
  Tədbirlərim/Yadda saxlanılanlar/Nailiyyətlər.
- **Ayarlar**: Hesab (ad, şəhər, səviyyə, telefon təsdiqi +40XP+badge); Məxfilik
  (görünüş 3-seçimli dialog, məsafə keçidi); Bildiriş tercihleri; Təhlükəsizlik
  (məsləhətlər, bloklananlar, dil, haqqında, şərtlər); hesabı sil; çıxış.
- **Onboarding** (ROL B): 0) 3 slayd (Kəşf et/Qoşul/Qazan) + Keç; 1) profil (ad, doğum
  tarixi 18+ yoxlaması, cins ist., şəkil 1–6, bio 150); 2) maraqlar (9 kateqoriya, min 3
  maks 8); 3) icazələr (məkan + bildiriş, rdd mümkün; mock dialoqlar) → `finishOnboarding`
  (+50 XP, demo host adoptu, dailyLogin).
- **Auth** (mock): Daxil ol / Qeydiyyat (ad, e-poçt, şifrə ≥8, təkrar) → 6 rəqəmli kod
  (istənilən) → signedIn; "Şifrəni unutmusan?" → link mesajı; Google/Apple → 900ms → signedIn.
- **Təhlükəsizlik**: 5 məsləhət kartı (nömrələnmiş) + koordinat məxfiliyi banneri.
- **Tərəfdaşlar**: "Öz məkanını əlavə et" + "Biznes rejimi" bannerləri; məkan kartları
  (şəkil 200px, kateqoriya, ünvan, deal+saat, "Burada tədbir yarat" prefill).
- **Biznes rejimi** (L.4 işçi tərəfi): obyekt seç → müştəri QR oxu (xpCardValidFor;
  alınmadı → izah xətası) → balans kartı (CountUp) + məzənnə → hesab ₼ + XP → canlı
  endirim önizləməsi (balans/minBill/cap xətaları AZ dilində) → təsdiq → spendVenueXp +
  markXpCardUsed → receipt (XP, qalıq, ledger id).
- **Məkan detal**: SliverAppBar şəkil 230px + kölgə; ad + endirim çipi; reyting ulduzları
  (count); info kart (ünvan, saat+açıq/bağlı, qiymət aralığı, avto/manuel təsdiq);
  aktiv təklif banneri; rəylər (VenueReview); Yol tap (snackbar) + Rezerv et.
- **XP kartım**: Peşəkarlıq kartı (səviyyə, xərclənmir); obyekt balans kartları
  (balansa görə desc; genişlənən: dinamik QR 124px + geri sayım 60s + fallback kod +
  Yenilə + ledger son 4 qeyd).

### 4.11 Mock data dəsti (birebir köçürüləcək)
14 tədbir (e1–e12, e0 keçmiş, e-mafiya keçmiş+host Dost+venue v5, e-chess Peşəkar) ·
5 insan (p1–p5, likesBack: p1,p2,p5) · 6 məkan (v1–v6; v1 avto-tesdiq YOX) · 25 şəhər
(koordinatlarla) · 11 kateqoriya qrupu (78 kateqoriya) · status/universitet/dil/məqsəd/
uyğunluq/büdcə siyahıları · 11 şəxsiyyət sualı (5 trait) · venueSlotTimes (v1–v4) ·
mockVenueReviews · 5 kupon təklifi · 3 ilkin bildiriş · 5 ilkin XP hesabı (ledger ilə) ·
ilkin söhbətlər (e1×3, e5×2, p2×2).

---

## 5. Tapılan qüsurlar (auditin əsas dəyəri)

| # | Qüsur | Yeri | Ağırlıq | Native planda |
|---|---|---|---|---|
| A1 | **Köhnə versiya sətri** "Haqqında (v0.3.0)" — v0.7.0-da belə | settings_screen | kosmetik | düzəldiləcək (BuildConfig-dən) |
| A2 | **Həftəlik rəy limiti heç vaxt sıfırlanmır** — `_reviewXpThisWeek` yalnız artır; "həftədə max 3" qaydası ilk həftədən sonra ölür | app_state | məntiq | həftə dönüşü əlavə olunacaq |
| A3 | **Ölü düymələr**: "Qrup söhbətinə keç" (event detail, boş onPressed), "Yol göstər/Yol tap" (yalnız snackbar), çat başlığında təqvim ikonu (boş) | event_detail, venue_detail, chats | UX | Söhbətlər tabına keçid gerçekləşdiriləcək; yol/təqvim "tezliklə" işarəsi ilə deaktiv |
| A4 | **Saxta avatar stack**: 3 boş yaşıl dairə (iştirakçı şəkilləri yoxdur) — vizual placeholder kimi qalır | event_card | vizual | iştirakçı sayı çipi ilə əvəz (dürüst UI) |
| A5 | **Toxunma hədəfləri 48dp-dən kiçik**: bookmark 34×34, bəzi düymələr 34px hündürlükdə | bir çox kart | accessibility | min 48dp toxunma sahəsi |
| A6 | **Sərbəst mətnli tarixlər** ('Şənbə', 'Bu gün', 'Noy 8') + `past` bool — real tarix sıralaması/filtri mümkün deyil | models/mock | məlumat | `epochDay: Int` əlavəsi (görüntü etiketi dəyişməz) |
| A7 | **İnsan pinləri saxta koordinatlarla** (match%/yaşdan törədilib) | map_screen | məxfilik/etik | sabit real-vari koordinat dəsti |
| A8 | **OSM tile birbaşa istehsal istifadəsi üçün qadağadır** (bulk policy) — D-030 m9 qeydinə baxmayaraq beta-sonrası risk | tile_map | uyğunluq | Phase 8 qərarı ilə birlikdə həll (bax docs/decisions.md D-037) |
| A9 | **XP kartı ekranı hər saniyə TAM ekranı yenidən qurur** (Timer.periodic + setState) | xp_card_screen | performans | yalnız geri sayım hissəsi recompose |
| A10 | `_mockReply` `hasListeners` yoxlaması ilə `notifyListeners` arasında yarış (dispose olunan ekranda printual exception) | app_state | minor | coroutine ləğvi ilə təhlükəsiz |

**Qeyd:** A2, A6, A7, A9 düzəlişləri məhsul məntiqinə toxunur — native versiyada həyata
keçirilir, lakin Flutter tətbiqi toxunulmaz qalır (mənbə spesifikasiya, D-036 qaydası:
mobile/ yalnız paritet təsdiqindən sonra dəyişilir).

---

## 6. Paritet siyahısı (native qapı meyarları)

Native tətbiq aşağıdakıların HAMSINA malik olmalıdır (yoxlanış listi kimi istifadə
olunacaq — hər bənd Phase 9-da birebir yoxlanılır):

- [ ] Dizayn tokenləri: 17 rəng + 10 radius + kölgələr + 20 tipo stili (bölmə 3.1–3.2)
- [ ] Hərəkət dili: d1–d4 + 4 əyri + 8 primitiv + disableAnimations ekvivalenti
- [ ] Liquid Glass 5 səth + ucuz fallback (reduced-motion/aşağı API)
- [ ] Shell: 4 tab + mərkəzi Yarat + başlıq (wordmark/3 ikon/badge) + edge-to-edge
- [ ] Kəşf: daxili tablar, pinned axtarış (2 sahə), salam+RankBadge, XpTrack, "Bu axşam"
      körpüsü, məkanlar (mini kartlar), insanlar (mini kartlar), endirimlər (yalız kartlar),
      cəmiyyət banneri, SafetyNote
- [ ] Tədbirlər: axtarış, 13 kateqoriya çipi, 3 seqment, "Sənin səviyyənə görə" (L.1),
      EventCard (şəkil+nişanlar+bookmark+məsafə+join), məkan rezerv xatırlatması
- [ ] Tədbir detalı: facts grid, join/pending/full/check-in/rəy/leave/bitmə axını düymələri
- [ ] Yaratma: AI mock doldurma, bütün sahələr + validasiya (ad, məkan, 2–500, min 1 AZN)
- [ ] Bitirmə: 4 addım (statik QR → şəkil ≥1 → taq ≥1 → nəticə), canlı XP önizləməsi
- [ ] Şəhər seçici: axtarış + 25 şəhər + region
- [ ] Xəritə: bütün pin növləri + filtrlər + alt kartlar + sayğac + rejim həbi + zoom +
      atribusiya + mənim yerim (bölmə 4.9)
- [ ] Match: swipe dəsti fizikası, möhürlər, match dialoqu, report (5 səbəd), blok,
      boş və bitmiş dəst halları, "Dəsti yenilə (demo)"
- [ ] Söhbətlər: siyahı (Qruplar/Şəxsi), chat görünüşü (başlıq, yazır..., statuslar,
      göndər), report/blok/sil, mock cavab
- [ ] Profil: bütün bölmələr (bölmə 4.10), 3 tab
- [ ] Ayarlar: bütün sətirlər + 4 dialoq (telefon, görünüş, bloklananlar, silmə)
- [ ] Onboarding: 4 addım + validasiyalar (18+, min 3 maraq) + finishOnboarding yan təsirləri
- [ ] Auth: 4 rejim + validasiyalar + mock gecikmələr
- [ ] Təhlükəsizlik, Tərəfdaşlar, Biznes rejimi (4 addım + xəta halları), Məkan detal,
      Rezerv axını (5 addım + nəticə + QR), Endirimlər (2 tab + alış + QR dialoqu),
      Rezervlərim (status çipləri + check-in/ləğv + noShow xəbərdarlığı), XP kartım
      (dinamik QR + geri sayım + ledger), Bildirişlər (siyahı + 11 tercih)
- [ ] XP mühərriki: bölmə 4.1-in HƏR qaydası (idempotency, cap-lər, səviyyələr, ledger)
- [ ] Mock data: bölmə 4.11 dəsti birebir
- [ ] Testlər: 89 Flutter testinin əhatəsi (bölmə 8)
- [ ] Düzəlişlər: A1–A10 (A8 istisna — Phase 8 qərarı)

---

## 7. Baza ölçmələri (Phase 9 müqayisəsi üçün)

| Metrik | Flutter v0.7.0 (baza) | Native (ölçülmüş, 2026-10-30) | Nəticə |
|---|---|---|---|
| APK ölçüsü (release, universal) | 55.6 MB | **2.76 MB** (R8 + resource shrink; debug 21.4 MB) | **20.2× kiçik** |
| Kod həcmi | 14 501 sətir Dart (42 fayl) | **11 911 sətir Kotlin** (38 fayl) | 18% az |
| Məntiq testləri | 61 (89-dan 28-i widget/no-op) | **67 JVM testi** — hamısı keçir (CI run 41079665996) | əhatə +
| CI build vaxtı (təmiz, cache-siz) | ~5 dəq (D-015) | ~6 dəq (JDK+Gradle download daxil; cache ilə ~3 dəq) | ≈ bərabər |
| Soyuq başlanğıc | ölçülməyib (cihaz yoxdur) | ölçülməyib | **BƏYANAT: hər ikisi cihazda ölçülməlidir** |
| Kadrsürəti (jank) | ölçülməyib | ölçülməyib | **BƏYANAT: Macrobenchmark — gələcək mərhələ** |

**Nəticə (yalnız ölçülmüş metrikalar üzrə):** native versiya APK ölçüsündə 20×
daha yüngül, kodda 18% daha kompakt, məntiq test əhatəsində daha geniş; build
vaxtı bərabərdir. Soyuq başlanğıc / kadrsürəti iddiaları bu mühitdə ölçülə
bilmədiyindən **verilmir** — dürüstlük qaydası (D-036).

**Dürüstlük qaydası (D-036):** soyuq başlanğıc və kadrsürəti bu mühitdə ölçülə bilmir
(-cihaz/emulator yoxdur). Phase 9 müqayisəsi yalnız **ölçülə bilən** metrikalar üzrə
(APK ölçüsü, build vaxtı, test əhatəsi, kod həcmi) veriləcək və ölçülməyən ölçülər
açıq şəkildə "ölçülməyib" kimi qeyd olunacaq. Native-in "daha yaxşı" olması yalnız
rəqəmlərlə sübut olunacaq; sübut olunmayan iddialar verilməyəcək.

---

## 8. Test xəritəsi (port planı)

| Flutter testi | Say | Native ekvivalenti |
|---|---|---|
| `rol_test.dart` — XpRules, XP mühərriki, match, rezerv, kupon, bildiriş, moderasiya, onboarding | 38 | `XpRulesTest`, `AppStateTest` (JVM) |
| `xp_economy_test.dart` — XpCardCode, MapVisibility, VenueXpConfig, previewSpend, spendVenueXp, finishActivity, L.1 sıralama | 23 | `XpEconomyTest` (JVM) |
| `motion_test.dart` — glass + motion primitivləri | 14 | `MotionSpecTest` (tokenlər) + UI test (əgər Robolectric işə düşərsə) |
| `widget_test.dart` — feed/shell/xəritə UI | 11 | Compose UI testləri (Robolectric) |
| `native_bridge_test.dart` — no-op platform | 3 | (native-də mənasızdır — JVM-də BridgeTest kiçik ekvivalent) |
| **Cəmi** | **89** | hədəf: ≥ 89 logic testi |

---

*Bu sənəd D-036 (native yenidən yazma qərarı) ilə birlikdə oxunmalıdır. Native tətbiq
`android-app/` qovluğündə qurulur; `mobile/` (Flutter) paritet təsdiqi və owner razılığına
qədər toxunulmaz qalır.*
