package com.gosms.gateway.data.api

import com.gosms.gateway.data.model.HeartbeatRequest
import com.gosms.gateway.data.model.HeartbeatResponse
import com.gosms.gateway.data.model.MessagesResponse
import com.gosms.gateway.data.model.RegisterRequest
import com.gosms.gateway.data.model.RegisterResponse
import com.gosms.gateway.data.model.SimpleResponse
import com.gosms.gateway.data.model.StatusReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/device/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/device/heartbeat")
    suspend fun heartbeat(@Body request: HeartbeatRequest): Response<HeartbeatResponse>

    @GET("api/device/messages")
    suspend fun getMessages(@Query("limit") limit: Int = 10): Response<MessagesResponse>

    @POST("api/device/status")
    suspend fun reportStatus(@Body request: StatusReportRequest): Response<SimpleResponse>

    @POST("api/device/unpair")
    suspend fun unpair(): Response<SimpleResponse>
}
