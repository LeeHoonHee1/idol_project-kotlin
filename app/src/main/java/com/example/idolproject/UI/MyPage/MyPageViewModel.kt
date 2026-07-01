package com.example.idolproject.UI.MyPage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.R
import com.example.idolproject.data.repository.FavoriteGroupSaveResult
import com.example.idolproject.data.repository.MyPageRepository
import com.example.idolproject.data.repository.NicknameChangeResult
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
class MyPageViewModel @Inject constructor(
    private val myPageRepository: MyPageRepository
) : ViewModel() {

    companion object {
        private const val NEED_EXP_PER_LEVEL = 100
    }

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MyPageEvent>()
    val event: SharedFlow<MyPageEvent> = _event.asSharedFlow()

    private var profileJob: Job? = null

    fun startObserveMyPage() {
        val uid = myPageRepository.getCurrentUserId()

        if (uid == null) {
            _uiState.value = MyPageUiState(
                isLoading = false,
                errorMessage = "로그인 정보를 확인해주세요."
            )
            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            myPageRepository.observeMyProfile(uid)
                .collect { profile ->
                    val correctBadgeId = getBadgeIdByLevel(profile.level)

                    myPageRepository.syncBadgeIdIfNeeded(
                        uid = uid,
                        currentBadgeId = profile.badgeId,
                        correctBadgeId = correctBadgeId
                    )

                    val expProgress = profile.exp.coerceIn(0, NEED_EXP_PER_LEVEL)
                    val remainExp = (NEED_EXP_PER_LEVEL - expProgress).coerceAtLeast(0)

                    _uiState.value = MyPageUiState(
                        isLoading = false,
                        nickname = profile.nickname,
                        statusMessage = profile.statusMessage,
                        level = profile.level,
                        exp = profile.exp,
                        expProgress = expProgress,
                        nextExpText = "다음 레벨까지 ${remainExp} EXP",
                        badgeId = correctBadgeId,
                        badgeResId = getBadgeImageRes(correctBadgeId),
                        favoriteGroupId = profile.favoriteGroupId,
                        favoriteGroupName = getFavoriteGroupName(profile.favoriteGroupId),
                        profileImageUrl = "",
                        errorMessage = null
                    )
                }
        }
    }

    fun changeNickname(newNickname: String) {
        val uid = myPageRepository.getCurrentUserId()

        if (uid == null) {
            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        val trimmedNickname = newNickname.trim()
        val nicknameKey = normalizeNicknameKey(trimmedNickname)

        if (trimmedNickname.length !in 2..20) {
            emitNicknameInputError("닉네임은 2~20자로 입력해주세요.")
            return
        }

        if (nicknameKey.isBlank()) {
            emitNicknameInputError("닉네임을 다시 확인해주세요.")
            return
        }

        viewModelScope.launch {
            runCatching {
                myPageRepository.changeNickname(
                    uid = uid,
                    newNickname = trimmedNickname,
                    newKey = nicknameKey
                )
            }.onSuccess {
                when (it) {
                    NicknameChangeResult.SAME -> {
                        _event.emit(MyPageEvent.ShowToast("기존 닉네임과 같습니다."))
                    }

                    NicknameChangeResult.CHANGED -> {
                        _event.emit(MyPageEvent.ShowToast("닉네임이 변경되었습니다."))
                    }
                }
            }.onFailure { throwable ->
                val message = if (throwable.message == "TAKEN") {
                    "이미 사용 중인 닉네임입니다."
                } else {
                    throwable.message ?: "닉네임 변경에 실패했습니다."
                }

                _event.emit(MyPageEvent.ShowToast(message))
            }
        }
    }

    fun saveFavoriteGroup(groupId: String) {
        val uid = myPageRepository.getCurrentUserId()

        if (uid == null) {
            emitToast("로그인 정보를 확인해주세요.")
            return
        }

        if (groupId.isBlank()) {
            emitToast("최애 그룹을 선택해주세요.")
            return
        }

        viewModelScope.launch {
            runCatching {
                myPageRepository.saveFavoriteGroup(
                    uid = uid,
                    newGroupId = groupId
                )
            }.onSuccess {
                when (it) {
                    FavoriteGroupSaveResult.SAME -> {
                        _event.emit(MyPageEvent.ShowToast("이미 선택된 최애 그룹입니다."))
                    }

                    FavoriteGroupSaveResult.CHANGED -> {
                        _event.emit(MyPageEvent.ShowToast("최애 그룹이 변경되었습니다."))
                    }
                }
            }.onFailure { throwable ->
                _event.emit(
                    MyPageEvent.ShowToast(
                        throwable.message ?: "최애 그룹 저장에 실패했습니다."
                    )
                )
            }
        }
    }

    private fun normalizeNicknameKey(nickname: String): String {
        return nickname.lowercase().replace("\\s".toRegex(), "")
    }

    private fun getBadgeIdByLevel(level: Int): String {
        return when (level) {
            in 1..4 -> "bronze"
            in 5..9 -> "silver"
            in 10..14 -> "gold"
            in 15..19 -> "platinum"
            in 20..29 -> "master"
            in 30..39 -> "grandmaster"
            else -> "challenger"
        }
    }

    private fun getBadgeImageRes(badgeId: String): Int {
        return when (badgeId) {
            "bronze" -> R.drawable.ic_badge_bronze
            "silver" -> R.drawable.ic_badge_silver
            "gold" -> R.drawable.ic_badge_gold
            "platinum" -> R.drawable.ic_badge_platinum
            "master" -> R.drawable.ic_badge_master
            "grandmaster" -> R.drawable.ic_badge_grandmaster
            "challenger" -> R.drawable.ic_badge_challenger
            else -> R.drawable.ic_badge_bronze
        }
    }

    private fun getFavoriteGroupName(groupId: String): String {
        return when (groupId) {
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "aespa" -> "aespa"
            "lesserafim" -> "LE SSERAFIM"
            "gidle" -> "(G)I-DLE"
            else -> "최애 그룹을 선택해 주세요"
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _event.emit(MyPageEvent.ShowToast(message))
        }
    }

    private fun emitNicknameInputError(message: String) {
        viewModelScope.launch {
            _event.emit(MyPageEvent.ShowNicknameInputError(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileJob?.cancel()
    }
}