package com.example.idolproject.UI.Home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.HomeRepository
import com.prolificinteractive.materialcalendarview.CalendarDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>()
    val event: SharedFlow<HomeEvent> = _event.asSharedFlow()

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val uid = homeRepository.getCurrentUserId()
            if (uid == null) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    isEmpty = true,
                    emptyTitle = "표시할 일정이 없어요",
                    emptyMessage = "로그인이 필요합니다.",
                    fanTalkTitle = "최애 팬톡 바로가기",
                    fanTalkSub = "로그인 후 최애 그룹 팬톡방에 입장할 수 있어요."
                )
                return@launch
            }

            runCatching {
                val favoriteGroup = homeRepository.getHomeFavoriteGroup()

                if (favoriteGroup == null) {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        favoriteGroupId = null,
                        favoriteGroupName = null,
                        feedItems = emptyList(),
                        listItems = emptyList(),
                        fanTalkTitle = "최애 팬톡 바로가기",
                        fanTalkSub = "내 페이지에서 최애 그룹을 설정하면 팬톡방에 입장할 수 있어요.",
                        isEmpty = true,
                        emptyTitle = "표시할 일정이 없어요",
                        emptyMessage = "최애 그룹을 먼저 설정해 주세요."
                    )
                    return@launch
                }

                val schedules = homeRepository.getFavoriteGroupSchedules(
                    favoriteGroupId = favoriteGroup.groupId,
                    today = CalendarDay.today(),
                    days = 3
                )

                val feedItems = schedules.map { schedule ->
                    mapHomeScheduleToFeed(schedule)
                }

                val listItems = buildSectionedList(feedItems)

                val isEmpty = feedItems.isEmpty()

                _uiState.value = HomeUiState(
                    isLoading = false,
                    favoriteGroupId = favoriteGroup.groupId,
                    favoriteGroupName = favoriteGroup.groupName,
                    feedItems = feedItems,
                    listItems = listItems,
                    fanTalkTitle = "${favoriteGroup.groupName} 팬톡방",
                    fanTalkSub = "팬들과 실시간으로 대화해보세요.",
                    isEmpty = isEmpty,
                    emptyTitle = "표시할 일정이 없어요",
                    emptyMessage = if (isEmpty) {
                        "오늘은 일정이 없습니다."
                    } else {
                        ""
                    },
                    errorMessage = null
                )
            }.onFailure { throwable ->
                _uiState.value = HomeUiState(
                    isLoading = false,
                    isEmpty = true,
                    emptyTitle = "표시할 일정이 없어요",
                    emptyMessage = throwable.message ?: "홈 정보를 불러오지 못했습니다.",
                    fanTalkTitle = "최애 팬톡 바로가기",
                    fanTalkSub = "잠시 후 다시 시도해 주세요.",
                    errorMessage = throwable.message
                )

                _event.emit(
                    HomeEvent.ShowToast(
                        throwable.message ?: "홈 정보를 불러오지 못했습니다."
                    )
                )
            }
        }
    }

    fun openFavoriteGroupChat() {
        val current = _uiState.value
        val groupId = current.favoriteGroupId
        val groupName = current.favoriteGroupName

        if (groupId.isNullOrBlank()) {
            viewModelScope.launch {
                _event.emit(HomeEvent.ShowToast("최애 그룹을 먼저 설정해 주세요."))
            }
            return
        }

        viewModelScope.launch {
            _event.emit(
                HomeEvent.OpenGroupChat(
                    groupId = groupId,
                    roomName = "${groupName ?: groupId} 팬톡방"
                )
            )
        }
    }

    private fun buildSectionedList(feedItems: List<HomeFeedItem>): List<HomeListItem> {
        val todayPrefix = formatMonthDay(CalendarDay.today())

        val todayItems = feedItems.filter { item ->
            item.time.startsWith(todayPrefix)
        }

        val upcomingItems = feedItems.filterNot { item ->
            item.time.startsWith(todayPrefix)
        }

        val result = mutableListOf<HomeListItem>()

        if (todayItems.isNotEmpty()) {
            result.add(HomeListItem.Header("오늘 일정", true))
            result.addAll(todayItems.map { HomeListItem.Feed(it) })
        }

        if (upcomingItems.isNotEmpty()) {
            result.add(HomeListItem.Header("다가오는 일정", false))
            result.addAll(upcomingItems.map { HomeListItem.Feed(it) })
        }

        return result
    }

    private fun mapHomeScheduleToFeed(item: HomeScheduleItem): HomeFeedItem {
        val today = CalendarDay.today()

        val contentText = if (item.memo.isBlank()) {
            item.title
        } else {
            "${item.title} · ${item.memo}"
        }

        return HomeFeedItem(
            category = HomeCategory.SCHEDULE,
            groupId = item.groupId,
            groupName = item.groupName,
            time = "${formatMonthDay(item.date)} · ${item.label}",
            content = contentText,
            imageResId = null,
            isToday = item.date == today
        )
    }

    private fun formatMonthDay(date: CalendarDay): String {
        return String.format("%02d/%02d", date.month, date.day)
    }
}