package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** The accent palette and the custom colour: always readable on the dark car screen. */
class AccentColorsTest {

    @Test fun sixteenPresetsWithUniqueNamesAndColours() {
        assertEquals(16, AccentColors.PRESETS.size)
        assertEquals(16, AccentColors.PRESETS.map { it.key }.toSet().size)
        assertEquals(16, AccentColors.PRESETS.map { it.argb }.toSet().size)
    }

    @Test fun theSixColoursOfEarlierVersionsAreStillThere() {
        for (old in listOf(0xFF4F8CFF, 0xFF22C55E, 0xFFF59E0B, 0xFFEF4444, 0xFFA855F7, 0xFF14B8A6)) {
            assertTrue("%X".format(old), AccentColors.isPreset(old))
        }
        assertEquals("the default accent stays blue", 0xFF4F8CFF, LauncherSettings().accent)
    }

    @Test fun everyPresetIsClearOnTheDarkScreen() {
        // 3:1 is the WCAG minimum for icons and large text
        for (p in AccentColors.PRESETS) {
            assertTrue("${p.key} contrast", AccentColors.contrast(p.argb, AccentColors.SCREEN) >= 3.0)
        }
    }

    @Test fun textOnEveryPresetIsReadable() {
        for (p in AccentColors.PRESETS) {
            val text = if (AccentColors.darkTextOn(p.argb)) AccentColors.DARK_TEXT else 0xFFFFFFFF
            assertTrue("${p.key} text contrast", AccentColors.contrast(p.argb, text) >= 3.0)
        }
    }

    @Test fun lightAccentsGetDarkTextAndDeepOnesWhite() {
        assertTrue(AccentColors.darkTextOn(0xFFE5E7EB))    // pearl
        assertTrue(AccentColors.darkTextOn(0xFFA3E635))    // lime
        assertFalse(AccentColors.darkTextOn(0xFF1D4ED8))   // a deep blue
    }

    @Test fun anyCustomColourIsClearOnTheDarkScreen() {
        var hue = 0f
        while (hue < 360f) {
            for (vivid in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                val c = AccentColors.custom(hue, vivid)
                assertTrue("hue $hue vivid $vivid", AccentColors.contrast(c, AccentColors.SCREEN) >= 3.0)
                assertEquals("opaque", 0xFFL, (c shr 24) and 0xFF)
            }
            hue += 5f
        }
    }

    @Test fun slidersComeBackToWhereTheyWere() {
        for (hue in listOf(0f, 37f, 120f, 200f, 289f, 345f)) {
            for (vivid in listOf(0f, 0.4f, 1f)) {
                val (h, v) = AccentColors.sliders(AccentColors.custom(hue, vivid))
                val dh = minOf(abs(h - hue), 360f - abs(h - hue))
                assertTrue("hue $hue -> $h", dh < 2.5f)
                assertTrue("vivid $vivid -> $v", abs(v - vivid) < 0.02f)
            }
        }
    }

    @Test fun hueWrapsAndVividIsClamped() {
        assertEquals(AccentColors.custom(10f, 1f), AccentColors.custom(370f, 1f))
        assertEquals(AccentColors.custom(350f, 1f), AccentColors.custom(-10f, 1f))
        assertEquals(AccentColors.custom(90f, 1f), AccentColors.custom(90f, 5f))
        assertEquals(AccentColors.custom(90f, 0f), AccentColors.custom(90f, -1f))
    }

    @Test fun knownColours() {
        assertEquals(0xFFFF0000, AccentColors.hsvToArgb(0f, 1f, 1f))
        assertEquals(0xFF00FF00, AccentColors.hsvToArgb(120f, 1f, 1f))
        assertEquals(0xFF0000FF, AccentColors.hsvToArgb(240f, 1f, 1f))
        assertEquals(21.0, AccentColors.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, AccentColors.contrast(0xFF336699, 0xFF336699), 0.0001)
    }

    @Test fun aStoredColourIsAlwaysOpaque() {
        assertEquals(0xFF4F8CFF, AccentColors.opaque(0x004F8CFF))
        assertEquals(0xFF4F8CFF, AccentColors.opaque(0x804F8CFF))
        assertEquals(0xFF123456, AccentColors.opaque(-0xEDCBAAL))   // a negative Int colour from an old save
    }

    @Test fun themesUseKnownStylesAndReadableAccents() {
        assertEquals(AccentColors.THEMES.size, AccentColors.THEMES.map { it.key }.toSet().size)
        for (t in AccentColors.THEMES) {
            assertTrue(t.key, AccentColors.contrast(t.accent, AccentColors.SCREEN) >= 3.0)
        }
    }
}
