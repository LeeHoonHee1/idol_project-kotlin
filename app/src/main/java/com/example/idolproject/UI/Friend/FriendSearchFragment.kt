package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.idolproject.R
import com.example.idolproject.data.repository.FriendSearchProfile
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendSearchFragment : Fragment(R.layout.fragment_friend_search) {

    private val viewModel: FriendViewModel by viewModels()

    private lateinit var etNickname: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var tvResult: TextView
    private lateinit var btnRequest: MaterialButton

    private lateinit var layoutSearchResultProfile: View
    private lateinit var ivSearchProfile: ImageView
    private lateinit var tvSearchResultNickname: TextView
    private lateinit var tvSearchResultStatus: TextView
    private lateinit var ivSearchBadge: ImageView
    private lateinit var tvSearchLevel: TextView
    private lateinit var tvSearchFavorite: TextView
    private lateinit var tvSearchSameFavorite: TextView

    private var foundUid: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        resetSearchResult()
        observeFriendSearchUiState()
        observeFriendEvent()

        btnSearch.setOnClickListener {
            val input = etNickname.text.toString().trim()
            viewModel.searchUserByNickname(input)
        }

        btnRequest.setOnClickListener {
            val uid = foundUid ?: return@setOnClickListener
            viewModel.sendFriendRequest(uid)
        }
    }

    private fun bindViews(view: View) {
        etNickname = view.findViewById(R.id.et_nickname)
        btnSearch = view.findViewById(R.id.btn_search)
        tvResult = view.findViewById(R.id.tv_result)
        btnRequest = view.findViewById(R.id.btn_request)

        layoutSearchResultProfile = view.findViewById(R.id.layout_search_result_profile)
        ivSearchProfile = view.findViewById(R.id.iv_search_profile)
        tvSearchResultNickname = view.findViewById(R.id.tv_search_result_nickname)
        tvSearchResultStatus = view.findViewById(R.id.tv_search_result_status)
        ivSearchBadge = view.findViewById(R.id.iv_search_badge)
        tvSearchLevel = view.findViewById(R.id.tv_search_level)
        tvSearchFavorite = view.findViewById(R.id.tv_search_favorite)
        tvSearchSameFavorite = view.findViewById(R.id.tv_search_same_favorite)
    }

    private fun observeFriendSearchUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendSearchUiState.collect { uiState ->
                    bindFriendSearchUi(uiState)
                }
            }
        }
    }

    private fun observeFriendEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is FriendEvent.ShowToast -> {
                            toast(event.message)
                        }

                        is FriendEvent.OpenFriendProfile -> {
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_container,
                                    FriendProfileFragment.newInstance(event.friendUid)
                                )
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
            }
        }
    }

    private fun bindFriendSearchUi(uiState: FriendSearchUiState) {
        if (uiState.isLoading) {
            foundUid = null
            tvResult.text = "검색 중..."
            btnRequest.isEnabled = false
            btnRequest.text = "친구 요청"
            layoutSearchResultProfile.visibility = View.GONE
            return
        }

        val profile = uiState.result

        if (profile == null) {
            foundUid = null
            tvResult.text = uiState.message ?: "검색 결과가 여기에 표시됩니다"
            btnRequest.isEnabled = false
            btnRequest.text = "친구 요청"
            layoutSearchResultProfile.visibility = View.GONE
            tvSearchSameFavorite.visibility = View.GONE
            return
        }

        foundUid = profile.uid
        renderSearchResult(profile)
    }

    private fun renderSearchResult(profile: FriendSearchProfile) {
        layoutSearchResultProfile.visibility = View.VISIBLE

        tvSearchResultNickname.text = profile.nickname
        tvSearchResultStatus.text = profile.statusMessage.ifBlank { "상태메시지 없음" }
        tvSearchLevel.text = "Lv.${profile.level}"

        tvSearchFavorite.text = if (profile.favoriteGroupName == "-") {
            "최애: -"
        } else {
            "최애: ${profile.favoriteGroupName}"
        }

        ivSearchBadge.setImageResource(mapBadgeRes(profile.badgeId))

        if (profile.photoUrl.isNullOrBlank()) {
            ivSearchProfile.setImageResource(R.drawable.person_24dp)
        } else {
            ivSearchProfile.load(profile.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }

        tvSearchSameFavorite.visibility = if (profile.isSameFavorite) {
            View.VISIBLE
        } else {
            View.GONE
        }

        when {
            profile.isMe -> {
                tvResult.text = "본인 계정이에요"
                btnRequest.isEnabled = false
                btnRequest.text = "나"
            }

            profile.isAlreadyFriend -> {
                tvResult.text = "이미 친구인 사용자예요"
                btnRequest.isEnabled = false
                btnRequest.text = "이미 친구"
            }

            profile.hasPendingRequest -> {
                tvResult.text = "이미 친구 요청을 보냈어요"
                btnRequest.isEnabled = false
                btnRequest.text = "요청됨"
            }

            profile.hasReceivedPendingRequest -> {
                tvResult.text = "이 사용자가 이미 나에게 친구 요청을 보냈어요"
                btnRequest.isEnabled = false
                btnRequest.text = "받은 요청 확인"
            }

            else -> {
                tvResult.text = "검색됨: ${profile.nickname}"
                btnRequest.isEnabled = true
                btnRequest.text = "친구 요청"
            }
        }
    }

    private fun mapBadgeRes(badgeId: String): Int {
        return when (badgeId) {
            "bronze" -> R.drawable.ic_badge_bronze
            "silver" -> R.drawable.ic_badge_silver
            "gold" -> R.drawable.ic_badge_gold
            "platinum" -> R.drawable.ic_badge_platinum
            "master" -> R.drawable.ic_badge_master
            "grandmaster" -> R.drawable.ic_badge_grandmaster
            "challenger" -> R.drawable.ic_badge_challenger
            else -> R.drawable.ic_badge_bronze
        }
    }

    private fun resetSearchResult() {
        foundUid = null
        tvResult.text = "검색 결과가 여기에 표시됩니다"
        btnRequest.isEnabled = false
        btnRequest.text = "친구 요청"
        layoutSearchResultProfile.visibility = View.GONE
        tvSearchSameFavorite.visibility = View.GONE
        ivSearchProfile.setImageResource(R.drawable.person_24dp)
        ivSearchBadge.setImageResource(R.drawable.ic_badge_bronze)
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}