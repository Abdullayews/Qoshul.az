package az.qoshul

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * D-031/D-032: Native Android qatı (Kotlin).
 *
 * Instagram-paradigmalı optimallaşdırmanın ilk addımı:
 *  - Edge-to-edge güvəncəsi: Dart tərəfi SystemUiMode.edgeToEdge qaldırır,
 *    bura dekor-fits-system-windows-i söndürərək insets hesabını kənar
 *    "sıçrayış"lardan qoruyur (tam ekran düzgün görünür).
 *  - MethodChannel "qoshul/native": immersive keçidləri (media baxışı üçün)
 *    və cihaz performans profili (Java tərəfindən, NativePerfInfo.collect).
 */
class MainActivity : FlutterActivity() {

    private val channelName = "qoshul/native"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // D-031: məzmun bütün sistem barlarının ALTINDAN axır.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "perfInfo" -> result.success(NativePerfInfo.collect(this))
                    "enterImmersive" -> {
                        setSystemBarsVisible(false)
                        result.success(true)
                    }
                    "exitImmersive" -> {
                        setSystemBarsVisible(true)
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    /** Barları gizlət/göstər — swipe ilə müvəqqəti geri gəlirlər. */
    private fun setSystemBarsVisible(visible: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
