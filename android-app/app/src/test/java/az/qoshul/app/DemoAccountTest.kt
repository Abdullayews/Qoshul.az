package az.qoshul.app

import az.qoshul.app.core.data.AppConfig
import az.qoshul.app.core.engine.AppEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-050: vahid hardcoded demo hesabı (PLACEHOLDER rejimi, main branch).
 *
 * Sübut: signInDemo() bir toxunuşla dolu profil verir — onboarding keçilir,
 * qeydiyyat XP-si sayılır, tədbirlər görünür ("hər funksiyanın görünməsi").
 * İkinci çağırış XP-ni ikiləşdirmir (idempotent qeydiyyat mükafatı).
 */
class DemoAccountTest {

    private fun newEngine() = AppEngine()

    @Test
    fun `demo hesabı bir toxunuşla dolu profillə daxil olur`() {
        val e = newEngine()
        e.signInDemo()
        val st = e.state.value
        assertTrue("daxil olundu", st.signedIn)
        assertTrue("onboarding keçildi", st.onboarded)
        assertEquals(AppConfig.DemoAccount.FULL_NAME, st.profileName)
        assertEquals(AppConfig.DemoAccount.CITY, st.city)
        assertEquals("student", st.status)
        assertTrue("maraqlar doludur", st.interests.size >= 3)
        assertTrue("bio doludur", st.bio.isNotBlank())
    }

    @Test
    fun `demo hesabında funksiyalar görünür — tədbir, XP, namizədlər`() {
        val e = newEngine()
        e.signInDemo()
        val st = e.state.value
        // e0 keçmiş tədbirdir (rəy axını demo-su) — qoşulmuş kimi görünür
        assertTrue("qoşulmuş tədbir var (e0 — rəy üçün)", st.joined.isNotEmpty())
        assertTrue("şəhər tədbirləri görünür", e.cityEvents.isNotEmpty())
        assertTrue("XP qazanıldı (qeydiyyat + günlük)", st.xp >= 320 + 50 + 5)
        assertTrue("match namizədləri görünür", e.matchCandidates.isNotEmpty())
        assertTrue("məkanlar görünür", e.partners.isNotEmpty())
        assertTrue("endirim XP hesabları görünür", st.venueXp.isNotEmpty())
        assertTrue("bildirişlər görünür", st.notifications.isNotEmpty())
    }

    @Test
    fun `demo girişi idempotentdir — ikinci dəfə XP ikiləşmir`() {
        val e = newEngine()
        e.signInDemo()
        val xpAfterFirst = e.state.value.xp
        e.signOut()
        e.signInDemo()
        val xpAfterSecond = e.state.value.xp
        assertTrue("XP ikiləşmədi ($xpAfterFirst → $xpAfterSecond)",
            xpAfterSecond >= xpAfterFirst && xpAfterSecond - xpAfterFirst <= 5) // yalnız günlük +5
    }

    @Test
    fun `çıxışdan sonra demo yenidən işləyir`() {
        val e = newEngine()
        e.signInDemo()
        e.signOut()
        assertTrue("çıxış təsirli", !e.state.value.signedIn)
        e.signInDemo()
        assertTrue("yenidən daxil oldu", e.state.value.signedIn)
        assertTrue("onboarding yenidən tələb olunmur", e.state.value.onboarded)
    }
}
