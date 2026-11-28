# P4 Performans Baseline — APK Ölçüsü və Başlanğıc (D-186, Loop 5)

> Tarix: 2026-11-28 (son yeniləmə: D-186 düzəlişi, Loop 5 içində).
> Ölçüm basisi: v0.62.0/v0.63.0 release APK (32 023 089 bayt, 30.5 MB —
> ekvivalentdir). Məqsəd: P4 optimizasiya dövrü üçün ölçülü baseline
> qeyd etmək — gələcək dəyişikliklərin təsirini müqayisə etmək üçün tək
> nöqtə. Metod: APK unzip + ZIP entry ölçüləri (`zipfile` — sıxılmış
> bayt; `du` blok ölçüsü DEYİL, çünki kiçik faylları 8 KB-a yuvarlaqlaşdırır).

## 1. APK tərkibi (v0.62.0 = v0.63.0, 30.5 MB)

| Bölmə | Ölçü | Pay | Qeyd |
|---|---|---|---|
| `lib/` (native) | 25.1 MB | 82% | 2 ABI: arm64-v8a 15.0 MB + armeabi-v7a 10.2 MB |
| ├ `libmaplibre-native-c.so` | 13.1 MB (arm64) | — | vektor xəritə nüvəsi (D-048) — ən böyük tək fayl |
| ├ `libjniMaplibreNativeC.so` | 1.0 MB | — | JNI körpü |
| ├ `libqoshul_jni.so` | 0.8 MB | — | bizim Rust nüvə (match/skor) |
| `classes.dex` | 9.3 MB | — | R8 minify AÇIQ (release) |
| `classes2.dex` | 0.25 MB | — | |
| `res/` | 1.6 MB | — | 5 TTF (OpenSans — 824 KB), webp/png ikonlar |
| `assets/composeResources/` | 1.06 MB (du) / **7.3 KB (ZIP real)** | 0.02% | maplibre 126 dil qovluğu — aşağıda F1 |
| `resources.arsc` | 0.42 MB | — | |
| `META-INF` + qalan | 0.8 MB | — | imza + manifest |

## 2. Tapıntılar və qiymətləndirmə

### F1 — maplibre lokal stringləri (CEHD → ÖLÇÜM → REVERT, D-186 dərsi)
`maplibre-compose` composeResources assetində 126 dilin
`strings.commonMain.cvr` faylını gətirir (kompas/attribution/km-mi
etiketləri). İlk təxmin `du -k` ölçüsünə əsasən ~1 MB idi. İki cəhd
edildi və hər ikisi qiymətsiz sübut oldu:

1. **Regex exclude** (`packaging.resources.excludes`): AGP yalnız GLOB
   qəbul edir — „Explicit name separator" xətası, CI qırmızı.
2. **Glob exclude** (`values-*/**`): build keçdi, amma v0.63.0 APK-da
   128 qovluq qaldı — **excludes java resurslarına aiddir, AAR
   assetlərinə YOX** (aapt2 mergeAssets axını ayrıdır).

**Kritik ölçüm**: ZIP entry-lərin real sıxılmış ölçüsü — 126 fayl ×
67 bayt = **7.3 KB (APK-nın 0.02%-i)**. `du` 8 KB blok yuvarlaqlaşdırması
126 faylı „1 MB" göstərirdi — yanlış premiss. Nəticə: **REVERT edildi**
(v0.64.0). Əgər gələcəkdə bu qənaət lazım olsaydı, düzgün alət AGP
Variant API `MultipleArtifact.MERGED_ASSETS` transformu olardı (rəsmi
resept: gradle-recipes/transformMultiple), amma 7 KB üçün build
məntiqəsi əlavə etmək mühəndislik baxımından sərfəli deyil.
**Dərs**: fayl sayı çox olan kiçik fayllarda həmişə ZIP compress_size
ölç, du blok ölçüsünü yox.

### F2 — native .so 25 MB (struktur, dəyişmədi)
Əsas kütlə maplibre-native (13.1 MB/ABI). Bu, D-048 qərarının bilinən
qiymətidir: vektor xəritə + OpenGL runtime. Alternativlər (Mapbox
kommersiya, Google Maps SDK — Play Services asılılığı) rədd edilib.
`armeabi-v7a` (10.2 MB) köhnə 32-bit cihazlar üçün saxlanılır —
Azərbaycan bazarında hələ payı var. Play-də app bundle per-device
çatdırır (bu APK birbaşa paylanma üçün universal build).
**Qərar**: dəyişdirilmir — funksional zərurətdir. Əsl optimizasiya
yolu Play-ə keçiddir (per-device .so).

### F3 — dex 9.3 MB (R8 açıqdır, dəyişmədi)
`isMinifyEnabled = true` + `isShrinkResources = true` (release).
Keep-qaydaları (supabase/ktor full-keep) konservativdir, amma R8-nin
şikəstəsi runtime reflection xətasıdır — ağır test örtüyü olmadan
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
hazırdır — `rust-core/qoshul-fonts`) potensial qənaət verərdi, amma
qlif auditindən keçən bütün simvollar istifadə olunur (122 unikal);
TTF subsetting qlif kəsintisi riski daşıyır. **Qərar**: dəyişmədi —
824 KB qəbul olunan qiymətdir.

## 3. Nəticə (D-186, düzəldilmiş)

| Göstərici | v0.62.0 | v0.64.0 (revert sonrası) |
|---|---|---|
| APK ölçüsü | 30.5 MB | 30.5 MB (dəyişməz — exclude heç nə qənaət etmirdi) |
| Lokal qovluq | 126 | 126 (saxlanılır — 7.3 KB, dəyəri yoxdur) |
| Başlanğıc | — | dəyişməz (F4: təmiz zəncir) |

P4 nəticəsi: **APK struktur etibarlı ilə optimaldır** — 82% native
xəritə nüvəsidir (D-048 qərarı), qalan hissə R8/shrink ilə sıxılıb.
Bu loopda edilən ən dəyərli iş mənfi nəticədir: locale exclude yolu
ölçüldü, qiymətsiz sübut edildi və sənədləşdirildi — gələcək
looplarda eyni cəhd təkrarlanmayacaq. Növbəti real optimizasiya
hədəfi: Play app bundle (per-device .so, −10-13 MB) — dağıtım kanalı
qərarı kimi sahib önündədir.

## 4. Ölçüm təkrarı (gələcək looplar üçün)

```bash
# düzgün metod — ZIP sıxılmış ölçü (du YOX):
python3 -c "
import zipfile
z = zipfile.ZipFile('app-release.apk')
comp = sum(i.compress_size for i in z.infolist())
by_top = {}
for i in z.infolist():
    top = i.filename.split('/')[0]
    by_top[top] = by_top.get(top, 0) + i.compress_size
for k, v in sorted(by_top.items(), key=lambda x: -x[1])[:8]:
    print(f'{k:20s} {v/1048576:8.2f} MB')
print(f'CƏM (sıxılmış): {comp/1048576:.1f} MB')
"
```
