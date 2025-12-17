package com.xianliticn.yuefu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    /**
     * 可视范围。键盘总长 = [whiteKeyWidth] * [keyCount]，此参数位于总长内。
     */
    visibleRange: NoteFlowVisibleRange
) {
    // 将速度转换为 像素/毫秒
    val pixelsPerMillis = pixelsPerSecond / 1000f

    Canvas(modifier = modifier.fillMaxWidth()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 判定线位置（通常在底部）
        val hitLineY = canvasHeight

        // 视口时间范围计算（用于剔除不可见音符）
        // 屏幕顶端对应的时间 = 当前时间 + (屏幕高度 / 速度)
        val visibleDurationMillis = (canvasHeight / pixelsPerMillis).toLong()
        val minTime = currentProgressMillis
        val maxTime = currentProgressMillis + visibleDurationMillis
        // 稍微加一点缓冲时间，防止边缘闪烁
        val bufferTime = 500L

        // 计算当前视口的平移量，将 visibleRange.startPx 映射到 Canvas 的 x=0
        val offsetX = -visibleRange.startPx

        // 遍历所有音符
        notes.forEach { note ->
            // 1. 简单的视口剔除优化：如果音符还没出现或已经落下，就不绘制
            if (note.endTimeMillis < minTime - bufferTime || note.startTimeMillis > maxTime + bufferTime) {
                return@forEach
            }

            // 2. 计算 X 轴位置 (根据 visibleRange 进行偏移)
            //在键中点绘制
            val rawMid = (note.keyIndex - 0.5f) * whiteKeyWidth
            val mid = rawMid + offsetX
            // 简单的宽度计算，实际可能需要根据黑白键调整
            val width = whiteKeyWidth * 0.4f

            // 水平方向的视口剔除：如果音符完全在视口左侧或右侧，则不绘制
            if (mid + width < 0 || mid > canvasWidth) {
                return@forEach
            }

            // 3. 计算 Y 轴位置
            // 下落效果：时间越大，位置越高（y值越小），时间等于当前时间时，到达底部
            // 公式：y = 判定线Y - (音符时间 - 当前时间) * 速度
            // 注意：Canvas坐标系原点在左上角

            val noteEndDistance = (note.endTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteStartDistance =
                (note.startTimeMillis - currentProgressMillis) * pixelsPerMillis

            // 屏幕上的 Y 坐标（底部是 heavy value）
            // noteBottom 是音符的头部（先到达判定线的部分，即 startTime）
            // noteTop 是音符的尾部（endTime）
            val noteBottomY = hitLineY - noteStartDistance
            val noteTopY = hitLineY - noteEndDistance

            // 计算高度
            val height = noteBottomY - noteTopY

            // 绘制
            drawRoundRect(
                color = note.color,
                topLeft = Offset(x = mid - width / 2f, y = noteTopY),
                size = Size(width = width, height = height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

data class NoteFlowVisibleRange(
    val startPx: Float,
    val endPx: Float
)