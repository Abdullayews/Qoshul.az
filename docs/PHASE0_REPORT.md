# PHASE 0 — Kəşf Hesabatı (yalnız oxuma, dəyişiklik yoxdur)

Tarix: 2026-11-28 · Repo: Abdullayews/Qoshul.az @ fe7dba1 (v0.59.1) · Tərtibatçı: senior Android mühəndisi (agent)

---

## 1. Inventar cədvəli

### 1.1 Build mühiti

| Parametr | Dəyər | Mənbə |
|---|---|---|
| AGP | 9.5.0 | `android-app/gradle/libs.versions.toml:7` |
| Kotlin | 2.4.21 | `libs.versions.toml:8` |
| Compose BOM | 2026.09.00 | `libs.versions.toml:9` |
| compileSdk | 37 | `app/build.gradle.kts:47` |
| minSdk | 24 | `app/build.gradle.kts:50` |
| targetSdk | 36 | `app/build.gradle.kts:51` |
| Java | 17 (desugaring aktiv) | `app/build.gradle.kts:135-137` |
| R8/minify | release-da aktiv (`isMinifyEnabled=true`) | `app/build.gradle.kts:104` |
| ABI | release: arm64-v8a, armeabi-v7a; debug: hamısı | `app/build.gradle.kts:112` |

### 1.2 Asılılıqlar (xəritə/auth/şəbəkə əsaslı)

| Kitabxana | Versiya | Təyinat | Qeyd |
|---|---|---|---|
| `org.maplibre.gl` (MapLibre Compose) | 0.20.0 (FFI nüvə 0.202610.0) | Vektor xəritə — **FALLBACK backend** | `libs.versions.toml:14`, `build.gradle.kts:154-156` |
| `com.google.maps.compose` + `play-services-maps` | 9.0.1 / 20.0.0 | Google xəritə — **ƏSAS backend** (D-108 direktivi) | `build.gradle.kts:163-165` |
| `io.github.jan-tennert.supabase` (umbrella) | 3.9.0 | Supabase core | auth-kt 3.9.0 (v3), postgrest, realtime |
| `io.ktor` CIO | 3.6.1 | Şəbəkə | |
| `androidx.credentials` + `googleid` | 1.6.0 / 1.2.1 | Google Credential Manager sign-in | `build.gradle.kts:185-187` |
| `multiplatform-settings` | 1.3.0 | Sessiya persistence (SettingsStorage) | |
| `kyant0 backdrop` | 2.0.1 | Liquid glass UI | |
| DI | **YOX** | — | Manual singleton-lar (`SupabaseBackend`, `AppEngine`) |
| Şəkil/cache kitabxanası | **YOX** (Coil/Glide yoxdur) | — | Şəkillər brend resurs + supabase storage (0011 avatars, PHOTO_UPLOAD_ENABLED=false) |

### 1.3 AndroidManifest icazələri

