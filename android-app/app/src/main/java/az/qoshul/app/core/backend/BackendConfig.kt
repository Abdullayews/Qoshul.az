package az.qoshul.app.core.backend

/**
 * D-151 (2026-11-25, direktiv: „demo məntiqi heç olmasın“): MOCK rejimi
 * TAMAMLANIŞI SİLİNDİ — tətbiq yalnız Supabase server rejimində işləyir.
 *
 * Konfiqurasiya (BuildConfig): SUPABASE_URL, SUPABASE_ANON_KEY,
 * BACKEND_MODE="SUPABASE" (local.properties → env/CI secret → PLACEHOLDER).
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
