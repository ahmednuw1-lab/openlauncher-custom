package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/** Nothing read from the internet may be larger than CarHome expects. */
class NetTest {

    @Test fun readsAnAnswerWithinTheLimit() {
        assertEquals("hello", Net.readCapped(ByteArrayInputStream("hello".toByteArray()), 5))
        assertEquals("", Net.readCapped(ByteArrayInputStream(ByteArray(0)), 10))
        assertEquals("طقس", Net.readCapped(ByteArrayInputStream("طقس".toByteArray()), 100))
    }

    @Test fun anAnswerOverTheLimitIsRefusedNotCutShort() {
        assertThrows(IOException::class.java) { Net.readCapped(ByteArrayInputStream("hello!".toByteArray()), 5) }
    }

    @Test fun anEndlessAnswerStopsAtTheLimit() {
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int { served++; return 'x'.code }
            override fun read(b: ByteArray, off: Int, len: Int): Int { served += len; b.fill('x'.code.toByte(), off, off + len); return len }
        }
        assertThrows(IOException::class.java) { Net.readCapped(endless, 100_000) }
        assertTrue("stopped soon after the limit, read $served", served < 200_000)
    }

    @Test fun downloadsAreLimited() {
        assertTrue(Net.apkSizeAllowed(11_000_000))
        assertTrue("unknown size may start; it is stopped at the limit", Net.apkSizeAllowed(-1))
        assertFalse(Net.apkSizeAllowed(Net.MAX_APK + 1))
        assertFalse(Net.downloadTooBig(11_000_000, 11_000_000))
        assertTrue("more than GitHub announced", Net.downloadTooBig(11_000_001, 11_000_000))
        assertFalse(Net.downloadTooBig(5_000_000, -1))
        assertTrue(Net.downloadTooBig(Net.MAX_APK + 1, -1))
    }

    @Test fun readsGitHubsDigest() {
        val hex = "e6b8e3be963f8c4c7860ff62c4c4d07e5c76d07a28a54d7a62ad42faf4bdd1aa"
        assertEquals(hex, Net.sha256From("sha256:$hex"))
        assertEquals(hex, Net.sha256From("SHA256:${hex.uppercase()}"))
        assertNull(Net.sha256From(null))
        assertNull(Net.sha256From(""))
        assertNull(Net.sha256From("md5:abcd"))
        assertNull(Net.sha256From("sha256:1234"))
        assertNull(Net.sha256From("sha256:" + "z".repeat(64)))
    }

    @Test fun hexIsLowerCaseTwoDigitsPerByte() =
        assertEquals("000fa0ff", Net.hex(byteArrayOf(0, 15, 0xA0.toByte(), 0xFF.toByte())))
}
