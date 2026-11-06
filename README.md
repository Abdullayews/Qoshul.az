<div align="center">

# 🤝 Qoshul.az — `server-only` branch

**Azərbaycanın sosial platforması — profillər, canlı söhbət, yaxınlıqdakı insanlar, xəritə, tədbirlər və AI köməkçi.**

⚠️ **Bu branch SERVER_ONLY rejimidir** — server qoşulana qədər heç bir məlumat yoxdur.

*“Qoşul” sözü “join” deməkdir. Hədəf bölgə: Azərbaycan · İnterfeys dili: Azərbaycan dili.*

[![Kotlin](https://img.shields.io/badge/Dil-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![CI](https://img.shields.io/github/actions/workflow/status/Abdullayews/Qoshul.az/native.yml?branch=server-only&style=for-the-badge&label=CI)](https://github.com/Abdullayews/Qoshul.az/actions/workflows/native.yml)

</div>

---

## 🧭 Bu branch nədir?

**SERVER_ONLY rejimi** (D-050/D-052): tətbiq **heç bir demo/fake məlumat
ehtiva etmir** — bütün siyahılar boşdur, giriş yalnız real server
autentifikasiyası ilə (`POST /auth/login`, OkHttp `ApiClient`). Paket adı
`az.qoshul.app.server` (main branch-dakı demo APK ilə paralel quraşdırma),
adı **“Qoshul Server”**.

Demo məlumatlı tam versiya **`main`** branch-dadır.

## ✨ Məhsul funksiyaları (server qoşulanda aktivləşəcək)

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
| 🛡️ **Təhlükəsizlik** | Şikayət/blok müdafiə qatı |
| 🤖 **AI köməkçi** | Tədbir təklifi edən köməkçi (backend fazasında proxy) |
| 🌐 **AZ-first** | İnterfeys tam Azərbaycan dilində |

## 🛠️ Texniki stack

| Qat | Texnologiya |
|---|---|
| Dil | **Kotlin** (AGP 9.5, Gradle 9.8.2) |
| UI | **Jetpack Compose** (Material 3) |
| Arxitektura | Tək `:app` modulu; saf-Kotlin `AppEngine` + `StateFlow`; `QoshulDataSource` kontraktı — bu branchda `ServerQoshulDataSource` (boş) + OkHttp `ApiClient` |
| Xəritə | **MapLibre Compose + OpenFreeMap** — açarsız, pulsuz (D-048) |
| QR | **Nayuki QR-Code-generator v1.8.0** (vendored, MIT) + ZXing sübut testləri (D-041) |
| Test | **87 JVM testi** — məntiq + QR sübutu + Robolectric UI + ServerMode |
| CI | **GitHub Actions** — `native.yml` (bu branchda da işləyir) |

## 🌿 İkili versiya (D-050)

| Branch | Rejim | Nə deməkdir |
|---|---|---|
| `main` | **PLACEHOLDER** | Bütün funksiyalar demo məlumatla dolu; vahid demo hesabı ilə bir toxunuşla giriş |
| `server-only` ◀ | **SERVER_ONLY** | Server qoşulana qədər heç bir məlumat yoxdur; yalnız real server auth |

## 📚 Sənədlər

- [`docs/decisions.md`](docs/decisions.md) — hər texniki qərar + mənbəsi
- [`docs/product-spec.md`](docs/product-spec.md) — məhsul spektri: rollar, axınlar, XP/kupon/rezerv qaydaları
- [`docs/design-spec.md`](docs/design-spec.md) — Liquid Glass minimal dizayn dili + hərəkət dili

## 🗃️ Tarixçə qeydi

Bu depo əvvəl Flutter tətbiqi (`mobile/`) + backend/infra skeletini ehtiva edirdi.
Native yenidən yazma (D-036) pariteti təsdiqləndikdən sonra köhnə qalıqlar
silindi (D-065, 2026-11-07) — tarixə `git log` ilə geri qayıtmaq olar.

## 📄 Lisenziya

Şəxsi startap layihəsi — açıq şəkildə, mərhələ-mərhələ qurulur.
