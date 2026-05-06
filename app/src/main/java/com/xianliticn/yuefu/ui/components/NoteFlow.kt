package com.xianliticn.yuefu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.music.VisualNoteEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NoteFlow(
    modifier: Modifier = Modifier,
    whiteKeyWidth: Float,
    keyCount: Int,
    notes: List<VisualNoteEvent>,
    pixelsPerSecond: Float = 300f,
    currentProgressMillis: Long,
    visibleRange: NoteFlowVisibleRange,
    effectLevel: EffectLevel = EffectLevel.HIGH
) {
    val pixelsPerMillis = pixelsPerSecond / 1000f

    // 粒子状态
    val particles = remember { mutableStateListOf<Particle>() }
    val burstStates = remember { mutableMapOf<Long, BurstState>() }
    var frameCounter by remember { mutableIntStateOf(0) }
    var lastProcessedProgress by remember { mutableLongStateOf(-1L) }
    var lastSustainPulse by remember { mutableLongStateOf(0L) }
    val animationTick = remember { mutableIntStateOf(0) }
    var canvasHeightPx by remember { mutableFloatStateOf(0f) }

    val currentProgressState = rememberUpdatedState(currentProgressMillis)
    val notesState = rememberUpdatedState(notes)
    val levelState = rememberUpdatedState(effectLevel)
    val canvasHeightState = rememberUpdatedState(canvasHeightPx)

    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            delay(16)

            val now = currentProgressState.value
            val notesList = notesState.value
            val level = levelState.value
            val hitLineY = canvasHeightState.value

            val delta = if (lastProcessedProgress >= 0) {
                (now - lastProcessedProgress).toFloat()
            } else {
                16f
            }

            // 更新现有粒子
            for (i in particles.size - 1 downTo 0) {
                val p = particles[i]
                p.x += p.vx * delta
                p.y += p.vy * delta
                p.vy += 0.00015f * delta
                p.rotation += p.rotationSpeed * delta
                p.life -= delta
                if (p.life <= 0) {
                    particles.removeAt(i)
                }
            }

            // 限制粒子总数
            while (particles.size > 300) {
                particles.removeAt(0)
            }

            if (hitLineY > 0) {
                // 检查新触发
                if (lastProcessedProgress >= 0 && now > lastProcessedProgress) {
                    val newlyTriggered = notesList.filter {
                        it.startTimeMillis > lastProcessedProgress && it.startTimeMillis <= now
                    }
                    newlyTriggered.forEach { note ->
                        if (level != EffectLevel.LOW) {
                            burstStates[note.startTimeMillis] = BurstState(
                                startTime = now,
                                keyIndex = note.keyIndex,
                                color = note.color
                            )
                        }
                    }
                }

                // 拖尾粒子
                frameCounter++
                val trailInterval = when (level) {
                    EffectLevel.LOW -> 6
                    EffectLevel.MEDIUM -> 4
                    EffectLevel.HIGH -> 3
                }
                if (frameCounter % trailInterval == 0) {
                    notesList.filter {
                        it.startTimeMillis <= now && it.endTimeMillis > now
                    }.forEach { note ->
                        emitTrailParticles(
                            particles, note, whiteKeyWidth, hitLineY, pixelsPerMillis, now
                        )
                    }
                }

                // 持续流粒子
                if (level == EffectLevel.HIGH) {
                    val pulseInterval = 200L
                    val currentPulse = now / pulseInterval
                    val lastPulse = lastSustainPulse / pulseInterval
                    if (currentPulse > lastPulse) {
                        notesList.filter {
                            it.isLongNote && it.startTimeMillis <= now && it.endTimeMillis > now
                        }.forEach { note ->
                            emitSustainParticles(particles, note, whiteKeyWidth, hitLineY)
                        }
                    }
                    lastSustainPulse = now
                }

                // Burst 阶段
                val iterator = burstStates.iterator()
                while (iterator.hasNext()) {
                    val (_, state) = iterator.next()
                    val elapsed = now - state.startTime

                    if (!state.stage1Emitted && elapsed >= 0) {
                        emitBurstStage1(particles, state, whiteKeyWidth, hitLineY)
                        state.stage1Emitted = true
                    }
                    if (!state.stage2Emitted && elapsed >= 50) {
                        emitBurstStage2(particles, state, whiteKeyWidth, hitLineY)
                        state.stage2Emitted = true
                    }
                    if (!state.stage3Emitted && elapsed >= 100) {
                        emitBurstStage3(particles, state, whiteKeyWidth, hitLineY, level)
                        state.stage3Emitted = true
                    }
                    if (elapsed > 800) {
                        iterator.remove()
                    }
                }
            }

            lastProcessedProgress = now

            // 触发重组以重绘粒子
            if (particles.isNotEmpty() || burstStates.isNotEmpty()) {
                animationTick.value++
            }
        }
    }

    // 读取 state 以确保重组时重绘
    val _particleSize = particles.size
    val _tick = animationTick.value

    Canvas(modifier = modifier
        .fillMaxWidth()
        .onSizeChanged { canvasHeightPx = it.height.toFloat() }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val hitLineY = canvasHeight

        // 视口时间范围（用于剔除不可见音符）
        val visibleDurationMillis = (canvasHeight / pixelsPerMillis).toLong()
        val minTime = currentProgressMillis
        val maxTime = currentProgressMillis + visibleDurationMillis
        val bufferTime = 500L

        val offsetX = -visibleRange.startPx

        // 绘制音符
        notes.forEach { note ->
            if (note.endTimeMillis < minTime - bufferTime || note.startTimeMillis > maxTime + bufferTime) {
                return@forEach
            }

            val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
            val mid = rawMid + offsetX
            val width = whiteKeyWidth * 0.4f

            if (mid + width < 0 || mid > canvasWidth) {
                return@forEach
            }

            val noteEndDistance = (note.endTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteStartDistance = (note.startTimeMillis - currentProgressMillis) * pixelsPerMillis

            val noteBottomY = hitLineY - noteStartDistance
            val noteTopY = hitLineY - noteEndDistance
            val height = noteBottomY - noteTopY

            drawRoundRect(
                color = note.color,
                topLeft = Offset(x = mid - width / 2f, y = noteTopY),
                size = Size(width = width, height = height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }

        // 绘制粒子
        particles.forEach { p ->
            val px = p.x + offsetX
            val py = p.y
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)

            when (p.type) {
                ParticleType.Trail, ParticleType.Sustain -> {
                    val radius = p.size * (0.5f + 0.5f * alpha)
                    drawCircle(
                        color = p.color.copy(alpha = alpha * 0.7f),
                        radius = radius,
                        center = Offset(px, py)
                    )
                }
                ParticleType.BurstSpark -> {
                    val radius = p.size * (0.3f + 0.7f * alpha)
                    drawCircle(
                        color = p.color.copy(alpha = alpha * 0.9f),
                        radius = radius,
                        center = Offset(px, py)
                    )
                }
                ParticleType.BurstRay -> {
                    val length = p.size * (2f + 1f * alpha)
                    val endX = px + cos(p.rotation) * length
                    val endY = py + sin(p.rotation) * length
                    drawLine(
                        color = p.color.copy(alpha = alpha * 0.8f),
                        start = Offset(px, py),
                        end = Offset(endX, endY),
                        strokeWidth = 2f
                    )
                }
                ParticleType.BurstRing -> {
                    val radius = p.size + (60f - p.size) * (1f - alpha)
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha * 0.6f),
                        radius = radius,
                        center = Offset(px, py),
                        style = Stroke(width = 3f * alpha)
                    )
                }
            }
        }
    }
}

