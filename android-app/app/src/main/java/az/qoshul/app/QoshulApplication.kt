package az.qoshul.app

import android.app.Application

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
    }

    companion object {
        lateinit var instance: QoshulApplication
            private set
    }
}
