package az.qoshul.app.core.backend

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.safety.Safety
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.gotrue.SettingsSessionManager
import io.github.jan.supabase.gotrue.SessionSource
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

/**
 * D-151 (2026-11-25, direktiv: „demo məntiqi heç olmasın; supabase və
 * google login aktiv olsun“): VAHİD app-scope Supabase klienti.
 *
 * Əvvəlki arxitekturada iki ayrı client var idi — SupabaseAuth (yalnız
 * Auth modulu) və SupabaseSync (yalnız Postgrest) → login-dən sonra RPC-lər
 * ANON token ilə gedirdi, RLS „to authenticated“ siyasətləri hamısını
 * bloklayırdı. İndi: BİR client, hər iki modul — sessiya JWT-si bütün
 * RPC-lərə avtomatik düşür.
 *
 * Sessiya persistence (D-151): SettingsSessionManager (multiplatform-settings,
 * Android-də SharedPreferences) — app restart-dan sonra login QALIR; access
 * token auto-refresh gotrue-kt daxilindədir.
 *
 * versiya pin: supabase-kt 3.9.0 + gotrue-kt 2.6.1 (D-068; namespace
 * `io.github.jan.supabase` — D-078 transfer qeydi).
 *
 * Təhlükəsizlik (qayda 3/D-080): yalnız ANON_KEY; SERVICE_ROLE_KEY
 * klientə heç vaxt düşmür.
 */
object SupabaseBackend {

    /** D-155: M4 verify cavabı (Google axını). */
    @Serializable
    data class M4GoogleUser(val id: String = "", val email: String = "")

    @Serializable
    data class M4GoogleSession(
        val access_token: String,
        val refresh_token: String,
        val expires_in: Long = 3600,
        val user: M4GoogleUser = M4GoogleUser(),
    )

    /** JVM testləri üçün açıq (şəbəkə çağırışı YOXDUR — yalnız konstruksiya). */
    val client: SupabaseClient? = runCatching {
        require(BackendConfig.isConfigured) {
            "Supabase konfiqurasiyası tamamlanmayıb (SUPABASE_URL/ANON_KEY)"
        }
        createSupabaseClient(
            supabaseUrl = BackendConfig.supabaseUrl,
            supabaseKey = BackendConfig.supabaseAnonKey,
        ) {
            install(Auth) {
                sessionManager = SettingsSessionManager(Settings())
            }
            install(Postgrest)
        }
    }.getOrElse {
        if (BackendConfig.isConfigured) {
            // Yalnız real konfiqurasiya xətası loglanır (fork PLACEHOLDER buildi səssizdir)
            Safety.onCaught?.invoke("SupabaseBackend", it)
        }
        null
    }

    private fun requireClient(): SupabaseClient =
        client ?: error("Supabase client hazır deyil (SUPABASE_URL/ANON_KEY yoxlan)")

    // ── Autentifikasiya (GoTrue) ──

    /**
     * Qeydiyyat — email+şifrə. Autoconfirm on olduğu üçün sessiya dərhal
     * açılır; profil adı raw_user_meta_data-ya düşür (profiles cədvəli
     * auth.users triggeri ilə yaradılır — 0002 sxemi).
     */
    suspend fun signUp(email: String, password: String, fullName: String?) {
        requireClient().auth.signUpWith(Email) {
            this.email = email
            this.password = password
            if (!fullName.isNullOrBlank()) {
                data = buildJsonObject { put("full_name", fullName) }
            }
        }
    }

    /** Giriş — email + şifrə. */
    suspend fun signIn(email: String, password: String) {
        requireClient().auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** 6 rəqəmli email OTP göndər (mövcud hesab üçün; yenisi yaradılmır). */
    suspend fun sendEmailOtp(email: String) {
        requireClient().auth.signInWith(OTP) {
            this.email = email
            createUser = false
        }
    }

    /** OTP təsdiqi — Email.EMAIL həm MAGIC_LINK, həm SIGNUP kodlarını əhatə edir. */
    suspend fun verifyOtp(email: String, code: String) {
        requireClient().auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL, email = email, token = code,
        )
    }

