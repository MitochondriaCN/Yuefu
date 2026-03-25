package com.xianliticn.yuefu

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class YuefuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installGlobalCrashHandler()
    }

    private fun installGlobalCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = throwable.stackTraceToString()
                (getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                    ClipData.newPlainText("Yuefu crash stack trace", stackTrace)
                )
            } catch (_: Exception) {
            } finally {
                previousHandler?.uncaughtException(thread, throwable) ?: run {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(10)
                }
            }
        }
    }
}

val Context.datastore by preferencesDataStore(name = "settings")
