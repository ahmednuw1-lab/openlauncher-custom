package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {

    @Test fun readsTheVersionFromTheReleaseTitle() {
        assertEquals("1.2.9", UpdateCheck.versionNameFrom("CarHome 1.2.9 (build 21)"))
        assertEquals("1.3", UpdateCheck.versionNameFrom("CarHome 1.3"))
        assertNull(UpdateCheck.versionNameFrom("CarHome build 17"))
        assertNull(UpdateCheck.versionNameFrom(null))
    }

    @Test fun readsTheVersionCodeFromTheNotes() {
        assertEquals(12, UpdateCheck.versionCodeFrom("CarHome 1.2.9.\nversionCode: 12\n"))
        assertNull(UpdateCheck.versionCodeFrom("Automatic build of abc123"))
        assertNull(UpdateCheck.versionCodeFrom(null))
    }

    @Test fun comparesVersionsNumberByNumber() {
        assertTrue(UpdateCheck.compareVersions("1.2.10", "1.2.9") > 0)
        assertTrue(UpdateCheck.compareVersions("1.3", "1.2.9") > 0)
        assertEquals(0, UpdateCheck.compareVersions("1.2.8", "1.2.8"))
        assertEquals(0, UpdateCheck.compareVersions("1.2", "1.2.0"))
        assertTrue(UpdateCheck.compareVersions("1.2.8", "1.2.9") < 0)
    }

    @Test fun theVersionCodeDecidesWhenThereIsOne() {
        assertTrue(UpdateCheck.isNewer(remoteCode = 12, remoteName = "1.2.9", localCode = 11, localName = "1.2.8"))
        assertFalse(UpdateCheck.isNewer(remoteCode = 11, remoteName = "1.2.8", localCode = 11, localName = "1.2.8"))
        assertFalse("an older build is never offered", UpdateCheck.isNewer(10, "1.2.7", 11, "1.2.8"))
    }

    @Test fun theNameDecidesWithoutACode() {
        assertTrue(UpdateCheck.isNewer(remoteCode = null, remoteName = "1.2.9", localCode = 11, localName = "1.2.8"))
        assertFalse(UpdateCheck.isNewer(remoteCode = null, remoteName = "1.2.8", localCode = 11, localName = "1.2.8"))
        assertFalse("nothing to compare", UpdateCheck.isNewer(remoteCode = null, remoteName = null, localCode = 11, localName = "1.2.8"))
    }

    @Test fun checksAtMostEverySixHours() {
        val h = 3_600_000L
        assertTrue(UpdateCheck.isDue(lastCheckAt = 0L, now = 10 * h))
        assertFalse(UpdateCheck.isDue(lastCheckAt = 10 * h, now = 15 * h))
        assertTrue(UpdateCheck.isDue(lastCheckAt = 10 * h, now = 16 * h))
        assertTrue("a clock set back doesn't block checks", UpdateCheck.isDue(lastCheckAt = 20 * h, now = 10 * h))
    }

    @Test fun onlyDownloadsFromGitHub() {
        assertTrue(UpdateCheck.trustedDownload("https://github.com/ahmednuw1-lab/openlauncher-custom/releases/download/carhome-21/carhome.apk"))
        assertFalse(UpdateCheck.trustedDownload("http://github.com/x/carhome.apk"))
        assertFalse(UpdateCheck.trustedDownload("https://evil.example/carhome.apk"))
        assertFalse(UpdateCheck.trustedDownload("https://github.com.evil.example/carhome.apk"))
        assertFalse(UpdateCheck.trustedDownload(null))
    }
}
