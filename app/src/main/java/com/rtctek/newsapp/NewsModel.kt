package com.rtctek.newsapp

import android.content.Intent
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rtctek.newsapp.utils.Constants

/**
 * A single news story.
 *
 * The entity doubles as the Room row for both the per-category offline cache
 * and the user's saved ("bookmark") list, discriminated by [category].
 */
@Entity(tableName = "articles")
data class NewsModel(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "headline")
    val headLine: String,

    @ColumnInfo(name = "imgurl")
    val image: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "url")
    val url: String? = null,

    @ColumnInfo(name = "source")
    val source: String? = null,

    /** ISO-8601 publication timestamp, exactly as returned by NewsAPI. */
    @ColumnInfo(name = "time")
    val time: String? = null,

    @ColumnInfo(name = "content")
    val content: String? = null,

    /** Tab category this row belongs to, or [Constants.SAVED_CATEGORY] for bookmarks. */
    @ColumnInfo(name = "category")
    val category: String = Constants.SAVED_CATEGORY,

    /** Original ordering inside the API response (used to restore list order). */
    @ColumnInfo(name = "sort_index")
    val sortIndex: Int = 0,
)

/** Puts all fields of [article] into this [Intent] so it can be re-read later. */
fun Intent.putArticleExtras(article: NewsModel): Intent = apply {
    putExtra(Constants.EXTRA_NEWS_TITLE, article.headLine)
    putExtra(Constants.EXTRA_NEWS_IMAGE_URL, article.image)
    putExtra(Constants.EXTRA_NEWS_DESCRIPTION, article.description)
    putExtra(Constants.EXTRA_NEWS_URL, article.url)
    putExtra(Constants.EXTRA_NEWS_SOURCE, article.source)
    putExtra(Constants.EXTRA_NEWS_PUBLICATION_TIME, article.time)
    putExtra(Constants.EXTRA_NEWS_CONTENT, article.content)
}

/** Rebuilds a [NewsModel] previously stored via [putArticleExtras], or `null`. */
fun Intent.getNewsArticle(): NewsModel? {
    val title = getStringExtra(Constants.EXTRA_NEWS_TITLE) ?: return null
    return NewsModel(
        id = 0L,
        headLine = title,
        image = getStringExtra(Constants.EXTRA_NEWS_IMAGE_URL),
        description = getStringExtra(Constants.EXTRA_NEWS_DESCRIPTION),
        url = getStringExtra(Constants.EXTRA_NEWS_URL),
        source = getStringExtra(Constants.EXTRA_NEWS_SOURCE),
        time = getStringExtra(Constants.EXTRA_NEWS_PUBLICATION_TIME),
        content = getStringExtra(Constants.EXTRA_NEWS_CONTENT),
        category = Constants.SAVED_CATEGORY,
        sortIndex = 0,
    )
}
