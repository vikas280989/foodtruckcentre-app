package au.com.foodtruckcentre.app.ui.screens

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.foodtruckcentre.app.BuildConfig
import au.com.foodtruckcentre.app.data.Api
import au.com.foodtruckcentre.app.data.ApiResult
import au.com.foodtruckcentre.app.data.ContactRequest
import au.com.foodtruckcentre.app.data.HtmlBlock
import au.com.foodtruckcentre.app.data.HtmlParser
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.data.Validation
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/** Loading state for website content. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Error(val message: String, val offline: Boolean) : Loadable<Nothing>
}

data class FeaturedContent(
    val headline: String?,
    val blocks: List<HtmlBlock>,
    val imageUrl: String?,
    val cuisine: SiteContent.Cuisine?,
)

data class LegalContent(val blocks: List<HtmlBlock>, val updated: String, val offlineCopy: Boolean)

/**
 * Website content (featured truck, legal pages) loaded live from foodtruckcentre.com.au,
 * so changes on the website appear in the app without a new APK.
 */
class ContentViewModel(private val app: Application) : AndroidViewModel(app) {

    private val api = Api.get(app)

    var featured by mutableStateOf<Loadable<FeaturedContent?>>(Loadable.Loading)
        private set
    val legal = mutableStateMapOf<SiteContent.LegalPage, Loadable<LegalContent>>()
    var contactEmail by mutableStateOf<String?>(null)
        private set

    init {
        loadFeatured()
        viewModelScope.launch { contactEmail = api.contactEmail() }
    }

    fun loadFeatured() {
        featured = Loadable.Loading
        viewModelScope.launch {
            featured = when (val r = api.page("featuring")) {
                is ApiResult.Success -> Loadable.Ready(parseFeatured(r.value.html))
                is ApiResult.Failure -> Loadable.Error(r.message, r.offline)
                is ApiResult.Invalid -> Loadable.Error(r.message, false)
            }
        }
    }

    fun refreshContactEmail() {
        if (contactEmail == null) viewModelScope.launch { contactEmail = api.contactEmail() }
    }

    private fun parseFeatured(html: String): FeaturedContent? {
        val blocks = HtmlParser.parse(html)
        val text = HtmlParser.plainText(html)
        if (text.isBlank() && blocks.none { it is HtmlBlock.Image }) return null // nothing featured right now
        val headline = blocks.filterIsInstance<HtmlBlock.Heading>().firstOrNull()?.text
        return FeaturedContent(
            headline = headline,
            blocks = blocks.filterNot { it is HtmlBlock.Image || (it is HtmlBlock.Heading && it.text == headline) },
            imageUrl = blocks.filterIsInstance<HtmlBlock.Image>().firstOrNull()?.url,
            cuisine = SiteContent.Cuisine.findIn(text),
        )
    }

    fun loadLegal(page: SiteContent.LegalPage, force: Boolean = false) {
        if (!force && legal[page] is Loadable.Ready && (legal[page] as Loadable.Ready).value.offlineCopy.not()) return
        legal[page] = Loadable.Loading
        viewModelScope.launch {
            legal[page] = when (val r = api.page(page.slug)) {
                is ApiResult.Success -> Loadable.Ready(LegalContent(HtmlParser.parse(r.value.html), r.value.modified, offlineCopy = false))
                else -> bundledLegal(page) ?: Loadable.Error(
                    (r as? ApiResult.Failure)?.message ?: "We couldn't load this page right now.",
                    (r as? ApiResult.Failure)?.offline == true,
                )
            }
        }
    }

    /** Copy of the page bundled with the app (taken from the website), shown only when offline. */
    private fun bundledLegal(page: SiteContent.LegalPage): Loadable<LegalContent>? = runCatching {
        val raw = app.assets.open("legal/${page.slug}.json").bufferedReader().use { it.readText() }
        val obj = Json.parseToJsonElement(raw).jsonObject
        Loadable.Ready(
            LegalContent(
                HtmlParser.parse(obj["html"]!!.jsonPrimitive.content),
                obj["modified"]!!.jsonPrimitive.content,
                offlineCopy = true,
            )
        )
    }.getOrNull()
}

class ContactViewModel(app: Application) : AndroidViewModel(app) {
    private val api = Api.get(app)

    val topics = listOf("General enquiry", "Corporate catering", "Feedback", "App support")

    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var topic by mutableStateOf(topics.first())
    var message by mutableStateOf("")
    val errors = mutableStateMapOf<String, String>()
    var submitting by mutableStateOf(false)
        private set
    var banner by mutableStateOf<QuoteViewModel.Banner?>(null)
    var sentId by mutableStateOf<String?>(null)

    private var submissionId = UUID.randomUUID().toString()
    private var startedAt = 0L

    fun touch() {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
    }

    fun submit() {
        if (submitting) return
        banner = null
        errors.clear()
        Validation.name(name)?.let { errors["name"] = it }
        Validation.email(email)?.let { errors["email"] = it }
        (Validation.message(message) ?: Validation.links(message))?.let { errors["message"] = it }
        if (errors.isNotEmpty()) {
            banner = QuoteViewModel.Banner("Please check the highlighted fields.", false)
            return
        }
        submitting = true
        val req = ContactRequest(
            submissionId = submissionId,
            elapsedSeconds = if (startedAt == 0L) 0 else (System.currentTimeMillis() - startedAt) / 1000,
            name = name.trim(),
            email = email.trim(),
            topic = topic,
            message = message.trim(),
            appVersion = BuildConfig.VERSION_NAME,
            // Device details are only sent for app support requests.
            device = if (topic == "App support") "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})".take(80) else "",
        )
        viewModelScope.launch {
            when (val r = api.submitContact(req)) {
                is ApiResult.Success -> {
                    sentId = r.value
                    name = ""; email = ""; message = ""; topic = topics.first()
                    submissionId = UUID.randomUUID().toString(); startedAt = 0L
                }
                is ApiResult.Invalid -> { errors.putAll(r.fields); banner = QuoteViewModel.Banner(r.message, false) }
                is ApiResult.Failure -> banner = QuoteViewModel.Banner(r.message, r.offline)
            }
            submitting = false
        }
    }
}
