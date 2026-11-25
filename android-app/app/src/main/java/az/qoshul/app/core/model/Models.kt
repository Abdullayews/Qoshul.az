package az.qoshul.app.core.model

/**
 * Domen modelləri — Flutter `core/models/models.dart` portu (D-036).
 * Yoxlanış: docs/audit-flutter-v0.7.0.md bölmə 4.
 *
 * A6 düzəlişi (audit): tədbirə `epochDay` əlavə olundu — real tarix sıralaması/
 * filtri üçün; `date` göstəri etiketi olaraq qalır (birebir görünüş).
 */

// ═════════════════════════ Tədbir ═════════════════════════

data class QoshulEvent(
    val id: String,
    val title: String,
    val category: String,
    val image: String,          // res açarı ("coffee" və s.) — ImageKey bax
    val city: String,
    val location: String,
    val lat: Double,
    val lng: Double,
    val distance: Double,
    val time: String,
    val date: String,           // göstəri etiketi: 'Şənbə', 'Bu gün', 'Noy 8'...
    val epochDay: Int,          // A6: real tarix (1970-01-01-dən gün sayı); keçmiş = Int.MIN_VALUE
    val past: Boolean,
    val capacity: Int,
    val participants: Int,
    val host: String,
    val description: String,
    val price: Int? = null,
    val ageLimit: String? = null,
    val language: String? = null,
    val level: String? = null,
    val format: String = "Oflayn",
    val approval: Boolean = false,
    val recurring: String = "Bir dəfəlik",
    val tags: List<String> = emptyList(),
    /** D-026 (L.3): bitirmə axını üçün partnyor obyekt id-si. */
    val venueId: String? = null,
) {
    val spotsLeft: Int get() = (capacity - participants).coerceAtLeast(0)
    val isFull: Boolean get() = participants >= capacity

    /** D-026: host dəyişimi üçün nüsxə (onboarding-də demo tədbir götürülür). */
    fun copyWithHost(host: String) = copy(host = host)
}

/** Şəkil açarları → res (Compose tərəfində həll olunur — core/data/ImageKeys). */
object ImageKey {
    const val COFFEE = "coffee"
    const val FRIENDS = "friends"
    const val PADEL = "padel"
    const val TECH = "tech"
}

// ═════════════════════════ İnsan ═════════════════════════

data class Person(
    val id: String,
    val firstName: String,
    val lastName: String,
    val image: String,
    val city: String,
    val status: String,          // student / working / ...
    val org: String,
    val interest: String,
    val match: Int,              // 0-100
    val reason: String,
    val age: Int = 24,
    val distanceKm: Double = 1.5,
    val rolInterests: List<String> = emptyList(),
    val likesBack: Boolean = false,
    /** A7 düzəlişi (audit): xəritə pini üçün sabit koordinat (saxta törəmə əvəzinə). */
    val mapLat: Double = 40.37,
    val mapLng: Double = 49.83,
) {
    val fullName: String get() = "$firstName $lastName"

    /** ROL: yalnız təxmini məsafə göstərilir. */
    val distanceLabel: String
        get() = if (distanceKm < 1) "~500 m" else "~${distanceKm.round()} km"

    private fun Double.round(): Int = kotlin.math.round(this).toInt()
}

// ═════════════════════════ Tərəfdaş məkan ═════════════════════════

data class Partner(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val city: String,
    val deal: String? = null,
    val hours: String,
    val image: String,
    val lat: Double,
    val lng: Double,
    // ROL E genişlənmələri
    val priceRange: String = "₼₼",
    val autoAccept: Boolean = true,
    val rating: Double = 4.6,
    val reviewsCount: Int = 12,
    val openFrom: Int = 9,
    val openTo: Int = 22,
    val discountPercent: Int = 0,
) {
    fun isOpenAt(hour: Int): Boolean = hour >= openFrom && hour < openTo
    val hasDiscount: Boolean get() = discountPercent > 0
}

// ═════════════════════════ Şəhər / kateqoriya ═════════════════════════

