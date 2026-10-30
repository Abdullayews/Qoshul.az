package az.qoshul.app.core.model

import java.time.LocalDateTime

/**
 * İkitərəfli XP iqtisadiyyatı modelləri — Flutter `core/models/xp_economy.dart`
 * portu (D-026, product-spec L). UI fazası: mock simulyasiya; backend fazasında
 * hər əməliyyat idempotency key ilə serverdə icra olunacaq (qayda 8/9).
 */

/** Xəritə görünüşü — 3 seçim (D-027): Hamı / Yalnız dostlar / Gizli. */
enum class MapVisibility(val label: String, val hint: String) {
    EVERYONE("Hamı", "Yerin xəritədə və match-də hərkəsə görünür"),
    FRIENDS("Yalnız dostlar", "Yerin yalnız səninlə dost olan istifadəçilərə görünür"),
    OFF("Gizli", "Xəritədə və match-də görünmürsən"),
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

    fun grant(amount: Int, note: String, id: String, at: LocalDateTime = LocalDateTime.now()) {
        balance += amount
        _ledger.add(XpLedgerEntry(id, venueId, true, amount, balance, note, at))
    }

    fun spend(amount: Int, note: String, id: String, at: LocalDateTime = LocalDateTime.now()) {
        balance -= amount
        _ledger.add(XpLedgerEntry(id, venueId, false, amount, balance, note, at))
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

    val isValid: Boolean get() = !used && LocalDateTime.now().isBefore(expiresAt)

    val secondsLeft: Int
        get() = java.time.Duration.between(LocalDateTime.now(), expiresAt).seconds
            .toInt().coerceIn(0, XpRules.XP_CARD_TTL_SECONDS)

    companion object {
        private var nonceSeq = 7

        /** Yeni kod — TTL 60 saniyə (D-026). Mock deterministik "random". */
        fun fresh(now: LocalDateTime = LocalDateTime.now()): XpCardCode {
            val seed = (now.nano / 1000) xor nonceSeq++.hashCode()
            val rnd = (seed and 0x7FFFFFFF) % 1000000
            return XpCardCode(
                nonce = "QSHUL-XP-${rnd.toString().padStart(6, '0')}-${(seed and 0x7FFFFFFF) % 97}",
                fallbackCode = rnd.toString().padStart(6, '0'),
                expiresAt = now.plusSeconds(XpRules.XP_CARD_TTL_SECONDS.toLong()),
            )
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

/** previewSpend nəticəsi (L.4-4). */
data class SpendPreview(
    val ok: Boolean,
    val discount: Double,
    /** null / 'balans' / 'minBill' / 'cap' / 'hesab' */
    val error: String?,
)
