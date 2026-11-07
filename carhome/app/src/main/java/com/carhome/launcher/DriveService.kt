package com.carhome.launcher

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Keeps GPS (speed alert, trip, breaks) running while another app is on screen,
 * and shows the floating speed bubble. Android requires a small permanent
 * notification for this.
 */
class DriveService : Service() {

    private lateinit var core: Core
    private var loop: Job? = null
    private var wm: WindowManager? = null
    private var bubble: BubbleView? = null
    private var params: WindowManager.LayoutParams? = null
    private var hiddenUntilNextDrive = false
    private var seenCarStarts = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        core = CarHomeApp.core(this)
        if (!startAsForeground()) { stopSelf(); return }
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        core.engine.attach(OWNER)
        seenCarStarts = core.carStarts.value
        loop = core.scope.launch {
            while (isActive) {
                runCatching { render() }
                delay(500L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        loop?.cancel()
        removeBubble()
        if (::core.isInitialized) core.engine.detach(OWNER)
        super.onDestroy()
    }

    private fun startAsForeground(): Boolean = runCatching {
        val ctx = core.localized()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_speed)
            .setContentTitle(ctx.getString(R.string.notif_title))
            .setContentText(ctx.getString(R.string.notif_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(NOTIF_ID, n)
        true
    }.getOrDefault(false)

    // ── Bubble ──────────────────────────────────────────────────────────────

    private fun render() {
        val s = core.settings.value
        if (core.carStarts.value != seenCarStarts) {
            seenCarStarts = core.carStarts.value
            hiddenUntilNextDrive = false
        }
        val e = core.engine
        val want = s.bubble && Settings.canDrawOverlays(this) && !core.uiVisible.value &&
            !hiddenUntilNextDrive && (!s.bubbleOnlyMoving || e.moving.value)
        if (!want) { removeBubble(); return }
        val v = ensureBubble(s)
        val mps = e.speedMps.value
        v.speedText = mps?.let { (if (e.speedApprox.value) "~" else "") + speedInUnit(it, s.useKmh).roundToInt() } ?: "—"
        v.unitText = core.localized().getString(if (s.useKmh) R.string.kmh else R.string.mph)
        v.overspeed = e.overspeed.value
        v.roadLimit = if (s.roadLimit) e.limit.value.roadLimit else null
        v.accent = s.accent.toInt()
        v.locked = s.bubbleLock
        v.refresh()
    }

    private fun sizePx(s: LauncherSettings) = (s.bubbleSize.dp * resources.displayMetrics.density).roundToInt()

    private fun screenWidth(): Int = runCatching {
        if (Build.VERSION.SDK_INT >= 30) wm!!.currentWindowMetrics.bounds.width()
        else resources.displayMetrics.widthPixels
    }.getOrDefault(resources.displayMetrics.widthPixels)

    private fun ensureBubble(s: LauncherSettings): BubbleView {
        val size = sizePx(s)
        bubble?.let { b ->
            val p = params!!
            if (p.width != size) {
                p.width = size
                p.height = size
                runCatching { wm?.updateViewLayout(b, p) }
            }
            return b
        }
        val margin = (12 * resources.displayMetrics.density).roundToInt()
        val p = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = core.prefs.bubbleX.takeIf { it >= 0 } ?: (screenWidth() - size - margin)
            y = core.prefs.bubbleY
        }
        val v = BubbleView(this, object : BubbleView.Listener {
            override fun position() = p.x to p.y
            override fun moveTo(x: Int, y: Int) {
                p.x = x.coerceAtLeast(0)
                p.y = y.coerceAtLeast(0)
                runCatching { wm?.updateViewLayout(bubble, p) }
            }
            override fun dropped() = snapToEdge()
            override fun tapped() {
                runCatching {
                    startActivity(Intent(this@DriveService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            override fun longPressed() {
                hiddenUntilNextDrive = true
                removeBubble()
                Toast.makeText(this@DriveService, core.localized().getString(R.string.bubble_hidden), Toast.LENGTH_SHORT).show()
            }
        })
        val added = runCatching { wm?.addView(v, p) }.isSuccess
        if (added) {
            bubble = v
            params = p
        }
        return v
    }

    /** Slides to the nearest side so it never sits in the middle of the map. */
    private fun snapToEdge() {
        val v = bubble ?: return
        val p = params ?: return
        val margin = (12 * resources.displayMetrics.density).roundToInt()
        val w = screenWidth()
        val target = if (p.x + p.width / 2 < w / 2) margin else w - p.width - margin
        ValueAnimator.ofInt(p.x, target).apply {
            duration = 220L
            addUpdateListener {
                p.x = it.animatedValue as Int
                runCatching { wm?.updateViewLayout(v, p) }
            }
            start()
        }
        core.prefs.bubbleX = target
        core.prefs.bubbleY = p.y
    }

    private fun removeBubble() {
        bubble?.let { runCatching { wm?.removeView(it) } }
        bubble = null
        params = null
    }

    companion object {
        private const val OWNER = "service"
        private const val CHANNEL = "driving"
        private const val NOTIF_ID = 42

        /** Starts or stops the service to match the settings. Call from the visible activity. */
        fun sync(context: Context) {
            val core = CarHomeApp.core(context)
            val s = core.settings.value
            val intent = Intent(context, DriveService::class.java)
            if ((s.backgroundTracking || s.bubble) && core.engine.hasFine()) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.stopService(intent) }
            }
        }
    }
}
