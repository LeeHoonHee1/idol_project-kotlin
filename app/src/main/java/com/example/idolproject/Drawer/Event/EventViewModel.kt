package com.example.idolproject.Drawer.Event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.EventRefreshResult
import com.example.idolproject.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(EventFilter.ALL)
    private val statusMessage = MutableStateFlow("이벤트를 불러오는 중이에요")

    val uiState: StateFlow<EventUiState> =
        combine(
            eventRepository.observeEvents(),
            selectedFilter,
            statusMessage
        ) { events, filter, message ->
            val filteredEvents = when (filter) {
                EventFilter.ALL -> events
                EventFilter.ACTIVE -> events.filter { it.status == EventStatus.ACTIVE }
                EventFilter.FINISHED -> events.filter { it.status == EventStatus.FINISHED }
            }

            if (filteredEvents.isEmpty()) {
                EventUiState.Empty()
            } else {
                EventUiState.Success(
                    events = filteredEvents,
                    message = message
                )
            }
        }
            .catch { throwable ->
                emit(
                    EventUiState.Error(
                        throwable.message ?: "이벤트 목록을 불러오지 못했습니다."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EventUiState.Loading
            )

    init {
        refreshEvents()
    }

    fun selectFilter(filter: EventFilter) {
        selectedFilter.value = filter
    }

    fun refreshEvents() {
        viewModelScope.launch {
            statusMessage.value = "이벤트를 불러오는 중이에요"

            val result = eventRepository.refreshEvents()

            statusMessage.value = when (result) {
                EventRefreshResult.Success -> "최신 이벤트를 확인했어요"
                EventRefreshResult.Fallback -> "네트워크 연결이 불안정해 샘플/캐시 이벤트를 표시 중이에요"
            }
        }
    }
}