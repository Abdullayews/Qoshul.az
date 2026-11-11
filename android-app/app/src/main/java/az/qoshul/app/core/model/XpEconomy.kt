package az.qoshul.app.core.model

import java.time.LocalDateTime

/**
 * İkitərəfli XP iqtisadiyyatı modelləri — Flutter `core/models/xp_economy.dart`
 * portu (D-026, product-spec L). UI fazası: mock simulyasiya; backend fazasında
 * hər əməliyyat idempotency key ilə serverdə icra olunacaq (qayda 8/9).
 */

/** Xəritə görünüşü — 3 seçim (D-027): Hamı / Yalnız dostlar / Gizli. */
enum class MapVisibility(val label: String, val hint: String) {
    EVERYONE("Hamı", "Yerin xəritədə və match'də hər kəsə görünür"),
    FRIENDS("Yalnız dostlar", "Yerin yalnız səninlə dost olan istifadəçilərə görünür"),
    OFF("Gizli", "Xəritədə və match'də görünmürsən"),
}

/** Obyekt başına XP tənzimləmələri — gələcəkdə `xp_config` cədvəli (L.3/L.4). */
data class VenueXpConfig(
    /** Məzənnə: 1 AZN endirim üçün tələb olunan XP (məs. 20 → 100 XP = 5 AZN). */
    val xpPerAzn: Int = 20,
    /** Maksimum endirim — hesabın faizi (obyekt ziyanından qorunma). */
    val maxDiscountPercent: Int = 25,
    /** Minimum hesab məbləği (AZN; 0 = limit yoxdur). */
    val minBillAzn: Int = 0,
) {
    /** Nümunə mətn: "100 XP = 5 AZN · maks 25% · min hesab 10 ₼". */
    fun describe(): String {
        val per100 = (100.0 / xpPerAzn).toInt()
        val base = "100 XP = $per100 ₼ · maks $maxDiscountPercent%"
        return if (minBillAzn > 0) "$base · min hesab $minBillAzn ₼" else base
    }
}

/** Ledger qeydi — hər əməliyyat izlənəndir (L.4-6; mübahisə sübutu). */
data class XpLedgerEntry(
    val id: String,
    val venueId: String,
    val grant: Boolean,        // true = qazanma, false = xərcləmə
    val amount: Int,           // həmişə müsbət
    val balanceAfter: Int,
    val note: String,
    val at: LocalDateTime,
)

/** İstifadəçinin bir obyektə məxsus Endirim XP hesabı (L.2 — ayrı baza). */
class VenueXpAccount(
    val venueId: String,
    val venueName: String,
    balance: Int,
    val config: VenueXpConfig = VenueXpConfig(),
) {
    var balance: Int = balance
        private set
    private val _ledger = mutableListOf<XpLedgerEntry>()
    val ledger: List<XpLedgerEntry> get() = _ledger

    /**
     * Qazanma — balansı artırır (L.2).
     * A2: `at` məcburidir — gizli LocalDateTime.now() bypass-ları aradan qalxıb;
     * çağıran tərəf (AppEngine) vahid clock-dan ötürür.
     */
    fun grant(amount: Int, note: String, id: String, at: LocalDateTime) {
        balance += amount
        _ledger.add(XpLedgerEntry(id, venueId, true, amount, balance, note, at))
    }

    /**
     * Xərcləmə — B6 düzəlişi (M0, 2026-11-07): mənfi balans MÜMKÜN DEYİL.
     * İstifadəçi qərarı: "kupon XP-si yalnız hesabda var olan qədəri istifadə
     * oluna bilər" → amount ≤ 0 və ya amount > balans olduqda FALSE qayıdır
     * və ledger-ə HEÇ NƏ yazılmır (atomiklik: ya tam, ya heç).
     * A2: `at` məcburidir (vahid clock).
     */
    fun spend(amount: Int, note: String, id: String, at: LocalDateTime): Boolean {
        if (amount <= 0 || amount > balance) return false
        balance -= amount
        _ledger.add(XpLedgerEntry(id, venueId, false, amount, balance, note, at))
        return true
    }

    /** Tarixçə seed-i — balansı DƏYİŞMİR (fake ilkin ledger üçün). */
    fun seedHistory(entry: XpLedgerEntry) {
        _ledger.add(entry)
    }

    /** Ledger ən yenidən köhnəyə (UI üçün tərs sıra). */
    val recentLedger: List<XpLedgerEntry> get() = _ledger.asReversed()
}

