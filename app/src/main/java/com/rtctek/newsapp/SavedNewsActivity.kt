package com.rtctek.newsapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.rtctek.newsapp.adapters.NewsListAdapter
import com.rtctek.newsapp.architecture.NewsViewModel

/**
 * The user's bookmarked stories. Swipe a row to delete it (with Undo),
 * tap the bookmark icon to remove it, or tap the row to read the story.
 */
class SavedNewsActivity : AppCompatActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_news)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapter = NewsListAdapter(
            showRelativeTime = false,
            onItemClickListener = ::openArticle,
            onBookmarkClickListener = viewModel::toggleSaved,
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val snackbarAnchor = findViewById<View>(R.id.saved_container)
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val article = adapter.currentList[position]
                viewModel.deleteSaved(article)
                Snackbar.make(snackbarAnchor, R.string.removed_from_saved, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.insertSaved(article) }
                    .show()
            }
        }).attachToRecyclerView(recyclerView)

        viewModel.savedNews.observe(this) { articles ->
            adapter.submitList(articles)
            findViewById<View>(R.id.empty_state).isVisible = articles.isEmpty()
        }
        viewModel.savedUrls.observe(this) { adapter.setSavedUrls(it) }
        viewModel.message.observe(this) { event ->
            event.getIfNotHandled()?.let {
                Snackbar.make(snackbarAnchor, it, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun openArticle(article: NewsModel) {
        startActivity(Intent(this, ReadNewsActivity::class.java).putArticleExtras(article))
    }
}
