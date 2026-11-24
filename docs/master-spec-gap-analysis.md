# Master Spec — Kod Bazası ilə Gap Analysis

> Müqayisə: `docs/master-spec.md` (mənbə spec) ↔ kod bazası @ `9b63a14` (main, 2026-11-24).
> Metod: Android (`az.qoshul.app`) və backend (supabase/, service/, infra/) üzrə əl ilə audit.
> Status şkalası: ✅ hazır · 🟡 qismən (yeni iş lazımdır) · ❌ yoxdur.

## Xülasə cədvəli

| # | Spec bölməsi | Status | Qısa qiymət |
|---|---|---|---|
| 1 | Full-width layout | 🟡 | Əsas ekranlarda 20dp kənar padding qalır; edge-to-edge yalnız sistem barları üçündür |
| 1 | Kateqoriya sinxronu | 🟡 | Mərkəzi mənbə var (FakeData, 12 qrup/89 kateqoriya), amma enum yoxdur; EventsScreen/MapScreen-də hardcode dublikatlar |
| 2 | Profil redaktəsi (modul + crop) | ❌ | Yalnız Ad+Soyad+4 statik şəkil; bio/tags/sosial link redaktəsi və qalereya+crop yoxdur |
| 2 | Public Profile View keçidi | ❌ | Route yoxdur; avatar klik → Match ekranı |
| 2 | Stories sistemi | ❌ | Kodda heç bir iz yoxdur |
| 3 | Avto-swipe keçidi | 🟡 | Swipe fizikası işləyir; düymə yolunda animasiya yoxdur |
| 3 | Unmatch real-vaxt silinmə | 🟡 | blockUser() StateFlow ilə UI yeniləyir; websocket yoxdur; ayrıca unmatch yoxdur |
| 3 | Oval vizualizasiya (≥24px) | ✅ | Kart radiusu 24dp (rXl), avatar dairədir |
| 4.1 | Hesab alt-bölməsi (auth-only) | ❌ | Qeydiyyat metodu indikatoru, şifrə dəyişmə, 2FA yoxdur; silmə dialoqu placeholder |
| 4.2 | Bloklananlar funksionallığı | 🟡 | Siyahı + blokdan çıxar işləyir; xəritə/iştirakçı/like məhdudiyyətləri yoxdur |
| 4.3 | Hüquqi mətnlər (GDPR/AR) | ❌ | "Şərtlər və məxfilik" sətri ölüdür (onClick yoxdur) |
| 4.4 | QR referral + Coin bonusu | ❌ | Dinamik QR/link yoxdur; yalnız mətn dialoqu + 100 XP (yalnız dəvət edənə) |
| 5 | "Qoşa" AI (ad, maskot, EQ, LLM) | ❌ | Ad "Qoshul köməkçisi"; LLM yoxdur (keyword mock); MBTI prompta ötürülmür; passiv ifadələr var; Civ-Civ yoxdur |
| 6 | AI tədbir yaradıcısı | 🟡 | aiFill() mock — 3 hardcoded şablon; tarix/etiket doldurmur |
| 6 | Dinamik poster generasiyası | ❌ | "poster" kodda 0 dəfə; Canvas bitmap yaratma yoxdur |
| 7 | Çat empty state | ❌ | D-115 ilə silinib; boş siyahıda yalnız başlıq |
| 7 | Tədbir Profili (qrup identikliyi) | ❌ | Poster+iştirakçı siyahısı+ümumi QR birləşmiş ekranı yoxdur |
| 7 | 4-rəqəmli təhlükəsizlik kodu | ❌ | Fallback kod 6 rəqəmlidir; iştirakçı təsdiq indikatoru yoxdur |
| 8 | 3 müstəqil xəritə layı | ❌ | Bütün pinlər birlikdə render olunur; lay toggle yoxdur |
| 8 | Məkan alt-filtrləri | 🟡 | "İndi açıqdır"/"Endirim var" çipləri var, amma məkan layına bağlı sub-panel deyil |
| 9 | "Qoşa Coin" valyutası | ❌ | Hər yerdə "XP" adlandırılması; coin ikonu yoxdur |
| 9 | Task Center (Novice/Daily/Advanced) | ❌ | Tapşırıq sistemi yoxdur; yalnız sabit XP hadisə sabitləri |
| 9 | Nailiyyətlərin gərginləşdirilməsi | 🟡 | 5 badge var; `levels.badge` boş sütundur; çətinlik artırılmayıb |
| 9 | 50–100 suallıq 16P testi | ❌ | Mövcud test 10 suallıq Big Five-dur; 5 arxetip; səhifədə 1 sual |
| 10 | İkili QR doğrulama | ❌ | GPS özünü check-in + təşkilatçı tək təsdiqi var; per-user QR, məkan admin skanı, ikili təsdiq yoxdur. Kamera skaneri (ZXing) D-081 ilə silinib |
| 11 | Biznes hesabı (VÖEN) | ❌ | Yalnız PARTNER rolu + managed_by; VÖEN/sənəd/korporativ tədbir yoxdur |
| 11 | Biznes Admin Paneli (web) | ❌ | Yalnız moderasiya admin API (X-Admin-Token); veb panel yoxdur |
| 11 | Masa/Kabinet idarəçiliyi | ❌ | venue_slots tarix/saat slotudur; masa/kabinet nömrələri, avadanlıq, interaktiv sxem yoxdur |
| 11 | Məkan Statusu/Hekayələri | ❌ | Stories cədvəli yoxdur; storage bucket plan mərhələsindədir |

