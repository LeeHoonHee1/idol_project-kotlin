package com.example.idolproject.Drawer.ComeBack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.ComebackScheduleRepository
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
class ComebackScheduleViewModel @Inject constructor(
    private val comebackScheduleRepository: ComebackScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComebackScheduleUiState())
    val uiState: StateFlow<ComebackScheduleUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ComebackScheduleEvent>()
    val event: SharedFlow<ComebackScheduleEvent> = _event.asSharedFlow()

    private var comebackJob: Job? = null

    fun start() {
        loadUserScheduleContext()
        observeComebacks()
    }

    private fun loadUserScheduleContext() {
        viewModelScope.launch {
            runCatching {
                val favoriteGroupIds = comebackScheduleRepository.getMyFavoriteGroupIds()
                val isAdmin = comebackScheduleRepository.isCurrentUserAdmin()
                favoriteGroupIds to isAdmin
            }.onSuccess { (favoriteGroupIds, isAdmin) ->
                val current = _uiState.value

                _uiState.value = buildUiState(
                    comebacks = current.comebacks,
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
                    comebacks = current.comebacks,
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

    private fun observeComebacks() {
        comebackJob?.cancel()
        comebackJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            runCatching {
                comebackScheduleRepository.observeComebacks().collect { comebacks ->
                    val current = _uiState.value

                    _uiState.value = buildUiState(
                        comebacks = comebacks,
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
                    errorMessage = throwable.message ?: "컴백 일정을 불러오지 못했습니다."
                )

                emitToast(throwable.message ?: "컴백 일정을 불러오지 못했습니다.")
            }
        }
    }

    fun selectDate(date: CalendarDay) {
        val current = _uiState.value

        _uiState.value = buildUiState(
            comebacks = current.comebacks,
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
            comebacks = current.comebacks,
            favoriteGroupIds = current.favoriteGroupIds,
            selectedDate = current.selectedDate,
            selectedGroupId = groupId,
            isAdmin = current.isAdmin,
            isLoading = current.isLoading,
            errorMessage = null
        )
    }

    fun addComeback(
        groupId: String,
        groupName: String,
        date: CalendarDay,
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
                comebackScheduleRepository.addComeback(
                    groupId = groupId,
                    groupName = groupName,
                    date = date,
                    title = trimmedTitle,
                    memo = trimmedMemo
                )
            }.onSuccess { item ->
                selectDate(item.date)
                _event.emit(ComebackScheduleEvent.ComebackAdded(item))
                _event.emit(ComebackScheduleEvent.ShowToast("컴백 일정이 추가되었습니다."))
            }.onFailure { throwable ->
                _event.emit(
                    ComebackScheduleEvent.ShowToast(
                        throwable.message ?: "컴백 일정 추가에 실패했습니다."
                    )
                )
            }
        }
    }

    fun updateComeback(item: ComebackItem) {
        if (item.id.isBlank()) {
            emitToast("수정할 컴백 일정 ID가 없습니다.")
            return
        }

        if (item.title.isBlank()) {
            emitToast("제목을 입력하세요.")
            return
        }

        viewModelScope.launch {
            runCatching {
                comebackScheduleRepository.updateComeback(item)
            }.onSuccess { updatedItem ->
                selectDate(updatedItem.date)
                _event.emit(ComebackScheduleEvent.ComebackUpdated(updatedItem))
                _event.emit(ComebackScheduleEvent.ShowToast("컴백 일정이 수정되었습니다."))
            }.onFailure { throwable ->
                _event.emit(
                    ComebackScheduleEvent.ShowToast(
                        throwable.message ?: "컴백 일정 수정에 실패했습니다."
                    )
                )
            }
        }
    }

    fun deleteComeback(item: ComebackItem) {
        if (item.id.isBlank()) {
            emitToast("삭제할 컴백 일정 ID가 없습니다.")
            return
        }

        viewModelScope.launch {
            runCatching {
                comebackScheduleRepository.deleteComeback(item.id)
            }.onSuccess {
                _event.emit(ComebackScheduleEvent.ComebackDeleted)
                _event.emit(ComebackScheduleEvent.ShowToast("컴백 일정이 삭제되었습니다."))
            }.onFailure { throwable ->
                _event.emit(
                    ComebackScheduleEvent.ShowToast(
                        throwable.message ?: "컴백 일정 삭제에 실패했습니다."
                    )
                )
            }
        }
    }

    private fun buildUiState(
        comebacks: List<ComebackItem>,
        favoriteGroupIds: List<String>,
        selectedDate: CalendarDay,
        selectedGroupId: String?,
        isAdmin: Boolean,
        isLoading: Boolean,
        errorMessage: String?
    ): ComebackScheduleUiState {
        val selectedDateItems = comebacks.filter { item ->
            item.date == selectedDate &&
                    (selectedGroupId == null || item.groupId == selectedGroupId)
        }

        val dateText = formatDisplayDate(selectedDate)
        val groupLabel = getDisplayGroupName(selectedGroupId)

        val selectedDateInfoText = when {
            selectedDateItems.isEmpty() -> {
                "$groupLabel · $dateText · 등록된 컴백 일정 없음"
            }

            selectedDateItems.size == 1 -> {
                "$groupLabel · $dateText · 컴백 1건"
            }

            else -> {
                "$groupLabel · $dateText · 컴백 ${selectedDateItems.size}건"
            }
        }

        return ComebackScheduleUiState(
            isLoading = isLoading,
            comebacks = comebacks,
            favoriteGroupIds = favoriteGroupIds,
            selectedDate = selectedDate,
            selectedGroupId = selectedGroupId,
            isAdmin = isAdmin,
            selectedDateItems = selectedDateItems,
            selectedDateInfoText = selectedDateInfoText,
            isSelectedDateEmpty = selectedDateItems.isEmpty(),
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
            _event.emit(ComebackScheduleEvent.ShowToast(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        comebackJob?.cancel()
    }
}