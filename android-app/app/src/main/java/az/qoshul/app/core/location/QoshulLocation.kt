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
            try {
                providers.forEach {
                    lm.requestLocationUpdates(it, 500L, 0f, listener, Looper.getMainLooper())
                }
            } catch (_: SecurityException) {
                close()
                return@callbackFlow
            } catch (_: IllegalArgumentException) {
                close()
                return@callbackFlow
            }
            awaitClose { providers.forEach { try { lm.removeUpdates(listener) } catch (_: Exception) {} } }
        }
        return withTimeoutOrNull(timeoutMs) {
            fixes.first()
        }
    }
}
