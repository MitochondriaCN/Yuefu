package com.xianliticn.yuefu.music

import androidx.compose.ui.graphics.Color
import com.xianliticn.yuefu.ui.theme.PartColor0
import com.xianliticn.yuefu.ui.theme.PartColor1
import com.xianliticn.yuefu.ui.theme.PartColor2
import com.xianliticn.yuefu.ui.theme.PartColor3
import com.xianliticn.yuefu.ui.theme.TextMuted
import org.dom4j.Document
import org.dom4j.Element

class Parser(
    private val partsColor: Map<Int, Color> = mapOf(
        0 to PartColor0,
        1 to PartColor1,
        2 to PartColor2,
        3 to PartColor3
    )
) {
    fun generateMidiEvents(
        musicXmlDoc: Document,
        modifiedBpm: Int? = null
    ): List<MidiEvent> {
        val midiEvents = mutableListOf<MidiEvent>()
        val root = musicXmlDoc.rootElement
        val parts = root.elements("part")
        var lastNoteDuration: Long = 0

        parts.forEachIndexed { partIndex, part ->
            // 每个 Part 独立计时
            var currentTimeNano: Long = 0
            var currentBpm = modifiedBpm ?: 120
            var divisions = 1

            val measures = part.elements("measure")
            for ((measureIndex, measure) in measures.withIndex()) {
                val element = measure as Element
                val currentMeasure = element.attributeValue("number")?.toIntOrNull() ?: (measureIndex + 1)

                // 1. 更新属性 (Attributes)
                element.element("attributes")?.let { attrs ->
                    attrs.elementText("divisions")?.let { divisions = it.toInt() }
                }

                // 2. 更新速度 (Direction/Sound)
                element.elements("direction").forEach { dir ->
                    dir.element("sound")?.attributeValue("tempo")?.let {
                        if (modifiedBpm == null) { // 只有没手动指定 BPM 时才更新
                            currentBpm = it.toDouble().toInt()
                        }
                    }
                }

                // 3. 遍历小节内的所有子节点 (按顺序处理 note, backup, forward)
                val children = element.elements()
                for (child in children) {
                    when (child.name) {
                        "note" -> {
                            val durationTicks = child.elementText("duration")?.toInt() ?: 0
                            val nanosPerTick =
                                (60_000_000_000.0 / (divisions * currentBpm)).toLong()
                            val durationNano = durationTicks * nanosPerTick

                            val isChord = child.element("chord") != null

                            // 一个 note 可能既是 stop 也是 start (连续连音中间的音符)
                            val ties = child.elements("tie")
                            val isTieStart = ties.any { it.attributeValue("type") == "start" }
                            val isTieStop = ties.any { it.attributeValue("type") == "stop" }

                            // 计算实际开始时间
                            val actualStart = if (isChord) {
                                currentTimeNano - lastNoteDuration
                            } else {
                                currentTimeNano
                            }

                            // 处理休止符
                            if (child.element("rest") != null) {
                                currentTimeNano += durationNano
                            } else {
                                val pitchElement = child.element("pitch")
                                if (pitchElement != null) {
                                    val step = pitchElement.elementText("step")
                                    val octave = pitchElement.elementText("octave").toInt()
                                    val alter = pitchElement.elementText("alter")?.toInt() ?: 0
                                    val midiPitch = calculateMidiPitch(step, octave, alter)

                                    // 1. 如果不是连音的结束部分（或者是新音符的开始），才生成按下事件
                                    // 如果是 isTieStop，说明这个音是延续上一个音的，不需要再次 Press
                                    if (!isTieStop) {
                                        midiEvents.add(
                                            MidiEvent(
                                                midiPitch,
                                                actualStart,
                                                Note.PRESS,
                                                false,
                                                partIndex, // 建议把 partIndex 加上，保持一致
                                                currentMeasure
                                            )
                                        )
                                    }

                                    // 2. 如果不是连音的开始部分（说明音符在这里结束），才生成释放事件
                                    // 如果是 isTieStart，说明这个音还要延续到下一个音，暂时不 Release
                                    if (!isTieStart) {
                                        midiEvents.add(
                                            MidiEvent(
                                                midiPitch,
                                                actualStart + durationNano,
                                                Note.RELEASE,
                                                false,
                                                partIndex,
                                                currentMeasure
                                            )
                                        )
                                    }

                                    if (!isChord) {
                                        currentTimeNano += durationNano
                                        lastNoteDuration = durationNano // 记录用于和弦对齐
                                    }
                                }
                            }
                        }

                        "backup" -> {
                            val durationTicks = child.elementText("duration")?.toInt() ?: 0
                            val nanosPerTick =
                                (60_000_000_000.0 / (divisions * currentBpm)).toLong()
                            currentTimeNano -= (durationTicks * nanosPerTick)
                        }

                        "forward" -> {
                            val durationTicks = child.elementText("duration")?.toInt() ?: 0
                            val nanosPerTick =
                                (60_000_000_000.0 / (divisions * currentBpm)).toLong()
                            currentTimeNano += (durationTicks * nanosPerTick)
                        }
                    }
                }
            }
        }
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
                    val octave = event.pitch / 12 - 1
                    val currentOctaveStart = octave * 7

                    // MIDI Pitch将C-1记为0，而keyIndex将A0记为0，
                    // 所以需要进行一定的偏移
                    val offset = when (noteInOctave) {
                        0 -> 0f; 1 -> 0.5f; 2 -> 1f; 3 -> 1.5f; 4 -> 2f; 5 -> 3f
                        6 -> 3.5f; 7 -> 4f; 8 -> 4.5f; 9 -> 5f; 10 -> 5.5f; 11 -> 6f
                        else -> 0f
                    } + 2f - 7f

                    visualNotes.add(
                        VisualNoteEvent(
                            startTimeMillis = startEvent.timeNano / 1_000_000,
                            endTimeMillis = event.timeNano / 1_000_000, // 直接使用当前 Release 的时间
                            keyIndex = currentOctaveStart + offset,
                            color = partsColor[startEvent.part] ?: TextMuted
                        )
                    )
                }
            }
        }

        return visualNotes
    }

    private fun calculateMidiPitch(step: String, octave: Int, alter: Int): Int {
        // C4 = 60 in MIDI
        val stepValues = mapOf(
            "C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11
        )
        val stepValue = stepValues[step] ?: 0
        return (octave + 1) * 12 + stepValue + alter
    }
}

enum class Note {
    PRESS, RELEASE
}

data class MidiEvent(
    val pitch: Int,
    val timeNano: Long,
    val note: Note,
    val isChord: Boolean,
    val part: Int,
    val measure: Int? = null
)
