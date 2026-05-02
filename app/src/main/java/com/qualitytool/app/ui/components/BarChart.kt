package com.qualitytool.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class BarItem(val label: String, val value: Int, val color: Color)

@Composable
fun BarChart(
    items: List<BarItem>,
    modifier: Modifier = Modifier
) {
    val maxVal = items.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val barW = 44f
    val gap = 16f

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(150.dp)
        ) {
            val totalW = items.size * barW + (items.size - 1) * gap
            val startX = (size.width - totalW) / 2
            val chartH = size.height - 20f

            items.forEachIndexed { i, item ->
                val h = (item.value.toFloat() / maxVal) * chartH
                val x = startX + i * (barW + gap)
                drawRoundRect(
                    color = item.color,
                    topLeft = Offset(x, chartH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
