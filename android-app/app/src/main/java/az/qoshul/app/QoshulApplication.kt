package az.qoshul.app

import android.app.Application
import android.util.Log
import az.qoshul.app.core.safety.Safety
import az.qoshul.app.core.ui.MapLogGate

/**
 * Qoshul.az native — Application.
 *
 * DI (D-036 qərarı): tətbiq miqyasında sadə AppContainer (manual DI).
 * Hilt bu mərhələdə RƏDD edilib: tək Activity + tək mərkəzi state holder
 * üçün annotation emalı (kapt/ksp) yalnız build vaxtını artırardı;
 * arxitektura backend fazasında (çoxlu ViewModel + repository) yenidən
 * qiymətləndiriləcək. Qərar docs/decisions.md D-036-də.
 */
class QoshulApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // D-061: xəritə log qapısı — MapLibre tile/stil retry sellənməsini
        // (ofiayn şəbəkədə "dayanmayan" error axını) rate-limit ilə kəsir.
        // İlk error görünür, təkrarları udulur. Heç vaxt çökmür (runCatching).
        MapLogGate.install()
        // D-061: Safety fallback-ləri loglayır (JVM testlərdə sink null-dur,
        // səssiz — deterministik testlər).
        Safety.onCaught = { tag, t -> Log.e("QoshulSafety", "$tag: ${t.message}", t) }
    }

    companion object {
        lateinit var instance: QoshulApplication
            private set
    }
}
