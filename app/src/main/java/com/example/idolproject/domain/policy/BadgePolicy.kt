package com.example.idolproject.domain.policy

object BadgePolicy {

    fun getBadgeIdByLevel(level: Int): String {
        return when (level) {
            in 1..4 -> "bronze"
            in 5..9 -> "silver"
            in 10..14 -> "gold"
            in 15..19 -> "platinum"
            in 20..29 -> "master"
            in 30..39 -> "grandmaster"
            else -> "challenger"
        }
    }

    fun resolveBadgeId(
        level: Int,
        badgeIdFromDb: String?
    ): String {
        val badgeId = badgeIdFromDb.orEmpty()

        return if (badgeId.isBlank() || badgeId == "default") {
            getBadgeIdByLevel(level)
        } else {
            badgeId
        }
    }
}