package com.xianliticn.yuefu

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YuefuApplication : Application() {
}

val Context.datastore by preferencesDataStore(name = "settings")