data class City(val name: String, val region: String, val lat: Double, val lng: Double)

/**
 * D-126 (v0.44.0, direktiv 2026-11-11: „bütün Azərbaycan şəhərləri mütləq
 * əlavə olunsun — event yaradılmasında və ana menyudakı axtarışda, A-Z
 * sıralı“): Azərbaycan əlifbası müqayisəçisi. String.compareTo hərfləri
 * Unicode kod nöqtəsinə görə sıralayır — əlifbamızın xüsusi hərfləri (ç, ə,
 * ğ, ı, ö, ş, ü) yanlış yerə düşür (məs. „Ə“ latın „z“-dən SONRA, „Ç“ isə
 * „C“-dən AYRI gəlir). Şəhər siyahıları A-Z göstəriləndə (CityPicker,
 * Yarat → Şəhər, ana menyu axtarışı) bu comparator MƏCBURİDİR.
 */
object AzCollation {
    // Azərbaycan əlifbası (32 hərf): a b c ç d e ə f g ğ h x ı i j k q
    // l m n o ö p r s ş t u ü v y z
    private const val ALPHABET = "abcçdeəfgğhxıijkqlmnoöprsştuüvyz"

    private fun rank(ch: Char): Int {
        val c = ch.lowercaseChar()
        val idx = ALPHABET.indexOf(c)
        return if (idx >= 0) idx else 100 + c.code
    }

    /** a < b → mənfi; Azərbaycan əlifba sırası ilə müqayisə. */
    fun compare(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        for (i in 0 until n) {
            val d = rank(a[i]) - rank(b[i])
            if (d != 0) return d
        }
        return a.length - b.length
    }
}

data class CategoryGroupData(val group: String, val items: List<String>)

// ═════════════════════════ Söhbət ═════════════════════════

data class ChatMessage(
    val sender: String,
    val body: String,
    val mine: Boolean = false,
    /**
     * M4 (audit v0.49): mesaj identikliyi — LazyColumn key-i. Əvvəl
     * "index-hashCode(body)" kompoziti işlədilirdi; yazı simulyasiyası
     * (typing effect) body-ni hər 24ms-də dəyişdiyindən key də dəyişir,
     * item silinib yenidən yaradılırdı (state/animasiya pozulurdu).
     * Defolt: unikal sayğac; typing .copy(body=…) ilə yayır → id qalır.
     * Bərabərlik eşitimən id DAHİL DEYİL (aşağıda override) — testlərdə
     * və eyni məzmunlu mesajlarda semantik bərabərlik qorunur.
     */
    val id: String = "m${counter.incrementAndGet()}",
) {
    override fun equals(other: Any?): Boolean =
        other is ChatMessage && other.sender == sender && other.body == body && other.mine == mine

    override fun hashCode(): Int = 31 * (31 * sender.hashCode() + body.hashCode()) + mine.hashCode()

    private companion object {
        val counter = java.util.concurrent.atomic.AtomicLong(0)
    }
}

// ═════════════════════════ Şəxsiyyət testi ═════════════════════════

data class PersonalityResult(
    val type: String,
    val summary: String,
    val traits: Map<String, Int>,
)

// ═════════════════════════ XP qaydaları (ROL F) ═════════════════════════

/**
 * XP qaydaları — ROL F dəyərləri (Flutter XpRules portu).
 * UI fazası: simulyasiya; backend fazasında server tərəfində (qayda 8).
 */
object XpRules {
    // XP hadisələri
    const val REGISTRATION_XP = 50
    const val PROFILE_COMPLETE_XP = 30
    const val PHONE_VERIFY_XP = 40
    const val DAILY_LOGIN_XP = 5
    const val STREAK_BONUS_XP = 30
    const val MATCH_XP = 10
    const val JOIN_XP = 10
    const val CHECK_IN_XP = 40
    const val HOSTING_XP = 60
    const val RESERVATION_XP = 30
    const val REVIEW_XP = 15
    const val INVITE_XP = 100

