package az.qoshul.app.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * REAL lokasiya mənbəyi (D-143, direktiv 2026-11-14: „kiçiltmə
 * düyməsinin yerinə telefondan alınan dataya görə istifadəçinin
 * lokasiyasına getməyi seç. istifadəçi lokasiyası mock data ola bilməz“).
 *
 * Framework [LocationManager] (GPS + network provayderləri) — Play
 * Services asılılığı YOXDUR, offline GPS fix verir.
 *
 * MOCK MÜDAFİƏSİ: [Location.isFromMockProvider] (emulyatorun „set
 * location“-i, mock provayderlər, GPS spoof tətbiqləri) true olan fix
 * RƏDD olunur — kamera heç vaxt saxta koordinata uçmur. Mock fix bütün
 * provayderlərdən gələrsə [LocateResult.Denied] qaytarılır (fallback
 * xətası kimi).
 *
 * JVM (Robolectric) test mühitində provayderlər yoxdur →
 * [LocateResult.Unavailable] — crash YOX (MapScreen test qaydası ilə
 * paritet).
 */
object QoshulLocation {

    /** Lokasiya axtarışının nəticəsi. */
    sealed interface LocateResult {
        /** Real fix: enli-dərəcə, uzunluq-dərəcə. */
        data class Found(val lat: Double, val lng: Double) : LocateResult

        /** İcazə verilməyib (və ya rədd edilib). */
        data object Denied : LocateResult

        /** İcazə var, amma fix yoxdur (provayder söndürülüb / vaxtaşımı). */
        data object Unavailable : LocateResult
    }

    /** FINE (və ya COARSE) icazəsi verilibmi? */
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Bir dəfəlik ən yaxşı REAL fix — GPS/network provayderlərinin
     * son bilinən mövqeyi (tezsaxlanan, təzə fix gözləmək lazım deyil;
     * yenidən düymə basılanda listener 5 saniyə təzə fix də dinləyir).
     *
     * Sıra: GPS last-known → network last-known → 5 s lik canlı gözləmə.
     * Hər namizəd mock-dursa keçilir.
     */
    suspend fun locateOnce(context: Context): LocateResult {
        if (!hasPermission(context)) return LocateResult.Denied
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return LocateResult.Unavailable

        // 1) last-known (anı, enerji sərfi sıfır)
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            val last = try {
                lm.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null // icazə aradan götürüldü (edge-case) — Davam
            } catch (_: IllegalArgumentException) {
                null // provayder mövcud deyil (emulyator konfiqurasiyası)
            }
            if (last != null && !last.isFromMockProvider) {
                return LocateResult.Found(last.latitude, last.longitude)
            }
        }

