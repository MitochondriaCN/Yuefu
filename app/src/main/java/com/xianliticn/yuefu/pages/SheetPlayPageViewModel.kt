package com.xianliticn.yuefu.pages

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.AppDatabase
import com.xianliticn.yuefu.music.Parser
import com.xianliticn.yuefu.music.SequenceEngine
import com.xianliticn.yuefu.utils.getAbsoluteImportFilePath
import com.xianliticn.yuefu.utils.readXml
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class SheetPlayPageViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {
    @Inject
    @ApplicationContext
    lateinit var context: Context

    private var midiDevice: MidiDevice? = null
    private var sheetId: Int = 0

    fun refresh(sheetId: Int) {
        this.sheetId = sheetId
    }

    fun handlePlay() {
        val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager

        viewModelScope.launch {
            val sheet = appDatabase.sheetDao().getById(sheetId)
            val sheetDoc =
                readXml(File(context.getAbsoluteImportFilePath(sheet!!.fileName)))
            val events = Parser().generateMidiEvents(sheetDoc)

            // 1. 获取系统自带的 MIDI 设备信息
            val deviceInfos = midiManager.devices
            // 通常第一个包含 OUTPUT_PORT 的设备就是系统的软合成器
            val synthInfo = deviceInfos.firstOrNull { info ->
                val properties = info.properties
                val name = properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.lowercase() ?: ""
                properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)?.lowercase() ?: ""

                info.inputPortCount > 0 && (name.contains("usb") == false)
            }

            if (synthInfo != null) {
                // 2. 打开设备
                midiManager.openDevice(synthInfo, { device ->
                    if (device == null) return@openDevice
                    midiDevice = device

                    // 3. 打开输入端口，这会返回一个 MidiReceiver
                    // 我们向这个 Receiver 发送数据，合成器就会收到并播放
                    val inputPort = device.openInputPort(0)

                    if (inputPort != null) {
                        // 4. 将这个真正的 Receiver 传给你的引擎
                        // 注意：这里需要切回主线程或者你的 Engine 所在线程，因为 openDevice 是异步回调
                        // 假设 SequenceEngine 接受一个 MidiReceiver
                        SequenceEngine(inputPort).play(events) // 或者是其他启动方法
                    }
                }, Handler(Looper.getMainLooper()))
            }
        }
    }
}