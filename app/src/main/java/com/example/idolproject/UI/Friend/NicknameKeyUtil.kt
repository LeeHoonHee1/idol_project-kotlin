package com.example.idolproject.UI.Friend

import com.example.idolproject.domain.policy.NicknamePolicy

object NicknameKeyUtil {
    fun normalizeNicknameKey(input: String): String {
        return NicknamePolicy.normalizeNicknameKey(input)
    }
}