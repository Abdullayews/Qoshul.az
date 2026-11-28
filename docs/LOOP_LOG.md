# LOOP_LOG — İterasiya dövrlərinin jurnalı

Format: hər dövr məqsəd → dəyişikliklər → verifikasiya → nəticə. Növbəti dövr başlamazdan əvvəl bu fayl oxunur; bitmiş iş təkrarlanmır.

---

## Loop 1 — 2026-11-28

**Məqsəd**: Phase 0 (kəşf) tamamlansın; P0 kritik xətalar (GPS, Google giriş, login gecikməsi) düzəldilsin.

**Bazа**: fe7dba1 (v0.59.1, D-181 tamamdır — maraq FK xətası + onboarding qapısı).

### Phase 0 (kəşf) — TAMAMLANDI

- Repo struktur, build mühiti, asılılıqlar, manifest inventarı → `docs/PHASE0_REPORT.md` §1
- Xəritə: dual backend (Google ƏSAS D-108 direktivi / MapLibre Compose 0.20 fallback); user dot + accuracy dairəsi + bearing + stale artıq VAR (D-170/D-180)
- Lokasiya: `QoshulLocation` — LocationManager GPS+network, mock rədd, 50m accuracy qapısı (stream-də), 15m/4s throttle; `locateOnce`-də accuracy filtri YOX (P0.1-H1)
- Auth: Credential Manager (filter=false ✓, nonce ✓, GoTrue IDToken ✓); signOut-da clearCredentialState YOX (P0.2-H1); köhnə session təmizliyi YOX (P0.2-H2); timeout-lar YOX (P0.3-H1)
- Cache-silmə təhlükəsi YOXDUR (P0.4 bu hissəsi qarşılanıb); crash reporting yalnız lokal Safety hook
- Ekranlar: Create sheet rememberSaveable ✓; ProfileEdit-də e-poçt+şifrə var (6.3-ə görə Ayarlara köçürüləcək), dillər/qrup ölçüsü/büdcə/səviyyə YOX (6.4-də əlavə)
- Testlər: 37 JVM/Robolectric faylı; CI 3 job; emülator CI-də YOX

### Açıq suallar (sahibdən)

1. Offline xəritə lazımdırmı? (təklif: bu mərhələdə YOX)
2. Tile provayderi: OpenFreeMap (təklif) / MapTiler / self-host?
3. Xəritə light/dark variantı lazımdırmı? (təklif: tək sxem)
4. Google SİGN-İN testi üçün test Google hesabı + SHA-1 qeydiyyatı (bu mühitdə mümkün deyil)

### Verifikasiya statusu (Phase 0)

- Kompilyasiya: baz commit fe7dba1 CI yaşıl (3/3 job) — dəyişiklik yoxdur
- Emülator/real cihaz: YOX (mühit məhdudiyyəti — PHASE0_REPORT §5)

### Loop 1 davamı — P0 icrası (D-182, v0.59.2)

**Dəyişən fayllar**:
- `core/location/QoshulLocation.kt` — isUsableFix vahid filtri (mock+accuracy+10s köhnəlik), locateOnce filtrli, isLocationEnabled
- `core/backend/SupabaseBackend.kt` — signInWithGoogle: əvvəlki signOut + e-poçt müqayisəsi; signOut(context)+clearCredentialState; fetchGoogleConfig TTL cache
- `features/auth/AuthScreen.kt` — AUTH_TIMEOUT_MS 15s (GoTrue) / 60s (Google dialoq); vaxtaşımı mesajları
- `features/map/MapScreen.kt` — GPS status çipi (axtarış ±50m / söndürülüb → ayarlar)
- `features/settings/SettingsScreen.kt` — signOut(signOutCtx)
- `DirectivesAuditTest.kt` — 6 yeni D-182 audit testi (35 ümumi)
- `docs/PHASE0_REPORT.md`, `docs/decisions.md` (D-182), version 0.59.2 (69)

**Verifikasiya**: kompilyasiya + unit test → CI (bu push); emülator/real cihaz YOX (mühit məhdudiyyəti — PHASE0_REPORT §5; GPS xəritədə görünməsi real cihazda SAHİB tərəfindən yoxlanılmalıdır).

**Bağlanan bəndlər**: P0.1 (kod+UI), P0.2 (sessiya təmizliyi+müqayisə), P0.3 (timeout+cache).
**Açıq qalanlar**: P0.1 real cihaz verifikasiyası; P0.4 (düymə çökmələri — emülator tələb edir); P1 MapLibre əsas backend keçidi; P1-P2 UX (6.2-6.4); P3 Snapchat analizi; P4 optimizasiya.
**Yeni aşkarlananlar**: fetchGoogleConfig hər girişdə HTTP round-trip edirdi (TTL cache ilə həll); `GoogleIdTokenCredential.id` deprecated (yeni `email` propertisi yeni googleid versiyasında — hazırda 1.2.1-də `id` istifadə olunur).

