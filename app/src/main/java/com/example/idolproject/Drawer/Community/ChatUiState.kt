package com.example.idolproject.Drawer.Community

data class ChatUiState(
    val isLoading: Boolean = true,
    val roomId: String = "",
    val roomName: String = "채팅방",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSendEnabled: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ChatEvent {
    data class ShowToast(
        val message: String
    ) : ChatEvent

    data object ScrollToBottom : ChatEvent

    data object ClearInput : ChatEvent
}