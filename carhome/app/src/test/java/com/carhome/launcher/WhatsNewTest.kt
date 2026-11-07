package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewTest {

    private val text = """
        # CarHome 1.2.10

        ## en
        - Settings backup and restore
        - Speed camera warnings

        ## ar
        - نسخ الإعدادات واستعادتها
        - تنبيه كاميرات السرعة

        versionCode: 13
    """.trimIndent()

    @Test fun picksTheEnglishList() =
        assertEquals(listOf("Settings backup and restore", "Speed camera warnings"), WhatsNew.parse(text, "en"))

    @Test fun picksTheArabicList() =
        assertEquals(listOf("نسخ الإعدادات واستعادتها", "تنبيه كاميرات السرعة"), WhatsNew.parse(text, "ar"))

    @Test fun otherLanguagesGetEnglish() =
        assertEquals(2, WhatsNew.parse(text, "fr").size)

    @Test fun notesWithoutLanguageSectionsAreReadAsOneList() =
        assertEquals(listOf("One", "Two"), WhatsNew.parse("Intro line\n- One\n* Two\nversionCode: 3", "ar"))

    @Test fun readsTheTitleVersion() = assertEquals("1.2.10", WhatsNew.version(text))

    @Test fun emptyOrMissingGivesNothing() {
        assertEquals(emptyList<String>(), WhatsNew.parse(null, "en"))
        assertEquals(emptyList<String>(), WhatsNew.parse("versionCode: 3", "en"))
    }
}