    // limitlər
    const val MATCH_XP_DAILY_CAP = 3
    const val REVIEW_XP_WEEKLY_CAP = 3
    const val HOSTING_MIN_PARTICIPANTS = 3
    const val NO_SHOW_LIMIT = 3
    const val NO_SHOW_RESTRICTION_DAYS = 7
    const val COUPON_VALIDITY_DAYS = 14

    // B2 (istifadəçi qərarı, 2026-11-07): ardıcıl seriya semantikası —
    // 1-2 gün gəlməyəndə seriya DONDURULUR (dəyişməz qalır),
    // 3+ gün gəlməyəndə SIFIRLANIR (yeni seriya 1-dən başlayır).
    const val STREAK_FREEZE_DAYS = 2
    const val STREAK_RESET_DAYS = 3

    // onboarding qaydaları (ROL B).
    // İstifadəçi qərarı (2026-11-07): maraq seçimi LIMITSİZ — köhnə
    // MAX_INTERESTS = 8 həddi ləğv olundu (D-074); minimum 3 qalır.
    const val MIN_AGE = 18
    const val MIN_INTERESTS = 3
    const val BIO_LIMIT = 150
    const val MAX_PHOTOS = 6

    // check-in qorunması (ROL F)
    const val CHECK_IN_MAX_DISTANCE_M = 200

    // D-026: Endirim XP (product-spec L.3)
    const val ORGANIZER_XP = 50
    const val TAGGED_PARTICIPANT_XP = 25
    const val XP_CARD_TTL_SECONDS = 60
    // D-151 (master-spec bölmə 7): iştirakçı təhlükəsizlik kodu 4 rəqəmdir
    const val FALLBACK_CODE_DIGITS = 4

    /** Kəşf peşəkar sıralaması (L.1) — səviyyə etiketi çəkisi. */
    fun proWeight(level: String?): Int = when (level) {
        "Peşəkar" -> 3
        "Orta" -> 2
        "Başlanğıc" -> 1
        else -> 0
    }

    // səviyyələr (ROL F)
    val rankMax = intArrayOf(99, 299, 699, 1499, 2999)
    val rankNames = listOf("Yeni", "Kəşfiyyatçı", "Aktiv", "Sosial", "Lider", "Əfsanə")

    fun rankForXp(xp: Int): String = rankNames[rankIndexForXp(xp)]

    /** Səviyyə indeksi (0=Yeni .. 5=Əfsanə). */
    fun rankIndexForXp(xp: Int): Int {
        for (i in rankMax.indices) if (xp <= rankMax[i]) return i
        return rankNames.size - 1
    }

    data class XpProgress(val nextName: String?, val toNext: Int, val pct: Int)

    /**
     * {nextName, toNext, pct} — web xpProgress portu.
     *
     * A4 invariantı (M0): **pct == 100 ⟺ toNext == 0**.
     * Keçmişdə xp tam rankMax sərhəddinə çatanda (99/299/699/1499/2999)
     * pct = 100 VƏ toNext = 1 eyni anda qayıdırdı ("100% tamamlandıb, amma
     * 1 XP qalıb" ziddiyyəti). Artıq sərhəddə pct 99-da saxlanılır —
     * toNext düzgün qalır (yalan danılmır), 100% yalnız terminal haldadır
     * (Əfsanə, xp ≥ 3000). Rust paraleli M2-də proptest ilə sübut olunacaq.
     */
    fun xpProgress(xp: Int): XpProgress {
        var lower = 0
        for (i in rankMax.indices) {
            val max = rankMax[i]
            if (xp <= max) {
                val span = max - lower
                val toNext = max + 1 - xp
                val pct = (((xp - lower) * 100.0 / span).toInt())
                    .coerceIn(0, if (toNext == 0) 100 else 99)
                return XpProgress(rankNames[i + 1], toNext, pct)
            }
            lower = max
        }
        return XpProgress(null, 0, 100)
    }

