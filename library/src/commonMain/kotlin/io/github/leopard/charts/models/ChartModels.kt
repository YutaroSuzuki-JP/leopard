package io.github.leopard.charts.models

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor

/**
 * Shape types for line chart data point indicators.
 */
enum class PointShape {
    Circle,
    Square,
    Triangle,
    None
}

/**
 * Represents a single point in a Line or Area chart.
 */
data class PointData(
    val x: Float,
    val y: Float,
    val xLabel: String = x.toString(),
    val yLabel: String = y.toString()
)

/**
 * Represents a series (line) in a multi-line chart.
 */
data class LineSeries(
    val name: String,
    val points: List<PointData>,
    val color: Color,
    val strokeWidth: Float = 3f,
    val isSmooth: Boolean = true,
    val showPoints: Boolean = true,
    val pointRadius: Float = 5f,
    val pointShape: PointShape = PointShape.Circle,
    val pathEffect: PathEffect? = null, // Supports custom dash/stroke patterns
    val fillGradientColors: List<Color>? = null // Gradient fill beneath the line
)

/**
 * Represents a single bar item (used for grouped or single bar charts).
 */
data class BarData(
    val label: String,
    val value: Float,
    val brush: Brush,
    val gradientColors: List<Color>? = null
) {
    constructor(label: String, value: Float, color: Color) : this(label, value, SolidColor(color))
    constructor(label: String, value: Float, gradientColors: List<Color>) : this(label, value, Brush.verticalGradient(gradientColors), gradientColors)
}

/**
 * Represents a group of bars (e.g. showing multiple series for a single X category).
 */
data class BarGroup(
    val groupLabel: String,
    val bars: List<BarData>
)

/**
 * Represents a single segment in a Pie or Donut chart.
 */
data class PieSlice(
    val value: Float,
    val label: String,
    val brush: Brush
) {
    constructor(value: Float, label: String, color: Color) : this(value, label, SolidColor(color))
}

/**
 * Represents a single dimension/axis value for a radar series.
 */
data class RadarPoint(
    val label: String,
    val value: Float
)

/**
 * Represents a dataset/series drawn on a radar chart.
 */
data class RadarSeries(
    val name: String,
    val points: List<RadarPoint>,
    val brush: Brush,
    val fillBrush: Brush
) {
    constructor(name: String, points: List<RadarPoint>, color: Color, fillColor: Color) :
            this(name, points, SolidColor(color), SolidColor(fillColor))
}

/**
 * Represents a single bubble in a Bubble & Scatter chart.
 */
data class BubbleData(
    val x: Float,
    val y: Float,
    val size: Float, // Absolute data value representing size/volume/weight
    val label: String,
    val brush: Brush,
    val meta: String = "",
    val gradientColors: List<Color>? = null
) {
    constructor(x: Float, y: Float, size: Float, label: String, color: Color, meta: String = "") :
            this(x, y, size, label, SolidColor(color), meta)
    constructor(x: Float, y: Float, size: Float, label: String, gradientColors: List<Color>, meta: String = "") :
            this(x, y, size, label, Brush.radialGradient(gradientColors), meta, gradientColors)
}

/**
 * Represents a single candlestick bar in a Candlestick / Stock chart.
 */
data class CandleData(
    val label: String,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float
)

