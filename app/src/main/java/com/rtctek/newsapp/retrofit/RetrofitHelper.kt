package com.rtctek.newsapp.retrofit

import com.rtctek.newsapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single source of Retrofit/OkHttp configuration.
 */
object RetrofitHelper {

    private const val BASE_URL = "https://newsapi.org/v2/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    /** Base URL is injectable so unit tests can point at a MockWebServer. */
    fun getInstance(baseUrl: String = BASE_URL): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun createApi(baseUrl: String = BASE_URL): NewsApi =
        getInstance(baseUrl).create(NewsApi::class.java)
}
