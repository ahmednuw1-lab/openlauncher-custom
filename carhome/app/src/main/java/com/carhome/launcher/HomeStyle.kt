package com.carhome.launcher

import org.json.JSONArray
import org.json.JSONObject

// ── Home screen styles (1.3) ─────────────────────────────────────────────────

/** Which home screen CarHome shows: tiles and cards, the calm modern one, or the user's own grid. */
enum class HomeStyle { CLASSIC, MODERN, CUSTOM }

/** Modern home: what fills the space with the clock. SHORTCUTS = places, a suggestion and reminders under it; TODAY = a "Today" panel beside it. */
enum class ModernFill { SHORTCUTS, TODAY }

/** Modern home: what the middle of the screen shows. AUTO = the clock while stopped, the speed while moving. */
enum class ModernCenter { AUTO, CLOCK, SPEED }

/** The screen edge the favourites button sits on. Never mirrored: in a left-hand-drive car the left edge is the driver's. */
enum class FavSide { LEFT, RIGHT }

/** How many favourite apps the modern home's side panel holds (three to a row). */
val FAV_COUNTS = listOf(6, 9, 12)
const val MAX_MODERN_CARDS = 3
val DEFAULT_MODERN_CARDS = listOf(CardType.MUSIC, CardType.WEATHER, CardType.PRAYER)

// ── Custom home: a grid the user fills with widgets ─────────────────────────

/** Columns × rows of the custom home grid. */
enum class GridSize(val cols: Int, val rows: Int) { SMALL(6, 3), NORMAL(8, 4), LARGE(10, 5) }

enum class WidgetType {
    CLOCK, DATE, SPEED, MUSIC, WEATHER, PRAYER, TRIP, CAR, TODAY,
    SHORTCUT, FAVORITES, ALL_APPS, SETTINGS, INTERNET, QIBLA, DUA
}

/** A widget's own card look: glass, solid, no card at all, or tinted with the accent colour. */
enum class WidgetStyle { GLASS, SOLID, CLEAR, TINTED }

enum class ClockFace { DIGITAL, ANALOG, WORDS }

/**
 * One widget on the custom home. [col] and [row] count from the top corner on the
 * reading side (top right in Arabic); [w] × [h] is its size in cells.
 */
data class Widget(
    val id: String,
    val type: WidgetType,
    val page: Int,
    val col: Int,
    val row: Int,
    val w: Int,
    val h: Int,
    val style: WidgetStyle = WidgetStyle.GLASS,
    /** SHORTCUT: an app's package, or "place:<id>"; empty until one is chosen */
    val value: String = "",
    val face: ClockFace = ClockFace.DIGITAL,
    val showArt: Boolean = true,
    val showControls: Boolean = true
)

data class CustomLayout(
    val grid: GridSize = GridSize.NORMAL,
    val pages: Int = 1,
    val widgets: List<Widget> = emptyList()
)

const val MAX_PAGES = 3

/** Placing, moving and resizing widgets. Pure functions: every change returns a new layout. */
object CustomGrid {

    /** The sizes (columns × rows) each kind of widget can take, smallest first. */
    fun sizes(t: WidgetType): List<Pair<Int, Int>> = when (t) {
        WidgetType.CLOCK -> listOf(1 to 1, 2 to 1, 2 to 2, 3 to 1, 3 to 2, 4 to 2)
        WidgetType.DATE -> listOf(2 to 1, 3 to 1)
        WidgetType.SPEED -> listOf(1 to 1, 2 to 1, 2 to 2, 3 to 2)
        WidgetType.MUSIC -> listOf(2 to 1, 3 to 1, 3 to 2, 4 to 2)
        WidgetType.WEATHER, WidgetType.PRAYER -> listOf(1 to 1, 2 to 1, 2 to 2, 3 to 1, 3 to 2)
        WidgetType.TRIP, WidgetType.CAR -> listOf(2 to 2, 3 to 1, 3 to 2, 4 to 1)
        WidgetType.TODAY -> listOf(2 to 2, 3 to 2, 4 to 2)
        WidgetType.SHORTCUT, WidgetType.FAVORITES, WidgetType.ALL_APPS,
        WidgetType.SETTINGS, WidgetType.INTERNET -> listOf(1 to 1, 2 to 1)
        WidgetType.QIBLA -> listOf(1 to 1, 2 to 2)
        WidgetType.DUA -> listOf(2 to 1, 3 to 1, 4 to 1, 4 to 2)
    }

