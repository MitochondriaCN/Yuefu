package com.xianliticn.yuefu.webapi

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OmrApi {

    @POST("omr")
    @Multipart
    suspend fun getMusicXml(@Part image: MultipartBody.Part): Response<ResponseBody>
}