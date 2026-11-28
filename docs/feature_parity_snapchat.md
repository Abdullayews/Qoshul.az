# Qoshul.az ↔ Snapchat Paritet Analizi (P3, D-185)

> Tarix: 2026-11-28 (Loop 4). Məqsəd: Snapchat-in rəsmi mənbələrindən
> (help.snapchat.com) əsas sosial mexanikaları müəyyənləşdirmək və
> Qoshul.az kod bazası ilə kateqoriyalar üzrə müqayisə etmək.
> **Bu, kopyalama planı deyil** — Qoshul.az maraq-görüş platformasıdır,
> Snapchat multimedia mesajlaşma tətbiqi; paritet yalnız istifadəçi
> gözləntilərini formalaşdıran mexanikalar (görünmə nəzarəti, streak,
> ephemeral mesajlar) üçün ölçü vahididir.
>
> Metod: hər Snapchat faktı yalnız rəsmi help.snapchat.com məqaləsindən;
> hər Qoshul.az faktı kod faylı+sətir referansı ilə. Qiymətləndirmə:
> **VAR** (tam uyğun) / **HİSSƏN** (mexanika var, dərəcə fərqlidir) /
> **YOX** (mövjud deyil) / **FƏRQLİ YANAŞMA** (bilərəkdən başqa model).

## 1. Xəritə və mövqe məxfiliyi

### Snapchat (rəsmi)
- Paylaşım **default OLARAQ BAĞLI**dır; yalnız açıq şəkildə seçilən
  adamlar mövqeni görür [mənbə: How do I share my location on Snap Map —
  help.snapchat.com/hc/en-us/articles/7012309470740]
- **4 rejim**: My Friends / My Friends, Except… / Only These Friends… /
  Ghost Mode (hamıdan gizli) [eyni mənbə]
- Ghost Mode taymerli: 3 saat / 24 saat / „söndürülənədək“; aktiv olanda
  dostlar Bitmoji əvəzinə mavi ghost nişanı görür [mənbə:
  help.snapchat.com/hc/en-us/articles/7012322854932]
- Diqqət: Ghost Mode-da belə Snap Map-ə göndərilən snaplar xəritədə
  görünə bilər [eyni mənbə]

### Qoshul.az
- `MapVisibility` 3 rejim: **EVERYONE / FRIENDS / OFF** — məxfilik
  seçimi serverə yazılır (RLS tətbiq edir): `AppEngine.kt:135,845-848`
  (H.1/D-170), məxfilik dialoqu `SettingsScreen.kt` (D-027)
- Mövqe axını: davamlı GPS (≤50m dəqiqlik, ≥15m hərəkət, 4s throttle,
  mock rədd): `QoshulLocation.kt:173`, `MapScreen.kt:196-207` (G.1)
- İzləmə rejimi: „Mənim yerimə get“ → zoom 16 + jest izləməni dayandırır
  (D-184): `MaplibreCanvas.kt` `CameraMoveReason.Gesture`

### Qiymətləndirmə: **HİSSƏN**
EVERYONE/FRIENDS/OFF Snapchat-in Ghost Mode/My Friends ikilisini əhatə
edir. Fərqlər: (1) Snapchat-də mövqe paylaşımı **default bağlıdır**,
bizdə default EVERYONE — onboarding-də aktiv seçim tələb etmək daha
uyğundur (Snapchat modelinə yaxınlaşma); (2) „Only These Friends…“
seçmə-list rejimi bizdə yoxdur; (3) Ghost Mode taymeri (3h/24h) yoxdur —
OFF əl ilə idarə olunur. **Təklif (P3.1)**: onboarding icazə addımında
mövqe paylaşım rejimi seçimi daxil etmək.

## 2. Giriş seriyası (Streaks)

### Snapchat (rəsmi)
- Streak = iki tərəf **hər gün** (24 saat içində) bir-birinə Snap
  göndərir; chat mesajı kifayət ETMİR, şəkil/video olmalıdır [mənbə:
  help.snapchat.com/hc/en-us/articles/7012394193684]
- Göstəricilər: alov emoji 🔥 + gün sayı; bitməkdə olan streak üçün
  hourglass ⏳ xəbərdarlığı; müəyyən müddətə bərpa mümkündür [eyni mənbə]

