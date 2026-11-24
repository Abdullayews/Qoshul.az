package az.qoshul.app.core.backend

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import az.qoshul.app.BuildConfig
import az.qoshul.app.core.safety.Safety
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.UUID

/**
 * D-149/D-150: real GoTrue autentifikasiyası — AuthScreen mock axınlarının
 * əvəzi (server inteqrasiyası direktivi 2026-11-24).
 *
 * Rejim qərarı [BackendConfig] verir: MOCK rejimində bu obyekt YARADILMIR
 * (AuthScreen köhnə lokal axını saxlayır — determinizm/tests pozulmur).
 * SUPABASE rejimində bütün auth əməliyyatları canlı GoTrue-a gedir:
 *  - [signUp]           — email+şifrə qeydiyyatı (autoconfirm on → dərhal sessiya)
 *  - [signIn]           — email+şifrə giriş
 *  - [sendEmailOtp]     — 6 rəqəmli email OTP göndər
 *  - [verifyOtp]        — OTP təsdiqi (e-poçt təsdiqi rejimi)
 *  - [resetPassword]    — „şifrəni unutdum“ linki
 *  - [signInWithGoogle] — Credential Manager ID token → GoTrue IDToken grant
 *
 * versiya pin: supabase-kt 3.9.0 + gotrue-kt 2.6.1 (D-068; namespace
 * `io.github.jan.supabase` — D-078 transfer qeydi). API imzaları 2.6.1
 * mənbəyindən təsdiq edilib (Auth.kt: signUpWith/signInWith/verifyEmailOtp/
 * resetPasswordForEmail; OTP.Config: email/createUser; IDToken.Config:
 * idToken/provider/nonce).
 *
 * Təhlükəsizlik (qayda 3/D-080): yalnız ANON_KEY istifadə olunur;
 * SERVICE_ROLE_KEY klientə heç vaxt düşmür.
 */
class SupabaseAuth(
    supabaseUrl: String = BackendConfig.supabaseUrl,
    supabaseAnonKey: String = BackendConfig.supabaseAnonKey,
) {
    /** JVM testləri üçün açıq (şəbəkə çağırışı YOXDUR — client yalnız konstruksiya olunur). */
    val client: SupabaseClient? = runCatching {
        createSupabaseClient(supabaseUrl, supabaseAnonKey) {
            install(Auth)
            install(Postgrest)
        }
    }.getOrElse {
        Safety.onCaught?.invoke("SupabaseAuth", it)
        null
    }

    /**
     * Qeydiyyat — email+şifrə. Autoconfirm on olduğu üçün sessiya dərhal
     * açılır; profil adı raw_user_meta_data-ya düşür (profiles cədvəli
     * auth.users triggeri ilə yaradılır — 0002 sxemi).
     */
    suspend fun signUp(email: String, password: String, fullName: String?) {
        val c = requireClient()
        c.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            if (!fullName.isNullOrBlank()) {
                data = buildJsonObject { put("full_name", fullName) }
            }
        }
    }

    /** Giriş — email + şifrə. */
    suspend fun signIn(email: String, password: String) {
        val c = requireClient()
        c.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** 6 rəqəmli email OTP göndər (mövcud hesab üçün; yenisi yaradılmır). */
    suspend fun sendEmailOtp(email: String) {
        val c = requireClient()
        c.auth.signInWith(OTP) {
            this.email = email
            createUser = false
        }
    }

    /** OTP təsdiqi — Email.EMAIL həm MAGIC_LINK, həm SIGNUP kodlarını əhatə edir. */
    suspend fun verifyOtp(email: String, code: String) {
        val c = requireClient()
        c.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    /** „Şifrəni unutdum“ — e-poçta yeniləmə linki (SITE_URL-ə yönlənir). */
    suspend fun resetPassword(email: String) {
        val c = requireClient()
        c.auth.resetPasswordForEmail(email)
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
        val c = requireClient()
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
        val idToken = GoogleIdTokenCredential
            .createFrom(result.credential.data).idToken

        c.auth.signInWith(IDToken) {
            idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    /** Aktiv sessiya varmı (auto-refresh gotrue-kt daxilindədir). */
    fun isSignedIn(): Boolean = client?.auth?.sessionStatus?.value is SessionStatus.Authenticated

    /** Çıxış — sessiya ləğv olunur, refresh token revoke edilir. */
    suspend fun signOut() {
        client?.auth?.signOut()
    }

    private fun requireClient(): SupabaseClient =
        client ?: error("Supabase client yaradıla bilmədi (SUPABASE_URL/ANON_KEY yoxlan)")

    companion object {
        /** Google düyməsi görünmə şərti — SUPABASE rejimi + WEB client ID konfiqurasiya olunub. */
        val googleAvailable: Boolean
            get() = BackendConfig.mode == BackendMode.SUPABASE &&
                BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

        /** SHA-256 → hex (Google nonce formatı; rəsmi sənəd nümunəsi ilə eyni). */
        internal fun sha256Hex(raw: String): String =
            MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
