package az.qoshul.app.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Qoshul API klienti — SERVER_ONLY rejimi (D-050, server-only branch).
 *
 * Minimal backend kontraktının SÜMÜYÜ (backend yazılanda OpenAPI ilə
 * formalizə olunacaq — docs/decisions.md D-050):
 *
 *   GET  /health          → 200 (canlılıq yoxlaması)
 *   POST /auth/login      → 200 {"token": "...", "profile": {"firstName": ...,
 *                            "lastName": ..., "city": ..., "onboarded": bool}}
 *                            | 401 (yanlış etimadnamə)
 *   POST /auth/register   → 200 {"token": "...", "profile": {...}} | 409
 *   POST /auth/forgot     → 200 (link göndərildi)
 *   POST /assistant       → 200 {"reply": "..."} | 401
 *   POST /events          → 201 | 401
 *
 * Server URL: SharedPreferences-də saxlanılır (Ayarlar → Server), standart
 * `https://api.qoshul.az` — istifadəçi lokal test üçün dəyişə bilər.
 *
 * DÜRÜSTLÜK QAYDASI: backend hələ YOXDUR — bütün çağırışlar bağlantı
 * xətası ilə nəticələnir və UI "Serverə qoşulunmur" vəziyyətini göstərir.
 * Bu, qəsdən işlənmiş davranışdır: tətbiq heç bir fake məlumat göstərmir.
 */
class ApiClient(context: Context) : QoshulApi {

    private val prefs = context.getSharedPreferences("qoshul_server", Context.MODE_PRIVATE)

    override var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL)?.trimEnd('/') ?: DEFAULT_URL
        set(value) {
            val clean = value.trim().trimEnd('/')
            prefs.edit().putString(KEY_URL, clean.ifEmpty { DEFAULT_URL }).apply()
        }

    override val connection: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.UNKNOWN)

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Serverdən gələn minimal profil (login/register cavabı). */
    data class ServerProfile(
        val firstName: String = "",
        val lastName: String = "",
        val city: String = "Bakı",
        val onboarded: Boolean = false,
    )

    // ── Canlılıq ──

    override suspend fun checkConnection(): Boolean {
        connection.value = ConnectionState.CHECKING
        val ok = httpGet("/health").isOk()
        connection.value = if (ok) ConnectionState.ONLINE else ConnectionState.OFFLINE
        return ok
    }

    // ── Auth ──

    override suspend fun login(email: String, password: String): ApiResult<ServerProfile> =
        when (val r = httpPost("/auth/login", jsonBody {
            put("email", email); put("password", password)
        })) {
            is HttpOutcome.Ok -> parseProfile(r.body)
            is HttpOutcome.Unauthorized -> ApiResult.Err("E-poçt və ya şifrə yanlışdır.")
            is HttpOutcome.Conflict -> ApiResult.Err("Bu e-poçt ilə hesab artıq var.")
            is HttpOutcome.Network -> ApiResult.Err(unreachable())
            is HttpOutcome.Server -> ApiResult.Err("Server xətası (${r.code}). Bir azdan yenidən yoxla.")
            HttpOutcome.Created -> ApiResult.Err("Gözlənilməz cavab.")
        }

    override suspend fun register(email: String, password: String, name: String): ApiResult<ServerProfile> =
        when (val r = httpPost("/auth/register", jsonBody {
            put("email", email); put("password", password); put("name", name)
        })) {
            is HttpOutcome.Ok -> parseProfile(r.body)
            is HttpOutcome.Unauthorized -> ApiResult.Err("Qeydiyyat məlumatları yanlışdır.")
            is HttpOutcome.Conflict -> ApiResult.Err("Bu e-poçt ilə hesab artıq var.")
            is HttpOutcome.Network -> ApiResult.Err(unreachable())
            is HttpOutcome.Server -> ApiResult.Err("Server xətası (${r.code}). Bir azdan yenidən yoxla.")
            HttpOutcome.Created -> ApiResult.Err("Gözlənilməz cavab.")
        }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> =
        when (val r = httpPost("/auth/forgot", jsonBody { put("email", email) })) {
            is HttpOutcome.Ok, HttpOutcome.Created -> ApiResult.Ok(Unit)
            is HttpOutcome.Network -> ApiResult.Err(unreachable())
            else -> ApiResult.Err("Server xətası. Bir azdan yenidən yoxla.")
        }

    // ── AI köməkçi ──

    override suspend fun assistant(message: String): ApiResult<String> =
        when (val r = httpPost("/assistant", jsonBody { put("message", message) })) {
            is HttpOutcome.Ok -> {
                val reply = runCatching {
                    json.parseToJsonElement(r.body).jsonObject["reply"]?.jsonPrimitive?.content
                }.getOrNull()
                if (reply != null) ApiResult.Ok(reply)
                else ApiResult.Err("Server cavabı oxuna bilmədi.")
            }
            is HttpOutcome.Unauthorized -> ApiResult.Err("Əvvəlcə daxil ol.")
            is HttpOutcome.Network -> ApiResult.Err(unreachable())
            is HttpOutcome.Server -> ApiResult.Err("Server xətası (${r.code}).")
            is HttpOutcome.Conflict -> ApiResult.Err("Konflikt (409).")
            HttpOutcome.Created -> ApiResult.Err("Gözlənilməz cavab.")
        }

    // ── Tədbir yaratma ──

    override suspend fun postEvent(payload: String): ApiResult<Unit> =
        when (val r = httpPost("/events", payload)) {
            is HttpOutcome.Ok, HttpOutcome.Created -> ApiResult.Ok(Unit)
            is HttpOutcome.Unauthorized -> ApiResult.Err("Əvvəlcə daxil ol.")
            is HttpOutcome.Network -> ApiResult.Err(unreachable())
            is HttpOutcome.Server -> ApiResult.Err("Server xətası (${r.code}).")
            is HttpOutcome.Conflict -> ApiResult.Err("Konflikt (409).")
        }

    // ── Daxili köməkçilər ──

    private fun parseProfile(body: String): ApiResult<ServerProfile> = runCatching {
        val o = json.parseToJsonElement(body).jsonObject
        val p = o["profile"]?.jsonObject ?: o
        ApiResult.Ok(
            ServerProfile(
                firstName = p["firstName"]?.jsonPrimitive?.content ?: "",
                lastName = p["lastName"]?.jsonPrimitive?.content ?: "",
                city = p["city"]?.jsonPrimitive?.content ?: "Bakı",
                onboarded = p["onboarded"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        )
    }.getOrElse { ApiResult.Err("Server cavabı oxuna bilmədi.") }

    private fun jsonBody(build: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): String =
        buildJsonObject(build).toString()

    private suspend fun httpGet(path: String): HttpOutcome = withContext(Dispatchers.IO) {
        runCatching {
            execute(Request.Builder().url(baseUrl + path).get().build())
        }.getOrElse { HttpOutcome.Network(it.message ?: "şəbəkə xətası") }
    }

    private suspend fun httpPost(path: String, body: String): HttpOutcome = withContext(Dispatchers.IO) {
        runCatching {
            execute(Request.Builder().url(baseUrl + path).post(body.toRequestBody(jsonType)).build())
        }.getOrElse { HttpOutcome.Network(it.message ?: "şəbəkə xətası") }
    }

    private fun execute(req: Request): HttpOutcome {
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return when {
                resp.code == 401 -> HttpOutcome.Unauthorized
                resp.code == 409 -> HttpOutcome.Conflict
                resp.code in 500..599 -> HttpOutcome.Server(resp.code)
                resp.isSuccessful -> {
                    if (resp.code == 201) HttpOutcome.Created else HttpOutcome.Ok(body)
                }
                else -> HttpOutcome.Server(resp.code)
            }
        }
    }

    private fun unreachable() =
        "Serverə qoşulunmur ($baseUrl). İnterneti və server ünvanını yoxla."

    private companion object {
        const val KEY_URL = "base_url"
        const val DEFAULT_URL = "https://api.qoshul.az"
    }
}

