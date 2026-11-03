package au.com.foodtruckcentre.app.data

import java.time.LocalDate

/** Client-side validation. The server repeats every check – this is for instant feedback only. */
object Validation {

    private val emailRegex = Regex("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneAllowed = Regex("^[0-9+()\\-\\s]{8,20}$")
    private val postcodeRegex = Regex("^\\d{4}$")

    fun name(value: String): String? =
        if (value.trim().length < 2) "Please enter your full name." else null

    fun email(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Please enter your email address."
            v.length > 254 || !emailRegex.matches(v) || v.contains("..") -> "Please enter a valid email address."
            else -> null
        }
    }

    fun phone(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return "Please enter your phone number."
        val digits = v.count { it.isDigit() }
        return if (!phoneAllowed.matches(v) || digits < 8 || digits > 15) "Please enter a valid phone number." else null
    }

    fun eventDate(value: LocalDate?, today: LocalDate = LocalDate.now()): String? = when {
        value == null -> "Please choose the event date."
        value.isBefore(today) -> "The event date can't be in the past."
        value.isAfter(today.plusYears(3)) -> "Please choose a date within the next 3 years."
        else -> null
    }

    fun postcode(value: String): String? =
        if (!postcodeRegex.matches(value.trim())) "Please enter a valid 4-digit Australian postcode." else null

    fun meals(value: String): String? {
        val n = value.trim().toIntOrNull()
        return when {
            value.isBlank() -> "Please enter the number of meals."
            n == null -> "Please enter a whole number."
            n < SiteContent.MIN_MEALS -> "The minimum number of meals is ${SiteContent.MIN_MEALS}."
            n > 100_000 -> "Please enter a realistic number of meals."
            else -> null
        }
    }

    fun required(value: String?, message: String = "Please choose an option."): String? =
        if (value.isNullOrBlank()) message else null

    fun cuisines(selected: Collection<SiteContent.Cuisine>): String? =
        if (selected.size > SiteContent.MAX_CUISINES) "Please select a maximum of three cuisines." else null

    fun message(value: String): String? =
        if (value.trim().length < 5) "Please write a short message." else null

    private val linkRegex = Regex("(https?://|www\\.)", RegexOption.IGNORE_CASE)

    fun links(value: String): String? =
        if (linkRegex.findAll(value).count() > 3) "Please remove some of the links from your message." else null
}
