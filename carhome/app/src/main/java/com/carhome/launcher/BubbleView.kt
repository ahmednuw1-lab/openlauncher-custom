package com.carhome.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * The floating speed bubble shown over other apps. Plain Android drawing (no
 * Compose) so it stays light while Maps is on screen.
 */
@SuppressLint("ViewConstructor")
class BubbleView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        fun position(): Pair<Int, Int>
        fun moveTo(x: Int, y: Int)
        fun dropped()
        fun tapped()
        fun longPressed()
    }

    var speedText = "—"
    var unitText = ""
    var overspeed = false
    var roadLimit: Int? = null
    var accent: Int = Color.parseColor("#4F8CFF")
    var locked = false

    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A7B0C2")
        textAlign = Paint.Align.CENTER
    }
    private val signText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    fun refresh() = invalidate()

    override fun onDraw(canvas: Canvas) {
        val d = minOf(width, height).toFloat()
        val cx = d / 2f
        val cy = d / 2f
        val r = d / 2f - 3 * density
        val red = Color.parseColor("#EF4444")

        fill.color = if (overspeed) Color.argb(235, 90, 18, 22) else Color.argb(230, 16, 22, 38)
        canvas.drawCircle(cx, cy, r, fill)
        ring.color = if (overspeed) red else accent
        ring.strokeWidth = 3.5f * density
        canvas.drawCircle(cx, cy, r, ring)

        text.color = if (overspeed) Color.parseColor("#FFD4D4") else Color.WHITE
        text.textSize = d * 0.36f
        canvas.drawText(speedText, cx, cy + d * 0.10f, text)
        small.textSize = d * 0.13f
        canvas.drawText(unitText, cx, cy + d * 0.29f, small)

        // Road speed-limit sign in the top corner
        roadLimit?.let { lim ->
            val sr = d * 0.2f
            val sx = d - sr - 1 * density
            val sy = sr + 1 * density
            fill.color = Color.WHITE
            canvas.drawCircle(sx, sy, sr, fill)
            ring.color = red
            ring.strokeWidth = sr * 0.22f
            canvas.drawCircle(sx, sy, sr - ring.strokeWidth / 2f, ring)
            signText.textSize = sr * (if (lim >= 100) 0.72f else 0.9f)
            canvas.drawText(lim.toString(), sx, sy + signText.textSize * 0.36f, signText)
        }
    }

    // ── Touch: drag to move, tap to open CarHome, long-press to hide ─────────

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var longFired = false
    private val longPress = Runnable { longFired = true; listener.longPressed() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                val (x, y) = listener.position()
                startX = x
                startY = y
                dragging = false
                longFired = false
                handler.postDelayed(longPress, 650L)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                if (!dragging && hypot(dx, dy) > slop) {
                    dragging = true
                    handler.removeCallbacks(longPress)
                }
                if (dragging && !locked) listener.moveTo(startX + dx.toInt(), startY + dy.toInt())
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                when {
                    dragging -> if (!locked) listener.dropped()
                    !longFired -> listener.tapped()
                }
            }
            MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longPress)
        }
        return true
    }
}
