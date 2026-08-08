package com.example.firefighterterminal

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.firefighterterminal.presentation.ui.analysis.AnalysisFragment
import com.example.firefighterterminal.presentation.ui.device.DeviceListFragment
import com.example.firefighterterminal.presentation.ui.map.FireMapFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val PAGE_DEVICE = 0
        const val PAGE_MAP = 1
        const val PAGE_ANALYSIS = 2
    }

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            PAGE_DEVICE -> DeviceListFragment.newInstance()
            PAGE_MAP -> FireMapFragment.newInstance()
            PAGE_ANALYSIS -> AnalysisFragment.newInstance()
            else -> throw IllegalArgumentException("Unknown position: $position")
        }
    }
}
