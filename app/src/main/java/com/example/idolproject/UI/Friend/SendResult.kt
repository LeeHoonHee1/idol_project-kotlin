package com.example.idolproject.UI.Friend

sealed interface SendResult {
    data object Sent : SendResult
    data object AlreadyPending : SendResult
    data object AlreadyFriends : SendResult

    data class Failed(
        val message: String
    ) : SendResult
}