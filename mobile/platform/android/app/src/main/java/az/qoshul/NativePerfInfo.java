package az.qoshul;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import java.util.HashMap;
import java.util.Map;

/**
 * D-032: Cihaz performans profili (Java).
 *
 * Dart tərəfi adaptiv keyfiyyət qərarı verir (zəif cihazda Liquid Glass
 * blur söndürülür → 60fps). Instagram-ın cihaz-əsaslı render keyfiyyəti
 * yanaşmasının sadə ekvivalenti: cihaz nə verirsə, UI o qədər effek çəkir.
 */
public final class NativePerfInfo {

    private NativePerfInfo() {
        // utility sinifi — instansiya yoxdur
    }

    /**
     * @return memoryClass (MB, per-app heap həddi), lowRam (zəif cihaz
     *         bayrağı), cores (CPU nüvə sayı), sdk (API səviyyəsi)
     */
    public static Map<String, Object> collect(Context context) {
        Map<String, Object> out = new HashMap<>();
        ActivityManager am =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            out.put("memoryClass", am.getMemoryClass());
            out.put("lowRam", am.isLowRamDevice());
        }
        out.put("cores", Runtime.getRuntime().availableProcessors());
        out.put("sdk", Build.VERSION.SDK_INT);
        return out;
    }
}
