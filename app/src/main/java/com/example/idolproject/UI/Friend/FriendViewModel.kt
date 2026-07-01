package com.example.idolproject.UI.Friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.FriendRepository
import com.example.idolproject.data.repository.FriendSearchResult
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
class FriendViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _friendListUiState = MutableStateFlow(FriendListUiState())
    val friendListUiState: StateFlow<FriendListUiState> = _friendListUiState.asStateFlow()

    private val _friendSearchUiState = MutableStateFlow(FriendSearchUiState())
    val friendSearchUiState: StateFlow<FriendSearchUiState> = _friendSearchUiState.asStateFlow()

    private val _friendRequestsUiState = MutableStateFlow(FriendRequestsUiState())
    val friendRequestsUiState: StateFlow<FriendRequestsUiState> = _friendRequestsUiState.asStateFlow()

    private val _event = MutableSharedFlow<FriendEvent>()
    val event: SharedFlow<FriendEvent> = _event.asSharedFlow()

    private var friendListJob: Job? = null
    private var pendingCountJob: Job? = null
    private var requestsJob: Job? = null

    fun startObserveFriendList() {
        val myUid = friendRepository.getCurrentUserId()

        if (myUid == null) {
            _friendListUiState.value = FriendListUiState(
                isLoading = false,
                errorMessage = "로그인이 필요합니다."
            )
            emitToast("로그인이 필요합니다.")
            return
        }

        friendListJob?.cancel()
        friendListJob = viewModelScope.launch {
            _friendListUiState.value = _friendListUiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            runCatching {
                friendRepository.observeFriends(myUid).collect { friends ->
                    _friendListUiState.value = _friendListUiState.value.copy(
                        isLoading = false,
                        friends = friends,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _friendListUiState.value = _friendListUiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "친구 목록을 불러오지 못했습니다."
                )
                emitToast(throwable.message ?: "친구 목록을 불러오지 못했습니다.")
            }
        }
    }

    fun startObservePendingRequestCount() {
        val myUid = friendRepository.getCurrentUserId()

        if (myUid == null) {
            _friendListUiState.value = _friendListUiState.value.copy(
                pendingRequestCount = 0
            )
            return
        }

        pendingCountJob?.cancel()
        pendingCountJob = viewModelScope.launch {
            runCatching {
                friendRepository.observePendingRequestCount(myUid).collect { count ->
                    _friendListUiState.value = _friendListUiState.value.copy(
                        pendingRequestCount = count
                    )
                }
            }.onFailure {
                _friendListUiState.value = _friendListUiState.value.copy(
                    pendingRequestCount = 0
                )
            }
        }
    }

    fun startObserveFriendRequests() {
        val myUid = friendRepository.getCurrentUserId()

        if (myUid == null) {
            _friendRequestsUiState.value = FriendRequestsUiState(
                isLoading = false,
                errorMessage = "로그인이 필요합니다."
            )
            emitToast("로그인이 필요합니다.")
            return
        }

        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            _friendRequestsUiState.value = _friendRequestsUiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            runCatching {
                friendRepository.observeFriendRequests(myUid).collect { requests ->
                    _friendRequestsUiState.value = FriendRequestsUiState(
                        isLoading = false,
                        requests = requests,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _friendRequestsUiState.value = FriendRequestsUiState(
                    isLoading = false,
                    requests = emptyList(),
                    errorMessage = throwable.message ?: "친구 요청 목록을 불러오지 못했습니다."
                )
                emitToast(throwable.message ?: "친구 요청 목록을 불러오지 못했습니다.")
            }
        }
    }

    fun searchUserByNickname(input: String) {
        val query = input.trim()

        if (query.isBlank()) {
            _friendSearchUiState.value = FriendSearchUiState(
                isLoading = false,
                query = query,
                result = null,
                message = "닉네임을 입력해주세요."
            )
            emitToast("닉네임을 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _friendSearchUiState.value = FriendSearchUiState(
                isLoading = true,
                query = query,
                result = null,
                message = null
            )

            runCatching {
                friendRepository.searchUserByNickname(query)
            }.onSuccess { result ->
                when (result) {
                    FriendSearchResult.LoginRequired -> {
                        _friendSearchUiState.value = FriendSearchUiState(
                            isLoading = false,
                            query = query,
                            result = null,
                            message = "로그인이 필요합니다."
                        )
                        emitToast("로그인이 필요합니다.")
                    }

                    FriendSearchResult.NotFound -> {
                        _friendSearchUiState.value = FriendSearchUiState(
                            isLoading = false,
                            query = query,
                            result = null,
                            message = "해당 닉네임의 유저를 찾을 수 없습니다."
                        )
                        emitToast("해당 닉네임의 유저를 찾을 수 없습니다.")
                    }

                    is FriendSearchResult.Found -> {
                        _friendSearchUiState.value = FriendSearchUiState(
                            isLoading = false,
                            query = query,
                            result = result.profile,
                            message = null
                        )
                    }
                }
            }.onFailure { throwable ->
                _friendSearchUiState.value = FriendSearchUiState(
                    isLoading = false,
                    query = query,
                    result = null,
                    message = throwable.message ?: "유저 검색에 실패했습니다."
                )
                emitToast(throwable.message ?: "유저 검색에 실패했습니다.")
            }
        }
    }

    fun sendFriendRequest(receiverUid: String) {
        viewModelScope.launch {
            runCatching {
                friendRepository.sendFriendRequest(receiverUid)
            }.onSuccess { result ->
                val message = when (result) {
                    SendResult.Sent -> "친구 요청을 보냈습니다."
                    SendResult.AlreadyPending -> "이미 친구 요청을 보낸 상태입니다."
                    SendResult.AlreadyFriends -> "이미 친구입니다."
                    is SendResult.Failed -> result.message
                }

                emitToast(message)

                val currentResult = _friendSearchUiState.value.result
                if (result == SendResult.Sent && currentResult != null) {
                    _friendSearchUiState.value = _friendSearchUiState.value.copy(
                        result = currentResult.copy(
                            hasPendingRequest = true
                        )
                    )
                }
            }.onFailure { throwable ->
                emitToast(throwable.message ?: "친구 요청에 실패했습니다.")
            }
        }
    }

    fun acceptRequest(senderUid: String) {
        viewModelScope.launch {
            runCatching {
                friendRepository.acceptRequest(senderUid)
            }.onSuccess { success ->
                if (success) {
                    emitToast("친구 요청을 수락했습니다.")
                } else {
                    emitToast("친구 요청 수락에 실패했습니다.")
                }
            }.onFailure { throwable ->
                emitToast(throwable.message ?: "친구 요청 수락에 실패했습니다.")
            }
        }
    }

    fun rejectRequest(senderUid: String) {
        viewModelScope.launch {
            runCatching {
                friendRepository.rejectRequest(senderUid)
            }.onSuccess { success ->
                if (success) {
                    emitToast("친구 요청을 거절했습니다.")
                } else {
                    emitToast("친구 요청 거절에 실패했습니다.")
                }
            }.onFailure { throwable ->
                emitToast(throwable.message ?: "친구 요청 거절에 실패했습니다.")
            }
        }
    }

    fun markAllPendingChecked() {
        viewModelScope.launch {
            runCatching {
                friendRepository.markAllPendingChecked()
            }
        }
    }

    fun deleteFriend(friendUid: String) {
        viewModelScope.launch {
            runCatching {
                friendRepository.deleteFriend(friendUid)
            }.onSuccess {
                emitToast("친구를 삭제했습니다.")
            }.onFailure { throwable ->
                emitToast(throwable.message ?: "친구 삭제에 실패했습니다.")
            }
        }
    }

    fun openFriendProfile(friendUid: String) {
        viewModelScope.launch {
            _event.emit(FriendEvent.OpenFriendProfile(friendUid))
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _event.emit(FriendEvent.ShowToast(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        friendListJob?.cancel()
        pendingCountJob?.cancel()
        requestsJob?.cancel()
    }
}