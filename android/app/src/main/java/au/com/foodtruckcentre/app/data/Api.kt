package au.com.foodtruckcentre.app.data

import android.content.Context
import au.com.foodtruckcentre.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Result of a network call, with messages written for customers (never raw technical errors). */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Invalid(val message: String, val fields: Map<String, String>) : ApiResult<Nothing>
    data class Failure(val message: String, val offline: Boolean = false) : ApiResult<Nothing>
}

@Serializable
data class QuoteRequest(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("elapsed_seconds") val elapsedSeconds: Long,
    val website: String = "", // honeypot – always empty from the real app
    val name: String,
    val email: String,
    val phone: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("food_time") val foodTime: String,
    @SerialName("event_address") val eventAddress: String,
    val postcode: String,
    @SerialName("event_type") val eventType: String,
    val meals: Int,
    val paying: String,
    val power: String,
    @SerialName("private_land") val privateLand: String,
    val entrance: String,
    val cuisines: List<String>,
    @SerialName("additional_info") val additionalInfo: String,
)

@Serializable
data class ContactRequest(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("elapsed_seconds") val elapsedSeconds: Long,
    val website: String = "",
    val name: String,
    val email: String,
    val topic: String,
    val message: String,
    @SerialName("app_version") val appVersion: String,
    val device: String,
)

@Serializable
private data class SubmitResponse(
    val ok: Boolean = false,
    @SerialName("enquiry_id") val enquiryId: String? = null,
    val message: String? = null,
    val fields: Map<String, String> = emptyMap(),
)

@Serializable
private data class AppConfigResponse(
    val ok: Boolean = false,
    @SerialName("contact_email") val contactEmail: String? = null,
)

/** A page from the WordPress site (via its public REST API). */
data class WebPage(val title: String, val html: String, val modified: String, val fromCache: Boolean)

class Api private constructor(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // never silently re-send a form
        .followSslRedirects(true)
        .followRedirects(true)
        // Small HTTP cache for website content (pages/images); never used for form submissions.
        .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val userAgent = "FoodTruckCentreApp/${BuildConfig.VERSION_NAME} (Android)"

    suspend fun submitQuote(request: QuoteRequest): ApiResult<String> =
        post("quote.php", json.encodeToString(QuoteRequest.serializer(), request))

    suspend fun submitContact(request: ContactRequest): ApiResult<String> =
        post("contact.php", json.encodeToString(ContactRequest.serializer(), request))

    /** Public business contact email configured on the server (null = hide the button). */
    suspend fun contactEmail(): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(BuildConfig.API_BASE_URL + "app-config.php")
                .header("User-Agent", userAgent)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = json.decodeFromString(AppConfigResponse.serializer(), resp.body?.string().orEmpty())
                body.contactEmail?.takeIf { it.contains('@') }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Loads a page from the website's WordPress REST API by slug. */
    suspend fun page(slug: String): ApiResult<WebPage> = withContext(Dispatchers.IO) {
        try {
            val url = BuildConfig.WEBSITE_URL + "wp-json/wp/v2/pages?slug=$slug&_fields=title,content,modified"
            val req = Request.Builder().url(url).header("User-Agent", userAgent).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ApiResult.Failure("We couldn't load this page right now.")
                val arr = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonArray
                val obj = arr.firstOrNull()?.jsonObject
                    ?: return@withContext ApiResult.Failure("This page isn't available on the website right now.")
                ApiResult.Success(
                    WebPage(
                        title = obj.rendered("title"),
                        html = obj.rendered("content"),
                        modified = (obj["modified"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                        fromCache = resp.networkResponse == null,
                    )
                )
            }
        } catch (e: Exception) {
            networkFailure(e)
        }
    }

    private fun JsonObject.rendered(key: String): String =
        ((this[key] as? JsonObject)?.get("rendered") as? JsonPrimitive)?.contentOrNull.orEmpty()

    private suspend fun post(path: String, body: String): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + path)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .post(body.toRequestBody(jsonType))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val parsed = runCatching { json.decodeFromString(SubmitResponse.serializer(), text) }.getOrNull()
                when {
                    resp.isSuccessful && parsed?.ok == true && !parsed.enquiryId.isNullOrBlank() ->
                        ApiResult.Success(parsed.enquiryId)
                    resp.code == 422 && parsed != null ->
                        ApiResult.Invalid(parsed.message ?: "Please check the highlighted fields.", parsed.fields)
                    resp.code == 429 ->
                        ApiResult.Failure(parsed?.message ?: "Too many requests. Please wait a few minutes and try again.")
                    parsed?.message != null && resp.code in 400..599 ->
                        ApiResult.Failure(parsed.message)
                    else ->
                        ApiResult.Failure("We couldn't send your enquiry right now. Please try again in a moment.")
                }
            }
        } catch (e: Exception) {
            networkFailure(e)
        }
    }

    private fun networkFailure(e: Exception): ApiResult.Failure = when (e) {
        is UnknownHostException -> ApiResult.Failure("You appear to be offline. Check your internet connection and try again.", offline = true)
        is javax.net.ssl.SSLException -> ApiResult.Failure("A secure connection couldn't be established. Please try again later.")
        is IOException -> ApiResult.Failure("The connection was interrupted. Please check your internet and try again.", offline = true)
        else -> ApiResult.Failure("Something went wrong. Please try again.")
    }

    companion object {
        @Volatile private var instance: Api? = null
        fun get(context: Context): Api =
            instance ?: synchronized(this) { instance ?: Api(context.applicationContext).also { instance = it } }
    }
}
