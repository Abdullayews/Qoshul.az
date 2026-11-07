# Qoshul.az — Rust + Kotlin Miqrasiya Araşdırma Hesabatı (Mərhələ 1)

> **Status:** Mərhələ 1 (ARAŞDIRMA) tamamlanıb ✅ · **M0 (bug fixlər) icra olundu**
> (istifadəçi təsdiqi: 2026-11-07 „bəli düzəlişlərə başla" — QAYDA 0 qapısı açıldı;
> qərarlar D-072…D-075, 140 test). **M1 (Supabase backend) icra olundu**
> (D-076…D-079, 165 test; sxem+RLS+qaydalar+ikili rejim; CI yaşıl + v0.17.0-beta release). **M2 (Rust nüvə + JNI) icra olundu** (D-080, 176+27 test; paritet host .so ilə sübut). Növbəti qapı: **M3** (Nayuki Java → Rust qrcodegen; Python → Rust fonts; ZXing → quircs).
> **Tarix:** 2026-11-07 · **Executor icra promptu:** v1.0 (2026-11-07)
> **Yoxlanan baza:** `main` @ `9c553d6` (D-065, v0.15.0) · `server-only` @ `9f1fb01`
> **Metod:** yalnız read-only əməliyyatlar (QAYDA 0); hər iddia fayl:sətir dəlili ilə;
> versiyalar rəsmi API-lərdən (crates.io, Maven Central) yoxlanılıb.
> **Qeyd:** prompt-un §1/§2.2 „hazır fakt"ları hipotez kimi qəbul edilib, hamısı
> yenidən yoxlanılıb — 3 tapıntıda dəqiqləşdirmə, 1 praktik riskdə təkzib var.

---

## 0. İcmal

| Kateqoriya | Nəticə |
|---|---|
| §2.2 kod bugları (A1–A4) | **4/4 TƏSDİQ** (A1 dəqiqləşdirilib: 3 yoldan yalnız 1-i səhv açardadır) |
| §2.2 spec-kod boşluqları (B1–B7) | **7/7 TƏSDİQ** + 1 yeni ölü sabit (B7 içinə) |
| Açıq dizayn sualı (C1) | **TƏSDİQ** — cari davranış REDD; qərar istifadəçidən gözlənilir |
| Metodoloji dərs (D1) | **TƏSDİQ** — bug yoxdur; `native.yml:7` düz mətn `branches: [main]` |
| Yeni tapıntılar (bu araşdırma) | **N1–N13** (aşağıda) — 2-ü sənədləşmə/idarəetmə, 11-i kod |
| Dil inventarı §1.2 | **TƏSDİQ** — 55 kt · 4 java (Nayuki) · 1 py · ZXing yalnız test |
| Test/CI §1.4 | **TƏSDİQ** — 121 test / 9 fayl; CI `paths:` filtri ilə (`android-app/**`) |
| Versiyalar §1.3 | **TƏSDİQ** — libs.versions.toml dəyərləri README ilə uyğundur |
| Əvvəlki tapşırıq (AI fayllarının silinməsi) | D-065-də (2026-11-07, `9c553d6`) icra edilib — repoda AI faylı QALMAYIB |

---

## 1. Yoxlama cədvəli — §2.2 tapıntılarının yenidən yoxlanması

### A1. `previewSpend` xəta açarı — **TƏSDİQ (dəqiqləşdirmə ilə)**

**Prompt iddiası:** `:682` və `:689` hər ikisi `"hesap"` qaytarır.
**Real vəziyyət (3 yoldan yalnız 1-i):**

| Yol | Yer | Qaytarılan açar | UI handling |
|---|---|---|---|
| Hesab tapılmadı | `AppEngine.kt:682` | **`"hesap"`** (türk orfoqrafiyası) | ❌ `else -> ""` — səssiz |
| Məbləğ/XP ≤ 0 | `AppEngine.kt:684` | `"hesab"` (düz) | ✅ PartnersScreen.kt:373 |
| `xpPerAzn ≤ 0` | `AppEngine.kt:689` | `"hesab"` (düz) | ✅ PartnersScreen.kt:373 |

- İstehlakçı: `PartnersScreen.kt:368-374` — `when (p.error)` yalnız
  `"balans"`/`"minBill"`/`"cap"`/`"hesab"` gözləyir, `else -> ""`.
- Sənədləşdirmə: `XpEconomy.kt:123` — `'hesab'` deyir (düz).
- **Təsir:** venue XP hesabı olmayan obyektdə xərcləmə cəhdi UI-da heç bir
  izahat almir. Hazırda latentdir (PartnersScreen-də hesab həmişə mövcuddur),
  server `xp_config`-dən gələndə real istifadəçi yoluna düşə bilər.
- **Düzəliş (M0):** xəta açarları string → sealed/enum tip (Rust-da `enum SpendError`,
  string açarlar qadağan).

