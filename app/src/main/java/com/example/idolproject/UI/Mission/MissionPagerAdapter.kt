package com.example.idolproject.UI.Mission  // 네 패키지에 맞게 수정해줘

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.idolproject.UI.Mission.DailyMissionFragment
import com.example.idolproject.UI.Mission.WeeklyMissionFragment

class MissionPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2   // 탭 2개 (일일, 주간)

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DailyMissionFragment()
            1 -> WeeklyMissionFragment()
            else -> DailyMissionFragment()
        }
    }
}
