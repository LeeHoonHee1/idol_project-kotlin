package com.example.idolproject.UI.Mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

class DailyMissionFragment : Fragment() {

    private lateinit var cardAttendance: MaterialCardView
    private lateinit var ivDailyIcon: ImageView
    private lateinit var tvDailyStatus: TextView
    private lateinit var tvDailyHint: TextView

    private lateinit var cardTestExp: MaterialCardView
    private lateinit var tvTestStatus: TextView
    private lateinit var tvTestHint: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val missionRepository = MissionRepository()

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
        checkTodayMissionStatus()
        setupTestMissionUi()
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
            completeAttendanceMission()
        }

        cardTestExp.setOnClickListener {
            grantTestExp()
        }
    }

    private fun setupTestMissionUi() {
        tvTestStatus.text = "반복 가능"
        tvTestHint.text = "레벨, EXP, 뱃지 반영을 빠르게 확인할 수 있는 테스트 카드예요."
        cardTestExp.alpha = 1.0f
        cardTestExp.isEnabled = true
    }

    private fun checkTodayMissionStatus() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "로그인 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
            updateDailyMissionUi(isCompleted = false)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                missionRepository.isTodayAttendanceCompleted(uid)
            }.onSuccess { isCompleted ->
                updateDailyMissionUi(isCompleted)
            }.onFailure {
                updateDailyMissionUi(isCompleted = false)
                Toast.makeText(
                    requireContext(),
                    "미션 상태를 불러오지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun completeAttendanceMission() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "로그인 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                cardAttendance.isEnabled = false

                val result = missionRepository.completeDailyAttendance(uid)

                if (result.isSuccess) {
                    updateDailyMissionUi(isCompleted = true)

                    showMissionRewardDialog(
                        icon = "🎉",
                        title = "출석 완료!",
                        message = "오늘의 팬 활동 미션을 완료했어요.",
                        rewardText = "EXP +${MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP}"
                    )
                } else {
                    cardAttendance.isEnabled = true
                    val message = result.exceptionOrNull()?.message ?: "출석 처리에 실패했습니다."
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    checkTodayMissionStatus()
                }
            } catch (e: Exception) {
                cardAttendance.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "출석 처리 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateDailyMissionUi(isCompleted: Boolean) {
        if (isCompleted) {
            tvDailyStatus.text = "완료"
            tvDailyStatus.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_mission_status_done)
            tvDailyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )

            ivDailyIcon.setImageResource(R.drawable.ic_mission_check)

            tvDailyHint.text =
                "오늘 보상 수령 완료 · 내일 다시 참여해보세요"

            cardAttendance.alpha = 0.92f
            cardAttendance.isEnabled = false
            cardAttendance.strokeColor =
                ContextCompat.getColor(requireContext(), R.color.lavender)

            playMissionCompleteAnimation(cardAttendance)
        } else {
            tvDailyStatus.text = "0 / 1"
            tvDailyStatus.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_lavender_chip)
            tvDailyStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.lavender)
            )

            ivDailyIcon.setImageResource(R.drawable.ic_mission_daily)

            tvDailyHint.text =
                "오늘의 팬 활동을 시작해보세요 · 완료 시 EXP +${MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP}"

            cardAttendance.alpha = 1.0f
            cardAttendance.isEnabled = true
            cardAttendance.strokeColor =
                ContextCompat.getColor(requireContext(), R.color.lavender)
        }
    }

    private fun grantTestExp() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "로그인 정보를 확인해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                cardTestExp.isEnabled = false
                tvTestStatus.text = "지급 중"

                val result = missionRepository.grantTestExp(uid, 100)

                if (result.isSuccess) {
                    tvTestStatus.text = "반복 가능"
                    tvTestHint.text = "테스트 EXP +100 지급 완료! 내 페이지, 친구, 랭킹 반영을 확인해보세요."

                    playMissionCompleteAnimation(cardTestExp)

                    if (result.isSuccess) {
                        tvTestStatus.text = "반복 가능"
                        tvTestHint.text = "테스트 EXP +100 지급 완료! 내 페이지, 친구, 랭킹 반영을 확인해보세요."

                        playMissionCompleteAnimation(cardTestExp)

                        showMissionRewardDialog(
                            icon = "🧪",
                            title = "테스트 EXP 지급 완료",
                            message = "레벨과 뱃지 변화를 확인하기 위한 테스트 보상을 지급했어요.",
                            rewardText = "EXP +100"
                        )
                    }
                } else {
                    tvTestStatus.text = "반복 가능"
                    val message = result.exceptionOrNull()?.message ?: "테스트 EXP 지급 실패"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }

                cardTestExp.isEnabled = true
            } catch (e: Exception) {
                tvTestStatus.text = "반복 가능"
                cardTestExp.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    "테스트 EXP 지급 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
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