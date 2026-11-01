package az.qoshul.app.core.safety

import kotlinx.coroutines.CancellationException

/**
 * "Nə olar əgər…" qoruma qatı (D-061 — istifadəçi direktivi 2026-11-01:
 * "hər function üçün bu olsa nə olar bu olsa nə olar halını placeholder
 * kimi doldur").
 *
 * PRINSİP: heç bir köməkçi funksiya tətbiqi çökməməlidir — gözlənilməz
 * girişdə (null, boş siyahı, xarab format, limitsiz dövr) TƏYİN OLUNMUŞ
 * fallback dəyər qaytarır və xətanı loglayır. "Placeholder" = hər
 * uğursuzluq halının konkret cavabı:
 *
 *   - parse alınmır      → mətn olduğu kimi, təklif null
 *   - tarix hesablansa   → "—" etiketi
 *   - hesab tapılmadı    → null (çağıran artıq null-gözənilən axın verir)
 *   - format xətası      → default format
 *
 * CancellationException ÖTÜRÜLÜR (korutin ləğvi normal həyat dövrüdür,
 * "xəta" deyil — udulsa cancellation coop pozular).
 *
 * Sink: [onCaught] app-də android.util.Log-a bağlanır (QoshulApplication),
 * JVM testlərində null — səssiz fallback, test deterministik qalır.
 */
object Safety {

    /** Yakalanan xətalar üçün log sink (app: Log.e; test: null). */
    @Volatile
    var onCaught: ((tag: String, throwable: Throwable) -> Unit)? = null

    /**
     * [block]-u işə salır; exception olarsa [fallback] qaytarır və
     * (sink varsa) xətanı "QoshulSafety" kimi loglayır.
     */
    fun <T> of(tag: String, fallback: T, block: () -> T): T =
        try {
            block()
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            runCatching { onCaught?.invoke(tag, t) }
            fallback
        }

    /** Unit qaytaran bloklar üçün qısa forma (fallback = Unit). */
    fun run(tag: String, block: () -> Unit) {
        of(tag, Unit, block)
    }
}
