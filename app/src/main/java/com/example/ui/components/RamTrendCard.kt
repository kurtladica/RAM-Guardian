package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.memory.MemoryPressureLevel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceContainer
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RamTrendCard(
    history: List<Float>,
    pressureLevel: MemoryPressureLevel,
    onStatusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusColor = when (pressureLevel) {
        MemoryPressureLevel.HIGH -> AmberAccent
        MemoryPressureLevel.MODERATE -> CyanAccent
        MemoryPressureLevel.NORMAL -> EmeraldAccent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("ram_trend_card")
    ) {
        Column {
            // Header: Title & Close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAM Usage Trend",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart area with Y-axis labels and canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-Axis labels (100%, 75%, 50%, 25%, 0%)
                Column(
                    modifier = Modifier
                        .height(120.dp)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                            color = TextTertiary
                        )
                    }
                }

                // Canvas Line Chart
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .testTag("ram_trend_canvas")
                ) {
                    val width = size.width
                    val height = size.height

                    // Draw subtle dashed/faint grid lines
                    val steps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    steps.forEach { frac ->
                        val y = height * frac
                        drawLine(
                            color = Color(0xFF1B2432),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (history.isEmpty()) return@Canvas

                    val points = mutableListOf<Offset>()
                    val stepX = if (history.size > 1) width / (history.size - 1) else width

                    history.forEachIndexed { i, pct ->
                        val x = if (history.size > 1) i * stepX else width / 2
                        val clamped = pct.coerceIn(0f, 100f)
                        val y = height * (1f - (clamped / 100f))
                        points.add(Offset(x, y))
                    }

                    if (points.size > 1) {
                        // Fill path
                        val fillPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    EmeraldAccent.copy(alpha = 0.25f),
                                    CyanAccent.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Stroke path
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            brush = Brush.horizontalGradient(listOf(CyanAccent, EmeraldAccent)),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw points on each node
                        points.forEach { pt ->
                            drawCircle(color = EmeraldAccent, radius = 2.5.dp.toPx(), center = pt)
                        }

                        // Last point highlight
                        val last = points.last()
                        drawCircle(color = CyanAccent, radius = 4.dp.toPx(), center = last)
                        drawCircle(color = CharcoalSurface, radius = 2.dp.toPx(), center = last)
                    } else if (points.size == 1) {
                        drawCircle(color = EmeraldAccent, radius = 4.dp.toPx(), center = points.first())
                    }
                }
            }

            // X-Axis timestamps
            val now = System.currentTimeMillis()
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(180_000L, 120_000L, 60_000L, 0L).forEach { offset ->
                    Text(
                        text = timeFmt.format(Date(now - offset)),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom pill: Normal memory pressure >
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CharcoalSurfaceContainer)
                    .clickable { onStatusClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = pressureLevel.label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