    /** The size a new widget gets when there is room for it. */
    fun defaultSize(t: WidgetType): Pair<Int, Int> = when (t) {
        WidgetType.CLOCK, WidgetType.MUSIC, WidgetType.TODAY -> 3 to 2
        WidgetType.SPEED, WidgetType.CAR -> 2 to 2
        WidgetType.WEATHER, WidgetType.DATE -> 2 to 1
        WidgetType.TRIP, WidgetType.DUA -> 3 to 1
        else -> 1 to 1
    }

    fun inBounds(w: Widget, g: GridSize): Boolean =
        w.w >= 1 && w.h >= 1 && w.col >= 0 && w.row >= 0 &&
            w.col + w.w <= g.cols && w.row + w.h <= g.rows && w.page in 0 until MAX_PAGES

    fun overlaps(a: Widget, b: Widget): Boolean =
        a.page == b.page && a.col < b.col + b.w && b.col < a.col + a.w && a.row < b.row + b.h && b.row < a.row + a.h

    /** Inside the grid, on an existing page, and not on top of any other widget. */
    fun fits(l: CustomLayout, w: Widget): Boolean =
        inBounds(w, l.grid) && w.page < l.pages && l.widgets.none { it.id != w.id && overlaps(it, w) }

    /** The first free spot for a [w] × [h] widget on [page], reading row by row; null when full. */
    fun firstFree(l: CustomLayout, w: Int, h: Int, page: Int, ignoreId: String? = null): Pair<Int, Int>? {
        if (w > l.grid.cols || h > l.grid.rows) return null
        for (r in 0..l.grid.rows - h) for (c in 0..l.grid.cols - w) {
            if (fits(l, Widget(ignoreId ?: PROBE, WidgetType.CLOCK, page, c, r, w, h))) return c to r
        }
        return null
    }

    /**
     * Adds a widget at the first free spot: on [page] first, then the other pages; in its
     * own default size, else the biggest size that still fits. Null when there is no room.
     */
    fun add(
        l: CustomLayout, type: WidgetType, page: Int, id: String,
        size: Pair<Int, Int>? = null, value: String = "", face: ClockFace = ClockFace.DIGITAL
    ): CustomLayout? {
        val wanted = listOfNotNull(size?.takeIf { it in sizes(type) }, defaultSize(type)) +
            sizes(type).sortedByDescending { it.first * it.second }
        val pages = listOf(page.coerceIn(0, l.pages - 1)) + (0 until l.pages).filter { it != page }
        for (sz in wanted.distinct()) {
            for (p in pages) {
                val spot = firstFree(l, sz.first, sz.second, p) ?: continue
                val w = Widget(id, type, p, spot.first, spot.second, sz.first, sz.second, value = value, face = face)
                return l.copy(widgets = l.widgets + w)
            }
        }
        return null
    }

    fun find(l: CustomLayout, id: String): Widget? = l.widgets.firstOrNull { it.id == id }

    private fun replace(l: CustomLayout, w: Widget) = l.copy(widgets = l.widgets.map { if (it.id == w.id) w else it })

    /** Moves a widget to a new top corner; unchanged when it wouldn't fit there. */
    fun move(l: CustomLayout, id: String, col: Int, row: Int): CustomLayout {
        val cur = find(l, id) ?: return l
        val moved = cur.copy(col = col, row = row)
        return if (fits(l, moved)) replace(l, moved) else l
    }

