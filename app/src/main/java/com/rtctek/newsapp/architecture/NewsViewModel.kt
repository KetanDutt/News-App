package com.rtctek.newsapp.architecture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.rtctek.newsapp.NewsModel
import com.rtctek.newsapp.R
import com.rtctek.newsapp.utils.CachePrefs
import com.rtctek.newsapp.utils.Constants
import com.rtctek.newsapp.utils.Event
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single source of truth for the UI:
 *
 *  * Category tabs read [observeCategory] (Room LiveData — instant after
 *    rotation) plus [categoryStatus] for loading/error feedback.
 *  * The saved screen reads [savedNews]; bookmark states across the app come
 *    from [savedUrls].
 *  * The search screen talks to [search] / [searchState].
 *
 * All network work is bound to [viewModelScope] so it is cancelled with the
 * ViewModel; all Room writes run inside transactions off the main thread.
 */
class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NewsDatabase.getDatabaseClient(application)
    private val dao = database.newsDao()
    private val repository = NewsRepository()

    private val categoryStatuses = HashMap<String, MutableLiveData<CategoryUiState>>()
    private val refreshJobs = HashMap<String, Job>()

    private val _refreshing = MediatorLiveData<Boolean>()
    val refreshing: LiveData<Boolean> = _refreshing

    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>> = _message

    private val _searchState = MutableLiveData(SearchUiState())
    val searchState: LiveData<SearchUiState> = _searchState

    private var searchJob: Job? = null

    /** The user's bookmarked stories (newest first). */
    val savedNews: LiveData<List<NewsModel>> = dao.observeSaved()

    /** URLs of all bookmarked stories, for live bookmark-state icons. */
    val savedUrls: LiveData<Set<String>> = dao.observeSavedUrls().map { it.toSet() }

    // ── Category feeds ──────────────────────────────────────────────

    /** Live, Room-backed articles for a category tab. */
    fun observeCategory(category: String): LiveData<List<NewsModel>> =
        dao.observeCategory(category)

    /** Loading/error state for a category tab, created on first request. */
    fun categoryStatus(category: String): LiveData<CategoryUiState> = statusFor(category)

    private fun statusFor(category: String): MutableLiveData<CategoryUiState> =
        categoryStatuses.getOrPut(category) {
            MutableLiveData(CategoryUiState()).also { status ->
                _refreshing.addSource(status) {
                    _refreshing.value =
                        categoryStatuses.values.any { it.value?.loading == true }
                }
            }
        }

    /**
     * Refreshes every category tab, unless the 30-minute cache is still fresh
     * and [force] is false (pull-to-refresh passes force = true).
     * The cache is only marked fresh when all tabs settle without errors.
     */
    fun refreshAll(force: Boolean) {
        if (!force && !CachePrefs.isStale(getApplication())) return
        var remaining = Constants.TAB_CATEGORIES.size
        var failures = 0
        Constants.TAB_CATEGORIES.forEach { tab ->
            startCategoryRefresh(tab.query) { success ->
                if (!success) failures++
                if (--remaining == 0 && failures == 0) {
                    CachePrefs.markFresh(getApplication())
                }
            }
        }
    }

    /** (Re)fetches a single category — used by the retry buttons. */
    fun refreshCategory(category: String) {
        startCategoryRefresh(category) { }
    }

    /**
     * Launches a category fetch unless one is already in flight. All state
     * mutation happens on the main thread (viewModelScope dispatcher).
     */
    private fun startCategoryRefresh(category: String, onSettled: (success: Boolean) -> Unit) {
        if (refreshJobs[category]?.isActive == true) {
            // A fetch is already running; treat as conservatively unsettled
            // (the in-flight job will update both data and status anyway).
            onSettled(false)
            return
        }
        val status = statusFor(category)
        status.value = (status.value ?: CategoryUiState()).copy(loading = true, error = null)

        refreshJobs[category] = viewModelScope.launch {
            val success = try {
                val articles = repository.fetchTopHeadlines(category = category)
                database.withTransaction {
                    dao.clearCategory(category)
                    dao.insertAll(articles)
                }
                status.postValue(CategoryUiState(loading = false))
                true
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                status.postValue(
                    CategoryUiState(loading = false, error = friendlyMessage(throwable))
                )
                false
            }
            onSettled(success)
        }
    }

    // ── Saved (bookmarked) news ─────────────────────────────────────

    /** Bookmarks [article], or removes it when it was already bookmarked. */
    fun toggleSaved(article: NewsModel) {
        val url = article.url ?: return
        viewModelScope.launch {
            if (dao.isArticleSaved(url)) {
                dao.deleteSavedByUrl(url)
                _message.value = Event(appString(R.string.removed_from_saved))
            } else {
                dao.insert(article.asSaved())
                _message.value = Event(appString(R.string.saved_for_later))
            }
        }
    }

    /** Re-inserts a previously removed article (Undo action). */
    fun insertSaved(article: NewsModel) {
        viewModelScope.launch { dao.insert(article.asSaved()) }
    }

    fun deleteSaved(article: NewsModel) {
        viewModelScope.launch { dao.deleteSavedByUrl(article.url) }
    }

    private fun NewsModel.asSaved(): NewsModel =
        copy(id = 0L, category = Constants.SAVED_CATEGORY, sortIndex = 0)

    // ── Search ──────────────────────────────────────────────────────

    /** Debounced (400 ms) full-text search against the "everything" endpoint. */
    fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchState.value = SearchUiState()
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _searchState.value = SearchUiState(loading = true, query = trimmed)
            try {
                val articles = repository.searchNews(trimmed)
                _searchState.value =
                    SearchUiState(loading = false, articles = articles, query = trimmed)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _searchState.value = SearchUiState(
                    loading = false,
                    error = friendlyMessage(throwable),
                    query = trimmed,
                )
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun friendlyMessage(throwable: Throwable): String = when (throwable) {
        is NewsApiException -> throwable.message ?: appString(R.string.error_generic)
        is IOException -> appString(R.string.error_no_internet)
        else -> appString(R.string.error_generic)
    }

    private fun appString(resId: Int): String = getApplication<Application>().getString(resId)

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
