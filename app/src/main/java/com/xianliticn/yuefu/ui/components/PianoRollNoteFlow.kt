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
import androidx.compose.runtime.mutableIntStateOf
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

    val glowBandEnabled: Boolean
        get() = level != EffectLevel.LOW

    // ========== 粒子效果等级配置 ==========

    val trailEnabled: Boolean get() = true
    val burstEnabled: Boolean get() = level != EffectLevel.LOW
    val sustainEnabled: Boolean get() = level == EffectLevel.HIGH

    /** 拖尾粒子产生间隔（帧数，约16ms/帧） */
    val trailEmitInterval: Int
        get() = when (level) {
            EffectLevel.LOW -> 6
            EffectLevel.MEDIUM -> 4
            EffectLevel.HIGH -> 3
        }

    /** 爆发阶段3扇形喷射粒子数 */
    val burstSparkCount: Int
        get() = when (level) {
            EffectLevel.LOW -> 0
            EffectLevel.MEDIUM -> 12
            EffectLevel.HIGH -> 25
        }

    /** 长音符持续流脉冲间隔（毫秒） */
    val sustainPulseIntervalMs: Long
        get() = when (level) {
            EffectLevel.LOW -> 0L
            EffectLevel.MEDIUM -> 0L
            EffectLevel.HIGH -> 180L
        }

    /** 长音符持续流每次脉冲粒子数 */
    val sustainParticlesPerPulse: Int
        get() = when (level) {
            EffectLevel.LOW -> 0
            EffectLevel.MEDIUM -> 0
            EffectLevel.HIGH -> 4
        }
}

// ==================== 背景色 ====================

val BackgroundWarm = Color(0xFF1E1A17)
val GlowBandColor = Color(0xFFFFF8F0)

// ==================== 粒子系统 ====================

enum class ParticleType {
    Trail,      // 拖尾粒子：小圆点，随机飘散
    BurstRay,   // 爆发星芒：光线
    BurstRing,  // 爆发环形：扩散圆环
    BurstSpark, // 爆发扇形喷射：小粒子
    Sustain     // 长音符持续流：向上飘散
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val baseSize: Float,
    val type: ParticleType = ParticleType.Trail,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var scale: Float = 1f,
    var extraData: Float = 0f  // 用于 BurstRing 记录最大半径等
) {
    fun update(deltaMs: Float) {
        x += vx * deltaMs
        y += vy * deltaMs
        rotation += rotationSpeed * deltaMs

        when (type) {
            ParticleType.Trail -> {
                vy += 0.0002f * deltaMs  // 轻微重力
                vx *= (1f - 0.001f * deltaMs)  // 轻微阻尼
            }
            ParticleType.BurstSpark -> {
                vy += 0.0004f * deltaMs  // 较强重力
                vx *= (1f - 0.002f * deltaMs)
            }
            ParticleType.Sustain -> {
                vy += 0.00015f * deltaMs  // 很弱的重力
                vx *= (1f - 0.0005f * deltaMs)
            }
            ParticleType.BurstRay -> {
                // 光线：匀速直线，快速淡出
            }
            ParticleType.BurstRing -> {
                // 圆环：匀速扩散
            }
        }

        life -= deltaMs / maxLife
    }

    val alpha: Float get() = when (type) {
        ParticleType.BurstRay -> (life * 2f).coerceIn(0f, 1f)  // 光线快速淡出
        ParticleType.BurstRing -> (life * 1.2f).coerceIn(0f, 0.7f)
        else -> (life * 1.5f).coerceIn(0f, 1f)
    }
    val currentSize: Float get() = baseSize * scale * when (type) {
        ParticleType.BurstRing -> (0.2f + life * 0.8f)  // 圆环随时间变小（线变细）
        else -> (0.5f + life * 0.5f)
    }
}

data class BurstState(
    val startTime: Long,
    var stage1Emitted: Boolean = false,
    var stage2Emitted: Boolean = false,
    var stage3Emitted: Boolean = false
)

// ==================== 粒子发射函数 ====================

/** 从音符后缘发射拖尾粒子 */
fun emitTrailParticles(
    particles: MutableList<Particle>,
    noteLeft: Float,
    noteRight: Float,
    noteTopY: Float,
    noteColor: Color,
    count: Int
) {
    repeat(count) {
        val x = Random.nextFloat() * (noteRight - noteLeft) + noteLeft
        val angle = Random.nextFloat() * kotlin.math.PI * 2
        val speed = Random.nextFloat() * 0.1f + 0.03f
        particles.add(
            Particle(
                x = x,
                y = noteTopY,
                vx = cos(angle).toFloat() * speed,
                vy = sin(angle).toFloat() * speed - 0.05f,
                life = 1f,
                maxLife = Random.nextFloat() * 150f + 200f,
                color = noteColor.copy(alpha = 0.7f),
                baseSize = Random.nextFloat() * 1.5f + 0.8f,
                type = ParticleType.Trail
            )
        )
    }
}

