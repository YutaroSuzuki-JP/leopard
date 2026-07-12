package io.github.leopard.demo

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.leopard.charts.bar.BarChart
import io.github.leopard.charts.bubble.BubbleChart
import io.github.leopard.charts.candlestick.CandlestickChart
import io.github.leopard.charts.utils.ChartUtils
import io.github.leopard.charts.config.AnimationConfig
import io.github.leopard.charts.config.AxisConfig
import io.github.leopard.charts.config.GridConfig
import io.github.leopard.charts.config.TooltipConfig
import io.github.leopard.charts.config.LineAnimationType
import io.github.leopard.charts.line.LineChart
import io.github.leopard.charts.models.*
import io.github.leopard.charts.pie.PieChart
import io.github.leopard.charts.radar.RadarChart

enum class ActiveTab(val label: String) {
    LINE("Line / Area Chart"),
    BAR("Bar Chart"),
    PIE("Pie / Donut Chart"),
    RADAR("Radar Chart"),
    BUBBLE("Bubble / Scatter Chart"),
    CANDLESTICK("Candlestick / Stock Chart")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFB300), // Leopard Amber Gold
            background = Color(0xFF121214), // Sleek deep space background
            surface = Color(0xFF1E1E22), // Premium glass-like surface
            onSurface = Color(0xFFE2E2E9)
        )
    ) {
        var activeTab by remember { mutableStateOf(ActiveTab.LINE) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isCompact = maxWidth < 768.dp

            if (isCompact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Mobile Top Bar Navigation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F11))
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = "🐆 Leopard",
                                style = TextStyle(
                                    color = Color(0xFFFFB300),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "KMP",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        ScrollableTabRow(
                            selectedTabIndex = activeTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFFFB300),
                            edgePadding = 16.dp,
                            divider = {}
                        ) {
                            ActiveTab.values().forEach { tab ->
                                Tab(
                                    selected = activeTab == tab,
                                    onClick = { activeTab = tab },
                                    text = { Text(tab.label, fontSize = 13.sp) }
                                )
                            }
                        }
                    }

                    // Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        when (activeTab) {
                            ActiveTab.LINE -> LineChartShowcase(isCompact = true)
                            ActiveTab.BAR -> BarChartShowcase(isCompact = true)
                            ActiveTab.PIE -> PieChartShowcase(isCompact = true)
                            ActiveTab.RADAR -> RadarChartShowcase(isCompact = true)
                            ActiveTab.BUBBLE -> BubbleChartShowcase(isCompact = true)
                            ActiveTab.CANDLESTICK -> CandlestickShowcase(isCompact = true)
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Sidebar - Navigation
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF0F0F11))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Brand Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Text(
                                text = "🐆 Leopard",
                                style = TextStyle(
                                    color = Color(0xFFFFB300),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "KMP",
                                    color = Color(0xFFFFB300),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Nav items
                        ActiveTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFFB300).copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .clickable { activeTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) Color(0xFFFFB300) else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "Built with Compose Canvas",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Divider
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.05f))
                    )

                    // Right Area - Content & Configuration Panel
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .padding(24.dp)
                        ) {
                            when (activeTab) {
                                ActiveTab.LINE -> LineChartShowcase(isCompact = false)
                                ActiveTab.BAR -> BarChartShowcase(isCompact = false)
                                ActiveTab.PIE -> PieChartShowcase(isCompact = false)
                                ActiveTab.RADAR -> RadarChartShowcase(isCompact = false)
                                ActiveTab.BUBBLE -> BubbleChartShowcase(isCompact = false)
                                ActiveTab.CANDLESTICK -> CandlestickShowcase(isCompact = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Showcase Sub-components with Customization Demonstrations
// ----------------------------------------------------------------------------

@Composable
fun LineChartShowcase(isCompact: Boolean) {
    var isSmooth by remember { mutableStateOf(true) }
    var showArea by remember { mutableStateOf(true) }
    var animate by remember { mutableStateOf(true) }
    var pointShape by remember { mutableStateOf(PointShape.Circle) }
    var isDashedLine by remember { mutableStateOf(false) }

    // Chart parameters state
    var scaleFactor by remember { mutableStateOf(1f) }
    var seriesCount by remember { mutableStateOf(1f) }
    var dataPointsCount by remember { mutableStateOf(7f) }
    var animationType by remember { mutableStateOf(LineAnimationType.Grow) }

    // Custom Animation & Background Settings
    var showBgGradient by remember { mutableStateOf(false) }
    var animSpecType by remember { mutableStateOf("Spring") } // "Spring", "Tween", "Linear"
    var animDuration by remember { mutableStateOf(1000f) }
    var scrollEnabled by remember { mutableStateOf(true) }

    // Grid Customization Settings
    var showHorizontalGrid by remember { mutableStateOf(true) }
    var showVerticalGrid by remember { mutableStateOf(true) }
    var gridStrokeWidth by remember { mutableStateOf(1f) }
    var gridStyleDashed by remember { mutableStateOf(true) }
    var horizontalGridCount by remember { mutableStateOf(5f) }

    val animationSpec = remember(animSpecType, animDuration) {
        when (animSpecType) {
            "Spring" -> spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            "Tween" -> tween<Float>(durationMillis = animDuration.toInt(), easing = FastOutSlowInEasing)
            else -> tween<Float>(durationMillis = animDuration.toInt(), easing = LinearEasing)
        }
    }

    val bgGradient = if (showBgGradient) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1B2F), Color(0xFF0F101E))
        )
    } else {
        null
    }

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th", "21st", "22nd", "23rd", "24th", "25th")
    val pointsCount = dataPointsCount.toInt()
    
    val points1 = List(pointsCount) { i ->
        val value = (15f + i * 4f + kotlin.math.sin(i * 0.8f) * 12f) * scaleFactor
        PointData(i.toFloat(), value, monthNames.getOrElse(i) { "P$i" })
    }
    
    val points2 = List(pointsCount) { i ->
        val value = (30f + kotlin.math.cos(i * 0.6f) * 15f + i * 2f) * scaleFactor
        PointData(i.toFloat(), value, monthNames.getOrElse(i) { "P$i" })
    }
    
    val points3 = List(pointsCount) { i ->
        val value = (10f + kotlin.math.sin(i * 1.2f) * 8f + i * i * 0.1f) * scaleFactor
        PointData(i.toFloat(), value, monthNames.getOrElse(i) { "P$i" })
    }

    val series = buildList {
        add(
            LineSeries(
                name = "Sales",
                points = points1,
                color = Color(0xFFFFB300),
                isSmooth = isSmooth,
                pointShape = pointShape,
                pathEffect = if (isDashedLine) PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) else null,
                fillGradientColors = if (showArea) listOf(
                    Color(0xFFFFB300).copy(alpha = 0.4f),
                    Color(0xFFFFB300).copy(alpha = 0.0f)
                ) else null
            )
        )
        if (seriesCount >= 2f) {
            add(
                LineSeries(
                    name = "Marketing",
                    points = points2,
                    color = Color(0xFF26A69A),
                    isSmooth = isSmooth,
                    pointShape = pointShape,
                    pathEffect = if (isDashedLine) PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) else null,
                    fillGradientColors = if (showArea) listOf(
                        Color(0xFF26A69A).copy(alpha = 0.4f),
                        Color(0xFF26A69A).copy(alpha = 0.0f)
                    ) else null
                )
            )
        }
        if (seriesCount >= 3f) {
            add(
                LineSeries(
                    name = "Operations",
                    points = points3,
                    color = Color(0xFFEF5350),
                    isSmooth = isSmooth,
                    pointShape = pointShape,
                    pathEffect = if (isDashedLine) PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) else null,
                    fillGradientColors = if (showArea) listOf(
                        Color(0xFFEF5350).copy(alpha = 0.4f),
                        Color(0xFFEF5350).copy(alpha = 0.0f)
                    ) else null
                )
            )
        }
    }

    ResponsiveLayout(
        isCompact = isCompact,
        title = "Line / Area Chart",
        description = "Fully customizable line and area charts supporting multiple dynamic series and high density data points.",
        chartBackgroundBrush = bgGradient,
        chartContent = {
            LineChart(
                series = series,
                modifier = Modifier.fillMaxSize(),
                gridConfig = GridConfig(
                    showHorizontalLines = showHorizontalGrid,
                    showVerticalLines = showVerticalGrid,
                    strokeWidth = gridStrokeWidth.dp,
                    horizontalLinesCount = horizontalGridCount.toInt(),
                    horizontalPathEffect = if (gridStyleDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null,
                    verticalPathEffect = if (gridStyleDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                ),
                axisConfig = AxisConfig(
                    yLabelFormatter = { "$${ChartUtils.format(it, 0)}" }
                ),
                animationConfig = AnimationConfig(
                    animateEntry = animate,
                    animationSpec = animationSpec,
                    lineAnimationType = animationType
                ),
                scrollEnabled = scrollEnabled
            )
        },
        controlContent = {
            Text(
                text = "Customization",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Gradient Background Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Backdrop / Card Gradient", color = Color.Gray)
                Switch(checked = showBgGradient, onCheckedChange = { showBgGradient = it })
            }

            // Horizontal Scroll Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horizontal Scroll", color = Color.Gray)
                Switch(checked = scrollEnabled, onCheckedChange = { scrollEnabled = it })
            }

            // Adjust series count
            Column {
                Text("Series Count: ${seriesCount.toInt()}", color = Color.Gray)
                Slider(
                    value = seriesCount,
                    onValueChange = { seriesCount = it },
                    valueRange = 1f..3f,
                    steps = 1
                )
            }

            // Adjust data points count
            Column {
                Text("Data Points: ${dataPointsCount.toInt()}", color = Color.Gray)
                Slider(
                    value = dataPointsCount,
                    onValueChange = { dataPointsCount = it },
                    valueRange = 5f..25f,
                    steps = 19
                )
            }

            // Animation Spec Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Animation Style", color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(LineAnimationType.Grow, LineAnimationType.Draw).forEach { type ->
                        val isSelected = animationType == type
                        val label = if (type == LineAnimationType.Grow) "Grow (Vertical)" else "Draw (Path)"
                        Button(
                            onClick = { animationType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF2C2C32),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Animation Curve Spec
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Animation Physics / Curve", color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Spring", "Tween", "Linear").forEach { type ->
                        val isSelected = animSpecType == type
                        Button(
                            onClick = { animSpecType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF2C2C32),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(type, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (animSpecType != "Spring") {
                Column {
                    Text("Duration: ${animDuration.toInt()} ms", color = Color.Gray)
                    Slider(
                        value = animDuration,
                        onValueChange = { animDuration = it },
                        valueRange = 300f..3000f
                    )
                }
            }

            // Toggle Smooth Bezier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smooth Bezier", color = Color.Gray)
                Switch(checked = isSmooth, onCheckedChange = { isSmooth = it })
            }

            // Toggle Area Gradient Fill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Area Fill Under Line", color = Color.Gray)
                Switch(checked = showArea, onCheckedChange = { showArea = it })
            }

            // Toggle Dashed Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dashed Stroke Line", color = Color.Gray)
                Switch(checked = isDashedLine, onCheckedChange = { isDashedLine = it })
            }

            // Point Shape Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Point Shape", color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PointShape.values().forEach { shape ->
                        val isSelected = pointShape == shape
                        Button(
                            onClick = { pointShape = shape },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF2C2C32),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(shape.name, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Toggle Entry Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Animate Entry", color = Color.Gray)
                Switch(checked = animate, onCheckedChange = { animate = it })
            }

            // Adjust values slider
            Column {
                Text("Scale Data Value: ${ChartUtils.format(scaleFactor, 1)}x", color = Color.Gray)
                Slider(
                    value = scaleFactor,
                    onValueChange = { scaleFactor = it },
                    valueRange = 0.5f..2.0f
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Text("Grid Lines Layout", fontWeight = FontWeight.Bold, color = Color.White)

            // Horizontal Grid Lines Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horizontal Grid Lines", color = Color.Gray)
                Switch(checked = showHorizontalGrid, onCheckedChange = { showHorizontalGrid = it })
            }

            // Vertical Grid Lines Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vertical Grid Lines", color = Color.Gray)
                Switch(checked = showVerticalGrid, onCheckedChange = { showVerticalGrid = it })
            }

            // Dashed Grid Style Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dashed Grid Style", color = Color.Gray)
                Switch(checked = gridStyleDashed, onCheckedChange = { gridStyleDashed = it })
            }

            // Grid Line Count Slider
            Column {
                Text("Horizontal Lines Count: ${horizontalGridCount.toInt()}", color = Color.Gray)
                Slider(
                    value = horizontalGridCount,
                    onValueChange = { horizontalGridCount = it },
                    valueRange = 3f..10f,
                    steps = 5
                )
            }

            // Grid Stroke Width Slider
            Column {
                Text("Grid Lines Thickness: ${ChartUtils.format(gridStrokeWidth, 1)} dp", color = Color.Gray)
                Slider(
                    value = gridStrokeWidth,
                    onValueChange = { gridStrokeWidth = it },
                    valueRange = 0.5f..4.0f
                )
            }
        },
        codeSnippet = """
            // Custom AnimationSpec & Background setup
            val config = AnimationConfig(
                animationSpec = tween(1200),
                lineAnimationType = LineAnimationType.Draw
            )
            LineChart(
                series = series,
                animationConfig = config,
                modifier = Modifier.background(bgGradient)
            )
        """.trimIndent()
    )
}

