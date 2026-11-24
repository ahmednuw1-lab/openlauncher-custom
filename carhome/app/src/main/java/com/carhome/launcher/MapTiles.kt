package com.carhome.launcher

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * OpenStreetMap map tiles for the dashboard and the map widget (1.4).
 *
 * Follows the OpenStreetMap tile policy: HTTPS, CarHome's own User-Agent, only the tiles
 * on screen (nothing fetched ahead or saved for offline use), every tile kept at least
 * 7 days and then refreshed with a conditional request. Tiles are only asked for while a
 * map is on screen.
 */
class MapTiles(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memory = object : LruCache<String, ImageBitmap>(MEMORY_TILES) {}
    private val loading = ConcurrentHashMap.newKeySet<String>()
    private val failedAt = ConcurrentHashMap<String, Long>()
    private val network = Semaphore(2)
    private val dir = File(context.cacheDir, "tiles")
    private val userAgent: String by lazy {
        val v = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1"
        "CarHome/$v (Android car launcher; +https://github.com/ahmednuw1-lab/openlauncher-custom)"
    }

    private val _version = MutableStateFlow(0)
    /** Goes up each time a tile arrives, so the map draws again. */
    val version: StateFlow<Int> = _version.asStateFlow()

    @Volatile private var trimmed = false

    /** The tile if it's ready; otherwise null, and it is fetched (from the box, else the internet). */
    fun get(z: Int, x: Int, y: Int): ImageBitmap? {
        val n = TileMath.tilesAcross(z)
        if (z !in 0..19 || y !in 0 until n) return null
        val tx = TileMath.wrapX(x, z)
        val key = "$z/$tx/$y"
        memory.get(key)?.let { return it }
        val failed = failedAt[key]
        if (failed != null && System.currentTimeMillis() - failed < RETRY_MS) return null
        if (loading.add(key)) scope.launch {
            try { load(z, tx, y, key) } finally { loading.remove(key) }
        }
        return null
    }

    /** The tile only if it's already in memory (never fetches): for a stand-in while zooming. */
    fun peek(z: Int, x: Int, y: Int): ImageBitmap? {
        if (z !in 0..19 || y !in 0 until TileMath.tilesAcross(z)) return null
        return memory.get("$z/${TileMath.wrapX(x, z)}/$y")
    }

    private suspend fun load(z: Int, x: Int, y: Int, key: String) {
        if (!trimmed) { trimmed = true; runCatching { trim() } }
        val file = File(dir, "$key.png")
        val tag = File(dir, "$key.etag")
        val onDisk = file.takeIf { it.isFile && it.length() > 0 }
        onDisk?.let { decode(it.readBytes())?.let { b -> memory.put(key, b); _version.update { it + 1 } } }
        val fresh = onDisk != null && System.currentTimeMillis() - onDisk.lastModified() < KEEP_MS
        if (fresh) return
        network.withPermit {
            val conn = URL("https://tile.openstreetmap.org/$z/$x/$y.png").openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("User-Agent", userAgent)
                if (onDisk != null) {
                    runCatching { tag.readText().trim() }.getOrNull()?.takeIf { it.isNotBlank() }
                        ?.let { conn.setRequestProperty("If-None-Match", it) }
                    conn.ifModifiedSince = onDisk.lastModified()
                }
                when (conn.responseCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> {
                        onDisk?.setLastModified(System.currentTimeMillis())
                        failedAt.remove(key)
                    }
                    HttpURLConnection.HTTP_OK -> {
                        val bytes = conn.inputStream.use { readCapped(it) }
                        val bmp = decode(bytes)
                        if (bmp == null) { failedAt[key] = System.currentTimeMillis(); return }
                        memory.put(key, bmp)
                        failedAt.remove(key)
                        _version.update { it + 1 }
                        runCatching {
                            file.parentFile?.mkdirs()
                            file.writeBytes(bytes)
                            conn.getHeaderField("ETag")?.let { tag.writeText(it) } ?: tag.delete()
                        }
                    }
                    else -> failedAt[key] = System.currentTimeMillis()
                }
            } catch (_: Exception) {
                failedAt[key] = System.currentTimeMillis()
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun decode(bytes: ByteArray): ImageBitmap? =
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

    private fun readCapped(input: java.io.InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (out.size() + n > Net.MAX_TILE) throw java.io.IOException("tile too large")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    /** Keeps the tile folder under its size limit: the tiles seen longest ago go first. */
    private fun trim() {
        val files = dir.walkTopDown().filter { it.isFile && it.name.endsWith(".png") }.toList()
        var total = files.sumOf { it.length() }
        if (total <= DISK_BYTES) return
        for (f in files.sortedBy { it.lastModified() }) {
            total -= f.length()
            f.delete()
            File(f.path.removeSuffix(".png") + ".etag").delete()
            if (total <= DISK_BYTES * 3 / 4) break
        }
    }

    companion object {
        /** About two screens of tiles in memory */
        private const val MEMORY_TILES = 96
        /** Tiles on the box: at most 60 MB */
        private const val DISK_BYTES = 60L * 1024 * 1024
        /** The tile policy asks for at least 7 days */
        private const val KEEP_MS = 7L * 24 * 60 * 60 * 1000
        private const val RETRY_MS = 30_000L
    }
}
