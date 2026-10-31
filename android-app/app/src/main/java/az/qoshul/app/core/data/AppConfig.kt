package az.qoshul.app.core.data

/**
 * Tətbiq rejimi (D-050, istifadəçi direktivi 2026-10-31).
 *
 * İKI versiya, İKİ branch:
 *  - PLACEHOLDER  → `main` branch: bütün funksiyalar görünür, məlumatlar
 *    fake/placeholder, vahid hardcoded demo hesabı (`signInDemo`) ilə bir toxunuşda
 *    tam tətbiq açılır. UI sınağı / investor demo / marketinq üçün.
 *  - SERVER_ONLY  → `server-only` branch: serverə qoşulana qədər HEÇ NƏ yoxdur —
 *    yalnız server məlumatı. Fake data yoxdur, giriş yalnız real server auth ilə.
 *
 * Rejim tərtibat vaxtı sabitdir (branch seçimi = rejim seçimi); runtime
 * keçidi yoxdur — iki ayrı APK/branch qəsdən ayrı saxlanılır.
 */
enum class AppMode {
    PLACEHOLDER,
    SERVER_ONLY,
}

/** Tətbiq miqyasında rejim konfiqurasiyası (branch-ə görə dəyişir). */
object AppConfig {

    /**
     * Bu branch-də aktiv rejim: SERVER_ONLY (server-only branch — D-050).
     * main branch-də PLACEHOLDER olur; runtime keçid YOXDUR.
     */
    val MODE: AppMode = AppMode.SERVER_ONLY

    val isPlaceholder: Boolean get() = MODE == AppMode.PLACEHOLDER
    val isServerOnly: Boolean get() = MODE == AppMode.SERVER_ONLY

    /** Ayarlar → "Haqqında" alt mətni (rejim şəffaflığı). */
    val modeLabel: String
        get() = when (MODE) {
            AppMode.PLACEHOLDER -> "Demo (placeholder məlumat)"
            AppMode.SERVER_ONLY -> "Yalnız server məlumatı"
        }

    /**
     * Vahid hardcoded demo hesabı (PLACEHOLDER rejimi, D-050).
     * "Hər funksiyanın görünməsi üçün" — bir toxunuşla dolu profillə daxil olma.
     */
    object DemoAccount {
        const val EMAIL = "demo@qoshul.az"
        const val PASSWORD = "qoshul-demo"
        const val FIRST_NAME = "Demo"
        const val LAST_NAME = "İstifadəçi"
        const val FULL_NAME = "Demo İstifadəçi"
        const val CITY = "Bakı"
        const val BIO = "Qoshul demo hesabı — bütün funksiyalar sınaq məlumatı ilə doludur."
    }
}