@Composable
fun BarChartShowcase(isCompact: Boolean) {
    var isStacked by remember { mutableStateOf(false) }
    var cornerRadius by remember { mutableStateOf(6f) }
    var animate by remember { mutableStateOf(true) }
    var showGradients by remember { mutableStateOf(true) }
    var scrollEnabled by remember { mutableStateOf(true) }

    // Simulation states
    var groupCount by remember { mutableStateOf(4f) }
    var barsPerGroup by remember { mutableStateOf(2f) }

    // Custom Animation & Background Settings
    var showBgGradient by remember { mutableStateOf(false) }
    var animSpecType by remember { mutableStateOf("Spring") } // "Spring", "Tween", "Linear"
    var animDuration by remember { mutableStateOf(1000f) }

    val animationSpec = remember(animSpecType, animDuration) {
        when (animSpecType) {
            "Spring" -> spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            "Tween" -> tween<Float>(durationMillis = animDuration.toInt(), easing = FastOutSlowInEasing)
            else -> tween<Float>(durationMillis = animDuration.toInt(), easing = LinearEasing)
        }
    }

    val bgGradient = if (showBgGradient) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF131B2F), Color(0xFF0C101D))
        )
    } else {
        null
    }

    // Premium gradient combinations
    val revColors = if (showGradients) listOf(Color(0xFF42A5F5), Color(0xFF1E88E5)) else null
    val profitColors = if (showGradients) listOf(Color(0xFF66BB6A), Color(0xFF43A047)) else null
    val expenseColors = if (showGradients) listOf(Color(0xFFFF8A65), Color(0xFFFF5722)) else null
    val taxColors = if (showGradients) listOf(Color(0xFFAB47BC), Color(0xFF8E24AA)) else null

    val revBrush = if (showGradients) Brush.verticalGradient(revColors!!) else SolidColor(Color(0xFF42A5F5))
    val profitBrush = if (showGradients) Brush.verticalGradient(profitColors!!) else SolidColor(Color(0xFF66BB6A))
    val expenseBrush = if (showGradients) Brush.verticalGradient(expenseColors!!) else SolidColor(Color(0xFFFF8A65))
    val taxBrush = if (showGradients) Brush.verticalGradient(taxColors!!) else SolidColor(Color(0xFFAB47BC))

    val labels = listOf("Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7", "Q8", "Q9", "Q10", "Q11", "Q12")
    val gCount = groupCount.toInt()
    val seriesNum = barsPerGroup.toInt()

    val barGroups = List(gCount) { i ->
        val groupLabel = labels.getOrElse(i) { "G${i + 1}" }
        val bars = buildList {
            add(BarData("Revenue", (100f + i * 20f + kotlin.math.sin(i * 1.0f) * 40f).coerceAtLeast(10f), revBrush, revColors))
            if (seriesNum >= 2) {
                add(BarData("Profit", (60f + i * 12f + kotlin.math.cos(i * 0.8f) * 25f).coerceAtLeast(5f), profitBrush, profitColors))
            }
            if (seriesNum >= 3) {
                add(BarData("Expenses", (40f + i * 8f + kotlin.math.sin(i * 1.5f) * 15f).coerceAtLeast(5f), expenseBrush, expenseColors))
            }
            if (seriesNum >= 4) {
                add(BarData("Tax", (15f + i * 3f + kotlin.math.cos(i * 2.0f) * 8f).coerceAtLeast(2f), taxBrush, taxColors))
            }
        }
        BarGroup(groupLabel = groupLabel, bars = bars)
    }

    ResponsiveLayout(
        isCompact = isCompact,
        title = "Bar Chart",
        description = "Clustered and stacked column charts supporting variable group sizes and customizable multiple series segments.",
        chartBackgroundBrush = bgGradient,
        chartContent = {
            BarChart(
                groups = barGroups,
                isStacked = isStacked,
                barCornerRadius = cornerRadius.dp,
                animationConfig = AnimationConfig(
                    animateEntry = animate,
                    animationSpec = animationSpec
                ),
                scrollEnabled = scrollEnabled
            )
        },
        controlContent = {
            Text(
                text = "Customization",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Gradient Background Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Backdrop / Card Gradient", color = Color.Gray)
                Switch(checked = showBgGradient, onCheckedChange = { showBgGradient = it })
            }

            // Horizontal Scroll Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horizontal Scroll", color = Color.Gray)
                Switch(checked = scrollEnabled, onCheckedChange = { scrollEnabled = it })
            }

            // Adjust group count
            Column {
                Text("Group Count: ${groupCount.toInt()}", color = Color.Gray)
                Slider(
                    value = groupCount,
                    onValueChange = { groupCount = it },
                    valueRange = 3f..12f,
                    steps = 8
                )
            }

            // Adjust bars per group
            Column {
                Text("Bars per Group: ${barsPerGroup.toInt()}", color = Color.Gray)
                Slider(
                    value = barsPerGroup,
                    onValueChange = { barsPerGroup = it },
                    valueRange = 1f..4f,
                    steps = 2
                )
            }

            // Animation Curve Spec Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Animation Physics / Curve", color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Spring", "Tween", "Linear").forEach { type ->
                        val isSelected = animSpecType == type
                        Button(
                            onClick = { animSpecType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF2C2C32),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(type, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (animSpecType != "Spring") {
                Column {
                    Text("Duration: ${animDuration.toInt()} ms", color = Color.Gray)
                    Slider(
                        value = animDuration,
                        onValueChange = { animDuration = it },
                        valueRange = 300f..3000f
                    )
                }
            }

            // Toggle Stacked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stacked Bars", color = Color.Gray)
                Switch(checked = isStacked, onCheckedChange = { isStacked = it })
            }

            // Toggle Gradients
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bar Gradient Fill", color = Color.Gray)
                Switch(checked = showGradients, onCheckedChange = { showGradients = it })
            }

            // Adjust corner radius
            Column {
                Text("Corner Radius: ${cornerRadius.toInt()} dp", color = Color.Gray)
                Slider(
                    value = cornerRadius,
                    onValueChange = { cornerRadius = it },
                    valueRange = 0f..16f
                )
            }

            // Toggle Entry Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Animate Entry", color = Color.Gray)
                Switch(checked = animate, onCheckedChange = { animate = it })
            }
        },
        codeSnippet = """
            // Custom AnimationSpec & Background setup
            val config = AnimationConfig(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            BarChart(
                groups = barGroups,
                animationConfig = config,
                modifier = Modifier.background(bgGradient)
            )
        """.trimIndent()
    )
}

