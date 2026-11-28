# LOOP_LOG — İterasiya dövrlərinin jurnalı

Format: hər dövr məqsəd → dəyişikliklər → verifikasiya → nəticə. Növbəti dövr başlamazdan əvvəl bu fayl oxunur; bitmiş iş təkrarlanmır.

---

## Loop 1 — 2026-11-28

**Məqsəd**: Phase 0 (kəşf) tamamlansın; P0 kritik xətalar (GPS, Google giriş, login gecikməsi) düzəldilsin.

**Bazа**: fe7dba1 (v0.59.1, D-181 tamamdır — maraq FK xətası + onboarding qapısı).

### Phase 0 (kəşf) — TAMAMLANDI

- Repo struktur, build mühiti, asılılıqlar, manifest inventarı → `docs/PHASE0_REPORT.md` §1
- Xəritə: dual backend (Google ƏSAS D-108 direktivi / MapLibre Compose 0.20 fallback); user dot + accuracy dairəsi + bearing + stale artıq VAR (D-170/D-180)
- Lokasiya: `QoshulLocation` — LocationManager GPS+network, mock rədd, 50m accuracy qapısı (stream-də), 15m/4s throttle; `locateOnce`-də accuracy filtri YOX (P0.1-H1)
- Auth: Credential Manager (filter=false ✓, nonce ✓, GoTrue IDToken ✓); signOut-da clearCredentialState YOX (P0.2-H1); köhnə session təmizliyi YOX (P0.2-H2); timeout-lar YOX (P0.3-H1)
- Cache-silmə təhlükəsi YOXDUR (P0.4 bu hissəsi qarşılanıb); crash reporting yalnız lokal Safety hook
- Ekranlar: Create sheet rememberSaveable ✓; ProfileEdit-də e-poçt+şifrə var (6.3-ə görə Ayarlara köçürüləcək), dillər/qrup ölçüsü/büdcə/səviyyə YOX (6.4-də əlavə)
- Testlər: 37 JVM/Robolectric faylı; CI 3 job; emülator CI-də YOX

### Açıq suallar (sahibdən)

1. Offline xəritə lazımdırmı? (təklif: bu mərhələdə YOX)
2. Tile provayderi: OpenFreeMap (təklif) / MapTiler / self-host?
3. Xəritə light/dark variantı lazımdırmı? (təklif: tək sxem)
4. Google SİGN-İN testi üçün test Google hesabı + SHA-1 qeydiyyatı (bu mühitdə mümkün deyil)

### Verifikasiya statusu (Phase 0)

- Kompilyasiya: baz commit fe7dba1 CI yaşıl (3/3 job) — dəyişiklik yoxdur
- Emülator/real cihaz: YOX (mühit məhdudiyyəti — PHASE0_REPORT §5)

### Loop 1 davamı — P0 icrası (D-182, v0.59.2)

**Dəyişən fayllar**:
- `core/location/QoshulLocation.kt` — isUsableFix vahid filtri (mock+accuracy+10s köhnəlik), locateOnce filtrli, isLocationEnabled
- `core/backend/SupabaseBackend.kt` — signInWithGoogle: əvvəlki signOut + e-poçt müqayisəsi; signOut(context)+clearCredentialState; fetchGoogleConfig TTL cache
- `features/auth/AuthScreen.kt` — AUTH_TIMEOUT_MS 15s (GoTrue) / 60s (Google dialoq); vaxtaşımı mesajları
- `features/map/MapScreen.kt` — GPS status çipi (axtarış ±50m / söndürülüb → ayarlar)
- `features/settings/SettingsScreen.kt` — signOut(signOutCtx)
- `DirectivesAuditTest.kt` — 6 yeni D-182 audit testi (35 ümumi)
- `docs/PHASE0_REPORT.md`, `docs/decisions.md` (D-182), version 0.59.2 (69)

**Verifikasiya**: kompilyasiya + unit test → CI (bu push); emülator/real cihaz YOX (mühit məhdudiyyəti — PHASE0_REPORT §5; GPS xəritədə görünməsi real cihazda SAHİB tərəfindən yoxlanılmalıdır).

**Bağlanan bəndlər**: P0.1 (kod+UI), P0.2 (sessiya təmizliyi+müqayisə), P0.3 (timeout+cache).
**Açıq qalanlar**: P0.1 real cihaz verifikasiyası; P0.4 (düymə çökmələri — emülator tələb edir); P1 MapLibre əsas backend keçidi; P1-P2 UX (6.2-6.4); P3 Snapchat analizi; P4 optimizasiya.
**Yeni aşkarlananlar**: fetchGoogleConfig hər girişdə HTTP round-trip edirdi (TTL cache ilə həll); `GoogleIdTokenCredential.id` deprecated (yeni `email` propertisi yeni googleid versiyasında — hazırda 1.2.1-də `id` istifadə olunur).
