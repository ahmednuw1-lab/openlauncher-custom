package com.carhome.launcher

/** The time in words, to the nearest five minutes ("quarter past nine", "الساعة التاسعة والربع"). */
object TimeWords {

    private val EN_HOURS = listOf("twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven")
    private val AR_HOURS = listOf(
        "الثانية عشرة", "الواحدة", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة",
        "السابعة", "الثامنة", "التاسعة", "العاشرة", "الحادية عشرة"
    )

    /** [hour24] 0–23 and [minute] 0–59. */
    fun phrase(hour24: Int, minute: Int, arabic: Boolean): String {
        val total = ((hour24 * 60 + minute + 2) / 5 * 5) % (24 * 60)
        val h = total / 60
        val m = total % 60
        val hourNow = h % 12
        val hourNext = (h + 1) % 12
        return if (arabic) arabic(hourNow, hourNext, m) else english(hourNow, hourNext, m)
    }

    private fun english(now: Int, next: Int, m: Int): String {
        val h = EN_HOURS[now]
        val n = EN_HOURS[next]
        return when (m) {
            0 -> "$h o'clock"
            5 -> "five past $h"
            10 -> "ten past $h"
            15 -> "quarter past $h"
            20 -> "twenty past $h"
            25 -> "twenty-five past $h"
            30 -> "half past $h"
            35 -> "twenty-five to $n"
            40 -> "twenty to $n"
            45 -> "quarter to $n"
            50 -> "ten to $n"
            else -> "five to $n"
        }
    }

    private fun arabic(now: Int, next: Int, m: Int): String {
        val h = "الساعة " + AR_HOURS[now]
        val n = "الساعة " + AR_HOURS[next]
        return when (m) {
            0 -> "$h تمامًا"
            5 -> "$h وخمس دقائق"
            10 -> "$h وعشر دقائق"
            15 -> "$h والربع"
            20 -> "$h والثلث"
            25 -> "$h والنصف إلا خمس دقائق"
            30 -> "$h والنصف"
            35 -> "$h والنصف وخمس دقائق"
            40 -> "$n إلا ثلثًا"
            45 -> "$n إلا ربعًا"
            50 -> "$n إلا عشر دقائق"
            else -> "$n إلا خمس دقائق"
        }
    }
}
