package az.qoshul.app

import az.qoshul.app.core.data.ApiClient
import az.qoshul.app.core.data.ApiResult
import az.qoshul.app.core.data.AppConfig
import az.qoshul.app.core.data.ConnectionState
import az.qoshul.app.core.data.QoshulApi
import az.qoshul.app.core.data.ServerQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-050: SERVER_ONLY rejimi (server-only branch) sübutları.
 *
 *  1. ServerQoshulDataSource tamamilə BOŞDUR — heç bir fake məlumat yoxdur.
 *  2. Bu branch-də mühərrikin default mənbəyi ServerQoshulDataSource-dur.
 *  3. Giriş YALNIZ server uğuru ilə açılır; xətada qalır.
 *  4. Mock signIn/signInDemo server rejimində bağlanıb.
 *  5. Assistant mock cavab vermir — yalnız server cavabı/xətası göstərilir.
 */
class ServerModeTest {

    // — Konfiqurasiya edilə bilən fake API (şəbəkə YOXDUR) —
    private class FakeApi(
        var loginResult: ApiResult<ApiClient.ServerProfile> =
            ApiResult.Err("Serverə qoşulunmur (test)."),
    ) : QoshulApi {
        override var baseUrl: String = "https://test.invalid"
        override val connection = MutableStateFlow(ConnectionState.OFFLINE)
        override suspend fun checkConnection() = false
        override suspend fun login(email: String, password: String) = loginResult
        override suspend fun register(email: String, password: String, name: String) = loginResult
        override suspend fun forgotPassword(email: String) = ApiResult.Err("Serverə qoşulunmur (test).")
        override suspend fun assistant(message: String) = ApiResult.Err("Serverə qoşulunmur (test).")
        override suspend fun postEvent(payload: String) = ApiResult.Err("Serverə qoşulunmur (test).")
    }

    @Test
    fun `server mənbəyi tamamilə boşdur — fake data yoxdur`() {
        val ds = ServerQoshulDataSource
        assertTrue("tədbirlər boş", ds.events.isEmpty())
        assertTrue("insanlar boş", ds.people.isEmpty())
        assertTrue("məkanlar boş", ds.partners.isEmpty())
        assertTrue("şəhərlər boş", ds.cities.isEmpty())
        assertTrue("təkliflər boş", ds.offers.isEmpty())
        assertTrue("bildirişlər boş", ds.initialNotifications.isEmpty())
        assertTrue("söhbətlər boş", ds.initialEventMessages.isEmpty() && ds.initialFriendMessages.isEmpty())
        assertTrue("slotlar boş", ds.venueSlotTimes.isEmpty())
        assertTrue("rəylər boş", ds.mockVenueReviews.isEmpty())
        assertTrue("XP hesabları boş", ds.initialVenueXp().isEmpty())
    }

    @Test
    fun `bu branch-də default mənbək serverdir və rejim SERVER_ONLY-dır`() {
        assertTrue("AppConfig.SERVER_ONLY aktivdir", AppConfig.isServerOnly)
        val e = AppEngine()
        assertEquals("default mənbək ServerQoshulDataSource-dur",
            ServerQoshulDataSource, e.dataSource)
        assertTrue("heç bir tədbir yoxdur", e.state.value.events.isEmpty())
        assertTrue("heç bir məkan yoxdur", e.partners.isEmpty())
        assertTrue("heç bir namizəd yoxdur", e.matchCandidates.isEmpty())
    }

    @Test
    fun `giriş yalnız server uğuru ilə açılır`() = runTest {
        val api = FakeApi() // offline
        val e = AppEngine(scope = null, server = api)
        val err = e.signInServer("user@qoshul.az", "parol123")
        assertNotNull("xəta mesajı qayıtdı", err)
        assertFalse("daxil olunmadı", e.state.value.signedIn)
    }

    @Test
    fun `server uğurunda profil tətbiq olunur və giriş açılır`() = runTest {
        val api = FakeApi(
            loginResult = ApiResult.Ok(
                ApiClient.ServerProfile(firstName = "Nurlan", lastName = "Əliyev", city = "Gəncə", onboarded = true)
            )
        )
        val e = AppEngine(scope = null, server = api)
        val err = e.signInServer("nurlan@qoshul.az", "parol12345")
        assertEquals(null, err)
        val st = e.state.value
        assertTrue("daxil olundu", st.signedIn)
        assertTrue("server profili tətbiq olundu", st.profileName.contains("Nurlan"))
        assertEquals("Gəncə", st.city)
        assertTrue("onboarding serverdən gəldi", st.onboarded)
    }

    @Test
    fun `mock giriş və demo hesab server rejimində bağlıdır`() {
        val e = AppEngine(scope = null, server = FakeApi())
        e.signIn()
        assertFalse("mock signIn işləmədi", e.state.value.signedIn)
        e.signInDemo()
        assertFalse("demo hesab işləmədi", e.state.value.signedIn)
        assertTrue("demo profili tətbiq olunmadı", e.state.value.profileName == "Dost")
    }

    @Test
    fun `assistant server rejimində mock cavab vermir`() {
        val e = AppEngine(scope = null, server = FakeApi())
        e.sendAssistant("Salam, nə var?")
        val msgs = e.state.value.assistantMsgs
        assertEquals("istifadəçi mesajı əlavə olundu", 2, msgs.size)
        assertTrue(
            "cavab server xətasıdır, mock generator deyil",
            msgs.last().body.contains("qoşulunmur", ignoreCase = true),
        )
    }
}
