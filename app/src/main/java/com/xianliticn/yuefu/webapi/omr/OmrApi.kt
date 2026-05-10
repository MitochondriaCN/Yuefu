package com.xianliticn.yuefu.webapi.omr

import com.xianliticn.yuefu.vo.ApiResponse
import com.xianliticn.yuefu.vo.SubmitTaskVo
import com.xianliticn.yuefu.vo.TaskStatus
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface OmrApi {

    @POST("omr")
    @Multipart
    @Deprecated("Use submitSheetImage() instead")
    suspend fun getMusicXml(@Part image: MultipartBody.Part): Response<ResponseBody>

    @POST("omr/submit/{engine}")
    @Multipart
    /**
     * 提交乐谱图片创建OMR任务
     */
    suspend fun submitSheetImage(
        @Path("engine") engine: OmrEngine,
        @Part image: MultipartBody.Part
    ): ApiResponse<SubmitTaskVo>

    /**
     * 提交Demo任务
     */
    @POST("omr/submit/demo")
    suspend fun submitDemo(
        @Query("sleepSec") sleepSec: Int,
        @Query("index") index: Int
    )

    /**
     * 获取OMR任务状态
     *
     * @return OMR任务状态
     */
    @GET("omr/status/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: String): ApiResponse<TaskStatus>

    /**
     * 下载OMR任务结果
     *
     * @return OMR任务结果文件流
     */
    @GET("omr/download/{taskId}")
    suspend fun downloadSheet(@Path("taskId") taskId: String): Response<ResponseBody>

    @GET("omr/download/pic/{taskId}")
    suspend fun downloadPicture(@Path("taskId") taskId: String): ApiResponse<String>
}