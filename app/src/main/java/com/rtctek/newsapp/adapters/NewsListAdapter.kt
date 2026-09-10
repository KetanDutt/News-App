package com.rtctek.newsapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rtctek.newsapp.NewsModel
import com.rtctek.newsapp.R
import com.rtctek.newsapp.utils.TimeUtils

/**
 * Recycler adapter for news story rows, backed by [ListAdapter]'s async
 * DiffUtil (no more full `notifyDataSetChanged`).
 *
 * The bookmark button reflects the current saved set (see [setSavedUrls])
 * and forwards taps to [onBookmarkClickListener], which is expected to toggle
 * the saved state via the view model.
 *
 * @param showRelativeTime true → "5h ago" style labels (feeds),
 *                         false → absolute dates (saved list).
 */
class NewsListAdapter(
    private val showRelativeTime: Boolean,
    private val onItemClickListener: (NewsModel) -> Unit,
    private val onBookmarkClickListener: (NewsModel) -> Unit,
) : ListAdapter<NewsModel, NewsListAdapter.NewsViewHolder>(DIFF_CALLBACK) {

    private var savedUrls: Set<String> = emptySet()

    /** Updates the bookmark icons, rebinding only the rows whose state changed. */
    fun setSavedUrls(urls: Set<String>) {
        val previous = savedUrls
        savedUrls = urls
        currentList.forEachIndexed { index, article ->
            if ((article.url in previous) != (article.url in urls)) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = getItem(position)
        holder.bind(article, article.url in savedUrls)
    }

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.news_image)
        private val title: TextView = itemView.findViewById(R.id.news_title)
        private val meta: TextView = itemView.findViewById(R.id.news_meta)
        private val bookmarkButton: ImageButton = itemView.findViewById(R.id.bookmark_button)

        fun bind(article: NewsModel, isSaved: Boolean) {
            title.text = article.headLine
            meta.text = buildMetaLine(article)
            image.load(article.image) {
                crossfade(true)
                placeholder(R.drawable.samplenews)
                error(R.drawable.samplenews)
            }
            updateBookmarkIcon(isSaved)

            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onItemClickListener(getItem(position))
            }
            bookmarkButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onBookmarkClickListener(getItem(position))
            }
        }

        private fun updateBookmarkIcon(isSaved: Boolean) {
            bookmarkButton.setImageResource(
                if (isSaved) R.drawable.ic_baseline_bookmark_24
                else R.drawable.ic_baseline_bookmark_border_24
            )
            bookmarkButton.contentDescription = itemView.context.getString(
                if (isSaved) R.string.cd_remove_bookmark else R.string.cd_add_bookmark
            )
        }

        private fun buildMetaLine(article: NewsModel): String {
            val source = article.source?.takeIf { it.isNotBlank() }
            val time = if (showRelativeTime) {
                TimeUtils.relativeTime(article.time)
            } else {
                TimeUtils.formatDate(article.time)
            }
            return listOfNotNull(source, time).joinToString(separator = "  •  ")
        }
    }

    companion object {

        /**
         * Rows are regenerated (new auto-generated ids) on every refresh, so
         * identity is based on the stable article URL, and content comparison
         * deliberately skips the volatile id.
         */
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NewsModel>() {
            override fun areItemsTheSame(oldItem: NewsModel, newItem: NewsModel): Boolean =
                oldItem.category == newItem.category && oldItem.url == newItem.url

            override fun areContentsTheSame(oldItem: NewsModel, newItem: NewsModel): Boolean =
                oldItem.headLine == newItem.headLine &&
                    oldItem.image == newItem.image &&
                    oldItem.description == newItem.description &&
                    oldItem.source == newItem.source &&
                    oldItem.time == newItem.time
        }
    }
}