`app/src/main/AndroidManifest.xml`:
- `INTERNET` ✓
- `ACCESS_COARSE_LOCATION` ✓, `ACCESS_FINE_LOCATION` ✓
- `READ_MEDIA_IMAGES` (API 33+), `READ_EXTERNAL_STORAGE` (≤32) ✓
- `POST_NOTIFICATIONS` ✓
- **YOXDUR**: `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, `uses-feature` OpenGL ES (glEsVersion bildirilməyib)

### 1.4 Xəritə arxitekturası (mövcud vəziyyət — əksər tələblər ARTIQ qarşılanıb)

| Funksiya | Status | Fayl:sətir |
|---|---|---|
| Dual backend seçimi (`resolveMapBackend`: google/maplibre) | var | `MapScreen.kt` + `BackendConfig` |
| Vektor tile + style (MapLibre, OpenFreeMap) | var | `MaplibreCanvas.kt` |
| Google Maps raster/vector | var | `GoogleMapCanvas.kt` |
| Marker/pin-lər (venue/event/person, lay filtri) | var | `MapPins.kt`, `MapScreen.kt:290-310` |
| Clustering | **YOX** | — |
| Geocoding / routing | **YOX** | — |
| Offline cache (OfflineManager/MBTiles) | **YOX** | — |
| User location dot + dəqiqlik dairəsi + bearing oxu | var | `GoogleMapCanvas.kt:176-192`, `MaplibreCanvas.kt:114+` |
| Stale fix gizlətmə (30s) | var | `MapScreen.kt:199-208` (D-180) |
| Kamera animasiyası (`pendingCamera`) | var | `MapScreen.kt:251+` |
| Attribution | **YOXDUR — P1-də əlavə olunmalı** | — |

### 1.5 Lokasiya (P0.1 mövcud təhlil)

`core/location/QoshulLocation.kt` — Framework `LocationManager` (Play Services asılılığı yoxdur):
- Provayderlər: GPS + NETWORK (+PASSIVE last-known), `requestLocationUpdates(2000ms, 10m)` — `QoshulLocation.kt:169`
- Mock müdafiə: `isFromMockProvider` → RƏDD — `QoshulLocation.kt:77,145,215` ⚠️ `isFromMockProvider` API 31-dən deprecated; `Location.isMock` API 31+ (`mock` properti) — P0.1-də yenilənməli
- Dəqiqlik qapısı: `MAX_ACCURACY_M = 50f` — `QoshulLocation.kt:114` (tələb 20m deyil, 50m — konfiqurasiya olunan sabit)
- Hərəkət yumşaltması 15m + throttle 4s — `QoshulLocation.kt:117-120`
- `locateOnce`: last-known (accuracy filtri YOX!) → 5s canlı gözləmə — `QoshulLocation.kt:63-90` ⚠️ last-known köhnə fix-i accuracy yoxlamadan qəbul edir → P0.1 hipotez
- Supabase-ə yazılma: `engine.reportLocation` → `LocationUpdateIntent` → `fn_update_location` (yalnız stream-dən, yəni accuracy≤50m fix-lər) — `MapScreen.kt:187-194`
- İcazə axını: FINE soruşulur; kalıcı rədd → tətbiq ayarlarına keçid — `MapScreen.kt:246-270`
- **YOXDUR**: `SettingsClient`/`LocationSettingsRequest` (GPS söndürülsə dialoq), "Dəqiq mövqe axtarılır (±X m)" UI göstəricisi, `elapsedRealtimeNanos` köhnəlik yoxlaması

### 1.6 Auth (P0.2/P0.3 mövcud təhlil)

`core/backend/SupabaseBackend.kt` + `features/auth/AuthScreen.kt`:
- Google: **Credential Manager** `GetGoogleIdOption` — `setFilterByAuthorizedAccounts(false)` (hesab seçimi HƏMİŞƏ göstərilir — tələb qarşılanır), `setNonce(hashed)` — `SupabaseBackend.kt:168-171`
- Supabase: `auth.signInWith(IDToken) { provider=Google; nonce=raw }` (GoTrue rəsmi axını) — `SupabaseBackend.kt:189-193`
- WEB client ID: m4 `/config`-dən dinamik (`fetchGoogleConfig`) → `BuildConfig` fallback — `SupabaseBackend.kt:198-236`
- Logout: `client.auth.signOut()` — `SupabaseBackend.kt:246-249` ⚠️ `clearCredentialState` çağırılmır → P0.2 hipotez (Credential Manager cache-i qalır, amma `filter=false` olduğundan auto-select yoxdur)
- Email yoxlaması (seçilmiş Google e-poçtu ↔ `auth.currentUser.email` müqayisəsi): **YOX** → P0.2-də əlavə olunacaq
- Timeout: **BÜTÜN auth çağırışlarında YOX** (Ktor default / GoTrue default) → P0.3 hipotez: zəif şəbəkədə `busy=true` sonsuz qalır; `busy=false` bütün catch-lərdə var, amma timeout olmasa catch heç gəlmir
- `googleSignIn` ardıcıllıq: `fetchGoogleConfig` (şəbəkə) → `getCredential` → `signInWithIdToken` — hamısı sequential; fetch + sign paralel mümkün deyil (asılıdırlar), amma `fetchGoogleConfig` m4-yə HTTP — Ktor timeoutu yoxdur
- Sessiya: `SettingsStorage` (multiplatform-settings) — restart-da qalır ✓

### 1.7 Çökmə / cache-silmə (P0.4 mövcud təhlil)

- `clearApplicationUserData` — **TAPILMADI** ✓
- `cacheDir.deleteRecursively` — **TAPILMADI** ✓
- `SharedPreferences.clear()` / `DataStore.clearAndDeleteAll` — **TAPILMADI** ✓
- `Context.deleteDatabase` — **TAPILMADI** ✓ (Room YOXDUR, DB yoxdur)
- `clear()` çağırışları yalnız in-memory list/lar (`AppEngine.kt:582-595` — server datası ilə əvəzetmə, normal) və `NavState.popAll` — **istifadəçi məlumatı silinmir** ✓
- Crash loglama: `Safety.onCaught` hook (qlobal) — `QoshulApplication`-a `Thread.setDefaultUncaughtExceptionHandler` var? → yoxlanıldı: `Safety` obyekti `core/safety/Safety.kt` (logcat mirror). Xarici crash reporting (Crashlytics) **YOX**
- `allowBackup=false` — `AndroidManifest.xml:24`

### 1.8 Ekranlar (P1–P2 UX tələbləri üçün baza)

| Ekran | Fayl | State idarəsi | P0/P1 tapıntı |
|---|---|---|---|
| Tədbir yaratma (Create sheet) | `features/events/CreateFinishSheets.kt` (110-116: `rememberSaveable` — title/idea/location/desc/tags/price/city) | rememberSaveable ✓ | Dropdown-lar: mövcuddur, dizayn yoxlanılacaq |
| Ayarlar | `features/settings/SettingsScreen.kt` (915 sətir) | engine state | E-poçt/şifrə bölməsi YOXDUR → 6.3 köçürmə lazımdır |
| Profili redaktə | `features/profile/ProfileEditScreen.kt` (387 sətir) | `remember` (rememberSaveable YOX) | E-poçt + şifrə bölməsi BURADADIR (D-167) → 6.3-ə görə Ayarlara köçürüləcək; dillər/qrup ölçüsü/büdcə/səviyyə burada YOXDUR → 6.4-ə görə əlavə olunacaq |
| Onboarding | `features/onboarding/OnboardingScreen.kt` | rememberSaveable + engine | D-181 (son iş) |

### 1.9 Testlər və CI

- 37 test faylı (JVM + Robolectric Compose UI) — `app/src/test/java/az/qoshul/app/`
- CI: `.github/workflows/native.yml` — 3 job (Native/Service/Self-host) + Release (tag-driven)
- CI-də emülator YOXDUR — bütün UI testlər Robolectric JVM-də
- `GoogleAuthAuditTest.kt` mövcuddur (auth paritet testləri)

### 1.10 Supabase (self-host, Oracle A1)

- URL: `http://89.168.117.146:8000` (GoTrue+PostgREST+Realtime), anon key build mühitindən
- Migrations: `supabase/migrations/0001-0014` (0014: D-181 FK guard — son)
- RLS aktiv; `fn_save_profile`, `fn_update_location`, `fn_set_avatar` RPC-ləri
- **Test (staging) mühiti AYRI YOXDUR** — canlı serverdə test hesabları ilə işlənir (bax açıq sual d)

