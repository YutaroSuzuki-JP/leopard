package io.github.leopard.charts.bubble

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import io.github.leopard.charts.config.AxisConfig
import io.github.leopard.charts.config.GridConfig
import io.github.leopard.charts.config.TooltipConfig
import io.github.leopard.charts.models.BubbleData
import io.github.leopard.charts.utils.ChartUtils
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun BubbleChart(
    bubbles: List<BubbleData>,
    modifier: Modifier = Modifier,
    minBubbleRadius: Dp = 6.dp,
    maxBubbleRadius: Dp = 32.dp,
    gridConfig: GridConfig = GridConfig(),
    axisConfig: AxisConfig = AxisConfig(),
    tooltipConfig: TooltipConfig = TooltipConfig(),
    animationConfig: AnimationConfig = AnimationConfig(),
    scrollEnabled: Boolean = true
) {
    if (bubbles.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val textMeasurer = rememberTextMeasurer()

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(bubbles) {
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
    val selectScale = remember { Animatable(1f) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != -1) {
            selectScale.snapTo(1f)
            selectScale.animateTo(
                targetValue = 1.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            selectScale.animateTo(1f, tween(150))
        }
    }

    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val allX = bubbles.map { it.x }
    val allY = bubbles.map { it.y }
    val allSize = bubbles.map { it.size }

    val minX = allX.minOrNull() ?: 0f
    val maxX = allX.maxOrNull() ?: 10f
    val minY = allY.minOrNull() ?: 0f
    val maxY = allY.maxOrNull() ?: 10f
    val minSize = allSize.minOrNull() ?: 1f
    val maxSize = allSize.maxOrNull() ?: 10f

    val xRange = maxX - minX
    val xPad = if (xRange == 0f) 1f else xRange * 0.15f
    val xMin = minX - xPad
    val xMax = maxX + xPad

    val yRange = maxY - minY
    val yPad = if (yRange == 0f) 1f else yRange * 0.15f
    val yMin = minY - yPad
    val yMax = maxY + yPad

    val density = androidx.compose.ui.platform.LocalDensity.current
    val minPointWidth = 70.dp

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val leftPadding = 60.dp
        val rightPadding = 20.dp
        val topPadding = 20.dp
        val bottomPadding = 40.dp

        val pointsCount = bubbles.size

        val availableWidth = (containerWidth - leftPadding - rightPadding).coerceAtLeast(0.dp)
        val calculatedPlotWidth = minPointWidth * pointsCount.coerceAtLeast(1)
        val isScrollable = scrollEnabled && calculatedPlotWidth > availableWidth
        
        val requiredChartWidth = if (isScrollable) {
            calculatedPlotWidth
        } else {
            availableWidth
        }

        val leftPaddingPx = leftPadding.value * density.density
        val rightPaddingPx = rightPadding.value * density.density
        val topPaddingPx = topPadding.value * density.density
        val bottomPaddingPx = bottomPadding.value * density.density

        val chartHeight = (containerHeight - topPadding - bottomPadding).value * density.density
        val bottomY = containerHeight.value * density.density - bottomPaddingPx

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftPadding)
        ) {
            val yTicks = ChartUtils.calculateNiceTicks(yMin, yMax, gridConfig.horizontalLinesCount)
            
            if (axisConfig.showYAxisLabels) {
                yTicks.forEach { valY ->
                    val ratio = (valY - yMin) / (yMax - yMin)
                    val y = bottomY - ratio * chartHeight
                    val label = axisConfig.yLabelFormatter(valY)
                    val textLayoutResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(color = axisConfig.labelColor, fontSize = axisConfig.labelSize)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = leftPaddingPx - textLayoutResult.size.width - 10.dp.toPx(),
                            y = y - textLayoutResult.size.height / 2f
                        )
                    )
                }
            }

            if (axisConfig.showYAxis) {
                drawLine(
                    color = axisConfig.axisColor,
                    start = Offset(leftPaddingPx, topPaddingPx),
                    end = Offset(leftPaddingPx, bottomY),
                    strokeWidth = axisConfig.axisThickness.toPx()
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftPadding, end = rightPadding)
        ) {
            val yTicks = ChartUtils.calculateNiceTicks(yMin, yMax, gridConfig.horizontalLinesCount)
            if (gridConfig.showHorizontalLines && yTicks.isNotEmpty()) {
                yTicks.forEach { valY ->
                    val ratio = (valY - yMin) / (yMax - yMin)
                    val y = bottomY - ratio * chartHeight
                    drawLine(
                        color = gridConfig.gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = gridConfig.strokeWidth.toPx(),
                        pathEffect = gridConfig.horizontalPathEffect
                    )
                }
            }
        }

        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftPadding)
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(requiredChartWidth + rightPadding)
                    .pointerInput(bubbles, isScrollable) {
                        if (isScrollable) {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchOffset = offset
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchOffset = offset
                                }
                            )
                        } else {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchOffset = offset
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchOffset = offset
                                }
                            )
                        }
                    }
                    .then(
                        if (!isScrollable) {
                            Modifier.pointerInput(bubbles) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        touchOffset = offset
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    },
                                    onDrag = { change, _ ->
                                        touchOffset = change.position
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                val plotWidth = requiredChartWidth.toPx()
                val progress = animProgress.value

                if (axisConfig.showXAxis) {
                    drawLine(
                        color = axisConfig.axisColor,
                        start = Offset(0f, bottomY),
                        end = Offset(plotWidth + rightPaddingPx, bottomY),
                        strokeWidth = axisConfig.axisThickness.toPx()
                    )
                }

                val xTicks = ChartUtils.calculateNiceTicks(xMin, xMax, gridConfig.verticalLinesCount)
                
                if (gridConfig.showVerticalLines && xTicks.isNotEmpty()) {
                    xTicks.forEach { valX ->
                        val ratio = (valX - xMin) / (xMax - xMin)
                        val x = ratio * plotWidth
                        drawLine(
                            color = gridConfig.gridColor,
                            start = Offset(x, topPaddingPx),
                            end = Offset(x, bottomY),
                            strokeWidth = gridConfig.strokeWidth.toPx(),
                            pathEffect = gridConfig.verticalPathEffect
                        )
                    }
                }

                if (axisConfig.showXAxisLabels && xTicks.isNotEmpty()) {
                    xTicks.forEach { valX ->
                        val ratio = (valX - xMin) / (xMax - xMin)
                        val x = ratio * plotWidth
                        val label = axisConfig.xLabelFormatter(valX.toString())
                        val textLayoutResult = textMeasurer.measure(
                            text = label,
                            style = TextStyle(color = axisConfig.labelColor, fontSize = axisConfig.labelSize)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                x = x - textLayoutResult.size.width / 2f,
                                y = bottomY + 8.dp.toPx()
                            )
                        )
                    }
                }

                val minRadPx = minBubbleRadius.toPx()
                val maxRadPx = maxBubbleRadius.toPx()

                fun sizeToRadius(sizeVal: Float): Float {
                    if (maxSize == minSize) return (minRadPx + maxRadPx) / 2f
                    val ratio = (sizeVal - minSize) / (maxSize - minSize)
                    return minRadPx + ratio * (maxRadPx - minRadPx)
                }

                val projectedBubbles = bubbles.map { bubble ->
                    val xRatio = (bubble.x - xMin) / (xMax - xMin)
                    val yRatio = (bubble.y - yMin) / (yMax - yMin)
                    val cx = xRatio * plotWidth
                    val cy = bottomY - yRatio * chartHeight
                    val baseRadius = sizeToRadius(bubble.size)
                    Offset(cx, cy) to baseRadius
                }

                if (touchOffset != null) {
                    val localTouch = touchOffset!!
                    var closestIdx = -1
                    var closestDist = Float.MAX_VALUE
                    projectedBubbles.forEachIndexed { idx, (center, baseRadius) ->
                        val dx = localTouch.x - center.x
                        val dy = localTouch.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val activeHitRadius = baseRadius + 12.dp.toPx()
                        if (dist <= activeHitRadius && dist < closestDist) {
                            closestDist = dist
                            closestIdx = idx
                        }
                    }
                    selectedIndex = closestIdx
                }

                projectedBubbles.forEachIndexed { idx, (center, baseRadius) ->
                    val isSelected = idx == selectedIndex
                    val bubble = bubbles[idx]
                    val entryScale = progress
                    if (entryScale <= 0f) return@forEachIndexed

                    val finalRadius = baseRadius * entryScale * (if (isSelected) selectScale.value else 1f)

                    if (isSelected) {
                        drawCircle(
                            color = bubble.brush.obtainColorAtCenter().copy(alpha = 0.25f),
                            radius = finalRadius + 8.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = bubble.brush.obtainColorAtCenter().copy(alpha = 0.4f),
                            radius = finalRadius + 3.dp.toPx(),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    val activeBrush = if (bubble.gradientColors != null) {
                        Brush.radialGradient(
                            colors = bubble.gradientColors,
                            center = center,
                            radius = finalRadius
                        )
                    } else {
                        bubble.brush
                    }

                    drawCircle(
                        brush = activeBrush,
                        radius = finalRadius,
                        center = center,
                        alpha = if (isSelected) 0.95f else 0.75f
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = finalRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                if (selectedIndex != -1 && tooltipConfig.showTooltip) {
                    val bubble = bubbles[selectedIndex]
                    val (center, radius) = projectedBubbles[selectedIndex]

                    val tooltipLines = mutableListOf<String>()
                    if (bubble.label.isNotEmpty()) {
                        tooltipLines.add(bubble.label)
                    }
                    tooltipLines.add("X: ${ChartUtils.format(bubble.x, 2)}")
                    tooltipLines.add("Y: ${ChartUtils.format(bubble.y, 2)}")
                    tooltipLines.add("Size: ${ChartUtils.format(bubble.size, 1)}")
                    if (bubble.meta.isNotEmpty()) {
                        tooltipLines.add(bubble.meta)
                    }

                    val tooltipPadding = tooltipConfig.padding.toPx()
                    var maxWidth = 0f
                    val lineLayouts = tooltipLines.map { txt ->
                        val result = textMeasurer.measure(
                            text = txt,
                            style = TextStyle(color = tooltipConfig.textColor, fontSize = tooltipConfig.textSize)
                        )
                        if (result.size.width > maxWidth) {
                            maxWidth = result.size.width.toFloat()
                        }
                        result
                    }

                    val totalTextHeight = lineLayouts.sumOf { it.size.height }.toFloat()
                    val tooltipWidth = maxWidth + tooltipPadding * 2f
                    val tooltipHeight = totalTextHeight + tooltipPadding * 2f

                    val tooltipX = (center.x - tooltipWidth / 2f).coerceIn(0f, plotWidth + rightPaddingPx - tooltipWidth)
                    val tooltipY = (center.y - radius - tooltipHeight - 12.dp.toPx()).coerceIn(topPaddingPx, bottomY - tooltipHeight)

                    drawRoundRect(
                        color = tooltipConfig.backgroundColor,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(tooltipConfig.cornerRadius.toPx()),
                        style = Fill
                    )

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(tooltipConfig.cornerRadius.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    var currentY = tooltipY + tooltipPadding
                    lineLayouts.forEach { layout ->
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(tooltipX + tooltipPadding, currentY)
                        )
                        currentY += layout.size.height
                    }
                }
            }
        }
    }
}

private fun Brush.obtainColorAtCenter(): Color {
    return if (this is SolidColor) {
        this.value
    } else {
        Color(0xFFFFB300)
    }
}