/** Müştərinin dinamik XP kartı kodu (L.4-1): 60 saniyəlik, tək istifadəlik. */
class XpCardCode private constructor(
    val nonce: String,
    val fallbackCode: String,
    val expiresAt: LocalDateTime,
) {
    var used: Boolean = false

    /** A2: `at` məcburi — gizli now() yoxdur; çağıran vahid clock verir. */
    fun isValid(at: LocalDateTime): Boolean = !used && at.isBefore(expiresAt)

    fun secondsLeft(at: LocalDateTime): Int =
        java.time.Duration.between(at, expiresAt).seconds
            .toInt().coerceIn(0, XpRules.XP_CARD_TTL_SECONDS)

    companion object {
        private var nonceSeq = 7

        /**
         * Yeni kod — TTL 60 saniyə (D-026).
         *
         * B5 (M0 interfeys hazırlığı): [codeSource] injeksiya olunur — mock
         * deterministikdir; M1/M2-də server CSPRNG nonce-u EYNİ imza ilə
         * veriləcək (rand crate). [az.qoshul.app.core.model.XpRules.FALLBACK_CODE_DIGITS]
         * rəqəm sayı artıq hardcode deyil (N7).
         */
        fun fresh(
            now: LocalDateTime,
            codeSource: () -> Int = { mockCode(now) },
        ): XpCardCode {
            val raw = ((codeSource() % 1_000_000) + 1_000_000) % 1_000_000
            val code = raw.toString().padStart(XpRules.FALLBACK_CODE_DIGITS, '0')
            return XpCardCode(
                nonce = "QSHUL-XP-$code-${raw % 97}",
                fallbackCode = code,
                expiresAt = now.plusSeconds(XpRules.XP_CARD_TTL_SECONDS.toLong()),
            )
        }

        /** Mock deterministik kod (B5 sənədi: crypto-random DEYİL — M1-də əvəz olunur). */
        private fun mockCode(now: LocalDateTime): Int {
            val seed = (now.nano / 1000) xor nonceSeq++.hashCode()
            return (seed and 0x7FFFFFFF) % 1_000_000
        }
    }
}

/** Təşkilatçı bitirmə axınının nəticəsi (L.3). */
data class ActivityXpResult(
    val organizerXp: Int,
    val perParticipantXp: Int,
    val taggedCount: Int,
    val venueId: String,
    val venueName: String,
) {
    val participantsTotal: Int get() = taggedCount * perParticipantXp
    val total: Int get() = organizerXp + participantsTotal
}

/**
 * Xərcləmə/kupon xətaları — TYPED (A1 düzəlişi, 2026-11-07 M0).
 *
 * Keçmiş: string açarlar ("hesap"/"hesab"/"balans"/"minBill"/"cap") —
 * "hesap" tipo-orfoqrafiyası UI-da `else -> ""` ilə SƏSSİM udulurdu.
 * Rust miqrasiyasında `enum SpendError` paraleli (M2); string açarlar qadağan.
 */
enum class SpendError {
    /** Obyektin XP hesabı tapılmadı (keçmiş "hesap"/"hesab"). */
    NO_ACCOUNT,
    /** Məbləğ və ya XP ≤ 0 (keçmiş "hesab"). */
    INVALID_INPUT,
    /** xp_config pozuntusu: xpPerAzn ≤ 0 (keçmiş "hesab"). */
    BAD_CONFIG,
    /** Hesabda kifayət qədər XP yoxdur (keçmiş "balans") —
     *  istifadəçi qərarı (2026-11-07): yalnız hesabda VAR OLAN qədər XP
     *  istifadə oluna bilər, mənfi balans mümkün deyil. */
    INSUFFICIENT_BALANCE,
    /** Hesab minimum məbləğdən aşağıdır (keçmiş "minBill"). */
    BILL_BELOW_MIN,
    /** Endirim hesabın maksimum faizini aşır (keçmiş "cap"). */
    CAP_EXCEEDED,
    // — buyCoupon-a məxsus yollar (N2: 5 səssiz-null yolu görünəndir) —
    /** Belə təklif yoxdur. */
    OFFER_NOT_FOUND,
    /** Səviyyə-avto təklif XP ilə alınmır (yalnız səviyyə ilə açılır). */
    AUTO_ONLY,
    /** Səviyyə tələbi ödənilməyib. */
    LEVEL_LOCKED,
}

/** previewSpend nəticəsi (L.4-4). */
data class SpendPreview(
    val ok: Boolean,
    val discount: Double,
    /** Uğursuzluq səbəbi — typed (A1); uğurda null. */
    val error: SpendError?,
)

/** buyCoupon nəticəsi (N2: uğursuzluq səbəbsiz SƏSSİZ deyil, görünəndir). */
sealed interface CouponPurchase {
    data class Success(val coupon: UserCoupon) : CouponPurchase
    data class Failure(val error: SpendError) : CouponPurchase
}
