package com.carhome.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ObdStatus { OFF, NO_BLUETOOTH, NO_PERMISSION, CHOOSE_ADAPTER, CONNECTING, CONNECTED, FAILED }

data class ObdData(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    /** SystemClock.elapsedRealtime() of this reading. */
    val at: Long = 0L,
    val coolantC: Int? = null,
    val fuelPercent: Int? = null,
    val voltage: Float? = null,
    /** Fault codes from the last "Read codes"; null = not read yet. */
    val codes: List<String>? = null
)

data class PairedDevice(val name: String, val address: String)

/**
 * Reads live engine data from a Bluetooth ELM327 OBD-II adapter (the common
 * cheap kind). The adapter is paired once in Android's Bluetooth settings.
 */
class ObdClient(private val core: Core) {

    private val _status = MutableStateFlow(ObdStatus.OFF)
    val status: StateFlow<ObdStatus> = _status.asStateFlow()

    private val _data = MutableStateFlow(ObdData())
    val data: StateFlow<ObdData> = _data.asStateFlow()

    private var job: Job? = null
    private var jobAddress = ""
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var readCodesRequested = false

    private fun adapter(): BluetoothAdapter? =
        (core.app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        core.app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<PairedDevice> {
        if (!hasPermission()) return emptyList()
        return runCatching {
            adapter()?.bondedDevices.orEmpty().map { PairedDevice(it.name ?: it.address, it.address) }
                .sortedByDescending { d -> d.name.contains("obd", true) || d.name.contains("elm", true) || d.name.contains("vlink", true) }
        }.getOrDefault(emptyList())
    }

    fun readCodes() { readCodesRequested = true }

    /** Starts or stops the connection to match the settings. */
    fun sync() {
        val s = core.settings.value
        if (!s.obd) { stop(); _status.value = ObdStatus.OFF; return }
        if (adapter() == null) { stop(); _status.value = ObdStatus.NO_BLUETOOTH; return }
        if (!hasPermission()) { stop(); _status.value = ObdStatus.NO_PERMISSION; return }
        if (s.obdAddress.isEmpty()) { stop(); _status.value = ObdStatus.CHOOSE_ADAPTER; return }
        // Already talking to this adapter? A different one chosen restarts the connection
        if (job?.isActive == true && jobAddress == s.obdAddress) return
        stop()
        jobAddress = s.obdAddress
        job = core.scope.launch(Dispatchers.IO) { runLoop(s.obdAddress) }
    }

    fun stop() {
        job?.cancel()
        job = null
        runCatching { socket?.close() }
        socket = null
    }

    @SuppressLint("MissingPermission")
    private suspend fun runLoop(address: String) {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            _status.value = ObdStatus.CONNECTING
            val ok = runCatching {
                val dev = adapter()!!.getRemoteDevice(address)
                val sock = dev.createRfcommSocketToServiceRecord(SPP)
                socket = sock
                runCatching { adapter()?.cancelDiscovery() }
                sock.connect()
                val io = Elm(sock.inputStream, sock.outputStream)
                listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0").forEach { io.cmd(it) }
                _status.value = ObdStatus.CONNECTED
                while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                    val speed = io.pid("010D", 1)?.let { it[0] }
                    val rpm = io.pid("010C", 2)?.let { (it[0] * 256 + it[1]) / 4 }
                    val coolant = io.pid("0105", 1)?.let { it[0] - 40 }
                    val fuel = io.pid("012F", 1)?.let { it[0] * 100 / 255 }
                    val volts = io.cmd("ATRV").replace("V", "").trim().toFloatOrNull()
                    var codes = _data.value.codes
                    if (readCodesRequested) {
                        readCodesRequested = false
                        codes = parseCodes(io.cmd("03"))
                    }
                    _data.value = ObdData(rpm, speed, android.os.SystemClock.elapsedRealtime(), coolant, fuel, volts, codes)
                    delay(1_000L)
                }
            }.isSuccess
            runCatching { socket?.close() }
            socket = null
            // Turned off or switched adapter meanwhile: don't overwrite the new status
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) return
            if (!ok) _status.value = ObdStatus.FAILED
            delay(10_000L)   // retry: the adapter may only power up with the ignition
        }
    }

    /** Minimal ELM327 command/response over the serial socket. */
    private class Elm(private val input: InputStream, private val output: OutputStream) {
        fun cmd(c: String): String {
            output.write((c + "\r").toByteArray())
            output.flush()
            val sb = StringBuilder()
            // Boot clock (a time change can't stretch it), and only read what has
            // arrived, so a silent adapter can't block forever
            val started = android.os.SystemClock.elapsedRealtime()
            while (true) {
                if (input.available() > 0) {
                    val b = input.read()
                    if (b < 0) throw java.io.IOException("adapter closed")
                    val ch = b.toChar()
                    if (ch == '>') break
                    sb.append(ch)
                } else {
                    // No '>' prompt in time: replies would get out of step, so reconnect
                    if (android.os.SystemClock.elapsedRealtime() - started > 10_000L) throw java.io.IOException("no reply to $c")
                    Thread.sleep(20)
                }
            }
            return sb.toString().replace("SEARCHING...", "").trim()
        }

        /** Data bytes of a mode-01 PID reply ("410C1AF8" → [0x1A, 0xF8]), or null. */
        fun pid(code: String, bytes: Int): IntArray? {
            val r = cmd(code).replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
            val key = "4" + code.substring(1)
            val i = r.indexOf(key)
            if (i < 0 || r.length < i + key.length + bytes * 2) return null
            val out = IntArray(bytes)
            for (k in 0 until bytes) {
                val start = i + key.length + k * 2
                out[k] = r.substring(start, start + 2).toIntOrNull(16) ?: return null
            }
            return out
        }
    }

    companion object {
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Mode 03 reply → codes like "P0133". */
        fun parseCodes(reply: String): List<String> {
            val out = mutableListOf<String>()
            reply.split('\r', '\n').map { it.replace(" ", "").uppercase() }.filter { it.startsWith("43") }.forEach { line ->
                var hex = line.substring(2)
                // CAN replies start with a count byte, making the length odd in bytes
                if ((hex.length / 2) % 2 == 1) hex = hex.substring(2)
                var k = 0
                while (k + 4 <= hex.length) {
                    val a = hex.substring(k, k + 2).toIntOrNull(16) ?: break
                    val b = hex.substring(k + 2, k + 4).toIntOrNull(16) ?: break
                    k += 4
                    if (a == 0 && b == 0) continue
                    val letter = "PCBU"[(a shr 6) and 3]
                    out += String.format(java.util.Locale.US, "%c%d%X%02X", letter, (a shr 4) and 3, a and 0xF, b)
                }
            }
            return out.distinct()
        }
    }
}
