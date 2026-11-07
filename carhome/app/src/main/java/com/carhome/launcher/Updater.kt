package com.carhome.launcher

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateStage { IDLE, CHECKING, LATEST, AVAILABLE, DOWNLOADING, INSTALLING, NEED_PERMISSION, FAILED }

data class UpdateState(
    val stage: UpdateStage = UpdateStage.IDLE,
    /** Version on GitHub, when one was found */
    val version: String? = null,
    /** Download progress 0–100 */
    val progress: Int = 0
)

/** A CarHome release on GitHub. */
data class UpdateRelease(val versionName: String?, val versionCode: Int?, val apkUrl: String, val apkSize: Long) {
    companion object {
        /** Reads GitHub's "latest release" answer; null when it has no usable carhome.apk. */
        fun fromJson(o: JSONObject): UpdateRelease? {
            val assets = o.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                if (a.optString("name") != "carhome.apk") continue
                val url = a.optString("browser_download_url")
                if (!UpdateCheck.trustedDownload(url)) return null
                return UpdateRelease(
                    versionName = UpdateCheck.versionNameFrom(o.optString("name")),
                    versionCode = UpdateCheck.versionCodeFrom(o.optString("body")),
                    apkUrl = url,
                    apkSize = a.optLong("size", -1L)
                )
            }
            return null
        }
    }
}

/**
 * Checks GitHub for a newer CarHome and installs it with Android's own installer.
 * Android only accepts an update signed with the same key, so a different file
 * can never replace CarHome.
 */
class Updater(private val core: Core) {

    private val ctx = core.app
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var release: UpdateRelease? = null
    private var job: Job? = null
    private val dir get() = File(ctx.cacheDir, "update").apply { mkdirs() }

    /** One file per version, so an older download can never be installed by mistake. */
    private fun apkFor(rel: UpdateRelease) = File(dir, "carhome-${rel.versionCode ?: rel.versionName ?: "new"}.apk")

    private fun localCode(): Int = runCatching {
        val p = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode.toInt() else @Suppress("DEPRECATION") p.versionCode
    }.getOrDefault(0)

    private fun localName(): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
    }.getOrNull() ?: "0"

    /** Automatic check: only when switched on and not checked in the last 6 hours. */
    fun checkIfDue() {
        if (!core.settings.value.updateCheck) return
        if (!UpdateCheck.isDue(core.prefs.updateCheckedAt, System.currentTimeMillis())) return
        check()
    }

    /** Asks GitHub for the latest release (the "Check now" button). */
    fun check() {
        if (job?.isActive == true) return
        _state.value = _state.value.copy(stage = UpdateStage.CHECKING)
        job = core.scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val body = get(UpdateCheck.LATEST_API) ?: error("no reply")
                    UpdateRelease.fromJson(JSONObject(body))
                }
            }
            core.prefs.updateCheckedAt = System.currentTimeMillis()
            val rel = r.getOrNull()
            release = rel
            _state.value = when {
                r.isFailure -> UpdateState(UpdateStage.FAILED)
                rel != null && UpdateCheck.isNewer(rel.versionCode, rel.versionName, localCode(), localName()) ->
                    UpdateState(UpdateStage.AVAILABLE, version = rel.versionName)
                else -> UpdateState(UpdateStage.LATEST)
            }
        }
    }

    /** Downloads the new version (if needed) and opens Android's install screen. */
    fun install() {
        val rel = release ?: return check()
        if (job?.isActive == true) return
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            // Android asks once: "Allow CarHome to install apps"
            _state.value = _state.value.copy(stage = UpdateStage.NEED_PERMISSION)
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        job = core.scope.launch {
            _state.value = UpdateState(UpdateStage.DOWNLOADING, rel.versionName, 0)
            val ok = withContext(Dispatchers.IO) { runCatching { download(rel) }.getOrDefault(false) }
            if (!ok) { _state.value = UpdateState(UpdateStage.FAILED, rel.versionName); return@launch }
            _state.value = UpdateState(UpdateStage.INSTALLING, rel.versionName, 100)
            val started = withContext(Dispatchers.IO) { runCatching { startInstall(apkFor(rel)) }.isSuccess }
            if (!started) _state.value = UpdateState(UpdateStage.FAILED, rel.versionName)
        }
    }

    /** Called by [UpdateReceiver] when the installer reports back. */
    internal fun onInstallStatus(status: Int, confirm: Intent?) {
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> confirm?.let {
                runCatching { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit   // Android restarts CarHome on the new version
            else -> _state.value = _state.value.copy(stage = UpdateStage.AVAILABLE)   // cancelled or failed: offer again
        }
    }

    private fun download(rel: UpdateRelease): Boolean {
        val out = apkFor(rel)
        if (rel.apkSize > 0 && out.length() == rel.apkSize) return true   // already downloaded
        // Clear downloads of other versions
        dir.listFiles()?.forEach { if (it != out) it.delete() }
        val conn = URL(rel.apkUrl).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "CarHome (Android launcher)")
            if (conn.responseCode != 200) return false
            val total = if (rel.apkSize > 0) rel.apkSize else conn.contentLengthLong
            val tmp = File(dir, "download.part")
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var shown = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val p = (done * 100 / total).toInt().coerceIn(0, 99)
                            if (p != shown) { shown = p; _state.value = _state.value.copy(progress = p) }
                        }
                    }
                }
            }
            if (total > 0 && tmp.length() != total) { tmp.delete(); return false }
            return tmp.renameTo(out.also { it.delete() })
        } finally {
            conn.disconnect()
        }
    }

    private fun startInstall(file: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(ctx.packageName) }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("carhome.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val callback = PendingIntent.getBroadcast(
                ctx, id, Intent(ctx, UpdateReceiver::class.java).setAction(UpdateReceiver.ACTION), flags
            )
            session.commit(callback.intentSender)
        }
    }

    private fun get(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "CarHome (Android launcher)")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

/** Receives the installer's answer (confirmation needed, done or failed). */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        @Suppress("DEPRECATION")
        val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        else intent.getParcelableExtra(Intent.EXTRA_INTENT)
        CarHomeApp.core(context).updater.onInstallStatus(status, confirm)
    }

    companion object {
        const val ACTION = "com.carhome.launcher.UPDATE_STATUS"
    }
}
