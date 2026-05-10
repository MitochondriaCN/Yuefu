package com.xianliticn.yuefu.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.ui.theme.PartColor0
import com.xianliticn.yuefu.ui.theme.PartColor1
import com.xianliticn.yuefu.ui.theme.PianoBlackDefault
import com.xianliticn.yuefu.ui.theme.PianoBlackGradientBottom
import com.xianliticn.yuefu.ui.theme.PianoBlackGradientTop
import com.xianliticn.yuefu.ui.theme.PianoBlackPressed
import com.xianliticn.yuefu.ui.theme.PianoBorder
import com.xianliticn.yuefu.ui.theme.PianoWhiteDefault
import com.xianliticn.yuefu.ui.theme.PianoWhiteGradientBottom
import com.xianliticn.yuefu.ui.theme.PianoWhiteGradientTop
import com.xianliticn.yuefu.ui.theme.PianoWhitePressed

@Composable
fun PianoKey(
    modifier: Modifier = Modifier,
    keyType: PianoKeyType = PianoKeyType.White,
    isPressed: Boolean = false,
    isResonating: Boolean = false,
    resonatingColor: Color? = null,
    onPressed: () -> Unit,
    onReleased: () -> Unit
) {
    val backgroundColor = when (keyType) {
        PianoKeyType.White ->
            if (isPressed) PianoWhitePressed else PianoWhiteDefault
        PianoKeyType.Black ->
            if (isPressed) PianoBlackPressed else PianoBlackDefault
    }
    val shadowElevation = if (isPressed) 2.dp else 4.dp

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onPressed()
                    tryAwaitRelease()
                    onReleased()
                }
            )
        }
    ) {
        if (isResonating && resonatingColor != null) {
            ResonanceGlow(
                modifier = Modifier.fillMaxSize(),
                color = resonatingColor,
                keyType = keyType
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(1.dp),
            shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
            color = backgroundColor,
            border = BorderStroke(1.dp, PianoBorder),
            shadowElevation = shadowElevation
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isPressed) {
                                if (keyType == PianoKeyType.Black)
                                    listOf(PianoBlackPressed, PianoBlackDefault)
                                else
                                    listOf(PianoWhitePressed, PianoWhiteDefault)
                            } else {
                                if (keyType == PianoKeyType.Black)
                                    listOf(PianoBlackGradientTop, PianoBlackGradientBottom)
                                else
                                    listOf(PianoWhiteGradientTop, PianoWhiteGradientBottom)
                            }
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x33000000))
                            )
                        )
                )
            }
        }

        if (isResonating && resonatingColor != null) {
            ResonanceRipple(
                modifier = Modifier.fillMaxSize(),
                color = resonatingColor,
                active = isResonating
            )
        }
    }
}

@Composable
private fun ResonanceGlow(
    modifier: Modifier,
    color: Color,
    keyType: PianoKeyType
) {
    val glowRadiusPx = 18.dp.value * 2
    val fillAlpha = if (keyType == PianoKeyType.Black) 0.55f else 0.40f
    val fillR = (color.red * 255).toInt()
    val fillG = (color.green * 255).toInt()
    val fillB = (color.blue * 255).toInt()

    Box(
        modifier = modifier.drawBehind {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    setARGB((fillAlpha * 255).toInt(), fillR, fillG, fillB)
                    setShadowLayer(
                        glowRadiusPx, 0f, 0f,
                        android.graphics.Color.argb(
                            (fillAlpha * 255).toInt(), fillR, fillG, fillB
                        )
                    )
                    style = Paint.Style.FILL
                }
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
            }
        }
    )
}

@Composable
private fun ResonanceRipple(
    modifier: Modifier,
    color: Color,
    active: Boolean
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (active) {
            scale.snapTo(0f)
            alpha.snapTo(0.85f)
            scale.animateTo(7.2f, animationSpec = tween(720))
            alpha.animateTo(0f, animationSpec = tween(720))
        } else {
            scale.snapTo(0f)
            alpha.snapTo(0f)
        }
    }

    Box(
        modifier = modifier.drawWithContent {
            if (scale.value > 0.01f && alpha.value > 0.01f) {
                drawCircle(
                    color = color.copy(alpha = alpha.value),
                    radius = 5.dp.toPx() * scale.value,
                    center = Offset(size.width / 2f, 0f)
                )
            }
        }
    )
}

// ── Keyboard data ──────────────────────────────────────────────────────────────

fun generate88Keys(): List<PianoKeyData> {
    val keys = mutableListOf<PianoKeyData>()
    for (midi in 21..108) {
        val noteInOctave = midi % 12
        val isBlack = when (noteInOctave) {
            1, 3, 6, 8, 10 -> true
            else -> false
        }
        val keyType = if (isBlack) PianoKeyType.Black else PianoKeyType.White

        val pianoKey = when (noteInOctave) {
            0  -> PianoKey.C
            1  -> PianoKey.CSharp
            2  -> PianoKey.D
            3  -> PianoKey.DSharp
            4  -> PianoKey.E
            5  -> PianoKey.F
            6  -> PianoKey.FSharp
            7  -> PianoKey.G
            8  -> PianoKey.GSharp
            9  -> PianoKey.A
            10 -> PianoKey.ASharp
            11 -> PianoKey.B
            else -> PianoKey.C
        }

        val octave = midi / 12 - 1
        val offset = when (noteInOctave) {
            0  -> 0f;  1  -> 0.5f; 2  -> 1f;   3  -> 1.5f
            4  -> 2f;  5  -> 3f;   6  -> 3.5f; 7  -> 4f
            8  -> 4.5f; 9  -> 5f;  10 -> 5.5f; 11 -> 6f
            else -> 0f
        } + 2f - 7f
        val keyIndex = octave * 7 + offset

        keys.add(
            PianoKeyData(
                note = pianoKey,
                octave = octave,
                type = keyType,
                keyIndex = keyIndex
            )
        )
    }
    return keys
}

fun findKeyAt(
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

@Preview(showBackground = true)
@Composable
fun PianoRollPreview() {
    PianoRollNoteFlow(
        modifier = Modifier.fillMaxWidth(),
        notes = listOf(
            com.xianliticn.yuefu.music.VisualNoteEvent(0, 1000, 3f, PartColor0),
            com.xianliticn.yuefu.music.VisualNoteEvent(300, 1000, 4f, PartColor1)
        ),
        currentProgressMillis = 200
    )
}
