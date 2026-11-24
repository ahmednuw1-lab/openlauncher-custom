package com.carhome.launcher

import android.graphics.Bitmap

/**
 * The next turn while Google Maps navigates, as its own notification shows it.
 * [title] is usually the distance ("300 m"), [text] the instruction or road, [sub]
 * the arrival time and what's left. [icon] is the turn arrow Google Maps draws, when it gives one.
 */
data class NavInfo(
    val title: String,
    val text: String,
    val sub: String,
    val icon: Bitmap?,
    val app: String,
    val key: String
)

/** Reading navigation notifications. Pure, so it can be tested without a phone. */
object NavParse {
    const val GOOGLE_MAPS = "com.google.android.apps.maps"
    val NAV_APPS = setOf(GOOGLE_MAPS)

    private val distance = Regex(
        // No \b: Java and Android disagree on word edges next to Arabic letters
        """^\s*[\d٠-٩.,٫]+\s*(m|km|ft|mi|yd|م|متر|أمتار|كم|كيلومتر|قدم|ميل)(?!\p{L})""",
        RegexOption.IGNORE_CASE
    )

    /** Does the title read like a distance ("300 m", "1.2 km", "٣٠٠ م")? */
    fun looksLikeDistance(title: String): Boolean = distance.containsMatchIn(title)

    /**
     * Is this notification a navigation app's turn-by-turn one? It must come from a
     * navigation app and stay on screen (ongoing); and be marked as navigation, or sit
     * in a channel named for it, or show a distance as its title.
     */
    fun isNav(pkg: String?, ongoing: Boolean, category: String?, channelId: String?, title: String): Boolean {
        if (pkg !in NAV_APPS || !ongoing) return false
        return category == "navigation" ||
            channelId?.contains("nav", ignoreCase = true) == true ||
            looksLikeDistance(title)
    }

    /** One line of notification text, tidied and kept short. */
    fun clean(s: CharSequence?, max: Int = 140): String =
        s?.toString()?.replace(Regex("""\s+"""), " ")?.trim()?.take(max).orEmpty()

    /** The next turn from a notification's texts; null when it says nothing. */
    fun info(
        title: CharSequence?, text: CharSequence?, sub: CharSequence?, bigText: CharSequence?,
        icon: Bitmap?, pkg: String, key: String
    ): NavInfo? {
        val t = clean(title, 60)
        // The long form, when given, holds the whole instruction
        val x = clean(bigText).ifBlank { clean(text) }
        val s = clean(sub, 80)
        if (t.isBlank() && x.isBlank()) return null
        return NavInfo(t, x, s, icon, pkg, key)
    }
}
