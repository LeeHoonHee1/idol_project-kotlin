package com.example.idolproject.Login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.AuthRepository
import com.example.idolproject.data.repository.AuthResult
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

    fun goRegister() {
        viewModelScope.launch {
            _event.emit(AuthEvent.GoRegister)
        }
    }

    fun login() {
        val current = _uiState.value
        val email = current.email.trim()
        val password = current.password.trim()

        if (email.isBlank() || password.isBlank()) {
            emitToast("이메일/비밀번호를 입력해 주세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(
                email = email,
                password = password,
                isLoading = true,
                isLoginButtonEnabled = false,
                errorMessage = null
            )

            when (val result = authRepository.login(email, password)) {
                AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginButtonEnabled = true,
                        errorMessage = null
                    )

                    _event.emit(AuthEvent.GoMain)
                }

                is AuthResult.Failed -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginButtonEnabled = true,
                        errorMessage = result.message
                    )

                    _event.emit(AuthEvent.ShowToast("로그인 실패: ${result.message}"))
                }
            }
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch {
            _event.emit(AuthEvent.ShowToast(message))
        }
    }
}