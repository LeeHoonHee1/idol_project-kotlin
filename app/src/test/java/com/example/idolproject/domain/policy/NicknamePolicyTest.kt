package com.example.idolproject.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknamePolicyTest {

    @Test
    fun `nickname key trims removes spaces and lowercases`() {
        val result = NicknamePolicy.normalizeNicknameKey("  Hoon Hee  ")

        assertEquals("hoonhee", result)
    }

    @Test
    fun `nickname with 2 characters is valid`() {
        val result = NicknamePolicy.isValidNickname("훈희")

        assertTrue(result)
    }

    @Test
    fun `nickname with 12 characters is valid`() {
        val result = NicknamePolicy.isValidNickname("abcdefghijkl")

        assertTrue(result)
    }

    @Test
    fun `nickname shorter than 2 characters is invalid`() {
        val result = NicknamePolicy.isValidNickname("a")

        assertFalse(result)
    }

    @Test
    fun `nickname longer than 12 characters is invalid`() {
        val result = NicknamePolicy.isValidNickname("abcdefghijklm")

        assertFalse(result)
    }

    @Test
    fun `nickname containing space is invalid`() {
        val result = NicknamePolicy.isValidNickname("hoon hee")

        assertFalse(result)
    }

    @Test
    fun `blank nickname is invalid`() {
        val result = NicknamePolicy.isValidNickname("   ")

        assertFalse(result)
    }
}