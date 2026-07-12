package io.github.leopard.charts.line

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.leopard.charts.config.AnimationConfig
import io.github.leopard.charts.config.AxisConfig
import io.github.leopard.charts.config.GridConfig
import io.github.leopard.charts.config.TooltipConfig
import io.github.leopard.charts.models.LineSeries
import io.github.leopard.charts.models.PointData
import io.github.leopard.charts.models.PointShape
import io.github.leopard.charts.utils.ChartUtils
import kotlin.math.abs

@Composable
fun LineChart(
    series: List<LineSeries>,
    modifier: Modifier = Modifier,
    gridConfig: GridConfig = GridConfig(),
    axisConfig: AxisConfig = AxisConfig(),
    tooltipConfig: TooltipConfig = TooltipConfig(),
    animationConfig: AnimationConfig = AnimationConfig(),
    scrollEnabled: Boolean = true
) {
    if (series.isEmpty() || series.all { it.points.isEmpty() }) {
        Box(modifier = modifier)
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // Animation progress using custom spec
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

    // Interactive state
    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Find global min and max
    val allPoints = series.flatMap { it.points }
    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minYVal = allPoints.minOf { it.y }
    val maxYVal = allPoints.maxOf { it.y }

    // Add some padding to Y axis
    val yRange = maxYVal - minYVal
    val pad = if (yRange == 0f) 10f else yRange * 0.1f
    val yMin = minYVal - pad
    val yMax = maxYVal + pad

    val density = androidx.compose.ui.platform.LocalDensity.current
    val minPointWidth = 55.dp // Minimum spacing between points for horizontal scroll

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val leftPadding = 60.dp
        val rightPadding = 20.dp
        val topPadding = 20.dp
        val bottomPadding = 40.dp

        val samplePoints = series.firstOrNull()?.points ?: emptyList()
        val pointsCount = samplePoints.size

        // Calculate required scroll width for the plot area
        // Available width for plotting
        val availableWidth = (containerWidth - leftPadding - rightPadding).coerceAtLeast(0.dp)
        val calculatedPlotWidth = minPointWidth * (pointsCount - 1).coerceAtLeast(1)
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

        // 1. Draw Sticky Y-Axis (Fixed on the left)
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftPadding)
        ) {
            val yTicks = ChartUtils.calculateNiceTicks(yMin, yMax, gridConfig.horizontalLinesCount)
            
            // Draw Y-Axis Labels
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

            // Draw Y-Axis Line
            if (axisConfig.showYAxis) {
                drawLine(
                    color = axisConfig.axisColor,
                    start = Offset(leftPaddingPx, topPaddingPx),
                    end = Offset(leftPaddingPx, bottomY),
                    strokeWidth = axisConfig.axisThickness.toPx()
                )
            }
        }

        // 2. Draw Horizontal Grid Lines (drawn across the whole container width but behind scroll content)
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

        // 3. Horizontal Scroll Container (Plots, Markers, Tooltips, X-axis)
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
                    .pointerInput(series, isScrollable) {
                        if (isScrollable) {
                            // If scrollable, use tap gestures to avoid interfering with horizontal swipes
                            detectTapGestures(
                                onPress = { offset ->
                                    touchOffset = offset
                                    tryAwaitRelease()
                                    touchOffset = null
                                },
                                onTap = { offset ->
                                    touchOffset = offset
                                }
                            )
                        } else {
                            // Traditional tap and drag tracking if scroll is locked
                            detectTapGestures(
                                onPress = { offset ->
                                    touchOffset = offset
                                    tryAwaitRelease()
                                    touchOffset = null
                                },
                                onTap = { offset ->
                                    touchOffset = offset
                                }
                            )
                        }
                    }
                    .then(
                        if (!isScrollable) {
                            Modifier.pointerInput(series) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        touchOffset = offset
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        touchOffset = null
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        touchOffset = null
                                    },
                                    onDrag = { change, _ ->
                                        touchOffset = change.position
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                // local plot coordinates starting from X = 0f
                val plotWidth = requiredChartWidth.toPx()
                val activeProgress = animProgress.value
                val renderedLinesPoints = mutableListOf<List<Offset>>()

                // Draw X-Axis Ticks (Vertical grid lines) & X Labels
                val xTicksCount = samplePoints.size
                val xIndices = if (xTicksCount > 1) {
                    val step = maxOf(1, xTicksCount / gridConfig.verticalLinesCount)
                    (0 until xTicksCount step step).toMutableList().apply {
                        if (last() != xTicksCount - 1) add(xTicksCount - 1)
                    }
                } else {
                    samplePoints.indices.toList()
                }

                // Draw Vertical Grid Lines
                if (gridConfig.showVerticalLines && xTicksCount > 1) {
                    xIndices.forEach { index ->
                        val p = samplePoints[index]
                        val ratio = (p.x - minX) / (maxX - minX)
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

                // Draw X-Axis Labels
                if (axisConfig.showXAxisLabels && xTicksCount > 0) {
                    xIndices.forEach { index ->
                        val p = samplePoints[index]
                        val ratio = (p.x - minX) / (maxX - minX)
                        val x = ratio * plotWidth
                        val label = axisConfig.xLabelFormatter(p.xLabel)
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

                // Draw X-Axis Base Line
                if (axisConfig.showXAxis) {
                    drawLine(
                        color = axisConfig.axisColor,
                        start = Offset(0f, bottomY),
                        end = Offset(plotWidth + rightPaddingPx, bottomY),
                        strokeWidth = axisConfig.axisThickness.toPx()
                    )
                }

                // Draw Plots (Lines & Areas)
                series.forEach { line ->
                    val points = line.points
                    if (points.isEmpty()) return@forEach

                    val screenPoints = points.map { p ->
                        val xRatio = (p.x - minX) / (maxX - minX)
                        val yRatio = (p.y - yMin) / (yMax - yMin)
                        val animatedYRatio = if (animationConfig.lineAnimationType == io.github.leopard.charts.config.LineAnimationType.Grow) {
                            yRatio * activeProgress
                        } else {
                            yRatio
                        }
                        Offset(
                            x = xRatio * plotWidth,
                            y = bottomY - animatedYRatio * chartHeight
                        )
                    }
                    renderedLinesPoints.add(screenPoints)

                    val fullPath = Path()
                    if (line.isSmooth && screenPoints.size > 2) {
                        fullPath.moveTo(screenPoints[0].x, screenPoints[0].y)
                        for (i in 0 until screenPoints.size - 1) {
                            val p0 = screenPoints[i]
                            val p1 = screenPoints[i + 1]
                            val controlX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlY2 = p1.y
                            fullPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }
                    } else {
                        fullPath.moveTo(screenPoints[0].x, screenPoints[0].y)
                        for (i in 1 until screenPoints.size) {
                            fullPath.lineTo(screenPoints[i].x, screenPoints[i].y)
                        }
                    }

                    val drawPath: Path
                    var endXLimit = Float.MAX_VALUE

                    if (animationConfig.lineAnimationType == io.github.leopard.charts.config.LineAnimationType.Draw && activeProgress < 1f) {
                        val measure = androidx.compose.ui.graphics.PathMeasure()
                        measure.setPath(fullPath, false)
                        val length = measure.length
                        val drawLength = length * activeProgress
                        
                        val partialPath = Path()
                        measure.getSegment(0f, drawLength, partialPath, true)
                        drawPath = partialPath

                        val endPos = measure.getPosition(drawLength)
                        endXLimit = endPos.x
                    } else {
                        drawPath = fullPath
                    }

                    // Area Fill
                    if (line.fillGradientColors != null && line.fillGradientColors.isNotEmpty()) {
                        val areaPath = Path()
                        if (animationConfig.lineAnimationType == io.github.leopard.charts.config.LineAnimationType.Draw && activeProgress < 1f) {
                            val measure = androidx.compose.ui.graphics.PathMeasure()
                            measure.setPath(fullPath, false)
                            val length = measure.length
                            val drawLength = length * activeProgress
                            
                            val partialPath = Path()
                            measure.getSegment(0f, drawLength, partialPath, true)
                            
                            val startPos = measure.getPosition(0f)
                            val endPos = measure.getPosition(drawLength)

                            areaPath.addPath(partialPath)
                            areaPath.lineTo(endPos.x, bottomY)
                            areaPath.lineTo(startPos.x, bottomY)
                            areaPath.close()
                        } else {
                            areaPath.addPath(fullPath)
                            areaPath.lineTo(screenPoints.last().x, bottomY)
                            areaPath.lineTo(screenPoints.first().x, bottomY)
                            areaPath.close()
                        }

                        val brush = Brush.verticalGradient(
                            colors = line.fillGradientColors,
                            startY = topPaddingPx,
                            endY = bottomY
                        )
                        drawPath(
                            path = areaPath,
                            brush = brush,
                            style = Fill
                        )
                    }

                    // Draw Stroke
                    drawPath(
                        path = drawPath,
                        color = line.color,
                        style = Stroke(
                            width = line.strokeWidth.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = line.pathEffect
                        )
                    )

                    // Draw Point Markers
                    if (line.showPoints) {
                        val radius = line.pointRadius.dp.toPx()
                        val innerRadius = (radius - 1.5f).coerceAtLeast(1f)

                        screenPoints.forEach { pointOffset ->
                            val shouldShow = when (animationConfig.lineAnimationType) {
                                io.github.leopard.charts.config.LineAnimationType.Grow -> true
                                io.github.leopard.charts.config.LineAnimationType.Draw -> pointOffset.x <= endXLimit
                            }
                            if (shouldShow) {
                                when (line.pointShape) {
                                    PointShape.Circle -> {
                                        drawCircle(
                                            color = Color.White,
                                            radius = radius,
                                            center = pointOffset
                                        )
                                        drawCircle(
                                            color = line.color,
                                            radius = innerRadius,
                                            center = pointOffset
                                        )
                                    }
                                    PointShape.Square -> {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(pointOffset.x - radius, pointOffset.y - radius),
                                            size = Size(radius * 2f, radius * 2f)
                                        )
                                        drawRect(
                                            color = line.color,
                                            topLeft = Offset(pointOffset.x - innerRadius, pointOffset.y - innerRadius),
                                            size = Size(innerRadius * 2f, innerRadius * 2f)
                                        )
                                    }
                                    PointShape.Triangle -> {
                                        val trianglePath = Path().apply {
                                            moveTo(pointOffset.x, pointOffset.y - radius)
                                            lineTo(pointOffset.x + radius, pointOffset.y + radius)
                                            lineTo(pointOffset.x - radius, pointOffset.y + radius)
                                            close()
                                        }
                                        drawPath(path = trianglePath, color = Color.White)
                                        val innerTrianglePath = Path().apply {
                                            moveTo(pointOffset.x, pointOffset.y - innerRadius)
                                            lineTo(pointOffset.x + innerRadius, pointOffset.y + innerRadius)
                                            lineTo(pointOffset.x - innerRadius, pointOffset.y + innerRadius)
                                            close()
                                        }
                                        drawPath(path = innerTrianglePath, color = line.color)
                                    }
                                    PointShape.None -> { /* Draw nothing */ }
                                }
                            }
                        }
                    }
                }

                // Interactive Tooltips
                if (tooltipConfig.showTooltip && touchOffset != null) {
                    val localTouch = touchOffset!!
                    if (localTouch.x >= 0f && localTouch.x <= plotWidth) {
                        var closestIndex = -1
                        var minDistance = Float.MAX_VALUE
                        samplePoints.forEachIndexed { index, p ->
                            val ratio = (p.x - minX) / (maxX - minX)
                            val xPos = ratio * plotWidth
                            val dist = abs(localTouch.x - xPos)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestIndex = index
                            }
                        }

                        if (closestIndex != -1) {
                            val pX = samplePoints[closestIndex]
                            val xRatio = (pX.x - minX) / (maxX - minX)
                            val xPos = xRatio * plotWidth

                            if (tooltipConfig.guideLineColor != null) {
                                drawLine(
                                    color = tooltipConfig.guideLineColor,
                                    start = Offset(xPos, topPaddingPx),
                                    end = Offset(xPos, bottomY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = tooltipConfig.guideLinePathEffect
                                )
                            }

                            val tooltipLines = mutableListOf<String>()
                            tooltipLines.add(axisConfig.xLabelFormatter(pX.xLabel))

                            series.forEachIndexed { seriesIdx, line ->
                                val screenPoints = renderedLinesPoints.getOrNull(seriesIdx)
                                val pY = line.points.getOrNull(closestIndex)
                                if (screenPoints != null && pY != null) {
                                    val pointOffset = screenPoints[closestIndex]
                                    drawCircle(
                                        color = line.color.copy(alpha = 0.3f),
                                        radius = (line.pointRadius + 4f).dp.toPx(),
                                        center = pointOffset
                                    )
                                    drawCircle(
                                        color = line.color,
                                        radius = (line.pointRadius + 1f).dp.toPx(),
                                        center = pointOffset
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = (line.pointRadius - 1f).dp.toPx(),
                                        center = pointOffset
                                    )

                                    tooltipLines.add("${line.name}: ${axisConfig.yLabelFormatter(pY.y)}")
                                }
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

                            var tooltipX = localTouch.x + 15.dp.toPx()
                            if (tooltipX + tooltipWidth > plotWidth + rightPaddingPx) {
                                tooltipX = localTouch.x - tooltipWidth - 15.dp.toPx()
                            }
                            var tooltipY = localTouch.y - tooltipHeight / 2f
                            tooltipY = tooltipY.coerceIn(topPaddingPx, bottomY - tooltipHeight)

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
    }
}
