package com.example.idolproject.domain.policy

import com.example.idolproject.UI.Ranking.UserRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingPolicyTest {

    @Test
    fun `top3 and others are split correctly`() {
        val users = listOf(
            user("u1"),
            user("u2"),
            user("u3"),
            user("u4"),
            user("u5")
        )

        val result = RankingPolicy.buildUserRanking(
            users = users,
            currentUserId = "u4"
        )

        assertEquals(listOf("u1", "u2", "u3"), result.top3.map { it.uid })
        assertEquals(listOf("u4", "u5"), result.others.map { it.uid })
    }

    @Test
    fun `my rank is calculated from index plus one`() {
        val users = listOf(
            user("u1"),
            user("u2"),
            user("u3"),
            user("u4")
        )

        val result = RankingPolicy.buildUserRanking(
            users = users,
            currentUserId = "u3"
        )

        assertEquals(3, result.myRank)
        assertEquals("u3", result.myUser?.uid)
    }

    @Test
    fun `my rank is null when current user does not exist`() {
        val users = listOf(
            user("u1"),
            user("u2"),
            user("u3")
        )

        val result = RankingPolicy.buildUserRanking(
            users = users,
            currentUserId = "unknown"
        )

        assertNull(result.myRank)
        assertNull(result.myUser)
    }

    @Test
    fun `my rank is null when current user id is null`() {
        val users = listOf(
            user("u1"),
            user("u2"),
            user("u3")
        )

        val result = RankingPolicy.buildUserRanking(
            users = users,
            currentUserId = null
        )

        assertNull(result.myRank)
        assertNull(result.myUser)
    }

    @Test
    fun `others is empty when users are three or fewer`() {
        val users = listOf(
            user("u1"),
            user("u2"),
            user("u3")
        )

        val result = RankingPolicy.buildUserRanking(
            users = users,
            currentUserId = "u2"
        )

        assertEquals(3, result.top3.size)
        assertEquals(0, result.others.size)
    }

    @Test
    fun `empty users returns empty ranking result`() {
        val result = RankingPolicy.buildUserRanking(
            users = emptyList(),
            currentUserId = "u1"
        )

        assertEquals(0, result.top3.size)
        assertEquals(0, result.others.size)
        assertNull(result.myRank)
        assertNull(result.myUser)
    }

    private fun user(
        uid: String,
        level: Long = 1L,
        exp: Long = 0L
    ): UserRank {
        return UserRank(
            uid = uid,
            nickname = uid,
            level = level,
            exp = exp
        )
    }
}