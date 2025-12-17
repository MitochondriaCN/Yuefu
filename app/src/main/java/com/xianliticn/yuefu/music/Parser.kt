package com.xianliticn.yuefu.music

import androidx.compose.ui.graphics.Color
import com.xianliticn.yuefu.ui.theme.Green800
import com.xianliticn.yuefu.ui.theme.Grey800
import com.xianliticn.yuefu.ui.theme.Orange800
import org.dom4j.Document
import org.dom4j.Element

class Parser(
    private val partsColor: Map<Int, Color> = mapOf(
        0 to Green800,
        1 to Orange800,
        2 to Color.Blue,
        3 to Color.Red
    )
) {
    fun generateMidiEvents(
        musicXmlDoc: Document,
        modifiedBpm: Int? = null
    ): List<MidiEvent> {
        val midiEvents = mutableListOf<MidiEvent>()

        // MusicXML 的根节点通常是 <score-partwise>
        val root = musicXmlDoc.rootElement

        // 声部
        val parts = root.elements("part")

        parts.forEachIndexed { index, part ->
            val measures = part.elements("measure")

            // 累计时间（纳秒），用于计算每个音符的绝对开始时间
            var currentTimeNano: Long = 0

            // 假设默认速度，如果 XML 中没有定义
            var currentBpm = modifiedBpm ?: 120

            // divisions 属性通常在第一个小节的 <attributes> 中定义
            // 它表示四分音符被划分的份数，用于计算 duration
            var divisions = 1

            for (measure in measures) {
                val element = measure as Element

                // 检查 attributes 更新 (如 divisions, key, time signature)
                val attributes = element.element("attributes")
                attributes?.elementText("divisions")?.let {
                    divisions = it.toInt()
                }

                // 检查 direction 更新 (如 tempo/BPM)
                val direction = element.element("direction")
                if (direction != null) {
                    val sound = direction.element("sound")
                    if (sound != null && sound.attributeValue("tempo") != null) {
                        currentBpm = sound.attributeValue("tempo").toDouble().toInt()
                    }
                }

                // 解析该小节内的所有音符 (note)
                val notes = element.elements("note")
                for (noteObj in notes) {
                    val noteElement = noteObj as Element

                    // 处理 duration (时值)
                    val durationText = noteElement.elementText("duration") ?: "0"
                    val durationTicks = durationText.toInt()

                    // 计算该音符持续的纳秒数
                    // 公式: (duration / divisions) * (60 / BPM) * 1,000,000,000
                    // = duration * (60_000_000_000 / (divisions * BPM))
                    // 防止除零
                    val ticksPerBeat = if (divisions > 0) divisions else 1
                    val safeBpm = if (currentBpm > 0) currentBpm else 120
                    val nanosPerTick = (60_000_000_000.0 / (ticksPerBeat * safeBpm)).toLong()
                    val durationNano = durationTicks * nanosPerTick

                    // 处理 pitch (音高)
                    // 如果是休止符 (<rest>)，则跳过生成事件，但依然要增加时间
                    if (noteElement.element("rest") != null) {
                        currentTimeNano += durationNano
                        continue
                    }

                    val pitchElement = noteElement.element("pitch")
                    if (pitchElement != null) {
                        val step = pitchElement.elementText("step") // C, D, E...
                        val octave = pitchElement.elementText("octave").toInt() // 4, 5...
                        val alter = pitchElement.elementText("alter")?.toInt() ?: 0 // 升降号: 1, -1

                        val midiPitch = calculateMidiPitch(step, octave, alter)

                        // 是否是和弦 (<chord/>)?
                        // 如果是 chord，它与前一个音符同时开始，不需要增加 currentTimeNano
                        val isChord = noteElement.element("chord") != null

                        // 创建按下 (PRESS) 事件
                        val startTime = if (isChord) currentTimeNano else currentTimeNano
                        midiEvents.add(MidiEvent(midiPitch, startTime, Note.PRESS, false))

                        // 创建释放 (RELEASE) 事件
                        // 释放时间 = 开始时间 + 持续时间
                        // 注意：为了连贯性，有时会稍微减少一点持续时间，这里按标准长度处理
                        midiEvents.add(
                            MidiEvent(
                                midiPitch,
                                startTime + durationNano,
                                Note.RELEASE,
                                false,
                                index
                            )
                        )

                        // 只有不是和弦时，才推进时间指针
                        if (!isChord) {
                            currentTimeNano += durationNano
                        }
                    }
                }
            }
        }

        // 按时间排序，确保事件顺序正确 (尤其是处理和弦或多声部时)
        return midiEvents.sortedBy { it.timeNano }
    }

    fun generateVisualNoteEvents(events: List<MidiEvent>): List<VisualNoteEvent> {
        val visualNotes = mutableListOf<VisualNoteEvent>()

        val sortedEvents = events.sortedBy { it.timeNano }

        val activeNotes = mutableMapOf<Int, MidiEvent>()

        for (event in sortedEvents) {
            if (event.note == Note.PRESS) {
                activeNotes[event.pitch] = event
            } else if (event.note == Note.RELEASE) {
                val startEvent = activeNotes.remove(event.pitch)

                if (startEvent != null) {
                    val noteInOctave = event.pitch % 12
                    val octave = event.pitch / 12
                    val currentOctaveStart = octave * 7

                    val offset = when (noteInOctave) {
                        0 -> 0f; 1 -> 0.5f; 2 -> 1f; 3 -> 1.5f; 4 -> 2f; 5 -> 3f
                        6 -> 3.5f; 7 -> 4f; 8 -> 4.5f; 9 -> 5f; 10 -> 5.5f; 11 -> 6f
                        else -> 0f
                    }

                    visualNotes.add(
                        VisualNoteEvent(
                            startTimeMillis = startEvent.timeNano / 1_000_000,
                            endTimeMillis = event.timeNano / 1_000_000, // 直接使用当前 Release 的时间
                            keyIndex = currentOctaveStart + offset,
                            color = partsColor[startEvent.part] ?: Grey800
                        )
                    )
                }
            }
        }

        return visualNotes
    }

    /**
     * 辅助函数：将 MusicXML 的 Step/Octave/Alter 转换为 MIDI Note Number (0-127)
     * 例如: C4 -> 60
     */
    private fun calculateMidiPitch(step: String, octave: Int, alter: Int): Int {
        val baseValues = mapOf(
            "C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11
        )

        val base = baseValues[step] ?: 0
        // MIDI note calculation: (Octave + 1) * 12 + Base + Alter
        return (octave + 1) * 12 + base + alter
    }
}