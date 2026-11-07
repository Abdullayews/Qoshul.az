package az.qoshul.app.core.data

import java.time.LocalDate

/**
 * Növbəti günlərin etiketləri: "Bu gün", "Sabah", "Noy 14" (D-050-dən
 * FakeQoshulDataSource-dan ayrıldı — təqvim hesablaması data mənbəyindən
 * asılı deyil, hər iki rejimdə eyni işləyir).
 *
 * A2 (M0): `today` məcburidir — gizli LocalDate.now() bypass-ı yoxdur;
 * çağıran vahid clock-dan (AppEngine.today()) ötürür.
 */
private val MONTHS = listOf(
    "Yan", "Fev", "Mar", "Apr", "May", "İyn", "İyl", "Avq", "Sen", "Okt", "Noy", "Dek"
)

fun nextDateLabels(count: Int = 7, today: LocalDate): List<String> {
    return (0 until count).map { i ->
        when (i) {
            0 -> "Bu gün"
            1 -> "Sabah"
            else -> dayLabel(today.plusDays(i.toLong()), today)
        }
    }
}

/** "Noy 14" formatlı gün etiketi (fake data ilə eyni format, D-051). */
fun dayLabel(d: LocalDate, today: LocalDate): String =
    "${MONTHS[d.monthValue - 1]} ${d.dayOfMonth}"

/**
 * D-051 fix: yaradılan tədbirlər ISO tarix ("2026-11-02") yerinə fake data
 * ilə eyni SADƏ etiket alır ("Bu gün" / "Sabah" / "Noy 2") — kartlarda
 * göstərim və "Bu axşam" sayğısı (FeedScreen) tutarlı olur.
 *
 * D-061 "nə olar əgər": epochDay aralığın xaricində olsa (korlanmış
 * dəyər) LocalDate.ofEpochDay atır — Safety.of "—" placeholder-i qaytarır.
 */
fun dateLabelFor(epochDay: Long, today: LocalDate): String =
    az.qoshul.app.core.safety.Safety.of(tag = "date-label", fallback = "—") {
        val d = LocalDate.ofEpochDay(epochDay)
        when (d) {
            today -> "Bu gün"
            today.plusDays(1) -> "Sabah"
            else -> dayLabel(d, today)
        }
    }

/**
 * M1 (D-078): tərs çevirmə — etiket ("Bu gün"/"Sabah"/"Noy 14") → ISO tarix.
 * Supabase güzgüsü üçün (fn_create_reservation real date istəyir).
 * Naməlum formatda null (Safety blok-ləğvi YOXDUR — çağıran null-gözələyir).
 */
fun dateLabelToIso(label: String, today: LocalDate): String? {
    val t = label.trim()
    return when {
        t == "Bu gün" -> today.toString()
        t == "Sabah" -> today.plusDays(1).toString()
        else -> {
            val m = Regex("^([A-Za-zçğıəöşüÇĞİƏÖŞÜ]{3})\\s+(\\d{1,2})$").find(t)
                ?: return null
            val mi = MONTHS.indexOfFirst { it.equals(m.groupValues[1], ignoreCase = true) }
            if (mi < 0) return null
            val day = m.groupValues[2].toIntOrNull() ?: return null
            runCatching { LocalDate.of(today.year, mi + 1, day).toString() }.getOrNull()
        }
    }
}
