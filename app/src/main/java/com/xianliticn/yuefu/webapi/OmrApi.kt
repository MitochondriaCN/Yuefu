package com.xianliticn.yuefu.webapi

import com.xianliticn.yuefu.vo.ApiResponse
import com.xianliticn.yuefu.vo.TaskStatus
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface OmrApi {

    @POST("omr")
    @Multipart
    @Deprecated("Use submitSheetImage() instead")
    suspend fun getMusicXml(@Part image: MultipartBody.Part): Response<ResponseBody>

    @POST("omr/submit")
    @Multipart
    /**
     * 提交乐谱图片创建OMR任务
     *
     * @return OMR任务ID
     */
    suspend fun submitSheetImage(@Part image: MultipartBody.Part): ApiResponse<Int>

    /**
     * 获取OMR任务状态
     *
     * @return OMR任务状态
     */
    @GET("omr/status/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: Int): ApiResponse<TaskStatus>

    /**
     * 下载OMR任务结果
     *
     * @return OMR任务结果文件流
     */
    @GET("omr/download/{taskId}")
    suspend fun downloadSheet(@Path("taskId") taskId: Int): Response<ResponseBody>
}