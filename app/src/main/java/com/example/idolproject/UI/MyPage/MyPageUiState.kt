package com.example.idolproject.UI.MyPage

data class MyPageUiState(
    val isLoading: Boolean = true,
    val nickname: String = "닉네임 없음",
    val statusMessage: String = "상태메시지를 입력해 주세요.",
    val level: Int = 1,
    val exp: Int = 0,
    val expProgress: Int = 0,
    val nextExpText: String = "다음 레벨까지 100 EXP",
    val badgeId: String = "bronze",
    val badgeResId: Int = 0,
    val favoriteGroupId: String = "",
    val favoriteGroupName: String = "최애 그룹을 선택해 주세요",
    val profileImageUrl: String = "",
    val errorMessage: String? = null
)

sealed interface MyPageEvent {
    data class ShowToast(
        val message: String
    ) : MyPageEvent

    data class ShowNicknameInputError(
        val message: String
    ) : MyPageEvent
}