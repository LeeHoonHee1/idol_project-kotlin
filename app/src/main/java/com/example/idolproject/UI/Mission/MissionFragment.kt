package com.example.idolproject.UI.Mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.idolproject.R
import com.example.idolproject.databinding.FragmentMissionBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MissionFragment : Fragment() {

    private var _binding: FragmentMissionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MissionViewModel by viewModels()

    private lateinit var ivMissionBadge: ImageView
    private lateinit var tvMissionLevel: TextView
    private lateinit var tvMissionExp: TextView
    private lateinit var progressMissionExp: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMissionPager()
        bindGrowthViews()
        observeMissionGrowthUiState()

        viewModel.startObserveMissionGrowthProfile()
    }

    private fun setupMissionPager() {
        val adapter = MissionPagerAdapter(this)
        binding.vpMission.adapter = adapter

        TabLayoutMediator(binding.tabMission, binding.vpMission) { tab, position ->
            tab.text = when (position) {
                0 -> "일일 미션"
                1 -> "주간 미션"
                else -> ""
            }
        }.attach()
    }

    private fun bindGrowthViews() {
        ivMissionBadge = binding.ivMissionBadge
        tvMissionLevel = binding.tvMissionLevel
        tvMissionExp = binding.tvMissionExp
        progressMissionExp = binding.progressMissionExp
    }

    private fun observeMissionGrowthUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.growthUiState.collect { uiState ->
                    bindMissionGrowthUi(uiState)
                }
            }
        }
    }

    private fun bindMissionGrowthUi(uiState: MissionGrowthUiState) {
        when {
            uiState.isLoading -> {
                setGrowthLoadingState()
            }

            uiState.errorMessage != null -> {
                setGrowthErrorState()
            }

            else -> {
                tvMissionLevel.text = "Lv.${uiState.level}"
                tvMissionExp.text = "EXP ${uiState.exp} / ${uiState.needExp} (${uiState.expPercent}%)"

                progressMissionExp.max = 100
                progressMissionExp.progress = uiState.expPercent

                ivMissionBadge.setImageResource(mapBadgeRes(uiState.badgeId))
            }
        }
    }

    private fun setGrowthLoadingState() {
        tvMissionLevel.text = "Lv.-"
        tvMissionExp.text = "EXP 불러오는 중..."
        progressMissionExp.max = 100
        progressMissionExp.progress = 0
        ivMissionBadge.setImageResource(R.drawable.ic_badge_bronze)
    }

    private fun setGrowthErrorState() {
        tvMissionLevel.text = "Lv.-"
        tvMissionExp.text = "EXP 정보를 불러오지 못했어요"
        progressMissionExp.max = 100
        progressMissionExp.progress = 0
        ivMissionBadge.setImageResource(R.drawable.ic_badge_bronze)
    }

    private fun mapBadgeRes(badgeId: String): Int {
        return when (badgeId) {
            "bronze" -> R.drawable.ic_badge_bronze
            "silver" -> R.drawable.ic_badge_silver
            "gold" -> R.drawable.ic_badge_gold
            "platinum" -> R.drawable.ic_badge_platinum
            "master" -> R.drawable.ic_badge_master
            "grandmaster" -> R.drawable.ic_badge_grandmaster
            "challenger" -> R.drawable.ic_badge_challenger
            else -> R.drawable.ic_badge_bronze
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}