        // 2) canlı fix — 5 s gözlə (GPS soyuq başlanğıcında tez fix üçün
        // network provayderi də dinlənilir; ilk REAL fix gələn kimi qayıt)
        val live = awaitProviderFix(lm, timeoutMs = 5_000)
        return when {
            live != null && !live.isFromMockProvider ->
                LocateResult.Found(live.latitude, live.longitude)
            else -> LocateResult.Unavailable // mock-only və ya heç nə
        }
    }

    /**
     * G.1 (D-170, Snapchat məntiqi): DAVAMLI lokasiya axını — dəqiqlik
     * filtri (yalnız accuracy ≤ [MAX_ACCURACY_M] fix-lər), hərəkət
     * yumşaltması (əvvəlki göndərilən nöqtədən ≥ [MIN_MOVE_M] uzaq)
     * və throttle ([MIN_INTERVAL_MS]-dən tez olmayan) ilə.
     *
     * İstehsal: MapScreen açıq olduqca toplanır → engine →
     * LocationUpdateIntent → fn_update_location (serverdəki yaxınlıq
     * sorğuları üçün). Mock fix-lər axında da RƏDD olunur.
     */
    data class StreamFix(
        val lat: Double,
        val lng: Double,
        /** Dəqiqlik radiusu, metr (dairənin ölçüsü üçün). */
        val accuracyM: Float,
        /** Dərəcə (0-360); istiqamət göstərmək üçün (null = bilinmir). */
        val bearing: Float?,
        /** Fix vaxtı (epoch millis) — köhnəlmiş fixi gizlətmək üçün. */
        val atMs: Long,
    )

    /** Dəqiqlik qapısı: bundan kobud fix-lər süzülür (Snapchat ~50m). */
    const val MAX_ACCURACY_M = 50f

    /** Minimum hərəkət (metr) — kiçik titrəmələr göndərilmir. */
    const val MIN_MOVE_M = 15f

    /** Minimum interval (ms) — throttle. */
    const val MIN_INTERVAL_MS = 4_000L

    /** Fix bu qədər köhnədirsə „stale“ sayılır (xəritədə gizlədilir). */
    const val STALE_MS = 30_000L

    /**
     * Davamlı axın — GPS+network provayderləri, minInterval/minDistance
     * ilə. Hər fix: mock yoxlaması → dəqiqlik filtri → hərəkət/hava
     * yumşaltması → [StreamFix] emit. İcazə yoxdursa axın DƏRHAL bağlanır.
     */
    fun locationStream(context: Context) = callbackFlow {
        // H1 (D-145): return@callbackFlow awaitClose-i ATLAYIR — qadağan;
        // early-exit close() + else-zənciri ilə yazılır
        val lm = if (hasPermission(context)) {
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        } else null
        if (lm != null) {

        var lastSentLat = 0.0
        var lastSentLng = 0.0
        var lastSentAt = 0L

        val listener = LocationListener { location ->
            // 1) mock fix RƏDD (G.4)
            if (location.isFromMockProvider) return@LocationListener
            // 2) dəqiqlik filtri
            if (location.accuracy > MAX_ACCURACY_M) return@LocationListener
            // 3) throttle + hərəkət yumşaltması
            val nowMs = System.currentTimeMillis()
            val movedEnough = lastSentAt == 0L || distanceM(
                lastSentLat, lastSentLng, location.latitude, location.longitude) >= MIN_MOVE_M
            val timeOk = nowMs - lastSentAt >= MIN_INTERVAL_MS
            if (lastSentAt != 0L && !(movedEnough && timeOk)) return@LocationListener
            lastSentLat = location.latitude; lastSentLng = location.longitude
            lastSentAt = nowMs
            trySend(
                StreamFix(
                    lat = location.latitude, lng = location.longitude,
                    accuracyM = location.accuracy,
                    bearing = location.bearing.takeIf { it != 0f },
                    atMs = location.time,
                )
            )
        }

        val registered = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { p ->
                try {
                    lm.requestLocationUpdates(p, 2_000L, 10f, listener, Looper.getMainLooper())
                    p
                } catch (_: SecurityException) { null }
                catch (_: IllegalArgumentException) { null }
            }
        if (registered.isEmpty()) close()
        awaitClose { registered.forEach { try { lm.removeUpdates(listener) } catch (_: Exception) {} } }
        } else {
            // icazə yoxdur / LocationManager yoxdur — axın dərhal bağlanır
            close()
            awaitClose { }
        }
    }

    /** İki nöqtə arası məsafə (Haversine, metr) — yumşaltma üçün. */
    fun distanceM(la1: Double, lo1: Double, la2: Double, lo2: Double): Double {
        val r = 6_371_000.0
        val dLa = Math.toRadians(la2 - la1)
        val dLo = Math.toRadians(lo2 - lo1)
        val a = Math.sin(dLa / 2) * Math.sin(dLa / 2) +
            Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) *
            Math.sin(dLo / 2) * Math.sin(dLo / 2)
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    /** İcazə istəyərkən istifadəçiyə göstərilən izah (a11y/snackbar üçün). */
    const val RATIONALE =
        "Yaxınlıqdakı məkanları göstərmək və xəritədə mövqeyinizə getmək üçün lokasiya icazəsi lazımdır."

    /**
     * Provayderlərdən ilk (mock olmayan) fix — [callbackFlow] üzərində
     * [kotlinx.coroutines.withTimeoutOrNull]: ya real fix, ya vaxtaşımı
     * (null). awaitClose listener-i qeydiyyatdan çıxarır (lek yoxdur).
     */
    private suspend fun awaitProviderFix(lm: LocationManager, timeoutMs: Long): Location? {
        val providers = try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { lm.allProviders.contains(it) }
        } catch (_: Exception) {
            emptyList()
        }
        if (providers.isEmpty()) return null

        val fixes = callbackFlow {
            val listener = LocationListener { location ->
                // mock fix kanala düşmür — kamera saxta koordinata uçmur
                if (!location.isFromMockProvider) trySend(location)
            }
            // H1 (audit v0.49): provayderlər FƏRDİ qeydiyyata alınır — biri
            // partlasa (Security/IllegalArgument) qalanın listener-i awaitClose
            // ilə təmizlənir. Əvvəl: forEach ümumi try içində idi, ikinci
            // provayder atanda erkən return awaitClose-i ATLAYIRDI və uğurla
            // qeydiyyata alan birincinin listener-i sızırdı.
            val registered = providers.mapNotNull { p ->
                try {
                    lm.requestLocationUpdates(p, 500L, 0f, listener, Looper.getMainLooper())
                    p
                } catch (_: SecurityException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            if (registered.isEmpty()) close()
            awaitClose {
                registered.forEach { try { lm.removeUpdates(listener) } catch (_: Exception) {} }
            }
        }
        return withTimeoutOrNull(timeoutMs) {
            fixes.first()
        }
    }
}