---

## Loop 2 — 2026-11-28 (D-183, v0.60.0)

**Məqsəd**: P1 MapLibre miqrasiyası — əsas backend keçidi, style URL konfiqurasiyası, attribution, OpenGL ES 3.0.

**Dəyişən fayllar**:
- `app/build.gradle.kts` — MAP_BACKEND default „maplibre“; yeni MAP_STYLE_URL BuildConfig (local.properties → env → OpenFreeMap bright); versiya 0.60.0 (70)
- `MapPins.kt` — resolveMapBackend sənədlənməsi (maplibre əsas; məntiq dəyişməz)
- `MaplibreCanvas.kt` — MAP_ATTRIBUTION sabiti (© OpenStreetMap contributors · OpenFreeMap)
- `MapScreen.kt` — attribution çipi xəritədə (aşağı-sol; pin kartı açılınca gizlənir)
- `AndroidManifest.xml` — uses-feature glEsVersion 0x00030000 required
- `MapBackendTest.kt` — 2 yeni test (default maplibre; style URL OpenFreeMap)
- `DirectivesAuditTest.kt` — D-183 4 audit testi (39 ümumi)
- `docs/decisions.md` — D-183

**Verifikasiya**: kompilyasiya + unit test → CI (bu push); emülator vizual yoxlanılmadı (mühit məhdudiyyəti — xəritə renderi vizual olaraq real cihazda yoxlanılmalıdır).

**Bağlanan bəndlər**: P1 §5.1 (backend keçidi + minSdk/ES3 yoxlaması), §5.2 (style — OpenFreeMap bright konfiqurasiya olunan URL), §5.3 (tile mənbəyi konfiqurasiyası), attribution tələbi (§2).
**Açıq qalanlar**: P1 §5.5 cleanup (Google Maps SDK + maps-compose asılılığını silmək — D-183-də SAXLANILDI, geri qaytarıla bilən keçid üçün; silinməsi ayrıca qərar tələb edir); P1-P2 UX (6.2-6.4: dropdown komponenti, Ayarlar→Hesab, Profilə dillər/qrup/büdcə/səviyyə); P3 Snapchat analizi; P4 optimizasiya.
**Yeni aşkarlananlar**: D-117-də SDK attribution overlay tam söndürüldüyündən lisenziya krediti UI-da yox idi — D-183 bizim çiplə həll etdi.

### Loop 3 planı (növbəti)
P1-P2 UX: (1) vahid dropdown komponenti (Material3 ExposedDropdownMenuBox, ≥48dp); (2) Ayarlar → Hesab bölməsi (e-poçt + şifrə köçürməsi ProfileEdit-dən); (3) Profili redaktə-yə dillər/qrup ölçüsü/büdcə/səviyyə (6.4 tələbləri: „Heç nə“ default NULL, „Fərq etmir“, „Öz aralığım“ min≤max); (4) xəritə zoom 16-17 + jest izləməni dayandırır.

---

## Loop 3 — 2026-11-28 (D-184, v0.61.0)

**Məqsəd**: P1-P2 UX paketi — vahid dropdown, profil redaktə zənginləşdirməsi, Ayarlar→Hesab köçürməsi, xəritə izləmə rejimi.

**Dəyişən fayllar**:
- `core/ui/Ui.kt` — QRDropDown (Material3 ExposedDropdownMenuBox, 56dp, allowEmpty)
- `ProfileEditScreen.kt` — rememberSaveable + Qrup ölçüsü/Büdcə(Öz aralığım)/Ana dil səviyyəsi/Dillər çipləri; e-poçt bölməsi çıxarıldı
- `SettingsScreen.kt` — Hesab bölməsinə E-poçt + Şifrə sətirləri + idarəetmə dialoqu (D-167-dən köçürmə)
- `MapScreen.kt` — followMe state; dəqiq fix → izləmə başlayır
- `MaplibreCanvas.kt` — followMe (zoom 16) + CameraMoveReason.Gesture jest ləğvi
- `GoogleMapCanvas.kt` — paritet (CameraMoveStartedReason.GESTURE)
- `AppEngine.kt` — setGroupSize/setBudget boş string ötürür (NULL clearing)
- `supabase/migrations/0015_profile_null_clearing.sql` — fn_save_profile üçvəziyyətli clearing
- `DirectivesAuditTest.kt` — D-184 6 audit testi
- `docs/decisions.md` — D-184

**Verifikasiya**: kompilyasiya + unit test → CI (bu push); emülator vizual yoxlanılmadı (mühit məhdudiyyəti).

**Bağlanan bəndlər**: 6.1 (rememberSaveable), 6.2 (QRDropDown ≥48dp), 6.3 (Ayarlar→Hesab), 6.4 (Profilə dillər/qrup/büdcə/səviyyə — „Heç nə“ NULL, „Fərq etmir“, „Öz aralığım“ min≤max), 6.5 (xəritə zoom 16 + jest izləməni dayandırır).
**Açıq qalanlar**: P1 §5.5 cleanup (Google Maps SDK silinməsi — ayrıca qərar); P3 Snapchat analizi (docs/feature_parity_snapchat.md); P4 optimizasiya; real cihaz vizual yoxlama.

