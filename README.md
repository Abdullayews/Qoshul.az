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
| 📅 **Tədbirlər** | Kəşf, yaratma, iştirak, bitirmə (XP verir) |
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
| UI/UX (M6) | **Overlay pick sistemi** (mini-kateqoriya trigger → üzərinə çıxan panel, D-089) + **Ghost-Downloader-3 nav inteqrasiyası** (D-088) + boz track ləğvi (D-090); vizual QA: Robolectric render + vizual audit + piksel (D-091) |
| Xəritə | **MapLibre Compose 0.20 + OpenFreeMap** — açarsız, pulsuz (D-048) |
| QR (M3) | **Rust qrcodegen 1.8.0** (Nayuki rəsmi Rust portu) JNI + quircs dekod sübutu (D-081) |
| Test | **191 JVM (0 fail, 13 Rust-paritet skip — host .so lokalda) + 35 Rust** — məntiq + Robolectric UI + overlay pick (PickerOverlayTest, D-089) + sync güzgü + Rust pariteti + quircs QR sübutu (D-080/D-081) + şrift audit (D-100) |
| APK | minSdk 24 · targetSdk 36 · R8 full + resource shrink · baseline profile · **Open Sans** şrifti (noto-set build + AZ qlifləri, subset 5 çəki — D-100) |
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
│   └── app/src/test/       174 JVM testi (Robolectric daxil)
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
