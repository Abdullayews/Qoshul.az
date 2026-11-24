package az.qoshul.app.core.data

/**
 * Tətbiq rejimi (D-050, istifadəçi direktivi 2026-10-31; D-149 yeniləmə
 * 2026-11-24: server inteqrasiyası — main branch artıq SUPABASE rejimində
 * də işləyir, BACKEND_MODE = MOCK|SUPABASE BuildConfig-dən).
 *
 * İKI versiya, İKİ branch:
 *  - PLACEHOLDER  → `main` branch: MOCK rejimində fake/placeholder məlumat
 *    (CI/test). SUPABASE rejimində canlı server (D-149).
 *  - SERVER_ONLY  → `server-only` branch: yalnız server məlumatı.
 *
 * Rejim tərtibat vaxtı sabitdir; runtime keçidi yoxdur.
 *
 * D-149: vahid hardcoded demo hesabı (DemoAccount, demo@qoshul.az /
 * qoshul-demo sabitləri) SİLİNDİ — istifadəçiyə təhvil verilən APK
 * SUPABASE rejimindədir, demo qalığı görünmür.
 */
enum class AppMode {
    PLACEHOLDER,
    SERVER_ONLY,
}

/** Tətbiq miqyasında rejim konfiqurasiyası (branch-ə görə dəyişir). */
object AppConfig {

    /** Bu branch-də aktiv rejim. server-only branch-də SERVER_ONLY olur. */
    val MODE: AppMode = AppMode.PLACEHOLDER

    val isPlaceholder: Boolean get() = MODE == AppMode.PLACEHOLDER
    val isServerOnly: Boolean get() = MODE == AppMode.SERVER_ONLY

    /** Ayarlar → "Haqqında" alt mətni (rejim şəffaflığı). */
    val modeLabel: String
        get() = when (MODE) {
            AppMode.PLACEHOLDER -> "Demo (placeholder məlumat)"
            AppMode.SERVER_ONLY -> "Yalnız server məlumatı"
        }
}