## Detallı tapıqlar (əsas fayl referansları)

Android prefiks: `android-app/app/src/main/java/az/qoshul/app/` (**…** ilə qısaldılır).

### 1. UI/UX
- 20dp contentPadding hər yerdə: `…/features/feed/FeedScreen.kt:252-255`, `…/features/events/EventsScreen.kt:177-180`, `…/features/profile/ProfileScreen.kt:112-116` və s. Karusellər 20dp içəridə başlayır (`FeedScreen.kt:338,365,387,424,454`).
- "Yaxınlıqdakı tədbirlər" adlı bölmə yoxdur (ən yaxını "Aktiv tədbirlər", `FeedScreen.kt:329`).
- Kateqoriyalar: mərkəzi mənbə `…/core/data/FakeData.kt:55-84` (12 qrup, 89 kateqoriya). "Yarat" bu mənbədən götürür (`CreateFinishSheets.kt:248-253`), amma filtr ekranında ayrıca hardcode 12 qrup (`EventsScreen.kt:135-157`, D-110 ilə "Biznes və karyera" filtdən çıxarılıb → 11), MapScreen-də başqa hardcode (`MapScreen.kt:307`). EventCategory enum yoxdur.

### 2. Profil
- ProfileEditScreen yalnız Ad+Soyad+4 statik brend şəkli (`ProfileEditScreen.kt:61-198`). Bio/tags/sosial linklər yalnız onboarding-də (`OnboardingScreen.kt:237-330`).
- Crop kitabxanası yoxdur (build.gradle.kts dependencies-də uCrop/cropper/image-picker yoxdur).
- Stories: grep 0 nəticə. Public Profile View: Route siyahısında yoxdur (`…/core/ui/Nav.kt:72-87`); PersonMiniCard klik → Route.Match (`FeedScreen.kt:430`).

### 3. Matchmaking
- Swipe fizikası işləyir (`MatchScreen.kt:277-345`, 110dp hədd, uçuş animasiyası); düymə yolu animasiyasız dəyişir (`MatchScreen.kt:485-507`).
- blockUser/unblockUser → emit → StateFlow (`AppEngine.kt:1026-1040`); websocket yoxdur.
- Şəkil formaları: kart 24dp radius (rXl, `core/theme/Color.kt:168-178`), avatar dairə — spec-in ≥24px tələbi qarşılanır.

### 4. Ayarlar
- Hesab alt-bölməsində vizual məlumatlar var (`SettingsScreen.kt:115-145`); 2FA/şifrə dəyişmə yoxdur; silmə dialoqu placeholder (`SettingsScreen.kt:466-487`).
- Bloklananlar işləyir (`SettingsScreen.kt:401-426`); blokun xəritə/iştirakçı/like təsiri yoxdur.
- "Şərtlər və məxfilik" ölü sətir (`SettingsScreen.kt:217`).
- Dəvət: `inviteFriend()` +100 XP, bir dəfə (`AppEngine.kt:360-374`); QR generasiyası ümumiyyətlə var (Rust qrcodegen, `…/core/ui/QrCode.kt`) amma referral üçün istifadə olunmur.
- Auth realdır: Supabase GoTrue (`…/core/backend/SupabaseAuth.kt:69-150` — signUp/signIn/OTP/reset/Google).

