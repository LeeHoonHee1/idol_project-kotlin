package com.example.idolproject.UI.Mission

data class DailyMissionUiState(
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
    val buttonEnabled: Boolean = false,
    val buttonText: String = "확인 중...",
    val descriptionText: String = "오늘의 출석 상태를 확인하고 있습니다."
)

data class WeeklyMissionUiState(
    val isLoading: Boolean = true,
    val completedDailyCount: Int = 0,
    val isRewardAvailable: Boolean = false,
    val isRewardReceived: Boolean = false,
    val buttonEnabled: Boolean = false,
    val buttonText: String = "확인 중...",
    val descriptionText: String = "이번 주 미션 상태를 확인하고 있습니다."
)

data class MissionGrowthUiState(
    val isLoading: Boolean = true,
    val level: Int = 1,
    val exp: Int = 0,
    val needExp: Int = 100,
    val expPercent: Int = 0,
    val badgeId: String = "bronze",
    val errorMessage: String? = null
)

sealed interface MissionEvent {
    data object PlayDailyCompleteAnimation : MissionEvent

    data class ShowToast(
        val message: String
    ) : MissionEvent

    data class ShowRewardDialog(
        val icon: String = "",
        val title: String,
        val message: String,
        val rewardText: String
    ) : MissionEvent
}