package com.xianliticn.yuefu.ui.components

import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ── Data classes ────────────────────────────────────────────────────────────────

data class ImpactEvent(
    val bornNanos: Long,
    val centerX: Float,
    val color: Color
)

data class RingState(
    val bornNanos: Long,
    val centerX: Float,
    val color: Color
) {
    companion object {
        const val LIFE_MS = 800L
        const val RADIUS_START_DP = 8f
        const val RADIUS_END_DP = 70f
        const val ALPHA_MAX = 0.55f
        const val STROKE_WIDTH_DP = 1.2f
        const val MAX_POOL = 20
    }
}

data class DropletState(
    val bornNanos: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var lastFrameNanos: Long,
    val color: Color
) {
    companion object {
        const val LIFE_MS = 720L
        const val GRAVITY_DP = 500f
        const val INITIAL_VY_DP = -180f
        const val DOT_RADIUS_DP = 2.2f
        const val MAX_POOL = 15
    }
}

// ── State holder ────────────────────────────────────────────────────────────────

class ImpactStateHolder {
    val rings = mutableStateListOf<RingState>()
    val droplets = mutableStateListOf<DropletState>()

    var frameNanos by mutableLongStateOf(0L)

    val hasImpacts: Boolean get() = rings.isNotEmpty() || droplets.isNotEmpty()

    fun spawnImpact(event: ImpactEvent) {
        val nanoNow = event.bornNanos
        rings.add(RingState(nanoNow,              event.centerX, event.color))
        rings.add(RingState(nanoNow + 120_000_000L, event.centerX, event.color))
        rings.add(RingState(nanoNow + 240_000_000L, event.centerX, event.color))
        droplets.add(
            DropletState(
                bornNanos = nanoNow,
                x = event.centerX,
                y = 0f,
                vx = ((Math.random() - 0.5) * 8).toFloat(),
                vy = DropletState.INITIAL_VY_DP,
                lastFrameNanos = nanoNow,
                color = event.color
            )
        )
        while (rings.size > RingState.MAX_POOL) rings.removeAt(0)
        while (droplets.size > DropletState.MAX_POOL) droplets.removeAt(0)
    }

    fun tick(nowNanos: Long, density: Density, hitLineY: Float) {
        frameNanos = nowNanos

        val itR = rings.iterator()
        while (itR.hasNext()) {
            if ((nowNanos - itR.next().bornNanos) / 1_000_000 >= RingState.LIFE_MS) itR.remove()
        }

        val gravity = with(density) { DropletState.GRAVITY_DP.dp.toPx() }

        val itD = droplets.iterator()
        while (itD.hasNext()) {
            val d = itD.next()
            val age = (nowNanos - d.bornNanos) / 1_000_000L
            if (age >= DropletState.LIFE_MS) { itD.remove(); continue }

            val dtSec = (nowNanos - d.lastFrameNanos) / 1_000_000_000f
            d.lastFrameNanos = nowNanos
            d.x += d.vx * dtSec
            d.y += d.vy * dtSec
            d.vy += gravity * dtSec

            if (d.y > hitLineY + with(density) { 4.dp.toPx() }) itD.remove()
        }
    }
}

@Composable
fun rememberImpactState(): ImpactStateHolder = remember { ImpactStateHolder() }

@Composable
fun AnimateImpactsLoop(
    impactHolder: ImpactStateHolder,
    hitLineY: Float,
    density: Density
) {
    val hasImpacts by remember {
        derivedStateOf { impactHolder.rings.isNotEmpty() || impactHolder.droplets.isNotEmpty() }
    }

    LaunchedEffect(hasImpacts, hitLineY) {
        if (hasImpacts) {
            while (impactHolder.rings.isNotEmpty() || impactHolder.droplets.isNotEmpty()) {
                withFrameNanos { nanos ->
                    impactHolder.tick(nanos, density, hitLineY)
                }
            }
        }
        impactHolder.frameNanos = 0L
    }
}

// ── DrawScope extensions ────────────────────────────────────────────────────────

fun DrawScope.drawRings(
    rings: List<RingState>,
    nowNanos: Long,
    offsetX: Float,
    hitLineY: Float,
    density: Density
) {
    if (rings.isEmpty()) return

    val ringPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    val glowPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    for (ring in rings) {
        val ageMs = (nowNanos - ring.bornNanos) / 1_000_000f
        if (ageMs < 0) continue
        val t = ageMs / RingState.LIFE_MS
        if (t >= 1f) continue

        val radiusStart = with(density) { RingState.RADIUS_START_DP.dp.toPx() }
        val radiusEnd = with(density) { RingState.RADIUS_END_DP.dp.toPx() }
        val radius = radiusStart + t * (radiusEnd - radiusStart)
        val alpha = RingState.ALPHA_MAX * (1f - t)

        val cx = ring.centerX + offsetX
        val cy = hitLineY

        if (cx < -radiusEnd || cx > size.width + radiusEnd) continue

        val r = (ring.color.red * 255).toInt()
        val g = (ring.color.green * 255).toInt()
        val b = (ring.color.blue * 255).toInt()
        val a = (alpha * 255).toInt()

        drawIntoCanvas { canvas ->
            ringPaint.strokeWidth = with(density) { RingState.STROKE_WIDTH_DP.dp.toPx() }
            ringPaint.setARGB(a, r, g, b)
            canvas.nativeCanvas.drawCircle(cx, cy, radius, ringPaint)

            val glowA = (alpha * 0.18f * 255).toInt()
            glowPaint.setARGB(glowA, r, g, b)
            canvas.nativeCanvas.drawCircle(cx, cy, radius * 0.75f, glowPaint)
        }
    }
}

