package au.com.foodtruckcentre.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.foodtruckcentre.app.data.Api
import au.com.foodtruckcentre.app.data.ApiResult
import au.com.foodtruckcentre.app.data.QuoteRequest
import au.com.foodtruckcentre.app.data.SiteContent
import au.com.foodtruckcentre.app.data.SiteContent.Cuisine
import au.com.foodtruckcentre.app.data.Validation
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Field keys – also used by the server in validation responses. */
object QuoteField {
    const val CUISINES = "cuisines"
    const val NAME = "name"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val EVENT_DATE = "event_date"
    const val FOOD_TIME = "food_time"
    const val ADDRESS = "event_address"
    const val POSTCODE = "postcode"
    const val EVENT_TYPE = "event_type"
    const val MEALS = "meals"
    const val PAYING = "paying"
    const val POWER = "power"
    const val PRIVATE_LAND = "private_land"
    const val ENTRANCE = "entrance"
    const val INFO = "additional_info"

    /** Display order, used to scroll to the first problem. */
    val order = listOf(CUISINES, NAME, EMAIL, PHONE, EVENT_DATE, FOOD_TIME, ADDRESS, POSTCODE, EVENT_TYPE, MEALS, PAYING, POWER, PRIVATE_LAND, ENTRANCE, INFO)
}

/**
 * Holds the quote form. Kept for the life of the app so switching tabs never loses typed details.
 * Nothing is written to disk.
 */
class QuoteViewModel(app: Application) : AndroidViewModel(app) {

    private val api = Api.get(app)

    val selectedCuisines = mutableStateListOf<Cuisine>()
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var eventDate by mutableStateOf<LocalDate?>(null)
    var foodTime by mutableStateOf("")
    var address by mutableStateOf("")
    var postcode by mutableStateOf("")
    var eventType by mutableStateOf<String?>(null)
    var meals by mutableStateOf("")
    var paying by mutableStateOf<String?>(null)
    var power by mutableStateOf<String?>(null)
    var privateLand by mutableStateOf<String?>(null)
    var entrance by mutableStateOf<String?>(null)
    var info by mutableStateOf("")

    val errors = mutableStateMapOf<String, String>()
    var submitting by mutableStateOf(false)
        private set
    var banner by mutableStateOf<Banner?>(null)
    var cuisineLimitHit by mutableStateOf(0) // increments to trigger the "max 3" message
        private set
    var scrollTarget by mutableStateOf<String?>(null)
    var success by mutableStateOf<Success?>(null)

    data class Banner(val message: String, val offline: Boolean)
    data class Success(val enquiryId: String, val firstName: String)

    private var submissionId = UUID.randomUUID().toString()
    private var startedAt = 0L

    fun touch() {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
    }

    /** Called from Services / Featuring / Home before opening the form. */
    fun prefill(cuisine: Cuisine? = null, eventType: String? = null) {
        touch()
        if (cuisine != null && cuisine !in selectedCuisines) {
            if (selectedCuisines.size < SiteContent.MAX_CUISINES) selectedCuisines.add(cuisine) else cuisineLimitHit++
        }
        if (eventType != null && eventType in SiteContent.eventTypes) this.eventType = eventType
        errors.remove(QuoteField.CUISINES)
    }

    fun toggleCuisine(c: Cuisine) {
        touch()
        if (c in selectedCuisines) {
            selectedCuisines.remove(c)
        } else if (selectedCuisines.size >= SiteContent.MAX_CUISINES) {
            cuisineLimitHit++
            return
        } else {
            selectedCuisines.add(c)
        }
        errors.remove(QuoteField.CUISINES)
    }

    fun clearError(key: String) {
        errors.remove(key)
    }

    fun validate(today: LocalDate = LocalDate.now()): Map<String, String> {
        val e = linkedMapOf<String, String>()
        Validation.cuisines(selectedCuisines)?.let { e[QuoteField.CUISINES] = it }
        Validation.name(name)?.let { e[QuoteField.NAME] = it }
        Validation.email(email)?.let { e[QuoteField.EMAIL] = it }
        Validation.phone(phone)?.let { e[QuoteField.PHONE] = it }
        Validation.eventDate(eventDate, today)?.let { e[QuoteField.EVENT_DATE] = it }
        Validation.postcode(postcode)?.let { e[QuoteField.POSTCODE] = it }
        Validation.required(eventType, "Please choose the type of event.")?.let { e[QuoteField.EVENT_TYPE] = it }
        Validation.meals(meals)?.let { e[QuoteField.MEALS] = it }
        Validation.required(paying, "Please tell us who will be paying for food.")?.let { e[QuoteField.PAYING] = it }
        Validation.required(power, "Please answer this question.")?.let { e[QuoteField.POWER] = it }
        Validation.required(privateLand, "Please answer this question.")?.let { e[QuoteField.PRIVATE_LAND] = it }
        Validation.required(entrance, "Please answer this question.")?.let { e[QuoteField.ENTRANCE] = it }
        Validation.links(info)?.let { e[QuoteField.INFO] = it }
        return e
    }

    fun submit() {
        if (submitting) return // duplicate-tap protection
        banner = null
        val found = validate()
        errors.clear()
        errors.putAll(found)
        if (found.isNotEmpty()) {
            scrollTarget = QuoteField.order.firstOrNull { it in found }
            banner = Banner(if (found.size == 1) "Please check the highlighted field." else "Please check the ${found.size} highlighted fields.", false)
            return
        }
        val date = eventDate ?: return
        submitting = true
        val request = QuoteRequest(
            submissionId = submissionId,
            elapsedSeconds = if (startedAt == 0L) 0 else (System.currentTimeMillis() - startedAt) / 1000,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            eventDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            foodTime = foodTime.trim(),
            eventAddress = address.trim(),
            postcode = postcode.trim(),
            eventType = eventType.orEmpty(),
            meals = meals.trim().toInt(),
            paying = paying.orEmpty(),
            power = power.orEmpty(),
            privateLand = privateLand.orEmpty(),
            entrance = entrance.orEmpty(),
            cuisines = Cuisine.entries.filter { it in selectedCuisines }.map { it.label },
            additionalInfo = info.trim(),
        )
        viewModelScope.launch {
            when (val r = api.submitQuote(request)) {
                is ApiResult.Success -> {
                    success = Success(r.value, name.trim().substringBefore(' '))
                    reset()
                }
                is ApiResult.Invalid -> {
                    errors.putAll(r.fields)
                    scrollTarget = QuoteField.order.firstOrNull { it in r.fields }
                    banner = Banner(r.message, false)
                }
                is ApiResult.Failure -> banner = Banner(r.message, r.offline)
            }
            submitting = false
        }
    }

    private fun reset() {
        selectedCuisines.clear()
        name = ""; email = ""; phone = ""; eventDate = null; foodTime = ""; address = ""; postcode = ""
        eventType = null; meals = ""; paying = null; power = null; privateLand = null; entrance = null; info = ""
        errors.clear(); banner = null
        submissionId = UUID.randomUUID().toString()
        startedAt = 0L
    }

    companion object {
        val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale("en", "AU"))
    }
}
