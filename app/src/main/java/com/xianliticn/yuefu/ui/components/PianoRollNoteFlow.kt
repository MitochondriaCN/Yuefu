package com.xianliticn.yuefu.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xianliticn.yuefu.music.VisualNoteEvent
import kotlin.math.roundToLong

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("FrequentlyChangingValue")
@Composable
fun PianoRollNoteFlow(
    modifier: Modifier = Modifier,
    isScrollMode: Boolean = false,
    keyboardHeight: Dp = 200.dp,
    pressedKeyIds: List<String> = emptyList(),
    scrollState: ScrollState = rememberScrollState(),
    notes: List<VisualNoteEvent> = emptyList(),
    currentProgressMillis: Long = 0L,
    effectLevel: EffectLevel = EffectLevel.HIGH,
    onKeyPressed: (PianoKeyData) -> Unit = {},
    onKeyReleased: (PianoKeyData) -> Unit = {}
) {
    val allKeys = remember { generate88Keys() }
    val whiteKeys = remember(allKeys) { allKeys.filter { it.type == PianoKeyType.White } }

    val whiteKeyWidth = 40.dp
    val blackKeyWidth = 24.dp
    val density = LocalDensity.current
    val whiteKeyWidthPx = remember(density) { with(density) { whiteKeyWidth.toPx() } }

    val keysLayoutMap = remember { mutableMapOf<String, Rect>() }
    val blackKeyHeight = keyboardHeight * 0.6f
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }

    val impactHolder = rememberImpactState()
    val resonatingColorMap = remember(notes, currentProgressMillis) {
        notes.filter { currentProgressMillis in it.startTimeMillis until it.endTimeMillis }
            .associate { it.keyIndex to it.color }
    }

    var initialScrollDone by remember { mutableStateOf(false) }

    // Auto-center on song load
    LaunchedEffect(notes) {
        if (notes.isNotEmpty() && !initialScrollDone) {
            initialScrollDone = true
            val midKeyIndex = notes.map { it.keyIndex }.sorted()[notes.size / 2]
            val targetPx = (midKeyIndex + 0.5f) * whiteKeyWidthPx - viewportWidthPx / 2f
            val maxScroll = (whiteKeyWidthPx * whiteKeys.size - viewportWidthPx).coerceAtLeast(0f)
            scrollState.scrollTo(targetPx.coerceIn(0f, maxScroll).roundToLong().toInt())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewportWidthPx = it.width.toFloat() }
    ) {
        // Layer 0: warm background drift
        if (effectLevel == EffectLevel.HIGH) {
            LiquidBackgroundDrift(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = keyboardHeight)
                    .clipToBounds()
            )
        }

        // Layer 1: falling notes + impact rings/droplets
        NoteFlow(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = keyboardHeight)
                .clipToBounds(),
            whiteKeyWidth = whiteKeyWidthPx,
            keyCount = whiteKeys.size,
            notes = notes,
            currentProgressMillis = currentProgressMillis,
            effectLevel = effectLevel,
            visibleRange = NoteFlowVisibleRange(
                startPx = scrollState.value.toFloat(),
                endPx = scrollState.value.toFloat() + viewportWidthPx
            ),
            impactHolder = impactHolder
        )

        // Layer 2: keyboard
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

                                    var currentKey =
                                        findKeyAt(touchPosition, keysLayoutMap, allKeys)
                                    currentKey?.let { onKeyPressed(it) }

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.lastOrNull() ?: break

                                        if (change.pressed) {
                                            touchPosition =
                                                change.position.copy(x = change.position.x + scrollState.value)
                                            val newKey =
                                                findKeyAt(touchPosition, keysLayoutMap, allKeys)

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
                        whiteKeys.forEachIndexed { index, keyData ->
                            val offsetX = whiteKeyWidth * index

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
                                isPressed = pressedKeyIds.contains(keyData.id) && !isScrollMode,
                                isResonating = resonatingColorMap.containsKey(keyData.keyIndex),
                                resonatingColor = resonatingColorMap[keyData.keyIndex],
                                onPressed = {
                                    if (isScrollMode) onKeyPressed(keyData)
                                },
                                onReleased = {
                                    if (isScrollMode) onKeyReleased(keyData)
                                }
                            )
                        }

                        var currentWhiteKeyIndex = 0
                        allKeys.forEach { keyData ->
                            if (keyData.type == PianoKeyType.White) {
                                currentWhiteKeyIndex++
                            } else {
                                val leftAnchorWhiteKeyIndex = currentWhiteKeyIndex
                                val offsetXDp =
                                    (whiteKeyWidth * leftAnchorWhiteKeyIndex) - (blackKeyWidth / 2)

                                PianoKey(
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .width(blackKeyWidth)
                                        .height(blackKeyHeight)
                                        .offset(x = offsetXDp)
                                        .onGloballyPositioned {
                                            with(density) {
                                                keysLayoutMap[keyData.id] =
                                                    Rect(
                                                        offsetXDp.toPx(),
                                                        0f,
                                                        (offsetXDp + blackKeyWidth).toPx(),
                                                        blackKeyHeight.toPx()
                                                    )
                                            }
                                        },
                                    keyType = PianoKeyType.Black,
                                    isPressed = pressedKeyIds.contains(keyData.id) && !isScrollMode,
                                    isResonating = resonatingColorMap.containsKey(keyData.keyIndex),
                                    resonatingColor = resonatingColorMap[keyData.keyIndex],
                                    onPressed = {
                                        if (isScrollMode) onKeyPressed(keyData)
                                    },
                                    onReleased = {
                                        if (isScrollMode) onKeyReleased(keyData)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