/** 爆发阶段1：星芒放射 */
fun emitBurstRays(
    particles: MutableList<Particle>,
    centerX: Float,
    centerY: Float,
    color: Color,
    rayCount: Int = 10
) {
    repeat(rayCount) { i ->
        val angle = (i / rayCount.toFloat()) * kotlin.math.PI * 2 + Random.nextFloat() * 0.2f
        val speed = Random.nextFloat() * 0.3f + 0.4f
        val length = Random.nextFloat() * 20f + 20f
        particles.add(
            Particle(
                x = centerX,
                y = centerY,
                vx = cos(angle).toFloat() * speed,
                vy = sin(angle).toFloat() * speed,
                life = 1f,
                maxLife = 120f,
                color = color.copy(alpha = 0.9f),
                baseSize = length,
                type = ParticleType.BurstRay,
                rotation = angle.toFloat(),
                scale = 1f,
                extraData = length
            )
        )
    }
}

/** 爆发阶段2：环形扩散 */
fun emitBurstRing(
    particles: MutableList<Particle>,
    centerX: Float,
    centerY: Float,
    color: Color
) {
    particles.add(
        Particle(
            x = centerX,
            y = centerY,
            vx = 0.2f,  // 扩散速度
            vy = 0f,
            life = 1f,
            maxLife = 180f,
            color = color.copy(alpha = 0.6f),
            baseSize = 3f,  // 线宽
            type = ParticleType.BurstRing,
            extraData = 60f  // 最大半径
        )
    )
}

/** 爆发阶段3：扇形喷射 */
fun emitBurstSparks(
    particles: MutableList<Particle>,
    centerX: Float,
    centerY: Float,
    color: Color,
    count: Int
) {
    repeat(count) {
        val angle = Random.nextFloat() * kotlin.math.PI * 0.8f + kotlin.math.PI * 0.6f  // 斜上方扇形
        val speed = Random.nextFloat() * 0.35f + 0.2f
        particles.add(
            Particle(
                x = centerX + Random.nextFloat() * 6f - 3f,
                y = centerY + Random.nextFloat() * 4f - 2f,
                vx = cos(angle).toFloat() * speed,
                vy = sin(angle).toFloat() * speed,
                life = 1f,
                maxLife = Random.nextFloat() * 150f + 300f,
                color = color.copy(alpha = 0.85f),
                baseSize = Random.nextFloat() * 2f + 1f,
                type = ParticleType.BurstSpark
            )
        )
    }
}