### Qoshul.az
- `loginStreak` **tətbiqə girişə** bağlıdır (çat mübadiləsinə yox):
  ardıcıl gün +1, 1-2 gün boşluq saxlanır, 3+ gün sıfırlanır (B2):
  `AppEngine.kt:154-155,497-511`
- 7 günlük seriya bonusu: `award("streak-7-…", XpRules.STREAK_BONUS_XP)`
  `AppEngine.kt:510-511`
- UI göstəricisi: streak state-də var (`AppEngine.kt:279`) amma
  **heç bir ekranda göstərilmir** (grep: features/ altında loginStreak
  istifadəsi 0)

### Qiymətləndirmə: **FƏRQLİ YANAŞMA + UI boşluğu**
Bizim streak fərdi girişə bağlıdır (Snapchat-inkişafda „self-streak“
/ Snapstreak-ə bənzər mexanika). İki boşluq var: (1) streak **görünmür
deyil** — ProfileScreen və ya TaskCenter-də alov + gün sayı göstərilməli
(**Təklif P3.2, kiçik iş**); (2) hourglass xəbərdarlığı yoxdur —
„bu gün girsən streak qurtarır“ mesajı bildirişlərlə verilə bilər.

## 3. Çat: ephemeral mesajlar və silmə

### Snapchat (rəsmi)
- „Delete is our default“: baxılandan sonra mesajlar avtomatik silinir;
  açılmamış snaplar 31 gün sonra; çat mesajları hamı baxandan sonra
  24 saat VƏ YA göndərilmədən 31 gün — hansı tez gəlsə [mənbə:
  help.snapchat.com/hc/en-us/articles/7012334940948]
- Çat başına 4 saxlama rejimi: After Viewing / 24 Hours / 7 Days /
  Never — hər iştirakçı dəyişə bilər və dəyişiklik hamıya görünür
  [eyni mənbə]
- Silinən mesaj dost cihazından „best attempt“ ilə çıxarılır; saxlanan
  (saved) mesajlar çatda qalır [eyni mənbə]

### Qoshul.az
- Çat: mətn mesajları, „yazır…“ göstəricisi (Realtime, 6s TTL):
  `ChatsScreen.kt:343-347,386-390` (D-175), `TypingIndicator.kt:26-29`
- Söhbət silmə + matç ləğvi: `ChatsScreen.kt:163-169` (D-151);
  bloklama engine-də `AppEngine.kt` (blockUser/unblockUser)
- Mesajlarda **avtomatik silinmə / TTL YOXDUR** — mesajlar sabit qalır
  (server: Supabase messages cədvəli)
- Şəkil göndərmə çatda YOXDUR (yalnız profil/tədbir şəkilləri statik)

### Qiymətləndirmə: **YOX (ephemeral) / VAR (əsas çat)**
Əsas çat mexanikası (yazır göstəricisi, blok, silmə) mövcuddur.
Snapchat-in əsas fərqləndirici xüsusiyyəti — mesajın avtomatik yoxa
çıxması — bizdə yoxdur və bu, **qəsdən başqa modeldir**: Qoshul.az
çatı görüş təşkilatı üçün praktik rabitə vasitəsidir (ünvan, saat,
iştirakçı siyahısı itirməmək lazımdır). **Təklif (P3.3, opsiya)**:
görüş tamamlandıqdan N gün sonra söhbəti arxivləşdirmək (silinmə
deyil — „tamamlandı“ statusu; D-151 matç ləğvi ilə uzlaşır).

## 4. Stories (günün məzmunu)

### Snapchat (rəsmi)
- İki ayrı silsilə: **My Story · Public** (izləyicilər + profil açan
  hər kəs) və **My Story · Friends** (yalnız qarşılıqlı dostlar) +
  dostlar üçün **Custom** seçim [mənbə:
  help.snapchat.com/hc/en-us/articles/7012338216468]
- Story snap-ları adətən 24 saatdan sonra silinir; açılmamış snaplar
  31 gün [mənbə: 7012334940948]

### Qoshul.az
- Stories **YOXDUR** (grep: kod bazasında Stor(y|ies) 0 nəticə —
  PHASE0_REPORT §2 də qeyd edir). Ən yaxın analoq: Feed-də „Aktiv
  tədbirlər“ bölməsi (`FeedScreen.kt:329`) — amma bu, şəxsi deyil,
  tədbir axınıdır.