@Composable
fun PieChartShowcase(isCompact: Boolean) {
    var innerRadiusRatio by remember { mutableStateOf(0.6f) }
    var animate by remember { mutableStateOf(true) }
    var sliceGap by remember { mutableStateOf(2.5f) }
    var showGradients by remember { mutableStateOf(true) }

    // Curated gradient palettes for slices
    val slices = if (showGradients) {
        listOf(
            PieSlice(30f, "Kotlin", Brush.horizontalGradient(listOf(Color(0xFF9575CD), Color(0xFF673AB7)))),
            PieSlice(25f, "Swift", Brush.horizontalGradient(listOf(Color(0xFFF06292), Color(0xFFE91E63)))),
            PieSlice(20f, "Java", Brush.horizontalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF03A9F4)))),
            PieSlice(15f, "TypeScript", Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))),
            PieSlice(10f, "Rust", Brush.horizontalGradient(listOf(Color(0xFFFF8A65), Color(0xFFFF5722))))
        )
    } else {
        listOf(
            PieSlice(30f, "Kotlin", Color(0xFF9575CD)),
            PieSlice(25f, "Swift", Color(0xFFF06292)),
            PieSlice(20f, "Java", Color(0xFF4FC3F7)),
            PieSlice(15f, "TypeScript", Color(0xFFFFD54F)),
            PieSlice(10f, "Rust", Color(0xFFFF8A65))
        )
    }

    ResponsiveLayout(
        isCompact = isCompact,
        title = "Pie & Donut Chart",
        description = "Segment gaps, gradient slice sweeps, and animated centers.",
        chartContent = {
            PieChart(
                slices = slices,
                innerRadiusRatio = innerRadiusRatio,
                sliceGapAngle = sliceGap,
                animationConfig = AnimationConfig(animateEntry = animate),
                legendConfig = TextStyle(color = Color.White)
            )
        },
        controlContent = {
            Text(
                text = "Customization",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Adjust slice gap slider
            Column {
                Text("Slice Spacing Gap: ${ChartUtils.format(sliceGap, 1)}°", color = Color.Gray)
                Slider(
                    value = sliceGap,
                    onValueChange = { sliceGap = it },
                    valueRange = 0.0f..10.0f
                )
            }

            // Adjust inner ratio slider
            Column {
                Text("Inner Radius Ratio: ${ChartUtils.format(innerRadiusRatio, 2)}", color = Color.Gray)
                Slider(
                    value = innerRadiusRatio,
                    onValueChange = { innerRadiusRatio = it },
                    valueRange = 0.0f..0.85f
                )
            }

            // Toggle Gradients
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gradient Brushes", color = Color.Gray)
                Switch(checked = showGradients, onCheckedChange = { showGradients = it })
            }

            // Toggle Entry Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Animate Entry", color = Color.Gray)
                Switch(checked = animate, onCheckedChange = { animate = it })
            }
        },
        codeSnippet = """
            PieChart(
                slices = slices,
                innerRadiusRatio = ${ChartUtils.format(innerRadiusRatio, 1)}f,
                sliceGapAngle = ${ChartUtils.format(sliceGap, 1)}f
            )
        """.trimIndent()
    )
}


