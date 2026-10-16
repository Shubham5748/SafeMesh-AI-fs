package com.safemesh.ai.data.api

import com.safemesh.ai.data.model.EmergencyRequest
import com.safemesh.ai.data.model.EmergencyResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("api/health")
    suspend fun checkHealth(): Response<Map<String, Any>>

    @POST("api/emergency")
    suspend fun dispatchEmergency(
        @Body request: EmergencyRequest
    ): Response<EmergencyResponse>
}

object ApiClient {
    private var currentBaseUrl: String = ""
    private var cachedService: ApiService? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    fun getService(baseUrl: String): ApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedService != null && currentBaseUrl == normalized) {
            return cachedService!!
        }

        currentBaseUrl = normalized
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        cachedService = service
        return service
    }
}