    /**
     * ROL B: 18+ yoxlaması.
     * A2: `now` məcburidir — gizli LocalDate.now() bypass-ı aradan qalxıb;
     * çağıran vahid clock-dan ötürür.
     */
    fun isAdult(birth: LocalDate, now: LocalDate): Boolean {
        var age = now.year - birth.year
        val hadBirthday = now.monthValue > birth.monthValue ||
            (now.monthValue == birth.monthValue && now.dayOfMonth >= birth.dayOfMonth)
        if (!hadBirthday) age--
        return age >= MIN_AGE
    }
}

/** JVM/Android ortaq tarix tipi (java.time — minSdk 24-də desugaring olmadan da
 *  API 26+; minSdk 24 üçün coreLibraryDesugaring aktiv edilib — app/build.gradle.kts). */
typealias LocalDate = java.time.LocalDate

// ═════════════════════════ Nişanlar (ROL F) ═════════════════════════

data class BadgeDef(val id: String, val name: String, val description: String)

// ═════════════════════════ Task Center (master-spec §9, D-155) ═════════════════════════

/**
 * Vizual Tapşırıqlar Mərkəzi — 3 dərəcəli iyerarxiya:
 *   Novice (onboarding) · Daily (gündəlik) · Advanced (irəli).
 * Mükafatlar „Qoşa Coin“ (XP) ilə — XpRules dəyərləri ilə sinxron.
 */
enum class TaskTier(val label: String, val hint: String) {
    NOVICE("Novice — Başlanğıc", "Onboarding və profil doldurulması"),
    DAILY("Daily — Gündəlik", "Gündəlik aktivliyin saxlanılması"),
    ADVANCED("Advanced — İrəli", "Yüksək sosial aktivlik"),
}

data class TaskDef(
    val id: String,
    val tier: TaskTier,
    val title: String,
    val rewardXp: Int,
    /** Award açarları — engine.award() idempotentdir (bir dəfə verilir). */
    val awardKey: String,
)

/** Tapşırıq kataloqu — mükafatlar XpRules sabitləridir (spec cədvəli). */
val taskDefs = listOf(
    // ── Novice: profil doldurulması (+50 planı spec-də; bizim iqtisadiyyatda
    //    qeydiyyat+profil ayrı-ayrı verilir — spec ilə uyğunlaşdırılmış) ──
    TaskDef("profile-photo", TaskTier.NOVICE, "Profil şəklini yüklə", XpRules.PROFILE_COMPLETE_XP, "profile-full"),
    TaskDef("test-16p", TaskTier.NOVICE, "16Personalities testini keç", XpRules.PHONE_VERIFY_XP, "personality-set"),
    // ── Daily: gündəlik aktivlik (+10 planı; login+streak sistemi birləşdirilir) ──
    TaskDef("daily-login", TaskTier.DAILY, "Hər gün proqrama daxil ol", XpRules.DAILY_LOGIN_XP, "login"),
    TaskDef("streak-7", TaskTier.DAILY, "7 günlük seriya tamamla", XpRules.STREAK_BONUS_XP, "streak-7"),
    TaskDef("browse-event", TaskTier.DAILY, "Bir tədbirə bax və qoşul", XpRules.JOIN_XP, "join"),
    // ── Advanced: yüksək sosial aktivlik (+100 planı; təşkilatçılıq) ──
    TaskDef("host-event", TaskTier.ADVANCED, "Həftədə 2 tədbir təşkil et", XpRules.HOSTING_XP, "hosting"),
    TaskDef("checkin-event", TaskTier.ADVANCED, "Real tədbir iştirakını təsdiqlə", XpRules.CHECK_IN_XP, "check-in"),
    TaskDef("invite-friend", TaskTier.ADVANCED, "Dostunu dəvət et", XpRules.INVITE_XP, "invite"),
)

/**
 * Gərginləşdirilmiş Nailiyyətlər (spec §9): nişanlar yalnız həqiqi sosial
 * aktivlikdən sonra kiliddən çıxır — kateqoriyalara görə tələb sayı artırılıb.
 */
