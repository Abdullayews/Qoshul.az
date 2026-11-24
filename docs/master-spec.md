# Qoşul.az Master Specification (TT — Texniki Tapşırıq)

> Bu sənəd Qoşul.az mobil tətbiqinin arxitekturası, istifadəçi təcrübəsi (UX/UI), süni intellekt modulunun ("Qoşa") inteqrasiyası, oyunlaşdırma (gamification) mexanizmləri və biznes platformasının genişləndirilməsi üzrə tam texniki tapşırığı özündə əks etdirir.
>
> Status: **mənbə sənəd (product-owner tərəfindən təqdim edilib)**. Kod bazası ilə müqayisə (gap analysis) `docs/master-spec-gap-analysis.md` faylındadır. 2026-11-24 tarixində əsas kod: commit `9b63a14` (main).

## Sənədin İcmalı və Funksional İnteqrasiya Xəritəsi

| Modul | Əsas Funksionallıq | İnteqrasiya Olunacaq Sistemlər |
|---|---|---|
| UI/UX & Profil | Full-width dizayn, Stories, Media idarəçiliyi | Qalereya API, Profil DB |
| Matchmaking | Avto-swipe, Oval vizualizasiya, Dinamik filtr | Matchmaking Algoritmi |
| "Qoşa" AI | Empatik psixoloji dəstək, Maskot brendinqi | 16Personalities Engine, LLM Core |
| Tədbir & QR | Dinamik Posterlər, İkili QR doğrulama, Kodu təsdiq | QR Generator/Scanner, Event DB |
| Biznes Rejimi | VÖEN doğrulama, Stol/Kabinet rezervasiyası, Admin Panel | Biznes Dashboard, Merchant API |

---

## 1. Ümumi İnterfeys (UI/UX) və Vizual Nizamlama

- **Tam Ekran Genişliyi (Full-Width Layout):**
  - Bütün əsas səhifələrdə (Home, Explore, Events) və horizontal siyahı bloklarında ("Sənə uyğun insanlar", "Populyar məkanlar", "Yaxınlıqdakı tədbirlər") kənar padding dəyərləri (left/right margin & padding) 0px səviyyəsinə endirilməlidir.
  - Karusel görünüşlü siyahılarda elementlər ekranın sol kənarından sağ kənarına qədər tam uzanmalı, edge-to-edge təcrübəsi təmin edilməlidir.
- **Kateqoriyaların Tam Uyğunluğu:**
  - "Yarat" (Create Event) bölməsində mövcud olan bütün tədbir kateqoriyaları və onlara uyğun ikonoqrafiya sistemi "Tədbirlər" (Events) bölməsinin axtarış, filtrasiya və təsnifat sisteminə 1:1 nisbətində inteqrasiya olunmalıdır.
  - Kateqoriya id-ləri mərkəzi EventCategory enumu üzərindən sinxronlaşdırılmalıdır.

## 2. İstifadəçi Profili, Media İdarəçiliyi və Hekayələr (Stories) Mexanizmi

- **Vizual və Şəxsi Məlumatlar:**
  - "Profili redaktə et" bölməsi modulyar struktura keçirilməlidir: Ad, Soyad, Bio, Maraq dairələri (Tags), Sosial media linkləri və vizual media idarəçiliyi tək ekranda cəmləşdirilməlidir.
  - Cihazın yerli qalereyasından istənilən ölçülü şəkli seçib crop/resize aləti ilə profil rəsmi kimi yükləmək imkanı təmin edilməlidir.
- **Profillərə Keçid Mexanizmi:**
  - İstifadəçilər "Sənə uyğun insanlar" karuselindən, daxili söhbətlərdən (Direct Messages) və ya tədbir qrup çatlarından istifadəçi avatarına toxunduqda həmin profilin daxili kartına (Public Profile View) hamar keçid (smooth animation) təmin edilməlidir.
- **Status və Hekayələr (Stories) Sistemi:** Sosial xaosun və lazımsız məzmun kirliliyinin önünə keçmək üçün Stories sistemi yalnız keçirilmiş tədbir anları, real xatirələr və aktiv tədbir statusları ilə məhdudlaşdırılmalıdır.
  - Profil səhifəsində "Profili redaktə et" düyməsinin dərhal yanında estetik "Plus (+)" ikonu yerləşdirilməlidir. Həmin düyməyə kliklədikdə istifadəçi cari tədbir hekayəsini və ya aktiv sosial statusunu vizual media olaraq əlavə edə bilməlidir.

## 3. Uyğun İnsanlar (Matchmaking) Təkmilləşdirmələri

