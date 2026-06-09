package com.example.idolproject.UI.Friend

object NicknameKeyUtil {
    fun normalizeNicknameKey(input: String): String {
        return input.trim().replace("\\s+".toRegex(), "").lowercase()
    }
}