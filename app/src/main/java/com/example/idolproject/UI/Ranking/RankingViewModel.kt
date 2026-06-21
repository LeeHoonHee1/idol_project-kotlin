package com.example.idolproject.UI.Ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.idolproject.data.repository.RankingRepository
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
                val top3 = users.take(3)
                val others = users.drop(3)

                val currentUserId = rankingRepository.getCurrentUserId()
                val myIndex = users.indexOfFirst { it.uid == currentUserId }
                val myRank = if (myIndex >= 0) myIndex + 1 else null
                val myUser = if (myIndex >= 0) users[myIndex] else null

                val state: UserRankingUiState = UserRankingUiState.Success(
                    top3 = top3,
                    others = others,
                    myRank = myRank,
                    myUser = myUser
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