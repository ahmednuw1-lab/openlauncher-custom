package com.carhome.launcher

/**
 * The saved tile list keeps every slot the user ever filled, so choosing fewer
 * tiles only hides the extra ones; choosing more brings them back.
 */
object TileSlots {

    /** The first [count] tiles, padded with empty ones. */
    fun visible(saved: List<String>, count: Int): List<String> = List(count) { i -> saved.getOrNull(i).orEmpty() }

    /** The visible tiles written over the saved list, keeping the hidden tail. */
    fun merge(visible: List<String>, saved: List<String>): List<String> = visible + saved.drop(visible.size)

    /** Clears [value] (an uninstalled app or a deleted place) from every slot, hidden ones too. */
    fun forget(saved: List<String>, value: String): List<String> = saved.map { if (it == value) "" else it }
}
