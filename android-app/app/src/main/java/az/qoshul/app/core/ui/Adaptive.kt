package az.qoshul.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Adaptiv pəncərə eni sinifləri — D-102 (v0.35.0, texniki araşdırma
 * hesabatı §7.4: "v0.35+ adaptive — Android 17-nin böyük ekran
 * məcburiyyətinə hazırlıq; Navigation bar → rail adaptasiyası").
 *
 * Canonical Material 3 window size class sərhədləri (yalnız EN —
 * naviqasiya qərarı üçün hündürlük lazım deyil):
 *  - Compact   < 600dp   — telefon (portret): alt LiquidNav qalır;
 *  - Medium    ≥ 600dp   — planşet/qatlanan (açıq dar): sol LiquidRail;
 *  - Expanded  ≥ 840dp   — planşet (land)/desk: sol LiquidRail.
 *
 * Yanaşma: material3-window-size-class artefaktı ƏLAVƏ OLUNMUR —
 * sərhədlər rəsmi kanonik sabitlərdir (600/840), kod bazasının
 * "öz-özünə kifayət" prinsipinə uyğun (Motion/Glass portları kimi)
 * və build şəbəkə riski sıfırdır. Gələcəkdə M3 adaptive kitabxanasına
 * keçid lazım olsa (height sinifi, hinge), bu API-nin arxası dəyişir,
 * çağırış nöqtələri xeyrini görmür.
 */
enum class WindowWidthClass { Compact, Medium, Expanded }

/**
 * En (dp) → sinif. Saf funksiya — JVM testlərində sərhəd dəyərləri
 * (599/600/839/840) birbaşa yoxlanılır. Sərhəd daxildir: 600 = Medium,
 * 840 = Expanded (Material 3 spesifikasiyası).
 */
fun widthClassOf(screenWidthDp: Int): WindowWidthClass = when {
    screenWidthDp < 600 -> WindowWidthClass.Compact
    screenWidthDp < 840 -> WindowWidthClass.Medium
    else -> WindowWidthClass.Expanded
}

/**
 * Cari pəncərənin en sinifi — konfiqurasiya dəyişəndə (fırlanma,
 * qatlanma) yenidən hesablanır. LocalConfiguration screenWidthDp
 * cari pəncərənin actual enini verir (multi-window daxil).
 */
@Composable
fun rememberWindowWidthClass(): WindowWidthClass {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        widthClassOf(configuration.screenWidthDp)
    }
}
