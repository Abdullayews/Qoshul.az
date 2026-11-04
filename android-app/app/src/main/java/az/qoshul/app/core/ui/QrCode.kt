package az.qoshul.app.core.ui

import az.qoshul.app.core.rust.QoshulNative
import az.qoshul.app.core.safety.Safety
import az.qoshul.app.core.theme.Q
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * REAL QR kodu (D-041 → M3/D-081) — Rust `qrcodegen` 1.8.0 (Nayuki-nin
 * rəsmi Rust portu) JNI üzərindən. Vendored Java (io.nayuki.qrcodegen)
 * M3-də TAMAMEN SİLİNDİ — birinci tərəf kodu 100% Kotlin+Rust.
 *
 * Arxitektura: həndəsə (QrGeometry) Rust-da yaranır (qrEncodeUi); JVM-də
 * host .so (`-Dqoshul.jni.path`), Android-də jniLibs (CI cargo-ndk). .so
 * yoxdursa DEQRADE fallback — sabit "QOSHUL" v1-L matrisi (Rust-dan
 * generasiya olunmuş konstant; skan oluna bilən real QR-dur, yalnız
 * payload dəyişmir) + bir dəfə xəbərdarlıq.
 *
 * Stil: data modulları standart kvadrat (maksimum skan etibarlılığı),
 * finder "gözləri" yumşaq künc radiusu ilə (brend görünüş). ECC = MEDIUM.
 */

/** QR həndəsəsi — modul matrisası (finder/timing/format daxil, quiet zone xaric). */
class QrGeometry(
    /** Modul sayı bir tərəf üzrə (versiya 1 = 21). */
    val size: Int,
    private val modules: BooleanArray,
) {
    fun module(x: Int, y: Int): Boolean = modules[y * size + x]

    companion object {
        /**
         * Payload-dan QR həndəsəsi. Versiya Rust-da avtomatik seçilir
         * (1..40, MEDIUM; sığmırsa genişlənir — QrCode.kt M2 pariteti).
         */
        fun of(payload: String, @Suppress("UNUSED_PARAMETER") maxVersion: Int = 12): QrGeometry {
            val text = payload.ifBlank { "QOSHUL" }
            if (QoshulNative.available) {
                val arr = Safety.of("qr", null as QrGeometry?) {
                    val data = QoshulNative.qrEncodeUi(text)
                    val n = data[0]
                    QrGeometry(n, BooleanArray(n * n) { data[1 + it] != 0 })
                }
                if (arr != null) return arr
            }
            return degraded()
        }

        /**
         * DEQRADE rejimi (.so yoxdur — yalnız Rust-sız lokal dava):
         * sabit "QOSHUL" v1-L matrisi (Rust qrcodegen-dən generasiya olunub).
         * Hələ də SKAN OLUNA BİLƏN real QR-dur.
         */
        private fun degraded(): QrGeometry {
            val d = FALLBACK_QOSHUL_V1
            val n = d[0]
            return QrGeometry(n, BooleanArray(n * n) { d[1 + it] != 0 })
        }

        // "QOSHUL", versiya 1, ECC L, boost=false — Rust qrcodegen çıxışı
        // (tests/qr_decode.rs qrameriya ilə eyni mənbədən).
        private val FALLBACK_QOSHUL_V1 = intArrayOf(
            21, // size
            1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1,
            1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1,
            1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1,
            1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1,
            1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1,
            1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1,
            1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0,
            1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0,
            1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1,
            0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0,
            1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0,
            1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1,
            1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 0,
            1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1,
            1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0,
            1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0,
        )
    }
}

/**
 * Real skan edilə bilən QR kod — D-041-dən (D-081-də Rust-a keçdi).
 * Kart konteyneri (ağ fon + haşiyə + padding) keçmiş vizual dillə qorunur.
 */
@Composable
fun QrCode(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    // U3.3 (UI-48): dark default Q.foreground-un hardcoded dublikatı deyil artıq —
    // vahid token; light — skan üçün ağ (Q.qrLight)
    dark: Color = Q.foreground,
    light: Color = Q.qrLight,
) {
    val geometry = remember(payload) { QrGeometry.of(payload) }
    Box(
        modifier
            .size(size)
            .background(light, RoundedCornerShape(Q.rLg.dp))
            .border(1.dp, Q.border, RoundedCornerShape(Q.rLg.dp))
            .padding(8.dp),
    ) {
        Canvas(Modifier.size(size - 16.dp)) {
            drawQr(geometry, dark, light)
        }
    }
}

/** Finder gözünün modul koordinatları (sol-üst künc) — üç göz üçün. */
internal fun finderEyeOrigins(n: Int): List<Pair<Int, Int>> =
    listOf(0 to 0, n - 7 to 0, 0 to n - 7)

/** Modulun finder gözü zonasına daxil olub-olmadığı (7×7 + 1 ayırıcı). */
internal fun inFinderZone(x: Int, y: Int, n: Int): Boolean =
    (x < 8 && y < 8) || (x >= n - 8 && y < 8) || (x < 8 && y >= n - 8)

/**
 * DrawScope — həndəsəni brend stili ilə çəkir.
 */
internal fun DrawScope.drawQr(g: QrGeometry, dark: Color, light: Color) {
    val quiet = 2f // daxili quiet zone (modul) — Box padding-i əlavə edir
    val n = g.size.toFloat()
    val cell = size.minDimension / (n + 2f * quiet)
    val origin = Offset((size.width - cell * n) / 2f, (size.height - cell * n) / 2f)
    val eyeR = CornerRadius(cell * 0.3f, cell * 0.3f)
    val coreR = CornerRadius(cell * 0.2f, cell * 0.2f)

    fun px(v: Float) = origin.x + v * cell
    fun py(v: Float) = origin.y + v * cell

    // 1) finder gözləri — ISO struktur (7 tünd / 5 açıq / 3 tünd), yumşaq künc
    for ((ex, ey) in finderEyeOrigins(g.size)) {
        drawRoundRect(
            color = dark,
            topLeft = Offset(px(ex.toFloat()), py(ey.toFloat())),
            size = Size(7 * cell, 7 * cell),
            cornerRadius = eyeR,
        )
        drawRoundRect(
            color = light,
            topLeft = Offset(px(ex + 1f), py(ey + 1f)),
            size = Size(5 * cell, 5 * cell),
            cornerRadius = CornerRadius(cell * 0.22f, cell * 0.22f),
        )
        drawRoundRect(
            color = dark,
            topLeft = Offset(px(ex + 2f), py(ey + 2f)),
            size = Size(3 * cell, 3 * cell),
            cornerRadius = coreR,
        )
    }

    // 2) data modulları — standart kvadrat (göz zonaları xaric)
    for (y in 0 until g.size) {
        for (x in 0 until g.size) {
            if (!g.module(x, y)) continue
            if (inFinderZone(x, y, g.size)) continue
            drawRect(
                color = dark,
                topLeft = Offset(px(x.toFloat()), py(y.toFloat())),
                size = Size(cell, cell),
            )
        }
    }
}