- **Avtomatik Keçid Məntiqi:** Profil kartı sol/sağ istiqamətinə keçildikdə (swipe left/right) və ya "İmtina" (Skip) düyməsi sıxıldıqda növbəti profilə hamar və avtomatik keçid mexanizmi bərpa edilməlidir (UI freeze problemi aradan qaldırılmalıdır).
- **Siyahının Dinamik Yenilənməsi:** İstifadəçi hər hansı bir profillə uyğunluğu ləğv etdikdə (unmatch/block), həmin profil istifadəçinin aktiv matçlar və təkliflər siyahısından real-vaxt rejimində (WebSocket/State update) dərhal silinməlidir.
- **Vizual Forma:** Matchmaking bölməsində profil şəkillərinin kəskin kvadrat çərçivələri ləğv edilərək, müasir estetik oval (rounded smooth borders, minimum border-radius: 24px) dizayna keçirilməlidir.

## 4. Ayarların Strukturlaşdırılması, Təhlükəsizlik və Hüquqi Baza

### 4.1. Texniki Hesab İdarəçiliyi

"Ayarlar" bölməsindəki "Hesab" (Account) alt-bölməsi vizual məlumatlardan təmizlənməli, yalnız autentifikasiya və təhlükəsizlik parametrlərinə ayrılmalıdır:

- Qeydiyyat metodu indikatoru (SSO Google, Apple ID, Email, Telefon).
- Şifrə dəyişmə və 2FA autentifikasiya parametrləri.
- Hesabın dondurulması və ya tam silinməsi (Delete Account) tələbi.

### 4.2. Funksional Bloklama və Qarşılıqlı Əlaqə

- "Bloklananlar" siyahısı tam funksional hala gətirilməlidir.
- Bloklanmış istifadəçilərin bir-birini xəritədə görməsi, tədbirlərdə iştirakçı siyahısında müşahidə etməsi və bəyənmə (like) göndərməsi tamamilə məhdudlaşdırılmalıdır.

### 4.3. Hüquqi Sənədlər Modulu

"Şərtlər və Məxfilik" (Terms & Privacy) bölməsinə aşağıdakı standartlara cavab verən tam hüquqi mətnlər yerləşdirilməlidir:

- Fərdi məlumatların qorunması (GDPR və AR Qanunvericiliyinə uyğun).
- Məkan məlumatlarının emalı üzrə istifadəçi razılığı.
- İctimai tədbirlərdə davranış qaydaları və platforma məsuliyyəti.

### 4.4. Dinamik Dəvət Mexanizmi (QR Referral System)

- "Dostunu dəvət et" bölməsində "Paylaş" düyməsinə tıklandıqda sistem tərəfindən istifadəçiyə unikal, tək-istifadəlik (dinamik həşli) QR kod və dəvət linki generasiya olunmalıdır.
- Həmin QR kod skan edildikdə dəvət olunan şəxs Google Play Store / App Store rəsmi səhifəsinə yönləndirilir. Qeydiyyat tamamlandıqdan sonra həm dəvət edən, həm də yeni istifadəçinin balansına avtomatik Qoşa Coin bonusu hesablanır.

## 5. "Qoşa" AI Köməkçisi – Maskot Brendinqi və Psixoloji Dəstək Modeli

- **Brendinq və Adlandırma:**
  - AI çatın rəsmi adı "Qoşa" olaraq təsdiqlənir. Sistem tərəfindən cavab verən bütün mesajların başlıq adı kimi "Qoşa" göstərilməlidir.
  - Ana ekrandakı blurlanmış neomorfik robot başı ikonu brend identikliyi kimi saxlanılmalıdır.
- **İkili Maskot İnteqrasiyası (Civ-Civ):**
  - Çatın yuxarı naviqasiya panelindəki avatar yerində iki rəsmi maskot əks olunmalıdır: Narıncı rəngli Ekstravert Civ-Civ və Yaşıl rəngli İntrovert Civ-Civ.
  - AI istifadəçinin MBTI nəticəsinə uyğun olaraq müvafiq maskot obrazını (ekstravertlər üçün narıncı, introvertlər üçün yaşıl) aktiv söhbət partneri kimi istifadə etməlidir.

```
       +-------------------------------------------------------+
       |                  QOŞA AI ENGINE                        |
       +-------------------------------------------------------+
                                   |
         +-------------------------+-------------------------+
         |                                                   |
         v                                                   v
  [ Extravert User ]                                  [ Introvert User ]
  Avatar: Narıncı Civ-Civ                             Avatar: Yaşıl Civ-Civ
  Tone: Motivasiyaedici, Proaktiv                    Tone: Empatik, Səmimi, Sakit
```

