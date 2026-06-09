package com.example.idolproject.Drawer.Community

data class ChatRoom(
    val id: String = "",
    val groupName: String = "",
    val roomName: String = "",
    val lastMessage: String = "",
    val unreadCount: Int = 0,
    val imageResId: Int = com.example.idolproject.R.drawable.person_24dp,
    val lastMessageAt: Long = 0L
)

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isMe: Boolean = false
)