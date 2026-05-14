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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.ui.theme.AccentAmber
import com.xianliticn.yuefu.ui.theme.CoreColor0
import com.xianliticn.yuefu.ui.theme.CoreColor1
import com.xianliticn.yuefu.ui.theme.CoreColor2
import com.xianliticn.yuefu.ui.theme.CoreColor3
import com.xianliticn.yuefu.ui.theme.HitLineGlow
import com.xianliticn.yuefu.ui.theme.HitLineShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    onNoteHit: (keyIndex: Float, color: Color) -> Unit = { _, _ -> }
) {
    val pixelsPerMillis = pixelsPerSecond / 1000f

    // 粒子状态
    val particles = remember { mutableStateListOf<Particle>() }
    var frameCounter by remember { mutableIntStateOf(0) }
    var lastProcessedProgress by remember { mutableLongStateOf(-1L) }
    var lastSustainPulse by remember { mutableLongStateOf(0L) }
    val animationTick = remember { mutableIntStateOf(0) }
    var canvasHeightPx by remember { mutableFloatStateOf(0f) }
    val noteHistories = remember { mutableMapOf<Long, ArrayDeque<NoteHistoryPoint>>() }

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
                p.velocityAngle = kotlin.math.atan2(p.vy, p.vx)
                p.life -= p.decayPerMs * delta
                if (p.life <= 0) {
                    particles.removeAt(i)
                }
            }

            // 限制粒子总数
            while (particles.size > 400) {
                particles.removeAt(0)
            }

            if (hitLineY > 0) {
                // 检查新触发，直接生成 burst
                if (lastProcessedProgress >= 0 && now > lastProcessedProgress) {
                    val newlyTriggered = notesList.filter {
                        it.startTimeMillis > lastProcessedProgress && it.startTimeMillis <= now
                    }
                    newlyTriggered.forEach { note ->
                        if (level != EffectLevel.LOW) {
                            spawnBurst(particles, note, whiteKeyWidth, hitLineY, level)
                        }
                        onNoteHit(note.keyIndex, note.color)
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

                // 历史采样（每2帧）
                if (frameCounter % 2 == 0) {
                    val ppm = pixelsPerSecond / 1000f
                    notesList.forEach { note ->
                        if (note.startTimeMillis <= now && note.endTimeMillis > now) {
                            val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
                            val noteStartDistance = (note.startTimeMillis - now) * ppm
                            val headY = hitLineY - noteStartDistance
                            val deque = noteHistories.getOrPut(note.startTimeMillis) { ArrayDeque() }
                            deque.addLast(
                                NoteHistoryPoint(
                                    x = rawMid,
                                    y = headY,
                                    width = whiteKeyWidth * 0.44f * 0.45f,
                                    color = note.color
                                )
                            )
                            while (deque.size > 16) deque.removeFirst()
                        }
                    }
                    noteHistories.keys.filter { key ->
                        notesList.none { it.startTimeMillis == key }
                    }.forEach { noteHistories.remove(it) }
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

            }

            lastProcessedProgress = now

            // 触发重组以重绘粒子
            if (particles.isNotEmpty()) {
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
        val cornerRadiusPx = 6.dp.toPx()
        val minNoteHeightPx = 4.dp.toPx()
        notes.forEach { note ->
            if (note.endTimeMillis < minTime - bufferTime || note.startTimeMillis > maxTime + bufferTime) {
                return@forEach
            }

            val rawMid = (note.keyIndex + 0.5f) * whiteKeyWidth
            val mid = rawMid + offsetX
            val width = whiteKeyWidth * 0.44f

            if (mid + width / 2f < 0 || mid - width / 2f > canvasWidth) {
                return@forEach
            }

            val noteEndDistance = (note.endTimeMillis - currentProgressMillis) * pixelsPerMillis
            val noteStartDistance = (note.startTimeMillis - currentProgressMillis) * pixelsPerMillis

            val noteBottomY = hitLineY - noteStartDistance
            val noteTopY = hitLineY - noteEndDistance
            val rawHeight = noteBottomY - noteTopY
            val height = rawHeight.coerceAtLeast(minNoteHeightPx)
            val drawTopY = noteBottomY - height

            val noteLeft = mid - width / 2f
            val noteRight = mid + width / 2f

            // ── Layer 1: 外层 radial gradient 光晕(halo) ──
            if (effectLevel == EffectLevel.HIGH) {
                val haloR = maxOf(width * 1.6f, height * 0.4f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            note.color.copy(alpha = 0.22f),
                            note.color.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(mid, drawTopY + height / 2f),
                        radius = haloR
                    ),
                    radius = haloR,
                    center = Offset(mid, drawTopY + height / 2f)
                )
            }

            // ── Layer 2: 主体(纵向渐变,尾淡头实) ──
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    note.color.copy(alpha = 0.40f),
                    note.color.copy(alpha = 0.75f),
                    note.color.copy(alpha = 0.95f),
                    note.color
                ),
                startY = drawTopY,
                endY = noteBottomY
            )

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x = noteLeft, y = drawTopY),
                size = Size(width = width, height = height),
                cornerRadius = CornerRadius(cornerRadiusPx)
            )

            // ── Layer 3: 内层亮芯线 ──
            val coreColor = when (note.partId % 4) {
                0 -> CoreColor0
                1 -> CoreColor1
                2 -> CoreColor2
                else -> CoreColor3
            }
            val coreY = drawTopY + 2.5f.dp.toPx()
            if (coreY < noteBottomY - cornerRadiusPx && height > cornerRadiusPx * 2) {
                drawLine(
                    color = coreColor.copy(alpha = 0.85f),
                    start = Offset(x = mid - width * 0.25f, y = coreY),
                    end = Offset(x = mid + width * 0.25f, y = coreY),
                    strokeWidth = 1.3f.dp.toPx()
                )
            }

            // ── Layer 4: 底部白色微光(水滴质感) ──
            val microGlowHeight = 8f.dp.toPx().coerceAtMost(height * 0.4f)
            if (microGlowHeight > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f)),
                        startY = noteBottomY - microGlowHeight,
                        endY = noteBottomY
                    ),
                    topLeft = Offset(x = noteLeft + 2f.dp.toPx(), y = noteBottomY - microGlowHeight),
                    size = Size(width = width - 4f.dp.toPx(), height = microGlowHeight)
                )
            }
        }

        // ── 绘制音符记忆拖尾 ──
        noteHistories.forEach { (_, deque) ->
            if (deque.size < 3) return@forEach
            val pts = deque.toList()
            for (i in 0 until pts.size - 1) {
                val ratio = (i + 1).toFloat() / pts.size
                val alpha = ratio * 0.18f
                val strokeW = pts[i].width * ratio
                val px1 = pts[i].x + offsetX
                val px2 = pts[i + 1].x + offsetX
                drawLine(
                    color = pts[i].color.copy(alpha = alpha),
                    start = Offset(px1, pts[i].y),
                    end = Offset(px2, pts[i + 1].y),
                    strokeWidth = strokeW.coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
            }
        }

        // ── 柔光地平线判定线 ──
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    HitLineGlow.copy(alpha = 0.20f),
                    HitLineGlow.copy(alpha = 0.35f),
                    HitLineGlow.copy(alpha = 0.20f)
                ),
                startX = 0f,
                endX = canvasWidth
            ),
            start = Offset(0f, hitLineY),
            end = Offset(canvasWidth, hitLineY),
            strokeWidth = 1.5f.dp.toPx()
        )
        drawLine(
            color = HitLineShadow.copy(alpha = 0.15f),
            start = Offset(0f, hitLineY + 1.5f.dp.toPx()),
            end = Offset(canvasWidth, hitLineY + 1.5f.dp.toPx()),
            strokeWidth = 3f.dp.toPx()
        )

        // 绘制粒子
        particles.forEach { p ->
            val px = p.x + offsetX
            val py = p.y
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (alpha <= 0f) return@forEach

            when (p.type) {
                ParticleType.Trail, ParticleType.Sustain -> {
                    val speed = kotlin.math.sqrt(p.vx * p.vx + p.vy * p.vy)
                    val angle = p.velocityAngle
                    val rx = p.size * (0.8f + speed * 1.5f)
                    val ry = p.size * 0.4f
                    withTransform({
                        translate(left = px, top = py)
                        rotate(angle * 180f / kotlin.math.PI.toFloat())
                    }) {
                        drawOval(
                            color = p.color.copy(alpha = alpha * 0.6f),
                            topLeft = Offset(-rx, -ry),
                            size = Size(rx * 2, ry * 2)
                        )
                    }
                }
                ParticleType.BurstSpark -> {
                    val len = p.size * (2.5f + 2f * alpha)
                    val angle = p.velocityAngle
                    withTransform({
                        translate(left = px, top = py)
                        rotate(angle * 180f / kotlin.math.PI.toFloat())
                    }) {
                        drawLine(
                            color = p.color.copy(alpha = alpha * 0.9f),
                            start = Offset(-len * 0.3f, 0f),
                            end = Offset(len * 0.7f, 0f),
                            strokeWidth = 1.8f * alpha,
                            cap = StrokeCap.Round
                        )
                    }
                }
                ParticleType.BurstRay -> {
                    val len = p.size * (2.5f + alpha)
                    val angle = p.velocityAngle
                    withTransform({
                        translate(left = px, top = py)
                        rotate(angle * 180f / kotlin.math.PI.toFloat())
                    }) {
                        drawLine(
                            color = p.color.copy(alpha = alpha * 0.9f),
                            start = Offset(0f, 0f),
                            end = Offset(len, 0f),
                            strokeWidth = 2f * alpha,
                            cap = StrokeCap.Round
                        )
                    }
                }
                ParticleType.BurstRing -> {
                    val radius = p.size + (60f - p.size) * (1f - alpha)
                    drawCircle(
                        color = p.color.copy(alpha = alpha * 0.5f),
                        radius = radius,
                        center = Offset(px, py),
                        style = Stroke(width = 2.5f * alpha)
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
    val decayPerMs: Float = 1f,
    val color: Color,
    val size: Float,
    val type: ParticleType,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var velocityAngle: Float = 0f
)

data class NoteHistoryPoint(
    val x: Float,
    val y: Float,
    val width: Float,
    val color: Color
)

private fun spawnBurst(
    particles: MutableList<Particle>,
    note: VisualNoteEvent,
    whiteKeyWidth: Float,
    hitLineY: Float,
    level: EffectLevel
) {
    val centerX = (note.keyIndex + 0.5f) * whiteKeyWidth
    val centerY = hitLineY
    val color = note.color

    // 6 trail 粒子
    repeat(6) {
        val angle = kotlin.random.Random.nextFloat() * kotlin.math.PI * 2
        val speed = 0.5f + kotlin.random.Random.nextFloat() * 2.5f
        particles.add(
            Particle(
                x = centerX, y = centerY,
                vx = kotlin.math.cos(angle).toFloat() * speed,
                vy = kotlin.math.sin(angle).toFloat() * speed,
                life = 800f + kotlin.random.Random.nextFloat() * 400f,
                maxLife = 1200f,
                decayPerMs = 0.8f,
                color = color,
                size = 2f + kotlin.random.Random.nextFloat() * 4f,
                type = ParticleType.Trail,
                rotation = kotlin.random.Random.nextFloat() * kotlin.math.PI.toFloat() * 2f,
                rotationSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 0.15f
            )
        )
    }

    // 4 sustain 粒子
    repeat(4) {
        val angle = -kotlin.math.PI / 2 + (kotlin.random.Random.nextFloat() - 0.5f) * 1.2f
        val speed = 0.5f + kotlin.random.Random.nextFloat() * 2.5f
        particles.add(
            Particle(
                x = centerX, y = centerY,
                vx = kotlin.math.cos(angle).toFloat() * speed,
                vy = kotlin.math.sin(angle).toFloat() * speed,
                life = 800f + kotlin.random.Random.nextFloat() * 400f,
                maxLife = 1200f,
                decayPerMs = 0.8f,
                color = color,
                size = 2f + kotlin.random.Random.nextFloat() * 4f,
                type = ParticleType.Sustain,
                rotation = kotlin.random.Random.nextFloat() * kotlin.math.PI.toFloat() * 2f,
                rotationSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 0.15f
            )
        )
    }

    // burst sparks
    val burstCount = when (level) {
        EffectLevel.LOW -> 0
        EffectLevel.MEDIUM -> 12
        EffectLevel.HIGH -> 18
    }
    repeat(burstCount) {
        val angle = -kotlin.math.PI * 2 / 3 + kotlin.random.Random.nextFloat() * kotlin.math.PI / 3
        val speed = 0.3f + kotlin.random.Random.nextFloat() * 0.3f
        particles.add(
            Particle(
                x = centerX + (kotlin.random.Random.nextFloat() - 0.5f) * 8f,
                y = centerY + (kotlin.random.Random.nextFloat() - 0.5f) * 4f,
                vx = kotlin.math.cos(angle).toFloat() * speed,
                vy = kotlin.math.sin(angle).toFloat() * speed,
                life = 600f + kotlin.random.Random.nextFloat() * 200f,
                maxLife = 800f,
                decayPerMs = 1.0f,
                color = if (kotlin.random.Random.nextBoolean()) AccentAmber else color,
                size = 2.5f + kotlin.random.Random.nextFloat() * 2f,
                type = ParticleType.BurstSpark,
                rotation = kotlin.random.Random.nextFloat() * kotlin.math.PI.toFloat() * 2f,
                rotationSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 0.15f
            )
        )
    }

    // 8 rays
    val rayCount = 8
    repeat(rayCount) { i ->
        val angle = kotlin.math.PI * 2 * i / rayCount
        particles.add(
            Particle(
                x = centerX, y = centerY,
                vx = kotlin.math.cos(angle).toFloat() * 1.8f,
                vy = kotlin.math.sin(angle).toFloat() * 1.8f,
                life = 600f,
                maxLife = 600f,
                decayPerMs = 1.6f,
                color = AccentAmber,
                size = 12f + kotlin.random.Random.nextFloat() * 8f,
                type = ParticleType.BurstRay,
                rotation = angle.toFloat()
            )
        )
    }

    // 1 ring
    particles.add(
        Particle(
            x = centerX, y = centerY,
            vx = 0f, vy = 0f,
            life = 1000f,
            maxLife = 1000f,
            decayPerMs = 1.0f,
            color = AccentAmber,
            size = 8f,
            type = ParticleType.BurstRing
        )
    )
}

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

    repeat(1 + kotlin.random.Random.nextInt(2)) {
        val angle = kotlin.random.Random.nextFloat() * kotlin.math.PI * 2
        val speed = 0.05f + kotlin.random.Random.nextFloat() * 0.1f
        particles.add(
            Particle(
                x = rawMid + (kotlin.random.Random.nextFloat() - 0.5f) * whiteKeyWidth * 0.25f,
                y = noteTopY + (kotlin.random.Random.nextFloat() - 0.5f) * 5f,
                vx = kotlin.math.cos(angle).toFloat() * speed,
                vy = kotlin.math.sin(angle).toFloat() * speed - 0.05f,
                life = 500f + kotlin.random.Random.nextFloat() * 300f,
                maxLife = 800f,
                decayPerMs = 1.0f,
                color = note.color.copy(alpha = 0.6f),
                size = 1.2f + kotlin.random.Random.nextFloat(),
                type = ParticleType.Trail
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
    repeat(3 + kotlin.random.Random.nextInt(3)) {
        val angle = -kotlin.math.PI / 2 + (kotlin.random.Random.nextFloat() - 0.5f) * 0.6f
        val speed = 0.08f + kotlin.random.Random.nextFloat() * 0.12f
        particles.add(
            Particle(
                x = centerX + (kotlin.random.Random.nextFloat() - 0.5f) * whiteKeyWidth * 0.15f,
                y = hitLineY,
                vx = kotlin.math.cos(angle).toFloat() * speed,
                vy = kotlin.math.sin(angle).toFloat() * speed,
                life = 600f + kotlin.random.Random.nextFloat() * 400f,
                maxLife = 1000f,
                decayPerMs = 0.8f,
                color = note.color.copy(alpha = 0.7f),
                size = 1.8f + kotlin.random.Random.nextFloat() * 1.5f,
                type = ParticleType.Sustain
            )
        )
    }
}

data class NoteFlowVisibleRange(
    val startPx: Float,
    val endPx: Float
)
