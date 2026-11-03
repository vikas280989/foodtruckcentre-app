package au.com.foodtruckcentre.app.data

import androidx.annotation.DrawableRes
import au.com.foodtruckcentre.app.R

/**
 * Content taken from https://foodtruckcentre.com.au/ (audited 3 Nov 2026).
 * Text is reproduced as published on the website – do not add services, claims or vendors here
 * that the website does not show. Featured truck and legal pages are loaded live from the website.
 */
object SiteContent {

    const val HERO_TITLE = "Book Australia’s\nBest Food Trucks"

    const val INTRO =
        "FoodTruckCentre.com.au helps you create amazing events with Australia’s best food trucks by providing " +
            "delicious and flexible catering options for corporate parties, weddings and other events."

    const val INTRO_2 =
        "From tacos, souvlakis, wood-fired pizzas to Asian food and more, food trucks make every event deliciously memorable."

    const val INTRO_3 =
        "Share your event details with us below to receive quotes and menus from a number of sensational food trucks."

    const val FORM_INTRO = "Complete the form below to receive quotes from Australia's best food trucks"

    const val CONTACT_INTRO = "If you have comments or questions please let us know"

    const val INSTAGRAM_URL = "https://www.instagram.com/foodtruckcentre"

    const val COPYRIGHT = "©2026 Food Truck Centre All Rights Reserved."

    data class Highlight(val title: String, val body: String, val kind: Kind) {
        enum class Kind { MENUS, EVENTS, EXPERIENCE }
    }

    val highlights = listOf(
        Highlight(
            "Wide selection of menus",
            "Choose from a diverse range of menu options crafted to suit every taste, occasion, and dietary preference.",
            Highlight.Kind.MENUS,
        ),
        Highlight(
            "500+ Events booked",
            "Trusted by hundreds of clients with a proven track record of successfully managing and delivering memorable events.",
            Highlight.Kind.EVENTS,
        ),
        Highlight(
            "6+ Years of experience",
            "Over six years of industry experience providing reliable service, professional expertise, and consistent quality.",
            Highlight.Kind.EXPERIENCE,
        ),
    )

    /** Cuisine categories exactly as listed on the website's quote form ("tick 3 max."). */
    enum class Cuisine(val label: String, val description: String) {
        BURGERS("Burgers / American", "Burgers, loaded fries"),
        MEXICAN("Mexican / Latin", "Tacos, burritos, quesadillas"),
        ASIAN("Asian Fusion", "Bao buns, dumplings, Korean chicken"),
        VIETNAMESE("Vietnamese", "Banh mi, rice bowls"),
        MEDITERRANEAN("Mediterranean / Middle Eastern", "Souvlaki, falafel, shawarma, kebabs"),
        BBQ("BBQ / Smoked Meats", "Brisket, ribs, pulled pork"),
        PIZZA("Pizza / Italian", "Wood-fired pizza, pizza slices"),
        SEAFOOD("Seafood", "Fish tacos, calamari, lobster rolls"),
        VEGAN("Vegan / Healthy", "Vegan burgers, wraps"),
        DESSERTS("Desserts / Coffee / Sweets", "Ice cream, churros, waffles, espresso");

        companion object {
            /** Finds a cuisine mentioned in free text, e.g. the Featuring page ("Pizza / Italian"). */
            fun findIn(text: String): Cuisine? {
                val normalised = text.replace(Regex("\\s+"), " ").lowercase()
                return entries.firstOrNull { c ->
                    normalised.contains(c.label.lowercase()) ||
                        normalised.contains(c.label.replace(" / ", "/").lowercase())
                }
            }
        }
    }

    const val MAX_CUISINES = 3
    const val MIN_MEALS = 50

    /** Dropdown options exactly as on the website form. */
    val eventTypes = listOf("Office Celebration", "Birthday", "Other")
    val payers = listOf("Organiser", "Guests")
    val yesNo = listOf("Yes", "No")

    data class Service(
        val id: String,
        val title: String,
        val description: String,
        @DrawableRes val image: Int,
        /** Event type to preselect – only where the website's own options clearly match. */
        val presetEventType: String?,
    )

    val services = listOf(
        Service("corporate", "Corporate Catering",
            "Professional catering solutions tailored for business meetings, office lunches, and corporate functions.",
            R.drawable.svc_corporate, presetEventType = "Office Celebration"),
        Service("conferences", "Conferences & Conventions",
            "Reliable food services designed for conferences, seminars, and large professional gatherings.",
            R.drawable.svc_conferences, presetEventType = null),
        Service("university", "University & School Events",
            "Fresh and quality catering for educational institutions, campus events, and academic celebrations.",
            R.drawable.svc_university, presetEventType = null),
        Service("production", "Production Catering",
            "Efficient catering support for film crews, media teams, and production environments.",
            R.drawable.svc_production, presetEventType = null),
        Service("private", "Private Parties",
            "Delicious menus crafted for birthdays, anniversaries, and personal celebrations.",
            R.drawable.svc_private, presetEventType = null),
        Service("largescale", "Large-Scale Events",
            "Expert catering management for weddings, festivals, and high-capacity events.",
            R.drawable.svc_largescale, presetEventType = null),
    )

    /** Legal pages published on the website (loaded live; a bundled copy is shown offline). */
    enum class LegalPage(val slug: String, val title: String) {
        PRIVACY("privacy-policy", "Privacy Policy"),
        TERMS("terms-and-conditions", "Terms and Conditions"),
        VENDOR("vendor-terms", "Vendor Terms"),
    }
}
