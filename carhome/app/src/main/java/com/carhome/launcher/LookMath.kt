package com.carhome.launcher

/** Small layout sums for the home screen, kept free of Android so they can be unit-tested. */
object LookMath {

    /** How many whole hour cells fit in [widthDp]; each needs at least [minCellDp]. */
    fun hourCells(widthDp: Float, minCellDp: Float = 58f): Int =
        if (!widthDp.isFinite() || widthDp <= 0f || minCellDp <= 0f) 0 else (widthDp / minCellDp).toInt()

    /** Where a speed sits on the gauge: 0 (empty) … 1 (full scale). */
    fun gaugeFraction(value: Float, maxScale: Float): Float =
        if (!value.isFinite() || maxScale <= 0f) 0f else (value / maxScale).coerceIn(0f, 1f)

    /**
     * Where the red zone starts on the gauge for a speed limit, or null when there is
     * no limit or it lies beyond the scale.
     */
    fun redZoneStart(limit: Int?, maxScale: Float): Float? {
        if (limit == null || limit <= 0 || maxScale <= 0f) return null
        val f = limit / maxScale
        return if (f >= 1f) null else f
    }

    /** The gauge scale: the usual 200 km/h or 130 mph, stretched when the limit is near its end. */
    fun gaugeMax(useKmh: Boolean, limit: Int?): Float {
        val base = if (useKmh) 200f else 130f
        val step = if (useKmh) 20f else 10f
        if (limit == null || limit < base * 0.9f) return base
        // Keep some room above the limit so the red zone is visible
        return (Math.ceil((limit * 1.15) / step) * step).toFloat()
    }

    /**
     * Heights for the temperature curve: 0 (coolest, bottom) … 1 (warmest, top).
     * A small spread of degrees is drawn over at least [minSpan] degrees so that
     * a change of 1° does not swing from the bottom to the top. Bad numbers sit in the middle.
     */
    fun tempLevels(temps: List<Double>, minSpan: Double = 6.0): List<Float> {
        val ok = temps.filter { it.isFinite() }
        if (ok.isEmpty()) return temps.map { 0.5f }
        val lo0 = ok.min()
        val hi0 = ok.max()
        val span = maxOf(hi0 - lo0, if (minSpan.isFinite() && minSpan > 0.0) minSpan else 1.0)
        val mid = (lo0 + hi0) / 2.0
        val lo = mid - span / 2.0
        return temps.map { t -> if (!t.isFinite()) 0.5f else ((t - lo) / span).toFloat().coerceIn(0f, 1f) }
    }
}
