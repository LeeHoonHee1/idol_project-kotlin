package com.example.idolproject.UI.Mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch

@HiltViewModel
class MissionViewModel @Inject constructor(
    private val missionRepository: MissionRepository
) : ViewModel() {
    private val _dailyUiState = MutableStateFlow(DailyMissionUiState())
    val dailyUiState: StateFlow<DailyMissionUiState> = _dailyUiState.asStateFlow()

    private val _weeklyUiState = MutableStateFlow(WeeklyMissionUiState())
    val weeklyUiState: StateFlow<WeeklyMissionUiState> = _weeklyUiState.asStateFlow()

    private val _growthUiState = MutableStateFlow(MissionGrowthUiState())
    val growthUiState: StateFlow<MissionGrowthUiState> = _growthUiState.asStateFlow()

    private var growthJob: Job? = null

    private val _event = MutableSharedFlow<MissionEvent>()
    val event: SharedFlow<MissionEvent> = _event.asSharedFlow()

    fun loadDailyMissionStatus() {
        val uid = missionRepository.getCurrentUserId()

        if (uid == null) {
            _dailyUiState.value = DailyMissionUiState(
                isLoading = false,
                isCompleted = false,
                buttonEnabled = false,
                buttonText = "로그인 필요",
                descriptionText = "로그인 정보를 확인해주세요."
            )

            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        viewModelScope.launch {
            _dailyUiState.value = _dailyUiState.value.copy(
                isLoading = true,
                buttonEnabled = false,
                buttonText = "확인 중...",
                descriptionText = "오늘의 출석 상태를 확인하고 있습니다."
            )

            runCatching {
                missionRepository.isTodayAttendanceCompleted(uid)
            }.onSuccess { isCompleted ->
                updateDailyUiState(isCompleted)
            }.onFailure {
                _dailyUiState.value = DailyMissionUiState(
                    isLoading = false,
                    isCompleted = false,
                    buttonEnabled = true,
                    buttonText = "출석하기",
                    descriptionText = "오늘의 팬 활동을 시작해보세요 · 완료 시 EXP +${MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP}"
                )

                _event.emit(
                    MissionEvent.ShowToast("미션 상태를 불러오지 못했습니다.")
                )
            }
        }
    }

    fun completeDailyAttendance() {
        val uid = missionRepository.getCurrentUserId()

        if (uid == null) {
            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        viewModelScope.launch {
            _dailyUiState.value = _dailyUiState.value.copy(
                buttonEnabled = false,
                buttonText = "처리 중..."
            )

            val result = missionRepository.completeDailyAttendance(uid)

            if (result.isSuccess) {
                updateDailyUiState(isCompleted = true)

                _event.emit(MissionEvent.PlayDailyCompleteAnimation)

                _event.emit(
                    MissionEvent.ShowRewardDialog(
                        icon = "",
                        title = "출석 완료!",
                        message = "오늘의 팬 활동 미션을 완료했어요.",
                        rewardText = "EXP +${MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP}"
                    )
                )
            } else {
                val message = result.exceptionOrNull()?.message ?: "출석 처리에 실패했습니다."

                _dailyUiState.value = _dailyUiState.value.copy(
                    buttonEnabled = true,
                    buttonText = "출석하기"
                )

                _event.emit(MissionEvent.ShowToast(message))
                loadDailyMissionStatus()
            }
        }
    }

    fun grantTestExp() {
        val uid = missionRepository.getCurrentUserId()

        if (uid == null) {
            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        viewModelScope.launch {
            val result = missionRepository.grantTestExp(uid, 100)

            if (result.isSuccess) {
                _event.emit(
                    MissionEvent.ShowRewardDialog(
                        icon = "",
                        title = "테스트 EXP 지급 완료",
                        message = "레벨과 뱃지 변화를 확인하기 위한 테스트 보상을 지급했어요.",
                        rewardText = "EXP +100"
                    )
                )
            } else {
                val message = result.exceptionOrNull()?.message ?: "테스트 EXP 지급 실패"
                _event.emit(MissionEvent.ShowToast(message))
            }
        }
    }

    fun loadWeeklyMissionStatus() {
        val uid = missionRepository.getCurrentUserId()

        if (uid == null) {
            _weeklyUiState.value = WeeklyMissionUiState(
                isLoading = false,
                completedDailyCount = 0,
                isRewardAvailable = false,
                isRewardReceived = false,
                buttonEnabled = false,
                buttonText = "로그인 필요",
                descriptionText = "로그인 정보를 확인해주세요."
            )

            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        viewModelScope.launch {
            _weeklyUiState.value = _weeklyUiState.value.copy(
                isLoading = true,
                buttonEnabled = false,
                buttonText = "확인 중...",
                descriptionText = "이번 주 미션 상태를 확인하고 있습니다."
            )

            runCatching {
                val weeklyCount = missionRepository.getWeeklyAttendanceCount(uid)
                val claimed = missionRepository.hasClaimedWeeklyReward(uid)
                weeklyCount to claimed
            }.onSuccess { (weeklyCount, claimed) ->
                updateWeeklyUiState(
                    count = weeklyCount,
                    claimed = claimed
                )
            }.onFailure {
                _weeklyUiState.value = WeeklyMissionUiState(
                    isLoading = false,
                    completedDailyCount = 0,
                    isRewardAvailable = false,
                    isRewardReceived = false,
                    buttonEnabled = true,
                    buttonText = "0 / 7",
                    descriptionText = "주간 미션 상태를 불러오지 못했습니다."
                )

                _event.emit(
                    MissionEvent.ShowToast("주간 미션 상태를 불러오지 못했습니다.")
                )
            }
        }
    }

    fun claimWeeklyReward() {
        val uid = missionRepository.getCurrentUserId()

        if (uid == null) {
            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        val currentState = _weeklyUiState.value

        if (currentState.completedDailyCount < 7) {
            emitToast("아직 주간 미션을 완료하지 않았습니다. (${currentState.completedDailyCount} / 7)")
            return
        }

        if (currentState.isRewardReceived) {
            emitToast("이번 주 보상은 이미 받았습니다.")
            return
        }

        viewModelScope.launch {
            _weeklyUiState.value = currentState.copy(
                buttonEnabled = false,
                buttonText = "처리 중..."
            )

            val result = missionRepository.claimWeeklyReward(uid)

            if (result.isSuccess) {
                updateWeeklyUiState(
                    count = currentState.completedDailyCount,
                    claimed = true
                )

                _event.emit(
                    MissionEvent.ShowRewardDialog(
                        icon = "",
                        title = "주간 보상 획득!",
                        message = "이번 주 7회 출석 미션을 완료했어요.",
                        rewardText = "EXP +${MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP}"
                    )
                )
            } else {
                val message = result.exceptionOrNull()?.message ?: "주간 보상 지급에 실패했습니다."

                _event.emit(MissionEvent.ShowToast(message))
                loadWeeklyMissionStatus()
            }
        }
    }

    private fun updateDailyUiState(isCompleted: Boolean) {
        _dailyUiState.value = if (isCompleted) {
            DailyMissionUiState(
                isLoading = false,
                isCompleted = true,
                buttonEnabled = false,
                buttonText = "완료",
                descriptionText = "오늘 보상 수령 완료 · 내일 다시 참여해보세요"
            )
        } else {
            DailyMissionUiState(
                isLoading = false,
                isCompleted = false,
                buttonEnabled = true,
                buttonText = "0 / 1",
                descriptionText = "오늘의 팬 활동을 시작해보세요 · 완료 시 EXP +${MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP}"
            )
        }
    }

    private fun updateWeeklyUiState(
        count: Int,
        claimed: Boolean
    ) {
        val safeCount = count.coerceIn(0, 7)
        val isRewardAvailable = safeCount >= 7 && !claimed

        _weeklyUiState.value = when {
            safeCount < 7 -> {
                val remainCount = 7 - safeCount

                WeeklyMissionUiState(
                    isLoading = false,
                    completedDailyCount = safeCount,
                    isRewardAvailable = false,
                    isRewardReceived = false,
                    buttonEnabled = true,
                    buttonText = "$safeCount / 7",
                    descriptionText = "이번 주 ${safeCount}회 출석 완료 · 보상까지 ${remainCount}회 남았어요"
                )
            }

            claimed -> {
                WeeklyMissionUiState(
                    isLoading = false,
                    completedDailyCount = safeCount,
                    isRewardAvailable = false,
                    isRewardReceived = true,
                    buttonEnabled = false,
                    buttonText = "수령 완료",
                    descriptionText = "이번 주 보상 수령 완료 · 다음 주에 다시 도전해보세요"
                )
            }

            else -> {
                WeeklyMissionUiState(
                    isLoading = false,
                    completedDailyCount = safeCount,
                    isRewardAvailable = isRewardAvailable,
                    isRewardReceived = false,
                    buttonEnabled = true,
                    buttonText = "보상 받기",
                    descriptionText = "조건 달성 완료! 탭해서 주간 보상 EXP +${MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP} 받기"
                )
            }
        }
    }

    fun startObserveMissionGrowthProfile() {
        growthJob?.cancel()
        growthJob = viewModelScope.launch {
            _growthUiState.value = MissionGrowthUiState(
                isLoading = true,
                errorMessage = null
            )

            missionRepository.observeMissionGrowthProfile()
                .catch { throwable ->
                    _growthUiState.value = MissionGrowthUiState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "성장 정보를 불러오지 못했습니다."
                    )
                }
                .collect { profile ->
                    _growthUiState.value = MissionGrowthUiState(
                        isLoading = false,
                        level = profile.level,
                        exp = profile.exp,
                        needExp = profile.needExp,
                        expPercent = profile.expPercent,
                        badgeId = profile.badgeId,
                        errorMessage = null
                    )
                }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _event.emit(MissionEvent.ShowToast(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        growthJob?.cancel()
    }
}

