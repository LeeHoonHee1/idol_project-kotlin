package com.example.idolproject.Data

import com.example.idolproject.Drawer.ComeBack.ComebackItem
import com.example.idolproject.Drawer.Group.GroupActivityType
import com.example.idolproject.Drawer.Group.GroupScheduleItem
import com.prolificinteractive.materialcalendarview.CalendarDay

object ScheduleDummyRepository {

    private val comebackItems = mutableListOf(
        ComebackItem(
            id = "1",
            groupId = "ive",
            groupName = "IVE",
            date = CalendarDay.from(2026, 4, 4),
            title = "IVE 컴백",
            memo = "네 번째 미니앨범 발매"
        ),
        ComebackItem(
            id = "2",
            groupId = "newjeans",
            groupName = "NewJeans",
            date = CalendarDay.from(2026, 4, 12),
            title = "NewJeans 컴백",
            memo = "디지털 싱글 발매"
        ),
        ComebackItem(
            id = "3",
            groupId = "aespa",
            groupName = "aespa",
            date = CalendarDay.from(2026, 4, 12),
            title = "aespa 컴백",
            memo = "정규 앨범 공개"
        )
    )

    private val groupScheduleItems = mutableListOf(
        GroupScheduleItem(
            id = "1",
            groupId = "ive",
            groupName = "IVE",
            date = CalendarDay.from(2026, 4, 3),
            type = GroupActivityType.MUSIC_SHOW,
            title = "뮤직뱅크 출연",
            memo = "KBS 생방송"
        ),
        GroupScheduleItem(
            id = "2",
            groupId = "newjeans",
            groupName = "NewJeans",
            date = CalendarDay.from(2026, 4, 3),
            type = GroupActivityType.RADIO,
            title = "라디오 게스트",
            memo = "SBS 파워FM"
        ),
        GroupScheduleItem(
            id = "3",
            groupId = "aespa",
            groupName = "aespa",
            date = CalendarDay.from(2026, 4, 4),
            type = GroupActivityType.VARIETY,
            title = "예능 출연",
            memo = "주말 예능 녹화"
        )
    )



    fun getComebackItems(): List<ComebackItem> {
        return comebackItems.toList()
    }

    fun addComebackItem(item: ComebackItem) {
        comebackItems.add(item)
    }

    fun getGroupScheduleItems(): List<GroupScheduleItem> {
        return groupScheduleItems.toList()
    }

    fun addGroupScheduleItem(item: GroupScheduleItem) {
        groupScheduleItems.add(item)
    }
}