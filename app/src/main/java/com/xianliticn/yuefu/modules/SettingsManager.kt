package com.xianliticn.yuefu.modules

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xianliticn.yuefu.datastore
import com.xianliticn.yuefu.ui.components.EffectLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsManager(private val context: Context) {

    private companion object {
        val IS_SURVEY_SHOWN_KEY = stringPreferencesKey("is_survey_shown")
        val IS_SHOWING_TUTORIAL_KEY = stringPreferencesKey("is_showing_tutorial")
        val EFFECT_LEVEL_KEY = intPreferencesKey("effect_level")
    }

    val isSurveyShown: Flow<Boolean> = context.datastore.data
        .map { preferences ->
            preferences[IS_SURVEY_SHOWN_KEY].toBoolean()
        }

    val isShowingTutorial: Flow<Boolean> = context.datastore.data
        .map { preferences ->
            preferences[IS_SHOWING_TUTORIAL_KEY].toBoolean()
        }

    val effectLevel: Flow<EffectLevel> = context.datastore.data
        .map { preferences ->
            val ordinal = preferences[EFFECT_LEVEL_KEY] ?: EffectLevel.HIGH.ordinal
            EffectLevel.entries.getOrNull(ordinal) ?: EffectLevel.HIGH
        }

    suspend fun setEffectLevel(level: EffectLevel) {
        context.datastore.edit { preferences ->
            preferences[EFFECT_LEVEL_KEY] = level.ordinal
        }
    }

    suspend fun setIsSurveyShown(isShown: Boolean) {
        context.datastore.edit { preferences ->
            preferences[IS_SURVEY_SHOWN_KEY] = isShown.toString()
        }
    }

    suspend fun setIsShowingTutorial(isShown: Boolean) {
        context.datastore.edit { preferences ->
            preferences[IS_SHOWING_TUTORIAL_KEY] = isShown.toString()
        }
    }
}