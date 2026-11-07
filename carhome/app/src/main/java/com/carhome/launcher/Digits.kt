package com.carhome.launcher

import java.util.Locale

/** CarHome shows Western digits (123) in every language, including Arabic. */
object Digits {

    /** The same language and region, with Western digits for every number, date and clock. */
    fun latin(locale: Locale): Locale =
        Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", "latn").build()
}
