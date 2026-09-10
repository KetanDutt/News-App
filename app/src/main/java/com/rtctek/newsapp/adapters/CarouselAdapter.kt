package com.rtctek.newsapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rtctek.newsapp.NewsModel
import com.rtctek.newsapp.R
import com.rtctek.newsapp.utils.TimeUtils

/**
 * Simple adapter powering the "Top Headlines" carousel (ViewPager2 pages) on
 * the Home tab. Replaces the previous third-party CarouselView dependency.
 */
class CarouselAdapter(
    private var items: List<NewsModel>,
    private val onItemClickListener: (NewsModel) -> Unit,
) : RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_headline, parent, false)
        return CarouselViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun submit(newItems: List<NewsModel>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.headline_image)
        private val title: TextView = itemView.findViewById(R.id.headline)
        private val meta: TextView = itemView.findViewById(R.id.headline_meta)

        fun bind(article: NewsModel) {
            title.text = article.headLine
            meta.text = listOfNotNull(
                article.source?.takeIf { it.isNotBlank() },
                TimeUtils.relativeTime(article.time),
            ).joinToString(separator = "  •  ")
            image.load(article.image) {
                crossfade(true)
                placeholder(R.drawable.samplenews)
                error(R.drawable.samplenews)
            }
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // items is captured; safe because submit() triggers a rebind.
                    onItemClickListener(items[position])
                }
            }
        }
    }
}
