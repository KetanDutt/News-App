package com.rtctek.newsapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.rtctek.newsapp.adapters.NewsListAdapter
import com.rtctek.newsapp.architecture.NewsViewModel
import com.rtctek.newsapp.architecture.SearchUiState

/**
 * Full-text news search. Queries are debounced in the view model; results
 * share the same list adapter used by the category feeds.
 */
class SearchActivity : AppCompatActivity() {

    private val viewModel: NewsViewModel by viewModels()

    private lateinit var adapter: NewsListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptySubtitle: TextView
    private lateinit var errorState: View
    private lateinit var errorMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = NewsListAdapter(
            showRelativeTime = true,
            onItemClickListener = ::openArticle,
            onBookmarkClickListener = viewModel::toggleSaved,
        )
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        shimmer = findViewById(R.id.shimmer_layout)
        emptyState = findViewById(R.id.empty_state)
        emptyTitle = emptyState.findViewById(R.id.empty_title)
        emptySubtitle = emptyState.findViewById(R.id.empty_subtitle)
        errorState = findViewById(R.id.error_state)
        errorMessage = errorState.findViewById(R.id.error_message)
        // A failed search is retried by typing again, not by the retry button.
        errorState.findViewById<View>(R.id.retry_button).isVisible = false

        viewModel.searchState.observe(this) { render(it) }
        viewModel.savedUrls.observe(this) { adapter.setSavedUrls(it) }
        viewModel.message.observe(this) { event ->
            event.getIfNotHandled()?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText.orEmpty())
                return true
            }
        })
        searchView.requestFocus()
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun render(state: SearchUiState) {
        if (state.loading) {
            shimmer.startShimmer()
        } else {
            shimmer.stopShimmer()
        }
        shimmer.isVisible = state.loading

        val hasResults = state.articles.isNotEmpty()
        recyclerView.isVisible = hasResults
        adapter.submitList(state.articles)

        val showEmpty = !state.loading && !hasResults && state.error == null
        val showError = !state.loading && !hasResults && state.error != null

        emptyState.isVisible = showEmpty
        if (showEmpty) {
            if (state.query.isBlank()) {
                emptyTitle.setText(R.string.search_prompt_title)
                emptySubtitle.setText(R.string.search_prompt_subtitle)
            } else {
                emptyTitle.text = getString(R.string.no_results_title)
                emptySubtitle.text = getString(R.string.no_results_subtitle, state.query)
            }
        }

        errorState.isVisible = showError
        if (showError) errorMessage.text = state.error
    }

    private fun openArticle(article: NewsModel) {
        startActivity(Intent(this, ReadNewsActivity::class.java).putArticleExtras(article))
    }
}
