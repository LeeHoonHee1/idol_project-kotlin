package com.example.idolproject.UI.Home

import com.prolificinteractive.materialcalendarview.CalendarDay

data class HomeScheduleItem(
    val source: HomeScheduleSource,
    val groupId: String,
    val groupName: String,
    val date: CalendarDay,
    val label: String,
    val title: String,
    val memo: String
)

enum class HomeScheduleSource {
    COMEBACK,
    GROUP_SCHEDULE
}