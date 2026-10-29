# Qoshul.az Dizayn Sistemi — Liquid Glass (minimal) + Hərəkət Dili

> v0.4.0 · D-025 · 2026-10-28
>
> İstifadəçi direktivi: "liquid glass teması olacaq minimal olaraq ama başqa
> bütün bölümlərin animasiyası üçün" — şüşə AZ, hərəkət HƏR YERDƏ.

## 1. Tədqiqat xülasəsi

Araşdırılan mənbələr: Apple iOS 26 "Liquid Glass" (WWDC25) HIG və təhlilləri,
Flutter ekosistemində mövcud implementasiyalar (`liquid_glass_widgets`,
`BackdropFilter` benchmarkları, Impeller blur issue-ləri), sosial tətbiq
animasiya patternləri (staggered lists, swipe deck fizikası, count-up).

### Apple Liquid Glass-in 4 əsas prinsipi

| Prinsip | Məna | Bizim tətbiqimiz |
|---|---|---|
| **Lensing** | Arxadakı məzmun şüşədən keçərkən yumşalır (blur+refraction) | `BackdropFilter` blur 14-18 sigma |
| **İşık qatı** | Üzdən gələn ambient işıq qradiyenti | Ağ alfa 0.36→0.60 diagonal qradiyent |
| **Spekulyar kənar** | Kənarda 1px işıq əksi | `Border.all(white 0.55)` |
| **Funksional qat** | Şüşə YALNIZ naviqasiya/kontrol — məzmun kartlarında YOX | 5 səth (aşağıda) |

Apple HIG-in əsas qaydası: **kontrol, məzmunun üstündə üzən ayrı qatdır**;
kontentə fokus verəndə kontrollar geri çəkilir. Şüşə hər yerdə deyil —
yalnız məzmunun real axdığı səthlərdə işləyir (blur üçün arxada məzmun olmalıdır).

### Niyə `liquid_glass_widgets` paketi YOX (qərar)

- D-023: UI fazasında **sıfır xarici asılılıq** — pozulmaz.
- Fragment shader variantı köhnə Android (GLES) cihazlarında riskli;
  təkrar istehsal (repro) problemləri və pakatın 0.3x versiya sürəti.
- Bizim ehtiyacımız "minimal" — 5 səth üçün SDK daxili `BackdropFilter`
  + qradiyent + kənar kifayət edir (Apple-in öz "Tinted" rejiminin oxşarı).

### Performans reallığı

