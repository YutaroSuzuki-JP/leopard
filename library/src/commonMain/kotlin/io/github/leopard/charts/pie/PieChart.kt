package io.github.leopard.charts.pie

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.leopard.charts.config.AnimationConfig
import io.github.leopard.charts.config.TooltipConfig
import io.github.leopard.charts.models.PieSlice
import io.github.leopard.charts.utils.ChartUtils
import kotlin.math.*

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    innerRadiusRatio: Float = 0f, // 0.0 for Pie, e.g. 0.6 for Donut
    sliceExplodeDistance: Dp = 12.dp,
    sliceGapAngle: Float = 1.5f, // Spacing gap between slices in degrees
    legendConfig: TextStyle = TextStyle(color = Color.DarkGray),
    tooltipConfig: TooltipConfig = TooltipConfig(),
    animationConfig: AnimationConfig = AnimationConfig()
) {
    if (slices.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val totalValue = slices.sumOf { it.value.toDouble() }.toFloat()

    // Sweep animation using custom AnimationSpec
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
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

    var selectedIndex by remember { mutableStateOf(-1) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(slices) {
                detectTapGestures { offset ->
                    if (totalValue == 0f) return@detectTapGestures
                    val width = size.width
                    val height = size.height
                    val centerX = width / 2f
                    val centerY = height / 2f
                    val outerRadius = min(width, height) / 2f * 0.8f

                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val distance = sqrt(dx * dx + dy * dy)

                    // Check if click is within bounds
                    val innerRadius = outerRadius * innerRadiusRatio
                    if (distance in innerRadius..outerRadius) {
                        var angle = ChartUtils.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < 0) angle += 360f

                        // Find which slice matches the angle
                        var currentAngle = 270f // Standard starting position at top (12 o'clock)
                        var clickedIdx = -1
                        for (i in slices.indices) {
                            val sweepAngle = (slices[i].value / totalValue) * 360f
                            val start = currentAngle % 360f
                            val end = (currentAngle + sweepAngle) % 360f

                            val isMatched = if (start < end) {
                                angle in start..end
                            } else {
                                angle >= start || angle <= end
                            }

                            if (isMatched) {
                                clickedIdx = i
                                break
                            }
                            currentAngle += sweepAngle
                        }
                        selectedIndex = if (selectedIndex == clickedIdx) -1 else clickedIdx
                    } else {
                        selectedIndex = -1
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val centerX = width / 2f
        val centerY = height / 2f
        val outerRadius = min(width, height) / 2f * 0.8f
        val innerRadius = outerRadius * innerRadiusRatio

        if (outerRadius <= 0f) return@Canvas

        var currentAngle = 270f
        val activeProgress = animProgress.value

        slices.forEachIndexed { index, slice ->
            val sweepAngle = (slice.value / totalValue) * 360f * activeProgress
            if (sweepAngle <= 0f) return@forEachIndexed

            // Subtract sliceGapAngle to draw gaps between slices
            val drawSweep = (sweepAngle - sliceGapAngle).coerceAtLeast(0.1f)
            val drawStart = currentAngle + sliceGapAngle / 2f

            // Calculate shift for exploded slice
            val isSelected = index == selectedIndex
            val shiftMultiplier = if (isSelected) 1f else 0f
            val bisectorAngle = ChartUtils.toRadians((currentAngle + sweepAngle / 2f).toDouble())
            val shiftX = cos(bisectorAngle).toFloat() * sliceExplodeDistance.toPx() * shiftMultiplier
            val shiftY = sin(bisectorAngle).toFloat() * sliceExplodeDistance.toPx() * shiftMultiplier

            val sliceCenter = Offset(centerX + shiftX, centerY + shiftY)

            if (innerRadiusRatio == 0f) {
                // Pie Chart: Draw standard filled arc using slice.brush
                val rect = Rect(
                    left = sliceCenter.x - outerRadius,
                    top = sliceCenter.y - outerRadius,
                    right = sliceCenter.x + outerRadius,
                    bottom = sliceCenter.y + outerRadius
                )
                val path = Path().apply {
                    moveTo(sliceCenter.x, sliceCenter.y)
                    arcTo(rect, drawStart, drawSweep, false)
                    close()
                }
                drawPath(path = path, brush = slice.brush, style = Fill)
            } else {
                // Donut Chart: Draw arc stroke using slice.brush
                val thickness = outerRadius - innerRadius
                val drawRadius = outerRadius - thickness / 2f
                val rect = Rect(
                    left = sliceCenter.x - drawRadius,
                    top = sliceCenter.y - drawRadius,
                    right = sliceCenter.x + drawRadius,
                    bottom = sliceCenter.y + drawRadius
                )
                drawArc(
                    brush = slice.brush,
                    startAngle = drawStart,
                    sweepAngle = drawSweep,
                    useCenter = false,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = thickness, cap = StrokeCap.Butt)
                )
            }

            // Draw clean border between slices only if no gap angle is configured
            if (innerRadiusRatio == 0f && sliceGapAngle == 0f) {
                val endX = sliceCenter.x + cos(ChartUtils.toRadians(currentAngle.toDouble())).toFloat() * outerRadius
                val endY = sliceCenter.y + sin(ChartUtils.toRadians(currentAngle.toDouble())).toFloat() * outerRadius
                drawLine(
                    color = Color.White,
                    start = sliceCenter,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            currentAngle += (slice.value / totalValue) * 360f
        }

        // Draw Interactive Tooltip/Label in the center (for donut chart selection)
        if (selectedIndex != -1 && innerRadiusRatio > 0.4f) {
            val selectedSlice = slices[selectedIndex]
            val pct = (selectedSlice.value / totalValue) * 100f
            val labelText = selectedSlice.label
            val valText = "${ChartUtils.format(pct, 1)}%"

            val layoutLabel = textMeasurer.measure(
                text = labelText,
                style = TextStyle(color = legendConfig.color, fontSize = (outerRadius * 0.12f).toSp())
            )
            val layoutVal = textMeasurer.measure(
                text = valText,
                // Attempt to resolve brush style for selected slice text (or solid color)
                style = TextStyle(color = legendConfig.color, fontSize = (outerRadius * 0.16f).toSp())
            )

            val labelW = layoutLabel.size.width.toFloat()
            val labelH = layoutLabel.size.height.toFloat()
            val valW = layoutVal.size.width.toFloat()

            drawText(
                textLayoutResult = layoutLabel,
                topLeft = Offset(centerX - labelW / 2f, centerY - labelH)
            )
            drawText(
                textLayoutResult = layoutVal,
                topLeft = Offset(centerX - valW / 2f, centerY)
            )
        } else if (selectedIndex != -1 && tooltipConfig.showTooltip) {
            val selectedSlice = slices[selectedIndex]
            val pct = (selectedSlice.value / totalValue) * 100f

            var tempAngle = 270f
            for (i in 0 until selectedIndex) {
                tempAngle += (slices[i].value / totalValue) * 360f
            }
            val midSweep = (selectedSlice.value / totalValue) * 180f
            val midAngle = ChartUtils.toRadians((tempAngle + midSweep).toDouble())
            val midRadius = innerRadius + (outerRadius - innerRadius) / 2f + sliceExplodeDistance.toPx()
            val tooltipCenter = Offset(
                centerX + cos(midAngle).toFloat() * midRadius,
                centerY + sin(midAngle).toFloat() * midRadius
            )

            val tooltipPadding = tooltipConfig.padding.toPx()
            val txt = "${selectedSlice.label}: ${selectedSlice.value} (${ChartUtils.format(pct, 1)}%)"
            val textLayout = textMeasurer.measure(
                text = txt,
                style = TextStyle(color = tooltipConfig.textColor, fontSize = tooltipConfig.textSize)
            )

            val tooltipW = textLayout.size.width + tooltipPadding * 2f
            val tooltipH = textLayout.size.height + tooltipPadding * 2f
            val topLeft = Offset(tooltipCenter.x - tooltipW / 2f, tooltipCenter.y - tooltipH / 2f)

            drawRoundRect(
                color = tooltipConfig.backgroundColor,
                topLeft = topLeft,
                size = Size(tooltipW, tooltipH),
                cornerRadius = CornerRadius(tooltipConfig.cornerRadius.toPx()),
                style = Fill
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(topLeft.x + tooltipPadding, topLeft.y + tooltipPadding)
            )
        }
    }
}
