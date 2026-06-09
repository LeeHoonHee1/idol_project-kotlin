package com.example.idolproject.Drawer.Group

import com.prolificinteractive.materialcalendarview.CalendarDay

enum class GroupActivityType(val displayName: String) {
    FAN_SIGN("팬싸"),
    VARIETY("예능"),
    RADIO("라디오"),
    MUSIC_SHOW("음방"),
    OTHER("기타")
}

data class GroupScheduleItem(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val date: CalendarDay = CalendarDay.today(),
    val type: GroupActivityType = GroupActivityType.OTHER,
    val title: String = "",
    val memo: String = ""
)

data class GroupScheduleDto(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val date: String = "",
    val type: String = "",
    val title: String = "",
    val memo: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L
)