- Hər `BackdropFilter` səthi ~1.3ms (benchmark; Impeller Android-də
  çoxlu filtr xərclidir — flutter/flutter#132735, #161297).
- Qayda: **bir ekranda max ~4 şüşə səth**; yalnız real üst-üstə düşmə yerlərində.
- `GlassQuality.blur = false` — köhnə cihaz/test fallback: blur'suz
  yarı-şəffaf düz rəng (Apple-in "Tinted" bənzəri, heç bir xərc yoxdur).

## 2. Şüşə səthləri — cəmi 5 (minimal qərar)

| # | Səth | Harada | Arxasında nə axır |
|---|---|---|---|
| 1 | **Alt nav pill** (üzən, radius 24, sigma 18) | AppShell | Siyahı məzmunu (`extendBody: true`) |
| 2 | **Xəritə filtr paneli** (radius 16) | MapScreen | OSM tile xəritəsi (D-030) |
| 3 | **Xəritə alt kartı** (seçilmiş pin) | MapScreen | OSM tile xəritəsi (D-030) |
| 4 | **Sayğac həbi** + uyğunluq çipi | MapScreen / MatchScreen | Xəritə / şəkil |
| 5 | **Match fəaliyyət həbləri** (Keç / Maraqlıyam) | MatchScreen kart üzərində | Profil şəkli |

Kartlar, siyahılar, formalar, sheet-lər — web-parity düz ağ (D-023 birebir
qorunur). Şüşə heç vaxt oxunmaqda çətinlik yaratmır: üstündəki mətn həmişə
tünd `Q.foreground` və ya ağ (şəkil üzərində).

## 3. Hərəkət dili (Motion) — bütün bölmələr

### Tokenlər (`Motion`)

| Token | Dəyər | İstifadə |
|---|---|---|
| `d1` | 150ms | çip seçimi, ikon bounce, toxunuş geri bildirimi |
| `d2` | 250ms | tab keçidi, konteyner morph, dot morph |
| `d3` | 400ms | kart girişi, sheet, bayram pop |
| `d4` | 550ms | səhifə keçidi, XP dolğusu, count-up |
| `ease` | easeOutCubic | standart hər şey (Apple "yumşaldılmış" əyri) |
| `enter` | easeOutBack | girişlər — kiçik overshoot |
| `spring` | elasticOut | vurğulu anlar (swipe geri qayıdış) |
| `decel` | decelerate | siyahı item girişləri |
| `staggerStep` | 45ms | siyahı item gecikmə addımı (max 8) |

`disableAnimations` (systemetmə əlçatı) hörmət olunur — bütün primitivlər
0ms-a düşür, məzmun birbaşa görünür.

### Primitivlər (`core/widgets/motion.dart`)

- **StaggerIn** — siyahı/seqsion girişləri: fade + 14px sürüşmə, sıraya görə gecikmə
- **PopIn** — bounce miqyas (badge, MATCH, nişanlar, QR reveal)
- **ScaleTap** — basılanda 0.97 miqyas (bütün kartlar/düymələr)
- **CountUp** — rəqəm sayğacı (XP, statlar)
- **TypingDots** — 3 nöqtə dalğası (çat "yazır...")
- **Shimmer / SkeletonBox** — yüklənmə iskeletləri
- **FadeThrough** — Material fade-through (tab keçidləri)
- **GlassPageRoute** — səhifə keçidi: fade + 18px sürüşmə + 0.98→1 miqyas

### Bölmə-bölmə hərəkət xəritəsi

| Bölmə | Hərəkətlər |
|---|---|
| **Shell** | şüşə nav pill; tab fade-through; seçilmiş ikon bounce (1.15×); aktiv nöqtə genişlənir; Yarat ScaleTap; oxunmamış badge PopIn |
| **Onboarding** | slayd məzmunu PopIn (keyli); nöqtə morph (6→18px) |
| **Auth** | GlassPageRoute girişlər; vahid forma (D-23 komponentlər) |
| **Kəşf (feed)** | başlıq/axtarış/çiplər staggered; XP zolağı animasiyalı dolğu + CountUp; kartlar StaggerIn+ScaleTap (scroll-da da girir); çip bounce |
| **Xəritə** | pin-lər staggered drop (damla kimi); şüşə filtr paneli; alt kart slide-up; seçilmiş pin morph (34→40px) |
| **Match** | real swipe dəsti: drag→fırlanma, 110px hədd→uçuş, yoxsa elastic geri; MARAQLIYAM/KEÇ möhürləri; arxa kartlar dəst dərinliyi; MATCH pop |
| **Tədbirlər** | başlıq+segmented stagger; tab dəyişəndə siyahı FadeThrough; kartlar StaggerIn+ScaleTap |
| **Məkan/Rezerv** | QR nəticədə PopIn; addım keçidləri (sheet) |
| **Rezervlərim** | GlassPageRoute; status çipləri (ResvStatusChip) |
| **Kuponlar** | təklif kartları staggered; XP ilə al (native ripple) |
| **Söhbətlər** | siyahı stagger; mesajlar StaggerIn; TypingDots "yazır..." |
| **Bildirişlər** | kartlar staggered; oxunmamış sayı pop |
| **Profil** | nişanlar sırayla PopIn; 4 stat CountUp; XP zolağı animasiyalı; tez-keçid ScaleTap |
| **Ayarlar** | switch-lər (native animasiya); GlassPageRoute |

## 4. Fayl strukturu

```
mobile/lib/core/widgets/
├── glass.dart    ← YENİ: GlassSurface, GlassPill, glassText, GlassQuality
├── motion.dart   ← YENİ: Motion tokenləri + 8 primitiv + GlassPageRoute
├── ui.dart       ← XpTrack (animasiyalı), CategoryChip (bounce), ...
└── rol.dart      ← VenueMiniCard/PersonMiniCard (ScaleTap), ...
```

## 5. Gələcək (backend fazasından sonra)

- Keşirmə (shimmer) real yüklənmə hallarına bağlanacaq (mock fazada məlumat
  ani gəlir — iskelet üçün süni gecikmə əlavə edilmir)
- Şüşə keyfiyyeti cihaz benchmarkı ilə avtomatik seçilə bilər
  (`GlassQuality.blur` hazır API-dir)
- Hero keçidləri (xəritə pin → məkan detalları) məlumat real olanda
