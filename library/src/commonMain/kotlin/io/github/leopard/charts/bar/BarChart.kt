package io.github.leopard.charts.bar

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
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
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
import io.github.leopard.charts.models.BarGroup
import io.github.leopard.charts.utils.ChartUtils

@Composable
fun BarChart(
    groups: List<BarGroup>,
    modifier: Modifier = Modifier,
    isStacked: Boolean = false,
    barCornerRadius: Dp = 4.dp,
    barSpacing: Dp = 4.dp,
    groupSpacing: Dp = 16.dp,
    gridConfig: GridConfig = GridConfig(),
    axisConfig: AxisConfig = AxisConfig(),
    tooltipConfig: TooltipConfig = TooltipConfig(),
    animationConfig: AnimationConfig = AnimationConfig(),
    scrollEnabled: Boolean = true
) {
    if (groups.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // Entry animation utilizing custom AnimationSpec
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(groups) {
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

    // Touch interactive state
    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Calculate global Y bounds
    val maxYVal = if (isStacked) {
        groups.maxOf { group -> group.bars.sumOf { it.value.toDouble() }.toFloat() }
    } else {
        groups.maxOf { group -> group.bars.maxOfOrNull { it.value } ?: 0f }
    }
    val yMin = 0f
    val yMax = if (maxYVal == 0f) 10f else maxYVal * 1.1f // Add 10% headroom

    val density = androidx.compose.ui.platform.LocalDensity.current
    val minGroupWidth = 60.dp // Minimum group spacing for scrolling

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val leftPadding = 60.dp
        val rightPadding = 20.dp
        val topPadding = 20.dp
        val bottomPadding = 40.dp

        val groupCount = groups.size

        // Calculate scrolling properties
        val availableWidth = (containerWidth - leftPadding - rightPadding).coerceAtLeast(0.dp)
        val calculatedPlotWidth = (minGroupWidth + groupSpacing) * groupCount.coerceAtLeast(1) - groupSpacing
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

        // 2. Draw Horizontal Grid Lines (drawn across the whole container width behind scroll content)
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

        // 3. Horizontal Scroll Container (Bars, Category Labels, Tooltips, X-axis)
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
                    .pointerInput(groups, isScrollable) {
                        if (isScrollable) {
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
                            Modifier.pointerInput(groups) {
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
                val plotWidth = requiredChartWidth.toPx()
                val activeProgress = animProgress.value

                // Draw X-Axis Line
                if (axisConfig.showXAxis) {
                    drawLine(
                        color = axisConfig.axisColor,
                        start = Offset(0f, bottomY),
                        end = Offset(plotWidth + rightPaddingPx, bottomY),
                        strokeWidth = axisConfig.axisThickness.toPx()
                    )
                }

                // Determine active group index from touch coordinates
                val plotSpacingPx = groupSpacing.toPx()
                val calculatedGroupWidth = (plotWidth - plotSpacingPx * (groupCount - 1)) / groupCount
                
                var activeGroupIndex = -1
                if (tooltipConfig.showTooltip && touchOffset != null) {
                    val localTouch = touchOffset!!
                    if (localTouch.x >= 0f && localTouch.x <= plotWidth) {
                        var currentX = 0f
                        for (i in 0 until groupCount) {
                            val nextX = currentX + calculatedGroupWidth
                            if (localTouch.x >= currentX && localTouch.x <= nextX + plotSpacingPx) {
                                activeGroupIndex = i
                                break
                            }
                            currentX = nextX + plotSpacingPx
                        }
                    }
                }

                // Draw active selection background glow
                if (activeGroupIndex != -1) {
                    val startX = activeGroupIndex * (calculatedGroupWidth + plotSpacingPx)
                    drawRoundRect(
                        color = Color.LightGray.copy(alpha = 0.15f),
                        topLeft = Offset(startX - 4.dp.toPx(), topPaddingPx),
                        size = Size(calculatedGroupWidth + 8.dp.toPx(), chartHeight),
                        cornerRadius = CornerRadius(barCornerRadius.toPx()),
                        style = Fill
                    )
                }

                // Draw Bars and X-Axis Labels
                var currentGroupX = 0f
                groups.forEachIndexed { groupIdx, group ->
                    val barsInGroup = group.bars
                    if (barsInGroup.isNotEmpty()) {
                        if (isStacked) {
                            val barW = calculatedGroupWidth
                            var currentYOffset = 0f
                            barsInGroup.forEach { bar ->
                                val barValue = bar.value * activeProgress
                                val barRatio = barValue / (yMax - yMin)
                                val barH = barRatio * chartHeight

                                val left = currentGroupX
                                val top = bottomY - currentYOffset - barH
                                val right = left + barW
                                val bottom = bottomY - currentYOffset

                                if (barH > 0) {
                                    val rect = Rect(left, top, right, bottom)
                                    val path = Path().apply {
                                        addRoundRect(
                                            RoundRect(
                                                rect = rect,
                                                topLeft = CornerRadius(barCornerRadius.toPx()),
                                                topRight = CornerRadius(barCornerRadius.toPx()),
                                                bottomLeft = CornerRadius(0f),
                                                bottomRight = CornerRadius(0f)
                                            )
                                        )
                                    }
                                    val activeBrush = if (bar.gradientColors != null) {
                                        Brush.verticalGradient(
                                            colors = bar.gradientColors,
                                            startY = top,
                                            endY = bottom
                                        )
                                    } else {
                                        bar.brush
                                    }
                                    drawPath(path = path, brush = activeBrush)
                                }
                                currentYOffset += barH
                            }
                        } else {
                            val totalBarSpacing = barSpacing.toPx() * (barsInGroup.size - 1)
                            val barW = (calculatedGroupWidth - totalBarSpacing) / barsInGroup.size

                            barsInGroup.forEachIndexed { barIdx, bar ->
                                val barValue = bar.value * activeProgress
                                val barRatio = barValue / (yMax - yMin)
                                val barH = barRatio * chartHeight

                                val left = currentGroupX + barIdx * (barW + barSpacing.toPx())
                                val top = bottomY - barH
                                val right = left + barW
                                val bottom = bottomY

                                if (barH > 0) {
                                    val rect = Rect(left, top, right, bottom)
                                    val path = Path().apply {
                                        addRoundRect(
                                            RoundRect(
                                                rect = rect,
                                                topLeft = CornerRadius(barCornerRadius.toPx()),
                                                topRight = CornerRadius(barCornerRadius.toPx()),
                                                bottomLeft = CornerRadius(0f),
                                                bottomRight = CornerRadius(0f)
                                            )
                                        )
                                    }
                                    val activeBrush = if (bar.gradientColors != null) {
                                        Brush.verticalGradient(
                                            colors = bar.gradientColors,
                                            startY = top,
                                            endY = bottom
                                        )
                                    } else {
                                        bar.brush
                                    }
                                    drawPath(path = path, brush = activeBrush)
                                }
                            }
                        }
                    }

                    // Draw category labels
                    if (axisConfig.showXAxisLabels) {
                        val label = axisConfig.xLabelFormatter(group.groupLabel)
                        val textLayoutResult = textMeasurer.measure(
                            text = label,
                            style = TextStyle(color = axisConfig.labelColor, fontSize = axisConfig.labelSize)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                x = currentGroupX + calculatedGroupWidth / 2f - textLayoutResult.size.width / 2f,
                                y = bottomY + 8.dp.toPx()
                            )
                        )
                    }

                    currentGroupX += calculatedGroupWidth + plotSpacingPx
                }

                // Render Tooltip Box
                if (activeGroupIndex != -1) {
                    val group = groups[activeGroupIndex]
                    val localTouch = touchOffset!!

                    val tooltipLines = mutableListOf<String>()
                    tooltipLines.add(group.groupLabel)
                    group.bars.forEach { bar ->
                        tooltipLines.add("${bar.label}: ${axisConfig.yLabelFormatter(bar.value)}")
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
