package com.xianliticn.yuefu.webapi

import com.xianliticn.yuefu.vo.ApiResponse
import com.xianliticn.yuefu.vo.SystemInfoVo
import retrofit2.http.GET

interface SystemInfoApi {

    @GET("systemInfo")
    suspend fun getSystemInfo(): ApiResponse<SystemInfoVo>
}