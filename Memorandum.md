## 音频实现

这是在不引入 C++ 的情况下，在 Android 上实现较好效果的折中方案。

核心逻辑流程：

1. 数据扁平化 (Flattening): 解析 MusicXML，处理所有反复记号（Repeats）、跳房子（Voltas），将其展开成一条线性的
   `List<MidiEvent>`
    - `MidiEvent: { timestampMs: Long, command: ByteArray }`
2. 双层缓冲机制:
    - UI 线程/主控线程： 维护一个 `playheadPosition`（播放头）。
    - 调度线程（HandlerThread 或 Coroutine Dispatchers.IO）：
        - 不要 `Thread.sleep(1)` 这样死等。
        - 采用 "Look-ahead"（前瞻）机制。比如每隔 50ms 醒来一次。
        - 每次醒来，读取未来 100ms 内需要播放的所有音符。
        - 使用 `android.media.MediaPlayer` 或 `SoundPool` 或 `MidiReceiver.send(msg, timestamp)`
          。关键点： `MidiReceiver.send` 支持传入一个 timestamp（纳秒）。这意味着你可以“预定”未来 50ms
          后发声，由系统底层去准时触发，从而规避 Java 线程抖动。

具体代码思路示例 (基于方案 4):

```Kotlin
// 伪代码思路
class MusicSequencer(private val midiReceiver: MidiReceiver) {
    private var startTime = 0L
    private var isPlaying = false
    private val lookAheadTime = 100L // 向前看100ms
    private val loopInterval = 20L   // 循环检查间隔

    fun start() {
        startTime = System.nanoTime()
        isPlaying = true
        checkAndSchedule()
    }

    private fun checkAndSchedule() {
        if (!isPlaying) return

        val now = System.nanoTime()
        // 计算当前播放到的逻辑时间（加上前瞻时间）
        val scheduleUntil = now - startTime + (lookAheadTime * 1_000_000)

        // 查找所有在这个时间范围内还没发送的音符
        val events = eventList.filter {
            it.timestampNano <= scheduleUntil && !it.isSent
        }

        events.forEach { event ->
            // 核心：利用 send 的第二个参数，指定确切的绝对纳秒时间
            // Android 系统会尝试在这个时间点精准发送 MIDI 指令
            val timestamp = startTime + event.timestampNano
            midiReceiver.send(event.midiData, 0, event.midiData.size, timestamp)
            event.isSent = true
        }

        // 继续循环
        delay(loopInterval) // 使用协程或Handler延时
        checkAndSchedule()
    }
}
```

总结建议

1. 如果你的乐谱很简单（如简单的钢琴曲）且对即时响应要求不高： 使用 Jetpack Media3 (Exoplayer) 或
   MediaPlayer 播放预先生成的 MIDI 文件。你可以把 MusicXML 转成 MIDI 文件，然后直接播 MIDI 文件。这是最省事的。
2. 如果你需要可视化的进度条、且要自己控制合成器（如 SoundFont）： 推荐使用 C++ (FluidSynth) + Oboe。这是目前
   Android 音乐类 App 的工业标准。
3. 如果你坚持纯 Kotlin 开发： 请务必使用 Native MIDI API (MidiReceiver.send) 的带时间戳方法，配合
   Look-ahead 调度算法，而不要依赖 Thread.sleep 来卡点。

关于 MusicXML 解析的一个提示： MusicXML 的结构是基于小节（Measure）的树状结构，而播放器需要的是基于时间线的线性结构。你需要编写一个
Transformer，将 XML 的 DOM 树转换为 Sequence of Events，在这个过程中你需要处理：

- BPM 变化（Tempo Change）
- 拍号变化（Time Signature）
- 反复记号（Repeats, Segno, Coda）— 这是一个难点，需要展开逻辑。

## 可视化实现

你需要将整个系统分为三层：

1. Sequencer (音频引擎层)： 负责守时、发声，是时间的权威来源。
2. State Holder (中间状态层)： 也就是 ViewModel，负责分发状态。
3. UI Layer (视图层)： 也就是你的 Compose 或 View，只负责“盲目”地根据状态绘制。
   
不要让 UI 自己去计时（比如 UI 启动一个 Timer），也不要完全依赖 UI 的绘制帧率。**音频引擎必须是时间的唯一主宰。**

你需要一种机制，把“正在播放的音符”告诉 UI。

推荐做法：基于时间戳的查询 (Pull) 或 回调 (Push)

这里推荐 Push（回调/流）模式，结合 Kotlin 的 SharedFlow 或 StateFlow。

在你的音频引擎的 Loop 中，除了发送 MIDI 声音，还要检查哪些音符刚刚开始，哪些音符刚刚结束。

```kotlin
// 伪代码：在音频线程中
fun onPlaybackTick(currentPosition: Long) {
    // 1. 发送 MIDI 声音 (略)

    // 2. 通知 UI
    // 找出所有 start <= current < start + duration 的音符
    val activeNotes = allVisualNotes.filter { note ->
        currentPosition >= note.startTime && currentPosition < (note.startTime + note.duration)
    }

    // 通过 Flow 发送给 ViewModel
    _activeNotesFlow.emit(activeNotes)
}
```

在 UI 层，你不需要做复杂的逻辑，只需要观察 activeNotesFlow。

```kotlin
// ViewModel
class MusicViewModel : ViewModel() {
    // 这是一个包含当前所有被按下的琴键音高列表的 State
    val activePitches = MutableStateFlow<Set<Int>>(emptySet())

    fun startMusic() {audioEngine.start { currentActiveNotes ->
            // 将正在播放的音符列表转换为 Set<Pitch>，减少 UI 重绘开销
            activePitches.value = currentActiveNotes.map { it.pitch }.toSet()
        }
    }
}

// Compose UI
@Composable
fun PianoKey(pitch: Int, activePitches: Set<Int>) {
    // 检查当前这个键是否在活跃列表中
    val isPressed = activePitches.contains(pitch)
    
    val color by animateColorAsState(
        targetValue = if (isPressed) Color.Red else Color.White,
        animationSpec = tween(50) // 加一点点过渡动画让视觉更丝滑
    )

    Box(
        modifier = Modifier
            .background(color)
            .clickable { /*...*/ }
    )
}
```

```
[ MusicXML 文件 ]
       | 解析
       v
[ 扁平化的事件列表 (List<Event>) ]
       |
       +-------------------------+
       |                         |
[ 音频线程 (High Priority) ]   [ ViewModel / UI 状态 ]| Loop                    | Observe
       |                         |
   1. 检查时间                   | <--- (Flow / LiveData) 发送当前活跃音高集合
   2. 触发 MIDI 发声 (Sound)      |      (Set<Int> activePitches)
       |                         |
       v                         v
[ 扬声器 ]                  [ 钢琴键盘 UI ]
                           (根据 Set<Int> 重绘按键颜色)

``` 