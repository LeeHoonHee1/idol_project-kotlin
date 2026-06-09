package com.example.idolproject.UI.Friend

object FriendRequestIdUtil {
    fun requestId(senderUid: String, receiverUid: String): String {
        return "${senderUid}_${receiverUid}"
    }
}