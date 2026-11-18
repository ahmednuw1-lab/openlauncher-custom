package com.carhome.launcher

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The custom home grid: widgets never overlap, never leave the grid, and survive saving. */
class CustomGridTest {

    private fun w(id: String, type: WidgetType, c: Int, r: Int, ww: Int, hh: Int, page: Int = 0) =
        Widget(id, type, page, c, r, ww, hh)

    private fun valid(l: CustomLayout) {
        for (a in l.widgets) {
            assertTrue("${a.id} inside the grid", CustomGrid.inBounds(a, l.grid))
            assertTrue("${a.id} on an existing page", a.page < l.pages)
            assertTrue("${a.id} has an allowed size", (a.w to a.h) in CustomGrid.sizes(a.type))
            for (b in l.widgets) if (a.id != b.id) assertFalse("${a.id} overlaps ${b.id}", CustomGrid.overlaps(a, b))
        }
        assertEquals("ids are unique", l.widgets.size, l.widgets.map { it.id }.toSet().size)
    }

    @Test fun defaultLayoutIsValidOnEveryGrid() {
        for (g in GridSize.entries) {
            val l = CustomGrid.defaultLayout(g)
            valid(l)
            assertEquals(g, l.grid)
            assertTrue("$g keeps the clock", l.widgets.any { it.type == WidgetType.CLOCK })
        }
    }

    @Test fun overlapIsOnlyOnTheSamePage() {
        val a = w("a", WidgetType.CLOCK, 0, 0, 2, 2)
        assertTrue(CustomGrid.overlaps(a, w("b", WidgetType.CLOCK, 1, 1, 2, 2)))
        assertFalse(CustomGrid.overlaps(a, w("b", WidgetType.CLOCK, 2, 0, 2, 2)))
        assertFalse(CustomGrid.overlaps(a, w("b", WidgetType.CLOCK, 0, 0, 2, 2, page = 1)))
    }