---

## 2. P0 xətaları — kök səbəb hipotezləri və sübutları

### P0.1 GPS lokasiyası görünmür

| # | Hipotez | Sübut (fayl:sətir) | Status |
|---|---|---|---|
| H1 | `locateOnce` last-known-i **accuracy yoxlamadan** qəbul edir — köhnə/səhv fix dot-u səhv yerdə göstərir; stream isə 50m qapısı ilə gələndə düzəlir, amma istifadəçi "yanlış yerdəm" görür | `QoshulLocation.kt:69-80` (locateOnce-da accuracy filtri yoxdur; yalnız stream-də var — :147) | yoxlanılmadı (emülator tələb edir) |
| H2 | GPS söndürülübsə (yaxud Google location services off) heç bir dialoq YOXDUR — `Unavailable` səssiz qayıdır, istifadəçi "lokasiya işləmir" deyə report edir | `QoshulLocation.kt:45`, `MapScreen.kt:171-179` (`else -> Unit`) | yoxlanılmadı |
| H3 | `isFromMockProvider` API 31+ deprecated və bəzi cihazlarda yanlış-negative verir; `Location.isMock` (API 31+) istifadə edilmir | `QoshulLocation.kt:77,145,215` | yoxlanılmadı |
| H4 | "Dəqiq mövqe axtarılır… (±X m)" UI göstəricisi yoxdur — istifadəçi dəqiqlik gözləntisini görmür, kobud fix-i "xəta" sayır | `MapScreen.kt:155-260` (belə UI yoxdur) | təsdiqləndi (kod oxunuşu) |
| H5 | `MAX_ACCURACY_M=50` çox liberaldır — şəhər daxilində 50m kobud göstərir; tələb 20m həddi istəyir | `QoshulLocation.kt:114` | təsdiqləndi (kod oxunuşu) |

**Nəticə**: GPS kodu mövcuddur və nisbətən sağlamdır (mock rədd, throttle, stale). Əsas çatışmazlıqlar: accuracy UI göstəricisi, settings dialoqu, `locateOnce`-də accuracy/köhnəlik filtri, isMock modern API.