- **Səmimi Psixoloq və Dəstək Modeli:**
  - "Qoşa" sadə sorğu-cavab botu deyil, iminal intelekta (EQ) malik səmimi psixoloq və sosiallaşma mentoru kimi proqramlaşdırılmalıdır.
  - "Özün yaz", "Özün tap" kimi passiv və neqativ ifadələr sistem promptundan tamamilə təmizlənməlidir. AI təşəbbüsü öz üzərinə götürməli, aktiv təkliflər verməlidir.
- **16Personalities Əsaslı Dəstək:**
  - AI istifadəçinin 16Personalities testinin nəticələrini (məsələn: INTJ, ENFP) daxili yaddaşda saxlamalı və cavab strukturlarını həmin şəxsiyyət tipinin emosional ehtiyaclarına uyğunlaşdırmalıdır.

## 6. AI Tədbir Yaradıcısı və Dinamik Tədbir Posterləri

- **Avtomatik Doldurma Engine:** "Tədbiri AI ilə doldur" imkanı: İstifadəçi tədbir haqqında qısa bir cümlə (məsələn: "Bu cümə axşamı dəniz kənarında gitarada ifa etmək istəyirəm") yazdıqda, AI həmin mətn al əsasında tədbirin adını, təsvirini, kateqoriyasını, daxili etiketlərini və optimal zaman aralığını avtomatik təyin edir.
- **Dinamik Tədbir Posteri Generasiyası:**
  - Tədbir təsdiqləndiyi anda sistem arxa fonda avtomatik şaquli/dördkünc vizual Tədbir Posteri generasiya etməlidir.
  - Poster üzərində estetik şəkildə yerləşdiriləcək məlumatlar: (i) Tədbirin adı və əsas konseptual vizualı; (ii) Tarix, saat və məkan adı; (iii) İştirakçılara veriləcək Qoşa Coin miqdarı; (iv) Qeyd: Poster üzərində heç bir şəxsi telefon nömrəsi göstərilməməlidir.

## 7. Söhbətlər, Tədbir Profilləri və İştirakçı Kodları

- **Mərkəzləşdirilmiş Söhbət İnterfeysi:** Söhbətlər (Chats) siyahısı mərkəzləşdirilməlidir. Aktiv söhbət olmadıqda vizual olaraq estetik illüstrasiyadan ibarət "Empty State" ekranı nümayiş etdirilməlidir.
- **Tədbir Profili (Group Identity):** Yaradılan hər bir tədbir avtomatik olaraq öz daxili "Tədbir Profili"nə malik olur. Söhbət siyahısından birbaşa bu profilə keçid mümkündür.
- **İştirakçı İndikatorları və 4-Rəqəmli Təhlükəsizlik Kodu:**
  - Tədbir profilində: AI generasiya olunmuş poster, iştirakçı siyahısı, ümumi tədbir QR kodu yer alır.
  - Siyahıda hər bir iştirakçı adının qarşısında onun tədbirə giriş anına qədər aktiv olan fərdi 4 rəqəmli təhlükəsizlik kodu (Security Code) və təsdiq indikatoru əks olunmalıdır.

## 8. Xəritə və Lay (Layer) İdarəçiliyi

- **Müstəqil Lay Sistemi:** Xəritə interfeysi sadə məsafə filtrindən çıxarılaraq müstəqil idarə edilə bilən 3 ana laya ayrılmalıdır:
  1. İnsanlar Layı: Aktiv, uyğunlaşan və status paylaşan istifadəçiləri göstərir.
  2. Hadisələr Layı: Yaxınlıqda baş tutacaq tədbir və görüş nöqtələrini göstərir.
  3. Məkanlar Layı: Partnyor restoran, kafe və əyləncə mərkəzlərini göstərir.
- **Məkan Alt-Filtrləri:** Məkanlar layı aktiv olduqda dərhal ekranın yuxarı hissəsində sürətli filtrasiya düymələri açılmalıdır: "İndi açıqdır" və "Endirim var".

## 9. Qoşa Coin Oyunlaşdırma, Tapşırıqlar Mərkəzi (Task Center) və 16Personalities İnteqrasiyası

- **Valyuta Standartlaşdırılması:** Tətbiq daxilindəki bütün təcrübə xalları (XP) rəsmi olaraq "Qoşa Coin" adlandırılır və xüsusi coin ikonu ilə təmsil olunur.
- **Vizual Tapşırıqlar Mərkəzi (Task Center):** Tapşırıqlar bölməsi 3 dərəcəli iyerarxiya üzrə qruplaşdırılmalıdır:

