package com.example.idolproject.Drawer.Event

data class EventItem(
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val reward: String,
    val status: EventStatus
)

enum class EventStatus(
    val label: String
) {
    ACTIVE("진행중"),
    UPCOMING("예정"),
    FINISHED("종료")
}