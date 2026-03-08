package com.xianliticn.yuefu.webapi

import com.xianliticn.yuefu.vo.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

@JvmSuppressWildcards
interface SurveyApi {

    @POST("survey/submit")
    suspend fun submitSurvey(@Body qaMap: Map<String, List<String>>): ApiResponse<Unit>
}