val badgeDefs = listOf(
    BadgeDef("firstEvent", "İlk tədbir", "İlk tədbirinə qoşul"),
    BadgeDef("threeEvents", "3 tədbir", "3 tədbirdə iştirak et (birincidən sonra)"),
    BadgeDef("tenEvents", "10 tədbir", "10 tədbirdə iştirak et — real check-in ilə"),
    BadgeDef("host", "Təşkilatçı", "3 tədbir təşkil et və tamamla"),
    BadgeDef("traveler", "Səyyah", "Başqa şəhərdə tədbirə qoşul"),
    BadgeDef("nightOwl", "Gecə quşu", "Axşam 20:00-dan sonra check-in"),
    BadgeDef("verified", "Təsdiqlənmiş", "Telefon nömrəsini təsdiqlə"),
    BadgeDef("social-10", "Sosial", "10 yeni matç qazan"),
    BadgeDef("reviewer", "Tənqidçi", "3 məkana şərh yaz"),
)

// ═════════════════════════ Assistant ═════════════════════════

data class EventProposal(
    val title: String,
    val category: String,
    val date: String,
    val time: String,
    val capacity: Int,
    val location: String,
    val description: String,
    val partner: String = "",
)

data class AssistantParse(val text: String, val proposal: EventProposal?)

/**
 * Assistant [TƏDBİR] parser — web assistant-event.ts portu.
 * (JSON parse — kotlinx.serialization əvəzinə sadə əl üsulu, sıfır asılılıq.)
 *
 * D-061 "nə olar əgər": format gözlənilmədən fərqli olsa da çökmür —
 * Safety.of fallback-i mətni olduğu kimi qaytarır, təklif null olur
 * (çağıran tərəf artıq null-gözənilən axında işləyir).
 */
fun parseAssistantEvent(text: String): AssistantParse =
    az.qoshul.app.core.safety.Safety.of(
        tag = "assistant-parse",
        fallback = AssistantParse(text, null),
    ) { parseAssistantEventUnsafe(text) }

private fun parseAssistantEventUnsafe(text: String): AssistantParse {
    val i = text.indexOf("[TƏDBİR]")
    if (i < 0) return AssistantParse(text, null)
    val clean = text.substring(0, i).trim()
    val raw = text.substring(i + 8).trim()
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start < 0 || end <= start) return AssistantParse(clean, null)
    val json = raw.substring(start, end + 1)
    fun str(key: String): String {
        val k = json.indexOf("\"$key\"")
        if (k < 0) return ""
        val colon = json.indexOf(':', k)
        if (colon < 0) return ""
        val q1 = json.indexOf('"', colon)
        if (q1 < 0) return ""
        var q2 = q1 + 1
        while (q2 < json.length && json[q2] != '"') q2++
        return json.substring(q1 + 1, q2)
    }

    // D-061: sitatsız ədəd dəstəyi — əvvəl int() yalnız str() ilə oxuyurdu:
    // "capacity":10 (sitatsız) halında str() növbəti açarın sitatını tapırdı
    // və dəyər HƏMİŞƏ default qalırdı (real assistant cavablarında 10 yerinə 8).
    fun int(key: String, def: Int): Int {
        // 1) sitatsız ədəd: "key":10,
        val k = json.indexOf("\"$key\"")
        if (k >= 0) {
            val colon = json.indexOf(':', k)
            if (colon >= 0) {
                val rest = json.substring(colon + 1).trim()
                Regex("^-?\\d+").find(rest)?.let { m ->
                    m.value.toIntOrNull()?.let { return it }
                }
            }
        }
        // 2) sitatlı ədəd: "key":"10"
        return str(key).toIntOrNull() ?: def
    }
    val proposal = EventProposal(
        title = str("title"),
        category = str("category"),
        date = str("date"),
        time = str("time"),
        capacity = int("capacity", 8),
        location = str("location"),
        description = str("description"),
        partner = str("partner"),
    )
    // D-061 "nə olar əgər": boş JSON ({}) və ya başlıqsız qalıq — təklif
    // YOX sayılır (boş "Təsdiqləndi: " kartı əvəzinə).
    return AssistantParse(clean, if (proposal.title.isBlank()) null else proposal)
}
