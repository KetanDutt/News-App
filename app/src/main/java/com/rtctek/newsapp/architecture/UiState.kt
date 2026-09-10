package com.rtctek.newsapp.architecture

import com.rtctek.newsapp.NewsModel

/** Loading/error state of a single news category tab. */
data class CategoryUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

/** Loading/error state of the search screen. */
data class SearchUiState(
    val loading: Boolean = false,
    val articles: List<NewsModel> = emptyList(),
    val error: String? = null,
    val query: String = "",
)