@Composable
fun RadarChartShowcase(isCompact: Boolean) {
    var animate by remember { mutableStateOf(true) }
    var useSpringAnimation by remember { mutableStateOf(true) }

    val series = listOf(
        RadarSeries(
            name = "Player A",
            points = listOf(
                RadarPoint("Attack", 85f),
                RadarPoint("Defense", 70f),
                RadarPoint("Speed", 90f),
                RadarPoint("Stamina", 60f),
                RadarPoint("Dribble", 80f),
                RadarPoint("Pass", 75f)
            ),
            brush = SolidColor(Color(0xFFFFB300)),
            fillBrush = SolidColor(Color(0xFFFFB300).copy(alpha = 0.2f))
        ),
        RadarSeries(
            name = "Player B",
            points = listOf(
                RadarPoint("Attack", 60f),
                RadarPoint("Defense", 85f),
                RadarPoint("Speed", 75f),
                RadarPoint("Stamina", 90f),
                RadarPoint("Dribble", 65f),
                RadarPoint("Pass", 80f)
            ),
            brush = SolidColor(Color(0xFF26A69A)),
            fillBrush = SolidColor(Color(0xFF26A69A).copy(alpha = 0.2f))
        )
    )

    ResponsiveLayout(
        isCompact = isCompact,
        title = "Radar Chart",
        description = "Dynamic radar plots supporting physical springs and custom damping ratios.",
        chartContent = {
            RadarChart(
                series = series,
                gridConfig = GridConfig(
                    horizontalLinesCount = 5,
                    gridColor = Color.LightGray.copy(alpha = 0.3f),
                    horizontalPathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                ),
                animationConfig = AnimationConfig(
                    animateEntry = animate,
                    animationSpec = if (useSpringAnimation) {
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    } else {
                        tween(1000, easing = LinearEasing)
                    }
                )
            )
        },
        controlContent = {
            Text(
                text = "Customization",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Toggle Spring Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use Spring Physics", color = Color.Gray)
                Switch(checked = useSpringAnimation, onCheckedChange = { useSpringAnimation = it })
            }

            // Toggle Entry Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Animate Entry", color = Color.Gray)
                Switch(checked = animate, onCheckedChange = { animate = it })
            }
        },
        codeSnippet = """
            RadarChart(
                series = series,
                animationConfig = AnimationConfig(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            )
        """.trimIndent()
    )
}


