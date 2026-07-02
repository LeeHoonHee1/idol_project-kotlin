package com.example.idolproject.Drawer.ComeBack

import com.prolificinteractive.materialcalendarview.CalendarDay

data class ComebackScheduleUiState(
    val isLoading: Boolean = true,
    val comebacks: List<ComebackItem> = emptyList(),
    val favoriteGroupIds: List<String> = emptyList(),
    val selectedDate: CalendarDay = CalendarDay.today(),
    val selectedGroupId: String? = null,
    val isAdmin: Boolean = false,
    val selectedDateItems: List<ComebackItem> = emptyList(),
    val selectedDateInfoText: String = "전체 · 오늘 · 등록된 컴백 일정 없음",
    val isSelectedDateEmpty: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ComebackScheduleEvent {
    data class ShowToast(
        val message: String
    ) : ComebackScheduleEvent

    data class ComebackAdded(
        val item: ComebackItem
    ) : ComebackScheduleEvent

    data class ComebackUpdated(
        val item: ComebackItem
    ) : ComebackScheduleEvent

    data object ComebackDeleted : ComebackScheduleEvent
}