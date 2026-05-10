package com.xianliticn.yuefu.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.music.VisualNoteEvent

@Composable
fun NoteFlow(
    modifier: Modifier = Modifier,
    whiteKeyWidth: Float,
    keyCount: Int,
    notes: List<VisualNoteEvent>,
    pixelsPerSecond: Float = 300f,
    currentProgressMillis: Long,
    visibleRange: NoteFlowVisibleRange,
    effectLevel: EffectLevel = EffectLevel.HIGH,
    impactHolder: ImpactStateHolder? = null
) {
    val density = LocalDensity.current
    var lastProgressMillis by remember { mutableLongStateOf(currentProgressMillis) }
    var canvasHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentProgressMillis, notes) {
        if (impactHolder != null) {
            val from = lastProgressMillis
            val to = currentProgressMillis
            if (to > from && to - from < 5_000L) {
                notes.forEach { note ->
                    if (note.startTimeMillis in (from + 1)..to) {
                        impactHolder.spawnImpact(
                            ImpactEvent(
                                bornNanos = System.nanoTime(),
                                centerX = (note.keyIndex + 0.5f) * whiteKeyWidth,
                                color = note.color
                            )
                        )
                    }
                }
            } else {
                impactHolder.rings.clear()
                impactHolder.droplets.clear()
            }
            lastProgressMillis = to
        }
    }

    if (impactHolder != null) {
        AnimateImpactsLoop(impactHolder, canvasHeightPx, density)
    }

    val pixelsPerMillis = pixelsPerSecond / 1000f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
    ) {
        canvasHeightPx = size.height

        impactHolder?.frameNanos

        val canvasWidth = size.width
        val canvasHeight = size.height
        val hitLineY = canvasHeight

        val visibleDurationMillis = (canvasHeight / pixelsPerMillis).toLong()
        val minTime = currentProgressMillis
        val maxTime = currentProgressMillis + visibleDurationMillis
        val bufferTime = 500L

        val offsetX = -visibleRange.startPx

        notes.forEach { note ->
            if (note.endTimeMillis < minTime - bufferTime ||
                note.startTimeMillis > maxTime + bufferTime
            ) return@forEach

            val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
            val mid = rawMid + offsetX
            val width = whiteKeyWidth * 0.44f

            if (mid + width / 2f < 0 || mid - width / 2f > canvasWidth) return@forEach

            val noteEndDistance = (note.endTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteStartDistance = (note.startTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteBottomY = hitLineY - noteStartDistance
            val noteTopY = hitLineY - noteEndDistance
            val height = noteBottomY - noteTopY

            drawLiquidNote(
                x = mid - width / 2f,
                y = noteTopY,
                w = width,
                h = height,
                color = note.color
            )
        }

        impactHolder?.let { holder ->
            drawRings(holder.rings, holder.frameNanos, offsetX, hitLineY, density)
            drawDroplets(holder.droplets, holder.frameNanos, offsetX, density)
        }
    }
}

private fun DrawScope.drawLiquidNote(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    color: Color
) {
    if (h <= 0f || w <= 0f) return
    val cornerPx = 6.dp.toPx()

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()
            setShadowLayer(
                14.dp.toPx(),
                0f, 0f,
                android.graphics.Color.argb((0.85f * 255).toInt(), r, g, b)
            )
            setARGB((0.18f * 255).toInt(), r, g, b)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawRoundRect(
            x - 1.dp.toPx(),
            y,
            x + w + 1.dp.toPx(),
            y + h,
            cornerPx,
            cornerPx,
            paint
        )
    }

    drawRoundRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = 0.05f),
                0.35f to color.copy(alpha = 0.30f),
                0.75f to color.copy(alpha = 0.85f),
                1f to color.copy(alpha = 1f)
            )
        ),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(cornerPx)
    )

    if (h > 16.dp.toPx()) {
        val hlHeight = 12.dp.toPx()
        val hlY = y + h - hlHeight
        drawRoundRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color(255, 235, 200, 0),
                    1f to Color(255, 250, 235, (0.65f * 255).toInt())
                )
            ),
            topLeft = Offset(x + 1.dp.toPx(), hlY),
            size = Size(w - 2.dp.toPx(), hlHeight),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
    }

    val lineX = x + w / 2f
    val lineTop = y + 3.dp.toPx()
    val lineHeight = (h - 6.dp.toPx()).coerceAtLeast(0f)
    if (lineHeight > 0f) {
        drawLine(
            color = Color(255, 250, 240, (0.30f * 255).toInt()),
            start = Offset(lineX, lineTop),
            end = Offset(lineX, lineTop + lineHeight),
            strokeWidth = 1.4.dp.toPx()
        )
    }
}

data class NoteFlowVisibleRange(
    val startPx: Float,
    val endPx: Float
)
