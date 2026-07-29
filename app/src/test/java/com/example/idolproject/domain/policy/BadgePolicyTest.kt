package com.example.idolproject.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class BadgePolicyTest {

    @Test
    fun `level 1 to 4 returns bronze`() {
        assertEquals("bronze", BadgePolicy.getBadgeIdByLevel(1))
        assertEquals("bronze", BadgePolicy.getBadgeIdByLevel(4))
    }

    @Test
    fun `level 5 to 9 returns silver`() {
        assertEquals("silver", BadgePolicy.getBadgeIdByLevel(5))
        assertEquals("silver", BadgePolicy.getBadgeIdByLevel(9))
    }

    @Test
    fun `level 10 to 14 returns gold`() {
        assertEquals("gold", BadgePolicy.getBadgeIdByLevel(10))
        assertEquals("gold", BadgePolicy.getBadgeIdByLevel(14))
    }

    @Test
    fun `level 15 to 19 returns platinum`() {
        assertEquals("platinum", BadgePolicy.getBadgeIdByLevel(15))
        assertEquals("platinum", BadgePolicy.getBadgeIdByLevel(19))
    }

    @Test
    fun `level 20 to 29 returns master`() {
        assertEquals("master", BadgePolicy.getBadgeIdByLevel(20))
        assertEquals("master", BadgePolicy.getBadgeIdByLevel(29))
    }

    @Test
    fun `level 30 to 39 returns grandmaster`() {
        assertEquals("grandmaster", BadgePolicy.getBadgeIdByLevel(30))
        assertEquals("grandmaster", BadgePolicy.getBadgeIdByLevel(39))
    }

    @Test
    fun `level 40 or higher returns challenger`() {
        assertEquals("challenger", BadgePolicy.getBadgeIdByLevel(40))
        assertEquals("challenger", BadgePolicy.getBadgeIdByLevel(100))
    }

    @Test
    fun `blank badge id resolves by level`() {
        val result = BadgePolicy.resolveBadgeId(
            level = 10,
            badgeIdFromDb = ""
        )

        assertEquals("gold", result)
    }

    @Test
    fun `default badge id resolves by level`() {
        val result = BadgePolicy.resolveBadgeId(
            level = 20,
            badgeIdFromDb = "default"
        )

        assertEquals("master", result)
    }

    @Test
    fun `existing badge id is preserved`() {
        val result = BadgePolicy.resolveBadgeId(
            level = 1,
            badgeIdFromDb = "gold"
        )

        assertEquals("gold", result)
    }
}