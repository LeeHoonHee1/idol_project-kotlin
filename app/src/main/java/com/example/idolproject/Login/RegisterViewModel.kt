package com.example.idolproject.Login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.AuthRepository
import com.example.idolproject.data.repository.NicknameCheckResult
import com.example.idolproject.data.repository.RegisterResult
import com.example.idolproject.domain.policy.NicknamePolicy
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AuthEvent>()
    val event: SharedFlow<AuthEvent> = _event.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            errorMessage = null
        )
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    fun onNicknameChanged(nickname: String) {
        _uiState.value = _uiState.value.copy(
            nickname = nickname,
            checkedNicknameKey = null,
            isNicknameAvailable = false,
            nicknameCheckState = NicknameCheckState.IDLE,
            isRegisterButtonEnabled = false,
            errorMessage = null
        )
    }

    fun checkNickname() {
        val nickname = _uiState.value.nickname.trim()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                nickname = nickname,
                nicknameCheckState = NicknameCheckState.CHECKING,
                isNicknameAvailable = false,
                isRegisterButtonEnabled = false,
                errorMessage = null
            )

            when (val result = authRepository.checkNicknameAvailable(nickname)) {
                is NicknameCheckResult.Available -> {
                    _uiState.value = _uiState.value.copy(
                        checkedNicknameKey = result.key,
                        isNicknameAvailable = true,
                        nicknameCheckState = NicknameCheckState.AVAILABLE,
                        isRegisterButtonEnabled = true,
                        errorMessage = null
                    )

                    _event.emit(AuthEvent.ShowToast("사용 가능한 닉네임이야 ✅"))
                }

                NicknameCheckResult.Taken -> {
                    _uiState.value = _uiState.value.copy(
                        checkedNicknameKey = null,
                        isNicknameAvailable = false,
                        nicknameCheckState = NicknameCheckState.TAKEN,
                        isRegisterButtonEnabled = false,
                        errorMessage = "이미 사용 중인 닉네임이야"
                    )

                    _event.emit(AuthEvent.ShowToast("이미 사용 중인 닉네임이야"))
                }

                is NicknameCheckResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        checkedNicknameKey = null,
                        isNicknameAvailable = false,
                        nicknameCheckState = NicknameCheckState.IDLE,
                        isRegisterButtonEnabled = false,
                        errorMessage = result.message
                    )

                    _event.emit(AuthEvent.ShowToast(result.message))
                }

                is NicknameCheckResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        checkedNicknameKey = null,
                        isNicknameAvailable = false,
                        nicknameCheckState = NicknameCheckState.IDLE,
                        isRegisterButtonEnabled = false,
                        errorMessage = result.message
                    )

                    _event.emit(AuthEvent.ShowToast("중복확인 실패: ${result.message}"))
                }
            }
        }
    }

    fun register() {
        val current = _uiState.value
        val email = current.email.trim()
        val password = current.password.trim()
        val nickname = current.nickname.trim()

        viewModelScope.launch {
            _uiState.value = current.copy(
                email = email,
                password = password,
                nickname = nickname,
                isLoading = true,
                isRegisterButtonEnabled = false,
                errorMessage = null
            )

            when (
                val result = authRepository.register(
                    email = email,
                    password = password,
                    nickname = nickname,
                    checkedNicknameKey = current.checkedNicknameKey,
                    isNicknameAvailable = current.isNicknameAvailable
                )
            ) {
                RegisterResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegisterButtonEnabled = false,
                        errorMessage = null
                    )

                    _event.emit(AuthEvent.ShowToast("회원가입 완료"))
                    _event.emit(AuthEvent.FinishRegister)
                }

                is RegisterResult.Failed -> {
                    val canRetryRegister =
                        _uiState.value.isNicknameAvailable &&
                                _uiState.value.checkedNicknameKey ==
                                NicknamePolicy.normalizeNicknameKey(nickname)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegisterButtonEnabled = canRetryRegister,
                        errorMessage = result.message
                    )

                    _event.emit(AuthEvent.ShowToast(result.message))
                }
            }
        }
    }
}