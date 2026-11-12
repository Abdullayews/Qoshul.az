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
[`docs/decisions.md`](docs/decisions.md)-də sənədlənib (D-001…D-097).

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

## ⚙️ Backend rejimləri (D-050)

`android-app/local.properties` (repoya düşmür) vasitəsilə iki rejim:

| `BACKEND_MODE` | Nə deməkdir |
|---|---|
| `MOCK` *(default)* | Bütün funksiyalar demo məlumatla dolu; vahid demo hesabı ilə bir toxunuşla giriş |
| `SUPABASE` | Real Supabase auth + sync (`SUPABASE_URL` / `SUPABASE_ANON_KEY` da local.properties-də) |

## 🗺️ Xəritə backend-i (D-108/D-109)

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

- [`docs/decisions.md`](docs/decisions.md) — hər texniki qərar + mənbəsi (D-001…D-097)
- [`infra/selfhost/README.md`](infra/selfhost/README.md) — **deploy: self-host Supabase Oracle Always Free VM-də** (M5)
- [`docs/product-spec.md`](docs/product-spec.md) — məhsul spektri: rollar, axınlar, XP/kupon/rezerv qaydaları
- [`docs/design-spec.md`](docs/design-spec.md) — Liquid Glass minimal dizayn dili + hərəkət dili

## 📄 Lisenziya

Şəxsi startap layihəsi — açıq şəkildə, mərhələ-mərhələ qurulur.
