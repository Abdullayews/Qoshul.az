package az.qoshul.app

import az.qoshul.app.core.ui.WindowWidthClass
import az.qoshul.app.core.ui.widthClassOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * D-102 (v0.35.0): adaptiv en sinifi — canonical Material 3 breakpoint
 * sertifikatı. widthClassOf saf funksiyadır — JVM-də sərhəd dəyərləri
 * birbaşa yoxlanılır (Robolectric lazım deyil).
 *
 * Sərhədlər: <600dp Compact (telefon), 600..839dp Medium (planşet dar /
 * qatlanan açıq), ≥840dp Expanded (planşet enli / desk). Sərhəd DAXİLDIR:
 * 600 = Medium, 840 = Expanded — Material 3 spesifikasiyası ilə bərabər.
 */
class AdaptiveTest {

    @Test
    fun `sərhədlər — 599 Compact, 600 Medium, 839 Medium, 840 Expanded`() {
        assertEquals(WindowWidthClass.Compact, widthClassOf(599))
        assertEquals(WindowWidthClass.Medium, widthClassOf(600))
        assertEquals(WindowWidthClass.Medium, widthClassOf(839))
        assertEquals(WindowWidthClass.Expanded, widthClassOf(840))
    }

    @Test
    fun `telefon diapazonu — Compact (360-599dp tipik portret)`() {
        listOf(360, 390, 412, 430, 599).forEach {
            assertEquals("en=$it", WindowWidthClass.Compact, widthClassOf(it))
        }
    }

    @Test
    fun `planşet-qatlanan diapazonu — Medium (600-839dp)`() {
        listOf(600, 673, 720, 838, 839).forEach {
            assertEquals("en=$it", WindowWidthClass.Medium, widthClassOf(it))
        }
    }

    @Test
    fun `geniş diapazon — Expanded (840dp+)`() {
        listOf(840, 900, 1024, 1280, 1920).forEach {
            assertEquals("en=$it", WindowWidthClass.Expanded, widthClassOf(it))
        }
    }

    @Test
    fun `kənar dəyərlər — 0 və mənfi Compact, çox böyük Expanded`() {
        assertEquals(WindowWidthClass.Compact, widthClassOf(0))
        assertEquals(WindowWidthClass.Compact, widthClassOf(-320))
        assertEquals(WindowWidthClass.Expanded, widthClassOf(Int.MAX_VALUE))
    }
}