@Composable
fun ResponsiveLayout(
    isCompact: Boolean,
    title: String,
    description: String,
    chartBackgroundBrush: Brush? = null,
    chartContent: @Composable BoxScope.() -> Unit,
    controlContent: @Composable ColumnScope.() -> Unit,
    codeSnippet: String
) {
    if (isCompact) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                style = TextStyle(color = Color.Gray, fontSize = 12.sp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = if (chartBackgroundBrush != null) Color.Transparent else Color(0xFF1E1E22)),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .then(
                        if (chartBackgroundBrush != null) {
                            Modifier.background(chartBackgroundBrush, RoundedCornerShape(12.dp))
                        } else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    content = chartContent
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = controlContent
                )
            }
            CodeSnippetBox(code = codeSnippet)
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = title,
                    style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = description,
                    style = TextStyle(color = Color.Gray, fontSize = 14.sp),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (chartBackgroundBrush != null) Color.Transparent else Color(0xFF1E1E22)),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp)
                        .then(
                            if (chartBackgroundBrush != null) {
                                Modifier.background(chartBackgroundBrush, RoundedCornerShape(12.dp))
                            } else Modifier
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        content = chartContent
                    )
                }
                CodeSnippetBox(code = codeSnippet)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    content = controlContent
                )
            }
        }
    }
}