/** 长音符持续流：向上飘散粒子 */
fun emitSustainParticles(
    particles: MutableList<Particle>,
    keyX: Float,
    hitLineY: Float,
    noteColor: Color,
    count: Int
) {
    repeat(count) {
        val angle = Random.nextFloat() * kotlin.math.PI * 0.5f + kotlin.math.PI * 0.25f  // 向上扇形
        val speed = Random.nextFloat() * 0.15f + 0.08f
        particles.add(
            Particle(
                x = keyX + Random.nextFloat() * 8f - 4f,
                y = hitLineY + Random.nextFloat() * 4f,
                vx = cos(angle).toFloat() * speed + Random.nextFloat() * 0.04f - 0.02f,
                vy = -sin(angle).toFloat() * speed - 0.1f,
                life = 1f,
                maxLife = Random.nextFloat() * 200f + 400f,
                color = noteColor.copy(alpha = 0.6f),
                baseSize = Random.nextFloat() * 2f + 1f,
                type = ParticleType.Sustain
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
    val burstStates = remember { mutableMapOf<Long, BurstState>() }
    var lastProgress by remember { mutableLongStateOf(-1L) }
    var trailFrameCounter by remember { mutableIntStateOf(0) }

    // 长音符脉冲计时：keyIndex -> 上次脉冲时间
    val sustainPulseTimers = remember { mutableMapOf<Float, Long>() }

    // 粒子更新驱动
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            frameTick++
            trailFrameCounter++
            // 清理死亡粒子
            particles.removeAll { it.life <= 0 }
            // 更新存活粒子
            particles.forEach { it.update(16f) }
            // 清理过期 burst state
            burstStates.entries.removeAll { (_, state) ->
                currentProgressMillis - state.startTime > 1000
            }
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

        // 检测新触发音符并启动爆发状态机
        if (lastProgress != currentProgressMillis && lastProgress >= 0) {
            notes.forEach { note ->
                val triggerId = note.startTimeMillis * 1000L + (note.keyIndex * 100).toLong()
                if (note.startTimeMillis in lastProgress..currentProgressMillis) {
                    if (!triggeredNotes.contains(triggerId)) {
                        triggeredNotes.add(triggerId)
                        // 启动爆发状态机
                        if (EffectConfig.burstEnabled) {
                            burstStates[triggerId] = BurstState(startTime = currentProgressMillis)
                        }
                    }
                }
            }
            // 清理过期记录
            triggeredNotes.removeAll { id ->
                val noteTime = id / 1000L
                noteTime < currentProgressMillis - 10000
            }
            burstStates.entries.removeAll { (id, state) ->
                currentProgressMillis - state.startTime > 800
            }
        }
        if (lastProgress != currentProgressMillis) {
            lastProgress = currentProgressMillis
        }

        // 处理爆发状态机（分阶段发射）
        if (EffectConfig.burstEnabled) {
            burstStates.forEach { (triggerId, state) ->
                val keyIndex = ((triggerId % 1000L) / 100).toFloat()
                val keyX = (keyIndex + 0.5f) * whiteKeyWidth + offsetX
                val elapsed = currentProgressMillis - state.startTime

                // 阶段1：星芒放射（0ms）
                if (!state.stage1Emitted && elapsed >= 0) {
                    state.stage1Emitted = true
                    val rayCount = if (EffectConfig.level == EffectLevel.HIGH) 12 else 8
                    emitBurstRays(particles, keyX, hitLineY, Color.White.copy(alpha = 0.9f), rayCount)
                }

                // 阶段2：环形扩散（50ms）
                if (!state.stage2Emitted && elapsed >= 50) {
                    state.stage2Emitted = true
                    emitBurstRing(particles, keyX, hitLineY, Color(0xFFFFF0C0).copy(alpha = 0.7f))
                }

                // 阶段3：扇形喷射（100ms）
                if (!state.stage3Emitted && elapsed >= 100) {
                    state.stage3Emitted = true
                    val count = EffectConfig.burstSparkCount
                    if (count > 0) {
                        emitBurstSparks(particles, keyX, hitLineY, Color(0xFFFFF8E0), count)
                    }
                }
            }
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

            // 拖尾粒子发射（从音符后缘）
            if (EffectConfig.trailEnabled && trailFrameCounter % EffectConfig.trailEmitInterval == 0) {
                emitTrailParticles(particles, left, right, noteTopY, note.color, 1)
            }

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

        // 长音符持续流脉冲发射
        if (EffectConfig.sustainEnabled) {
            notes.forEach { note ->
                if (note.isLongNote && currentProgressMillis in note.startTimeMillis..note.endTimeMillis) {
                    val keyX = (note.keyIndex + 0.5f) * whiteKeyWidth + offsetX
                    val lastPulse = sustainPulseTimers[note.keyIndex] ?: 0L
                    if (currentProgressMillis - lastPulse >= EffectConfig.sustainPulseIntervalMs) {
                        sustainPulseTimers[note.keyIndex] = currentProgressMillis
                        emitSustainParticles(
                            particles,
                            keyX,
                            hitLineY,
                            note.color,
                            EffectConfig.sustainParticlesPerPulse
                        )
                    }
                }
            }
        }

        // 绘制粒子（根据类型绘制不同形态）
        particles.forEach { p ->
            if (p.life <= 0) return@forEach

            val px = p.x + offsetX
            if (px < -50f || px > canvasWidth + 50f || p.y < -50f || p.y > canvasHeight + 50f) return@forEach

            when (p.type) {
                ParticleType.Trail, ParticleType.BurstSpark, ParticleType.Sustain -> {
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha * 0.8f),
                        radius = p.currentSize,
                        center = Offset(px, p.y)
                    )
                }
                ParticleType.BurstRay -> {
                    val cos = kotlin.math.cos(p.rotation)
                    val sin = kotlin.math.sin(p.rotation)
                    val halfLen = p.currentSize * 0.5f
                    drawLine(
                        color = p.color.copy(alpha = p.alpha),
                        start = Offset(px - cos * halfLen * 0.2f, p.y - sin * halfLen * 0.2f),
                        end = Offset(px + cos * halfLen, p.y + sin * halfLen),
                        strokeWidth = 2.5f
                    )
                }
                ParticleType.BurstRing -> {
                    val radius = p.extraData * (1f - p.life * 0.5f)
                    if (radius > 0) {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha),
                            radius = radius,
                            center = Offset(px, p.y),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = p.currentSize.coerceAtLeast(0.5f))
                        )
                    }
                }
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