| Kateqoriya | Məqsəd | Nümunə Tapşırıq | Mükafat |
|---|---|---|---|
| Novice (Başlanğıc) | Onboarding və profil doldurulması | Profil şəklini yüklə, 16P testini keç | +50 Qoşa Coin |
| Daily (Gündəlik) | Gündəlik aktivliyin saxlanılması | Xəritədə 1 məkanı ziyarət et, 1 tədbirə bax | +10 Qoşa Coin |
| Advanced (İrəli) | Yüksək sosial aktivlik | Həftədə 2 real tədbir təşkil et və tamamla | +100 Qoşa Coin |

- **Gərginləşdirilmiş Nailiyyətlər (Achievements) Sistemi:** Coin və nişanların (badges) qazanılma çətinliyi artırılmalıdır. Nişanlar yalnız həqiqi sosial aktivlik və real doğrulanan tədbir iştiraklarından sonra kiliddən çıxarılmalıdır.
- **16Personalities İnteqrasiya Modeli:** Səthi 10 suallıq test ləğv edilir. Əvəzində 50–100 sualdan ibarət psixometrik model inteqrasiya olunur.
  - UX interfeysi: Hər səhifədə dəqiq 6 sual göstərilir, istifadəçi "Növbəti" düyməsi ilə irəliləyir. Testin sonunda 16 tiptən biri (məs: Memar - INTJ, Məntiqçi - INTP, Debatçı - ENTP) təyin olunaraq istifadəçi kartına işlənir.

## 10. İkili QR-Kod Doğrulama Sistemi və Məkan Rezervasiyası

Fırıldaqçılığın və saxta tədbir iştiraklarının 100% qarşısını almaq üçün aşağıdakı ikili doğrulama zənciri tətbiq olunur:

```
[İştirakçı 4-Rəqəmli Kod / QR] ---> [Təşkilatçı Skaner Yoxlaması]
                                              │
                                              v
[Qoşa Coin & Kupon Balansa Oturur] <--- [Məkan/Restoran Admin Skaneri]
```

1. **Fərdi Doğrulama:** 10 nəfərlik tədbirdə hər bir iştirakçının individual QR kodu və 4-rəqəmli kodu olur. Təşkilatçı 10 iştirakçının kodunu ayrı-ayrılıqda skan etməlidir.
2. **İkili İnzibati Yoxlama:** Məkanda keçirilən tədbirlərdə iştirakın təsdiqlənməsi üçün həm təşkilatçı, həm də Məkan Admini (restoran/kafe məsulu) öz paneli vasitəsilə QR kodu skan etməlidir. Yalnız ikili təsdiqdən sonra Qoşa Coin-lər və endirim kuponları istifadəçilərin balansında aktivləşir.

## 11. Genişləndirilmiş Biznes Rejimi, Admin Paneli və Məkan Ekosistemi

- **Biznes Hesabına Keçid və Doğrulama:**
  - Şirkətlər və məkanlar üçün biznes profili qeydiyyatı: İstifadəçinin mövcud profili üzərindən VÖEN, hüquqi unvan, rəsmi restoran/məkan adı və sənədlər tələb olunur.
  - Müəssisələr öz işçiləri üçün daxili (özəl) korporativ tədbirlər təşkil etmək imkanına malik olurlar.
- **Biznes Admin Paneli:** VÖEN/Email və Şifrə ilə daxil olunan müstəqil web/mobile idarəetmə paneli (Auto-fill dəstəyi ilə). Panelin alt hissəsində gələn müştərilərin iştirakını təsdiqləmək üçün sürətli QR-kod skaneri interfeysi yerləşdirilir.
- **Masa və Kabinet İdarəçiliyi (Inventory Management):** Məkan admini paneldən obyektdəki:
  - Stol sayını və stolların spesifik nömrələrini,
  - Kabinet sayını, nömrələrini və daxili avadanlıq xüsusiyyətlərini (PlayStation, Karaoke, VIP və s.) daxil edib idarə edə bilir.
- **Məkan Profili və İnteraktiv Statuslar:**
  - İstifadəçilər məkanın profilinə daxil olduqda interaktiv masa/kabinet sxemini görür və rezervasiya edə bilirlər.
  - Məkanda baş tutan tədbirlərdən çəkilən foto və videolar avtomatik olaraq həmin məkanın profilində "Məkan Statusu/Hekayələri" bölməsində ziyarətçi xatirələri kimi nümayiş olunur.