enum class ParticleType {
    Trail, BurstRay, BurstRing, BurstSpark, Sustain
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val size: Float,
    val type: ParticleType,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f
)

data class BurstState(
    val startTime: Long,
    val keyIndex: Float,
    val color: Color,
    var stage1Emitted: Boolean = false,
    var stage2Emitted: Boolean = false,
    var stage3Emitted: Boolean = false
)

private fun emitTrailParticles(
    particles: MutableList<Particle>,
    note: VisualNoteEvent,
    whiteKeyWidth: Float,
    hitLineY: Float,
    pixelsPerMillis: Float,
    currentTime: Long
) {
    val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
    val noteEndDistance = (note.endTimeMillis - currentTime) * pixelsPerMillis
    val noteTopY = hitLineY - noteEndDistance

    val count = 1 + kotlin.random.Random.nextInt(2)
    repeat(count) {
        val angle = kotlin.random.Random.nextFloat() * kotlin.math.PI * 2
        val speed = 0.05f + kotlin.random.Random.nextFloat() * 0.1f
        particles.add(
            Particle(
                x = rawMid + (kotlin.random.Random.nextFloat() - 0.5f) * whiteKeyWidth * 0.25f,
                y = noteTopY + (kotlin.random.Random.nextFloat() - 0.5f) * 5f,
                vx = cos(angle).toFloat() * speed,
                vy = (sin(angle).toFloat() * speed - 0.05f),
                life = 300f + kotlin.random.Random.nextFloat() * 200f,
                maxLife = 500f,
                color = note.color.copy(alpha = 0.6f),
                size = 1f + kotlin.random.Random.nextFloat(),
                type = ParticleType.Trail
            )
        )
    }
}

