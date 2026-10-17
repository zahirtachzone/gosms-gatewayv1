package com.gosms.gateway.data.api

import com.gosms.gateway.BuildConfig
import com.gosms.gateway.data.storage.SecureStorage
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(
    private val secureStorage: SecureStorage,
    private val onRevocationListener: (() -> Unit)? = null
) {
    private val moshi: Moshi = Moshi.Builder().build()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")

        val token = secureStorage.getDeviceToken()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        val deviceId = secureStorage.getDeviceId()
        builder.header("X-Device-Id", deviceId)

        val response = chain.proceed(builder.build())
        if (response.code == 401 || response.code == 403) {
            onRevocationListener?.invoke()
        }
        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }
        builder.build()
    }

    fun getService(overrideBaseUrl: String? = null): ApiService {
        val rawUrl = overrideBaseUrl ?: secureStorage.getServerUrl() ?: "https://localhost"
        val normalizedUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
