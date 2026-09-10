package com.rtctek.newsapp.architecture

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtctek.newsapp.NewsModel

/**
 * Room DAO backing both the per-category offline cache and the saved list.
 */
@Dao
interface NewsDao {

    // ── Category cache ──────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<NewsModel>)

    @Query("DELETE FROM articles WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY sort_index ASC")
    fun observeCategory(category: String): LiveData<List<NewsModel>>

    // ── Saved (bookmarked) news ─────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: NewsModel): Long

    @Query("DELETE FROM articles WHERE category = 'saved' AND url = :url")
    suspend fun deleteSavedByUrl(url: String?)

    @Query("SELECT * FROM articles WHERE category = 'saved' ORDER BY id DESC")
    fun observeSaved(): LiveData<List<NewsModel>>

    @Query("SELECT url FROM articles WHERE category = 'saved'")
    fun observeSavedUrls(): LiveData<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE category = 'saved' AND url = :url)")
    suspend fun isArticleSaved(url: String?): Boolean
}
