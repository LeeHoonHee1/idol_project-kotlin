package com.example.idolproject.UI.Mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
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
class WeeklyMissionFragment : Fragment() {

    private val viewModel: MissionViewModel by viewModels()

    private lateinit var cardWeeklyAttendance: MaterialCardView
    private lateinit var ivWeeklyIcon: ImageView
    private lateinit var tvWeeklyStatus: TextView
    private lateinit var tvWeeklyHint: TextView
    private lateinit var progressWeeklyMission: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_mission_weekly, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupClickListeners()
        observeWeeklyMissionUiState()
        observeMissionEvent()

        viewModel.loadWeeklyMissionStatus()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadWeeklyMissionStatus()
    }

    private fun bindViews(view: View) {
        cardWeeklyAttendance = view.findViewById(R.id.card_weekly_attendance)
        ivWeeklyIcon = view.findViewById(R.id.iv_weekly_icon)
        tvWeeklyStatus = view.findViewById(R.id.tv_weekly_status)
        tvWeeklyHint = view.findViewById(R.id.tv_weekly_hint)
        progressWeeklyMission = view.findViewById(R.id.progress_weekly_mission)
        progressWeeklyMission.max = 7
    }

    private fun setupClickListeners() {
        cardWeeklyAttendance.setOnClickListener {
            viewModel.claimWeeklyReward()
        }
    }

    private fun observeWeeklyMissionUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyUiState.collect { uiState ->
                    bindWeeklyMissionUi(uiState)
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

                            if (event.title.contains("주간")) {
                                playMissionRewardReadyAnimation(cardWeeklyAttendance)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindWeeklyMissionUi(uiState: WeeklyMissionUiState) {
        val safeCount = uiState.completedDailyCount.coerceIn(0, 7)

        progressWeeklyMission.max = 7
        progressWeeklyMission.progress = safeCount

        tvWeeklyStatus.text = uiState.buttonText
        tvWeeklyHint.text = uiState.descriptionText
        cardWeeklyAttendance.isEnabled = uiState.buttonEnabled

        when {
            uiState.isLoading -> {
                tvWeeklyStatus.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_lavender_chip
                )
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.lavender)
                )
                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_weekly)
                cardWeeklyAttendance.alpha = 0.7f
                cardWeeklyAttendance.strokeColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.lavender
                )
            }

            uiState.isRewardReceived -> {
                tvWeeklyStatus.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_mission_status_done
                )
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.black)
                )
                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_check)
                cardWeeklyAttendance.alpha = 0.92f
                cardWeeklyAttendance.strokeColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.lavender
                )
            }

            uiState.isRewardAvailable -> {
                tvWeeklyStatus.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_mission_status_reward
                )
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_gift)
                cardWeeklyAttendance.alpha = 1.0f
                cardWeeklyAttendance.strokeColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.lavender
                )
                playMissionRewardReadyAnimation(cardWeeklyAttendance)
            }

            else -> {
                tvWeeklyStatus.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_lavender_chip
                )
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.lavender)
                )
                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_weekly)
                cardWeeklyAttendance.alpha = 1.0f
                cardWeeklyAttendance.strokeColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.lavender
                )
            }
        }
    }

    private fun playMissionRewardReadyAnimation(target: View) {
        target.animate().cancel()
        target.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(180)
            .withEndAction {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
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