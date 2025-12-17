package com.xianliticn.yuefu.ui.components

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.xianliticn.yuefu.music.VisualNoteEvent
import com.xianliticn.yuefu.ui.theme.Orange800

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("FrequentlyChangingValue")
@Composable
fun PianoRollNoteFlow(
    modifier: Modifier = Modifier,
    isScrollMode: Boolean = false,
    pressedKeyIds: List<String> = emptyList(),
    scrollState: ScrollState = rememberScrollState(),
    notes: List<VisualNoteEvent> = emptyList(),
    currentProgressMillis: Long = 0L,
    onKeyPressed: (PianoKeyData) -> Unit = {},
    onKeyReleased: (PianoKeyData) -> Unit = {}
) {
    // 1. 生成所有按键数据
    val allKeys = remember { generate88Keys() }
    val whiteKeys = remember(allKeys) { allKeys.filter { it.type == PianoKeyType.White } }

    // 2. 尺寸定义
    val whiteKeyWidth = 40.dp // 白键固定宽度
    val blackKeyWidth = 24.dp // 黑键固定宽度
    val density = LocalDensity.current

    // [新增] 将dp转换为px供NoteFlow使用
    val whiteKeyWidthPx = with(density) { whiteKeyWidth.toPx() }

    // 4. 触摸位置检测相关状态 (用于滑奏模式)

    // 存储每个白键和黑键的布局区域，用于命中测试
    val keysLayoutMap = remember { mutableMapOf<String, Rect>() }

    // 这是一个包含滚动逻辑和绘制逻辑的大容器
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }

        NoteFlow(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 200.dp)
                .clipToBounds(),
            whiteKeyWidth = whiteKeyWidthPx,
            keyCount = whiteKeys.size,
            notes = notes,
            currentProgressMillis = currentProgressMillis,
            //同步滚动
            visibleRange = NoteFlowVisibleRange(
                startPx = scrollState.value.toFloat(),
                endPx = scrollState.value.toFloat() + maxWidthPx
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // [修改] 给键盘一个固定高度，给上方留出空间
                .align(Alignment.BottomCenter) // [修改] 对齐到底部
        ) {
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                // 核心键盘区域
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        // 根据模式决定是否允许滚动
                        .horizontalScroll(scrollState, isScrollMode)
                        // 处理滑奏的触摸事件
                        .pointerInput(isScrollMode) {
                            if (!isScrollMode) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    // 计算当前的滚动偏移量，因为 pointerInput 坐标是相对于控件可见区域的
                                    // 而我们需要相对于内容的绝对坐标
                                    var touchPosition =
                                        down.position.copy(x = down.position.x + scrollState.value)

                                    // 查找被按下的键
                                    var currentKey =
                                        findKeyAt(touchPosition, keysLayoutMap, allKeys)
                                    currentKey?.let {
                                        onKeyPressed(it)
                                    }

                                    // 持续追踪移动
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.lastOrNull() ?: break

                                        if (change.pressed) {
                                            touchPosition =
                                                change.position.copy(x = change.position.x + scrollState.value)
                                            val newKey =
                                                findKeyAt(touchPosition, keysLayoutMap, allKeys)

                                            if (newKey != null && newKey.id != currentKey?.id) {
                                                // 释放旧键，按下新键
                                                currentKey?.let { onKeyReleased(it) }
                                                currentKey = newKey
                                                onKeyPressed(newKey)
                                            }
                                        } else {
                                            break // 手指抬起
                                        }
                                    }
                                    // 循环结束（手指抬起），释放最后的键
                                    currentKey?.let {
                                        onKeyReleased(it)
                                    }
                                }
                            }
                        }
                ) {
                    // 使用 Box 来进行复杂的层叠布局 (白键底层，黑键上层)
                    // 计算总宽度
                    val totalWidthDp = whiteKeyWidth * whiteKeys.size

                    Box(
                        modifier = Modifier
                            .width(totalWidthDp)
                            .fillMaxHeight()
                    ) {
                        // --- 绘制白键 ---
                        whiteKeys.forEachIndexed { index, keyData ->
                            // 白键的 X 偏移量很简单：索引 * 宽度
                            val offsetX = whiteKeyWidth * index

                            PianoKey(
                                modifier = Modifier
                                    .width(whiteKeyWidth)
                                    .fillMaxHeight()
                                    .offset(x = offsetX)
                                    .onGloballyPositioned { layoutCoordinates ->
                                        // 记录白键的区域 (用于命中测试)
                                        val rect = layoutCoordinates.size.toSize()
                                        // 注意：这里的offset是相对父容器的，需要结合我们在Box里的布局逻辑
                                        // 在 Box 中使用 offset 修饰符，layoutCoordinates 拿到的位置通常是准确的相对位置
                                        // 但最稳妥的是手动计算 Rect，因为我们知道确切的数学位置
                                        with(density) {
                                            keysLayoutMap[keyData.id] = Rect(
                                                offsetX.toPx(),
                                                0f,
                                                (offsetX + whiteKeyWidth).toPx(),
                                                200.dp.toPx() // 假设键盘高200
                                            )
                                        }
                                    },
                                keyType = PianoKeyType.White,
                                isPressed = pressedKeyIds.contains(keyData.id) && !isScrollMode, // 只有非滚动模式才显示按下效果
                                // 单击模式 (如果是滚动模式，点击依然可以发声，这取决于你的需求，这里保留)
                                onPressed = {
                                    if (isScrollMode) {
                                        onKeyPressed(keyData)
                                    }
                                },
                                onReleased = {
                                    if (isScrollMode) {
                                        onKeyReleased(keyData)
                                    }
                                }
                            )
                        }

                        // --- 绘制黑键 ---
                        // 必须在白键之后绘制，确保 z-index 更高 (或者显式设置 zIndex)
                        var currentWhiteKeyIndex = 0
                        allKeys.forEach { keyData ->
                            if (keyData.type == PianoKeyType.White) {
                                currentWhiteKeyIndex++
                            } else {
                                // 是黑键。它位于当前白键索引 (currentWhiteKeyIndex) 和前一个之间。
                                // 实际上，黑键总是位于它前一个白键的右侧边界中心。
                                // 比如 C# 位于 C (第0个白键) 和 D (第1个白键) 之间。
                                // 此时 currentWhiteKeyIndex 已经指向了 D (因为 C 已经被遍历过了)。
                                // 所以黑键中心位置 = (currentWhiteKeyIndex - 1 + 1) * whiteWidth - (blackWidth / 2) ???
                                // 让我们简化逻辑：
                                // 如果当前遍历到的是 C#，之前遍历过 C (whiteIndex=1)。C# 应该跨越 C 和 D。
                                // C 的右边界是 1 * whiteWidth。
                                // 所以 C# 的左边界 = (1 * whiteWidth) - (blackWidth / 2)。

                                val leftAnchorWhiteKeyIndex = currentWhiteKeyIndex
                                val offsetXDp =
                                    (whiteKeyWidth * leftAnchorWhiteKeyIndex) - (blackKeyWidth / 2)

                                PianoKey(
                                    modifier = Modifier
                                        .zIndex(1f) // 确保在白键上面
                                        .width(blackKeyWidth)
                                        .height(120.dp) // 黑键较短
                                        .offset(x = offsetXDp)
                                        .onGloballyPositioned {
                                            // 手动计算黑键区域
                                            with(density) {
                                                keysLayoutMap[keyData.id] =
                                                    Rect(
                                                        offsetXDp.toPx(),
                                                        0f,
                                                        (offsetXDp + blackKeyWidth).toPx(),
                                                        120.dp.toPx()
                                                    )
                                            }
                                        },
                                    keyType = PianoKeyType.Black,
                                    isPressed = pressedKeyIds.contains(keyData.id) && !isScrollMode,
                                    onPressed = {
                                        if (isScrollMode) {
                                            onKeyPressed(keyData)
                                        }
                                    },
                                    onReleased = {
                                        if (isScrollMode) {
                                            onKeyReleased(keyData)
                                        }
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

@Composable
fun PianoKey(
    modifier: Modifier = Modifier,
    keyType: PianoKeyType = PianoKeyType.White,
    isPressed: Boolean = false,
    onPressed: () -> Unit,
    onReleased: () -> Unit
) {
    // 动态计算颜色
    val backgroundColor = when (keyType) {
        PianoKeyType.White ->
            if (isPressed) Color(0xFFDDDDDD) else Color(0xFFFFFFF0)

        PianoKeyType.Black ->
            if (isPressed) Color(0xFFBBBBBB) else Color(0xFF000000)
    }
    val shadowElevation = if (isPressed) 2.dp else 4.dp

    // 使用 Surface 提供基础的形状和阴影
    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressed()
                        tryAwaitRelease()
                        onReleased()
                    }
                )
            }
            .fillMaxHeight(),
        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp), // 底部圆角
        color = backgroundColor,
        border = BorderStroke(1.dp, Color(0xFFCCCCCC)), // 细微的边框
        shadowElevation = shadowElevation
    ) {
        // 在按键内部增加渐变，模拟光照，使其看起来不是平面的
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) {
                            if (keyType == PianoKeyType.Black)
                                listOf(Color(0xFF444444), Color(0xFF222222))
                            else
                                listOf(Color(0xFFBBBBBB), Color(0xFFDDDDDD))
                        } else {
                            if (keyType == PianoKeyType.Black)
                                listOf(Color(0xFF333333), Color(0xFF000000))
                            else
                                listOf(Color.White, Color(0xFFF2F2F2))
                        }
                    )
                )
        ) {
            // 底部加深，模拟琴键的厚度
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
}

