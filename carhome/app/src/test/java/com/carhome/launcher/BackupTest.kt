package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A backup must bring back exactly what was saved, value types included. */
class BackupTest {

    private val saved: Map<String, Any> = mapOf(
        "settings" to """{"speedLimit":120,"language":"ARABIC"}""",
        "tiles" to "com.google.android.apps.maps|place:home1||",
        "bubble_x" to 42,
        "trip_started" to 1_700_000_000_000L,
        "fuel" to 2.33f,
        "parked_only" to setOf("com.youtube", "com.netflix"),
        "obd_on" to true
    )

    @Test fun everythingComesBackWithItsType() {
        val back = Backup.decode(Backup.encode(saved, "1.2.10"))!!
        assertEquals(saved, back)
        assertTrue(back["bubble_x"] is Int)
        assertTrue(back["trip_started"] is Long)
        assertTrue(back["fuel"] is Float)
        assertTrue(back["parked_only"] is Set<*>)
    }

    @Test fun thisBoxOnlyValuesStayOut() {
        val withLocal = saved + mapOf("saved_brightness" to 120, "update_checked_at" to 5L, "last_version_code" to 12)
        val back = Backup.decode(Backup.encode(withLocal, "1.2.10"))!!
        assertFalse(back.containsKey("saved_brightness"))
        assertFalse(back.containsKey("update_checked_at"))
        assertFalse(back.containsKey("last_version_code"))
        assertEquals(saved, back)
    }

    @Test fun otherFilesAreRefused() {
        assertNull(Backup.decode("""{"hello":"world"}"""))
        assertNull(Backup.decode("not json at all"))
        assertNull(Backup.decode(""))
    }

    @Test fun aBackupFromANewerFormatIsRefused() {
        val text = Backup.encode(saved, "9.9").replace("\"version\":1", "\"version\":99")
        assertNull(Backup.decode(text))
    }

    @Test fun unknownValueTypesAreSkipped() {
        val text = Backup.encode(saved, "1.2.10").replace("\"t\":\"b\"", "\"t\":\"zz\"")
        val back = Backup.decode(text)!!
        assertFalse(back.containsKey("obd_on"))
        assertEquals(saved.size - 1, back.size)
    }

    @Test fun theFileSaysWhatItIs() {
        val text = Backup.encode(saved, "1.2.10")
        assertTrue(text.contains("\"format\":\"carhome-backup\""))
        assertTrue(text.contains("\"app\":\"1.2.10\""))
    }

    @Test fun aKnownValueOfTheWrongTypeIsDropped() {
        // A hand-edited or damaged file must never write e.g. a number where text is read
        val bad = Backup.encode(saved + mapOf("tiles" to 7, "parked_only" to "com.youtube", "odometer_km" to 12.5f), "1.2.10")
        val back = Backup.decode(bad)!!
        assertFalse(back.containsKey("tiles"))
        assertFalse(back.containsKey("parked_only"))
        assertFalse(back.containsKey("odometer_km"))
        assertEquals(saved["settings"], back["settings"])
    }

    @Test fun aBackupWithoutSettingsIsRefused() {
        // An empty backup would otherwise wipe everything on restore
        assertNull(Backup.decode("""{"format":"carhome-backup","version":1,"data":{}}"""))
        assertNull(Backup.decode(Backup.encode(saved - "settings", "1.2.10")))
    }

    @Test fun settingsThatArentReadableAreRefused() {
        assertNull(Backup.decode(Backup.encode(saved + ("settings" to 5), "1.2.10")))
        assertNull(Backup.decode(Backup.encode(saved + ("settings" to "not json"), "1.2.10")))
    }

    @Test fun firstRunMarksStayWithThisBox() {
        val back = Backup.decode(Backup.encode(saved + mapOf("asked_location" to true, "boot_count" to 9), "1.2.10"))!!
        assertFalse(back.containsKey("asked_location"))
        assertFalse(back.containsKey("boot_count"))
    }
}