    /** Moves a widget to another page, at the first free spot there; unchanged when that page is full. */
    fun moveToPage(l: CustomLayout, id: String, page: Int): CustomLayout {
        val cur = find(l, id) ?: return l
        if (page == cur.page || page !in 0 until l.pages) return l
        val spot = firstFree(l, cur.w, cur.h, page, ignoreId = id) ?: return l
        return replace(l, cur.copy(page = page, col = spot.first, row = spot.second))
    }

    /**
     * Changes a widget's size. It keeps its corner when it can; otherwise it slides back
     * inside the grid, or moves to the first free spot on its page. Unchanged when the
     * size isn't one the widget can take, or there's no room for it.
     */
    fun resize(l: CustomLayout, id: String, w: Int, h: Int): CustomLayout {
        val cur = find(l, id) ?: return l
        if ((w to h) !in sizes(cur.type)) return l
        val resized = cur.copy(w = w, h = h)
        if (fits(l, resized)) return replace(l, resized)
        val slid = resized.copy(
            col = minOf(cur.col, l.grid.cols - w).coerceAtLeast(0),
            row = minOf(cur.row, l.grid.rows - h).coerceAtLeast(0)
        )
        if (fits(l, slid)) return replace(l, slid)
        val spot = firstFree(l, w, h, cur.page, ignoreId = id) ?: return l
        return replace(l, resized.copy(col = spot.first, row = spot.second))
    }

    /** The allowed size nearest to [w] × [h] (for dragging a widget's corner). */
    fun nearestSize(t: WidgetType, w: Int, h: Int): Pair<Int, Int> =
        sizes(t).minByOrNull { (sw, sh) -> (sw - w) * (sw - w) + (sh - h) * (sh - h) } ?: (1 to 1)

    fun remove(l: CustomLayout, id: String): CustomLayout = l.copy(widgets = l.widgets.filterNot { it.id == id })

    fun update(l: CustomLayout, id: String, change: (Widget) -> Widget): CustomLayout =
        l.copy(widgets = l.widgets.map { if (it.id == id) change(it).copy(id = it.id, page = it.page, col = it.col, row = it.row, w = it.w, h = it.h) else it })

    fun addPage(l: CustomLayout): CustomLayout = if (l.pages < MAX_PAGES) l.copy(pages = l.pages + 1) else l

    /** Deletes a page with its widgets; the pages after it move up by one. The last page can't go. */
    fun removePage(l: CustomLayout, page: Int): CustomLayout {
        if (l.pages <= 1 || page !in 0 until l.pages) return l
        return l.copy(
            pages = l.pages - 1,
            widgets = l.widgets.filter { it.page != page }.map { if (it.page > page) it.copy(page = it.page - 1) else it }
        )
    }

    /** Clears an app or place that no longer exists from every shortcut. */
    fun forget(l: CustomLayout, value: String): CustomLayout =
        l.copy(widgets = l.widgets.map { if (it.type == WidgetType.SHORTCUT && it.value == value) it.copy(value = "") else it })

    /**
     * The layout on another grid size. Widgets keep their spot when it still fits;
     * a widget too big for the new grid takes its biggest size that fits; the rest
     * move to the first free spot (on their page, then the others). A widget with
     * no room anywhere is left out.
     */
    fun regrid(l: CustomLayout, g: GridSize): CustomLayout {
        var out = CustomLayout(g, l.pages.coerceIn(1, MAX_PAGES), emptyList())
        val seen = mutableSetOf<String>()
        for (w in l.widgets.sortedWith(compareBy({ it.page }, { it.row }, { it.col }))) {
            if (w.id.isBlank() || !seen.add(w.id)) continue
            val size = if ((w.w to w.h) in sizes(w.type) && w.w <= g.cols && w.h <= g.rows) w.w to w.h
            else sizes(w.type).filter { it.first <= g.cols && it.second <= g.rows }.maxByOrNull { it.first * it.second } ?: continue
            val cand = w.copy(w = size.first, h = size.second, page = w.page.coerceIn(0, out.pages - 1))
            if (fits(out, cand)) { out = out.copy(widgets = out.widgets + cand); continue }
            val pages = listOf(cand.page) + (0 until out.pages).filter { it != cand.page }
            for (p in pages) {
                val spot = firstFree(out, size.first, size.second, p) ?: continue
                out = out.copy(widgets = out.widgets + cand.copy(page = p, col = spot.first, row = spot.second))
                break
            }
        }
        return out
    }

