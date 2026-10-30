package az.qoshul.app.core.model

import java.time.LocalDateTime

// ═══════════════════ Kuponlar (ROL G) — coupon.dart portu ═══════════════════

/** Partnyor məkanın endirim təklifi. */
data class CouponOffer(
    val id: String,
    val venueId: String,
    val venueName: String,
    val label: String,           // '15% endirim'
    val percent: Int = 0,        // faiz (0 = sabit məbləğ)
    val fixedAzn: Int = 0,       // sabit AZN (0 = faiz)
    val minRankIndex: Int = 0,
    val xpPrice: Int = 0,        // 0 = yalnız səviyyə ilə avtomatik
    val validityDays: Int = XpRules.COUPON_VALIDITY_DAYS,
    val autoByLevel: Boolean = false,
) {
    /** Təklif bu istifadəçi üçün açıqdır mı? (səviyyə yoxlaması) */
    fun unlockedFor(rankIndex: Int): Boolean = rankIndex >= minRankIndex
}

/** İstifadəçinin aldığı kupon: unikal kod + müddət + birdəfəlik. */
data class UserCoupon(
    val id: String,
    val offerId: String,
    val code: String,
    val venueId: String,
    val venueName: String,
    val label: String,
    val expiresAt: LocalDateTime,
    val purchasedAt: LocalDateTime,
    var used: Boolean = false,
) {
    val isExpired: Boolean get() = LocalDateTime.now().isAfter(expiresAt)

    /** 'AKTİV' | 'İSTİFADƏ OLUNDU' | 'VAXTIBİTMİŞ' */
    val statusLabel: String
        get() = when {
            used -> "İSTİFADƏ OLUNDU"
            isExpired -> "VAXTIBİTMİŞ"
            else -> "AKTİV"
        }

    val isActive: Boolean get() = !used && !isExpired

    /** Qalan gün sayı. */
    val daysLeft: Int
        get() = if (isExpired) 0
        else java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays().toInt() + 1
}

// ═══════════════════ Rezervlər (ROL E) — reservation.dart portu ═══════════════════

/** Rezerv statusları (ROL E). */
enum class ResvStatus(val label: String) {
    PENDING("GÖZLƏYİR"),
    CONFIRMED("TƏSDİQLƏNDİ"),
    USED("İSTİFADƏ OLUNDU"),
    CANCELLED("LƏĞV EDİLDİ"),
    NO_SHOW("GƏLMƏDİ"),
    REJECTED("RƏDD EDİLDİ"),
}

/** Məkanın rezerv saat slotu (ROL E). */
data class VenueSlot(
    val venueId: String,
    val date: String,   // 'Bu gün', 'Sabah', 'Noy 3'...
    val time: String,   // '18:00'
    val capacity: Int = 8,
)

/** Rezerv — status axını + QR (ROL E, addım 4-6). */
data class Reservation(
    val id: String,
    val venueId: String,
    val venueName: String,
    val venueImage: String,
    val date: String,
    val time: String,
    val party: Int,
    val note: String,
    val couponCode: String?,
    val qrCode: String,
    val createdAt: LocalDateTime,
    var status: ResvStatus = ResvStatus.PENDING,
) {
    val statusLabel: String get() = status.label

    /** Ləğv edilə bilən statuslar (ROL E-7: mock — həmişə pulsuz). */
    val canCancel: Boolean
        get() = status == ResvStatus.PENDING || status == ResvStatus.CONFIRMED

    /** Check-in (QR skan) mümkündür. */
    val canCheckIn: Boolean get() = status == ResvStatus.CONFIRMED
}

// ═══════════════════ Bildirişlər (ROL H) — notification.dart portu ═══════════════════

enum class NotifType(val label: String) {
    MATCH("Yeni match"),
    MESSAGE("Yeni mesaj"),
    EVENT_JOIN("Tədbirə qoşulma sorğusu"),
    EVENT_APPROVAL("Qoşulma təsdiqi"),
    EVENT_REMINDER("Tədbir xatırlatması"),
    RESV_UPDATE("Rezerv yeniliyi"),
    RESV_REMINDER("Rezerv xatırlatması"),
    LEVEL_UP("XP və səviyyə"),
    COUPON_EXPIRING("Kupon vaxtı"),
    NEARBY_EVENT("Yaxınlıqda tədbir"),
    MARKETING("Yeniliklər və təkliflər");

    val isMarketing: Boolean get() = this == MARKETING
}

data class AppNotification(
    val id: String,
    val type: NotifType,
    val title: String,
    val body: String,
    val at: LocalDateTime,
    var read: Boolean = false,
)

/** Gecə sükut rejimi (ROL H): 23:00-08:00 arası marketinq göndərilmir. */
fun isQuietHours(t: LocalDateTime): Boolean = t.hour >= 23 || t.hour < 8

/** Bildiriş tercihleri — hər tip ayrıca söndürülür. */
class NotificationPrefs {
    private val enabled = NotifType.entries.associateWith { true }.toMutableMap()
    fun isEnabled(t: NotifType): Boolean = enabled[t] ?: true
    fun setEnabled(t: NotifType, v: Boolean) { enabled[t] = v }
}

// ═══════════════════ Moderasiya (ROL I) — moderation.dart portu ═══════════════════

/** Report səbəbləri (ROL I). */
val reportReasons = listOf(
    "Saxta profil",
    "Təhqürət və zorakılıq",
    "Spam və reklam",
    "Uyğunsuz məzmun",
    "Başqa səbəb",
)

data class ReportEntry(
    val targetType: String,   // 'user' | 'event' | 'venue'
    val targetId: String,
    val reason: String,
    val note: String,
    val at: LocalDateTime,
)

/** İlk görüşlər üçün təhlükəsizlik məsləhətləri (ROL I). */
val safetyTips = listOf(
    "İctimai yerdə görüş" to
        "İlk görüşü kafe, park və ya digər adam olan məkanda keçir. Şəxsi ünvanda görüşmə.",
    "Yaxın adamını xəbərdar et" to
        "Gedəcəyin yeri və kiminlə görüşəcəyini dostuna yaz. Canlı konum paylaşımı da faydalıdır.",
    "Şəxsi məlumatını dərhal paylaşma" to
        "Ev ünvanı, iş yeri ünvanı və maliyyə məlumatlarını tanışlığın ilk günündə vermə.",
    "Narahatçılıq yaranarsa" to
        "Söhbəti bitir və tətbiqdən blokla / report et. Təcili halda 102 (polis) və ya 112 zəng et.",
    "Yolverilməz davranış" to
        "Təhdid, təhqürət və ya təzyiq görəndənsə dərhal report göndər — moderasiya komandası baxır.",
)
