package com.weargluco.watch.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object LibreLinkUpClient {

    private const val BASE_URL_EU = "https://api-eu.libreview.io/"
    private const val BASE_URL_US = "https://api-us.libreview.io/"
    private const val BASE_URL_DEFAULT = "https://api.libreview.io/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun create(
        region: String = "eu",
        token: String? = null
    ): LibreLinkUpApi {
        val baseUrl = when (region.lowercase()) {
            "eu" -> BASE_URL_EU
            "us" -> BASE_URL_US
            else -> BASE_URL_DEFAULT
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("product", "llu.android")
                    .header("version", "4.16.0")
                    .header("Content-Type", "application/json")
                    .header("Cache-Control", "no-cache")
                if (token != null) {
                    builder.header("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(LibreLinkUpApi::class.java)
    }
}
