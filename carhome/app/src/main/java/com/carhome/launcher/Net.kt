package com.carhome.launcher

import java.io.IOException
import java.io.InputStream

/** Size limits for everything CarHome reads from the internet or an adapter. */
object Net {
    /** Weather and air-quality answers are a few KB */
    const val MAX_WEATHER = 512 * 1024
    /** Map answers (roads, cameras) for a small area */
    const val MAX_MAP = 2 * 1024 * 1024
    /** GitHub's "latest release" answer */
    const val MAX_RELEASE_INFO = 512 * 1024
    /** A CarHome APK is about 11 MB; nothing larger is ever downloaded */
    const val MAX_APK = 80L * 1024 * 1024
    /** One OBD reply is a few dozen characters */
    const val MAX_OBD_REPLY = 4096

    /**
     * Reads the stream as UTF-8 text, at most [max] bytes. A longer answer is an
     * error (not cut short), so a broken or hostile server can't fill the memory.
     */
    fun readCapped(input: InputStream, max: Int): String {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (out.size() + n > max) throw IOException("answer larger than $max bytes")
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }

    /** May a download of [total] bytes (as announced; -1 unknown) be started? */
    fun apkSizeAllowed(total: Long): Boolean = total <= MAX_APK

    /** Has a running download gone past what was announced, or past the hard limit? */
    fun downloadTooBig(done: Long, total: Long): Boolean =
        done > MAX_APK || (total > 0 && done > total)

    /** "sha256:ab12…" (GitHub's asset digest) → lower-case hex, or null when absent or malformed. */
    fun sha256From(digest: String?): String? {
        val d = digest?.trim()?.lowercase() ?: return null
        if (!d.startsWith("sha256:")) return null
        val hex = d.removePrefix("sha256:")
        return hex.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
    }

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
