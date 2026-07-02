package com.example.idolproject.Drawer.Group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.GroupScheduleRepository
import com.prolificinteractive.materialcalendarview.CalendarDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupScheduleViewModel @Inject constructor(
    private val groupScheduleRepository: GroupScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupScheduleUiState())
    val uiState: StateFlow<GroupScheduleUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<GroupScheduleEvent>()
    val event: SharedFlow<GroupScheduleEvent> = _event.asSharedFlow()

    private var scheduleJob: Job? = null

    fun start() {
        loadUserScheduleContext()
        observeGroupSchedules()
    }

    private fun loadUserScheduleContext() {
        viewModelScope.launch {
            runCatching {
                val favoriteGroupIds = groupScheduleRepository.getMyFavoriteGroupIds()
                val isAdmin = groupScheduleRepository.isCurrentUserAdmin()
                favoriteGroupIds to isAdmin
            }.onSuccess { (favoriteGroupIds, isAdmin) ->
                val current = _uiState.value

                _uiState.value = buildUiState(
                    schedules = current.schedules,
                    favoriteGroupIds = favoriteGroupIds,
                    selectedDate = current.selectedDate,
                    selectedGroupId = current.selectedGroupId,
                    isAdmin = isAdmin,
                    isLoading = current.isLoading,
                    errorMessage = null
                )
            }.onFailure { throwable ->
                val current = _uiState.value

                _uiState.value = buildUiState(
                    schedules = current.schedules,
                    favoriteGroupIds = emptyList(),
                    selectedDate = current.selectedDate,
                    selectedGroupId = null,
                    isAdmin = false,
                    isLoading = current.isLoading,
                    errorMessage = throwable.message ?: "사용자 정보를 불러오지 못했습니다."
                )
            }
        }
    }

    private fun observeGroupSchedules() {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            runCatching {
                groupScheduleRepository.observeGroupSchedules().collect { schedules ->
                    val current = _uiState.value

                    _uiState.value = buildUiState(
                        schedules = schedules,
                        favoriteGroupIds = current.favoriteGroupIds,
                        selectedDate = current.selectedDate,
                        selectedGroupId = current.selectedGroupId,
                        isAdmin = current.isAdmin,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                val current = _uiState.value

                _uiState.value = current.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "그룹 일정을 불러오지 못했습니다."
                )

                emitToast(throwable.message ?: "그룹 일정을 불러오지 못했습니다.")
            }
        }
    }

    fun selectDate(date: CalendarDay) {
        val current = _uiState.value

        _uiState.value = buildUiState(
            schedules = current.schedules,
            favoriteGroupIds = current.favoriteGroupIds,
            selectedDate = date,
            selectedGroupId = current.selectedGroupId,
            isAdmin = current.isAdmin,
            isLoading = current.isLoading,
            errorMessage = null
        )
    }

    fun selectGroup(groupId: String?) {
        val current = _uiState.value

        _uiState.value = buildUiState(
            schedules = current.schedules,
            favoriteGroupIds = current.favoriteGroupIds,
            selectedDate = current.selectedDate,
            selectedGroupId = groupId,
            isAdmin = current.isAdmin,
            isLoading = current.isLoading,
            errorMessage = null
        )
    }

    fun addGroupSchedule(
        groupId: String,
        groupName: String,
        date: CalendarDay,
        type: GroupActivityType,
        title: String,
        memo: String
    ) {
        val trimmedTitle = title.trim()
        val trimmedMemo = memo.trim()

        if (trimmedTitle.isBlank()) {
            emitToast("제목을 입력하세요.")
            return
        }

        viewModelScope.launch {
            runCatching {
                groupScheduleRepository.addGroupSchedule(
                    groupId = groupId,
                    groupName = groupName,
                    date = date,
                    type = type,
                    title = trimmedTitle,
                    memo = trimmedMemo
                )
            }.onSuccess { item ->
                selectDate(item.date)
                _event.emit(GroupScheduleEvent.ScheduleAdded(item))
                _event.emit(GroupScheduleEvent.ShowToast("일정이 추가되었습니다."))
            }.onFailure { throwable ->
                _event.emit(
                    GroupScheduleEvent.ShowToast(
                        throwable.message ?: "일정 추가에 실패했습니다."
                    )
                )
            }
        }
    }

    fun updateGroupSchedule(item: GroupScheduleItem) {
        if (item.id.isBlank()) {
            emitToast("수정할 일정 ID가 없습니다.")
            return
        }

        if (item.title.isBlank()) {
            emitToast("제목을 입력하세요.")
            return
        }

        viewModelScope.launch {
            runCatching {
                groupScheduleRepository.updateGroupSchedule(item)
            }.onSuccess { updatedItem ->
                selectDate(updatedItem.date)
                _event.emit(GroupScheduleEvent.ScheduleUpdated(updatedItem))
                _event.emit(GroupScheduleEvent.ShowToast("일정이 수정되었습니다."))
            }.onFailure { throwable ->
                _event.emit(
                    GroupScheduleEvent.ShowToast(
                        throwable.message ?: "일정 수정에 실패했습니다."
                    )
                )
            }
        }
    }

    fun deleteGroupSchedule(item: GroupScheduleItem) {
        if (item.id.isBlank()) {
            emitToast("삭제할 일정 ID가 없습니다.")
            return
        }

        viewModelScope.launch {
            runCatching {
                groupScheduleRepository.deleteGroupSchedule(item.id)
            }.onSuccess {
                _event.emit(GroupScheduleEvent.ScheduleDeleted)
                _event.emit(GroupScheduleEvent.ShowToast("일정이 삭제되었습니다."))
            }.onFailure { throwable ->
                _event.emit(
                    GroupScheduleEvent.ShowToast(
                        throwable.message ?: "일정 삭제에 실패했습니다."
                    )
                )
            }
        }
    }

    private fun buildUiState(
        schedules: List<GroupScheduleItem>,
        favoriteGroupIds: List<String>,
        selectedDate: CalendarDay,
        selectedGroupId: String?,
        isAdmin: Boolean,
        isLoading: Boolean,
        errorMessage: String?
    ): GroupScheduleUiState {
        val selectedDateItems = schedules.filter { item ->
            item.date == selectedDate &&
                    (selectedGroupId == null || item.groupId == selectedGroupId)
        }

        val dateText = formatDisplayDate(selectedDate)
        val groupLabel = getDisplayGroupName(selectedGroupId)

        val selectedDateInfoText = if (selectedDateItems.isEmpty()) {
            "$groupLabel · $dateText · 등록된 일정이 없어요"
        } else {
            "$groupLabel · $dateText · 일정 ${selectedDateItems.size}건"
        }

        return GroupScheduleUiState(
            isLoading = isLoading,
            schedules = schedules,
            favoriteGroupIds = favoriteGroupIds,
            selectedDate = selectedDate,
            selectedGroupId = selectedGroupId,
            isAdmin = isAdmin,
            selectedDateItems = selectedDateItems,
            selectedDateInfoText = selectedDateInfoText,
            errorMessage = errorMessage
        )
    }

    private fun getDisplayGroupName(groupId: String?): String {
        return when (groupId) {
            null -> "전체"
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "lesserafim" -> "LE SSERAFIM"
            "aespa" -> "aespa"
            "babymonster" -> "BABYMONSTER"
            else -> groupId
        }
    }

    private fun formatDisplayDate(date: CalendarDay): String {
        return String.format("%04d.%02d.%02d", date.year, date.month, date.day)
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _event.emit(GroupScheduleEvent.ShowToast(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        scheduleJob?.cancel()
    }
}