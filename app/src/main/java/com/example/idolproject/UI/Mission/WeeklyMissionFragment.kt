package com.example.idolproject.UI.Mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.util.CoilUtils.result
import com.example.idolproject.R
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class WeeklyMissionFragment : Fragment() {

    private lateinit var cardWeeklyAttendance: MaterialCardView
    private lateinit var ivWeeklyIcon: ImageView
    private lateinit var tvWeeklyStatus: TextView
    private lateinit var tvWeeklyHint: TextView
    private lateinit var progressWeeklyMission: ProgressBar

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val missionRepository = MissionRepository()

    private var currentWeeklyCount: Int = 0
    private var isRewardClaimed: Boolean = false

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
        loadWeeklyMissionStatus()
    }

    override fun onResume() {
        super.onResume()
        loadWeeklyMissionStatus()
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
            handleWeeklyMissionClick()
        }
    }

    private fun loadWeeklyMissionStatus() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            currentWeeklyCount = 0
            isRewardClaimed = false
            updateWeeklyMissionUi(count = 0, claimed = false)
            Toast.makeText(requireContext(), "로그인 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val weeklyCount = missionRepository.getWeeklyAttendanceCount(uid)
                val claimed = missionRepository.hasClaimedWeeklyReward(uid)

                currentWeeklyCount = weeklyCount
                isRewardClaimed = claimed

                updateWeeklyMissionUi(weeklyCount, claimed)
            } catch (e: Exception) {
                e.printStackTrace()

                currentWeeklyCount = 0
                isRewardClaimed = false
                updateWeeklyMissionUi(count = 0, claimed = false)

                Toast.makeText(
                    requireContext(),
                    "주간 미션 상태를 불러오지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleWeeklyMissionClick() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "로그인 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentWeeklyCount < 7) {
            Toast.makeText(
                requireContext(),
                "아직 주간 미션을 완료하지 않았습니다. ($currentWeeklyCount / 7)",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isRewardClaimed) {
            Toast.makeText(
                requireContext(),
                "이번 주 보상은 이미 받았습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                cardWeeklyAttendance.isEnabled = false

                val result = missionRepository.claimWeeklyReward(uid)

                if (result.isSuccess) {
                    isRewardClaimed = true
                    updateWeeklyMissionUi(currentWeeklyCount, claimed = true)
                    playMissionRewardReadyAnimation(cardWeeklyAttendance)

                    showMissionRewardDialog(
                        icon = "🏆",
                        title = "주간 보상 획득!",
                        message = "이번 주 7회 출석 미션을 완료했어요.",
                        rewardText = "EXP +${MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP}"
                    )
                } else {
                    cardWeeklyAttendance.isEnabled = true

                    val message = result.exceptionOrNull()?.message ?: "주간 보상 지급에 실패했습니다."
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                    loadWeeklyMissionStatus()
                }
            } catch (e: Exception) {
                e.printStackTrace()

                cardWeeklyAttendance.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    "주간 보상 처리 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateWeeklyMissionUi(count: Int, claimed: Boolean) {
        val safeCount = count.coerceIn(0, 7)

        progressWeeklyMission.max = 7
        progressWeeklyMission.progress = safeCount

        when {
            safeCount < 7 -> {
                val remainCount = 7 - safeCount

                tvWeeklyStatus.text = "$safeCount / 7"
                tvWeeklyStatus.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_lavender_chip)
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.lavender)
                )

                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_weekly)

                tvWeeklyHint.text =
                    "이번 주 ${safeCount}회 출석 완료 · 보상까지 ${remainCount}회 남았어요"

                cardWeeklyAttendance.alpha = 1.0f
                cardWeeklyAttendance.isEnabled = true
                cardWeeklyAttendance.strokeColor =
                    ContextCompat.getColor(requireContext(), R.color.lavender)
            }

            claimed -> {
                tvWeeklyStatus.text = "수령 완료"
                tvWeeklyStatus.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_mission_status_done)
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.black)
                )

                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_check)

                tvWeeklyHint.text =
                    "이번 주 보상 수령 완료 · 다음 주에 다시 도전해보세요"

                cardWeeklyAttendance.alpha = 0.92f
                cardWeeklyAttendance.isEnabled = false
                cardWeeklyAttendance.strokeColor =
                    ContextCompat.getColor(requireContext(), R.color.lavender)
            }

            else -> {
                tvWeeklyStatus.text = "보상 받기"
                tvWeeklyStatus.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_mission_status_reward)
                tvWeeklyStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )

                ivWeeklyIcon.setImageResource(R.drawable.ic_mission_gift)

                tvWeeklyHint.text =
                    "조건 달성 완료! 탭해서 주간 보상 EXP +${MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP} 받기"

                cardWeeklyAttendance.alpha = 1.0f
                cardWeeklyAttendance.isEnabled = true
                cardWeeklyAttendance.strokeColor =
                    ContextCompat.getColor(requireContext(), R.color.lavender)

                playMissionRewardReadyAnimation(cardWeeklyAttendance)
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
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btn_dialog_confirm
        )

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