    @Test fun addFindsTheFirstFreeSpot() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(w("a", WidgetType.CLOCK, 0, 0, 3, 2)))
        val added = CustomGrid.add(l, WidgetType.SPEED, 0, "s")!!
        valid(added)
        val s = CustomGrid.find(added, "s")!!
        assertEquals(3, s.col)
        assertEquals(0, s.row)
        assertEquals(2 to 2, s.w to s.h)
    }

    @Test fun addUsesTheChosenSizeWhenItFits() {
        val added = CustomGrid.add(CustomLayout(), WidgetType.MUSIC, 0, "m", size = 2 to 1)!!
        assertEquals(2 to 1, CustomGrid.find(added, "m")!!.let { it.w to it.h })
    }

    @Test fun addShrinksOrGoesToAnotherPageOrGivesUp() {
        // One free cell left: a clock still fits, as 1×1
        val nearlyFull = CustomLayout(GridSize.SMALL, 1, listOf(
            w("a", WidgetType.CLOCK, 0, 0, 4, 2), w("b", WidgetType.TRIP, 0, 2, 4, 1),
            w("c", WidgetType.CAR, 4, 0, 2, 2), w("d", WidgetType.SHORTCUT, 4, 2, 1, 1)
        ))
        val one = CustomGrid.add(nearlyFull, WidgetType.CLOCK, 0, "x")!!
        assertEquals(1 to 1, CustomGrid.find(one, "x")!!.let { it.w to it.h })
        // Now the page is full: nothing else fits…
        assertNull(CustomGrid.add(one, WidgetType.SHORTCUT, 0, "y"))
        // …but a second page has room
        val twoPages = CustomGrid.addPage(one)
        val onPage2 = CustomGrid.add(twoPages, WidgetType.MUSIC, 0, "m")!!
        assertEquals(1, CustomGrid.find(onPage2, "m")!!.page)
    }

    @Test fun moveRefusesOverlapsAndTheEdge() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(w("a", WidgetType.CLOCK, 0, 0, 2, 2), w("b", WidgetType.SPEED, 4, 0, 2, 2)))
        assertEquals(l, CustomGrid.move(l, "a", 3, 0))     // onto b
        assertEquals(l, CustomGrid.move(l, "a", 7, 0))     // past the edge
        val moved = CustomGrid.move(l, "a", 0, 2)
        assertEquals(2, CustomGrid.find(moved, "a")!!.row)
        valid(moved)
    }

    @Test fun resizeKeepsTheCornerOrSlidesBackInside() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(w("a", WidgetType.CLOCK, 6, 3, 2, 1)))
        val big = CustomGrid.resize(l, "a", 3, 2)
        valid(big)
        val a = CustomGrid.find(big, "a")!!
        assertEquals(3 to 2, a.w to a.h)
        assertEquals(5, a.col)   // slid back so it still fits
        assertEquals(2, a.row)
    }

    @Test fun resizeRefusesSizesTheWidgetCantTake() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(w("a", WidgetType.MUSIC, 0, 0, 3, 2)))
        assertEquals(l, CustomGrid.resize(l, "a", 1, 1))
    }

    @Test fun nearestSizeSnapsToAnAllowedOne() {
        assertEquals(2 to 1, CustomGrid.nearestSize(WidgetType.MUSIC, 1, 1))
        assertEquals(4 to 2, CustomGrid.nearestSize(WidgetType.CLOCK, 6, 5))
    }

    @Test fun updateCanNotMoveOrResize() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(w("a", WidgetType.CLOCK, 0, 0, 2, 2)))
        val u = CustomGrid.update(l, "a") { it.copy(col = 5, w = 4, style = WidgetStyle.CLEAR) }
        val a = CustomGrid.find(u, "a")!!
        assertEquals(0, a.col)
        assertEquals(2, a.w)
        assertEquals(WidgetStyle.CLEAR, a.style)
    }

    @Test fun removingAPageMovesTheLaterOnesUp() {
        val l = CustomLayout(GridSize.NORMAL, 3, listOf(
            w("p0", WidgetType.CLOCK, 0, 0, 2, 2, 0), w("p1", WidgetType.CLOCK, 0, 0, 2, 2, 1), w("p2", WidgetType.CLOCK, 0, 0, 2, 2, 2)
        ))
        val r = CustomGrid.removePage(l, 1)
        assertEquals(2, r.pages)
        assertNull(CustomGrid.find(r, "p1"))
        assertEquals(1, CustomGrid.find(r, "p2")!!.page)
        val r2 = CustomGrid.removePage(r, 1)
        assertEquals(1, r2.pages)
        assertEquals(listOf("p0"), r2.widgets.map { it.id })
    }

    @Test fun theLastPageStays() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(w("a", WidgetType.CLOCK, 0, 0, 2, 2)))
        assertEquals(l, CustomGrid.removePage(l, 0))
    }

    @Test fun pagesStopAtTheMaximum() {
        var l = CustomLayout()
        repeat(10) { l = CustomGrid.addPage(l) }
        assertEquals(MAX_PAGES, l.pages)
    }

    @Test fun smallerGridMovesOrShrinksWidgets() {
        val l = CustomGrid.defaultLayout(GridSize.NORMAL)
        val small = CustomGrid.regrid(l, GridSize.SMALL)
        valid(small)
        assertEquals(GridSize.SMALL, small.grid)
        assertTrue(small.widgets.isNotEmpty())
        // Back to a bigger grid keeps everything that was kept
        val back = CustomGrid.regrid(small, GridSize.LARGE)
        valid(back)
        assertEquals(small.widgets.map { it.id }.toSet(), back.widgets.map { it.id }.toSet())
    }

    @Test fun forgetClearsOnlyThatShortcut() {
        val l = CustomLayout(GridSize.NORMAL, 1, listOf(
            Widget("a", WidgetType.SHORTCUT, 0, 0, 0, 1, 1, value = "com.example.maps"),
            Widget("b", WidgetType.SHORTCUT, 0, 1, 0, 1, 1, value = "place:home")
        ))
        val f = CustomGrid.forget(l, "com.example.maps")
        assertEquals("", CustomGrid.find(f, "a")!!.value)
        assertEquals("place:home", CustomGrid.find(f, "b")!!.value)
    }

    @Test fun saveAndLoadRoundTrip() {
        val l = CustomGrid.add(CustomGrid.defaultLayout(), WidgetType.SHORTCUT, 0, "s", value = "place:abc")!!
            .let { CustomGrid.update(it, "d-clock") { w -> w.copy(face = ClockFace.ANALOG, style = WidgetStyle.TINTED) } }
            .let { CustomGrid.update(it, "d-music") { w -> w.copy(showArt = false, showControls = false) } }
        val back = layoutFromJson(JSONObject(layoutToJson(l).toString()))
        assertEquals(l.grid, back.grid)
        assertEquals(l.pages, back.pages)
        assertEquals(l.widgets.toSet(), back.widgets.toSet())
    }

    @Test fun damagedSavesAreMadeSafe() {
        val o = JSONObject()
            .put("grid", "HUGE")          // unknown: the normal grid
            .put("pages", 99)             // too many: the maximum
            .put("widgets", JSONArray()
                .put(JSONObject().put("id", "a").put("type", "CLOCK").put("c", 0).put("r", 0).put("w", 3).put("h", 2))
                .put(JSONObject().put("id", "b").put("type", "CLOCK").put("c", 1).put("r", 1).put("w", 3).put("h", 2))   // overlaps a
                .put(JSONObject().put("id", "c").put("type", "UFO").put("c", 0).put("r", 3).put("w", 1).put("h", 1))     // unknown type
                .put(JSONObject().put("id", "a").put("type", "SPEED").put("c", 6).put("r", 0).put("w", 2).put("h", 2))   // repeated id
                .put(JSONObject().put("id", "d").put("type", "SPEED").put("c", 7).put("r", 3).put("w", 2).put("h", 2))   // off the grid
                .put("not a widget"))
        val l = layoutFromJson(o)
        valid(l)
        assertEquals(GridSize.NORMAL, l.grid)
        assertEquals(MAX_PAGES, l.pages)
        assertNotNull(CustomGrid.find(l, "b"))   // moved, not lost
        assertNull(CustomGrid.find(l, "c"))
        assertNotNull(CustomGrid.find(l, "d"))
        assertEquals(1, l.widgets.count { it.id == "a" })
    }

    @Test fun emptyJsonGivesAnEmptyValidLayout() {
        val l = layoutFromJson(JSONObject())
        assertEquals(GridSize.NORMAL, l.grid)
        assertEquals(1, l.pages)
        assertTrue(l.widgets.isEmpty())
    }
}
