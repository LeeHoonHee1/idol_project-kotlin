package com.example.idolproject.UI.Friend

enum class FriendRequestStatus(val raw: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected"),
    CANCELED("canceled"),
    UNFRIENDED("unfriended");

    companion object {
        fun from(raw: String?): FriendRequestStatus =
            values().firstOrNull { it.raw == raw } ?: PENDING
    }
}