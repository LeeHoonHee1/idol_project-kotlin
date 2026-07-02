package com.example.idolproject.Drawer.Group

import com.prolificinteractive.materialcalendarview.CalendarDay

data class GroupScheduleUiState(
    val isLoading: Boolean = true,
    val schedules: List<GroupScheduleItem> = emptyList(),
    val favoriteGroupIds: List<String> = emptyList(),
    val selectedDate: CalendarDay = CalendarDay.today(),
    val selectedGroupId: String? = null,
    val isAdmin: Boolean = false,
    val selectedDateItems: List<GroupScheduleItem> = emptyList(),
    val selectedDateInfoText: String = "전체 · 오늘 · 등록된 일정이 없어요",
    val errorMessage: String? = null
)

sealed interface GroupScheduleEvent {
    data class ShowToast(
        val message: String
    ) : GroupScheduleEvent

    data class ScheduleAdded(
        val item: GroupScheduleItem
    ) : GroupScheduleEvent

    data class ScheduleUpdated(
        val item: GroupScheduleItem
    ) : GroupScheduleEvent

    data object ScheduleDeleted : GroupScheduleEvent
}