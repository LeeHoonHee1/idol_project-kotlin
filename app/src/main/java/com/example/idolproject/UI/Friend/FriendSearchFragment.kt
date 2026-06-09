package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.idolproject.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendSearchFragment : Fragment(R.layout.fragment_friend_search) {

    private val repo = FriendRequestRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
    private var foundNicknameInput: String = ""

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
        resetSearchResult()

        btnSearch.setOnClickListener {
            searchFriend()
        }

        btnRequest.setOnClickListener {
            sendFriendRequest()
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

    private fun searchFriend() {
        val input = etNickname.text.toString().trim()

        if (input.isBlank()) {
            toast("닉네임을 입력하세요")
            return
        }

        foundNicknameInput = input
        foundUid = null

        tvResult.text = "검색 중..."
        btnRequest.isEnabled = false
        btnRequest.text = "친구 요청"
        layoutSearchResultProfile.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repo.findUserUidByNicknameExact(input)
            }.onSuccess { uid ->
                foundUid = uid

                if (uid == null) {
                    tvResult.text = "검색 결과 없음"
                    btnRequest.isEnabled = false
                    layoutSearchResultProfile.visibility = View.GONE
                } else {
                    loadSearchUserProfile(uid)
                }
            }.onFailure { e ->
                tvResult.text = "검색 실패: ${e.message}"
                btnRequest.isEnabled = false
                layoutSearchResultProfile.visibility = View.GONE
            }
        }
    }

    private fun loadSearchUserProfile(targetUid: String) {
        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank()) {
            tvResult.text = "로그인이 필요해"
            btnRequest.isEnabled = false
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val targetSnap = db.collection("users")
                    .document(targetUid)
                    .get()
                    .await()

                val mySnap = db.collection("users")
                    .document(myUid)
                    .get()
                    .await()

                val isAlreadyFriend = db.collection("friends")
                    .document(myUid)
                    .collection("list")
                    .document(targetUid)
                    .get()
                    .await()
                    .exists()

                val hasPendingRequest = db.collection("friend_requests")
                    .document(FriendRequestIdUtil.requestId(myUid, targetUid))
                    .get()
                    .await()
                    .let { snap ->
                        snap.exists() &&
                                FriendRequestStatus.from(snap.getString("status")) == FriendRequestStatus.PENDING
                    }

                val hasReceivedPendingRequest = db.collection("friend_requests")
                    .document(FriendRequestIdUtil.requestId(targetUid, myUid))
                    .get()
                    .await()
                    .let { snap ->
                        snap.exists() &&
                                FriendRequestStatus.from(snap.getString("status")) == FriendRequestStatus.PENDING
                    }

                SearchUserProfileState(
                    targetUid = targetUid,
                    targetSnapExists = targetSnap.exists(),
                    nickname = targetSnap.getString("nickname") ?: foundNicknameInput,
                    statusMessage = targetSnap.getString("statusMessage") ?: "상태메시지 없음",
                    level = (targetSnap.getLong("level") ?: 1L).toInt(),
                    badgeId = targetSnap.getString("badgeId").orEmpty(),
                    favoriteGroupId = targetSnap.getString("favoriteGroupId").orEmpty(),
                    photoUrl = targetSnap.getString("photoUrl")
                        ?: targetSnap.getString("profileImageUrl"),
                    myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty(),
                    isMe = myUid == targetUid,
                    isAlreadyFriend = isAlreadyFriend,
                    hasPendingRequest = hasPendingRequest,
                    hasReceivedPendingRequest = hasReceivedPendingRequest
                )
            }.onSuccess { state ->
                renderSearchResult(state)
            }.onFailure { e ->
                tvResult.text = "유저 정보 불러오기 실패: ${e.message}"
                btnRequest.isEnabled = false
                layoutSearchResultProfile.visibility = View.GONE
            }
        }
    }

    private fun renderSearchResult(state: SearchUserProfileState) {
        if (!state.targetSnapExists) {
            tvResult.text = "검색 결과 없음"
            btnRequest.isEnabled = false
            layoutSearchResultProfile.visibility = View.GONE
            return
        }

        val finalBadgeId = if (state.badgeId.isBlank() || state.badgeId == "default") {
            getBadgeIdByLevel(state.level)
        } else {
            state.badgeId
        }

        val favoriteGroupName = if (state.favoriteGroupId.isBlank()) {
            "-"
        } else {
            groupDisplayMap[state.favoriteGroupId] ?: state.favoriteGroupId
        }

        val isSameFavorite = state.myFavoriteGroupId.isNotBlank() &&
                state.favoriteGroupId.isNotBlank() &&
                state.myFavoriteGroupId == state.favoriteGroupId

        layoutSearchResultProfile.visibility = View.VISIBLE

        tvSearchResultNickname.text = state.nickname
        tvSearchResultStatus.text = state.statusMessage.ifBlank { "상태메시지 없음" }
        tvSearchLevel.text = "Lv.${state.level}"
        tvSearchFavorite.text = if (favoriteGroupName == "-") {
            "최애: -"
        } else {
            "최애: $favoriteGroupName"
        }

        ivSearchBadge.setImageResource(mapBadgeRes(finalBadgeId))

        if (state.photoUrl.isNullOrBlank()) {
            ivSearchProfile.setImageResource(R.drawable.person_24dp)
        } else {
            ivSearchProfile.load(state.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }

        tvSearchSameFavorite.visibility = if (isSameFavorite) {
            View.VISIBLE
        } else {
            View.GONE
        }

        when {
            state.isMe -> {
                tvResult.text = "본인 계정이에요"
                btnRequest.isEnabled = false
                btnRequest.text = "나"
            }

            state.isAlreadyFriend -> {
                tvResult.text = "이미 친구인 사용자예요"
                btnRequest.isEnabled = false
                btnRequest.text = "이미 친구"
            }

            state.hasPendingRequest -> {
                tvResult.text = "이미 친구 요청을 보냈어요"
                btnRequest.isEnabled = false
                btnRequest.text = "요청됨"
            }

            state.hasReceivedPendingRequest -> {
                tvResult.text = "이 사용자가 이미 나에게 친구 요청을 보냈어요"
                btnRequest.isEnabled = false
                btnRequest.text = "받은 요청 확인"
            }

            else -> {
                tvResult.text = "검색됨: ${state.nickname}"
                btnRequest.isEnabled = true
                btnRequest.text = "친구 요청"
            }
        }
    }

    private fun sendFriendRequest() {
        val me = auth.currentUser?.uid ?: return
        val other = foundUid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                repo.sendRequest(me, other)
            }.onSuccess { result ->
                when (result) {
                    is SendResult.Sent,
                    is SendResult.AlreadyPending -> {
                        btnRequest.isEnabled = false
                        btnRequest.text = "요청됨"
                        tvResult.text = "친구 요청을 보냈어요"
                        toast("친구 요청 전송")
                    }

                    is SendResult.AlreadyFriends -> {
                        btnRequest.isEnabled = false
                        btnRequest.text = "이미 친구"
                        tvResult.text = "이미 친구인 사용자예요"
                        toast("이미 친구입니다")
                    }

                    is SendResult.Failed -> {
                        toast(result.message)
                    }
                }
            }.onFailure { e ->
                toast("요청 실패: ${e.message}")
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

    private fun getBadgeIdByLevel(level: Int): String {
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

    private data class SearchUserProfileState(
        val targetUid: String,
        val targetSnapExists: Boolean,
        val nickname: String,
        val statusMessage: String,
        val level: Int,
        val badgeId: String,
        val favoriteGroupId: String,
        val photoUrl: String?,
        val myFavoriteGroupId: String,
        val isMe: Boolean,
        val isAlreadyFriend: Boolean,
        val hasPendingRequest: Boolean,
        val hasReceivedPendingRequest: Boolean
    )
}