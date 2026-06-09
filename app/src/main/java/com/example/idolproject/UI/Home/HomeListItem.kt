package com.example.idolproject.UI.Home

sealed class HomeListItem {

    data class Header(
        val title: String,
        val isTodaySection: Boolean
    ) : HomeListItem()

    data class Feed(
        val item: HomeFeedItem
    ) : HomeListItem()
}