# P4 Performans Baseline — APK Ölçüsü və Başlanğıc (D-186, Loop 5)

> Tarix: 2026-11-28. Ölçüm basisi: v0.62.0 release APK (32 023 089 bayt,
> 30.5 MB). Məqsəd: P4 optimizasiya dövrü üçün ölçülü baseline qeyd
> etmək — gələcək dəyişikliklərin təsirini müqayisə etmək üçün tək
> nöqtə. Metod: APK unzip + `du` bölmə təhlili (alət: plain bash;
> APK Analyzer CI mühitində yoxdur).

## 1. APK tərkibi (v0.62.0, 30.5 MB)

| Bölmə | Ölçü | Pay | Qeyd |
|---|---|---|---|
| `lib/` (native) | 25.1 MB | 82% | 2 ABI: arm64-v8a 15.0 MB + armeabi-v7a 10.2 MB |
| ├ `libmaplibre-native-c.so` | 13.1 MB (arm64) | — | vektor xəritə nüvəsi (D-048) — ən böyük tək fayl |
| ├ `libjniMaplibreNativeC.so` | 1.0 MB | — | JNI körpü |
| ├ `libqoshul_jni.so` | 0.8 MB | — | bizim Rust nüvə (match/skor) |
| `classes.dex` | 9.3 MB | — | R8 minify AÇIQ (release) |
| `classes2.dex` | 0.25 MB | — | |
| `res/` | 1.6 MB | — | 5 TTF (OpenSans — 824 KB), webp/png ikonlar |
| `assets/composeResources/` | 1.06 MB | — | **maplibre-compose 126 dil qovluğu** (hərəsi ~4 KB) |
| `resources.arsc` | 0.42 MB | — | |
| `META-INF` + qalan | 0.8 MB | — | imza + manifest |

## 2. Tapıntılar və qiymətləndirmə

### F1 — maplibre lokal stringləri (İCRA OLUNDU, D-186)
`maplibre-compose` öz composeResources assetində 126 dilin
`strings.commonMain.cvr` faylını gətirir (compass, attribution, km/mi
simvolları — hər dil ~4 KB, cəm ~1 MB). Tətbiq azərbaycan dilindədir,
xəritə etiketləri latın simvollarıdır. **Həll**: `packaging.resources.excludes`
**glob** ilə bütün `values-*` qovluqları çıxarılır — AGP excludes regex
qəbul etmir (birinci cəhd lookahead regex idi, CI-də „Explicit name
separator" xətası verdi); glob-da inkar ifadəsi olmadığından az/tr/ru
da gedir — onlar yalnız kompas/attribution/km-mi etiketləridir,
ana `values/` (default en) qalır, istifadəçi üçün görünməz fərq.
`androidResources.localeFilter` bura aid deyil — o `res/` üçündür,
bu assetdir. Qənaət ~1 MB (126→1 qovluq).

### F2 — native .so 25 MB (struktur, dəyişmədi)
Əsas kütlə maplibre-native (13.1 MB/ABI). Bu, D-048 qərarının bilinən
qiymətidir: vektor xəritə + OpenGL runtime. Alternativlər (Mapbox
kommersiya, Google Maps SDK — Play Services asılılığı) rədd edilib.
`armeabi-v7a` (10.2 MB) köhnə 32-bit cihazlar üçün saxlanılır —
Azərbaycan bazarında hələ payı var. Play-də app bundle per-device
çatdırır (bu APK birbaşa paylanma üçün universal build).
**Qərar**: dəyişdirilmir — funksional zərurətdir.

### F3 — dex 9.3 MB (R8 açıqdır, dəyişmədi)
`isMinifyEnabled = true` + `isShrinkResources = true` (release).
Keep-qaydaları (supabase/ktor full-keep) konservativdir, amma R8-nin
şikāstəsi runtime reflection xətasıdır — ağır test örtüyü olmadan
azaltmaq riskli. **Qərar**: dəyişdirilmir; gələcəkdə `-checkdiscard`
ilə ölçülü azaltma araşdırıla bilər.

### F4 — başlanğıc zənciri (təmiz, dəyişmədi)
- `QoshulApplication.onCreate` (35 sətir): instance + MapLogGate
  install (rate-limit log filter) + Safety sink — ağır iş YOX
- DI: manual AppContainer (D-036; Hilt rədd — kapt/ksp build vaxtı)
- `AppEngine.init`: yalnız `dataInvariants()` — boş ServerDataSource
  üzərində O(1); ağır data `refreshFromServer` ilə asinxron gəlir
- `MainActivity`: edge-to-edge + setContent — standart Compose başlanğıcı
- profileinstaller 1.4.1 pin (D-043) — API 24-27 profile install
- **Qərar**: başlanğıcda optimizasiya lazımı deyil; real ölçüm
  (macrobenchmark) yalnız real cihazda mümkündür — sahib testi gözlənilir

### F5 — res/ fontlar 824 KB (dəyişmədi)
5 OpenSans çəki (regular→extrabold). Subsetting (qoshul-fonts aləti
hazırdır — `rust-core/qoshul-fonts`) potensial ~40% qənaət verərdi,
amma qlif auditindən keçən bütün simvollar istifadə olunur
(122 unikal); TTF subsetting qlif kəsintisi riski daşıyır.
**Qərar**: dəyişmədi — 824 KB qəbul olunan qiymətdir.

## 3. Nəticə (D-186)

| Göstərici | v0.62.0 | v0.63.0 (gözlənilən) |
|---|---|---|
| APK ölçüsü | 30.5 MB | ~29.5 MB (F1: −1 MB) |
| Lokal qovluq | 126 | 1 (yalnız ana values/, default en) |
| Başlanğıc | — | dəyişməz (F4: təmiz zəncir) |

Qənaət kiçikdir (~3%), amma sıfır riskli və düzgün praktikadır:
istifadəlməyən lokalizasiya APK-da daşınmamalıdır (birinci cəhd regex
lookahead idi — AGP „Explicit name separator" glob xətası verdi, CI-də
tutuldu və tam values-* exclude-a keçildi; dərs LOOP_LOG Loop 5-də).
Əsl optimizasiya sahəsi F2 (native lib) Play app bundle ilə həll
olunacaq — birbaşa APK paylanması saxlanıldığı müddətdə 25 MB lib/
qiyməti sabitdir.

## 4. Ölçüm təkrarı (gələcək looplar üçün)

```bash
unzip -q app-release.apk -d /tmp/apk && du -k /tmp/apk/* | sort -rn | head
# locale qənaəti yoxlaması:
ls /tmp/apk/assets/composeResources/org.maplibre.compose.generated/ | wc -l
# → 1 olmalıdır (yalnız values/, default en)
```
