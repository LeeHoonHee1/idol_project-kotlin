package com.example.idolproject.UI.Ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.RankingRepository
import com.example.idolproject.domain.policy.RankingPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository
) : ViewModel() {

    val userRankingUiState: StateFlow<UserRankingUiState> =
        rankingRepository.observeUserRanking()
            .map { users ->
                val currentUserId = rankingRepository.getCurrentUserId()
                val rankingResult = RankingPolicy.buildUserRanking(
                    users = users,
                    currentUserId = currentUserId
                )

                val state: UserRankingUiState = UserRankingUiState.Success(
                    top3 = rankingResult.top3,
                    others = rankingResult.others,
                    myRank = rankingResult.myRank,
                    myUser = rankingResult.myUser
                )

                state
            }
            .catch { throwable ->
                emit(
                    UserRankingUiState.Error(
                        message = throwable.message ?: "유저 랭킹을 불러오지 못했습니다."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = UserRankingUiState.Loading
            )

    val groupRankingUiState: StateFlow<GroupRankingUiState> =
        rankingRepository.observeGroupRanking()
            .map { groups ->
                val state: GroupRankingUiState = GroupRankingUiState.Success(
                    top3 = groups.take(3),
                    others = groups.drop(3)
                )

                state
            }
            .catch { throwable ->
                emit(
                    GroupRankingUiState.Error(
                        message = throwable.message ?: "그룹 랭킹을 불러오지 못했습니다."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GroupRankingUiState.Loading
            )
}