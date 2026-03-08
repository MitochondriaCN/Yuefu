package com.xianliticn.yuefu.modules

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xianliticn.yuefu.datastore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsManager(private val context: Context) {

    private companion object {
        val IS_SURVEY_SHOWN_KEY = stringPreferencesKey("is_survey_shown")
    }

    val isSurveyShown: Flow<Boolean> = context.datastore.data
        .map { preferences ->
            preferences[IS_SURVEY_SHOWN_KEY].toBoolean()
        }

    suspend fun setIsSurveyShown(isShown: Boolean) {
        context.datastore.edit { preferences ->
            preferences[IS_SURVEY_SHOWN_KEY] = isShown.toString()
        }
    }
}