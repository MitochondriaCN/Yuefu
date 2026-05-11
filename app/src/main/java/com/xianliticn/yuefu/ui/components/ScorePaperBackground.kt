package com.xianliticn.yuefu.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

private val staffYsDp = listOf(30, 44, 58, 72, 86)

private data class Note(val xDp: Int, val yDp: Int, val stemTopDp: Int)

private val tileNotes = listOf(
    Note(60, 65, 30),
    Note(150, 51, 25),
    Note(220, 79, 44),
)

fun Modifier.scorePaperTexture(
    color: Color,
    alpha: Float = 0.04f,
): Modifier = this.drawBehind {
    val tileWidth = 280.dp.toPx()
    val tileHeight = 180.dp.toPx()
    val staffStroke = 0.5.dp.toPx()
    val stemStroke = 0.8.dp.toPx()
    val noteRadius = 3.dp.toPx()

    val lineColor = color.copy(alpha = alpha)
    val noteColor = color.copy(alpha = alpha * 0.7f)

    val cols = ceil(size.width / tileWidth).toInt() + 1
    val rows = ceil(size.height / tileHeight).toInt() + 1

    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val ox = col * tileWidth
            val oy = row * tileHeight

            for (yDp in staffYsDp) {
                val y = oy + yDp.dp.toPx()
                drawLine(
                    color = lineColor,
                    start = Offset(ox, y),
                    end = Offset(ox + tileWidth, y),
                    strokeWidth = staffStroke,
                )
            }

            for (note in tileNotes) {
                val cx = ox + note.xDp.dp.toPx()
                val cy = oy + note.yDp.dp.toPx()
                val sy = oy + note.stemTopDp.dp.toPx()
                drawCircle(
                    color = noteColor,
                    radius = noteRadius,
                    center = Offset(cx, cy),
                )
                drawLine(
                    color = noteColor,
                    start = Offset(cx, cy),
                    end = Offset(cx, sy),
                    strokeWidth = stemStroke,
                )
            }
        }
    }
}