### A2. Vaxt mənbəyi parçalanması — **TƏSDİQ + GENİŞLƏNDİRİLDİ**

`clock: () -> LocalDateTime` injeksiyası mövcuddur (`AppEngine.kt:56-61`,
sənəd :49-50 „vaxt idarə olunur"), amma bypass edilir. **Tam siyahı:**

| # | Yer | Məqam | Prompt-da varmı? |
|---|---|---|---|
| 1 | `AppEngine.kt:100` | `matchXpDay` field-initsializatoru | ❌ yeni |
| 2 | `AppEngine.kt:102` | `reviewXpWeekKey` field-initsializatoru | ❌ yeni |
| 3 | `AppEngine.kt:209` | `activeEvents` — `LocalDate.now().toEpochDay()` | ✅ |
| 4 | `AppEngine.kt:296` | `dailyLogin` default parametr | ✅ |
| 5 | `AppEngine.kt:370` | `finishOnboarding` default parametr | ✅ |
| 6 | `AppEngine.kt:493` | `addReview` default parametr | ❌ yeni |
| 7 | `AppEngine.kt:537` | `swipe` — match XP günlük limiti | ✅ |
| 8 | `AppEngine.kt:1019,1021` | `nextSaturdayIso` — 2 ayrı call-site (loop şərtində təkrar!) | ✅ (diapazon :1016-1028) |
| 9 | `Models.kt:218` | `isAdult` default parametr | ❌ yeni |
| 10 | `XpEconomy.kt:58,63` | `grant/spend` `at` default-ları | ❌ yeni |
| 11 | `XpEconomy.kt:85,88` | `XpCardCode.isValid`, `secondsLeft` | ❌ yeni |
| 12 | `XpEconomy.kt:95` | `XpCardCode.fresh` default | ❌ yeni |
| 13 | `Domain.kt:36,51` | `UserCoupon.isExpired`, `daysLeft` | ✅ |
| 14 | `DateLabels.kt:14,25,36` | `nextDateLabels/dayLabel/dateLabelFor` default-ları | ✅ |
| 15 | `FakeData.kt:33,523,529,535` | `today` seed-i + bildiriş vaxtları | ❌ yeni (qəsdən tarix-nisbi demo, amma test determinizmini məhdudlaşdırır) |
| 16 | UI: `MapScreen.kt:171`, `VenueDetailScreen.kt:97` | „indi açıqdır" üçün saat | ❌ yeni |
| 17 | UI: `CreateFinishSheets.kt:96,235` | tarix seçici default-ları | ❌ yeni |

**Nəticə:** prompt-un siyahısı düz, amma tam deyil idi — əslində **12 əlavə**
bypass nöqtəsi var (o cümlədən model qatında və UI-da). Rust nüvəsində tək
`Clock` trait + JNI tərəfindən tək təchizat (M2); testlərdə frozen clock məcburi.

### A3. İstinad bütövlüyü yoxlanılmır; v3 üçün XP hesabı yoxdur — **TƏSDİQ**

- `partners` = v1…v6: `FakeData.kt:326-376` ✅
- `initialVenueXp()` yalnız **v5, v2, v6, v1, v4** qaytarır: `FakeData.kt:542-581` — **v3 (Techno Hub) YOXDUR** ✅
- `o5 → v3`: `FakeData.kt:510-514` (`autoByLevel=true, xpPrice=0` → hazırda latent) ✅
- `buyCoupon` **5 səssiz-null yolu**: `AppEngine.kt:645` (offer yoxdur), `:646`
  (autoByLevel), `:647` (səviyyə bağlı), **`:648` (venue hesabı yoxdur — prompt-un
  dəqiq iddiası təsdiqləndi)**, `:649` (balans az) — heç birində istifadəçiyə izahat yoxdur.
- Başlanğıc data invariantlarının (partners ⊆ venueXp, offers.venueId ⊆ partners,
  events.venueId ⊆ partners) yoxlanılması **repoda heç bir yerdə yoxdur**.
  İndiki datada: offers/events istinadları düzgündür; yeganə pozuntu
  `partners ∖ venueXp = {v3}`.
- **Düzəliş (M0):** startup invariant checki (pozuntu → səssiz deyil, dərhal xəta);
  `buyCoupon` uğursuzluğu səbəblə görünən `Result` kimi. M1-də FK, M2-də typed ID-lər.

### A4. `xpProgress` sərhəd ziddiyyəti — **TƏSDİQ**

- `Models.kt:203-215`: xp dəqiq `rankMax[i]`-ə bərabər olanda `pct = 100` **VƏ**
  `toNext = max+1-xp = 1`. Misal: `xp=99` → `pct=100, toNext=1` („100% tamamlandıb,
  amma 1 XP qalıb"). Ziddiyyət **bütün 5 sərhəddə** baş verir (99/299/699/1499/2999).
- Terminal hal düzgündür: `xp≥3000` → `XpProgress(null, 0, 100)`.
- **Düzəliş (M0):** invariant `pct==100 ⟺ toNext==0`; Rust-da proptest ilə
  0…3000 aralığının tam yoxlanışı.

### B1. Check-in 200 m yaxınlıq — **TƏSDİQ**

- `Models.kt:172`: `CHECK_IN_MAX_DISTANCE_M = 200` tərif edilib.
- `grep -rn CHECK_IN_MAX_DISTANCE_M app/src/` → **yalnız tərif; istifadə = 0**.
- `AppEngine.checkInEvent` (`:436-455`) heç bir məsafə yoxlamır.
- Miqrasiya: haversine (`geo` crate) client + server tərəfli doğrulama (M1/M2).

### B2. Ardıcıl seriya sıfırlanmır — **TƏSDİQ**

- `AppEngine.dailyLogin` `:296-307`: gap yoxlaması yoxdur — `loginStreakV` yalnız
  `==7` → bonus+reset, əks halda `++`. Həftələr sonra qayıdan istifadəçi seriyasını
  itirmir. Spec: `product-spec.md:140` („ardıcıl seriya").
- `dailyLoginDone` (`:104`, `:297`) proses həyat dövrünə bağlı, heç yerdə sıfırlanmır.
- Bonus məntiqi özü düzgündür (7-ci girişdə +30, sonra reset) — bug yalnız gap-dadır.
- Miqrasiya: server tərəfli — `bu gün − sonGiriş > 1 gün → seriya = 1` (M1).

### B3. Dəvət XP-i şərtsiz — **TƏSDİQ**

- `AppEngine.inviteFriend` `:273-284`: düyməyə toxunanda dərhal +100.
- Spec `product-spec.md:147`: „dəvətli qeydiyyatı bitirib ilk tədbirə gələndə".
- Miqrasiya: dəvət linki → qeydiyyat → ilk check-in zənciri (server state machine, M1).

### B4. Rəy moderasiyasız — **TƏSDİQ**

- `AppEngine.addReview` `:493-504`: dərhal +15. Spec `product-spec.md:146`:
  „moderasiyadan keçən … həftədə max 3". Həftə limiti özü işləyir (A2 audit
  qeydi `:51`), moderasiya yoxdur. Miqrasiya: `pending` status → təsdiqdən sonra XP (M1).

### B5. XP kartı nonce-u crypto-random deyil — **TƏSDİQ**

- `XpCardCode.fresh` `XpEconomy.kt:91-103`: `(now.nano/1000) xor nonceSeq++` mod
  1e6 — deterministik „mock random" (sənəd :94 bunu açıq etiraf edir).
- `markXpCardUsed` `AppEngine.kt:734-737`: client state-ini dəyişir.
- 60 s TTL düzgündür: `XP_CARD_TTL_SECONDS=60` (`Models.kt:177`), `expiresAt` `:101`.
- Miqrasiya: CSPRNG (`rand`) + nonce server tərəfindən issue/istifadə qeydi (M1/M2);
  interfeys artıq „server nonce" qəbul edəcək şəkildə hazırlanır (M0).

### B6. Xərcləmə atomikliyi — **TƏSDİQ**

- `AppEngine.spendVenueXp` `:703-723` (komment :703: „double-spend qadağan (mock-da
  tək axın)"); `VenueXpAccount.spend` `XpEconomy.kt:63-66` — heç bir yoxlama YOX,
  mənfi balans mümkün. `grant`-ın `balance` üzərində `private set`-i yaxşıdır (`:53`),
  amma `spend` üçün müdafiə yoxdur.
- Miqrasiya: Rust `Result` API + DB transaksiyası/optimistic-lock; mənfi balans
  tip səviyyəsində iflasız (M1/M2).

### B7. Qeyri-müəyyən qaydalar + ölü kod — **TƏSDİQ + yeni nümunə**

- `MapVisibility` (`Models.kt:12-16`) yalnız öz pin göstərməsinə təsir edir —
  „kim MƏNİ görür" server sorğu filtrində zorlanmalıdır (M1, RLS/ST_DWithin sorğusu).
- **Yeni (N7):** `FALLBACK_CODE_DIGITS = 6` (`Models.kt:178`) yalnız testdə istifadə
  olunur (`XpEngineTest.kt:83`); produksiya `padStart(6,'0')` `XpEconomy.kt:100`-də
  **hardcode** — sabit dəyişsə test keçər, davranış dəyişməz. Ya istifadə, ya birləşdir.

### C1. `previewSpend` cap davranışı — **TƏSDİQ; QƏRAR GÖZLƏYİR**

- Cari: cap aşarsa əməliyyat **REDD** (`AppEngine.kt:696-698` → `SpendPreview(false, cap, "cap")`);
  UI: `PartnersScreen.kt:371-372` „…Daha az XP yaz".
- Alternativ: XP **clamp** (cap qədər xərclənir, qalan balansda qalır). Hər ikisi
  spec L.4 („maksimum endirim = hesabın X%-i") ilə uzlaşır.
- **İstifadəçidən gözlənilən qərar:** REDD (cari) yoxsa CLAMP. Qərar D-qeydi ilə
  spec-ə yazılır, hər iki tərəf (UI mətni + Rust testləri) uyğunlaşdırılır.

### D1. „Yalançı bug" — CI trigger — **TƏSDİQ (bug yoxdur)**

- `.github/workflows/native.yml:7`: `branches: [main]` — düz mətn, bayt səviyyəsində
  problemsiz. Dərs qüvvədədir: hər iddia iki müstəqil üsulla yoxlanılır.
- Əlavə fakt: workflow-da `paths:` filtri var (`native.yml:8-10`) — yalnız
  `android-app/**` və özü dəyişəndə CI işə düşür. Sənəd dəyişiklikləri (bu
  hesabat daxil) pipeline işə salmır — bu, doc-only push-lar üçün sərfəlidir.

---

## 2. Yeni tapıntılar (bu araşdırmanın özününkü)

### Kod

- **N1 — ID kolliziya sinifi (server fazası üçün kritik).**
  - `createEvent` `AppEngine.kt:467`: `"c${size+1}-${ms%100000}"` — `nano/1e6` =
    saniyənin millisidir (0–999) → suffix cəmi **1000 fərqli dəyər**; `size+1` ancaq
    sessiya daxilində unikal. İki müştəri eyni sayda tədbirlə eyni millisiqani seçsə → çakışma.
  - `createReservation` `:573`: `"r${size+1}"` — suffix **heç yoxdur**: hər
    istifadəçinin ilk rezervi `r1` olur → cross-device **zəmanətli** kolliziya.
  - Rezerv QR `:578`: `"QSH-r{n}-{ms%10000}"` — yenə 1000 dəyər.
  - `notify` `:796`: `"n${size+1}-${ms}"` **+ siyahı cap 50 (`:800`)** → cap-dan
    sonra `size+1` sabit 51 qalır → eyni ms-də təkrar ID.
  - Hazırda zərərsiz (yaddaş siyahıları, ID map açarı deyil). Miqrasiyada bütün
    yeni ID-lər ULID/newtype (`VenueId`, `EventId`…) — `"${size+1}"` üslubu tam aradan qalxır.

- **N2 — `buyCoupon`-un 5 səssiz-null yolu** (A3-ün dəqiqləşdirilməsi, yuxarıda).
  UI heç bir yolda izahat göstərmir.

- **N3 — Review həftə-limiti „yanığı".** `addReview` `:499-501`:
  `reviewXpThisWeek++` **award() idempotensiya yoxlamasından əvvəl** icra olunur —
  eyni tədbirə təkrar rəy yazanda `award("review-$eventId")` false qaytarır, amma
  həftə slotu yanıb. Əlavə: `eventId`-nin mövcudluğu/keçmişliyi/iştirak faktı yoxlanmır.

- **N4 — Locale-həssas formatlaşdırma.** `String.format("%.2f")`:
  `AppEngine.kt:718`, `PartnersScreen.kt:372,394` — system locale-dən asılı onluq
  ayırıcı (az-AZ → vergül). Giriş tərəfi əksinə vergülü nöqtəyə çevirir
  (`PartnersScreen.kt:365`). AZ kontekstində vergül „düz"dür, amma vahid konvensiya
  yoxdur — vahid formatlaşdırıcı (tək nöqtədən) tələb olunur.

- **N5 — Mutable model sahələri (dəqiq inventar).** `UserCoupon.used` (`Domain.kt:34`),
  `Reservation.status` (`:87`), `AppNotification.read` (`:123`), `XpCardCode.used`
  (`XpEconomy.kt:83`). Paralel axın təhlükəsi hazırda yoxdur (tək axın), amma
  data-class mutasiya paterni Rust-da `&mut`/interior mutability dizaynına
  uyğunlaşdırılmalıdır.

- **N6 — Rate limit yoxdur.** Spec TAB 3 („Mesaj göndərmə rate limit-i və spam
  filtri var") — `sendEventMessage/sendFriendMessage` (`:857-869`) limitsiz.
  Backend fazası qaydası (M1).

- **N7 — Ölü sabit (B7 daxilinde).** `FALLBACK_CODE_DIGITS` produksiyada istifadəsiz.

- **N8 — „Qrupla rezerv" yoxdur.** Spec E („rezervi tədbirə/dostlarına paylaşa
  bilər, onlar Qoşul basır") — kodda paylaşma axını yoxdur. M1+ dizayn boşluğu.

- **N9 — Moderator/admin alətləri yoxdur.** Spec A-4 + I: report-lar avtomatik
  3+→restrict (`:840-853`) ilə məhdudlaşır; admin növbəsi, şəkil moderasiyası
  (şəkil yükləmə özü yoxdur) backend fazasıdır.

- **N10 — FakeData tarix-nisbi nondeterminizm.** `FakeData.kt:33`
  (`today = LocalDate.now()`) demo datanı həmişə „təzə" saxlayır (qəsdən, yaxşı
  dizayn), amma buna bağlanan testlər tam frozen deyil — gecə yarısı keçidi
  (e2 `epochDay = today`, `:127`) flake riski daşıyır. A2 həlli bunu da əhatə etməlidir.

- **N11 — epochDay Int (təkzib — praktik risk yoxdur).** `QoshulEvent.epochDay: Int`
  (`Models.kt:25`), `toEpochDay().toInt()` (`AppEngine.kt:209`): Int epoch-günü
  ±5.8 milyon ilə qədər daşıyır → daşma praktik mümkün deyil. Newtype yenə də
  tövsiyə olunur (tip təhlükəsizliyi üçün), amma bu „bug" deyil.

### Sənədləşmə/idarəetmə

- **N12 — D-reyestri kolliziyaları (iki səviyyədə).**
  (a) **main-in öz daxilində:** „Qərarlar (mühəndislik seçimi)" cədvəli
  (decisions.md-in ikinci cədvəli) D-008…D-014 nömrələrini **yenidən işləyib** —
  eyni nömrələr birinci cədvəldəki (D-001…D-065) qeydlərlə fərli məzmun daşıyır
  (köhnə backend/VM planlaşdırma era-sı: NestJS/BullMQ/Oracle — D-065 ilə
  arxa plan silindi, amma cədvəl qalıb).
  (b) **branch-lər arasında:** `D-052` main-də „Crash və inset təmiri",
  server-only-də „server-only branch həyata keçib"; server-only reyestrində
  D-053…D-064 yoxdur (12 qeyd əksik).
  Miqrasiya başlamazdan əvvəl siyasət lazımdır: **main-in birinci cədvəli =
  kanonik reyestr; köhnə ikinci cədvəl tarixi qalıq kimi işarələnir;
  server-only sync edilir** (D-071). Yeni qərarlar D-066-dan davam edir.

- **N13 — README/CI göstəriciləri aktualdır.** README „121 JVM testi" / D-001…D-065 /
  v0.15.0 state — hamısı real fayl vəziyyəti ilə üst-üstə düşür (waller yoxdur,
  yalançı iddia yoxdur). `server-only` branch-də `okhttp 5.5.1` əlavəsi
  `libs.versions.toml`-da şərh ilə sənədlənib.

---

## 3. Spec-uyğunluq matrisi (ROL A–L → kod → miqrasiya təbəqəsi)

| ROL | Qayda | Kod statusu | Yer | Miqrasiya təbəqəsi |
|---|---|---|---|---|
| A | Qonaq/İstifadəçi rolları | ✅ | AuthScreen qapısı | — (Kotlin qalır) |
| A | Partnyor (biznes rejimi) | ✅ mobil-daxili | PartnersScreen | M1: Supabase rol/RLS |
| A | Moderator/Admin | ❌ (yalnız avto-restrict) | AppEngine:840-853 | M1 (SQL) + M4 (panel API) |
| B | Onboarding 6 addım, +50 XP | ✅ (18+, 3-8 maraq, bio 150, 1-6 şəkil) | OnboardingScreen, AppEngine:248-376 | — ; real auth M1 |
| B | Telefon təsdiqi (+40) | ✅ mock | AppEngine:287-293 | M1 (Supabase Auth) |
| C | 4 tab + daxili tablar, pinned axtarış | ✅ (D-028/029/034) | AppShell, FeedScreen | — |
| D | Swipe/match +10, gündə max 3 | ✅ | AppEngine:516-548 | Rust nüvə (M2), limit serverdə |
| D | ST_DWithin + GiST sıralama | ❌ (mock sadə filtr) | — | M1: SQL+PostGIS |
| D | „Maraqlıyam" gündəlik limiti | ❓ spec özü [TƏSDİQ GÖZLƏYİR] | product-spec.md:109 | istifadəçi qərarı |
| E | Rezerv axını 6 status | ✅ | AppEngine:562-635, Domain.kt:57-97 | status keçidləri Rust (M2) |
| E | QR təkistifadə + partnyor skanı | ✅ | AppEngine:594-604 | server doğrulama M1 |
| E | 24s/2s xatırlatma | ✅ mock tetikleyici | AppEngine:814-821 | M4: idempotent job |
| E | Qrupla rezerv paylaşımı | ❌ (N8) | — | M1+ dizayn |
| F | 12 XP hadisəsi + idempotensiya | ✅ hamısı | AppEngine:227-504 | Rust `award(key)` (M2) |
| F | 200 m check-in yoxlaması | ❌ (B1) | Models.kt:172 (ölü) | Rust `geo` + server (M1/M2) |
| F | Səviyyələr/nişanlar | ✅ | Models.kt:189-241 | Rust (M2) |
| G | Səviyyə-avto + XP ilə kupon | ✅ (L.6 venue balansı) | AppEngine:640-675 | Rust + DB transaksiya (M1/M2) |
| G | 14 gün, təkistifadə, XP qayıtmır | ✅ | Models.kt:162, AppEngine:600,663-668 | — |
| H | 11 bildiriş tipi + per-type toggle | ✅ | Domain.kt:101-134 | — |
| H | Sükut saatı 23-08 | ✅ | Domain.kt:127, AppEngine:792 | Rust `isQuietHours` (M2/M4) |
| H | Real-time + FCM | ❌ | — | M1 Realtime; M4 FCM |
| I | Report 3+ → restrict, blok | ✅ | AppEngine:826-853 | server zorlaması M1 |
| I | Şəkil moderasiyası | ❌ (yükləmə yoxdur) | — | M1 Storage + M4 job |
| I | Dəqiq koordinat göstərilmir | ✅ client („~2 km") | Models.kt:81-83 | RLS/sorğu filtr M1 |
| J | Backend cədvəlləri | — (backend yoxdur, D-065) | spec J siyahısı | M1: sxem+FK+RLS |
| K | Son istifadəçi ssenarisi | ✅ E2E demo | DemoAccount, DemoAccountTest | — |
| L.1 | Peşəkarlıq XP, xərclənməz | ✅ | XpRules, proWeight | Rust (M2) |
| L.2 | Obyektə-məxsus, TTL yoxdur | ✅ | VenueXpAccount | Rust + DB (M1/M2) |
| L.3 | Statik QR + taq 50/25, xp_config | ✅ | AppEngine:744-785, VenueXpConfig | server doğrulama M1 |
| L.4 | Dinamik QR 60s, 6 addım, cap | ✅ (cap = REDD, C1 qərarı) | XpEconomy:77-105, AppEngine:680-723 | Rust + server nonce (M1/M2) |
| L.5 | 6 rəqəm fallback, NFC yoxdur | ✅ | XpEconomy.kt:100 | — |
| L.6 | Kupon venue balansından | ✅ | AppEngine:643-671 | — |
| L.7 | Görünüş 3 seçim | ✅ client-only | MapVisibility | server filtr M1 (B7) |

---

## 4. Dil inventarı — yoxlama + miqrasiya planı

| Təbəqə | Fayl sayı (main) | Təsdiq | Tale |
|---|---|---|---|
| Kotlin (core+features+test) | 55 | ✅ | QALIR (AppEngine orchestrator qalır, qaydalar M2-də Rust-a köçür) |
| Java — `io/nayuki/qrcodegen` | 4 | ✅ (yalnız `core/ui/QrCode.kt` istifadə edir) | M3: `qrcodegen` crate (Rust) |
| Python — `scripts/subset_fonts.py` | 1 (157 sətir, fontTools) | ✅ | M3: Rust CLI (`write-fonts`+`skrifa`), eyni audit/ sübut məntiqi |
| ZXing 3.5.4 | test-only | ✅ (main src-də yalnız komment: QrCode.kt:29,131) | M3: `quircs` (render→dekod→payload sübutu `cargo test`-də) |
| server-only əlavəsi | okhttp 5.5.1 | ✅ | M1-də supabase-kt ilə əvəz/uzlaşma qiymətləndirilir |
| XML/YAML/MD/TTF/WebP/kts | konvensiya | ✅ | §3.3 sərhədlərində qalır |

**Hədəf (M3 sonunda):** birinci tərəf kodu 100% Kotlin+Rust — `git grep io.nayuki` boş,
`.py` yoxdur.

---

## 5. Versiya qərarları (D-066…D-071-in əsası; hamısı 2026-11-07 tarixli rəsmi sorğu)

### Rust crate-ləri (crates.io API)

| Crate | Stable | Son yeniləmə | Məqsəd |
|---|---|---|---|
| `qrcodegen` | **1.8.0** | 2022-04-17 | Nayuki-nin rəsmi Rust portu (vendored Java v1.8.0 ilə eyni buraxılış — paritet üçün ideal) |
| `quircs` | 0.10.3 | 2025-10-20 | QR dekod (skan-sübut testləri) |
| `geo` | 0.33.1 | 2026-04-20 | haversine (B1, 200 m) |
| `axum` | 0.8.9 | 2026-04-14 | M4 mikroservis |
| `reqwest` | 0.13.5 | 2026-09-08 | HTTP client (FCM, LLM proxy) |
| `rand` | 0.10.3 | 2026-09-20 | CSPRNG nonce (B5) |
| `proptest` | 1.12.0 | 2026-10-10 | property-based testlər (A4, idempotensiya, double-spend) |
| `write-fonts` | 0.55.0 | 2026-10-21 | M3 font subsetting |
| `skrifa` | 0.49.0 | 2026-10-21 | qlif oxunuşu/audit (AZ qlif + ₼ sübutu) |
| `jni` | 0.22.4 | 2026-03-16 | Android JNI körpüsü |

Mənbə: `https://crates.io/api/v1/crates/<ad>` (User-Agent ilə; 2026-11-07).

### Gradle/Kotlin tərəfi

| Asılılıq | Stable | Mənbə |
|---|---|---|
| `supabase-kt` (umbrella) | **3.9.0** | repo1.maven.org …/supabase-kt/maven-metadata.xml |
| `gotrue-kt` (auth) | **2.6.1** (head 2.7.0-beta-1 — beta götürülür) | eyni |
| `postgrest-kt` / `realtime-kt` / `storage-kt` / `functions-kt` | 3.9.0 | eyni |

- **supabase-kt statusu TƏSDİQ edildi:** rəsmi repo README (2026-11-03 versiyası):
  „We are working towards making supabase-kt official … migrate the namespace and
  transfer" — prompt-un iddiası düzdür. M1-də pin + yenidən yoxlama məcburidir.

### Xarici faktlar

- **FCM:** rəsmi Admin SDK-lar yalnız **Node.js / Java / Python / Go**
  (firebase.google.com/docs/libraries — „Official Admin SDKs" cədvəli). Rust üçün
  yol: FCM **HTTP v1** REST + OAuth2 service-account tokeni (`reqwest` ilə).
  Cəmiyyət SDK-sı (0xrustdev/Firebase-Admin-SDK) var, amma yetkin deyil — istifadə edilmir.
- **PostGIS:** `ST_DWithin` spatial indeksdən istifadə edir, `geography` tipində
  məsafə metrlə ilə; `ST_Distance`/`ST_Buffer` əvəzinə tövsiyə olunur
  (postgis.net/documentation/tips/st-dwithin).

---

## 6. Risklər və açıq suallar (istifadəçiyə yönəldilmiş)

**2026-11-07 yeniləmə — 1/3/4/5/6/7-CEVABLANDI:**
1. ✅ **C1 → REDD + balans sərhədi (D-072)** — istifadəçi: „yalnız hesabda var olan
   qədəri istifadə oluna bilər"; mənfi balans qadağan (B6), səbəb typed göstərilir.
2. ⬜ JVM testləri × JNI (arxitektura riski, M2) — dəyişməyib.
3. ✅ **supabase-kt 3.9.0 pin (D-078)** — namespace transferi BAŞ verib: paket
   `io.github.jan.supabase`; JVM artifaktları `supabase-kt-jvm`/`postgrest-kt-jvm`
   mövcuddur (repo1 200). Qərar: pin saxlanılır, M2 qapısında yenidən yoxlanılır.
4. ⬜ Font subsetting Rust stack-i (M3) — dəyişməyib.
5. ✅ **Streak semantikası (B2) → dondurulma modeli (D-073)** — 1-2 gün dondurulur,
   3+ gün sıfırlanır.
6. ✅ **„Maraqlıyam" limiti → LIMITSİZ (D-074)** — həm onboarding maraq seçimində
   maksimum-8 həddi ləğv olundu, həm match gündəlik hərəkət limiti yoxdur
   (XP mükafatı gündə max 3 qalır).
7. ✅ **Partnyor paneli → MOBİL-DAHİLİ (D-077 kontekstində)** — PartnersScreen
   biznes rejimi qalır (veb panel YOX); M4-də moderator/admin panel API REST
   şəklində gəlir (N9).
8. ✅ **D-reyestri (N12) → D-071 siyasəti tətbiq olundu** — bu sessiyada yeni
   qərarlar D-076+ kimi ardıcıl yazıldı; köhnə ikinci cədvəl toxunulmaz qalır.

Orijinal siyahı (arxiv):

1. **C1 qərarı (bloklayıcı, M0-a təsir edir):** cap aşarsa — REDD (cari:
   „Daha az XP yaz") yoxsa CLAMP (cap qədər xərclə, qalan saxla)?
2. **JVM testləri × JNI (arxitektura riski, M2):** 121 test `testDebugUnitTest`
   daxilində işləyir. Rust nüvəsinə keçəndə iki yol var: (a) `cdylib` .so-nu
   JVM testlərində də yükləmək (CI-da Rust toolchain addımı), (b) Rust qaydalarını
   yalnız serverdə saxlamaq (amma bu §3 „Android-ə JNI" tələbinə ziddir).
   Tövsiyə: (a) — paritet bir qaynaqdan qalır. M2 planında dəqiqləşəcək.
3. **supabase-kt rəsmiləşməsi:** namespace transferi dövründə breaking dəyişiklik
   riski — M1-də versiya pinlənib, hər milestone-da yenidən yoxlanılacaq.
4. **Font subsetting Rust stack-i:** pyftsubset-ın hazır ekvivalenti yoxdur —
   audit + yazım məntiyi `write-fonts`/`skrifa` ilə sıfırdan yazılacaq
   (AZ qlif + ₼ U+20BC proqramlı sübutu saxlanılır). İş miqyası: ~orta.
5. **Streak semantikası (B2):** „ardıcıl" = gap>1 gün → reset qərarı spec-ə
   yazılmalıdır (təklif: bəli — beynəlxalq standart davranış).
6. **„Maraqlıyam" gündəlik limiti:** spec-də [TƏSDİQ GÖZLƏYİR] — dəyər lazımdır
   (məs. gündə 50?) yoxsa limitsiz?
7. **Partnyor paneli:** veb yoxsa mobil-daxili (spec açıq sualı „a") — M1 sxeminə
   təsir edir.
8. **D-reyestri (N12):** main-kanonik siyasəti təsdiq — D-071 kimi təklif edilib.

---

## 7. Nəticə və M0 qapısı

**M0 İCRA OLUNDU (2026-11-07, D-075):** §7-dəki siyahının hamısı həyata keçdi —
A1 (typed SpendError + „hesap" tipo-orfoqrafiyası aradan qalxdı), A2 (vahid clock —
tam siyahı, 17 nöqtə), A3 (invariant check + v3 hesabı + görünən buyCoupon),
A4 (pct/toNext invariantı), B5 (injeksiya interfeysi), B6 (mənfi balans qadağası),
N3 (review slot), N7 (ölü sabit istifadə olunur) + istifadəçi qərarları D-072/D-073/D-074.
Testlər: 121 → **140** (103-ü lokal JVM-də icra olundu — 86 XpEngine+Rules, 17 WhatIf;
qalan Robolectric/UI testləri CI qapısında). Lokal icra üçün kotlinc 2.1.0 +
kotlinx-coroutines 1.10.2 istifadə olundu (sandbox-da Android SDK yoxdur).

**M1 İCRA OLUNDU (2026-11-07, D-076..D-079):** sxem (0001: 29 cədvəl + RLS +
PostGIS + ULID), server qaydaları (0002: 24 SECURITY DEFINER funksiya — D-073/D-074/
D-072/B1/B3/B4/N6/N8/N9 paritetləri), seed (0003), tətbiq inteqrasiyası
(`core/backend/` — ikili rejim, PLACEHOLDER konfiq → avtomatik MOCK),
SQL struktur lint-i (CI-da). Testlər: 140 → **165** (lokal JVM: 100/100).
**Növbəti qapı: M2 (Rust nüvə — JNI, Clock trait, proptest paritetləri).**

**M2 İCRA OLUNDU (2026-11-07, D-080):** `rust-core/` (qoshul-core: types/clock/rules/
economy/geo; qoshul-jni: cdylib, numerik səth) — A4 proptest 0..3100 TAM, D-073 matris,
B6 double-spend, haversine simmetriyası; `RustParityTest` 11/11 host .so ilə; CI-də
cargo-ndk jniLibs (APK-da real Rust) + JVM testləri host .so məcburiyyəti (risk 2,
variant (a) təsdiqləndi). Engine Kotlin fallback-də qalır — paritet sübut olundu,
keçid M3-də qiymətləndirilir.

---

### Arxiv: M0 qapısı (2026-11-07, istifadəçi təsdiqindən əvvəl)

- §2.2-nin **hamısı** (A1–A4, B1–B7, C1, D1) fayl:sətir dəlilləri ilə yoxlanıldı:
  12 TƏSDİQ, 1 TƏSDİQ-qərar-gözləyir (C1), 1 TƏSDİQ-bug-yoxdur (D1), A1/A3 dəqiqləşdirildi.
- Əlavə **N1–N13** tapıntısı qeydə alındı (2-si idarəetmə, 9-u kod, 2-si təsdiq
  statuslu qeyd — N11 təkzib).
- Versiyalar rəsmi mənbələrdən təsdiqləndi → D-066…D-071 yazıldı.
- **M0 (bug düzəlişləri) hazırdır — icra üçün istifadəçi təsdiqi gözlənilir (QAYDA 0):**
  A1 (enum xəta tipi), A2 (vahid clock — tam siyahı ilə), A3 (invariant check +
  buyCoupon görünən uğursuzluq), A4 (pct/toNext invariantı), B5-in interfeys
  hazırlığı, N3 (review slot yanığı), N7 (ölü sabit). Hər fix 6-addım protokolu ilə
  (test əvvəl QIRMIZI → YAŞIL, D-qeydi, 121 test pariteti).

*Araşdırma qaydası: bu hesabatdakı hər iddia yuxarıdakı fayl:sətir referansları
ilə yerində yoxlanıla bilər; yoxlanmamış heç nə „təsdiqlənib" statusu daşımır.*
