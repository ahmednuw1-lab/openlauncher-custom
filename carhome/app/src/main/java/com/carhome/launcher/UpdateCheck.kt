package com.carhome.launcher

import java.net.URI

/** Update-check rules that don't need Android, so they can be unit-tested. */
object UpdateCheck {

    /** Where CarHome releases are published (public repository). */
    const val REPO = "ahmednuw1-lab/openlauncher-custom"
    const val LATEST_API = "https://api.github.com/repos/$REPO/releases/latest"

    private const val CHECK_EVERY_MS = 6 * 3_600_000L

    /** "CarHome 1.2.9 (build 21)" → "1.2.9" */
    fun versionNameFrom(title: String?): String? =
        title?.let { Regex("""(\d+(?:\.\d+)+)""").find(it)?.value }

    /** The "versionCode: 12" line the build writes into the release notes. */
    fun versionCodeFrom(notes: String?): Int? =
        notes?.let { Regex("""versionCode:\s*(\d+)""").find(it)?.groupValues?.get(1)?.toIntOrNull() }

    /** Compares dotted versions number by number ("1.2.10" > "1.2.9"; "1.2" == "1.2.0"). */
    fun compareVersions(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = (x.getOrNull(i) ?: 0).compareTo(y.getOrNull(i) ?: 0)
            if (d != 0) return d
        }
        return 0
    }

    /** Is the published release newer than the installed one? The version code decides when known. */
    fun isNewer(remoteCode: Int?, remoteName: String?, localCode: Int, localName: String): Boolean = when {
        remoteCode != null -> remoteCode > localCode
        remoteName != null -> compareVersions(remoteName, localName) > 0
        else -> false
    }

    /** Automatic checks at most every 6 hours (a clock set back doesn't block them). */
    fun isDue(lastCheckAt: Long, now: Long): Boolean =
        lastCheckAt <= 0L || now < lastCheckAt || now - lastCheckAt >= CHECK_EVERY_MS

    /** Only an https download from this repository's releases on github.com is accepted. */
    fun trustedDownload(url: String?): Boolean {
        val u = runCatching { URI(url ?: return false) }.getOrNull() ?: return false
        return u.scheme == "https" && u.host == "github.com" &&
            (u.path ?: "").startsWith("/$REPO/releases/download/")
    }
}
