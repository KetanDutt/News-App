package com.rtctek.newsapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStateAdapter
import androidx.lifecycle.Lifecycle
import com.rtctek.newsapp.fragments.NewsListFragment
import com.rtctek.newsapp.utils.Constants

/** Backing adapter for the home-screen tab bar. */
class FragmentAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = Constants.TAB_CATEGORIES.size

    override fun createFragment(position: Int): Fragment = NewsListFragment.newInstance(
        category = Constants.TAB_CATEGORIES[position].query,
        isGeneral = position == 0,
    )
}