@Composable
fun CodeSnippetBox(code: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0F)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Kotlin Code Snippet",
                color = Color(0xFFFFB300),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = code,
                color = Color(0xFFA7A7AF),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun BubbleChartShowcase(isCompact: Boolean) {
    var minRadius by remember { mutableStateOf(8f) }
    var maxRadius by remember { mutableStateOf(32f) }
    var animate by remember { mutableStateOf(true) }
    var scatterMode by remember { mutableStateOf(false) }
    var scrollEnabled by remember { mutableStateOf(true) }
    var showGradients by remember { mutableStateOf(true) }

    val rawBubbles = remember {
        listOf(
            BubbleData(x = 1.2f, y = 2.4f, size = 15f, label = "Alpha Sector", color = Color(0xFFFFB300), meta = "Priority High"),
            BubbleData(x = 3.5f, y = 1.8f, size = 42f, label = "Beta Outpost", color = Color(0xFF26A69A), meta = "Active Node"),
            BubbleData(x = 2.4f, y = 5.2f, size = 8f, label = "Gamma Probe", color = Color(0xFFEF5350), meta = "Low battery"),
            BubbleData(x = 5.8f, y = 4.1f, size = 55f, label = "Delta Core", color = Color(0xFFAB47BC), meta = "System Mainframe"),
            BubbleData(x = 4.0f, y = 6.3f, size = 23f, label = "Epsilon Array", color = Color(0xFF42A5F5), meta = "Sync complete"),
            BubbleData(x = 6.7f, y = 2.9f, size = 31f, label = "Zeta Hub", color = Color(0xFF9CCC65), meta = "Traffic: 120/m"),
            BubbleData(x = 7.5f, y = 5.8f, size = 12f, label = "Eta Station", color = Color(0xFFFFB300), meta = "Standby mode")
        )
    }

    val displayBubbles = remember(scatterMode, showGradients) {
        rawBubbles.map { bubble ->
            val finalSize = if (scatterMode) 20f else bubble.size
            val baseColor = bubble.brush.obtainColorAtCenter()
            val gradientColors = if (showGradients) listOf(baseColor, baseColor.copy(alpha = 0.4f)) else null
            val brush = if (showGradients) {
                Brush.radialGradient(
                    colors = gradientColors!!
                )
            } else {
                SolidColor(baseColor)
            }
            bubble.copy(size = finalSize, brush = brush, gradientColors = gradientColors)
        }
    }

    ResponsiveLayout(
        isCompact = isCompact,
        title = "Bubble / Scatter Chart",
        description = "Multi-dimensional plot. Bubbles expand dynamically with a spring popup overshoot effect on load and tap.",
        chartContent = {
            BubbleChart(
                bubbles = displayBubbles,
                minBubbleRadius = minRadius.dp,
                maxBubbleRadius = maxRadius.dp,
                gridConfig = GridConfig(
                    showHorizontalLines = true,
                    showVerticalLines = true
                ),
                animationConfig = AnimationConfig(animateEntry = animate),
                scrollEnabled = scrollEnabled
            )
        },
        controlContent = {
            Text(
                text = "Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Column {
                Text("Min Bubble Radius: ${minRadius.toInt()}dp", color = Color.LightGray, fontSize = 12.sp)
                Slider(
                    value = minRadius,
                    onValueChange = { minRadius = it.coerceAtMost(maxRadius - 2f) },
                    valueRange = 4f..20f
                )
            }

            Column {
                Text("Max Bubble Radius: ${maxRadius.toInt()}dp", color = Color.LightGray, fontSize = 12.sp)
                Slider(
                    value = maxRadius,
                    onValueChange = { maxRadius = it.coerceAtLeast(minRadius + 2f) },
                    valueRange = 16f..50f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scatter Mode (Fixed Size)", color = Color.Gray)
                Switch(checked = scatterMode, onCheckedChange = { scatterMode = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Animate Entry", color = Color.Gray)
                Switch(checked = animate, onCheckedChange = { animate = it })
            }

            // Horizontal Scroll Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horizontal Scroll", color = Color.Gray)
                Switch(checked = scrollEnabled, onCheckedChange = { scrollEnabled = it })
            }

            // Bubble Gradient Fill Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bubble Gradient Fill", color = Color.Gray)
                Switch(checked = showGradients, onCheckedChange = { showGradients = it })
            }
        },
        codeSnippet = """
            BubbleChart(
                bubbles = bubbles,
                minBubbleRadius = ${minRadius.toInt()}.dp,
                maxBubbleRadius = ${maxRadius.toInt()}.dp,
                animationConfig = AnimationConfig(animateEntry = $animate)
            )
        """.trimIndent()
    )
}


@Composable
fun CandlestickShowcase(isCompact: Boolean) {
    var animate by remember { mutableStateOf(true) }
    var wickThickness by remember { mutableStateOf(1.5f) }
    var candleSpacingRatio by remember { mutableStateOf(0.7f) }
    var scrollEnabled by remember { mutableStateOf(true) }
    var showGradients by remember { mutableStateOf(true) }

    var bullColor by remember { mutableStateOf(Color(0xFF26A69A)) }
    var bearColor by remember { mutableStateOf(Color(0xFFEF5350)) }

    val positiveColors = if (showGradients) listOf(bullColor, bullColor.copy(alpha = 0.5f)) else null
    val negativeColors = if (showGradients) listOf(bearColor, bearColor.copy(alpha = 0.5f)) else null

    val positiveBrush = if (showGradients) {
        Brush.verticalGradient(colors = positiveColors!!)
    } else {
        SolidColor(bullColor)
    }

    val negativeBrush = if (showGradients) {
        Brush.verticalGradient(colors = negativeColors!!)
    } else {
        SolidColor(bearColor)
    }

    val stockData = remember {
        listOf(
            CandleData("Jul 01", open = 150f, high = 155f, low = 148f, close = 152f),
            CandleData("Jul 02", open = 152f, high = 158f, low = 151f, close = 156f),
            CandleData("Jul 03", open = 156f, high = 157f, low = 152f, close = 153f),
            CandleData("Jul 04", open = 153f, high = 162f, low = 150f, close = 160f),
            CandleData("Jul 05", open = 160f, high = 161f, low = 155f, close = 157f),
            CandleData("Jul 06", open = 157f, high = 166f, low = 156f, close = 164f),
            CandleData("Jul 07", open = 164f, high = 168f, low = 162f, close = 167f),
            CandleData("Jul 08", open = 167f, high = 172f, low = 165f, close = 171f),
            CandleData("Jul 09", open = 171f, high = 172f, low = 166f, close = 168f),
            CandleData("Jul 10", open = 168f, high = 175f, low = 167f, close = 174f)
        )
    }

    ResponsiveLayout(
        isCompact = isCompact,
        title = "Candlestick / Stock Chart",
        description = "Financial OHLC data representation with smooth growth animations and interactive detail cards.",
        chartContent = {
            CandlestickChart(
                candles = stockData,
                positiveBrush = positiveBrush,
                negativeBrush = negativeBrush,
                wickThickness = wickThickness.dp,
                candleWidthRatio = candleSpacingRatio,
                animationConfig = AnimationConfig(animateEntry = animate),
                axisConfig = AxisConfig(
                    yLabelFormatter = { "$${ChartUtils.format(it, 2)}" }
                ),
                scrollEnabled = scrollEnabled,
                positiveGradientColors = positiveColors,
                negativeGradientColors = negativeColors
            )
        },
        controlContent = {
            Text(
                text = "Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Column {
                Text("Wick Thickness: ${ChartUtils.format(wickThickness, 1)}dp", color = Color.LightGray, fontSize = 12.sp)
                Slider(
                    value = wickThickness,
                    onValueChange = { wickThickness = it },
                    valueRange = 0.5f..5.0f
                )
            }

            Column {
                Text("Candle Width Ratio: ${ChartUtils.format(candleSpacingRatio, 2)}", color = Color.LightGray, fontSize = 12.sp)
                Slider(
                    value = candleSpacingRatio,
                    onValueChange = { candleSpacingRatio = it },
                    valueRange = 0.3f..0.9f
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Color Themes", color = Color.LightGray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            bullColor = Color(0xFF26A69A)
                            bearColor = Color(0xFFEF5350)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A).copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TradingView", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            bullColor = Color(0xFF00E676)
                            bearColor = Color(0xFFFF1744)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676).copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Neon Pop", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Animate Entry", color = Color.Gray)
                Switch(checked = animate, onCheckedChange = { animate = it })
            }

            // Horizontal Scroll Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horizontal Scroll", color = Color.Gray)
                Switch(checked = scrollEnabled, onCheckedChange = { scrollEnabled = it })
            }

            // Candle Gradient Fill Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Candle Gradient Fill", color = Color.Gray)
                Switch(checked = showGradients, onCheckedChange = { showGradients = it })
            }
        },
        codeSnippet = """
            CandlestickChart(
                candles = stockData,
                positiveColor = Color(0x${bullColor.toHex()}),
                negativeColor = Color(0x${bearColor.toHex()}),
                wickThickness = ${wickThickness}f.dp,
                candleWidthRatio = ${candleSpacingRatio}f,
                animationConfig = AnimationConfig(animateEntry = $animate)
            )
        """.trimIndent()
    )
}


private fun Color.toHex(): String {
    val alpha = (alpha * 255).toInt().toString(16).padStart(2, '0')
    val red = (red * 255).toInt().toString(16).padStart(2, '0')
    val green = (green * 255).toInt().toString(16).padStart(2, '0')
    val blue = (blue * 255).toInt().toString(16).padStart(2, '0')
    return "$alpha$red$green$blue".uppercase()
}

private fun Brush.obtainColorAtCenter(): Color {
    return if (this is SolidColor) {
        this.value
    } else {
        Color(0xFFFFB300)
    }
}
