package com.rtctek.newsapp

import com.rtctek.newsapp.architecture.NewsApiException
import com.rtctek.newsapp.architecture.NewsRepository
import com.rtctek.newsapp.retrofit.RetrofitHelper
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for [NewsRepository] JSON parsing and error handling,
 * backed by a local [MockWebServer] (no Android framework involved).
 */
class NewsRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: NewsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = RetrofitHelper.createApi(baseUrl = server.url("/v2/").toString())
        repository = NewsRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful response is mapped to NewsModel list`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "status": "ok",
                  "totalResults": 2,
                  "articles": [
                    {
                      "source": { "id": null, "name": "The Times of India" },
                      "author": "Author",
                      "title": "  Headline one  ",
                      "description": "Description one",
                      "url": "https://example.com/one",
                      "urlToImage": "https://example.com/one.jpg",
                      "publishedAt": "2024-05-01T10:00:00Z",
                      "content": "Content one"
                    },
                    {
                      "source": { "id": "ndtv", "name": "NDTV News" },
                      "author": null,
                      "title": "Headline two",
                      "description": null,
                      "url": "https://example.com/two",
                      "urlToImage": null,
                      "publishedAt": null,
                      "content": null
                    }
                  ]
                }
                """.trimIndent()
            ).setHeader("Content-Type", "application/json")
        )

        val articles = repository.fetchTopHeadlines(country = "in", category = "general")

        assertEquals(2, articles.size)
        assertEquals("Headline one", articles[0].headLine) // trimmed
        assertEquals("The Times of India", articles[0].source)
        assertEquals("https://example.com/one", articles[0].url)
        assertEquals("general", articles[0].category)
        assertEquals(0, articles[0].sortIndex)
        assertEquals(1, articles[1].sortIndex)
        assertNull(articles[1].description)

        val request = server.takeRequest()
        assertEquals("/v2/top-headlines?country=in&category=general&apiKey=test-key", request.path)
    }

    @Test
    fun `articles with blank title or missing url are skipped`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "status": "ok",
                  "totalResults": 3,
                  "articles": [
                    { "title": null, "url": "https://example.com/a", "source": {"name": "S"} },
                    { "title": "   ", "url": "https://example.com/b", "source": {"name": "S"} },
                    { "title": "Valid", "url": null, "source": {"name": "S"} },
                    { "title": "Kept", "url": "https://example.com/c", "source": {"name": "S"} }
                  ]
                }
                """.trimIndent()
            ).setHeader("Content-Type", "application/json")
        )

        val articles = repository.fetchTopHeadlines(category = "sports")

        assertEquals(1, articles.size)
        assertEquals("Kept", articles[0].headLine)
    }

    @Test
    fun `http error surfaces the API error message`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(429).setBody(
                """
                { "status": "error", "code": "rateLimited", "message": "You've made too many requests." }
                """.trimIndent()
            ).setHeader("Content-Type", "application/json")
        )

        val exception = runCatching {
            repository.fetchTopHeadlines(category = "general")
        }.exceptionOrNull()

        assertTrue(exception is NewsApiException)
        assertEquals("You've made too many requests.", exception?.message)
    }

    @Test
    fun `200 envelope with status error also throws`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                { "status": "error", "code": "apiKeyInvalid", "message": "Your API key is invalid." }
                """.trimIndent()
            ).setHeader("Content-Type", "application/json")
        )

        val exception = runCatching {
            repository.searchNews("cricket")
        }.exceptionOrNull()

        assertTrue(exception is NewsApiException)
        assertEquals("Your API key is invalid.", exception?.message)
    }

    @Test
    fun `search request hits everything endpoint with expected params`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{ "status": "ok", "totalResults": 0, "articles": [] }"""
            ).setHeader("Content-Type", "application/json")
        )

        val articles = repository.searchNews("cricket")

        assertTrue(articles.isEmpty())
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/v2/everything?"))
        assertTrue(request.path!!.contains("q=cricket"))
        assertTrue(request.path!!.contains("sortBy=publishedAt"))
        assertTrue(request.path!!.contains("pageSize=20"))
    }
}