### P0.2 Google ilə giriş səhv hesaba daxil olur

| # | Hipotez | Sübut | Status |
|---|---|---|---|
| H1 | `signOut` `clearCredentialState` çağırmır — Credential Manager keçmiş seçimi xatırlayır; amma `setFilterByAuthorizedAccounts(false)` olduğundan **auto-select baş verə bilməz** | `SupabaseBackend.kt:246-249` vs `:169` | qismən istisna — yoxlanılmadı |
| H2 | **Köhnə Supabase session**: yeni girişdən əvvəl `signOut()` çağırılmır; GoTrue köhnə refresh tokeni ilə sessiyanı bərpa edə bilər → seçilmiş Google hesabı ≠ açılan profil | `AuthScreen.kt:109-123` (signInWithGoogle-dan əvvəl signOut yoxdur); `SettingsStorage` sessiyanı saxlayır | yoxlanılmadı (ən güclü namizəd) |
| H3 | Profil `auth.uid()` ilə yox, köhnə `profiles` sətri ilə yüklənir (uid join xətası) | `SupabaseReader.loadAll` → `readMyProfile` (uid əsaslı, D-181-də təsdiqləndi) | istisna — düzgün işləyir |
| H4 | E-poçt müqayisəsi yoxdur — uyuşmazlıq səssiz keçir | `AuthScreen.kt:119-121` | təsdiqləndi (kod oxunuşu) |

### P0.3 Login-də uzun fırlanma

| # | Hipotez | Sübut | Status |
|---|---|---|---|
| H1 | Auth çağırışlarında timeout yoxdur — zəif şəbəkədə `busy=true` qalır, catch heç işə düşmür | `AuthScreen.kt:109-123` (`withTimeout` yoxdur); `SupabaseBackend.kt` (Ktor/GoTrue default timeout-lar) | təsdiqləndi (kod oxunuşu) |
| H2 | `fetchGoogleConfig` m4-yə ekstra HTTP round-trip — m4 yavaş cavab verəndə girişdən əvvəl gecikmə | `AuthScreen.kt:114`, `SupabaseBackend.kt:198+` | təsdiqləndi |
| H3 | Girişdən sonra `refreshFromServer` tam `loadAll` (events+people+partners+offers+slots+profil) — hamısı login kritik yolunda | `AppEngine.kt:646-663` (`refreshFromServer` → `SupabaseReader.loadAll`) | təsdiqləndi |
| H4 | Google dialoq ləğvi (cancel) busy-ni saxlayır mı? — `GetCredentialCancellationException` `Throwable` catch-də tutulur, `busy=false` çağırılır → təhlükəsiz | `AuthScreen.kt:122-123` | istisna |

### P0.4 Düymə çökmələri + cache silinməsi

- Cache-silmə təhlükəsi **TAPILMADI** (yuxarıda 1.7) — bu tələb artıq qarşılanır.
- Çökmələr: xarici crash reporting yoxdur; Robolectric testlər əsas axınları örtür (37 fayl). Real cihaz/emülator yoxlaması mümkün deyil (bu mühitdə KVM yoxdur — bax §5).

---

## 3. MapLibre miqrasiya planı (P1)

**Mövcud vəziyyət**: layihədə MapLibre Compose 0.20.0 ARTIQ inteqre edilib (fallback backend kimi, D-048). "Miqrasiya" əslində **Google Maps ƏSAS → MapLibre ƏSAS** keçidi deməkdir (D-108 direktivini ləğv edən yeni direktiv).

Mərhələlər:
1. `MAP_BACKEND` default `google` → `maplibre` (build.gradle.kts:75-79) + `resolveMapBackend` məntiqi tərsinə
2. `GoogleMapCanvas` (286 sətir) paritet yoxlaması → silinmə (P1 cleanup)
3. Style: OpenFreeMap style URL konfiqurasiya olunan dəyərə (`BuildConfig.MAP_STYLE_URL`) — `MaplibreCanvas.kt`
4. Attribution: `© OpenStreetMap contributors` + OpenFreeMap link — MapLibre `MapView` attribution (default var, kompozisiyada görünür?) yoxlanacaq; `MapChrome`-a logo/attribution əlavə
5. `uses-feature android:glEsVersion="0x00030000"` (MapLibre v11+ OpenGL ES 3.0) — minSdk 24 uyğundur (API 18+ ES3 dəstəyi; cihaz coverage ~99.8%)
6. `play-services-maps` + `maps-compose` + `MAPS_API_KEY` placeholder silinir → APK kiçilir, imza SHA-1 asılılığı azalır
7. Feature parity cədvəli: pin-lər ✓, user dot+dairə ✓, kamera ✓, clustering (yox idi — əlavə olunmur), attribution YENİ əlavə

