package com.example.idolproject.UI.Home

data class HomeFeedItem(
    val category: HomeCategory,
    val groupId: String,
    val groupName: String,
    val time: String,
    val content: String,
    val imageResId: Int? = null,
    val isToday: Boolean = false
)

enum class HomeCategory {
    SCHEDULE,
    NEWS,
    PHOTO,
    POPULAR
}