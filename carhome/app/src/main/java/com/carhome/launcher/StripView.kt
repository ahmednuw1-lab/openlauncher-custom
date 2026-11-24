package com.carhome.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View

/**
 * The strip CarHome shows beside Google Maps while it navigates (1.4): back to CarHome,
 * hide, the speed with the road's limit, and the song with its buttons. Plain Android
 * drawing (no Compose), like the speed bubble, so it stays light over the map.
 */
@SuppressLint("ViewConstructor")
class StripView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        fun home()
        fun hide()
        fun previous()
        fun playPause()
        fun next()
    }

    var homeLabel = "CarHome"
    var showSpeed = true
    var speedText = "—"
    var unitText = ""
    var overspeed = false
    var roadLimit: Int? = null
    var showMusic = true
    var songTitle = ""
    var songArtist = ""
    var playing = false
    var hasSong = false
    var accent: Int = Color.parseColor("#4F8CFF")
    /** Which screen edge the strip is on: the hide arrow points that way */
    var onLeft = true

    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val grey = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C9D0DC") }
    private val sign = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val path = Path()

    // Touch areas, worked out while drawing
    private val homeRect = RectF()
    private val hideRect = RectF()
    private val prevRect = RectF()
    private val playRect = RectF()
    private val nextRect = RectF()

    fun refresh() = invalidate()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 12 * d
        val red = Color.parseColor("#EF4444")
        val card = Color.argb(20, 255, 255, 255)

        // Background: fully opaque, so the app underneath never shows through
        fill.color = Color.rgb(9, 13, 24)
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.color = Color.argb(40, 255, 255, 255)
        if (onLeft) canvas.drawRect(w - d, 0f, w, h, fill) else canvas.drawRect(0f, 0f, d, h, fill)

        // Top row: back to CarHome, and hide
        val rowH = 48 * d
        hideRect.set(w - pad - rowH, pad, w - pad, pad + rowH)
        homeRect.set(pad, pad, hideRect.left - 8 * d, pad + rowH)
        fill.color = (accent and 0x00FFFFFF) or (0x40 shl 24)
        canvas.drawRoundRect(homeRect, 14 * d, 14 * d, fill)
        label.textSize = 15 * d
        label.typeface = Typeface.DEFAULT_BOLD
        label.color = accent
        val home = TextUtils.ellipsize(homeLabel, label, homeRect.width() - 16 * d, TextUtils.TruncateAt.END).toString()
        canvas.drawText(home, homeRect.centerX() - label.measureText(home) / 2, homeRect.centerY() + label.textSize * 0.35f, label)
        fill.color = card
        canvas.drawRoundRect(hideRect, 14 * d, 14 * d, fill)
        chevron(canvas, hideRect.centerX(), hideRect.centerY(), 7 * d, pointLeft = onLeft)

        var top = homeRect.bottom + pad
        val bottom = h - pad
        val musicH = if (showMusic) minOf(150 * d, (bottom - top) * 0.45f) else 0f

        // Speed with the road's limit
        if (showSpeed) {
            val speedBottom = if (showMusic) bottom - musicH - pad else bottom
            val box = RectF(pad, top, w - pad, speedBottom)
            fill.color = if (overspeed) Color.argb(235, 90, 18, 22) else card
            canvas.drawRoundRect(box, 20 * d, 20 * d, fill)
            val hasSign = roadLimit != null
            big.color = if (overspeed) Color.parseColor("#FFD4D4") else Color.WHITE
            big.textSize = minOf(box.width() * 0.42f, box.height() * (if (hasSign) 0.34f else 0.46f))
            val numY = box.top + box.height() * (if (hasSign) 0.40f else 0.52f)
            canvas.drawText(speedText, box.centerX(), numY + big.textSize * 0.35f, big)
            grey.textSize = 13 * d
            grey.textAlign = Paint.Align.CENTER
            canvas.drawText(unitText, box.centerX(), numY + big.textSize * 0.35f + 18 * d, grey)
            grey.textAlign = Paint.Align.LEFT
            roadLimit?.let { lim ->
                val r = minOf(29 * d, box.height() * 0.16f)
                val cy = box.bottom - r - 12 * d
                fill.color = Color.WHITE
                canvas.drawCircle(box.centerX(), cy, r, fill)
                ring.color = red
                ring.strokeWidth = r * 0.2f
                canvas.drawCircle(box.centerX(), cy, r - ring.strokeWidth / 2f, ring)
                sign.textSize = r * (if (lim >= 100) 0.72f else 0.9f)
                canvas.drawText(lim.toString(), box.centerX(), cy + sign.textSize * 0.36f, sign)
            }
            top = box.bottom + pad
        }

        // The song and its buttons
        if (showMusic) {
            val box = RectF(pad, bottom - musicH, w - pad, bottom)
            fill.color = (accent and 0x00FFFFFF) or (0x33 shl 24)
            canvas.drawRoundRect(box, 20 * d, 20 * d, fill)
            val inner = box.width() - 24 * d
            label.typeface = Typeface.DEFAULT_BOLD
            label.color = Color.WHITE
            label.textSize = 15 * d
            grey.textSize = 12 * d
            if (hasSong) {
                val t = TextUtils.ellipsize(songTitle, label, inner, TextUtils.TruncateAt.END).toString()
                canvas.drawText(t, box.left + 12 * d, box.top + 26 * d, label)
                val a = TextUtils.ellipsize(songArtist, grey, inner, TextUtils.TruncateAt.END).toString()
                canvas.drawText(a, box.left + 12 * d, box.top + 44 * d, grey)
            }
            val cy = box.bottom - 34 * d
            val gap = box.width() / 3f
            prevRect.set(box.left, cy - 24 * d, box.left + gap, cy + 24 * d)
            playRect.set(box.left + gap, cy - 26 * d, box.left + 2 * gap, cy + 26 * d)
            nextRect.set(box.left + 2 * gap, cy - 24 * d, box.right, cy + 24 * d)
            fill.color = Color.WHITE
            canvas.drawCircle(playRect.centerX(), cy, 24 * d, fill)
            fill.color = Color.parseColor("#0B1020")
            if (playing) {
                canvas.drawRect(playRect.centerX() - 7 * d, cy - 8 * d, playRect.centerX() - 2.5f * d, cy + 8 * d, fill)
                canvas.drawRect(playRect.centerX() + 2.5f * d, cy - 8 * d, playRect.centerX() + 7 * d, cy + 8 * d, fill)
            } else {
                triangle(canvas, playRect.centerX() + 2 * d, cy, 9 * d, right = true, color = fill.color)
            }
            // Previous and next stay in left-to-right order, like every music player
            fill.color = Color.WHITE
            triangle(canvas, prevRect.centerX(), cy, 7 * d, right = false, color = Color.WHITE)
            canvas.drawRect(prevRect.centerX() - 9 * d, cy - 7 * d, prevRect.centerX() - 7 * d, cy + 7 * d, fill)
            triangle(canvas, nextRect.centerX(), cy, 7 * d, right = true, color = Color.WHITE)
            canvas.drawRect(nextRect.centerX() + 7 * d, cy - 7 * d, nextRect.centerX() + 9 * d, cy + 7 * d, fill)
        } else {
            prevRect.setEmpty(); playRect.setEmpty(); nextRect.setEmpty()
        }
    }

    private fun triangle(canvas: Canvas, cx: Float, cy: Float, r: Float, right: Boolean, color: Int) {
        path.reset()
        if (right) { path.moveTo(cx - r, cy - r); path.lineTo(cx + r, cy); path.lineTo(cx - r, cy + r) }
        else { path.moveTo(cx + r, cy - r); path.lineTo(cx - r, cy); path.lineTo(cx + r, cy + r) }
        path.close()
        fill.color = color
        canvas.drawPath(path, fill)
    }

    private fun chevron(canvas: Canvas, cx: Float, cy: Float, r: Float, pointLeft: Boolean) {
        ring.color = Color.parseColor("#C9D0DC")
        ring.strokeWidth = 2.5f * d
        ring.strokeCap = Paint.Cap.ROUND
        path.reset()
        val s = if (pointLeft) 1f else -1f
        path.moveTo(cx + s * r * 0.5f, cy - r)
        path.lineTo(cx - s * r * 0.5f, cy)
        path.lineTo(cx + s * r * 0.5f, cy + r)
        canvas.drawPath(path, ring)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked != MotionEvent.ACTION_UP) return true
        val x = e.x
        val y = e.y
        when {
            homeRect.contains(x, y) -> listener.home()
            hideRect.contains(x, y) -> listener.hide()
            prevRect.contains(x, y) -> listener.previous()
            playRect.contains(x, y) -> listener.playPause()
            nextRect.contains(x, y) -> listener.next()
        }
        return true
    }
}
