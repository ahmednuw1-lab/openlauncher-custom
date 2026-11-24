package com.carhome.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val appPackage: String,
    val controller: MediaController
)

/**
 * Notification-listener service: the only public way for a launcher to see and
 * control other apps' media sessions. The user grants it once in Settings.
 */
class MediaService : NotificationListenerService() {

    private var active: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { refresh() }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        runCatching {
            sessionManager()?.addOnActiveSessionsChangedListener(
                sessionsListener, ComponentName(this, MediaService::class.java), mainHandler
            )
        }
        refresh()
        runCatching { scanNav() }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        cleanup()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    // Session and controller callbacks already report changes; notifications are only a
    // fallback, so ignore everything except media ones (progress, chats, downloads…)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (isMedia(sbn)) refresh()
        if (sbn?.packageName in NavParse.NAV_APPS) readNav(sbn)
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (isMedia(sbn)) refresh()
        if (sbn != null && sbn.key == _nav.value?.key) _nav.value = null
    }

    // ── Navigation (1.4): the next turn from Google Maps' own notification ──────

    private fun readNav(sbn: StatusBarNotification?) {
        val n = sbn?.notification ?: return
        val x = n.extras ?: return
        val title = NavParse.clean(x.getCharSequence(android.app.Notification.EXTRA_TITLE))
        val ongoing = (n.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        if (!NavParse.isNav(sbn.packageName, ongoing, n.category, n.channelId, title)) {
            // The same notification can stop being a navigation one (the trip ended)
            if (sbn.key == _nav.value?.key) _nav.value = null
            return
        }
        val info = NavParse.info(
            x.getCharSequence(android.app.Notification.EXTRA_TITLE),
            x.getCharSequence(android.app.Notification.EXTRA_TEXT),
            x.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT),
            x.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT),
            null, sbn.packageName, sbn.key
        )
        val old = _nav.value
        if (info == null) { if (old?.key == sbn.key) _nav.value = null; return }
        // Google Maps posts again every few seconds: only a real change goes on screen
        if (old != null && old.key == info.key && old.title == info.title && old.text == info.text && old.sub == info.sub) return
        // The turn arrow Google Maps draws (its large icon), at a size the card can use
        val icon = runCatching {
            n.getLargeIcon()?.loadDrawable(this)?.let { d ->
                val size = (48 * resources.displayMetrics.density).toInt().coerceAtLeast(24)
                d.toBitmap(size, size)
            }
        }.getOrNull()
        _nav.value = info.copy(icon = icon)
    }

    private fun scanNav() {
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
            .filter { it.packageName in NavParse.NAV_APPS }
        active.forEach { readNav(it) }
        if (active.none { it.key == _nav.value?.key }) _nav.value = null
    }

    private fun isMedia(sbn: StatusBarNotification?): Boolean {
        val n = sbn?.notification ?: return false
        return n.extras?.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION) == true ||
            n.category == android.app.Notification.CATEGORY_TRANSPORT
    }

    /** What the card shows, minus the picture: when it's unchanged nothing is re-sent. */
    private var lastKey: String? = null
    private var lastArtKey: String? = null
    private var lastArt: Bitmap? = null

    private fun cleanup() {
        instance = null
        lastKey = null; lastArtKey = null; lastArt = null
        runCatching { sessionManager()?.removeOnActiveSessionsChangedListener(sessionsListener) }
        active?.unregisterCallback(controllerCallback)
        active = null
        _nowPlaying.value = null
        _nav.value = null
    }

    private fun sessionManager() = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private fun refresh() {
        val sessions = try {
            sessionManager()?.getActiveSessions(ComponentName(this, MediaService::class.java)).orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
        val pick = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()

        if (pick == null) {
            active?.unregisterCallback(controllerCallback)
            active = null
            lastKey = null; lastArtKey = null; lastArt = null
            _nowPlaying.value = null
            return
        }
        // getActiveSessions returns new objects each call — compare tokens, not instances
        if (pick.sessionToken != active?.sessionToken) {
            active?.unregisterCallback(controllerCallback)
            active = pick
            pick.registerCallback(controllerCallback, mainHandler)
        }
        val c = active ?: return
        val meta = c.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        val artKey = "${c.sessionToken.hashCode()}|$title|$artist|${meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)}"
        val key = "$artKey|$playing"
        if (key == lastKey && _nowPlaying.value != null) return   // nothing the card shows has changed
        lastKey = key
        // Same song: keep the picture we already have (no new bitmap, no new colour pass)
        if (artKey != lastArtKey) {
            lastArtKey = artKey
            lastArt = runCatching {
                meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            }.getOrNull()
        }
        _nowPlaying.value = NowPlaying(
            title = title,
            artist = artist,
            art = lastArt,
            isPlaying = playing,
            appPackage = c.packageName.orEmpty(),
            controller = c
        )
    }

    companion object {
        private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
        val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

        private val _nav = MutableStateFlow<NavInfo?>(null)
        /** The next turn while Google Maps navigates (1.4); null otherwise. */
        val nav: StateFlow<NavInfo?> = _nav

        @Volatile private var instance: MediaService? = null
        fun requestRefresh(context: Context? = null) {
            val svc = instance
            if (svc != null) { svc.refresh(); return }
            // Granted but not connected (some boxes after an app update): ask Android to rebind
            if (context != null && hasAccess(context)) runCatching {
                NotificationListenerService.requestRebind(ComponentName(context, MediaService::class.java))
            }
        }

        fun hasAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}
