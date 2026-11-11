package az.qoshul.app.core.backend

/**
 * M1 (D-078): backend rejimi — D-050 ikili rejimin genişləndirilməsi.
 *
 *  - [MOCK]     — cari davranış: FakeData + AppEngine lokal simulyasiyası
 *                 (offline-first). Standard CI/test rejimi.
 *  - [SUPABASE] — tətbiq Supabase layihəsinə qoşulur: mutasiyalar
 *                 [SyncSink] üzərindən serverə güzgülənir (intent → RPC),
 *                 qaydaları server tərəfdə SQL funksiyaları ZORLAYIR
 *                 (qayda 8/9 — supabase/README.md §4).
 */
enum class BackendMode { MOCK, SUPABASE }

/**
 * BuildConfig → rejim qərarı. PLACEHOLDER dəyərlər (D-079: istifadəçi
 * hələ verməyib) AVTOMATİK MOCK-a düşür — crash yoxdur, səssiz geri çəkilir.
 */
object BackendConfig {

    val mode: BackendMode by lazy {
        val configured = az.qoshul.app.BuildConfig.BACKEND_MODE.equals("SUPABASE", true) &&
            isUsableUrl(az.qoshul.app.BuildConfig.SUPABASE_URL) &&
            isUsableKey(az.qoshul.app.BuildConfig.SUPABASE_ANON_KEY)
        if (configured) BackendMode.SUPABASE else BackendMode.MOCK
    }

    val supabaseUrl: String get() = az.qoshul.app.BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = az.qoshul.app.BuildConfig.SUPABASE_ANON_KEY

    /** "https://…" ilə başlayır və PLACEHOLDER deyil. */
    fun isUsableUrl(url: String): Boolean =
        url.startsWith("https://") && !url.contains("PLACEHOLDER") && url.contains(".")

    /** JWT-ə oxşar (3 hissəli) və PLACEHOLDER deyil. */
    fun isUsableKey(key: String): Boolean =
        !key.contains("PLACEHOLDER") && key.count { it == '.' } >= 2 && key.length > 40

    /** Yekun qərar mətni (log/UI üçün). */
    fun describe(): String = when (mode) {
        BackendMode.MOCK ->
            "MOCK (offline demo; SUPABASE_URL/ANON_KEY local.properties faylında qurulanda " +
                "BACKEND_MODE=SUPABASE ilə server rejiminə keçir)"
        BackendMode.SUPABASE -> "SUPABASE (${supabaseUrl})"
    }
}
