package com.carhome.launcher

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dashboard settings and the map widgets (1.4). */
class DashboardTest {

    private val defaults = LauncherSettings()
    private fun roundTrip(s: LauncherSettings) = settingsFromJson(JSONObject(settingsToJson(s).toString()))

    @Test fun newThingsStartOffOrQuiet() {
        assertEquals(HomeStyle.CLASSIC, defaults.homeStyle)
        assertFalse("the strip over Google Maps is off until turned on", defaults.mapsStrip)
        assertEquals(FavSide.LEFT, defaults.dashMapSide)
        assertEquals(60, defaults.dashMapWidth)
        assertEquals(listOf(CardType.SPEED, CardType.MUSIC), defaults.dashCards)
    }

    @Test fun dashboardSettingsRoundTrip() {
        val s = defaults.copy(
            homeStyle = HomeStyle.DASHBOARD, dashMapSide = FavSide.RIGHT, dashMapWidth = 70,
            mapLook = MapLook.NIGHT, mapAutoZoom = false, mapPlaces = false, mapCameras = false,
            dashCards = listOf(CardType.MUSIC, CardType.PRAYER, CardType.SPEED), dashPlacesRow = false,
            navCard = false, mapsStrip = true, stripSide = FavSide.RIGHT, stripSpeed = false, stripMusic = false
        )
        assertEquals(s, roundTrip(s))
    }

    @Test fun badDashboardValuesFallBack() {
        val o = settingsToJson(defaults)
            .put("dashMapWidth", 65)
            .put("mapLook", "SEPIA")
            .put("stripSide", "TOP")
            .put("dashCards", JSONArray(listOf("SPEED", "UFO", "SPEED", "MUSIC", "TRIP", "CAR")))
        val s = settingsFromJson(o)
        assertEquals(60, s.dashMapWidth)
        assertEquals(MapLook.DARK, s.mapLook)
        assertEquals(FavSide.LEFT, s.stripSide)
        assertEquals(listOf(CardType.SPEED, CardType.MUSIC, CardType.TRIP), s.dashCards)
    }

    @Test fun mapWidgetsHaveSizesThatFitEveryGrid() {
        for (t in listOf(WidgetType.MAP, WidgetType.NEXT_TURN)) {
            assertTrue(CustomGrid.defaultSize(t) in CustomGrid.sizes(t))
            for (g in GridSize.entries) {
                assertTrue("$t fits $g", CustomGrid.sizes(t).any { it.first <= g.cols && it.second <= g.rows })
            }
        }
    }

    @Test fun aMapWidgetCanBeAddedSavedAndLoaded() {
        val l = CustomGrid.add(CustomLayout(GridSize.NORMAL, 1, emptyList()), WidgetType.MAP, 0, "m")
        assertNotNull(l)
        val l2 = CustomGrid.add(l!!, WidgetType.NEXT_TURN, 0, "n")!!
        val back = layoutFromJson(JSONObject(layoutToJson(l2).toString()))
        assertEquals(l2, back)
        assertEquals(4 to 3, back.widgets.first { it.id == "m" }.let { it.w to it.h })
    }
}