private fun findKeyAt(
    position: Offset,
    map: Map<String, Rect>,
    allKeys: List<PianoKeyData>
): PianoKeyData? {
    // 1. 先找黑键
    val blackKeyMatch = allKeys.filter { it.type == PianoKeyType.Black }.find { key ->
        map[key.id]?.contains(position) == true
    }
    if (blackKeyMatch != null) return blackKeyMatch

    // 2. 再找白键
    return allKeys.filter { it.type == PianoKeyType.White }.find { key ->
        map[key.id]?.contains(position) == true
    }
}

fun generate88Keys(): List<PianoKeyData> {
    val keys = mutableListOf<PianoKeyData>()
    // 钢琴通常从 A0, A#0, B0 开始
    val startKeys = listOf(PianoKey.A, PianoKey.ASharp, PianoKey.B)
    startKeys.forEach {
        keys.add(
            PianoKeyData(
                it,
                0,
                if (it.isBlack) PianoKeyType.Black else PianoKeyType.White
            )
        )
    }

    // 中间 1 到 7 个八度
    for (octave in 1..7) {
        PianoKey.entries.forEach { key ->
            keys.add(
                PianoKeyData(
                    key,
                    octave,
                    if (key.isBlack) PianoKeyType.Black else PianoKeyType.White
                )
            )
        }
    }

    // 最后一个键 C8
    keys.add(PianoKeyData(PianoKey.C, 8, PianoKeyType.White))

    return keys
}

data class PianoKeyData(
    val note: PianoKey,
    val octave: Int, // 音高，例如 C4, C5
    val type: PianoKeyType
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
            VisualNoteEvent(
                0, 1000, 3f, Orange800
            ),
            VisualNoteEvent(
                300, 1000, 4f, Orange800
            )
        ),
        currentProgressMillis = 200
    )
}