package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Arabic text, but Western digits (123) everywhere. */
class DigitsTest {

    private val arabic = Digits.latin(Locale.forLanguageTag("ar"))

    @Test fun numbersAreWestern() = assertEquals("120", String.format(arabic, "%d", 120))

    @Test fun decimalsAreWestern() = assertEquals("2.33", String.format(arabic, "%.2f", 2.33).replace('٫', '.'))

    @Test fun clockIsWestern() {
        val f = SimpleDateFormat("HH:mm", arabic).apply { timeZone = TimeZone.getTimeZone("UTC") }
        assertEquals("18:25", f.format(Date(18 * 3_600_000L + 25 * 60_000L)))
    }

    @Test fun languageStaysArabic() = assertEquals("ar", arabic.language)

    @Test fun englishIsUnchanged() = assertEquals("120", String.format(Digits.latin(Locale.ENGLISH), "%d", 120))
}