/** API səthinin kontraktı — mühərrik (AppEngine) bu interfeysə bağlı qalır. */
interface QoshulApi {
    /** Server bazası (Ayarlar → Server-də dəyişilir). */
    var baseUrl: String

    /** Bağlantı vəziyyəti — UI göstəriciləri üçün (AppShell nişanı, Auth kartı). */
    val connection: StateFlow<ConnectionState>

    suspend fun checkConnection(): Boolean
    suspend fun login(email: String, password: String): ApiResult<ApiClient.ServerProfile>
    suspend fun register(email: String, password: String, name: String): ApiResult<ApiClient.ServerProfile>
    suspend fun forgotPassword(email: String): ApiResult<Unit>
    suspend fun assistant(message: String): ApiResult<String>
    suspend fun postEvent(payload: String): ApiResult<Unit>
}

/** Bağlantı vəziyyəti (AppShell başlıq nişanı + Auth status kartı). */
enum class ConnectionState { UNKNOWN, CHECKING, ONLINE, OFFLINE }

/** Vahid nəticə: Ok(data) | Err(istifadəyə hazır azərbaycanca mesaj). */
sealed interface ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>
    data class Err(val message: String) : ApiResult<Nothing>
}

internal sealed interface HttpOutcome {
    data class Ok(val body: String) : HttpOutcome
    data object Created : HttpOutcome
    data object Unauthorized : HttpOutcome
    data object Conflict : HttpOutcome
    data class Server(val code: Int) : HttpOutcome
    data class Network(val message: String) : HttpOutcome
}

internal fun HttpOutcome.isOk(): Boolean = this is HttpOutcome.Ok || this is HttpOutcome.Created
