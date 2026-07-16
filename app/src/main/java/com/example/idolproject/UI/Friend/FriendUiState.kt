package com.example.idolproject.UI.Friend

import com.example.idolproject.data.repository.FriendSearchProfile
import com.example.idolproject.data.repository.FriendProfile

data class FriendListUiState(
    val isLoading: Boolean = true,
    val friends: List<Friend> = emptyList(),
    val pendingRequestCount: Int = 0,
    val errorMessage: String? = null
)

data class FriendSearchUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val result: FriendSearchProfile? = null,
    val message: String? = null
)

data class FriendRequestsUiState(
    val isLoading: Boolean = true,
    val requests: List<FriendRequestItem> = emptyList(),
    val errorMessage: String? = null
)

data class FriendProfileUiState(
    val isLoading: Boolean = true,
    val profile: FriendProfile? = null,
    val errorMessage: String? = null
)

sealed interface FriendEvent {
    data class ShowToast(
        val message: String
    ) : FriendEvent

    data class OpenFriendProfile(
        val friendUid: String
    ) : FriendEvent
}