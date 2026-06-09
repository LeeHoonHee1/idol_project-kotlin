package com.example.idolproject.UI.Ranking

data class UserRank(
    val uid: String = "",
    val nickname: String = "",
    val level: Long = 1L,
    val exp: Long = 0L,
    val badgeId: String = "",
    val profileImageUrl: String = "",
    val favoriteGroupId: String = "",
    val pointsTotal: Long = 0L
)