**Versiya seçimi**: MapLibre Compose 0.20.0 (artıq versiya kataloqunda; FFI nüvə 0.202610.0). Yeniləmə Maven Central-dan yoxlanacaq (https://central.sonatype.com/artifact/org.maplibre.gl/android-sdk/versions) — P1-də.

---

## 4. Açıq suallar (hər dövründə bir sual — növbəti dövr üçün)

**(a) Offline xəritə?** Hazırda yoxdur; P1-də yalnız online (OpenFreeMap) nəzərdə tutulur. Offline (MBTiles/PMTiles) ayrıca backlog-a düşür — təsdiq lazımdır.

**(b) Tile provayderi?** OpenFreeMap (açarsız, OSM əsaslı, MapLibre style hazır) təklif edilir. Alternativ: MapTiler (açar tələb edir). Self-host (Planetiler+Martin) server resursu tələb edir. **Təklifim: OpenFreeMap** — attribution `© OpenStreetMap contributors`.

**(c) Rəng sxemi?** Mövcud `core/theme/MapPalette.kt` tək sxemdir. Light/dark xəritə variantı lazımdırmı? (Təklifim: tək — app-ın öz dark aması var, xəritə stilində də tünd fon OpenFreeMap "dark" ilə mümkündür.)

**(d) Supabase test mühiti?** Ayrı staging YOXDUR. Mövcud praktika: canlı self-host serverdə `d181probe_*@qoshul.com` kimi müvəqqəti test hesabları (sonra silinir). Google test hesabı: sahibdən lazımdır (emülatorda SHA-1 + Web client ID qeydiyyatı tələb olunur — bu mühitdə mümkün deyil, "yoxlanılmadı" statusu ilə gedilir).

---

## 5. Mühit məhdudiyyətləri (dürüst status)

Bu agent mühitində:
- **Android SDK/emülator YOXDUR** (KVM yoxdur) → `adb emu geo fix` testi, real cihaz testi, Google Sign-In cihaz testi **mümkün deyil** — "yoxlanılmadı" statusu ilə qeyd olunur
- Kompilyasiya: CI (GitHub Actions) üzərində hər push-da `./gradlew assembleDebug + test` — **kompilyasiya olundu / unit test keçdi** statusları CI ilə sübut olunur
- Canlı Supabase (self-host) SQL sınaqları mümkündür (SSH) — DB səviyyəli verifikasiya edilir
- UI testlər Robolectric (JVM) — mövcud 37 fayl dəstəyi ilə

**Qadağa qəbulu**: "istifadəçi lokasiyası xəritədə (ən azı emülatorda, geo fix ilə) sübutla görünmədən push etmək olmaz" — bu mühitdə emülator olmadığından P0.1 dəyişiklikləri CI yaşıl + Robolectric paritet testləri ilə gedəcək və **hər push-da bu məhdudiyyət açıq qeyd olunacaq**. Real cihaz verifikasiyası sahib tərəfindən tələb olunur.

---

## 6. Növbəti addımlar (Loop 1 planı)

1. **P0.1**: `locateOnce`-ə accuracy + köhnəlik (elapsedRealtime) filtri; `Location.isMock` (API 31+, deprecated fallback); "Dəqiq mövqe axtarılır (±X m)" UI; GPS settings dialoqu (`LocationSettingsRequest` əvəzinə `LocationManager.isProviderEnabled` + `Settings.ACTION_LOCATION_SOURCE_SETTINGS` — Play Services asılılığı olmadan)
2. **P0.2**: `signInWithGoogle` əvvəli `signOut()` (köhnə session təmizliyi); `clearCredentialState`; e-poçt müqayisəsi (seçilən Google e-poçtu ↔ GoTrue user) + uyuşmazlıqda ləğv
3. **P0.3**: bütün auth çağırışlarına `withTimeout(15s)`; fetchGoogleConfig cache (TTL) — m4 round-trip azaldılır
4. **P1**: MAP_BACKEND → maplibre; attribution; Google Maps asılılığının silinməsi
5. Hər düzəliş: test + CI yaşıl + LOOP_LOG.md yenilənməsi
