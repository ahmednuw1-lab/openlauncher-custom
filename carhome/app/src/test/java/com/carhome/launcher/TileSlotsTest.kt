package com.carhome.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TileSlotsTest {

    private val twelve = (1..12).map { "app$it" }

    @Test fun showingFewerTilesKeepsTheHiddenOnes() {
        val saved = TileSlots.merge(visible = twelve.take(4), saved = twelve)
        assertEquals(twelve, saved)
    }

    @Test fun changingAVisibleTileKeepsTheHiddenOnes() {
        val visible = twelve.take(4).toMutableList().apply { this[1] = "maps" }
        val saved = TileSlots.merge(visible, twelve)
        assertEquals("maps", saved[1])
        assertEquals(twelve.drop(4), saved.drop(4))
    }

    @Test fun growingAgainBringsThemBack() {
        val saved = TileSlots.merge(twelve.take(4), twelve)
        assertEquals(twelve.take(8), TileSlots.visible(saved, 8))
    }

    @Test fun visibleIsPaddedWithEmptyTiles() {
        assertEquals(listOf("a", "b", "", ""), TileSlots.visible(listOf("a", "b"), 4))
    }

    @Test fun forgettingAnAppClearsItEverywhere() {
        val saved = TileSlots.forget(listOf("a", "gone", "b", "", "gone"), "gone")
        assertEquals(listOf("a", "", "b", "", ""), saved)
    }
}
