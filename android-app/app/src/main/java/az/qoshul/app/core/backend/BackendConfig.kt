package az.qoshul.app.core.backend

/**
 * D-151 (2026-11-25, direktiv: „demo məntiqi heç olmasın“): MOCK rejimi
 * TAMAMLANIŞI SİLİNDİ — tətbiq yalnız Supabase server rejimində işləyir.
 *
 * Konfiqurasiya (BuildConfig): SUPABASE_URL, SUPABASE_ANON_KEY
 * (local.properties → env/CI secret → PLACEHOLDER). BACKEND_MODE sahəsi
 * D-170-də silindi — tək rejim var, seçim yoxdur.
 *
 * PLACEHOLDER qaldıqda (məs. fork build) [SupabaseBackend.client] null
 * olur — tətbiq açılır, amma xəta aydın görünür ([describe] ayarlarda
 * göstərilir); SƏSSİZ DEMO FALLBACK YOXDUR.
 */
object BackendConfig {

    /** Server konfiqurasiyası tamdırmı? (URL + anon key real dəyərdir) */
    val isConfigured: Boolean by lazy {
        isUsableUrl(az.qoshul.app.BuildConfig.SUPABASE_URL) &&
            isUsableKey(az.qoshul.app.BuildConfig.SUPABASE_ANON_KEY)
    }

    val supabaseUrl: String get() = az.qoshul.app.BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = az.qoshul.app.BuildConfig.SUPABASE_ANON_KEY

    /**
     * D-155 (master-spec §4.1 — Google SSO): M4 mikroservisi bazası.
     * SUPABASE_URL (https://supabase.qoshul.com) üzərindən /m4/ prefix-i
     * Envoy ilə qoshul-m4 clusterinə yönlənir (google id_token doğrulama).
     * SUPABASE_URL qurulmayıbsa M4 də yoxdur.
     */
    val m4Url: String get() = supabaseUrl.trimEnd('/') + "/m4"

    /**
     * D-156 (28444 xətası): setServerClientId yalnız WEB tipli klient ID
     * qəbul edir. WEB ID artıq APK-ya build-time deyil, M4 /config-dən
     * RUN-TIME oxunur — istifadəçi Console-da ID yaratdıqda yeni APK
     * buraxmağa ehtiyac qalmır (yalnız server env yenilənir).
     */
    @Volatile var googleWebClientId: String? = null
        private set

    /** WEB ID m4-dən gəlibsə yadda saxla (app start + retry-lərdə). */
    fun adoptGoogleWebClientId(id: String?) {
        if (!id.isNullOrBlank() && id != "null") googleWebClientId = id
    }

    /** Google düyməsi göstərilsinmi? (BuildConfig ID VEYA m4-dən gələn ID). */
    val googleReady: Boolean
        get() = googleWebClientId != null || (
            az.qoshul.app.BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() &&
            !az.qoshul.app.BuildConfig.GOOGLE_WEB_CLIENT_ID.contains("PLACEHOLDER")
        )

    /** Aktiv WEB ID: dinamik (m4) üstünlük təşkil edir, yoxsa BuildConfig. */
    val activeGoogleClientId: String?
        get() = googleWebClientId ?: az.qoshul.app.BuildConfig.GOOGLE_WEB_CLIENT_ID
            .takeIf { it.isNotBlank() && !it.contains("PLACEHOLDER") }

    /** "https://…" ilə başlayır və PLACEHOLDER deyil. */
    fun isUsableUrl(url: String): Boolean =
        url.startsWith("https://") && !url.contains("PLACEHOLDER") && url.contains(".")

    /** JWT-ə oxşar (3 hissəli) və PLACEHOLDER deyil. */
    fun isUsableKey(key: String): Boolean =
        !key.contains("PLACEHOLDER") && key.count { it == '.' } >= 2 && key.length > 40

    /** Ayarlar → "Haqqında" alt mətni (konfiqurasiya şəffaflığı). */
    fun describe(): String =
        if (isConfigured) "Server rejimi ($supabaseUrl)"
        else "Konfiqurasiya tamamlanmayıb (SUPABASE_URL/ANON_KEY)"
}