fun DrawScope.drawDroplets(
    droplets: List<DropletState>,
    nowNanos: Long,
    offsetX: Float,
    density: Density
) {
    val dotRadius = with(density) { DropletState.DOT_RADIUS_DP.dp.toPx() }

    for (d in droplets) {
        val ageMs = (nowNanos - d.bornNanos) / 1_000_000f
        val t = ageMs / DropletState.LIFE_MS
        if (t >= 1f) continue

        val speedFactor = min(1f, abs(d.vy) / (with(density) { 220.dp.toPx() }))
        val stretch = 1f + speedFactor * 0.6f
        val squeeze = 1f - speedFactor * 0.18f
        val alpha = (1f - t) * 0.95f

        val px = d.x + offsetX
        val py = d.y

        val r = (d.color.red * 255).toInt()
        val g = (d.color.green * 255).toInt()
        val b = (d.color.blue * 255).toInt()

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.translate(px, py)
            nc.scale(squeeze, stretch)

            val dotPaint = Paint().apply {
                setARGB((alpha * 255).toInt(), r, g, b)
                style = Paint.Style.FILL
                isAntiAlias = true
                setShadowLayer(dotRadius * 2f, 0f, 0f,
                    android.graphics.Color.argb((alpha * 0.9f * 255).toInt(), r, g, b)
                )
            }
            nc.drawCircle(0f, 0f, dotRadius, dotPaint)

            val hlPaint = Paint().apply {
                setARGB((alpha * 0.7f * 255).toInt(), 255, 255, 255)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            nc.drawCircle(-dotRadius * 0.27f, -dotRadius * 0.32f, dotRadius * 0.32f, hlPaint)

            nc.restore()
        }
    }
}

// ── Background drift composable ─────────────────────────────────────────────────

@Composable
fun LiquidBackgroundDrift(modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition("liquid-bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec<Float>(animation = androidx.compose.animation.core.tween(
            durationMillis = 30_000,
            easing = LinearEasing
        ))
    )

    val paint1 = remember { Paint().apply { style = Paint.Style.FILL } }
    val paint2 = remember { Paint().apply { style = Paint.Style.FILL } }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height * 0.66f

        val deg1 = 95f
        val angle1 = Math.toRadians(deg1.toDouble())
        val dirX1 = cos(angle1).toFloat()
        val dirY1 = sin(angle1).toFloat()
        val period1 = with(density) { 90.dp.toPx() }
        val drift1 = phase * with(density) { 120.dp.toPx() }
        val s1x = dirX1 * drift1
        val s1y = dirY1 * drift1

        paint1.shader = android.graphics.LinearGradient(
            s1x, s1y,
            s1x + dirX1 * period1, s1y + dirY1 * period1,
            intArrayOf(
                android.graphics.Color.argb(20, 255, 180, 110),
                android.graphics.Color.argb(20, 255, 180, 110),
                android.graphics.Color.argb(0, 255, 180, 110),
                android.graphics.Color.argb(0, 255, 180, 110)
            ),
            floatArrayOf(0f, 0.44f, 0.44f, 1f),
            Shader.TileMode.REPEAT
        )

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, w, h, paint1)
        }

        val deg2 = 85f
        val angle2 = Math.toRadians(deg2.toDouble())
        val dirX2 = cos(angle2).toFloat()
        val dirY2 = sin(angle2).toFloat()
        val period2 = with(density) { 180.dp.toPx() }
        val drift2 = (1f - phase) * with(density) { 640.dp.toPx() }
        val s2x = dirX2 * drift2
        val s2y = dirY2 * drift2

        paint2.shader = android.graphics.LinearGradient(
            s2x, s2y,
            s2x + dirX2 * period2, s2y + dirY2 * period2,
            intArrayOf(
                android.graphics.Color.argb(15, 239, 200, 140),
                android.graphics.Color.argb(15, 239, 200, 140),
                android.graphics.Color.argb(0, 239, 200, 140),
                android.graphics.Color.argb(0, 239, 200, 140)
            ),
            floatArrayOf(0f, 0.22f, 0.22f, 1f),
            Shader.TileMode.REPEAT
        )

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, w, h, paint2)
        }
    }
}
