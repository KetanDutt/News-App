package com.rtctek.newsapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.rtctek.newsapp.adapters.FragmentAdapter
import com.rtctek.newsapp.architecture.NewsViewModel
import com.rtctek.newsapp.utils.Constants
import com.rtctek.newsapp.utils.NetworkUtils

/**
 * Home screen: a toolbar, a category tab bar and a ViewPager2 of
 * [com.rtctek.newsapp.fragments.NewsListFragment]s. Loading, error and retry
 * handling lives inside each fragment; this activity only wires the shell,
 * pull-to-refresh and the app bar menu.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        viewPager.adapter = FragmentAdapter(supportFragmentManager, lifecycle)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(Constants.TAB_CATEGORIES[position].labelRes)
        }.attach()

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            if (NetworkUtils.isOnline(this)) {
                viewModel.refreshAll(force = true)
            } else {
                swipeRefresh.isRefreshing = false
                showMessage(getString(R.string.error_offline))
            }
        }

        viewModel.refreshing.observe(this) { swipeRefresh.isRefreshing = it == true }
        viewModel.message.observe(this) { event ->
            event.getIfNotHandled()?.let { showMessage(it) }
        }

        // No artificial startup delay: fragments render instantly from the
        // Room cache and refresh happens in the background when data is stale.
        viewModel.refreshAll(force = false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> {
            startActivity(Intent(this, SearchActivity::class.java))
            true
        }
        R.id.action_saved -> {
            startActivity(Intent(this, SavedNewsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showMessage(text: String) {
        Snackbar.make(findViewById(R.id.main_container), text, Snackbar.LENGTH_SHORT).show()
    }
}
