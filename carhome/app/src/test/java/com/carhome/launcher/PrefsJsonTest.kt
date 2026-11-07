package com.carhome.launcher

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Settings must survive updates: every field round-trips, and bad or old data falls back per field. */
class PrefsJsonTest {

    private val defaults = LauncherSettings()

    private fun roundTrip(s: LauncherSettings) = settingsFromJson(JSONObject(settingsToJson(s).toString()))

    @Test fun defaultsRoundTrip() = assertEquals(defaults, roundTrip(defaults))

    @Test fun emptyJsonGivesDefaults() = assertEquals(defaults, settingsFromJson(JSONObject()))

    @Test fun changedSettingsRoundTrip() {
        val s = defaults.copy(
            speedAlert = !defaults.speedAlert,
            speedLimit = 100,
            alertMode = AlertMode.BEEP_REPEAT,
            alertChannel = SoundChannel.MEDIA,
            speedSource = SpeedSource.GPS,
            approxSpeed = !defaults.approxSpeed,
            useKmh = !defaults.useKmh,
            prayerMethod = PrayerMethod.MWL,
            language = AppLanguage.ARABIC,
            background = Background.SKY,
            cards = listOf(CardType.SPEED, CardType.PRAYER, CardType.CLOCK)
        )
        assertEquals(s, roundTrip(s))
    }

    @Test fun unknownValueInAnyEnumFieldFallsBackToItsDefault() {
        // Every enum-valued key in the saved JSON
        val base = settingsToJson(defaults)
        val enumKeys = base.keys().asSequence().filter { k ->
            val v = base.opt(k)
            v is String && enumNames().contains(v)
        }.toList()
        for (k in enumKeys) {
            val o = JSONObject(base.toString())
            o.put(k, "NOT_A_REAL_VALUE")
            assertEquals("unknown value for $k falls back to the default", defaults, settingsFromJson(o))
        }
    }

    @Test fun unknownEnumNameKeepsOtherFields() {
        val o = settingsToJson(defaults.copy(speedLimit = 90))
        o.put("alertMode", "SOME_FUTURE_MODE")
        val s = settingsFromJson(o)
        assertEquals(defaults.alertMode, s.alertMode)
        assertEquals(90, s.speedLimit)
    }

    @Test fun wrongTypesFallBackToDefaults() {
        val o = settingsToJson(defaults)
        o.put("speedLimit", "fast")
        o.put("speedAlert", "maybe")
        o.put("tileCount", 7)   // not one of 4/6/8/12
        val s = settingsFromJson(o)
        assertEquals(defaults.speedLimit, s.speedLimit)
        assertEquals(defaults.speedAlert, s.speedAlert)
        assertEquals(defaults.tileCount, s.tileCount)
    }

    @Test fun cardsDropUnknownAndDuplicates() {
        val o = settingsToJson(defaults)
        o.put("cards", JSONArray(listOf("SPEED", "UFO", "SPEED", "CLOCK")))
        assertEquals(listOf(CardType.SPEED, CardType.CLOCK), settingsFromJson(o).cards)
    }

    @Test fun extraKeysFromNewerVersionsAreIgnored() {
        val o = settingsToJson(defaults)
        o.put("someNewSetting", true)
        assertEquals(defaults, settingsFromJson(o))
    }

    private fun enumNames(): Set<String> = listOf(
        AlertMode.entries, CardType.entries, Side.entries, ColumnWidth.entries, TextSize.entries,
        SpeedStyle.entries, AppLanguage.entries, NoRoadData.entries, PrayerMethod.entries,
        TripReset.entries, NightMode.entries, BubbleSize.entries, VolumeBoost.entries,
        Background.entries, SpeedSource.entries, SoundChannel.entries
    ).flatten().map { it.name }.toSet()
}
