package com.example.idolproject.UI.Mission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionRewardManagerTest {

    @Test
    fun `reward exp is added without level up`() {
        val result = MissionRewardManager.applyExpReward(
            currentLevel = 1,
            currentExp = 20,
            rewardExp = 10
        )

        assertEquals(1, result.newLevel)
        assertEquals(30, result.newExp)
        assertEquals("bronze", result.newBadgeId)
        assertFalse(result.leveledUp)
    }

    @Test
    fun `level increases when exp reaches 100`() {
        val result = MissionRewardManager.applyExpReward(
            currentLevel = 1,
            currentExp = 90,
            rewardExp = 10
        )

        assertEquals(2, result.newLevel)
        assertEquals(0, result.newExp)
        assertEquals("bronze", result.newBadgeId)
        assertTrue(result.leveledUp)
    }

    @Test
    fun `remaining exp is carried over after level up`() {
        val result = MissionRewardManager.applyExpReward(
            currentLevel = 1,
            currentExp = 95,
            rewardExp = 10
        )

        assertEquals(2, result.newLevel)
        assertEquals(5, result.newExp)
        assertEquals("bronze", result.newBadgeId)
        assertTrue(result.leveledUp)
    }

    @Test
    fun `multiple levels can be gained from large reward exp`() {
        val result = MissionRewardManager.applyExpReward(
            currentLevel = 1,
            currentExp = 50,
            rewardExp = 250
        )

        assertEquals(4, result.newLevel)
        assertEquals(0, result.newExp)
        assertEquals("bronze", result.newBadgeId)
        assertTrue(result.leveledUp)
    }

    @Test
    fun `badge changes when level reaches silver range`() {
        val result = MissionRewardManager.applyExpReward(
            currentLevel = 4,
            currentExp = 90,
            rewardExp = 10
        )

        assertEquals(5, result.newLevel)
        assertEquals(0, result.newExp)
        assertEquals("silver", result.newBadgeId)
        assertTrue(result.leveledUp)
    }

    @Test
    fun `weekly reward exp can level up with remaining exp`() {
        val result = MissionRewardManager.applyExpReward(
            currentLevel = 9,
            currentExp = 80,
            rewardExp = MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP
        )

        assertEquals(10, result.newLevel)
        assertEquals(30, result.newExp)
        assertEquals("gold", result.newBadgeId)
        assertTrue(result.leveledUp)
    }

    @Test
    fun `daily reward exp constant is 10`() {
        assertEquals(10, MissionRewardManager.DAILY_ATTENDANCE_REWARD_EXP)
    }

    @Test
    fun `weekly reward exp constant is 50`() {
        assertEquals(50, MissionRewardManager.WEEKLY_ATTENDANCE_REWARD_EXP)
    }

    @Test
    fun `need exp per level constant is 100`() {
        assertEquals(100, MissionRewardManager.NEED_EXP_PER_LEVEL)
    }
}