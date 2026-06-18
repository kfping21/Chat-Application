package com.zjgsu.treehole.ui

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zjgsu.treehole.ui.tabs.MessagesTabFragment
import com.zjgsu.treehole.ui.tabs.NotificationsTabFragment

class EchoesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment {
        return if (position == 0) NotificationsTabFragment() else MessagesTabFragment()
    }
}
