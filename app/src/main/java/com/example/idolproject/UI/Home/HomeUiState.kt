package com.example.idolproject.UI.Home

data class HomeUiState(
    val isLoading: Boolean = true,
    val favoriteGroupId: String? = null,
    val favoriteGroupName: String? = null,
    val feedItems: List<HomeFeedItem> = emptyList(),
    val listItems: List<HomeListItem> = emptyList(),
    val fanTalkTitle: String = "최애 팬톡 바로가기",
    val fanTalkSub: String = "내 페이지에서 최애 그룹을 설정하면 팬톡방에 입장할 수 있어요.",
    val isEmpty: Boolean = true,
    val emptyTitle: String = "표시할 일정이 없어요",
    val emptyMessage: String = "최애 그룹을 먼저 설정해 주세요.",
    val errorMessage: String? = null
)

sealed interface HomeEvent {
    data class ShowToast(
        val message: String
    ) : HomeEvent

    data class OpenGroupChat(
        val groupId: String,
        val roomName: String
    ) : HomeEvent
}