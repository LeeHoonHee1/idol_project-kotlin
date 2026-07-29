package com.example.idolproject.domain.policy

object NicknamePolicy {

    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 12

    fun normalizeNicknameKey(nickname: String): String {
        return nickname.trim()
            .lowercase()
            .replace("\\s+".toRegex(), "")
    }

    fun isValidNickname(nickname: String): Boolean {
        val trimmed = nickname.trim()

        if (trimmed.length !in MIN_LENGTH..MAX_LENGTH) return false
        if (trimmed.contains(" ")) return false

        return true
    }

    fun getInvalidMessage(): String {
        return "닉네임은 2~12자, 공백 없이 입력해줘"
    }
}