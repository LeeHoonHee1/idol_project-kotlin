package com.example.idolproject.Drawer.Community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ChatEvent>()
    val event: SharedFlow<ChatEvent> = _event.asSharedFlow()

    private var messageJob: Job? = null

    fun start(
        roomId: String?,
        roomName: String?
    ) {
        val safeRoomId = roomId.orEmpty()
        val safeRoomName = roomName.orEmpty().ifBlank { "채팅방" }

        if (safeRoomId.isBlank()) {
            _uiState.value = ChatUiState(
                isLoading = false,
                roomId = "",
                roomName = safeRoomName,
                messages = emptyList(),
                inputText = "",
                isSendEnabled = false,
                errorMessage = "채팅방 정보가 없습니다."
            )
            emitToast("채팅방 정보가 없습니다.")
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            roomId = safeRoomId,
            roomName = safeRoomName,
            errorMessage = null
        )

        observeMessages(
            roomId = safeRoomId,
            roomName = safeRoomName
        )
    }

    private fun observeMessages(
        roomId: String,
        roomName: String
    ) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            runCatching {
                chatRepository.observeMessages(roomId).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        roomId = roomId,
                        roomName = roomName,
                        messages = messages,
                        errorMessage = null
                    )

                    if (messages.isNotEmpty()) {
                        _event.emit(ChatEvent.ScrollToBottom)
                    }

                    markAsRead()
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "메시지를 불러오지 못했습니다."
                )

                _event.emit(
                    ChatEvent.ShowToast(
                        throwable.message ?: "메시지를 불러오지 못했습니다."
                    )
                )
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(
            inputText = text,
            isSendEnabled = text.trim().isNotBlank()
        )
    }

    fun sendCurrentMessage() {
        val current = _uiState.value
        val text = current.inputText.trim()

        if (current.roomId.isBlank()) {
            emitToast("채팅방 정보가 없습니다.")
            return
        }

        if (text.isBlank()) {
            return
        }

        viewModelScope.launch {
            runCatching {
                chatRepository.sendMessage(
                    roomId = current.roomId,
                    roomName = current.roomName,
                    text = text
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    inputText = "",
                    isSendEnabled = false
                )

                _event.emit(ChatEvent.ClearInput)
            }.onFailure { throwable ->
                _event.emit(
                    ChatEvent.ShowToast(
                        throwable.message ?: "메시지 전송에 실패했습니다."
                    )
                )
            }
        }
    }

    fun markAsRead() {
        val roomId = _uiState.value.roomId
        if (roomId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                chatRepository.markAsRead(roomId)
            }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _event.emit(ChatEvent.ShowToast(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageJob?.cancel()
    }
}