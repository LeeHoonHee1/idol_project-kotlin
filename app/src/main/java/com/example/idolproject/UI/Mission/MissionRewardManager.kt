package com.example.idolproject.UI.Mission

import com.example.idolproject.domain.policy.BadgePolicy

object MissionRewardManager {

    const val NEED_EXP_PER_LEVEL = 100
    const val DAILY_ATTENDANCE_REWARD_EXP = 10
    const val WEEKLY_ATTENDANCE_REWARD_EXP = 50

    data class RewardResult(
        val newLevel: Int,
        val newExp: Int,
        val newBadgeId: String,
        val leveledUp: Boolean
    )

    fun applyExpReward(
        currentLevel: Int,
        currentExp: Int,
        rewardExp: Int
    ): RewardResult {
        var level = currentLevel
        var exp = currentExp + rewardExp
        var leveledUp = false

        while (exp >= NEED_EXP_PER_LEVEL) {
            exp -= NEED_EXP_PER_LEVEL
            level += 1
            leveledUp = true
        }

        val badgeId = BadgePolicy.getBadgeIdByLevel(level)

        return RewardResult(
            newLevel = level,
            newExp = exp,
            newBadgeId = badgeId,
            leveledUp = leveledUp
        )
    }
}