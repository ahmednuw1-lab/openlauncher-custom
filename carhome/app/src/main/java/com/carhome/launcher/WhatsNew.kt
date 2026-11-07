package com.carhome.launcher

/**
 * Reads the "What's new" notes: the app's assets/whats-new.md, which the build
 * also uses as the GitHub release notes. Format:
 *
 *     # CarHome 1.2.10
 *     ## en
 *     - change
 *     ## ar
 *     - تغيير
 */
object WhatsNew {

    const val ASSET = "whats-new.md"

    /** The bullet points for [lang] ("en" or "ar"); English for other languages. */
    fun parse(text: String?, lang: String): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val sections = mutableMapOf<String, MutableList<String>>()
        var current = ""   // bullets before any "## xx" heading
        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.startsWith("## ") -> current = line.removePrefix("## ").trim().lowercase()
                line.startsWith("- ") || line.startsWith("* ") ->
                    sections.getOrPut(current) { mutableListOf() } += line.drop(2).trim()
            }
        }
        val want = if (lang == "ar") "ar" else "en"
        return sections[want] ?: sections["en"] ?: sections[""] ?: sections.values.firstOrNull() ?: emptyList()
    }

    /** "# CarHome 1.2.10" → "1.2.10" */
    fun version(text: String?): String? =
        text?.lines()?.firstOrNull { it.trim().startsWith("# ") }?.let { UpdateCheck.versionNameFrom(it) }
}
