package az.qoshul.app.core.ui

import android.util.Log
import org.maplibre.compose.logging.MapLogLevel
import org.maplibre.compose.logging.MapLogRecord
import org.maplibre.compose.logging.MapLogger
import org.maplibre.compose.logging.MapLogging

/**
 * Xəritə log qapısı (D-061 — istifadəçi direktivi 2026-11-01: "kodun təməlini
 * yoxla, errorlar davam edir hətta dayanmır").
 *
 * PROBLEM: MapLibre native mühərriki oflayn/zəif şəbəkədə tile və stil
 * yükləmələrini SONSUZ yenidən sınayır — hər uğursuzluq ayrıca error
 * qeydi kimi logcat-ə axır ("dayanmır"). Xəritə tabı açıq qaldıqca minlərlə
 * təkrarlanan "Failed to load tile …" sətri yığılır.
 *
 * HƏLL (iki qat):
 *  1) [MapLogGate] — xəritə logger-ini örtür: YALNIZ Error səviyyəsi keçir,
 *     eyni imzalı mesajı 15 saniyəlik pəncərədə 1 dəfə, hər pəncərədə
 *     maksimum 3 qeyd ötürür. İlk baş verən error GİZLƏNMİR (platforma
 *     logger-inə ötürülür) — yalnız təkrar sellənməsi dayandırılır.
 *  2) MapScreen-də stil yüklənməsi uğursuz olsa aydın fallback UI (orada).
 *
 * İmza normalizasiyası: rəqəmlər "#" ilə əvəzlənir — "Failed to load tile
 * 13/4523/2891" və "…13/4523/2892" eyni imza sayılır (koordinat fərqi
 * dedup-u əhəmiyyətsiz etməsin).
 *
 * Quraşdırma: QoshulApplication.onCreate — ilk xəritə istifadəsindən əvvəl.
 * runCatching: logger API-si gələcək MapLibre yeniləmələrində dəyişərsə
 * tətbiq HEÇ vaxt bunun üzündən açılmaz (safety-first, D-061 ruhu).
 */
object MapLogGate {

    private const val TAG = "QoshulMapLogGate"

    /** Eyni imzalı mesajın təkrarının udulduğu pəncərə. */
    private const val WINDOW_MS = 15_000L

    /** Bir pəncərədə ötürülən maksimum error sayı (sellənmə qorunması). */
    private const val MAX_PER_WINDOW = 3

    @Volatile
    private var installed = false

    private val lock = Any()
    private val seen = HashMap<String, Long>()
    private var windowStart = 0L
    private var forwarded = 0

    /** Platforma logger-inə ötürülən qeyd sayı (test üçün). */
    val forwardedCount: Int get() = synchronized(lock) { forwarded }

    /**
     * Xəritə logger qapısını quraşdırır (idempotent). Heç bir halda
     * exception çıxmır — logger APIsi əlçatmaz/fərqli olsa da tətbiq açılır.
     */
    fun install() {
        if (installed) return
        installed = true
        runCatching {
            val platform = MapLogging.platformLogger
            MapLogging.logger = object : MapLogger {
                override val minLevel: MapLogLevel = MapLogLevel.Error

                override fun log(record: MapLogRecord) {
                    // Səviyyə filtri (minLevel-in özü də süzgəcdir, amma
                    // bu təbəqə müstəqil işləməlidir — "nə olar əgər"
                    // yanaşması, D-061): Error altı heç keçmir.
                    if (record.level < MapLogLevel.Error) return

                    val now = System.currentTimeMillis()
                    if (!allow(now, signature(record.category, record.message))) return
                    // İlk baş verən error real olaraq görünür — problemi
                    // gizlətmirik, yalnız sonsuz təkrarını kəsirik.
                    runCatching { platform.log(record) }
                        .onFailure { Log.w(TAG, "platform logger ötürməsi alınmadı", it) }
                }
            }
        }.onFailure {
            Log.w(TAG, "MapLogGate quraşdırılmadı — xəritə logları standart axır", it)
        }
    }

    /** [true] qaytarırsa qeyd ötürülür; pəncərə/imza limitləri burada. */
    internal fun allow(now: Long, sig: String): Boolean = synchronized(lock) {
        if (now - windowStart >= WINDOW_MS) {
            windowStart = now
            forwarded = 0
            seen.clear()
        }
        if (forwarded >= MAX_PER_WINDOW) return false
        val last = seen[sig]
        if (last != null && now - last < WINDOW_MS) return false
        seen[sig] = now
        forwarded++
        return true
    }

    /** Eyni xətalı fərqli koordinatları birləşdirən imza. */
    internal fun signature(category: String?, message: String?): String =
        "${category.orEmpty()}|${message.orEmpty().replace(Regex("\\d+"), "#")}"

    /** Yalnız test üçün daxili sayğacın sıfırlanması. */
    internal fun resetForTest() {
        synchronized(lock) {
            seen.clear()
            windowStart = 0L
            forwarded = 0
        }
    }
}
