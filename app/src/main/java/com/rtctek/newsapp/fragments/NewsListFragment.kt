package com.rtctek.newsapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.rtctek.newsapp.NewsModel
import com.rtctek.newsapp.R
import com.rtctek.newsapp.ReadNewsActivity
import com.rtctek.newsapp.adapters.CarouselAdapter
import com.rtctek.newsapp.adapters.NewsListAdapter
import com.rtctek.newsapp.architecture.CategoryUiState
import com.rtctek.newsapp.architecture.NewsViewModel
import com.rtctek.newsapp.putArticleExtras
import com.rtctek.newsapp.utils.Constants
import com.rtctek.newsapp.utils.Constants.TOP_HEADLINES_COUNT

/**
 * One news category tab. Every tab uses this single fragment — the Home tab
 * additionally shows a "Top Headlines" carousel.
 *
 * Articles come from the Room cache via the shared activity-scoped
 * [NewsViewModel]; the status LiveData drives shimmer/error placeholders.
 */
class NewsListFragment : Fragment() {

    private val viewModel: NewsViewModel by activityViewModels()

    private val category: String
        get() = requireArguments().getString(ARG_CATEGORY) ?: Constants.GENERAL

    private val isGeneral: Boolean
        get() = requireArguments().getBoolean(ARG_IS_GENERAL)

    private lateinit var adapter: NewsListAdapter
    private var carouselAdapter: CarouselAdapter? = null

    private var latestArticles: List<NewsModel> = emptyList()
    private var latestStatus: CategoryUiState = CategoryUiState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val layout = if (isGeneral) R.layout.fragment_general else R.layout.fragment_news_list
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NewsListAdapter(
            showRelativeTime = true,
            onItemClickListener = ::openArticle,
            onBookmarkClickListener = viewModel::toggleSaved,
        )

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_view)
        if (isGeneral) {
            // The Home tab lists inside a NestedScrollView; disable its own
            // nested scrolling so everything scrolls as one page.
            recycler.isNestedScrollingEnabled = false
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<View>(R.id.retry_button)?.setOnClickListener {
            viewModel.refreshCategory(category)
        }

        viewModel.observeCategory(category).observe(viewLifecycleOwner) { articles ->
            latestArticles = articles
            if (isGeneral) setupCarousel(view, articles)
            val listForAdapter =
                if (isGeneral && articles.size > TOP_HEADLINES_COUNT) {
                    articles.drop(TOP_HEADLINES_COUNT)
                } else {
                    articles
                }
            adapter.submitList(listForAdapter)
            updatePlaceholderViews(view)
        }

        viewModel.categoryStatus(category).observe(viewLifecycleOwner) { status ->
            latestStatus = status
            updatePlaceholderViews(view)
        }

        viewModel.savedUrls.observe(viewLifecycleOwner) { adapter.setSavedUrls(it) }
    }

    /** Shows the top-headlines carousel once the feed has enough content. */
    private fun setupCarousel(view: View, articles: List<NewsModel>) {
        val container = view.findViewById<View>(R.id.carousel_container)
        if (articles.size <= TOP_HEADLINES_COUNT) {
            container.isVisible = false
            return
        }
        container.isVisible = true

        val topHeadlines = articles.take(TOP_HEADLINES_COUNT)
        val existing = carouselAdapter
        if (existing != null) {
            existing.submit(topHeadlines)
            return
        }

        val pager = view.findViewById<ViewPager2>(R.id.carousel_pager)
        carouselAdapter = CarouselAdapter(topHeadlines, ::openArticle).also {
            pager.adapter = it
            TabLayoutMediator(view.findViewById(R.id.carousel_dots), pager) { _, _ -> }.attach()
        }
    }

    private fun updatePlaceholderViews(view: View) {
        val nothingLoaded = latestArticles.isEmpty()

        val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.shimmer_layout)
        if (latestStatus.loading && nothingLoaded) {
            shimmer.isVisible = true
            shimmer.startShimmer()
        } else {
            shimmer.stopShimmer()
            shimmer.isVisible = false
        }

        val errorState = view.findViewById<View>(R.id.error_state)
        errorState.isVisible = !latestStatus.loading && nothingLoaded
        errorState.findViewById<TextView>(R.id.error_message)?.text =
            latestStatus.error ?: getString(R.string.no_articles)
    }

    private fun openArticle(article: NewsModel) {
        startActivity(
            Intent(requireContext(), ReadNewsActivity::class.java).putArticleExtras(article)
        )
    }

    companion object {
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_IS_GENERAL = "arg_is_general"

        fun newInstance(category: String, isGeneral: Boolean = false): NewsListFragment =
            NewsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                    putBoolean(ARG_IS_GENERAL, isGeneral)
                }
            }
    }
}
