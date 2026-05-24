package app.zlauncher.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        PAGE_WIDGETS -> WidgetsFragment()
        PAGE_HOME -> HomeFragment()
        PAGE_DRAWER -> AppDrawerFragment()
        else -> HomeFragment()
    }

    companion object {
        const val PAGE_WIDGETS = 0
        const val PAGE_HOME = 1
        const val PAGE_DRAWER = 2
    }
}