### Qiymətləndirmə: **YOX**
Şəxsi story mexanikası platformanın məqsədinə uyğun deyil (maraq
görüşləri — gündəlik vlog deyil). Əvəzində tədbir sonrası **„Nə
oldu“ foto-hesabat** mexanikası mövcuddur: təşkilatçı bitirmə axını
(statik QR → şəkil → taq, D-026 L.3): `CreateFinishSheets.kt:578-580`.
Bu, iştirakçıların tədbir nəticəsini görməsinə xidmət edir — Snapchat
Stories-in sosial funksiyasının („nə edirəm“ paylaşımı) platformaya
uyğun tərcüməsi.

## 5. Kamera / Lens / Bitmoji

### Snapchat (rəsmi)
- Kamera tətbiqin İDENTİFİKASİYASIDIR — Snap yaratmaq əsas əməliyyattdır;
  Lenses (AR filtrlər), Bitmoji avatarları, Spotlight-a Snap təqdimatı
  [mənbələr: What is Spotlight — help.snapchat.com/hc/en-us/articles/
  7012271311892; plans — 45845487406740]

### Qoshul.az
- Kamera YOXDUR (tətbiq heç bir kameranı açmır); AR/Lens YOXDUR;
  avatarlar 4 brend şəkli ilə məhduddur (`ProfileEditScreen.kt`,
  D-112/D-177 — şəkil yükləmə backendi deaktiv, PHOTO_UPLOAD=false)

### Qiymətləndirmə: **YOX (qəsdən)**
Kamera mərkəzli model bizim məhsul müddəamıza ziddir. Yalnız bir
praktik boşluq: **istifadəçi profil şəkli yükləyə bilmir** (4 brend
avatardan seçir). Bu, backend aktivləşəndə (D-177) həll olunacaq.

## 6. Məzmun kəşfi (Spotlight / Discover)

### Snapchat (rəsmi)
- Spotlight: ictimai short-video axını, „ən əyləncəli Snap-lar, kim
  yaratdığından asılı olmayaraq“; canlı tədbir/trend etiketləri
  (Live/Trending) [mənbə: 7012271311892]

### Qoshul.az
- Ən yaxın analoq: **Feed + AI seçim skorları** — `fn_recommend_events`
  server RPC (D-179) + kəşf sıralaması `Models.kt:250` („Kəşf peşəkar
  sıralaması“ — səviyyə etiketi çəkisi). Məzmun istifadəçi yaradır
  (tədbirlər), amma video/ictimai yayım YOXDUR.

### Qiymətləndirmə: **FƏRQLİ YANAŞMA**
Kəşf bizdə **tədbir/tapar** (partner endirimləri) üzərində qurulub —
Snapchat-in ictimai məzmun kəşfindən fərqli, məhsul üçün düzgün model.

## 7. Monetizasiya / planlar

### Snapchat (rəsmi)
- Snapchat+ (aylıq/illik), Family Plan (2 üzv + 250GB Memories hər
  üzvə), Platinum (reklamsız Story/Lens + Lens+), Lens+ — 40+ eksklüziv
  xüsusiyyət [mənbə: 45845487406740]

### Qoshul.az
- **Qoşa Coin** XP-əsaslı valyutası: XP → Coin konversiyası, kupon
  alışı, səviyyə kilidi (LEVEL_LOCKED): `AppEngine.kt:1250-1265`,
  XpRules (`Models.kt:260` ranklar: Yeni→Əfsanə); dəvət bonusu
  (+INVITE_XP) `AppEngine.kt:360-374` (D-056)
- Abunəlik YOXDUR — pul ödənişsiz, iştirakla qazanılır

### Qiymətləndirmə: **FƏRQLİ YANAŞMA (güclü)**
Snapchat-i ödənişli abunə ilə, Qoshul.az-ı iştirakla qazanılan valyuta
ilə monetizasiya edir. Bu, platforma məqsədinə (görüşlərə çıxmağı
mükafatlandırmaq) uyğundur və dəyişdirilməli deyil.

## Xülasə cədvəli

