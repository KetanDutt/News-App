package com.rtctek.newsapp.architecture

import com.rtctek.newsapp.BuildConfig
import com.rtctek.newsapp.NewsModel
import com.rtctek.newsapp.retrofit.NewsApi
import com.rtctek.newsapp.retrofit.NewsDataFromJson
import com.rtctek.newsapp.retrofit.RetrofitHelper
import com.rtctek.newsapp.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

/** Thrown when NewsAPI answers with an error envelope or a non-success code. */
class NewsApiException(message: String) : Exception(message)

/**
 * Fetches data from NewsAPI and maps it to the Room entity.
 * Stateless besides the API instance — trivially unit-testable with MockWebServer.
 */
class NewsRepository(private val api: NewsApi = RetrofitHelper.createApi()) {

    /** Top headlines for [country]/[category], newest-first as delivered by the API. */
    suspend fun fetchTopHeadlines(
        country: String = Constants.COUNTRY,
        category: String,
    ): List<NewsModel> = withContext(Dispatchers.IO) {
        val response = api.getTopHeadlines(
            country = country,
            category = category,
            apiKey = BuildConfig.API_KEY,
        )
        parseResponse(response, category)
    }

    /** Full-text search across NewsAPI's "everything" endpoint. */
    suspend fun searchNews(query: String, page: Int = 1): List<NewsModel> =
        withContext(Dispatchers.IO) {
            val response = api.searchNews(
                query = query,
                language = "en",
                sortBy = "publishedAt",
                pageSize = Constants.SEARCH_PAGE_SIZE,
                page = page,
                apiKey = BuildConfig.API_KEY,
            )
            parseResponse(response, Constants.SEARCH_CATEGORY)
        }

    companion object {

        internal fun parseResponse(
            response: Response<NewsDataFromJson>,
            category: String,
        ): List<NewsModel> {
            val body = response.body()
            if (response.isSuccessful && body?.status == "ok") {
                return body.articles.orEmpty()
                    .filter { !it.title.isNullOrBlank() && !it.url.isNullOrBlank() }
                    .mapIndexed { index, article ->
                        NewsModel(
                            id = 0L,
                            headLine = article.title!!.trim(),
                            image = article.urlToImage,
                            description = article.description,
                            url = article.url,
                            source = article.source?.name?.takeIf { it.isNotBlank() },
                            time = article.publishedAt,
                            content = article.content,
                            category = category,
                            sortIndex = index,
                        )
                    }
            }

            // NewsAPI reports failures as JSON with a human-readable message.
            val serverMessage = body?.message
                ?: response.errorBody()?.string()?.let { extractErrorMessage(it) }
                ?: response.message()
            throw NewsApiException(
                serverMessage?.takeIf { it.isNotBlank() }
                    ?: "NewsAPI request failed with code ${response.code()}"
            )
        }

        private fun extractErrorMessage(raw: String): String? = try {
            JSONObject(raw).optString("message").takeIf { it.isNotBlank() }
        } catch (_: JSONException) {
            null
        }
    }
}