### 5–6. Qoşa AI
- Başlıq "Qoshul köməkçisi" (`AssistantScreen.kt:93`); balon adı "Köməkçi".
- assistantReplyFor() keyword-mock (`AppEngine.kt:1179-1225`); personalityV state-də var (`AppEngine.kt:354-357`) amma AI istifadə etmir. LLM inteqrasiyası heç yerdə yoxdur.
- Passiv ifadələr: `AppEngine.kt:1192,1203,1217`, `AssistantScreen.kt:115` ("İlk mesajı sən yaz").
- aiFill() mock: 3 şablon (`CreateFinishSheets.kt:148-166,224-240`); aiLocation ölü state.
- Poster generasiyası: "poster" 0 keçid.

### 7. Çatlar
- Siyahı boşsa başlıqdan başqa heç nə (`ChatsScreen.kt:233-235,279`, D-115).
- Tədbir Profili ekranı yoxdur; iştirakçılar yalnız say olaraq ("N iştirakçı").
- Fallback kod 6 rəqəm: `Models.kt:235`, `XpEconomy.kt:122`.

### 8. Xəritə
- `venuePins + eventPins + peoplePins` həmişə birlikdə (`MapScreen.kt:203-220`); lay toggle yoxdur. "İndi açıqdır"/"Endirim var" çipləri ümumi filtdir (`MapScreen.kt:344-346`).

### 9. Oyunlaşdırma
- "Coin" sözü kodda yoxdur; XP ikonları WorkspacePremium/Toll/EmojiEvents (`XpCardScreen.kt:97`).
- Task/quest sistemi yoxdur; XpRules sabitləri `Models.kt:191-234`. 5 badge (`Models.kt:305-313`).
- Test: 10 sual Big Five (`FakeData.kt:499-510`), 5 arxetip (`PersonalityQuizScreen.kt:258-264`), səhifədə 1 sual.

### 10. QR doğrulama
- Check-in GPS əsaslıdır (`supabase/migrations/0002_server_rules.sql:484-549`, ST_DWithin 200m).
- Təşkilatçı bitirmə axını: statik QR + "QR-ı oxudum" düyməsi — kamera skaneri yoxdur (`CreateFinishSheets.kt:576-740`); ZXing D-081 ilə silinib (`app/build.gradle.kts:211`).
- Rezervasiya zənciri var: fn_confirm_reservation (menecer skanı) → fn_use_reservation (`0002:600-655`).
- Per-user tədbir QR, ikili təsdiq, ikili təsdiqə bağlı kupon aktivləşməsi — hamısı yoxdur.

### 11. Biznes
- PARTNER rolu (`0001_schema.sql:63-64`), venues.managed_by (`0001:176`); "Biznes rejimi" sheet-i demo axınıdır (`PartnersScreen.kt:192-310`).
- service/src/admin.rs yalnız moderasiyadır. VÖEN/merchant/business kodda 0 iz.
- venue_slots tarix/saat slotudur (`0001:191-198`); masa/kabinet nömrələri, PlayStation/Karaoke/VIP atributları, interaktiv sxem yoxdur.
- Storage bucket (event-photos) plan mərhələsində (`supabase/README.md:99-104`); infra-da storage profili deyaktivdir.

## Prioritet təklifi (icra ardıcıllığı)

1. **P0 — mövcud sistemlərin spec-ə uyğunlaşdırılması** (aşağı risk, yüksək görünən dəyər): full-width padding, kateqoriya enum-unifikasiyası, çat empty state, 4-rəqəmli kod, "Qoşa Coin" rename, düymə-animasiyası, blok məhdudiyyətləri, hüquqi mətnlər, silmə axını.
2. **P1 — Qoşa AI core**: LLM inteqrasiyası (service/ üzərindən proksi), sistem promptunun EQ modelinə keçirilməsi, Civ-Civ maskotları, MBTI-nin prompta ötürülməsi, 16P testinin 50–100 suala genişləndirilməsi (6-sual paginasiya).
3. **P2 — Məzmun infrastrukturu**: storage bucket-lərin aktivləşdirilməsi, qalereya+crop, profil modul redaktəsi, Stories (yalnız tədbir mənşəli), poster generasiyası (Canvas), Public Profile View.
4. **P3 — Doğrulama və biznes**: per-user QR + ikili skan zənciri (kamera skanerinin qaytarılması), Task Center + Coin iqtisadiyyatı, biznes hesabı (VÖEN), admin paneli, masa/kabinet idarəçiliyi.

> Qeyd: bu sənəd statusun qeydidir; icra üçün ayrıca task breakdown (D-xxx) açılmalıdır.