| # | Mexanika | Snapchat | Qoshul.az | Qiymət | Boşluq/Təklif |
|---|---|---|---|---|---|
| 1 | Mövqe məxfiliyi | 4 rejim + Ghost taymeri, default BAĞLI | 3 rejim (E/F/OFF), default EVERYONE | HİSSƏN | P3.1: onboarding-də seçim; „Only These Friends“ |
| 2 | Streak | çat mübadiləsi, alov+hourglass | giriş seriyası (engine), UI-da görünmür | FƏRQLİ | P3.2: ProfileScreen-də göstər + hourglass bildirişi |
| 3 | Ephemeral çat | delete-by-default, 4 TTL rejimi | sabit mesajlar (qəsdən) | YOX/YOX-lazımsız | P3.3: görüş sonu arxiv statusu (opsiya) |
| 4 | Stories | Public/Friends/Custom, 24h | YOX; tədbir „Nə oldu“ axını var | YOX | qəsdən yox — tədbir hesabatı uzlaşdır |
| 5 | Kamera/Lens/Bitmoji | mərkəzi funksiya | YOX; 4 brend avatar | YOX (qəsdən) | D-177 şəkil backendi aktivləşəndə profil şəkli |
| 6 | Məzmun kəşfi | Spotlight ictimai video | AI tədbir tövsiyəsi + kəşf sırası | FƏRQLİ | model düzgündür |
| 7 | Monetizasiya | Snapchat+ abunələr | Qoşa Coin (XP-iştirak) | FƏRQLİ (güclü) | dəyişmə |

## Nəticə və Loop 4+ tövsiyələri

Qoshul.az Snapchat-in **multimedia-mərkəzli** modelini kopyalamır və
kopyalamamalıdır; paritet yalnız **məxfilik idarəetməsi** və
**seriya/davamlılıq mexanikalarında** istifadəçi gözləntisi yaradır.
Konkret nəticələr:

1. **P3.1 (orta)** — onboarding PermissionsStep-də xəritə görünüşü
   rejimi seçimi (default FRIENDS təklif etmək Snapchat-in „default
   bağlı“ prinsipinə uyğun olardı); „yalnız seçilmiş dostlar“ üçün
   dost siyahısı UI gərəkdir (əvvəlcə D-176 dostluq sistemi genişlənməlidir).
2. **P3.2 (kiçik, ən yüksək dəyər/əmək nisbəti)** — loginStreak
   göstəricisi ProfileScreen-də (alov emoji + gün) + 1-2 gün
   boşluqda „streak qurtarma“ bildirişi; motor hazırdır (`AppEngine.kt:497`).
3. **P3.3 (opsiya)** — görüş bitəndən 7 gün sonra çatın „arxiv“
   statusu (silinməsə də, aktiv siyahıdan çıxar — Snapchat-in
   „delete is our default“ prinsipinin yumşaq variantı).
4. Kamera/Lens/Stories/Spotlight kopyalanmır — məhsul müddəasına ziddir.

## Mənbələr (hamısı açılıb təsdiqlənib, 2026-11-28)

1. help.snapchat.com/hc/en-us/articles/7012309470740 — How do I share
   my location on Snap Map (4 rejim, default bağlı)
2. help.snapchat.com/hc/en-us/articles/7012322854932 — How do I turn
   on Ghost Mode (taymerlər: 3h/24s/söndürülənədək; mavi ghost nişanı)
3. help.snapchat.com/hc/en-us/articles/7012394193684 — How do Streaks
   work and when do they expire (24 saat qaydası, 🔥/⏳, bərpa)
4. help.snapchat.com/hc/en-us/articles/7012334940948 — When does
   Snapchat delete Snaps and Chats („delete is our default“, 4 TTL
   rejimi, 31 gün açılmamış)
5. help.snapchat.com/hc/en-us/articles/7012338216468 — My Story Public
   vs Friends fərqi (+ Custom seçim)
6. help.snapchat.com/hc/en-us/articles/7012271311892 — What is Spotlight
   (ictimai kəşf səthi, Live/Trending etiketlər)
7. help.snapchat.com/hc/en-us/articles/45845487406740 — What Snapchat
   plans are available (Snapchat+/Family/Platinum/Lens+ strukturu)
