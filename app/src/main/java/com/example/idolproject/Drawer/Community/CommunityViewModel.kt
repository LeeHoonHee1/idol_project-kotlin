package com.example.idolproject.Drawer.Community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.CommunityRepository
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
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CommunityEvent>()
    val event: SharedFlow<CommunityEvent> = _event.asSharedFlow()

    private var roomJob: Job? = null

    fun start() {
        loadMyFavoriteGroupAndObserveRoom()
    }

    fun refresh() {
        loadMyFavoriteGroupAndObserveRoom()
    }

    private fun loadMyFavoriteGroupAndObserveRoom() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val uid = communityRepository.getCurrentUserId()
            if (uid == null) {
                stopRoomObserver()

                _uiState.value = CommunityUiState(
                    isLoading = false,
                    favoriteGroupId = null,
                    favoriteGroupName = null,
                    chatRooms = emptyList(),
                    descriptionText = "로그인 후 커뮤니티를 이용할 수 있어요.",
                    isEmpty = true,
                    errorMessage = null
                )
                return@launch
            }

            runCatching {
                communityRepository.getMyFavoriteGroup()
            }.onSuccess { favoriteGroup ->
                if (favoriteGroup == null) {
                    stopRoomObserver()

                    _uiState.value = CommunityUiState(
                        isLoading = false,
                        favoriteGroupId = null,
                        favoriteGroupName = null,
                        chatRooms = emptyList(),
                        descriptionText = "내 페이지에서 최애 그룹을 먼저 설정해 주세요.",
                        isEmpty = true,
                        errorMessage = null
                    )
                    return@onSuccess
                }

                _uiState.value = CommunityUiState(
                    isLoading = false,
                    favoriteGroupId = favoriteGroup.groupId,
                    favoriteGroupName = favoriteGroup.groupName,
                    chatRooms = emptyList(),
                    descriptionText = "내 최애 그룹 기준으로 팬톡방이 표시돼요.",
                    isEmpty = false,
                    errorMessage = null
                )

                observeFavoriteRoom(
                    groupId = favoriteGroup.groupId,
                    groupName = favoriteGroup.groupName
                )
            }.onFailure { throwable ->
                stopRoomObserver()

                _uiState.value = CommunityUiState(
                    isLoading = false,
                    favoriteGroupId = null,
                    favoriteGroupName = null,
                    chatRooms = emptyList(),
                    descriptionText = "최애 그룹 정보를 불러오지 못했습니다.",
                    isEmpty = true,
                    errorMessage = throwable.message
                )

                _event.emit(
                    CommunityEvent.ShowToast(
                        throwable.message ?: "최애 그룹 정보를 불러오지 못했습니다."
                    )
                )
            }
        }
    }

    private fun observeFavoriteRoom(
        groupId: String,
        groupName: String
    ) {
        roomJob?.cancel()
        roomJob = viewModelScope.launch {
            communityRepository.observeFavoriteGroupChatRoom(
                groupId = groupId,
                groupName = groupName
            ).collect { room ->
                val current = _uiState.value

                _uiState.value = current.copy(
                    isLoading = false,
                    favoriteGroupId = groupId,
                    favoriteGroupName = groupName,
                    chatRooms = listOf(room),
                    descriptionText = "내 최애 그룹 기준으로 팬톡방이 표시돼요.",
                    isEmpty = false,
                    errorMessage = null
                )
            }
        }
    }

    fun openChatRoom(room: ChatRoom) {
        viewModelScope.launch {
            _event.emit(
                CommunityEvent.OpenChatRoom(
                    roomId = room.id,
                    roomName = room.roomName.ifBlank {
                        "${room.groupName} 팬톡방"
                    }
                )
            )
        }
    }

    private fun stopRoomObserver() {
        roomJob?.cancel()
        roomJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRoomObserver()
    }
}