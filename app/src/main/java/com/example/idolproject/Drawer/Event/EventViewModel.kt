package com.example.idolproject.Drawer.Event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val uiState: StateFlow<EventUiState> =
        combine(
            eventRepository.observeEvents(),
            selectedFilter
        ) { events, filter ->
            val filteredEvents = when (filter) {
                EventFilter.ALL -> events
                EventFilter.ACTIVE -> events.filter { it.status == EventStatus.ACTIVE }
                EventFilter.FINISHED -> events.filter { it.status == EventStatus.FINISHED }
            }

            if (filteredEvents.isEmpty()) {
                EventUiState.Empty()
            } else {
                EventUiState.Success(filteredEvents)
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
        seedSampleEvents()
    }

    fun selectFilter(filter: EventFilter) {
        selectedFilter.value = filter
    }

    private fun seedSampleEvents() {
        viewModelScope.launch {
            eventRepository.seedSampleEventsIfNeeded()
        }
    }
}