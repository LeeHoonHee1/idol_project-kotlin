package com.example.idolproject.Drawer.Community

data class CommunityUiState(
    val isLoading: Boolean = true,
    val favoriteGroupId: String? = null,
    val favoriteGroupName: String? = null,
    val chatRooms: List<ChatRoom> = emptyList(),
    val descriptionText: String = "내 최애 그룹 기준으로 팬톡방이 표시돼요.",
    val isEmpty: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CommunityEvent {
    data class ShowToast(
        val message: String
    ) : CommunityEvent

    data class OpenChatRoom(
        val roomId: String,
        val roomName: String
    ) : CommunityEvent
}