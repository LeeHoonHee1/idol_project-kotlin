package com.example.idolproject.UI.Ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.idolproject.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class RankingFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tab_ranking)
        viewPager = view.findViewById(R.id.viewpager_ranking)

        // 뷰페이저 어댑터 연결
        viewPager.adapter = RankingPagerAdapter(this)

        // 탭과 뷰페이저 연결
        val tabTitles = arrayOf("유저 랭킹", "그룹 랭킹")

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    /** 뷰페이저용 어댑터 */
    private inner class RankingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> UserRankingFragment()   // 유저 레벨 랭킹 탭
                1 -> GroupRankingFragment()  // 그룹 하트 랭킹 탭
                else -> UserRankingFragment()
            }
        }
    }
}