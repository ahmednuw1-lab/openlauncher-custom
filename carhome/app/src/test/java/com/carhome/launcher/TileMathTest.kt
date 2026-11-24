package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Map tile maths for the dashboard and the map widget (1.4). */
class TileMathTest {

    @Test fun theMiddleOfTheWorldIsTheMiddleTileCorner() {
        assertEquals(1.0, TileMath.x(0.0, 1), 1e-9)
        assertEquals(1.0, TileMath.y(0.0, 1), 1e-9)
        assertEquals(0.0, TileMath.x(-180.0, 5), 1e-9)
    }

    @Test fun hofufIsInTheRightTile() {
        // Al-Hofuf (25.38 N, 49.59 E) at zoom 10 is OpenStreetMap tile 653 / 437
        assertEquals(653, TileMath.x(49.59, 10).toInt())
        assertEquals(437, TileMath.y(25.38, 10).toInt())
    }

    @Test fun positionsGoBackAndForth() {
        for (z in listOf(3, 10, 17)) {
            val lat = 24.7136; val lon = 46.6753
            assertEquals(lat, TileMath.lat(TileMath.y(lat, z), z), 1e-9)
            assertEquals(lon, TileMath.lon(TileMath.x(lon, z), z), 1e-9)
        }
    }

    @Test fun polesAreClampedInsteadOfBreaking() {
        val y = TileMath.y(90.0, 4)
        assertTrue(y.isFinite() && y >= -1e-6)
    }

    @Test fun rescaleDoublesPerZoomLevel() {
        assertEquals(20.0, TileMath.rescale(10.0, 4, 5), 1e-9)
        assertEquals(2.5, TileMath.rescale(10.0, 6, 4), 1e-9)
    }

    @Test fun columnsWrapRoundTheWorld() {
        assertEquals(0, TileMath.wrapX(4, 2))
        assertEquals(3, TileMath.wrapX(-1, 2))
        assertEquals(2, TileMath.wrapX(2, 2))
    }

    @Test fun visibleTilesCoverTheViewAndNoRowsOffTheWorld() {
        // A 3 × 2 tile view centred inside tile (10, 20)
        val tiles = TileMath.visible(10.5, 20.5, 3.0, 2.0, 6)
        assertTrue((9 to 19) in tiles && (12 to 21) in tiles)
        assertTrue(tiles.all { it.first in 9..12 && it.second in 19..21 })
        // At the top of the world no row above 0 is asked for
        assertTrue(TileMath.visible(1.0, 0.2, 2.0, 2.0, 3).all { it.second >= 0 })
    }

    @Test fun zoomFollowsTheSpeed() {
        assertEquals(17, TileMath.zoomForSpeed(null))
        assertEquals(17, TileMath.zoomForSpeed(5f))
        assertEquals(16, TileMath.zoomForSpeed(40f))
        assertEquals(15, TileMath.zoomForSpeed(80f))
        assertEquals(14, TileMath.zoomForSpeed(130f))
    }

    @Test fun autoZoomDoesNotFlickAtTheEdgeOfABand() {
        // 16 is for 20–60 km/h: just over 60 keeps 16, clearly over moves to 15
        assertEquals(16, TileMath.autoZoom(62f, 16))
        assertEquals(15, TileMath.autoZoom(70f, 16))
        // And back down: just under 60 keeps 15, clearly under moves to 16
        assertEquals(15, TileMath.autoZoom(57f, 15))
        assertEquals(16, TileMath.autoZoom(45f, 15))
        // Stopping always goes in close
        assertEquals(17, TileMath.autoZoom(null, 14))
    }

    @Test fun zoomStaysInRange() {
        assertEquals(TileMath.MIN_ZOOM, TileMath.clampZoom(0))
        assertEquals(TileMath.MAX_ZOOM, TileMath.clampZoom(25))
    }

    @Test fun darkMapTurnsLightColoursDarkAndKeepsHues() {
        val m = MapColours.darkMatrix()
        val (wr, wg, wb) = MapColours.apply(m, 255, 255, 255)
        assertTrue("white land becomes dark", wr + wg + wb < 3 * 60)
        val (kr, kg, kb) = MapColours.apply(m, 0, 0, 0)
        assertTrue("black text becomes light", kr + kg + kb > 3 * 150)
        // A main road (light orange) stays warm: more red than blue
        val (rr, _, rb) = MapColours.apply(m, 252, 214, 164)
        assertTrue(rr > rb)
        // Alpha is untouched
        assertEquals(1f, m[18], 1e-6f)
        assertEquals(0f, m[19], 1e-6f)
    }

    @Test fun matrixTimesIdentityIsItself() {
        val id = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        val m = MapColours.darkMatrix()
        val p = MapColours.times(m, id)
        assertTrue(m.indices.all { abs(m[it] - p[it]) < 1e-4f })
    }
}
