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
    // The strip beside Google Maps (1.4)
    private var strip: StripView? = null
    private var stripParams: WindowManager.LayoutParams? = null
    /** The route the strip was hidden for: it stays hidden until that route ends */
    private var stripHiddenFor: String? = null
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

    // Not sticky: a restart in the background isn't allowed to track location on
    // newer Android, so the home screen starts it again whenever it's shown
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        loop?.cancel()
        removeBubble()
        removeStrip()
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
        val stripShown = renderStrip(s)
        // The strip already shows the speed: no bubble beside it
        val want = s.bubble && Settings.canDrawOverlays(this) && !core.uiVisible.value && !stripShown &&
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

    // ── Strip beside Google Maps (1.4) ────────────────────────────────────────

    /** Shows, updates or removes the strip; true while it's on screen. */
    private fun renderStrip(s: LauncherSettings): Boolean {
        val nav = MediaService.nav.value
        if (nav == null) stripHiddenFor = null
        val want = s.mapsStrip && nav != null && Settings.canDrawOverlays(this) && !core.uiVisible.value &&
            stripHiddenFor != nav.key && (s.stripSpeed || s.stripMusic)
        if (!want) { removeStrip(); return false }
        val v = ensureStrip(s) ?: return false
        val e = core.engine
        val ctx = core.localized()
        val mps = e.speedMps.value
        v.homeLabel = ctx.getString(R.string.app_name)
        v.showSpeed = s.stripSpeed
        v.speedText = mps?.let { (if (e.speedApprox.value) "~" else "") + speedInUnit(it, s.useKmh).roundToInt() } ?: "—"
        v.unitText = ctx.getString(if (s.useKmh) R.string.kmh else R.string.mph)
        v.overspeed = e.overspeed.value
        v.roadLimit = if (s.roadLimit) e.limit.value.roadLimit else null
        val np = MediaService.nowPlaying.value
        v.showMusic = s.stripMusic
        v.hasSong = np != null && np.title.isNotBlank()
        v.songTitle = np?.title.orEmpty()
        v.songArtist = np?.artist.orEmpty()
        v.playing = np?.isPlaying == true
        v.accent = s.accent.toInt()
        v.onLeft = s.stripSide == FavSide.LEFT
        v.refresh()
        return true
    }

    private fun ensureStrip(s: LauncherSettings): StripView? {
        val gravity = (if (s.stripSide == FavSide.LEFT) Gravity.LEFT else Gravity.RIGHT) or Gravity.TOP
        strip?.let { v ->
            val p = stripParams!!
            if (p.gravity != gravity) {
                p.gravity = gravity
                runCatching { wm?.updateViewLayout(v, p) }
            }
            return v
        }
        // Not "layout in screen": the strip stays clear of the status and navigation bars
        val p = WindowManager.LayoutParams(
            (STRIP_DP * resources.displayMetrics.density).roundToInt(),
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        ).apply {
            this.gravity = gravity
            if (Build.VERSION.SDK_INT >= 30) fitInsetsTypes = android.view.WindowInsets.Type.systemBars()
        }
        val v = StripView(this, object : StripView.Listener {
            override fun home() {
                runCatching {
                    startActivity(Intent(this@DriveService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            override fun hide() {
                stripHiddenFor = MediaService.nav.value?.key
                removeStrip()
                Toast.makeText(this@DriveService, core.localized().getString(R.string.strip_hidden), Toast.LENGTH_SHORT).show()
            }
            override fun previous() { MediaService.nowPlaying.value?.controller?.transportControls?.skipToPrevious() }
            override fun next() { MediaService.nowPlaying.value?.controller?.transportControls?.skipToNext() }
            override fun playPause() {
                val np = MediaService.nowPlaying.value ?: return
                if (np.isPlaying) np.controller.transportControls.pause() else np.controller.transportControls.play()
            }
        })
        val added = runCatching { wm?.addView(v, p) }.isSuccess
        if (!added) return null
        strip = v
        stripParams = p
        return v
    }

    private fun removeStrip() {
        strip?.let { runCatching { wm?.removeView(it) } }
        strip = null
        stripParams = null
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
        /** Width of the strip beside Google Maps */
        private const val STRIP_DP = 216

        /** Starts or stops the service to match the settings. Call from the visible activity. */
        fun sync(context: Context) {
            val core = CarHomeApp.core(context)
            val s = core.settings.value
            val intent = Intent(context, DriveService::class.java)
            if ((s.backgroundTracking || s.bubble || s.mapsStrip) && core.engine.hasFine()) {
                runCatching { context.startForegroundService(intent) }
            } else {
                runCatching { context.stopService(intent) }
            }
        }
    }
}
