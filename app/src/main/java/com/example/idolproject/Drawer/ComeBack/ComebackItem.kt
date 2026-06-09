package com.example.idolproject.Drawer.ComeBack

import com.prolificinteractive.materialcalendarview.CalendarDay

data class ComebackItem(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val date: CalendarDay = CalendarDay.today(),
    val title: String = "",
    val memo: String = ""
)