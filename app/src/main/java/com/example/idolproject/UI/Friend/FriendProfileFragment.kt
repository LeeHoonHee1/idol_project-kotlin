package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.idolproject.R
import com.example.idolproject.data.repository.FriendProfile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FriendProfileFragment : Fragment(R.layout.fragment_friend_profile) {

    companion object {
        private const val ARG_UID = "friendUid"

        fun newInstance(friendUid: String): FriendProfileFragment {
            return FriendProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_UID, friendUid)
                }
            }
        }
    }

    private val viewModel: FriendViewModel by viewModels()

    private lateinit var ivProfile: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvFavorite: TextView
    private lateinit var ivBadge: ImageView
    private lateinit var tvSameFavorite: TextView
    private lateinit var tvBadgeHint: TextView

    private val groupDisplayMap = mapOf(
        "ive" to "IVE",
        "aespa" to "aespa",
        "newjeans" to "NewJeans",
        "lesserafim" to "LE SSERAFIM",
        "babymonster" to "BABYMONSTER"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        observeFriendProfileUiState()

        val friendUid = arguments?.getString(ARG_UID).orEmpty()
        viewModel.startObserveFriendProfile(friendUid)
    }

    private fun bindViews(view: View) {
        ivProfile = view.findViewById(R.id.iv_profile)
        tvNickname = view.findViewById(R.id.tv_nickname)
        tvStatus = view.findViewById(R.id.tv_status)
        tvLevel = view.findViewById(R.id.tv_level)
        tvFavorite = view.findViewById(R.id.tv_favorite)
        ivBadge = view.findViewById(R.id.iv_badge)
        tvSameFavorite = view.findViewById(R.id.tv_same_favorite)
        tvBadgeHint = view.findViewById(R.id.tv_badge_hint)
    }

    private fun observeFriendProfileUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friendProfileUiState.collect { uiState ->
                    bindFriendProfileUi(uiState)
                }
            }
        }
    }

    private fun bindFriendProfileUi(uiState: FriendProfileUiState) {
        when {
            uiState.isLoading -> {
                setLoadingState()
            }

            uiState.errorMessage != null -> {
                setErrorState(uiState.errorMessage)
            }

            uiState.profile != null -> {
                bindFriendProfile(uiState.profile)
            }
        }
    }

    private fun setLoadingState() {
        tvNickname.text = "불러오는 중..."
        tvStatus.text = ""
        tvLevel.text = "Lv.-"
        tvFavorite.text = "최애: -"
        tvSameFavorite.visibility = View.GONE
        tvBadgeHint.text = "대표 뱃지 정보를 불러오는 중..."
        ivProfile.setImageResource(R.drawable.person_24dp)
        ivBadge.setImageResource(R.drawable.ic_badge_bronze)
    }

    private fun setErrorState(message: String) {
        tvNickname.text = "불러오기 실패"
        tvStatus.text = message
        tvLevel.text = "Lv.-"
        tvFavorite.text = "최애: -"
        tvSameFavorite.visibility = View.GONE
        tvBadgeHint.text = "대표 뱃지 정보를 불러오지 못했어요"
        ivProfile.setImageResource(R.drawable.person_24dp)
        ivBadge.setImageResource(R.drawable.ic_badge_bronze)
    }

    private fun bindFriendProfile(profile: FriendProfile) {
        val favoriteGroupName = if (profile.favoriteGroupId.isBlank()) {
            "-"
        } else {
            groupDisplayMap[profile.favoriteGroupId] ?: profile.favoriteGroupId
        }

        tvNickname.text = profile.nickname
        tvStatus.text = profile.statusMessage.ifBlank { "상태메시지 없음" }
        tvLevel.text = "Lv.${profile.level}"
        tvFavorite.text = if (favoriteGroupName == "-") {
            "최애: -"
        } else {
            "최애\n$favoriteGroupName"
        }

        tvSameFavorite.visibility = if (profile.isSameFavorite) {
            View.VISIBLE
        } else {
            View.GONE
        }

        ivBadge.setImageResource(mapBadgeRes(profile.badgeId))
        tvBadgeHint.text = makeBadgeHint(profile.badgeId, profile.level)

        if (profile.photoUrl.isNullOrBlank()) {
            ivProfile.setImageResource(R.drawable.person_24dp)
        } else {
            ivProfile.load(profile.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
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

    private fun makeBadgeHint(badgeId: String, level: Int): String {
        val badgeName = when (badgeId) {
            "bronze" -> "Bronze"
            "silver" -> "Silver"
            "gold" -> "Gold"
            "platinum" -> "Platinum"
            "master" -> "Master"
            "grandmaster" -> "Grand Master"
            "challenger" -> "Challenger"
            else -> "Bronze"
        }

        return "$badgeName 뱃지를 가진 Lv.$level 팬 친구예요"
    }
}