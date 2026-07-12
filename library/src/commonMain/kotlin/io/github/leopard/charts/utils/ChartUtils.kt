package io.github.leopard.charts.utils

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

object ChartUtils {

    /**
     * Formats a Float value to a string with a specified number of decimal places.
     */
    fun format(value: Float, decimals: Int = 1): String {
        var multiplier = 1f
        repeat(decimals) { multiplier *= 10f }
        val rounded = round(value * multiplier) / multiplier
        val s = rounded.toString()
        if (decimals == 0 && s.endsWith(".0")) {
            return s.substring(0, s.length - 2)
        }
        return s
    }

    /**
     * Converts radians to degrees.
     */
    fun toDegrees(radians: Double): Double {
        return radians * (180.0 / kotlin.math.PI)
    }

    /**
     * Converts degrees to radians.
     */
    fun toRadians(degrees: Double): Double {
        return degrees * (kotlin.math.PI / 180.0)
    }

    /**
     * Calculates nice round numbers for intervals/ticks on axes.
     */
    fun calculateNiceTicks(min: Float, max: Float, ticksCount: Int): List<Float> {
        if (min >= max) {
            val base = if (min == 0f) 10f else kotlin.math.abs(min) * 2f
            return listOf(0f, base / 2f, base)
        }
        val range = max - min
        val rawSpacing = range / (ticksCount - 1)
        val exponent = floor(log10(rawSpacing.toDouble())).toInt()
        val fraction = rawSpacing / 10.0.pow(exponent.toDouble()).toFloat()
        
        val niceFraction = when {
            fraction < 1.5f -> 1.0f
            fraction < 3.0f -> 2.0f
            fraction < 7.0f -> 5.0f
            else -> 10.0f
        }
        val niceSpacing = niceFraction * 10.0.pow(exponent.toDouble()).toFloat()
        val niceMin = floor(min / niceSpacing) * niceSpacing
        val niceMax = ceil(max / niceSpacing) * niceSpacing
        
        val ticks = mutableListOf<Float>()
        var current = niceMin
        // Use a small epsilon to avoid floating-point omission of the last tick
        val epsilon = niceSpacing * 0.01f
        while (current <= niceMax + epsilon) {
            ticks.add(current)
            current += niceSpacing
        }
        return ticks
    }
}
