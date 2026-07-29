package com.example.idolproject.Drawer.Event

sealed interface EventUiState {
    data object Loading : EventUiState

    data class Success(
        val events: List<EventItem>,
        val message: String = "최신 이벤트를 확인했어요"
    ) : EventUiState

    data class Empty(
        val message: String = "표시할 이벤트가 없어요"
    ) : EventUiState

    data class Error(
        val message: String
    ) : EventUiState
}