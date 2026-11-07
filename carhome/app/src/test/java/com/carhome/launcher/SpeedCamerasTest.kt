package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedCamerasTest {

    private val lat0 = 25.38
    private val lon0 = 49.59
    private val mLat = 111_195.0
    private fun north(m: Double) = lat0 + m / mLat
    private fun cam(id: Long, northM: Double, eastM: Double = 0.0, max: Int? = null) =
        SpeedCamera(id, north(northM), lon0 + eastM / (mLat * Math.cos(Math.toRadians(lat0))), max)

    private val drivingNorth = 0f
    private val fast = 25f   // 90 km/h

    @Test fun cameraStraightAheadIsFound() {
        val a = SpeedCameras.ahead(lat0, lon0, drivingNorth, fast, listOf(cam(1, 300.0, max = 120)))
        assertNotNull(a)
        assertEquals(300.0, a!!.distanceM, 3.0)
        assertEquals(120, a.camera.maxKmh)
    }

    @Test fun cameraBehindIsIgnored() =
        assertNull(SpeedCameras.ahead(lat0, lon0, drivingNorth, fast, listOf(cam(1, -300.0))))

    @Test fun cameraOffToTheSideIsIgnored() =
        assertNull(SpeedCameras.ahead(lat0, lon0, drivingNorth, fast, listOf(cam(1, 100.0, eastM = 300.0))))

    @Test fun farCameraIsIgnored() =
        assertNull(SpeedCameras.ahead(lat0, lon0, drivingNorth, fast, listOf(cam(1, 900.0))))

    @Test fun nearestAheadWins() {
        val a = SpeedCameras.ahead(lat0, lon0, drivingNorth, fast, listOf(cam(1, 450.0), cam(2, 200.0), cam(3, -100.0)))
        assertEquals(2L, a!!.camera.id)
    }

    @Test fun notWhileCrawling() =
        assertNull(SpeedCameras.ahead(lat0, lon0, drivingNorth, 3f, listOf(cam(1, 200.0))))

    @Test fun withoutAHeadingOnlyVeryCloseCounts() {
        assertNull(SpeedCameras.ahead(lat0, lon0, null, fast, listOf(cam(1, 300.0))))
        assertNotNull(SpeedCameras.ahead(lat0, lon0, null, fast, listOf(cam(1, 120.0))))
    }

    @Test fun bearingAcrossNorthWorks() {
        assertEquals(10f, SpeedCameras.angleDiff(355f, 5f), 0.01f)
        assertEquals(180f, SpeedCameras.angleDiff(0f, 180f), 0.01f)
    }

    @Test fun alertSoundsOncePerCamera() {
        val w = CameraAlerter()
        val c = cam(7, 400.0)
        assertTrue("first sighting beeps", w.update(CameraAhead(c, 400.0), now = 0L))
        assertFalse(w.update(CameraAhead(c, 300.0), now = 1_000L))
        assertFalse(w.update(null, now = 2_000L))
        assertFalse("same camera again soon: no second beep", w.update(CameraAhead(c, 250.0), now = 60_000L))
        assertTrue("after 10 minutes it may beep again", w.update(CameraAhead(c, 400.0), now = 11 * 60_000L))
    }

    @Test fun anotherCameraBeepsToo() {
        val w = CameraAlerter()
        assertTrue(w.update(CameraAhead(cam(1, 400.0), 400.0), 0L))
        assertTrue(w.update(CameraAhead(cam(2, 400.0), 400.0), 5_000L))
    }

    @Test fun aCameraForOncomingTrafficIsIgnored() {
        // Across the median, 30 m to the side, tagged for traffic going south
        val oncoming = cam(9, 400.0, 30.0).copy(directionDeg = 180f)
        assertNull(SpeedCameras.ahead(lat0, lon0, drivingNorth, 25f, listOf(oncoming)))
        val ours = cam(10, 400.0, -10.0).copy(directionDeg = 0f)
        assertEquals(10L, SpeedCameras.ahead(lat0, lon0, drivingNorth, 25f, listOf(oncoming, ours))!!.camera.id)
    }

    @Test fun aCameraWithoutDirectionStillWarns() =
        assertEquals(11L, SpeedCameras.ahead(lat0, lon0, drivingNorth, 25f, listOf(cam(11, 300.0)))!!.camera.id)

    @Test fun readsTheDirectionTag() {
        assertEquals(90f, SpeedCameras.parseDirection("90")!!, 0.01f)
        assertEquals(45f, SpeedCameras.parseDirection("NE")!!, 0.01f)
        assertEquals(337.5f, SpeedCameras.parseDirection("nnw")!!, 0.01f)
        assertNull("both ways", SpeedCameras.parseDirection("90;270"))
        assertNull("relative to the road, unknown here", SpeedCameras.parseDirection("forward"))
        assertNull(SpeedCameras.parseDirection(null))
        assertNull(SpeedCameras.parseDirection("400"))
    }
}
