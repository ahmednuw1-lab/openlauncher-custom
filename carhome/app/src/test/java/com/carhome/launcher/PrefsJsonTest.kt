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
        Background.entries, SpeedSource.entries, SoundChannel.entries,
        TopBar.entries, CardFill.entries, Corners.entries, IconSize.entries, IconShape.entries
    ).flatten().map { it.name }.toSet()

    // ── 1.2.11: top bar choice and the look ──

    @Test fun lookSettingsRoundTrip() {
        val s = defaults.copy(
            topBar = TopBar.MUSIC, cardFill = CardFill.SOLID, corners = Corners.SQUARE, highContrast = true,
            iconSize = IconSize.LARGE, iconShape = IconShape.CIRCLE, accent = 0xFF7DD3FC
        )
        assertEquals(s, roundTrip(s))
    }

    @Test fun olderSettingsWithWeatherOnKeepTheWeatherStrip() {
        val o = JSONObject().put("showWeather", true)
        assertEquals(TopBar.WEATHER, settingsFromJson(o).topBar)
    }

    @Test fun olderSettingsWithWeatherOffKeepAnEmptyTopBar() {
        val o = JSONObject().put("showWeather", false)
        val s = settingsFromJson(o)
        assertEquals(TopBar.NONE, s.topBar)
        assertEquals(false, s.showWeather)
    }

    @Test fun theNewTopBarChoiceWinsOverTheOldSwitch() {
        val o = JSONObject().put("showWeather", true).put("topBar", "DATE")
        assertEquals(TopBar.DATE, settingsFromJson(o).topBar)
    }

    @Test fun aSavedAccentIsMadeOpaque() {
        assertEquals(0xFF22D3EE, settingsFromJson(JSONObject().put("accent", 0x0022D3EEL)).accent)
    }

    @Test fun anyCustomAccentRoundTrips() {
        val c = AccentColors.custom(200f, 0.6f)
        assertEquals(c, roundTrip(defaults.copy(accent = c)).accent)
    }
}
