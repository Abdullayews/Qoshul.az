<div align="center">

# 🤝 Qoshul.az

**Azərbaycanın sosial platforması — profillər, canlı söhbət, yaxınlıqdakı insanlar, xəritə, tədbirlər və AI köməkçi.**

*“Qoşul” sözü “join” deməkdir. Hədəf bölgə: Azərbaycan · İnterfeys dili: Azərbaycan dili.*

[![Kotlin](https://img.shields.io/badge/Dil-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
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
[`docs/decisions.md`](docs/decisions.md)-də sənədlənib (D-001…D-075).

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
| Xəritə | **MapLibre Compose 0.20 + OpenFreeMap** — açarsız, pulsuz (D-048) |
| QR | **Nayuki QR-Code-generator v1.8.0** (vendored, MIT) + ZXing sübut testləri (D-041) |
| Test | **165 JVM testi** — məntiq + QR sübutu + Robolectric UI + sync güzgü (D-042; M0: D-075, M1: D-078) |
| APK | minSdk 24 · targetSdk 36 · R8 full + resource shrink · baseline profile · font subsetting |
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
│   └── app/src/test/       165 JVM testi (Robolectric daxil)
├── supabase/               M1: sxem + RLS + server qaydaları (D-076/D-077)
├── scripts/                lint_sql.py (SQL struktur lint-i)
├── docs/                   decisions.md · product-spec.md · design-spec.md · migration-research.md
└── .github/workflows/      native.yml (SQL lint + test + APK)
```

## 🌿 İkili versiya (D-050)

| Branch | Rejim | Nə deməkdir |
|---|---|---|
| `main` | **PLACEHOLDER** | Bütün funksiyalar demo məlumatla dolu; vahid demo hesabı ilə bir toxunuşla giriş |
| `server-only` | **SERVER_ONLY** | Server qoşulana qədər heç bir məlumat yoxdur; yalnız real server auth (`az.qoshul.app.server`) |

## 📲 Quraşdırma

APK yalnız **GitHub Releases**-də saxlanılır — [Releases səhifəsi](https://github.com/Abdullayews/Qoshul.az/releases)
və ya saytdakı QR: [qoshul.az](https://qoshul.az) (endirmələr birbaşa
GitHub-dan gəlir). Beta imzası ilə imzalanıb (D-016).

## 📚 Sənədlər

- [`docs/decisions.md`](docs/decisions.md) — hər texniki qərar + mənbəsi (D-001…D-075)
- [`docs/product-spec.md`](docs/product-spec.md) — məhsul spektri: rollar, axınlar, XP/kupon/rezerv qaydaları
- [`docs/design-spec.md`](docs/design-spec.md) — Liquid Glass minimal dizayn dili + hərəkət dili

## 🗃️ Tarixçə qeydi

Bu depo əvvəl Flutter tətbiqi (`mobile/`) + backend/infra skeletini ehtiva edirdi.
Native yenidən yazma (D-036) pariteti təsdiqləndikdən sonra köhnə qalıqlar
silindi (D-065, 2026-11-07) — tarixə `git log` ilə geri qayıtmaq olar.

## 📄 Lisenziya

Şəxsi startap layihəsi — açıq şəkildə, mərhələ-mərhələ qurulur.
