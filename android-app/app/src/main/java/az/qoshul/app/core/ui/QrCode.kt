package az.qoshul.app.core.ui

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
import io.nayuki.qrcodegen.QrCode as NayukiQrCode
import io.nayuki.qrcodegen.QrCode.Ecc
import io.nayuki.qrcodegen.QrSegment

/**
 * REAL QR kod generasiyası (D-041) — Nayuki v1.8.0 vendored (io.nayuki.qrcodegen,
 * MIT) üzərində brend render qatı.
 *
 * Arxitektura: həndəsə (QrGeometry) saf Kotlin-dır — JVM testində ZXing ilə
 * dekodlaşdırılıb SÜBUT olunur (QrScannabilityTest); composable yalnız həndəsəni
 * Canvas-a çəkir. Beləliklə "skan edilə bilər" iddiası testlə doğrulanır.
 *
 * Stil: data modulları standart kvadrat (maksimum skan etibarlılığı), finder
 * "gözləri" yumşaq künc radiusu ilə (brend görünüş). Sükut ECC = MEDIUM.
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
         * Payload-dan QR həndəsəsi yaradır. Versiya avtomatik seçilir
         * (min..max aralığında sığan ən kiçiyi). Sığmırsa aralıq genişlənir;
         * hər halda proqram çökmür (son fallback versiya-1 qırılmış payload).
         */
        fun of(payload: String, maxVersion: Int = 12): QrGeometry {
            val text = payload.ifBlank { "QOSHUL" }
            var vMax = maxVersion.coerceIn(1, 40)
            while (true) {
                try {
                    val segs = QrSegment.makeSegments(text)
                    val qr = NayukiQrCode.encodeSegments(segs, Ecc.MEDIUM, 1, vMax, -1, true)
                    return QrGeometry(qr.size, flatten(qr))
                } catch (_: IllegalArgumentException) {
                    if (vMax >= 40) return fallbackGeometry(text)
                    vMax = (vMax + 1).coerceAtMost(40)
                } catch (_: Exception) {
                    return fallbackGeometry(text)
                }
            }
        }

        private fun flatten(qr: NayukiQrCode): BooleanArray {
            val n = qr.size
            val out = BooleanArray(n * n)
            for (y in 0 until n) for (x in 0 until n) out[y * n + x] = qr.getModule(x, y)
            return out
        }

        /** Ağır xəta halında: kiçildilmiş payload versiya 1-də (proqram çökmür). */
        private fun fallbackGeometry(text: String): QrGeometry {
            var t = text.take(17)
            while (t.isNotEmpty()) {
                try {
                    val segs = QrSegment.makeSegments(t)
                    val qr = NayukiQrCode.encodeSegments(segs, Ecc.LOW, 1, 1, 0, false)
                    return QrGeometry(qr.size, flatten(qr))
                } catch (_: Exception) {
                    t = t.dropLast(1)
                }
            }
            // "QOSHUL" versiya 1-də mütləq sığır
            val segs = QrSegment.makeSegments("QOSHUL")
            val qr = NayukiQrCode.encodeSegments(segs, Ecc.LOW, 1, 1, 0, false)
            return QrGeometry(qr.size, flatten(qr))
        }
    }
}

/**
 * Real skan edilə bilən QR kod — QrPlaceholder-ın varisi (D-041).
 * Kart konteyneri (ağ fon + haşiyə + padding) keçmiş vizual dillə qorunur.
 */
@Composable
fun QrCode(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    dark: Color = Color(0xFF0F172B),
    light: Color = Color.White,
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
 * DrawScope — həndəsəni brend stili ilə çəkir. Test eyni modul riyaziyyatını
 * AWT-də rəngləyib ZXing ilə dekodlaşdırır (render pariteti yoxlanılır).
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