    /** „Şifrəni unutdum“ — e-poçta yeniləmə linki (SITE_URL-ə yönlənir). */
    suspend fun resetPassword(email: String) {
        requireClient().auth.resetPasswordForEmail(email)
    }

    /**
     * Google native sign-in (D-150 → D-155) — Credential Manager.
     *
     * D-155 (2026-11-25, „konfiqurasiya xətası“ diaqnozu): Google rəsmi
     * sənədlərinə görə setServerClientId yalnız WEB tipli ID qəbul edir;
     * istifadəçinin yaratdığı ANDROID tipli ID burada DEVELOPER_ERROR
     * verirdi. Həll: Credential Manager ilə aud=ANDROID_ID token alınır,
     * doğrulamani M4 servisi edir (Google JWKS RS256 + nonce), sessiya
     * GoTrue admin API ilə açılır → importSession.
     *
     * Nonce qaydası: raw nonce (UUID) → SHA-256 HEX → Google-a HASHED gedir
     * (setNonce), M4-ə RAW gedir — server hash edib token-in nonce claim-i
     * ilə yoxlayır (replay qoruması).
     *
     * @param serverClientId ANDROID client ID (Google Cloud „Android“ tipli
     *   OAuth klienti, paket+SHA-1 ilə). Web klient gələcəkdə yaradılarsa
     *   bu axın dəyişməz qalır — M4 aud siyahısına WEB ID də qəbul edir.
     */
    suspend fun signInWithGoogle(context: Context, serverClientId: String) {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256Hex(rawNonce)

        // D-156 (28444 xətası): setServerClientId yalnız WEB tipli ID
        // qəbul edir. serverClientId parametri WEB ID olmalıdır — dinamik
        // olaraq m4 /config-dən gəlir (fetchGoogleConfig).
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = CredentialManager.create(context).getCredential(
            context = context, request = request,
        )
        val googleIdToken = GoogleIdTokenCredential
            .createFrom(result.credential.data).idToken

        // Kanonik yol (D-156): GoTrue IDToken grant — GoTrue client_id
        // siyahısında WEB ID birincidir; ən təmiz axın (Supabase rəsmi).
        // Uğursuzsa (GoTrue hələ WEB ID tanımırsa) → M4 verify fallback
        // (öz JWKS doğrulamamız — aud siyahısında hər iki ID ola bilər).
        try {
            requireClient().auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
                nonce = rawNonce
            }
        } catch (e: Exception) {
            val session = m4VerifyGoogleToken(googleIdToken, rawNonce)
            requireClient().auth.importSession(
                UserSession(
                    accessToken = session.access_token,
                    refreshToken = session.refresh_token,
                    providerRefreshToken = null,
                    providerToken = null,
                    expiresIn = session.expires_in,
                    tokenType = "bearer",
                    user = UserInfo(
                        id = session.user.id,
                        email = session.user.email,
                        aud = "authenticated",
                        role = "authenticated",
                    ),
                    type = "m4_google",
                    expiresAt = kotlinx.datetime.Instant.fromEpochSeconds(
                        System.currentTimeMillis() / 1000 + session.expires_in
                    ),
                ),
                false,
                SessionSource.External,
            )
        }
    }

    /**
     * D-156: M4 /config — dinamik Google WEB klient ID (28444 həlli).
     * APK yeniləmədən sonra da ID serverdən gəlir; uğursuzluqda səssizcə
     * BuildConfig dəyərinə qalır (null isə googleReady=false).
     */
    suspend fun fetchGoogleConfig() {
        val http = HttpClient(CIO)
        try {
            val resp = http.get(BackendConfig.m4Url + "/config") {
                header("apikey", BackendConfig.supabaseAnonKey)
            }
            if (resp.status.isSuccess()) {
                val el = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
                BackendConfig.adoptGoogleWebClientId(
                    (el["google_web_client_id"] as? JsonPrimitive)?.content
                )
            }
        } catch (_: Exception) {
            // Şəbəkə xətası — sessiya açıq deyil, sadəcə BuildConfig qalır
        } finally {
            http.close()
        }
    }

    /** D-155: M4 /auth/google/verify çağırışı (ktor CIO — əlavə native asılılıq YOX). */
    private suspend fun m4VerifyGoogleToken(
        idToken: String,
        rawNonce: String,
    ): M4GoogleSession {
        val http = HttpClient(CIO)
        try {
            val body = buildJsonObject {
                put("id_token", idToken)
                put("nonce", rawNonce)
            }
            val resp = http.post(BackendConfig.m4Url + "/auth/google/verify") {
                header("apikey", BackendConfig.supabaseAnonKey)
                header("Content-Type", "application/json")
                setBody(body.toString())
            }
            val text = resp.bodyAsText()
            if (!resp.status.isSuccess()) {
                val desc = runCatching {
                    Json.parseToJsonElement(text).jsonObject["error_description"]?.toString()
                }.getOrNull() ?: "HTTP ${resp.status.value}"
                throw IllegalStateException("Google doğrulama alınmadı: $desc")
            }
            return Json { ignoreUnknownKeys = true }.decodeFromString(text)
        } finally {
            http.close()
        }
    }

    /** Aktiv sessiya varmı (restart-dan sonra SettingsStorage-dən bərpa olunur). */
    fun isSignedIn(): Boolean =
        client?.auth?.sessionStatus?.value is SessionStatus.Authenticated

    /** Çıxış — sessiya ləğv olunur, refresh token revoke edilir, storage təmizlənir. */
    suspend fun signOut() {
        client?.auth?.signOut()
    }

    /** Google düyməsi görünmə şərti (D-156): dinamik — m4 /config VEYA BuildConfig. */
    val googleAvailable: Boolean
        get() = BackendConfig.isConfigured && BackendConfig.googleReady

    // ── RPC (Postgrest) — sessiya JWT-si avtomatik gedir ──

    /** Uğursuz RPC-lərin son N-i (debug/UI). */
    val lastErrors = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())

    /** Asinxron RPC — [BackendSync.report] yolunun icrası. */
    fun rpcAsync(scope: CoroutineScope, call: BackendCall) {
        val c = client ?: return
        scope.launch {
            runCatching { c.postgrest.rpc(function = call.fn, parameters = call.params) }
                .onFailure { t ->
                    Safety.onCaught?.invoke("sync:${call.fn}", t)
                    lastErrors.value =
                        (listOf("${call.fn}: ${t.message?.take(120)}") + lastErrors.value).take(20)
                }
        }
    }

    /** Sinxron RPC (oxu yolu / test). */
    suspend fun rpcNow(call: BackendCall): Result<JsonElement?> {
        val c = requireClient()
        return runCatching { c.postgrest.rpc(function = call.fn, parameters = call.params) as JsonElement? }
    }

    /** SHA-256 → hex (Google nonce formatı; rəsmi sənəd nümunəsi ilə eyni). */
    internal fun sha256Hex(raw: String): String =
        MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

/**
 * Sink — [SupabaseSync.mapEvent] çıxışını [SupabaseBackend.rpcAsync]-ə ötürür.
 */
class SupabaseSync(
    private val scope: CoroutineScope,
) : SyncSink {
    override fun report(event: SyncEvent) {
        val call = mapEvent(event) ?: return
        SupabaseBackend.rpcAsync(scope, call)
    }

    companion object {
        /** mapEvent — [SyncEvent] → RPC (test olunan xalis funksiya). */
        fun mapEvent(e: SyncEvent): BackendCall? = mapEventImpl(e)
    }
}

/** Yalnız main source-da istifadə — MainActivity bir sətirlə qurur. */
object BackendSync {
    fun create(scope: CoroutineScope): SyncSink =
        if (BackendConfig.isConfigured) SupabaseSync(scope)
        else SyncSink { /* konfiqurasiya yoxdursa güzgü getmir; xəta describe()-da görünür */ }
}
