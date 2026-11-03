package au.com.foodtruckcentre.app

import au.com.foodtruckcentre.app.data.SiteContent.Cuisine
import au.com.foodtruckcentre.app.data.Validation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ValidationTest {
    private val today = LocalDate.of(2026, 11, 3)

    @Test fun emailRules() {
        assertNull(Validation.email("jane@example.com"))
        assertNull(Validation.email("jane.o'neil+events@mail.example.com.au"))
        assertNotNull(Validation.email(""))
        assertNotNull(Validation.email("jane@"))
        assertNotNull(Validation.email("jane@example"))
        assertNotNull(Validation.email("ja ne@example.com"))
        assertNotNull(Validation.email("jane@@example.com"))
    }

    @Test fun phoneRules() {
        assertNull(Validation.phone("0412 345 678"))
        assertNull(Validation.phone("+61 2 9876 5432"))
        assertNull(Validation.phone("(02) 9876-5432"))
        assertNotNull(Validation.phone("12345"))
        assertNotNull(Validation.phone("phone me"))
        assertNotNull(Validation.phone(""))
    }

    @Test fun mealsMinimumIs50() {
        assertNotNull(Validation.meals("49"))
        assertNull(Validation.meals("50"))
        assertNull(Validation.meals("1200"))
        assertNotNull(Validation.meals(""))
        assertNotNull(Validation.meals("fifty"))
    }

    @Test fun eventDateCannotBeInThePast() {
        assertNotNull(Validation.eventDate(null, today))
        assertNotNull(Validation.eventDate(today.minusDays(1), today))
        assertNull(Validation.eventDate(today, today))
        assertNull(Validation.eventDate(today.plusMonths(2), today))
        assertNotNull(Validation.eventDate(today.plusYears(4), today))
    }

    @Test fun postcodeIsFourDigits() {
        assertNull(Validation.postcode("2000"))
        assertNotNull(Validation.postcode("200"))
        assertNotNull(Validation.postcode("20000"))
        assertNotNull(Validation.postcode("ABCD"))
    }

    @Test fun maxThreeCuisines() {
        assertNull(Validation.cuisines(listOf(Cuisine.PIZZA, Cuisine.BBQ, Cuisine.SEAFOOD)))
        assertNotNull(Validation.cuisines(listOf(Cuisine.PIZZA, Cuisine.BBQ, Cuisine.SEAFOOD, Cuisine.VEGAN)))
    }

    @Test fun cuisineFoundInFeaturingText() {
        assertEquals(Cuisine.PIZZA, Cuisine.findIn("click HERE and then select the Pizza / Italian\ncuisine from the list."))
        assertNull(Cuisine.findIn("No cuisine here"))
    }

    @Test fun nameAndMessage() {
        assertNotNull(Validation.name(" "))
        assertNull(Validation.name("Jo Lee"))
        assertNotNull(Validation.message("hi"))
        assertNotNull(Validation.links("http://a http://b http://c www.d"))
        assertNull(Validation.links("see http://a"))
    }
}
