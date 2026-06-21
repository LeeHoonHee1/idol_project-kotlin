package com.example.idolproject.UI.Ranking

sealed interface UserRankingUiState {
    data object Loading : UserRankingUiState

    data class Success(
        val top3: List<UserRank>,
        val others: List<UserRank>,
        val myRank: Int?,
        val myUser: UserRank?
    ) : UserRankingUiState

    data class Error(
        val message: String
    ) : UserRankingUiState
}

sealed interface GroupRankingUiState {
    data object Loading : GroupRankingUiState

    data class Success(
        val top3: List<GroupRank>,
        val others: List<GroupRank>
    ) : GroupRankingUiState

    data class Error(
        val message: String
    ) : GroupRankingUiState
}