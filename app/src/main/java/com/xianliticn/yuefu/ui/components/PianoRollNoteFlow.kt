package com.xianliticn.yuefu.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.ui.theme.Orange800
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ==================== 特效等级配置（预留接口） ====================

enum class EffectLevel {
    LOW, MEDIUM, HIGH
}

object EffectConfig {
    var level: EffectLevel = EffectLevel.MEDIUM

    val glowBlurRadius: Float
        get() = when (level) {
            EffectLevel.LOW -> 4f
            EffectLevel.MEDIUM -> 8f
            EffectLevel.HIGH -> 16f
        }

    val particlesPerNote: Int
        get() = when (level) {
            EffectLevel.LOW -> 4
            EffectLevel.MEDIUM -> 10
            EffectLevel.HIGH -> 18
        }

    val glowBandEnabled: Boolean
        get() = level != EffectLevel.LOW
}

// ==================== 背景色 ====================

val BackgroundWarm = Color(0xFF1E1A17)
val GlowBandColor = Color(0xFFFFF8F0)

// ==================== 粒子系统 ====================

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val baseSize: Float
) {
    fun update(deltaMs: Float) {
        x += vx * deltaMs
        y += vy * deltaMs
        vy += 0.0003f * deltaMs
        life -= deltaMs / maxLife
    }

    val alpha: Float get() = (life * 1.5f).coerceIn(0f, 1f)
    val currentSize: Float get() = baseSize * (0.6f + life * 0.4f)
}

fun emitParticles(
    particles: MutableList<Particle>,
    keyX: Float,
    hitLineY: Float,
    noteColor: Color,
    count: Int
) {
    repeat(count) {
        val angle = Random.nextFloat() * kotlin.math.PI * 2
        val speed = Random.nextFloat() * 0.25f + 0.08f
        particles.add(
            Particle(
                x = keyX,
                y = hitLineY,
                vx = cos(angle).toFloat() * speed,
                vy = -abs(sin(angle).toFloat() * speed) - 0.15f,
                life = 1f,
                maxLife = Random.nextFloat() * 200f + 250f,
                color = noteColor,
                baseSize = Random.nextFloat() * 2.5f + 1.5f
            )
        )
    }
}

// ==================== 钢琴键盘数据 ====================

data class PianoKeyData(
    val note: PianoKey,
    val octave: Int,
    val type: PianoKeyType,
    val keyIndex: Float
) {
    val id: String get() = "${note.name}$octave"
}

enum class PianoKeyType {
    White, Black
}

enum class PianoKey(val isBlack: Boolean) {
    C(false), CSharp(true),
    D(false), DSharp(true),
    E(false),
    F(false), FSharp(true),
    G(false), GSharp(true),
    A(false), ASharp(true),
    B(false)
}

fun generate88Keys(): List<PianoKeyData> {
    val keys = mutableListOf<PianoKeyData>()
    // 钢琴从 A0, A#0, B0 开始
    val startKeys = listOf(PianoKey.A, PianoKey.ASharp, PianoKey.B)
    startKeys.forEachIndexed { index, key ->
        val keyIndex = when (index) {
            0 -> 0f
            1 -> 0.5f
            else -> 1f
        }
        keys.add(PianoKeyData(key, 0, if (key.isBlack) PianoKeyType.Black else PianoKeyType.White, keyIndex))
    }

    // 中间 1 到 7 个八度
    for (octave in 1..7) {
        val baseIndex = 2 + (octave - 1) * 7
        PianoKey.entries.forEach { key ->
            val offset = when (key) {
                PianoKey.C -> 0f
                PianoKey.CSharp -> 0.5f
                PianoKey.D -> 1f
                PianoKey.DSharp -> 1.5f
                PianoKey.E -> 2f
                PianoKey.F -> 3f
                PianoKey.FSharp -> 3.5f
                PianoKey.G -> 4f
                PianoKey.GSharp -> 4.5f
                PianoKey.A -> 5f
                PianoKey.ASharp -> 5.5f
                PianoKey.B -> 6f
            }
            keys.add(
                PianoKeyData(
                    key, octave,
                    if (key.isBlack) PianoKeyType.Black else PianoKeyType.White,
                    baseIndex + offset
                )
            )
        }
    }

    // 最后一个键 C8
    keys.add(PianoKeyData(PianoKey.C, 8, PianoKeyType.White, 51f))

    return keys
}

