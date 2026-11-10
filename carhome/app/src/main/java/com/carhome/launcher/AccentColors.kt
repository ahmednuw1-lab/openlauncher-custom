package com.carhome.launcher

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** One ready-made accent colour; [key] names it in the interface. */
data class AccentPreset(val key: String, val argb: Long)

/** A one-tap look: accent colour, background, card style and corners together. */
data class ThemePreset(val key: String, val accent: Long, val background: Background, val cardFill: CardFill, val corners: Corners)

/**
 * Accent colours: the ready-made palette, the custom colour maths, and the
 * rules that keep any chosen colour readable on a dark car screen. No Android
 * classes, so it is unit-tested.
 */
object AccentColors {

    /** Modern colours that stay clear on a dark screen, in sunlight and at night. */
    val PRESETS: List<AccentPreset> = listOf(
        AccentPreset("blue", 0xFF4F8CFF),
        AccentPreset("ice", 0xFF7DD3FC),
        AccentPreset("cyan", 0xFF22D3EE),
        AccentPreset("teal", 0xFF14B8A6),
        AccentPreset("mint", 0xFF6EE7B7),
        AccentPreset("green", 0xFF22C55E),
        AccentPreset("lime", 0xFFA3E635),
        AccentPreset("amber", 0xFFF59E0B),
        AccentPreset("orange", 0xFFFF7A45),
        AccentPreset("coral", 0xFFFB7185),
        AccentPreset("red", 0xFFEF4444),
        AccentPreset("magenta", 0xFFEC4899),
        AccentPreset("violet", 0xFFA855F7),
        AccentPreset("indigo", 0xFF818CF8),
        AccentPreset("gold", 0xFFE7C27D),
        AccentPreset("pearl", 0xFFE5E7EB)
    )

    val THEMES: List<ThemePreset> = listOf(
        ThemePreset("midnight", 0xFF4F8CFF, Background.CLASSIC, CardFill.GLASS, Corners.ROUNDED),
        ThemePreset("ice", 0xFF7DD3FC, Background.CLASSIC, CardFill.FROSTED, Corners.EXTRA),
        ThemePreset("sunset", 0xFFFF7A45, Background.SKY, CardFill.GLASS, Corners.ROUNDED),
        ThemePreset("carbon", 0xFFE5E7EB, Background.CLASSIC, CardFill.SOLID, Corners.SQUARE),
        ThemePreset("emerald", 0xFF34D399, Background.CLASSIC, CardFill.GLASS, Corners.ROUNDED),
        ThemePreset("royal", 0xFFE7C27D, Background.CLASSIC, CardFill.FROSTED, Corners.ROUNDED)
    )

    /** Custom colours never get darker or greyer than this: they'd vanish on the dark screen. */
    const val MIN_SATURATION = 0.25f
    const val MAX_SATURATION = 1f
    private const val VALUE = 1f
    /** Icons and large text need 3:1 against the background (WCAG); a little more for sunlight */
    const val MIN_CONTRAST = 3.2

    /** Always a fully opaque colour, whatever was stored. */
    fun opaque(argb: Long): Long = (argb and 0x00FFFFFFL) or 0xFF000000L

    /** A custom accent from the two sliders: hue 0–360 and how vivid (0 soft … 1 vivid). */
    fun custom(hue: Float, vivid: Float): Long {
        var s = MIN_SATURATION + (MAX_SATURATION - MIN_SATURATION) * vivid.coerceIn(0f, 1f)
        val h = ((hue % 360f) + 360f) % 360f
        var c = hsvToArgb(h, s, VALUE)
        // Pure deep blue is too dark to see on the dark screen: soften it just enough
        while (contrast(c, SCREEN) < MIN_CONTRAST && s > MIN_SATURATION) {
            s = (s - 0.02f).coerceAtLeast(MIN_SATURATION)
            c = hsvToArgb(h, s, VALUE)
        }
        return c
    }

    /** Where the sliders stand for a colour: hue 0–360 and vivid 0–1. */
    fun sliders(argb: Long): Pair<Float, Float> {
        val (h, s, _) = argbToHsv(argb)
        val vivid = ((s - MIN_SATURATION) / (MAX_SATURATION - MIN_SATURATION)).coerceIn(0f, 1f)
        return h to vivid
    }

    fun isPreset(argb: Long): Boolean = PRESETS.any { it.argb == opaque(argb) }

    fun hsvToArgb(h: Float, s: Float, v: Float): Long {
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = v - c
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
        return 0xFF000000L or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    /** Hue 0–360, saturation 0–1, value 0–1. */
    fun argbToHsv(argb: Long): Triple<Float, Float, Float> {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val s = if (max == 0f) 0f else d / max
        return Triple(h, s, max)
    }

    /** Relative luminance 0 (black) … 1 (white), as in the WCAG contrast rules. */
    fun luminance(argb: Long): Double {
        fun lin(c: Long): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin((argb shr 16) and 0xFF) + 0.7152 * lin((argb shr 8) and 0xFF) + 0.0722 * lin(argb and 0xFF)
    }

    /** WCAG contrast ratio between two colours (1 … 21). */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Text and icons drawn ON the accent (a selected chip, a filled button): dark on light colours. */
    fun darkTextOn(argb: Long): Boolean = contrast(argb, DARK_TEXT) > contrast(argb, 0xFFFFFFFF)

    const val DARK_TEXT = 0xFF0B1220L
    /** The darkest part of CarHome's own background */
    const val SCREEN = 0xFF070A12L
}
