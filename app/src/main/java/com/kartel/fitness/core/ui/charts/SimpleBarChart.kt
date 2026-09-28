package com.kartel.fitness.core.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class ChartPoint(val label: String, val value: Float)

/**
 * Простой столбчатый график на Compose Canvas (roadmap п.9 "Аналитика").
 * Без сторонних библиотек — для 6-7 точек данных этого достаточно.
 */
@Composable
fun SimpleBarChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() },
) {
    if (data.isEmpty()) return
    val maxValue = (data.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier.fillMaxWidth().height(160.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val barCount = data.size
            val spacing = 8.dp.toPx()
            val labelAreaHeight = 36.dp.toPx()
            val chartHeight = size.height - labelAreaHeight
            val barWidth = (size.width - spacing * (barCount + 1)) / barCount

            // базовая линия
            drawLine(
                color = gridColor,
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 1.dp.toPx(),
            )

            data.forEachIndexed { index, point ->
                val barHeight = if (maxValue > 0) (point.value / maxValue) * (chartHeight - 8.dp.toPx()) else 0f
                val left = spacing + index * (barWidth + spacing)
                val top = chartHeight - barHeight

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 11.sp.toPx()
                    }
                    drawText(point.label, left + barWidth / 2, size.height - 6.dp.toPx(), paint)

                    val valuePaint = android.graphics.Paint().apply {
                        color = barColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 11.sp.toPx()
                    }
                    if (point.value > 0) {
                        drawText(
                            valueFormatter(point.value),
                            left + barWidth / 2,
                            (top - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
                            valuePaint,
                        )
                    }
                }
            }
        }
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt(),
    )
}
