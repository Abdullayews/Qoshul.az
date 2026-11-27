package az.qoshul.app.core.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import az.qoshul.app.core.safety.Safety
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * D-175 (ikinci dalğa): „yazır...“ göstəricisi — YALNIZ Supabase Realtime.
 *
 * Köhnə davranış (SİLİNDİ): öz mesajını göndərəndən sonra 1400ms saxta
 * taymer yandırırdı — qarşı tərəf heç nə yazmasa da „yazır...“ görünürdü.
 *
 * Yeni davranış:
 *  • TYPING BROADCAST: hər chat üçün realtime kanal (`typing:chat:{key}`);
 *    istifadəçi yazanda `typing` hadisəsi yayımlanır (throttle 2s),
 *    qarşı tərəfdən gələn `typing` hadisəsi 6s göstərilir (TTL — stale
 *    göstəriş yoxdur; mesaj gələndə refresh typing-i əvəz edir).
 *  • MESSAGES INSERT: qarşı tərəf mesaj göndərəndə typing-in sönməsi
 *    refresh (SupabaseReader) mesaj siyahısını yeniləyir; TTL zaten
 *    qısa müddətli göstərişi təmizləyir.
 *
 * Konfiqurasiya yoxdursa (PLACEHOLDER/test) heç nə yayımlanmır və
 * göstəriş heç vaxt yanmır (real-time olmadan chat özü də offline-dur).
 */
object TypingIndicator {

    /** Görünən „yazır...“ vəziyyətləri: chatKey → son hadisə vaxtı (mono ms). */
    private val lastSeen = mutableMapOf<String, Long>()

    /** Yayımlama throttle pəncərəsi: chatKey → son göndərmə (mono ms). */
    private val lastSent = mutableMapOf<String, Long>()

    private const val SHOW_TTL_MS = 6000L
    private const val SEND_THROTTLE_MS = 2000L

    /**
     * „Yazır...“ göstəricisi — chat açıq olan müddətdə:
     *  (a) kanala qoşulub `typing` broadcast hadisələrini dinləyir;
     *  (b) 500ms polling ilə TTL keçib-keçmədiyini yoxlayır.
     */
    @Composable
    fun rememberTyping(chatKey: String): Boolean {
        var typing by remember { mutableStateOf(false) }
        // hadisə dinləyicisi — chatKey dəyişəndə yenidən qur
        LaunchedEffect(chatKey) {
            typing = false
            listenTyping(chatKey)
        }
        // TTL sayğacı — hadisə gələndə yanır, 6s sonra sönməlidir
        LaunchedEffect(chatKey) {
            while (true) {
                val t = lastSeen[chatKey]
                typing = t != null && (System.nanoTime() / 1_000_000 - t) < SHOW_TTL_MS
                delay(500)
            }
        }
        return typing
    }

    /** Öz yazmağımızı yayımla (throttle 2s — hər hərfdə broadcast yox). */
    fun broadcastTyping(chatKey: String, scope: kotlinx.coroutines.CoroutineScope) {
        val now = System.nanoTime() / 1_000_000
        val last = lastSent[chatKey] ?: 0L
        if (now - last < SEND_THROTTLE_MS) return
        lastSent[chatKey] = now
        val c = SupabaseBackend.client ?: return
        Safety.run("typing:broadcast") {
            val ch = c.realtime.channel("typing:chat:$chatKey")
            scope.launch {
                runCatching {
                    // typed broadcast: JsonPrimitive payload JsonObject-a çevrilir
                    ch.broadcast(
                        event = "typing",
                        message = mapOf("chat" to chatKey),
                    )
                }.onFailure { Safety.onCaught?.invoke("typing:broadcast", it) }
            }
        }
    }

    /** Qarşı tərəfdən typing hadisələrini dinlə (LaunchedEffect daxilində). */
    private suspend fun listenTyping(chatKey: String) {
        val c = SupabaseBackend.client ?: return
        val ch = c.realtime.channel("typing:chat:$chatKey")
        runCatching { ch.subscribe(blockUntilSubscribed = true) }
            .onFailure {
                Safety.onCaught?.invoke("typing:subscribe", it)
                return // kanal açılmadı — typing göstərilmir (səssiz false)
            }
        ch.broadcastFlow<Map<String, String>>("typing").collect {
            lastSeen[chatKey] = System.nanoTime() / 1_000_000
        }
    }
}
