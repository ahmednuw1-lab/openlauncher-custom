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
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        cleanup()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun cleanup() {
        instance = null
        runCatching { sessionManager()?.removeOnActiveSessionsChangedListener(sessionsListener) }
        active?.unregisterCallback(controllerCallback)
        active = null
        _nowPlaying.value = null
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
        _nowPlaying.value = NowPlaying(
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "",
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: "",
            art = runCatching {
                meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            }.getOrNull(),
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            appPackage = c.packageName.orEmpty(),
            controller = c
        )
    }

    companion object {
        private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
        val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

        @Volatile private var instance: MediaService? = null
        fun requestRefresh() { instance?.refresh() }

        fun hasAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}
