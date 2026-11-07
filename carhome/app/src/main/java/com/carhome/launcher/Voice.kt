package com.carhome.launcher

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class VoiceStatus { OFF, STARTING, READY, NO_LANGUAGE, NO_ENGINE }

/** Spoken alerts (Android text-to-speech), the alert beep and the soft prayer chime. */
class Voice(private val core: Core) {

    private val _status = MutableStateFlow(VoiceStatus.OFF)
    val status: StateFlow<VoiceStatus> = _status.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsLocale: Locale? = null
    private var pending: String? = null
    private val main = Handler(Looper.getMainLooper())

    /** Starts the speech engine early so the first alert isn't delayed. */
    fun prepare() {
        if (tts != null) return
        _status.value = VoiceStatus.STARTING
        tts = runCatching {
            TextToSpeech(core.app) { result ->
                main.post {
                    if (result == TextToSpeech.SUCCESS) {
                        applyLocale()
                        pending?.let { speakNow(it) }
                    } else {
                        _status.value = VoiceStatus.NO_ENGINE
                    }
                    pending = null
                }
            }
        }.getOrNull()
        if (tts == null) _status.value = VoiceStatus.NO_ENGINE
    }

    private fun applyLocale() {
        val t = tts ?: return
        val loc = core.appLocale()
        val r = runCatching { t.setLanguage(loc) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        ttsLocale = loc
        runCatching {
            t.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        _status.value = if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)
            VoiceStatus.NO_LANGUAGE else VoiceStatus.READY
    }

    /** Speaks [text] if spoken alerts are on (or [force] for the Settings test). */
    fun say(text: String, force: Boolean = false) {
        if (!force && !core.settings.value.voice) return
        if (tts == null) prepare()
        when (_status.value) {
            VoiceStatus.STARTING -> pending = text
            VoiceStatus.READY, VoiceStatus.NO_LANGUAGE -> speakNow(text)
            else -> Unit
        }
    }

    fun say(@StringRes id: Int, vararg args: Any, force: Boolean = false) {
        say(core.localized().getString(id, *args), force)
    }

    private fun speakNow(text: String) {
        val t = tts ?: return
        if (ttsLocale != core.appLocale()) applyLocale()
        if (_status.value == VoiceStatus.NO_LANGUAGE) return
        runCatching { t.speak(text, TextToSpeech.QUEUE_ADD, null, "carhome-${System.nanoTime()}") }
    }

    // ── Sounds ──────────────────────────────────────────────────────────────
    // On a CarPlay box, sound only reaches the car once the car switches its audio
    // to the box, which can take most of a second. So every sound starts with a
    // short silence, asks for audio focus (music ducks, like a Maps prompt), and is
    // sent as a navigation prompt by default, or as media if the user picks that.

    private val am = core.app.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager

    private val alertPcm: ShortArray by lazy { buildAlert() }
    private val chimePcm: ShortArray by lazy { buildChime() }

    /** The speed alert: three clear beeps. */
    fun beep() = play(alertPcm)

    /** Two soft bell notes (E6 → C6), made in code so no sound file is needed. */
    fun chime() = play(chimePcm)

    private fun play(pcm: ShortArray) {
        runCatching {
            val media = core.settings.value.alertChannel == SoundChannel.MEDIA
            val attrs = AudioAttributes.Builder()
                .setUsage(if (media) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val focus = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .build()
            runCatching { am.requestAudioFocus(focus) }
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setVolume(1f)
            track.play()
            val ms = pcm.size * 1000L / SAMPLE_RATE + 300
            main.postDelayed({
                runCatching { track.release() }
                runCatching { am.abandonAudioFocusRequest(focus) }
            }, ms)
        }
    }

    private fun silence(seconds: Double) = DoubleArray((SAMPLE_RATE * seconds).toInt())

    /** 0.6 s of silence for the car to switch audio, then three 1.5 kHz beeps. */
    private fun buildAlert(): ShortArray {
        val lead = silence(0.6)
        val beep = 0.16
        val gap = 0.09
        val body = DoubleArray((SAMPLE_RATE * (3 * beep + 2 * gap + 0.05)).toInt())
        for (k in 0 until 3) {
            val s0 = (k * (beep + gap) * SAMPLE_RATE).toInt()
            val n = (beep * SAMPLE_RATE).toInt()
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                // 5 ms fade in and out so the beeps don't click
                val env = min(1.0, min(t, beep - t) / 0.005)
                body[s0 + i] = env * (0.55 * sin(2 * PI * 1480.0 * t) + 0.15 * sin(2 * PI * 2960.0 * t))
            }
        }
        return toPcm(lead + body)
    }

    private fun buildChime(): ShortArray {
        val notes = listOf(1318.51 to 0.0, 1046.50 to 0.28)   // frequency, start time (s)
        val total = (SAMPLE_RATE * 1.4).toInt()
        val out = DoubleArray(total)
        for ((f, start) in notes) {
            val s0 = (start * SAMPLE_RATE).toInt()
            for (i in s0 until total) {
                val t = (i - s0).toDouble() / SAMPLE_RATE
                val attack = min(1.0, t / 0.004)
                val env = attack * exp(-t * 4.2)
                out[i] += env * (0.30 * sin(2 * PI * f * t) + 0.08 * sin(4 * PI * f * t))
            }
        }
        return toPcm(silence(0.6) + out)
    }

    private fun toPcm(d: DoubleArray) = ShortArray(d.size) { i -> (d[i].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }

    fun release() {
        runCatching { tts?.shutdown() }
        tts = null
        _status.value = VoiceStatus.OFF
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
