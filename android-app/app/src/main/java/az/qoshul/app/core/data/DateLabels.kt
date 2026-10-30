package az.qoshul.app.core.data

import java.time.LocalDate

/**
 * Növbəti günlərin etiketləri: "Bu gün", "Sabah", "Noy 14" (D-050-dən
 * FakeQoshulDataSource-dan ayrıldı — təqvim hesablaması data mənbəyindən
 * asılı deyil, hər iki rejimdə eyni işləyir).
 */
fun nextDateLabels(count: Int = 7, today: LocalDate = LocalDate.now()): List<String> {
    val months = listOf(
        "Yan", "Fev", "Mar", "Apr", "May", "İyn", "İyl", "Avq", "Sen", "Okt", "Noy", "Dek"
    )
    return (0 until count).map { i ->
        val d = today.plusDays(i.toLong())
        when (i) {
            0 -> "Bu gün"
            1 -> "Sabah"
            else -> "${months[d.monthValue - 1]} ${d.dayOfMonth}"
        }
    }
}
