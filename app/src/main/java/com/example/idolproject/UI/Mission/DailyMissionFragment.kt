package com.example.idolproject.UI.Mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.idolproject.R
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DailyMissionFragment : Fragment() {

    private val viewModel: MissionViewModel by viewModels()

    private lateinit var cardAttendance: MaterialCardView
    private lateinit var ivDailyIcon: ImageView
    private lateinit var tvDailyStatus: TextView
    private lateinit var tvDailyHint: TextView

    private lateinit var cardTestExp: MaterialCardView
    private lateinit var tvTestStatus: TextView
    private lateinit var tvTestHint: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_mission_daily, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupClickListeners()
        setupTestMissionUi()
        observeDailyMissionUiState()
        observeMissionEvent()

        viewModel.loadDailyMissionStatus()
    }

    private fun bindViews(view: View) {
        cardAttendance = view.findViewById(R.id.card_daily_attendance)
        ivDailyIcon = view.findViewById(R.id.iv_daily_icon)
        tvDailyStatus = view.findViewById(R.id.tv_daily_status)
        tvDailyHint = view.findViewById(R.id.tv_daily_hint)

        cardTestExp = view.findViewById(R.id.card_test_exp)
        tvTestStatus = view.findViewById(R.id.tv_test_status)
        tvTestHint = view.findViewById(R.id.tv_test_hint)
    }

    private fun setupClickListeners() {
        cardAttendance.setOnClickListener {
            viewModel.completeDailyAttendance()
        }

        cardTestExp.setOnClickListener {
            viewModel.grantTestExp()
        }
    }

    private fun setupTestMissionUi() {
        tvTestStatus.text = "반복 가능"
        tvTestHint.text = "레벨, EXP, 뱃지 반영을 빠르게 확인할 수 있는 테스트 카드예요."
        cardTestExp.alpha = 1.0f
        cardTestExp.isEnabled = true
    }

    private fun observeDailyMissionUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dailyUiState.collect { uiState ->
                    bindDailyMissionUi(uiState)
                }
            }
        }
    }

    private fun observeMissionEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is MissionEvent.ShowToast -> {
                            Toast.makeText(
                                requireContext(),
                                event.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is MissionEvent.ShowRewardDialog -> {
                            showMissionRewardDialog(
                                icon = event.icon,
                                title = event.title,
                                message = event.message,
                                rewardText = event.rewardText
                            )

                            if (event.title.contains("테스트")) {
                                tvTestStatus.text = "반복 가능"
                                tvTestHint.text = "테스트 EXP +100 지급 완료! 내 페이지, 친구, 랭킹 반영을 확인해보세요."
                                cardTestExp.isEnabled = true
                                playMissionCompleteAnimation(cardTestExp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindDailyMissionUi(uiState: DailyMissionUiState) {
        tvDailyStatus.text = uiState.buttonText
        tvDailyHint.text = uiState.descriptionText
        cardAttendance.isEnabled = uiState.buttonEnabled

        if (uiState.isLoading) {
            cardAttendance.alpha = 0.7f
            ivDailyIcon.setImageResource(R.drawable.ic_mission_daily)
            tvDailyStatus.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_lavender_chip
            )
            tvDailyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.lavender)
            )
            return
        }

        if (uiState.isCompleted) {
            tvDailyStatus.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_mission_status_done
            )
            tvDailyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )
            ivDailyIcon.setImageResource(R.drawable.ic_mission_check)
            cardAttendance.alpha = 0.92f
            cardAttendance.strokeColor = ContextCompat.getColor(
                requireContext(),
                R.color.lavender
            )
            playMissionCompleteAnimation(cardAttendance)
        } else {
            tvDailyStatus.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_lavender_chip
            )
            tvDailyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.lavender)
            )
            ivDailyIcon.setImageResource(R.drawable.ic_mission_daily)
            cardAttendance.alpha = 1.0f
            cardAttendance.strokeColor = ContextCompat.getColor(
                requireContext(),
                R.color.lavender
            )
        }
    }

    private fun playMissionCompleteAnimation(target: View) {
        target.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(120)
            .withEndAction {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun showMissionRewardDialog(
        icon: String,
        title: String,
        message: String,
        rewardText: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_mission_reward, null)

        val tvIcon = dialogView.findViewById<TextView>(R.id.tv_dialog_icon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val tvReward = dialogView.findViewById<TextView>(R.id.tv_dialog_reward)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_dialog_confirm)

        tvIcon.text = icon
        tvTitle.text = title
        tvMessage.text = message
        tvReward.text = rewardText

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}