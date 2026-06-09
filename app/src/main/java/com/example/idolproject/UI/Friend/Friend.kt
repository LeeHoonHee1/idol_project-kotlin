package com.example.idolproject.UI.Friend

data class Friend(
    val uid: String,
    val nickname: String,
    val statusMessage: String = "상태메시지 없음",
    val favoriteGroupId: String = "",
    val favoriteGroupName: String = "최애: -",
    val level: Int = 1,
    val badgeId: String = "bronze",
    val photoUrl: String? = null,
    val isSameFavorite: Boolean = false
)