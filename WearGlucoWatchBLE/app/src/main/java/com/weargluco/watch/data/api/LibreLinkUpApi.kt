package com.weargluco.watch.data.api

import com.weargluco.watch.data.api.models.ConnectionsResponse
import com.weargluco.watch.data.api.models.GraphResponse
import com.weargluco.watch.data.api.models.LoginRequest
import com.weargluco.watch.data.api.models.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LibreLinkUpApi {

    @POST("llu/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("llu/connections")
    suspend fun getConnections(): ConnectionsResponse

    @GET("llu/connections/{patientId}/graph")
    suspend fun getGraph(@Path("patientId") patientId: String): GraphResponse
}