private fun emitBurstStage1(
    particles: MutableList<Particle>,
    state: BurstState,
    whiteKeyWidth: Float,
    hitLineY: Float
) {
    val centerX = (state.keyIndex + 0.5f) * whiteKeyWidth
    val centerY = hitLineY
    val rayCount = 8 + kotlin.random.Random.nextInt(5)

    repeat(rayCount) { i ->
        val angle = kotlin.math.PI * 2 * i / rayCount
        particles.add(
            Particle(
                x = centerX,
                y = centerY,
                vx = cos(angle).toFloat() * 0.15f,
                vy = sin(angle).toFloat() * 0.15f,
                life = 150f,
                maxLife = 150f,
                color = Color.White.copy(alpha = 0.85f),
                size = 15f + kotlin.random.Random.nextFloat() * 10f,
                type = ParticleType.BurstRay,
                rotation = angle.toFloat()
            )
        )
    }
}

private fun emitBurstStage2(
    particles: MutableList<Particle>,
    state: BurstState,
    whiteKeyWidth: Float,
    hitLineY: Float
) {
    val centerX = (state.keyIndex + 0.5f) * whiteKeyWidth
    val centerY = hitLineY
    particles.add(
        Particle(
            x = centerX,
            y = centerY,
            vx = 0.2f,
            vy = 0f,
            life = 200f,
            maxLife = 200f,
            color = Color(0xFFFFD700).copy(alpha = 0.7f),
            size = 10f,
            type = ParticleType.BurstRing
        )
    )
}

private fun emitBurstStage3(
    particles: MutableList<Particle>,
    state: BurstState,
    whiteKeyWidth: Float,
    hitLineY: Float,
    level: EffectLevel
) {
    val centerX = (state.keyIndex + 0.5f) * whiteKeyWidth
    val centerY = hitLineY
    val count = when (level) {
        EffectLevel.LOW -> 0
        EffectLevel.MEDIUM -> 15
        EffectLevel.HIGH -> 25
    }

    repeat(count) {
        val angle = -kotlin.math.PI * 2 / 3 + kotlin.random.Random.nextFloat() * kotlin.math.PI / 3
        val speed = 0.3f + kotlin.random.Random.nextFloat() * 0.3f
        particles.add(
            Particle(
                x = centerX + (kotlin.random.Random.nextFloat() - 0.5f) * 8f,
                y = centerY + (kotlin.random.Random.nextFloat() - 0.5f) * 4f,
                vx = cos(angle).toFloat() * speed,
                vy = sin(angle).toFloat() * speed,
                life = 400f + kotlin.random.Random.nextFloat() * 200f,
                maxLife = 600f,
                color = if (kotlin.random.Random.nextBoolean())
                    Color.White.copy(alpha = 0.9f)
                else
                    Color(0xFFFFD700).copy(alpha = 0.8f),
                size = 2f + kotlin.random.Random.nextFloat() * 2f,
                type = ParticleType.BurstSpark
            )
        )
    }
}

private fun emitSustainParticles(
    particles: MutableList<Particle>,
    note: VisualNoteEvent,
    whiteKeyWidth: Float,
    hitLineY: Float
) {
    val centerX = (note.keyIndex + 0.5f) * whiteKeyWidth
    val count = 3 + kotlin.random.Random.nextInt(3)

    repeat(count) {
        val angle = -kotlin.math.PI / 2 + (kotlin.random.Random.nextFloat() - 0.5f) * 0.6f
        val speed = 0.08f + kotlin.random.Random.nextFloat() * 0.12f
        particles.add(
            Particle(
                x = centerX + (kotlin.random.Random.nextFloat() - 0.5f) * whiteKeyWidth * 0.15f,
                y = hitLineY,
                vx = cos(angle).toFloat() * speed,
                vy = sin(angle).toFloat() * speed,
                life = 500f + kotlin.random.Random.nextFloat() * 300f,
                maxLife = 800f,
                color = note.color.copy(alpha = 0.7f),
                size = 1.5f + kotlin.random.Random.nextFloat() * 1.5f,
                type = ParticleType.Sustain
            )
        )
    }
}

data class NoteFlowVisibleRange(
    val startPx: Float,
    val endPx: Float
)