    /** A loaded layout made safe: no overlaps, nothing outside the grid, no repeated ids. */
    fun sanitize(l: CustomLayout): CustomLayout = regrid(l, l.grid)

    /** What a new custom home starts with: the usual cards, room left for the user's own apps. */
    fun defaultLayout(g: GridSize = GridSize.NORMAL): CustomLayout {
        val base = CustomLayout(
            GridSize.NORMAL, 1, listOf(
                Widget("d-clock", WidgetType.CLOCK, 0, 0, 0, 3, 2, style = WidgetStyle.CLEAR),
                Widget("d-speed", WidgetType.SPEED, 0, 3, 0, 2, 2),
                Widget("d-music", WidgetType.MUSIC, 0, 5, 0, 3, 2, style = WidgetStyle.TINTED),
                Widget("d-weather", WidgetType.WEATHER, 0, 0, 2, 2, 1),
                Widget("d-prayer", WidgetType.PRAYER, 0, 2, 2, 1, 1),
                Widget("d-trip", WidgetType.TRIP, 0, 3, 2, 2, 2),
                Widget("d-favs", WidgetType.FAVORITES, 0, 7, 3, 1, 1),
                Widget("d-net", WidgetType.INTERNET, 0, 2, 3, 1, 1),
                Widget("d-apps", WidgetType.ALL_APPS, 0, 6, 3, 1, 1)
            )
        )
        return if (g == GridSize.NORMAL) base else regrid(base, g)
    }

    private const val PROBE = "\u0000probe"
}

// ── Saving ───────────────────────────────────────────────────────────────────

private inline fun <reified T : Enum<T>> enumNamed(name: String?, def: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: def

fun layoutToJson(l: CustomLayout): JSONObject = JSONObject().apply {
    put("v", 1)
    put("grid", l.grid.name)
    put("pages", l.pages)
    put("widgets", JSONArray().apply {
        l.widgets.forEach { w ->
            put(JSONObject().apply {
                put("id", w.id); put("type", w.type.name); put("p", w.page)
                put("c", w.col); put("r", w.row); put("w", w.w); put("h", w.h)
                put("style", w.style.name); put("value", w.value); put("face", w.face.name)
                put("art", w.showArt); put("ctl", w.showControls)
            })
        }
    })
}

/** Reads a saved layout. Unknown or damaged widgets are skipped; the rest is made safe with [CustomGrid.sanitize]. */
fun layoutFromJson(o: JSONObject): CustomLayout {
    val arr = o.optJSONArray("widgets") ?: JSONArray()
    val widgets = (0 until arr.length()).mapNotNull { i ->
        val w = arr.optJSONObject(i) ?: return@mapNotNull null
        val type = WidgetType.entries.firstOrNull { it.name == w.optString("type") } ?: return@mapNotNull null
        val id = w.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Widget(
            id = id, type = type,
            page = w.optInt("p", 0), col = w.optInt("c", 0), row = w.optInt("r", 0),
            w = w.optInt("w", 1), h = w.optInt("h", 1),
            style = enumNamed(w.optString("style"), WidgetStyle.GLASS),
            value = w.optString("value", "").take(300),
            face = enumNamed(w.optString("face"), ClockFace.DIGITAL),
            showArt = w.optBoolean("art", true),
            showControls = w.optBoolean("ctl", true)
        )
    }
    val l = CustomLayout(
        grid = enumNamed(o.optString("grid"), GridSize.NORMAL),
        pages = o.optInt("pages", 1).coerceIn(1, MAX_PAGES),
        widgets = widgets
    )
    return CustomGrid.sanitize(l)
}
