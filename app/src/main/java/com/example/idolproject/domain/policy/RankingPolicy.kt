package com.example.idolproject.domain.policy

import com.example.idolproject.UI.Ranking.UserRank

object RankingPolicy {

    data class UserRankingResult(
        val top3: List<UserRank>,
        val others: List<UserRank>,
        val myRank: Int?,
        val myUser: UserRank?
    )

    fun buildUserRanking(
        users: List<UserRank>,
        currentUserId: String?
    ): UserRankingResult {
        val top3 = users.take(3)
        val others = users.drop(3)

        val myIndex = users.indexOfFirst { it.uid == currentUserId }
        val myRank = if (myIndex >= 0) myIndex + 1 else null
        val myUser = if (myIndex >= 0) users[myIndex] else null

        return UserRankingResult(
            top3 = top3,
            others = others,
            myRank = myRank,
            myUser = myUser
        )
    }
}