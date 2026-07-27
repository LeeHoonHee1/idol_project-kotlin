package com.example.idolproject.data.local

data class UserSessionData(
    val uid: String = "",
    val nickname: String = "",
    val role: String = "user",
    val pointsTotal: Long = 0L
) {
    val isLoggedIn: Boolean
        get() = uid.isNotBlank()

    val isAdmin: Boolean
        get() = role == "admin"
}