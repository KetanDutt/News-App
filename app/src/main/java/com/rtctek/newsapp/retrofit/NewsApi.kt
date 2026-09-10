package com.rtctek.newsapp.retrofit

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * NewsAPI v2 endpoints.
 *
 * https://newsapi.org/docs/endpoints/top-headlines
 * https://newsapi.org/docs/endpoints/everything
 */
interface NewsApi {

    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String,
        @Query("category") category: String?,
        @Query("apiKey") apiKey: String,
    ): Response<NewsDataFromJson>

    @GET("everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("language") language: String,
        @Query("sortBy") sortBy: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") page: Int,
        @Query("apiKey") apiKey: String,
    ): Response<NewsDataFromJson>
}
