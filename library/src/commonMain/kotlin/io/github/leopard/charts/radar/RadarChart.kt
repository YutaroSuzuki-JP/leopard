package io.github.leopard.charts.radar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.leopard.charts.config.AnimationConfig
import io.github.leopard.charts.config.AxisConfig
import io.github.leopard.charts.config.GridConfig
import io.github.leopard.charts.models.RadarSeries
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun RadarChart(
    series: List<RadarSeries>,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    gridConfig: GridConfig = GridConfig(),
    axisConfig: AxisConfig = AxisConfig(),
    animationConfig: AnimationConfig = AnimationConfig()
) {
    if (series.isEmpty() || series.first().points.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // Animation progress using custom AnimationSpec
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(series) {
        if (animationConfig.animateEntry) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = animationConfig.animationSpec
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    val samplePoints = series.first().points
    val numAxes = samplePoints.size
    val maxVal = maxValue ?: series.flatMap { s -> s.points.map { it.value } }.maxOrNull() ?: 10f

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(width, height) / 2f * 0.7f

        if (maxRadius <= 0f) return@Canvas

        val angleStep = (2.0 * PI / numAxes).toFloat()
        val startAngle = -PI.toFloat() / 2f // Top (12 o'clock)

        // 1. Draw Grid concentric webs (e.g. 5 levels of circles/polygons)
        val levels = gridConfig.horizontalLinesCount
        for (level in 1..levels) {
            val levelRatio = level.toFloat() / levels.toFloat()
            val radius = maxRadius * levelRatio
            val gridPath = Path()

            for (i in 0 until numAxes) {
                val angle = (startAngle + i * angleStep).toDouble()
                val x = (centerX + cos(angle) * radius).toFloat()
                val y = (centerY + sin(angle) * radius).toFloat()
                if (i == 0) {
                    gridPath.moveTo(x, y)
                } else {
                    gridPath.lineTo(x, y)
                }
            }
            gridPath.close()

            // Draw level polygon web line
            drawPath(
                path = gridPath,
                color = gridConfig.gridColor,
                style = Stroke(
                    width = gridConfig.strokeWidth.toPx(),
                    pathEffect = gridConfig.horizontalPathEffect
                )
            )
        }

        // 2. Draw Spokes / Axes lines & Vertex Labels
        for (i in 0 until numAxes) {
            val angle = (startAngle + i * angleStep).toDouble()
            val outerX = (centerX + cos(angle) * maxRadius).toFloat()
            val outerY = (centerY + sin(angle) * maxRadius).toFloat()

            // Draw Spoke
            if (axisConfig.showXAxis) {
                drawLine(
                    color = axisConfig.axisColor.copy(alpha = 0.4f),
                    start = Offset(centerX, centerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw Vertex Labels slightly offset outwards from boundary
            if (axisConfig.showXAxisLabels) {
                val labelOffset = 15.dp.toPx()
                val labelX = (centerX + cos(angle) * (maxRadius + labelOffset)).toFloat()
                val labelY = (centerY + sin(angle) * (maxRadius + labelOffset)).toFloat()
                val label = samplePoints[i].label

                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(color = axisConfig.labelColor, fontSize = axisConfig.labelSize)
                )

                // Adjust position based on label quadrant
                val textWidth = textLayoutResult.size.width
                val textHeight = textLayoutResult.size.height
                val drawX = labelX - textWidth / 2f
                val drawY = labelY - textHeight / 2f

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(drawX, drawY)
                )
            }
        }

        // 3. Draw Series Polygons
        val activeProgress = animProgress.value
        series.forEach { s ->
            val seriesPath = Path()
            val points = s.points

            for (i in 0 until numAxes) {
                val pointVal = points.getOrNull(i)?.value ?: 0f
                val ratio = (pointVal / maxVal) * activeProgress
                val radius = maxRadius * ratio
                val angle = (startAngle + i * angleStep).toDouble()
                val x = (centerX + cos(angle) * radius).toFloat()
                val y = (centerY + sin(angle) * radius).toFloat()

                if (i == 0) {
                    seriesPath.moveTo(x, y)
                } else {
                    seriesPath.lineTo(x, y)
                }
            }
            seriesPath.close()

            // Fill transparent inner polygon using s.fillBrush
            drawPath(
                path = seriesPath,
                brush = s.fillBrush,
                style = Fill
            )

            // Draw line boundary using s.brush
            drawPath(
                path = seriesPath,
                brush = s.brush,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Draw points at vertices
            for (i in 0 until numAxes) {
                val pointVal = points.getOrNull(i)?.value ?: 0f
                val ratio = (pointVal / maxVal) * activeProgress
                val radius = maxRadius * ratio
                val angle = (startAngle + i * angleStep).toDouble()
                val x = (centerX + cos(angle) * radius).toFloat()
                val y = (centerY + sin(angle) * radius).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                // Draw inner circle center using s.brush
                // Wait! drawCircle takes color or brush. Let's use brush!
                drawCircle(
                    brush = s.brush,
                    radius = 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