### Loop 4 planı (növbəti)
P3: docs/feature_parity_snapchat.md — rəsmi Snapchat mənbələrindən (support.snapchat.com, business.snapchat.com) kateqoriyalar üzrə paritet analizi: Stories/Map/Chat/Snap/Discover qarşılıqları; mənbə linkli. Sonra P4 optimizasiya dövrü (başlanğıc sürəti, R8, keş).

---

## Loop 4 — 2026-11-28 (D-185, v0.62.0)

**Məqsəd**: P3 — Snapchat paritet analizi (rəsmi mənbələrdən) + P3.2 icrası.

**Dəyişən fayllar**:
- `docs/feature_parity_snapchat.md` — YENİ: 7 kateqoriya üzrə paritet analizi (mövqe məxfiliyi, streak, ephemeral çat, stories, kamera/Lens, kəşf, monetizasiya); 7 rəsmi help.snapchat.com mənbəsi açılıb təsdiqlənib; hər Qoshul.az faktı kod fayl:sətir ref ilə
- `ProfileScreen.kt` — P3.2: giriş seriyası göstəricisi (🔥 N gün) statistika sırasında
- `DirectivesAuditTest.kt` — D-185 audit testləri
- `docs/decisions.md` — D-185

**Verifikasiya**: kompilyasiya + unit test → CI (bu push).

**Bağlanan bəndlər**: P3 (Snapchat analizi — LOOP_LOG Loop 3 planındakı vəd); P3.2 (streak UI).
**Açıq qalanlar**: P3.1 (onboarding xəritə rejimi seçimi — D-176 dostluq sistemindən sonra); P3.3 (görüş sonu çat arxivi — opsiya); P4 optimizasiya dövrü (başlanğıc sürəti, R8, keş); real cihaz vizual yoxlama.

### Loop 5 planı (növbəti)
P4 optimizasiya: (1) başlanğıc sürəti ölçümü (baseline report); (2) R8/ProGuard qurğuları yoxlaması; (3) şəkil/keş strategiyası; (4) APK ölçüsü analizi (30 MB —成分 baxımı). Sonra sahib tərəfindən real cihaz testi dövrü.

---

## Loop 5 — 2026-11-28 (D-186, v0.63.0)

**Məqsəd**: P4 optimizasiya — APK ölçüsü təhlili, başlanğıc baseline,
ən yüksək dəyərli risk-free optimizasiyanın icrası.

**Təhlil** (`docs/perf_baseline.md`):
- APK 30.5 MB: lib/ 25.1 MB (maplibre .so — strukturla bağlı), dex 9.3 MB
  (R8 açıqdır), maplibre 126 dil asseti ~1 MB
- Başlanğıc zənciri təmiz: Application.onCreate 3 əməliyyat, AppEngine.init
  O(1), profileinstaller var — başlanğıc optimizasiyası lazımsız
- R8 keep azaltma və ABI buraxma RƏDD (runtime risk / cihaz payı)

**İcra (D-186)**:
- `build.gradle.kts` packaging: maplibre 122 lokal qovluq regex exclude
  (values + az/en/tr/ru qalır) — gözlənilən −0.9 MB
- `docs/perf_baseline.md` — ölçülü baseline + təkrarölçüm komandası

**Verifikasiya**: CI build + audit testi (D-186) + release APK ölçüsü
müqayisəsi (perf_baseline.md §4).

**REVERT (Loop 5 davamı, v0.64.0)**: v0.63.0 APK ölçüsü dəyişməz
qaldı (32,023,089 bayt) — exclude AAR assetlərinə tətbiq olunmur.
Kritik ölçüm: ZIP entry-lərin real sıxılmış ölçüsü 7.3 KB (0.02%) —
`du` blok yuvarlaqlaşdırması (8 KB × 126 fayl) „1 MB" yalanını
göstərirdi. Exclude tam revert edildi; 7 KB üçün Variant API
MERGED_ASSETS transformu yazmaq mənasızdır. Sənəd düzgün metodla
(zipfile compress_size) yenidən yazıldı; dərs: çoxsaylı kiçik
fayllarda du YOX, ZIP ölçüsü.

### Loop 6 planı (növbəti)
Sahib gözləməsi: real cihaz testi (v0.63.0 APK) — xəritə çat oxuma,
çat realtime, swipe axını, streak göstəricisi profildə (D-185), APK
ölçüsü təsdiqi. Kod tərəfində: P3.1 (onboarding xəritə rejimi seçimi)
və ya Play app bundle hazırlığı (per-device .so çatdırılma) — sahibin
prioritet seçiminə görə.
