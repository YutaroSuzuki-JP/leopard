package io.github.leopard.charts.candlestick

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
import io.github.leopard.charts.models.CandleData
import io.github.leopard.charts.utils.ChartUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<CandleData>,
    modifier: Modifier = Modifier,
    positiveBrush: Brush = SolidColor(Color(0xFF26A69A)), // Bullish (Green)
    negativeBrush: Brush = SolidColor(Color(0xFFEF5350)), // Bearish (Red)
    wickThickness: Dp = 1.5.dp,
    candleWidthRatio: Float = 0.7f,
    gridConfig: GridConfig = GridConfig(),
    axisConfig: AxisConfig = AxisConfig(),
    tooltipConfig: TooltipConfig = TooltipConfig(),
    animationConfig: AnimationConfig = AnimationConfig(),
    scrollEnabled: Boolean = true,
    positiveGradientColors: List<Color>? = null,
    negativeGradientColors: List<Color>? = null
) {
    if (candles.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // 1. Entry animation progress
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(candles) {
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

    // 2. Interactive highlight state
    var selectedIndex by remember { mutableStateOf(-1) }
    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // 3. Compute Stock Value Bounds
    val allHighs = candles.map { it.high }
    val allLows = candles.map { it.low }

    val minPrice = allLows.minOrNull() ?: 0f
    val maxPrice = allHighs.maxOrNull() ?: 100f
    val priceRange = maxPrice - minPrice

    // Dynamic padding on Y Axis to prevent candles touching top/bottom limits
    val yPad = if (priceRange == 0f) 10f else priceRange * 0.1f
    val yMin = max(0f, minPrice - yPad)
    val yMax = maxPrice + yPad

    val density = androidx.compose.ui.platform.LocalDensity.current
    val minCandleWidth = 35.dp // Minimum column segment width for scroll check

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val leftPadding = 60.dp
        val rightPadding = 20.dp
        val topPadding = 20.dp
        val bottomPadding = 40.dp

        val candleCount = candles.size

        // Calculate scrolling properties
        val availableWidth = (containerWidth - leftPadding - rightPadding).coerceAtLeast(0.dp)
        val calculatedPlotWidth = minCandleWidth * candleCount.coerceAtLeast(1)
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

        // 3. Horizontal Scroll Container (Candlesticks, tooltips, axis labels)
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
                    .pointerInput(candles, isScrollable) {
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
                            Modifier.pointerInput(candles) {
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

                // Candle dimensions mapping
                val columnWidth = plotWidth / candleCount
                val candleWidth = columnWidth * candleWidthRatio
                val halfCandleW = candleWidth / 2f

                // Resolve selected index
                if (touchOffset != null) {
                    val localTouch = touchOffset!!
                    if (localTouch.x >= 0f && localTouch.x <= plotWidth) {
                        val index = (localTouch.x / columnWidth).toInt().coerceIn(0, candleCount - 1)
                        selectedIndex = index
                    }
                }

                // Draw active selection indicator block
                if (selectedIndex != -1) {
                    val hx = selectedIndex * columnWidth
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.08f),
                        topLeft = Offset(hx, topPaddingPx),
                        size = Size(columnWidth, chartHeight)
                    )
                }

                // Draw stock columns (Candlestick + Wick)
                candles.forEachIndexed { idx, candle ->
                    val isBullish = candle.close >= candle.open
                    val brush = if (isBullish) positiveBrush else negativeBrush
                    val color = brush.obtainColorAtCenter()

                    val cx = idx * columnWidth + columnWidth / 2f

                    val highVal = (candle.high - yMin) * activeProgress + yMin
                    val lowVal = (candle.low - yMin) * activeProgress + yMin
                    val openVal = (candle.open - yMin) * activeProgress + yMin
                    val closeVal = (candle.close - yMin) * activeProgress + yMin

                    val wickTopY = bottomY - ((highVal - yMin) / (yMax - yMin)) * chartHeight
                    val wickBottomY = bottomY - ((lowVal - yMin) / (yMax - yMin)) * chartHeight

                    val openY = bottomY - ((openVal - yMin) / (yMax - yMin)) * chartHeight
                    val closeY = bottomY - ((closeVal - yMin) / (yMax - yMin)) * chartHeight

                    val bodyTopY = min(openY, closeY)
                    val bodyBottomY = max(openY, closeY)
                    val bodyHeight = max(1f, abs(openY - closeY))

                    // Draw wicks
                    drawLine(
                        color = color,
                        start = Offset(cx, wickTopY),
                        end = Offset(cx, wickBottomY),
                        strokeWidth = wickThickness.toPx()
                    )

                    val activeBrush = if (isBullish) {
                        if (positiveGradientColors != null) {
                            Brush.verticalGradient(
                                colors = positiveGradientColors,
                                startY = bodyTopY,
                                endY = bodyBottomY
                            )
                        } else {
                            brush
                        }
                    } else {
                        if (negativeGradientColors != null) {
                            Brush.verticalGradient(
                                colors = negativeGradientColors,
                                startY = bodyTopY,
                                endY = bodyBottomY
                            )
                        } else {
                            brush
                        }
                    }

                    // Draw body
                    drawRect(
                        brush = activeBrush,
                        topLeft = Offset(cx - halfCandleW, bodyTopY),
                        size = Size(candleWidth, bodyHeight),
                        style = Fill
                    )

                    // Draw axis label
                    if (axisConfig.showXAxisLabels) {
                        val showLabel = candleCount < 12 || idx % (candleCount / 5).coerceAtLeast(1) == 0 || idx == candleCount - 1
                        if (showLabel) {
                            val label = axisConfig.xLabelFormatter(candle.label)
                            val textLayoutResult = textMeasurer.measure(
                                text = label,
                                style = TextStyle(color = axisConfig.labelColor, fontSize = axisConfig.labelSize)
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    x = cx - textLayoutResult.size.width / 2f,
                                    y = bottomY + 8.dp.toPx()
                                )
                            )
                        }
                    }
                }

                // Render Stock Tooltip
                if (selectedIndex != -1 && tooltipConfig.showTooltip) {
                    val candle = candles[selectedIndex]
                    val cx = selectedIndex * columnWidth + columnWidth / 2f

                    if (tooltipConfig.guideLineColor != null) {
                        drawLine(
                            color = tooltipConfig.guideLineColor,
                            start = Offset(cx, topPaddingPx),
                            end = Offset(cx, bottomY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = tooltipConfig.guideLinePathEffect
                        )
                    }

                    val priceChange = candle.close - candle.open
                    val percentChange = (priceChange / candle.open) * 100f
                    val sign = if (priceChange >= 0f) "+" else ""

                    val tooltipLines = listOf(
                        "Date: ${candle.label}",
                        "Open: ${axisConfig.yLabelFormatter(candle.open)}",
                        "High: ${axisConfig.yLabelFormatter(candle.high)}",
                        "Low: ${axisConfig.yLabelFormatter(candle.low)}",
                        "Close: ${axisConfig.yLabelFormatter(candle.close)}",
                        "Change: $sign${axisConfig.yLabelFormatter(priceChange)} ($sign${ChartUtils.format(percentChange, 2)}%)"
                    )

                    val tooltipPadding = tooltipConfig.padding.toPx()
                    var maxWidth = 0f
                    val lineLayouts = tooltipLines.mapIndexed { idx, txt ->
                        val color = if (idx == 5) {
                            if (priceChange >= 0) positiveBrush.obtainColorAtCenter() else negativeBrush.obtainColorAtCenter()
                        } else {
                            tooltipConfig.textColor
                        }

                        val result = textMeasurer.measure(
                            text = txt,
                            style = TextStyle(color = color, fontSize = tooltipConfig.textSize)
                        )
                        if (result.size.width > maxWidth) {
                            maxWidth = result.size.width.toFloat()
                        }
                        result
                    }

                    val totalTextHeight = lineLayouts.sumOf { it.size.height }.toFloat()
                    val tooltipWidth = maxWidth + tooltipPadding * 2f
                    val tooltipHeight = totalTextHeight + tooltipPadding * 2f

                    var tooltipX = cx + 15.dp.toPx()
                    if (tooltipX + tooltipWidth > plotWidth + rightPaddingPx) {
                        tooltipX = cx - tooltipWidth - 15.dp.toPx()
                    }
                    val tooltipY = topPaddingPx + 10.dp.toPx()

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
        Color(0xFF26A69A)
    }
}
