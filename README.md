<div align="center">

# 🤝 Qoshul.az

**Azərbaycanın sosial platforması — profillər, canlı söhbət, yaxınlıqdakı insanlar, xəritə, tədbirlər və AI köməkçi.**

*“Qoşul” sözü “join” deməkdir. Hədəf bölgə: Azərbaycan · İnterfeys dili: Azərbaycan dili.*

[![Kotlin](https://img.shields.io/badge/Dil-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Release](https://img.shields.io/github/v/release/Abdullayews/Qoshul.az?style=for-the-badge&include_prereleases&label=Release)](https://github.com/Abdullayews/Qoshul.az/releases)
[![CI](https://img.shields.io/github/actions/workflow/status/Abdullayews/Qoshul.az/native.yml?branch=main&style=for-the-badge&label=CI)](https://github.com/Abdullayews/Qoshul.az/actions/workflows/native.yml)

</div>

---

## 🧭 Bu nədir?

**Qoshul.az** — Azərbaycan bazarı üçün native Android tətbiqi (**Kotlin + Jetpack
Compose**). Tətbiqində: istifadəçi profilləri, yaxınlıqdakı insanlarla tanışlıq
(Match), interaktiv xəritə, tədbirlər, obyektlər və partnyorlar, kuponlar,
rezervasiyalar (real skan edilən QR ilə), ikili XP iqtisadiyyatı, təhlükəsizlik
(qeydiyyat/blok) və AI köməkçi.

Bütün texniki qərarlar mənbəsi ilə birlikdə
[`docs/decisions.md`](docs/decisions.md)-də sənədlənib (D-001…D-141).

## ✨ Məhsul funksiyaları

| | |
|---|---|
| 👤 **Profillər** | Şəxsiyyət testi, maraqlar, şəkillər |
| 📍 **Match & xəritə** | Yaxınlıqdakı insanlar + canlı xəritə (OpenFreeMap) |
| 💬 **Söhbətlər** | Canlı söhbət axınları (backend fazasında real vaxt rejimi) |
| 🔔 **Bildirişlər** | Tədbir/XP/rezerv bildirişləri |
| 📅 **Tədbirlər** | Ana menyu, yaratma, iştirak, bitirmə (XP verir) |
| 🏟️ **Obyektlər & partnyorlar** | Məkanlar, partnyor endirimləri, biznes rejimi |
| 🎟️ **Kuponlar & rezervasiyalar** | QR kod ilə təsdiq (real, skan edilən QR) |
| ⚡ **İkili XP** | Peşəkarlıq XP + obyektə-məxsus Endirim XP |
| 🛡️ **Təhlükəsizlik** | Şikayət/blok, “nə olar əgər…” müdafiə qatı |
| 🤖 **AI köməkçi** | Tədbir təklifi edən köməkçi (backend fazasında proxy) |
| 🌐 **AZ-first** | İnterfeys tam Azərbaycan dilində |

## 🛠️ Texniki stack

| Qat | Texnologiya |
|---|---|
| Dil | **Kotlin 2.4.21** (AGP 9.5, Gradle 9.8.2) |
| UI | **Jetpack Compose** (BOM 2026.09, Material 3) |
| Arxitektura | Tək `:app` modulu, package-by-feature; saf-Kotlin `AppEngine` + `StateFlow` (manual DI) |
| Backend (M1) | **Supabase sxem + RLS + SECURITY DEFINER qaydalar** (`supabase/`) — supabase-kt 3.9.0 pin (D-078); MOCK/SUPABASE ikili rejim, PLACEHOLDER konfiq |
| Nüvə (M2) | **Rust** (`rust-core/`: qoshul-core + qoshul-jni) — geo 0.33.1, proptest 1.12, jni 0.22.4 (D-080); JNI APK-da, JVM-də host .so pariteti |
| Mikroservis (M4) | **axum 0.8.9** (`service/`): FCM HTTP v1, idempotent joblar (24s/2s), admin API — D-082; PLACEHOLDER konfiq → degrade |
| Deploy (M5) | **Self-host Supabase — Oracle Always Free** (Ampere A1 ARM64, 2 OCPU/12 GB — yeni limitlər D-083): slim compose core+6 profil, Caddy TLS, 12/12 imaj arm64 (D-084..D-087) |
| UI/UX (M6) | **Overlay pick sistemi → D-110 SELECTOR BOX** (etiketli seçim qutusu + vahid-sütun siyahı paneli: ikon + başlıq + izah, seçili vurğulu) + **Ghost-Downloader-3 nav inteqrasiyası** (D-088); vizual QA: Robolectric render + vizual audit + piksel (D-091); **adaptiv naviqasiya** — ≥600dp-də LiquidRail (D-102); nav lentz kapsulu ləğvi (D-103); status/nav bar görünüşü işıq kimi sabit (D-104); **iOS 26 üslublu üzən nav — FloatingTabBar** (vendored Apache-2.0, D-105/106); **əsl liquid glass** — Echo-Music recipesi (D-107); **5-slot nav** — Ana menyu · Xəritə · Yarat · Profil · Söhbətlər yan-yana, yığılmada Ana menyu sol / Söhbətlər sağ dairələr, digərləri aşağı yox olur (D-111); **sürətli tab keçidi** — fade 140/220ms (D-113); xəritə/söhbət tablarında baş bar gizli, AI FAB nav bar üstündə sağda, yalnız Tədbirlərdə (D-115/D-111) · **xəritə xromu təmizliyi** — SDK loqo/atribusiya/miqyas/compass overlay-ləri gizli, qara zolaq Texture rejimi ilə təmir, „düzləşdir“ həbi zoom altında (D-117) · **dizayn sistemi** — Q snapshot tokenlər, #008B2D əsas, Yaşıl/Qızılı × Ağ/Qara (D-118/D-119) · **haptik + pressScale + indication=null** (D-121) · **2 fazalı (sıralı) tab keçid animasiyası** (D-122) |
| Xəritə | **Google Maps SDK** (maps-compose 9.0.1 — ƏSAS backend, D-109; `MAPS_API_KEY` ilə) — açarsızda **MapLibre + OpenFreeMap** avtomatik fallback (D-048) |
| Naviqasiya barı | **FloatingTabBar** (iOS 26, D-105/106) + **əsl liquid glass** — Kyant0/backdrop, Echo-Music recipesi (D-107; API 31+); **5 slot yan-yana** — Ana menyu · Xəritə · Yarat · Profil · Söhbətlər; yığılmada iki dairə kənarlarda (D-111) |
| QR (M3) | **Rust qrcodegen 1.8.0** (Nayuki rəsmi Rust portu) JNI + quircs dekod sübutu (D-081) |
| Test | **277 JVM (0 fail, 13 Rust-paritet skip — host .so lokalda) + 35 Rust** — məntiq + Robolectric UI + overlay pick (PickerOverlayTest, D-089) + sync güzgü + Rust pariteti + quircs QR sübutu (D-080/D-081) + şrift audit (D-100) + glass recipe/backend seam + keçid (D-107/D-108/D-109) + dizayn sistemi (DesignSettingsTest, D-118/D-119) + WCAG kontrast sübutu (PaletteContrastTest, D-123) + 79 AZ şəhər A-Z (AzCitiesTest, D-126) + AI FAB görünüşü (AiFabVisibilityTest, D-130) + biznes/ayar axını (PartnersBiznesRemovedTest, D-131) + tab qorunması (SettingsBackTabTest, D-127) + direktiv auditi (DirectivesAuditTest, D-126..D-134) + klaviatura UX auditi (KeyboardAuditTest, D-136..D-139) |
| APK | minSdk 24 · targetSdk 36 · R8 full + resource shrink · baseline profile · **Open Sans** şrifti (noto-set build + AZ qlifləri, subset 5 çəki — D-100) · adaptiv nav (D-102) · təmiz nav + sabit işıq status bar (D-103/D-104) · iOS 26 FloatingNav + əsl liquid glass + 5-slot (D-105/106/107/111) · Google Maps əsas backend (D-109) · **dinamik palitra — Yaşıl #008B2D / Qızılı × Ağ/Qara mod** (D-118/D-119) · selector box (D-110) · profil redaktə ekranı (D-112) · kalendar/saat pickers (D-114) · **istifadəçi ikonu (jxl → adaptive + legacy API 24/25)** (D-120) · xəritə overlay təmizliyi + Texture rejimi + düzləşdirmə həbi (D-117) · haptik + pressScale + indication=null (D-121) · 2 fazalı tab keçidi (D-122) · **klaviatura UX — düzgün KeyboardType-lar, ImeAction zəncirləri, Enter ilə göndərmə, 48dp save hədəfi** (D-136..D-139) |
| CI | **GitHub Actions** — `native.yml`: SQL lint + testlər + release APK artifaktı |

## 📁 Depo strukturu

```
Qoshul.az/
├── android-app/            Native tətbiq (Kotlin + Compose)
│   ├── app/src/main/       core/ (backend · data · engine · glass · model ·
│   │                       motion · safety · theme · ui) + features/ (assistant · auth ·
│   │                       chats · coupons · events · feed · map · match ·
│   │                       notifications · onboarding · partners · profile ·
│   │                       reservations · safety · settings · shell · venues · xp)
│   └── app/src/test/       277 JVM testi (Robolectric daxil)
├── supabase/               M1: sxem + RLS + server qaydaları (D-076/D-077)
├── rust-core/              M2-M3: qoshul-core (qaydalar+QR) + qoshul-jni (JNI) + qoshul-fonts (audit)
├── service/                M4: axum mikroservis — FCM, joblar, admin API (D-082)
├── infra/selfhost/         M5: self-host Supabase (Oracle Always Free) — slim compose + envoy + caddy
├── scripts/                lint_sql.py · make_release.py · migrate_selfhost.sh · validate_selfhost.sh
├── docs/                   decisions.md · product-spec.md · design-spec.md
└── .github/workflows/      native.yml (Rust fmt/clippy/test + jniLibs + SQL lint + test + APK)
```

## ⚙️ Tətbiq: backend rejimləri (D-050)

`android-app/local.properties` (repoya düşmür) vasitəsilə iki rejim:

| `BACKEND_MODE` | Nə deməkdir |
|---|---|
| `MOCK` *(default)* | Bütün funksiyalar demo məlumatla dolu; vahid demo hesabı ilə bir toxunuşla giriş |
| `SUPABASE` | Real Supabase auth + sync (`SUPABASE_URL` / `SUPABASE_ANON_KEY` da local.properties-də) |

## 🗺️ Tətbiq: xəritə backend-i (D-108/D-109)

**Google Maps ƏSAS backenddir** (direktiv: „google ilə dəyişdirib“). Açar
(`MAPS_API_KEY=AIza…` — Google Cloud Console → Maps SDK for Android)
təqdim olunan kimi Google xəritəsi işə düşür; açar yoxdursa xəritə boş
qalmasın deyə MapLibre avtomatik fallback seçilir:

| `MAP_BACKEND` | Nə deməkdir |
|---|---|
| `google` *(default)* | Google Maps SDK (maps-compose 9.0.1) — `MAPS_API_KEY` **tələb olunur**; açar yoxdursa maplibre fallback (resolveMapBackend) |
| `maplibre` | MapLibre + OpenFreeMap — açarsız, pulsuz (D-048); açıq seçim və ya açarsız fallback |

Açarı iki yerdən vermək olar: `android-app/local.properties` (lokal build)
və ya **repo secret `MAPS_API_KEY`** (CI — native.yml ötürür; secret qoyulanda
release APK-ları Google xəritə ilə qurulur). Keçid üçün kod dəyişikliyi
lazım DEYİL — pinlər, filtrlər, alt kart, zoom həbləri eynidir; yalnız tile
səthi dəyişir.

## 🛠️ Tətbiq: mənbədən qurma (installation)

Tələblər: **JDK 21** (Temurin) · **Android SDK** (API 36) · **Rust stable**
(qoshul-jni JNI körpüsü üçün) · Linux/macOS (NDK cross-build).

```bash
git clone https://github.com/Abdullayews/Qoshul.az.git && cd Qoshul.az

# 1) Rust nüvəsi — host .so (JVM paritet testləri) + Android jniLibs
cd rust-core
cargo build --release -p qoshul-jni                        # host (x86_64)
NDK=<android-ndk-dir>                                      # arm64-v8a + armeabi-v7a:
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang"
export CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi24-clang"
cargo build --release --target aarch64-linux-android -p qoshul-jni
cargo build --release --target armv7-linux-androideabi -p qoshul-jni
mkdir -p ../android-app/app/src/main/jniLibs/arm64-v8a ../android-app/app/src/main/jniLibs/armeabi-v7a
cp target/aarch64-linux-android/release/libqoshul_jni.so ../android-app/app/src/main/jniLibs/arm64-v8a/
cp target/armv7-linux-androideabi/release/libqoshul_jni.so ../android-app/app/src/main/jniLibs/armeabi-v7a/

# 2) Android tətbiqi (MOCK rejimi — heç bir açar tələb olunmur)
cd ../android-app
./gradlew testDebugUnitTest        # 277 JVM testi
./gradlew assembleRelease          # app-release.apk (R8 + resource shrink)
```

MOCK rejimi (default) heç bir konfiqurasiya olmadan qurulur — `local.properties`
yalnız SUPABASE rejimi (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `BACKEND_MODE`)
və ya Google xəritə (`MAPS_API_KEY`, `MAP_BACKEND`) istəyəndə lazımdır.

## 🖥️ Backend: quraşdırma, konfiqurasiya, işə salma

Backend **iki hissədən** ibarətdir (mandat: „backend = tətbiqin istifadə
etdiyi server hissəsi“):

### 1) Supabase — data qatı (M1)

**Quraşdırma (self-host, D-083..D-087):** tam bələdçi
[`infra/selfhost/README.md`](infra/selfhost/README.md)-də (Oracle Always Free
Ampere A1, slim compose core+6 profil, Caddy TLS). Qısa yol:

```bash
cd infra/selfhost
cp .env.example .env && sh generate-keys.sh --update-env   # sirlər avtomatik
# .env-də 🔶 domen dəyərlərini doldur (SUPABASE_PUBLIC_URL, SITE_URL=https://qoshul.az)
docker compose -f docker-compose.yml --profile core up -d
cd ../.. && scripts/migrate_selfhost.sh                    # migrations (idempotent)
```

**Miqralasiyalar:** `supabase/migrations/` — versiyalı fayllar
(`0001_schema.sql` → `0002_server_rules.sql` → `0003_seed.sql`); mövcud
fayllar redaktə olunmur, yeni dəyişiklik yeni faylla gəlir. `scripts/lint_sql.py`
struktur lint-i aparır (CI-də məcburidir).

### 2) qoshul-service — axum mikroservisi (M4, D-082)

**Quraşdırma:** Rust stable tələb olunur.

```bash
cd service
cp .env.example .env      # PLACEHOLDER dəyərləri doldur (aşağıda)
cargo build --release
cargo test                # 14 test: 6 lib + 8 inteqrasiya (JWT openssl sübutu)
```

**Konfiqurasiya** (`service/.env` — nümunə: `service/.env.example`, D-079
PLACEHOLDER prinsipi):

| Env | Default | Təsvir |
|---|---|---|
| `PORT` | `8080` | HTTP port (0.0.0.0) |
| `SUPABASE_URL` | PLACEHOLDER | Self-host Supabase URL-i |
| `SUPABASE_SERVICE_ROLE_KEY` | PLACEHOLDER | service_role açarı — yalnız mikroservisdə, tətbiqə heç vaxt düşmür |
| `FCM_SERVICE_ACCOUNT_JSON` | PLACEHOLDER | FCM service-account JSON — push bildirişlər |
| `JOBS_INTERVAL_SECS` | `60` | Job intervalı (hər tur idempotent) |
| `ADMIN_TOKEN` | PLACEHOLDER | Admin panel `X-Admin-Token` — PLACEHOLDER → panel qapalı |

PLACEHOLDER halında service **deqrad işləyir** (crash yox): `/health` və admin
API sxematik cavab verir, FCM söndürülür, Supabase əməliyyatları xəta qaytarır.

**İşə salma:**

```bash
cd service && cargo run --release
curl http://127.0.0.1:8080/health    # {"ok":true,"service":"qoshul-m4",...}
```

**API icmalı** (tam siyahı: `service/src/lib.rs`):

| Metod | Yol | Təsvir |
|---|---|---|
| GET | `/health` | Sağlamlıq + konfiq statusu (versiya, supabase/fcm/admin) |
| POST | `/jobs/reminders/run` | Yaddaş xatırladıcıları — idempotent (DB sütunları) |
| POST | `/jobs/moderation/run` | Moderasiya jobu — idempotent |
| GET | `/admin/reports?status=` | Şikayət siyahısı (`X-Admin-Token` lazımdır) |
| POST | `/admin/reports/{id}` | Şikayət həlli `{action: RESOLVED\|DISMISSED}` |
| POST | `/admin/profiles/{id}` | Profil məhdudlaşdırma `{days, reason}` / `{unrestrict: true}` |
| POST | `/admin/reviews/{id}` | Rəy moderasiyası `{approve: bool}` |
| GET | `/admin/actions` | Admin əməliyyat jurnalı |

Tətbiq bu servisə **birbaşa müraciət etmir** (D-082): tətbiq Supabase-ə
(PostgREST RPC + auth) qoşulur; mikroservis FCM push, joblar və admin paneli
üçündür. Yeni endpoint əlavəsi backward-compatible sayılır; endpoint silinməsi
və ya sahə tipinin dəyişməsi **breaking change**-dir — yalnız API versiyası
artırılaraq edilir və köhnə tətbiq versiyalarının davranışı sənədlənir.

### 3) Tətbiq ↔ backend uyğunluq cədvəli

| App versiyası | Backend komponenti | Versiya | Qeyd |
|---|---|---|---|
| v0.27.0-beta+ | Supabase sxem + RLS | `supabase/migrations` (0001..0003) | Sxem təqdim olundu (D-076/D-077) |
| v0.28.0-beta+ | Supabase sync (14 RPC) | 0002_server_rules | `SupabaseSync.kt` — bütün RPC-lər `fn_*` server qaydalarıdır |
| v0.29.0-beta+ | qoshul-service (axum) | 0.20.0 | Tətbiqə birbaşa təsiri YOX (admin/FCM/jobs) — D-082 |
| v0.45.0-beta (cari) | hamısı | — | 14/14 RPC uyğundur; kontrakt dəyişməyib |

**Kontrakt qaydası:** tətbiq `SupabaseSync.kt`-də çağırdığı hər RPC üçün
`supabase/migrations/`-da `fn_*` funksiyası tələb edir; ən köhnə dəstəklənən
tətbiq versiyası v0.27.0-beta-dır (sxem 0001-dən bəri RLS qaydaları stabil,
breaking dəyişiklik olmayıb).

## 📲 Quraşdırma (APK)

APK yalnız **GitHub Releases**-də saxlanılır — [Releases səhifəsi](https://github.com/Abdullayews/Qoshul.az/releases)
və ya saytdakı QR: [qoshul.az](https://qoshul.az) (endirmələr birbaşa
GitHub-dan gəlir).

1. Ən son buraxılışdan **`Qoshul.az-v… apk`** faylını endirin
2. Android **7.0+** (minSdk 24), arm64-v8a və ya armeabi-v7a
3. “Naməlum mənbələrdən quraşdırma” icazəsi verin və açın
4. Hər release səhifəsində APK ölçüsü + SHA-256 göstərilir

Beta imzası ilə imzalanıb — birbaşa quraşdırma üçün (D-016: keystore
mərhələsi Play Store-a keçiddə gələcək).

## 📚 Sənədlər

- [`docs/decisions.md`](docs/decisions.md) — hər texniki qərar + mənbəsi (D-001…D-141)
- [`infra/selfhost/README.md`](infra/selfhost/README.md) — **deploy: self-host Supabase Oracle Always Free VM-də** (M5)
- [`docs/product-spec.md`](docs/product-spec.md) — məhsul spektri: rollar, axınlar, XP/kupon/rezerv qaydaları
- [`docs/design-spec.md`](docs/design-spec.md) — Liquid Glass minimal dizayn dili + hərəkət dili

## 📄 Lisenziya

Şəxsi startap layihəsi — açıq şəkildə, mərhələ-mərhələ qurulur.
