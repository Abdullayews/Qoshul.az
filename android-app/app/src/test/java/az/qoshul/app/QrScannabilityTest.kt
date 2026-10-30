package az.qoshul.app

import az.qoshul.app.core.ui.QrGeometry
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * QR SKAN OLUNA BİLƏRLİYİNİN SÜBUTU (D-041).
 *
 * QrCode composable-ın çəkdiyi həndəsə ilə eyni modul riyaziyyatını AWT-də
 * rəngləyirik (rounded finder gözləri də daxil — RoundRectangle2D eyni nisbi
 * radiusla) və ZXing (zxing:core 3.5.4, yalnız test asılılığı) ilə dekodlaşdırırıq.
 * Dekodlaşdırılmış mətn payload-a bərabərdirsə, generasiya + render stili real
 * skanerlər üçün oxunulandır — bu iddia varsayım deyil, testlə bağlıdır.
 */
class QrScannabilityTest {

    /** Compose drawQr semantikasının AWT ekvivalenti (eyni modul quruluşu). */
    private fun render(g: QrGeometry, scale: Int = 8, quietModules: Int = 4): BufferedImage {
        val dim = (g.size + 2 * quietModules) * scale
        val img = BufferedImage(dim, dim, BufferedImage.TYPE_INT_RGB)
        val gfx = img.createGraphics()
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gfx.color = Color.WHITE
        gfx.fillRect(0, 0, dim, dim)
        gfx.color = Color.BLACK

        val cell = scale.toFloat()
        fun rect(mx: Float, my: Float, w: Float, h: Float, radiusFraction: Float) {
            val arc = 2f * cell * radiusFraction
            gfx.fill(
                RoundRectangle2D.Float(
                    mx * cell, my * cell, // modul → piksel
                    w * cell, h * cell, arc, arc,
                ),
            )
        }

        // finder gözləri — 7 tünd / 5 ağ / 3 tünd (rounded, drawQr kimi)
        for ((ex, ey) in az.qoshul.app.core.ui.finderEyeOrigins(g.size)) {
            gfx.color = Color.BLACK
            rect(quietModules + ex.toFloat(), quietModules + ey.toFloat(), 7f, 7f, 0.30f)
            gfx.color = Color.WHITE
            rect(quietModules + ex + 1f, quietModules + ey + 1f, 5f, 5f, 0.22f)
            gfx.color = Color.BLACK
            rect(quietModules + ex + 2f, quietModules + ey + 2f, 3f, 3f, 0.20f)
        }

        // data modulları — kvadrat
        for (y in 0 until g.size) {
            for (x in 0 until g.size) {
                if (!g.module(x, y)) continue
                if (az.qoshul.app.core.ui.inFinderZone(x, y, g.size)) continue
                gfx.fill(
                    java.awt.Rectangle(
                        (quietModules + x) * scale, (quietModules + y) * scale, scale, scale,
                    ),
                )
            }
        }
        gfx.dispose()
        return img
    }

    private fun decode(img: BufferedImage): String {
        val w = img.width
        val h = img.height
        val pixels = IntArray(w * h)
        img.getRGB(0, 0, w, h, pixels, 0, w)
        val source = RGBLuminanceSource(w, h, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")
        return QRCodeReader().decode(bitmap, hints).text
    }

    private fun assertRoundTrips(payload: String) {
        val g = QrGeometry.of(payload)
        assertEquals("QR generasiyası ↔ ZXing dekod (payload='$payload')", payload, decode(render(g)))
    }

    @Test fun `XP kartı nonce formatı skan edilir`() {
        assertRoundTrips("QSHUL-XP-a91d7f-58")
    }

    @Test fun `rezerv QR kodu skan edilir`() {
        assertRoundTrips("QSH-r12-ab48")
    }

    @Test fun `kupon kodu skan edilir`() {
        assertRoundTrips("QSHUL-4821-v5")
    }

    @Test fun `obyekt statik QR azərbaycan mətni ilə skan edilir`() {
        // UTF-8: ə/ğ/ş/İ qlifləri — AZ mətnin tam dövriyyəsi
        assertRoundTrips("STATIC-v5-Xankəndi Kafe Şəbiştay")
    }

    @Test fun `uzun payload skan edilir`() {
        assertRoundTrips("STATIC-v3-" + "Kənd təsərrüfatı və yaşıllıq zonası — ".repeat(4))
    }

    @Test fun `boş payload fallback verir və skan edilir`() {
        val g = QrGeometry.of("")
        assertTrue("fallback geometry yaradılır", g.size in 21..177)
        assertEquals("QOSHUL", decode(render(g)))
    }

    @Test fun `struktur invariantlər — finder və timing naxışları`() {
        val g = QrGeometry.of("QSHUL-XP-a91d7f-58")
        // finder nüvəsi (3×3) hər üç gözün mərkəzində tünd
        for ((ex, ey) in az.qoshul.app.core.ui.finderEyeOrigins(g.size)) {
            assertTrue(g.module(ex + 3, ey + 3))
            // xarici halqa küncləri tünd
            assertTrue(g.module(ex, ey))
            assertTrue(g.module(ex + 6, ey + 6))
        }
        // timing naxışı: 6-cı sətir/sütun, finder zonaları arası, cüt indekslərdə tünd
        for (i in 8 until g.size - 8 step 2) {
            assertTrue("timing row[6,$i] tünd olmalıdır", g.module(6, i))
            assertTrue("timing col[$i,6] tünd olmalıdır", g.module(i, 6))
        }
        // versiya 1-10 aralığında (bizim payload-lar üçün)
        assertTrue(g.size in 21..57)
    }
}
