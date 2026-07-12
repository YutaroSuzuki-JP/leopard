package io.github.leopard.charts.config

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.leopard.charts.utils.ChartUtils

/**
 * Grid line styling and presence configuration.
 */
data class GridConfig(
    val showHorizontalLines: Boolean = true,
    val showVerticalLines: Boolean = true,
    val gridColor: Color = Color.LightGray.copy(alpha = 0.5f),
    val strokeWidth: Dp = 1.dp,
    val horizontalLinesCount: Int = 5,
    val verticalLinesCount: Int = 5,
    val horizontalPathEffect: PathEffect? = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
    val verticalPathEffect: PathEffect? = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
)

/**
 * Axis labels, ticks, and bounds configuration.
 */
data class AxisConfig(
    val showXAxis: Boolean = true,
    val showYAxis: Boolean = true,
    val axisColor: Color = Color.DarkGray,
    val axisThickness: Dp = 2.dp,
    val labelColor: Color = Color.Gray,
    val labelSize: TextUnit = 12.sp,
    val yLabelFormatter: (Float) -> String = { ChartUtils.format(it, 1) },
    val xLabelFormatter: (String) -> String = { it },
    val showTicks: Boolean = true,
    val tickLength: Dp = 4.dp,
    val showXAxisLabels: Boolean = true,
    val showYAxisLabels: Boolean = true
)

/**
 * Tooltip overlay configuration for interactive elements.
 */
data class TooltipConfig(
    val showTooltip: Boolean = true,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.8f),
    val textColor: Color = Color.White,
    val textSize: TextUnit = 12.sp,
    val cornerRadius: Dp = 6.dp,
    val padding: Dp = 8.dp,
    val elevation: Dp = 4.dp,
    val guideLineColor: Color? = Color.DarkGray.copy(alpha = 0.4f),
    val guideLinePathEffect: PathEffect? = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
)

/**
 * Entry and transition animation configurations.
 */
enum class LineAnimationType {
    Grow,  // Bottom-up growth animation
    Draw   // Left-to-right path drawing animation (point to point connection)
}

data class AnimationConfig(
    val animateEntry: Boolean = true,
    val animationSpec: AnimationSpec<Float> = tween(
        durationMillis = 1000,
        easing = FastOutSlowInEasing
    ),
    val lineAnimationType: LineAnimationType = LineAnimationType.Grow
)

/**
 * Legend presentation configuration.
 */
data class LegendConfig(
    val showLegend: Boolean = true,
    val textColor: Color = Color.LightGray,
    val textSize: TextUnit = 12.sp,
    val iconSize: Dp = 8.dp,
    val spacing: Dp = 12.dp
)
