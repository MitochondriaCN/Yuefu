package com.xianliticn.yuefu.pages

import android.content.Context
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

    private var sheetId: Int = 0
    private var se = SequenceEngine()

    fun refresh(sheetId: Int) {
        this.sheetId = sheetId
    }

    fun handlePlay() {

        viewModelScope.launch {
            val sheet = appDatabase.sheetDao().getById(sheetId)
            val sheetDoc =
                readXml(File(context.getAbsoluteImportFilePath(sheet!!.fileName)))
            val events = Parser().generateMidiEvents(sheetDoc)

            se.play(events)
            se.currentProgressMillis.collect { p ->

            }

        }
    }
}