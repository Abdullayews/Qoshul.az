package az.qoshul.app.core.data

import java.time.LocalDate

/**
 * Növbəti günlərin etiketləri: "Bu gün", "Sabah", "Noy 14" (D-050-dən
 * FakeQoshulDataSource-dan ayrıldı — təqvim hesablaması data mənbəyindən
 * asılı deyil, hər iki rejimdə eyni işləyir).
 */
private val MONTHS = listOf(
    "Yan", "Fev", "Mar", "Apr", "May", "İyn", "İyl", "Avq", "Sen", "Okt", "Noy", "Dek"
)

fun nextDateLabels(count: Int = 7, today: LocalDate = LocalDate.now()): List<String> {
    return (0 until count).map { i ->
        when (i) {
            0 -> "Bu gün"
            1 -> "Sabah"
            else -> dayLabel(today.plusDays(i.toLong()), today)
        }
    }
}

/** "Noy 14" formatlı gün etiketi (fake data ilə eyni format, D-051). */
fun dayLabel(d: LocalDate, today: LocalDate = LocalDate.now()): String =
    "${MONTHS[d.monthValue - 1]} ${d.dayOfMonth}"

/**
 * D-051 fix: yaradılan tədbirlər ISO tarix ("2026-11-02") yerinə fake data
 * ilə eyni SADƏ etiket alır ("Bu gün" / "Sabah" / "Noy 2") — kartlarda
 * göstərim və "Bu axşam" sayğısı (FeedScreen) tutarlı olur.
 */
fun dateLabelFor(epochDay: Long, today: LocalDate = LocalDate.now()): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return when (d) {
        today -> "Bu gün"
        today.plusDays(1) -> "Sabah"
        else -> dayLabel(d, today)
    }
}
