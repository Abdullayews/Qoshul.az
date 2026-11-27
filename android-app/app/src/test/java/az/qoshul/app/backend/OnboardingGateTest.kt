package az.qoshul.app.backend

import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.backend.SyncSink
import az.qoshul.app.core.backend.SupabaseReader.ServerData
import az.qoshul.app.core.engine.AppEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * C (D-170): onboarding qərarı YALNIZ serverdən gələn profillə verilir.
 *
 * Ssenarilər:
 *  1. signIn() → signedIn=true, amma profileLoaded=false → UI loading
 *     göstərir, onboarded qərarı YOX (əvvəl: default false onboarding-i
 *     açırdı — təkrar girişdə onboarding „flaş“ edirdi).
 *  2. Server onboarded=true qaytarır → onboarding BİR DAHA göstərilmir.
 *  3. Server onboarded=false (yeni qeydiyyat) → onboarding göstərilir.
 *  4. Xəta → syncError dolu, profileLoaded=false (retry düyməsi).
 *  5. Default state: static data YOXDUR (A.1) — ad/şəhər/status boşdur.
 */
class OnboardingGateTest {

    private val frozen: LocalDateTime = LocalDateTime.of(2026, 11, 27, 12, 0)

    private fun engine() = AppEngine(
        dataSource = az.qoshul.app.core.data.FakeQoshulDataSource,
        clock = { frozen }, sync = object : SyncSink {
            override fun report(event: SyncEvent) {}
        },
    )

    private fun profile(onboarded: Boolean) = az.qoshul.app.core.backend.SupabaseReader.MyProfile(
        id = "p1", firstName = "Ayaz", lastName = "Məmmədov", city = "Bakı",
        bio = "", xp = 0, onboarded = onboarded, photoCount = 0,
        gender = "", status = "", university = "", workField = "",
        languages = emptyList(), goals = emptyList(),
        interests = emptyList(), groupSize = "", budget = "",
        availability = emptyList(),
    )

    @Test fun `1 signIn profilsiz onboarding qərarı vermir`() {
        val e = engine()
        e.signIn()
        val st = e.state.value
        assertTrue("signedIn olmalıdır", st.signedIn)
        assertFalse("profil hələ YÜKLƏNMƏYİB — loading göstərilir", st.profileLoaded)
        // onboarded sahəsi hələ QƏRAR deyil — UI !profileLoaded budağındadır
    }

    @Test fun `2 təkrar girişdə server onboarded=true → onboarding YOX`() {
        val e = engine()
        e.signIn()
        e.applyServerData(ServerData(myProfile = profile(onboarded = true)))
        val st = e.state.value
        assertTrue(st.profileLoaded)
        assertTrue("köhnə hesab — onboarding göstərilmir", st.onboarded)
    }

    @Test fun `3 yeni qeydiyyat server onboarded=false → onboarding VAR`() {
        val e = engine()
        e.signIn()
        e.applyServerData(ServerData(myProfile = profile(onboarded = false)))
        val st = e.state.value
        assertTrue(st.profileLoaded)
        assertFalse("yeni hesab — onboarding göstərilir", st.onboarded)
    }

    @Test fun `4 default state static data yoxdur (A1)`() {
        val e = engine()
        val st = e.state.value
        assertEquals("", st.profileName)
        assertEquals("", st.city)
        assertEquals("", st.status)
        assertEquals("", st.groupSize)
        assertEquals("", st.budget)
        assertTrue(st.languages.isEmpty())
        assertEquals(0, st.photoCount)
        assertEquals(0, st.xp)  // I.1: başlanğıc XP 0 (320 deyil)
    }

    @Test fun `5 signOut sonrakı giriş üçün qapını sıfırlayır`() {
        val e = engine()
        e.signIn()
        e.applyServerData(ServerData(myProfile = profile(onboarded = true)))
        e.signOut()
        val st = e.state.value
        assertFalse(st.profileLoaded)
        assertFalse(st.onboarded)
        assertFalse(st.signedIn)
    }
}
