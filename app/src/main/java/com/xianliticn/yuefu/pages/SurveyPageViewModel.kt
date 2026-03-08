package com.xianliticn.yuefu.pages

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.webapi.SurveyApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SurveyPageViewModel @Inject constructor(
    private val surveyApi: SurveyApi
) : ViewModel() {

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    fun handleFinished(qaMap: Map<String, List<String>>) {
        if (qaMap.isNotEmpty())
            viewModelScope.launch {
                runCatching { surveyApi.submitSurvey(qaMap) }
                    .onSuccess {
                        Toast.makeText(
                            context,
                            context.getString(R.string.survey_submit_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        _isFinished.value = true
                    }
                    .onFailure {
                        Toast.makeText(
                            context,
                            context.getString(R.string.survey_submit_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }
}
