package com.example.idolproject.Login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoginButtonEnabled: Boolean = true,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val checkedNicknameKey: String? = null,
    val isNicknameAvailable: Boolean = false,
    val nicknameCheckState: NicknameCheckState = NicknameCheckState.IDLE,
    val isLoading: Boolean = false,
    val isRegisterButtonEnabled: Boolean = false,
    val errorMessage: String? = null
)

enum class NicknameCheckState {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN
}

sealed interface AuthEvent {
    data class ShowToast(
        val message: String
    ) : AuthEvent

    data object GoMain : AuthEvent

    data object GoRegister : AuthEvent

    data object FinishRegister : AuthEvent
}