package com.carhome.launcher

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
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

    private var tone: ToneGenerator? = null

    /** The speed alert beep (media volume, so it plays through the car speakers). */
    fun beep() {
        runCatching {
            val t = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, 90).also { tone = it }
            t.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
        }
    }

    private val chimePcm: ShortArray by lazy { buildChime() }

    /** Two soft bell notes (E6 → C6), made in code so no sound file is needed. */
    fun chime() {
        runCatching {
            val pcm = chimePcm
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
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
            track.play()
            val ms = pcm.size * 1000L / SAMPLE_RATE + 200
            main.postDelayed({ runCatching { track.release() } }, ms)
        }
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
        return ShortArray(total) { i -> (out[i].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
    }

    fun release() {
        runCatching { tts?.shutdown() }
        tts = null
        runCatching { tone?.release() }
        tone = null
        _status.value = VoiceStatus.OFF
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