// ==================== 琴键组件 ====================

@Composable
fun PianoKey(
    modifier: Modifier = Modifier,
    keyType: PianoKeyType = PianoKeyType.White,
    highlightColor: Color? = null,
    onPressed: () -> Unit,
    onReleased: () -> Unit
) {
    val highlightProgress by animateFloatAsState(
        targetValue = if (highlightColor != null) 1f else 0f,
        animationSpec = tween(80, easing = FastOutSlowInEasing),
        label = "highlight"
    )

    val pressOffset by animateFloatAsState(
        targetValue = if (highlightColor != null) 3f else 0f,
        animationSpec = tween(80, easing = FastOutSlowInEasing),
        label = "press"
    )

    val baseColor = when (keyType) {
        PianoKeyType.White -> Color(0xFFF5F0E8)
        PianoKeyType.Black -> Color(0xFF2A2520)
    }

    val finalColor = if (highlightColor != null && highlightProgress > 0.01f) {
        androidx.compose.ui.graphics.lerp(
            baseColor,
            highlightColor.copy(alpha = 0.75f),
            highlightProgress
        )
    } else {
        baseColor
    }

    val shadowElevation = if (highlightColor != null) 6.dp else 1.dp

    Surface(
        modifier = modifier
            .graphicsLayer { translationY = pressOffset }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressed()
                        tryAwaitRelease()
                        onReleased()
                    }
                )
            },
        shape = RoundedCornerShape(
            topStart = 3.dp,
            topEnd = 3.dp,
            bottomStart = 10.dp,
            bottomEnd = 10.dp
        ),
        color = finalColor,
        border = BorderStroke(
            0.5.dp,
            if (keyType == PianoKeyType.White) Color(0x15FFFFFF) else Color(0x30FFFFFF)
        ),
        shadowElevation = shadowElevation
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 基础渐变
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (keyType == PianoKeyType.White) {
                                listOf(
                                    finalColor.copy(alpha = 0.95f),
                                    finalColor.copy(alpha = 0.7f)
                                )
                            } else {
                                listOf(
                                    Color(0xFF3A3530).copy(alpha = 0.9f),
                                    finalColor.copy(alpha = 0.85f)
                                )
                            }
                        )
                    )
            )

            // 顶部高光反射（按下时增强）
            if (highlightProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.5f * highlightProgress),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // 底部阴影（增加立体感）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x20000000)
                            )
                        )
                    )
            )
        }
    }
}

