package az.qoshul.app.core.backend

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.safety.Safety
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.russhwolf.settings.Settings
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
     * Google native sign-in (D-150) — Credential Manager (modern API;
     * köhnə GoogleSignInClient deprecate + supabase-kt #442 null token).
     *
     * Nonce qaydası: raw nonce (UUID) → SHA-256 HEX → Google-a HASHED gedir
     * (setNonce), Supabase-ə RAW gedir — GoTrue raw-u hash edib token-in
     * nonce claim-i ilə yoxlayır (replay qoruması).
     *
     * @param serverClientId WEB client ID (Google Cloud „Web application“
     *   tipli OAuth klienti). Android klienti (paket+SHA-1) Google Cloud-da
     *   qalır, kodda olmur; Supabase client_id siyahısında WEB ID birinci.
     */
    suspend fun signInWithGoogle(context: Context, serverClientId: String) {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256Hex(rawNonce)

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

        requireClient().auth.signInWith(IDToken) {
            idToken = googleIdToken
            provider = Google
            nonce = rawNonce
        }
    }

    /** Aktiv sessiya varmı (restart-dan sonra SettingsStorage-dən bərpa olunur). */
    fun isSignedIn(): Boolean =
        client?.auth?.sessionStatus?.value is SessionStatus.Authenticated

    /** Çıxış — sessiya ləğv olunur, refresh token revoke edilir, storage təmizlənir. */
    suspend fun signOut() {
        client?.auth?.signOut()
    }

    /** Google düyməsi görünmə şərti — konfiqurasiya tamdır + WEB client ID var. */
    val googleAvailable: Boolean
        get() = BackendConfig.isConfigured &&
            BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

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