// ==================== 主组件：钢琴瀑布流 ====================

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("FrequentlyChangingValue")
@Composable
fun PianoRollNoteFlow(
    modifier: Modifier = Modifier,
    isScrollMode: Boolean = false,
    keyboardHeight: Dp = 200.dp,
    notes: List<VisualNoteEvent> = emptyList(),
    currentProgressMillis: Long = 0L,
    onKeyPressed: (PianoKeyData) -> Unit = {},
    onKeyReleased: (PianoKeyData) -> Unit = {}
) {
    // 1. 生成所有按键数据
    val allKeys = remember { generate88Keys() }
    val whiteKeys = remember(allKeys) { allKeys.filter { it.type == PianoKeyType.White } }
    val keyIndexMap = remember(allKeys) {
        allKeys.associateBy { it.keyIndex }
    }

    // 2. 键盘滚动状态（需在 NoteFlowWithParticles 之前声明）
    val scrollState = rememberScrollState()

    // 3. 计算当前按下的琴键及颜色
    val pressedKeyColors = remember(notes, currentProgressMillis) {
        val map = mutableMapOf<Float, Color>()
        notes.forEach { note ->
            if (currentProgressMillis in note.startTimeMillis..note.endTimeMillis) {
                map[note.keyIndex] = note.color
            }
        }
        map
    }

    // 3. 光晕带强度
    val glowIntensity by remember(notes, currentProgressMillis) {
        val activeCount = notes.count {
            currentProgressMillis in it.startTimeMillis..it.endTimeMillis
        }
        mutableFloatStateOf((activeCount * 0.06f).coerceIn(0.05f, 0.45f))
    }

    // 4. 尺寸定义
    val whiteKeyWidth = 40.dp
    val blackKeyWidth = 24.dp
    val density = LocalDensity.current
    val whiteKeyWidthPx = with(density) { whiteKeyWidth.toPx() }

    // 5. 触摸位置检测
    val keysLayoutMap = remember { mutableMapOf<String, Rect>() }
    val blackKeyHeight = keyboardHeight * 0.6f
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }

    // 6. 粒子动画驱动（由 NoteFlowWithParticles 内部管理）

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewportWidthPx = it.width.toFloat() }
            .background(BackgroundWarm)
    ) {
        // ===== 瀑布流区域 =====
        NoteFlowWithParticles(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = keyboardHeight)
                .clipToBounds(),
            whiteKeyWidth = whiteKeyWidthPx,
            keyCount = whiteKeys.size,
            notes = notes,
            currentProgressMillis = currentProgressMillis,
            visibleRange = NoteFlowVisibleRange(
                startPx = scrollState.value.toFloat(),
                endPx = scrollState.value.toFloat() + viewportWidthPx
            )
        )

        // ===== 光晕带（键盘上方） =====
        if (EffectConfig.glowBandEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = -keyboardHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GlowBandColor.copy(alpha = glowIntensity * 0.4f),
                                GlowBandColor.copy(alpha = glowIntensity * 0.6f),
                                GlowBandColor.copy(alpha = glowIntensity * 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ===== 钢琴键盘区域 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(keyboardHeight)
                .align(Alignment.BottomCenter)
        ) {
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .horizontalScroll(scrollState, isScrollMode)
                        .pointerInput(isScrollMode) {
                            if (!isScrollMode) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    var touchPosition =
                                        down.position.copy(x = down.position.x + scrollState.value)

                                    var currentKey = findKeyAt(touchPosition, keysLayoutMap, allKeys)
                                    currentKey?.let { onKeyPressed(it) }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.lastOrNull() ?: break

                                        if (change.pressed) {
                                            touchPosition =
                                                change.position.copy(x = change.position.x + scrollState.value)
                                            val newKey = findKeyAt(touchPosition, keysLayoutMap, allKeys)

                                            if (newKey != null && newKey.id != currentKey?.id) {
                                                currentKey?.let { onKeyReleased(it) }
                                                currentKey = newKey
                                                onKeyPressed(newKey)
                                            }
                                        } else {
                                            break
                                        }
                                    }
                                    currentKey?.let { onKeyReleased(it) }
                                }
                            }
                        }
                ) {
                    val totalWidthDp = whiteKeyWidth * whiteKeys.size

                    Box(
                        modifier = Modifier
                            .width(totalWidthDp)
                            .fillMaxHeight()
                    ) {
                        // --- 白键 ---
                        whiteKeys.forEachIndexed { index, keyData ->
                            val offsetX = whiteKeyWidth * index
                            val highlightColor = pressedKeyColors[keyData.keyIndex]

                            PianoKey(
                                modifier = Modifier
                                    .width(whiteKeyWidth)
                                    .fillMaxHeight()
                                    .offset(x = offsetX)
                                    .onGloballyPositioned {
                                        with(density) {
                                            keysLayoutMap[keyData.id] = Rect(
                                                offsetX.toPx(),
                                                0f,
                                                (offsetX + whiteKeyWidth).toPx(),
                                                keyboardHeight.toPx()
                                            )
                                        }
                                    },
                                keyType = PianoKeyType.White,
                                highlightColor = highlightColor,
                                onPressed = { onKeyPressed(keyData) },
                                onReleased = { onKeyReleased(keyData) }
                            )
                        }

                        // --- 黑键 ---
                        var currentWhiteKeyIndex = 0
                        allKeys.forEach { keyData ->
                            if (keyData.type == PianoKeyType.White) {
                                currentWhiteKeyIndex++
                            } else {
                                val leftAnchorWhiteKeyIndex = currentWhiteKeyIndex
                                val offsetXDp =
                                    (whiteKeyWidth * leftAnchorWhiteKeyIndex) - (blackKeyWidth / 2)
                                val highlightColor = pressedKeyColors[keyData.keyIndex]

                                PianoKey(
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .width(blackKeyWidth)
                                        .height(blackKeyHeight)
                                        .offset(x = offsetXDp)
                                        .onGloballyPositioned {
                                            with(density) {
                                                keysLayoutMap[keyData.id] = Rect(
                                                    offsetXDp.toPx(),
                                                    0f,
                                                    (offsetXDp + blackKeyWidth).toPx(),
                                                    blackKeyHeight.toPx()
                                                )
                                            }
                                        },
                                    keyType = PianoKeyType.Black,
                                    highlightColor = highlightColor,
                                    onPressed = { onKeyPressed(keyData) },
                                    onReleased = { onKeyReleased(keyData) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findKeyAt(
    position: Offset,
    map: Map<String, Rect>,
    allKeys: List<PianoKeyData>
): PianoKeyData? {
    val blackKeyMatch = allKeys.filter { it.type == PianoKeyType.Black }.find { key ->
        map[key.id]?.contains(position) == true
    }
    if (blackKeyMatch != null) return blackKeyMatch

    return allKeys.filter { it.type == PianoKeyType.White }.find { key ->
        map[key.id]?.contains(position) == true
    }
}

// ==================== 增强版 NoteFlow（带粒子） ====================

@Composable
fun NoteFlowWithParticles(
    modifier: Modifier = Modifier,
    whiteKeyWidth: Float,
    keyCount: Int,
    notes: List<VisualNoteEvent>,
    pixelsPerSecond: Float = 300f,
    currentProgressMillis: Long,
    visibleRange: NoteFlowVisibleRange
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val triggeredNotes = remember { mutableSetOf<Long>() }
    var lastProgress by remember { mutableLongStateOf(-1L) }

    // 粒子更新驱动
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            frameTick++
            // 清理死亡粒子
            particles.removeAll { it.life <= 0 }
            // 更新存活粒子
            particles.forEach { it.update(16f) }
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth()) {
        // 强制读取 frameTick 以确保粒子更新时触发 Canvas 重绘
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick

        val canvasWidth = size.width
        val canvasHeight = size.height
        val hitLineY = canvasHeight
        val pixelsPerMillis = pixelsPerSecond / 1000f

        // 绘制背景
        drawRect(color = BackgroundWarm)

        // 视口时间范围
        val visibleDurationMillis = (canvasHeight / pixelsPerMillis).toLong()
        val minTime = currentProgressMillis
        val maxTime = currentProgressMillis + visibleDurationMillis
        val bufferTime = 500L
        val offsetX = -visibleRange.startPx

        // 检测新触发音符并发射粒子
        if (lastProgress != currentProgressMillis && lastProgress >= 0) {
            notes.forEach { note ->
                val triggerId = note.startTimeMillis * 1000L + (note.keyIndex * 100).toLong()
                if (note.startTimeMillis in lastProgress..currentProgressMillis) {
                    if (!triggeredNotes.contains(triggerId)) {
                        triggeredNotes.add(triggerId)
                        // 直接发射粒子
                        val keyX = (note.keyIndex + 0.5f) * whiteKeyWidth
                        emitParticles(
                            particles,
                            keyX,
                            hitLineY,
                            note.color,
                            EffectConfig.particlesPerNote
                        )
                    }
                }
            }
            // 清理过期记录
            triggeredNotes.removeAll { id ->
                val noteTime = id / 1000L
                noteTime < currentProgressMillis - 10000
            }
        }
        if (lastProgress != currentProgressMillis) {
            lastProgress = currentProgressMillis
        }

        // 绘制音符辉光层
        notes.forEach { note ->
            if (note.endTimeMillis < minTime - bufferTime || note.startTimeMillis > maxTime + bufferTime) return@forEach

            val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
            val mid = rawMid + offsetX
            val noteWidth = whiteKeyWidth * 0.4f

            if (mid + noteWidth < 0 || mid > canvasWidth) return@forEach

            val noteEndDistance = (note.endTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteStartDistance = (note.startTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteBottomY = hitLineY - noteStartDistance
            val noteTopY = hitLineY - noteEndDistance
            val noteHeight = noteBottomY - noteTopY

            val glowRadiusOuter = if (note.isLongNote) 24f else 16f
            val glowRadiusInner = if (note.isLongNote) 16f else 10f

            // 辉光层1：最外层柔光
            drawRoundRect(
                color = note.color.copy(alpha = 0.12f),
                topLeft = Offset(x = mid - noteWidth * 1.2f, y = noteTopY - 6f),
                size = androidx.compose.ui.geometry.Size(
                    width = noteWidth * 2.4f,
                    height = noteHeight + 12f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(glowRadiusOuter)
            )

            // 辉光层2：中层
            drawRoundRect(
                color = note.color.copy(alpha = 0.22f),
                topLeft = Offset(x = mid - noteWidth * 0.8f, y = noteTopY - 3f),
                size = androidx.compose.ui.geometry.Size(
                    width = noteWidth * 1.6f,
                    height = noteHeight + 6f
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(glowRadiusInner)
            )
        }

        // 绘制音符主体
        notes.forEach { note ->
            if (note.endTimeMillis < minTime - bufferTime || note.startTimeMillis > maxTime + bufferTime) return@forEach

            val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
            val mid = rawMid + offsetX
            val noteWidth = whiteKeyWidth * 0.4f

            if (mid + noteWidth < 0 || mid > canvasWidth) return@forEach

            val noteEndDistance = (note.endTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteStartDistance = (note.startTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteBottomY = hitLineY - noteStartDistance
            val noteTopY = hitLineY - noteEndDistance
            val noteHeight = noteBottomY - noteTopY

            val isBlackKey = (note.keyIndex % 1f) != 0f
            val noteAlpha = if (isBlackKey) 0.85f else 0.75f
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                if (note.isLongNote) 12f else 4f
            )

            val left = mid - noteWidth / 2f
            val right = mid + noteWidth / 2f

            // 主体
            drawRoundRect(
                color = note.color.copy(alpha = noteAlpha),
                topLeft = Offset(x = left, y = noteTopY),
                size = androidx.compose.ui.geometry.Size(width = noteWidth, height = noteHeight),
                cornerRadius = cornerRadius
            )

            // 长音符进度填充（Deemo 风格：底部发光条逐渐上升）
            if (note.isLongNote) {
                val progress = when {
                    currentProgressMillis <= note.startTimeMillis -> 0f
                    currentProgressMillis >= note.endTimeMillis -> 1f
                    else -> (currentProgressMillis - note.startTimeMillis).toFloat() / note.durationMillis
                }
                if (progress > 0f) {
                    val fillHeight = noteHeight * progress
                    val fillTop = noteBottomY - fillHeight
                    drawRoundRect(
                        color = note.color.copy(alpha = 0.45f),
                        topLeft = Offset(x = left, y = fillTop),
                        size = androidx.compose.ui.geometry.Size(width = noteWidth, height = fillHeight),
                        cornerRadius = cornerRadius
                    )
                    // 填充顶部高光边
                    if (fillHeight > 3f) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(x = left + 2f, y = fillTop),
                            end = Offset(x = right - 2f, y = fillTop),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // 声部纹理
            when (note.partId) {
                1 -> { // 横向细条纹
                    val stripeCount = (noteHeight / 12f).toInt().coerceIn(2, 5)
                    val stripeSpacing = noteHeight / (stripeCount + 1)
                    for (i in 1..stripeCount) {
                        val y = noteTopY + stripeSpacing * i
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(left + 3f, y),
                            end = Offset(right - 3f, y),
                            strokeWidth = 1f
                        )
                    }
                }
                2 -> { // 斜向条纹
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(left, noteTopY, right, noteBottomY)
                    val stripeSpacing = 10f
                    val diagonalOffset = noteHeight * 0.4f
                    for (i in -3..8) {
                        val x = left + i * stripeSpacing
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(x, noteBottomY),
                            end = Offset(x + diagonalOffset, noteTopY),
                            strokeWidth = 1.5f
                        )
                    }
                    drawContext.canvas.restore()
                }
                3 -> { // 点阵
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(left, noteTopY, right, noteBottomY)
                    val dotSpacingY = 10f
                    val dotSpacingX = noteWidth / 3f
                    val rows = (noteHeight / dotSpacingY).toInt().coerceAtLeast(1)
                    for (row in 0 until rows) {
                        for (col in 0..1) {
                            val offsetX = if (row % 2 == 0) 0f else dotSpacingX / 2f
                            val cx = left + dotSpacingX * (col + 0.8f) + offsetX
                            val cy = noteTopY + dotSpacingY * (row + 0.5f)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = 1.2f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                    drawContext.canvas.restore()
                }
            }

            // 内部高光条（增加通透感）
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(x = mid - noteWidth / 4f, y = noteTopY + 2f),
                size = androidx.compose.ui.geometry.Size(
                    width = noteWidth / 2f,
                    height = (noteHeight * 0.3f).coerceAtLeast(4f)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
            )
        }

        // 绘制粒子
        particles.forEach { p ->
            if (p.life > 0 && p.x in -20f..canvasWidth + 20f && p.y in -20f..canvasHeight + 20f) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha * 0.8f),
                    radius = p.currentSize,
                    center = Offset(p.x + offsetX, p.y)
                )
            }
        }
    }
}

// ==================== 可见范围数据类 ====================

data class NoteFlowVisibleRange(
    val startPx: Float,
    val endPx: Float
)

// ==================== 兼容旧接口的 NoteFlow ====================

@Composable
fun NoteFlow(
    modifier: Modifier = Modifier,
    whiteKeyWidth: Float,
    keyCount: Int,
    notes: List<VisualNoteEvent>,
    pixelsPerSecond: Float = 300f,
    currentProgressMillis: Long,
    visibleRange: NoteFlowVisibleRange
) {
    NoteFlowWithParticles(
        modifier = modifier,
        whiteKeyWidth = whiteKeyWidth,
        keyCount = keyCount,
        notes = notes,
        pixelsPerSecond = pixelsPerSecond,
        currentProgressMillis = currentProgressMillis,
        visibleRange = visibleRange
    )
}

// ==================== 预览 ====================

@Preview(showBackground = true, backgroundColor = 0xFF1E1A17)
@Composable
fun PianoRollPreview() {
    PianoRollNoteFlow(
        modifier = Modifier.fillMaxWidth(),
        notes = listOf(
            VisualNoteEvent(0, 1000, 3f, Orange800),
            VisualNoteEvent(300, 1000, 4f, Orange800),
            VisualNoteEvent(600, 1200, 5f, com.xianliticn.yuefu.ui.theme.Green800)
        ),
        currentProgressMillis = 200
    )
}
