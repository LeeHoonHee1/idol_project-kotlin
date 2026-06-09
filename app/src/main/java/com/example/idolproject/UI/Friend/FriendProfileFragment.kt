package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import coil.load
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var userListener: ListenerRegistration? = null

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

        val friendUid = arguments?.getString(ARG_UID).orEmpty()

        if (friendUid.isBlank()) {
            Log.e("FRIEND_PROFILE", "friendUid is blank")
            tvNickname.text = "친구 정보를 찾을 수 없어"
            return
        }

        setLoadingState()
        listenFriendProfile(friendUid)
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

    private fun listenFriendProfile(friendUid: String) {
        val myUid = auth.currentUser?.uid

        userListener?.remove()

        userListener = db.collection("users")
            .document(friendUid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("FRIEND_PROFILE", "listen failed", err)
                    tvNickname.text = "불러오기 실패"
                    tvStatus.text = err.message.orEmpty()
                    return@addSnapshotListener
                }

                if (snap == null || !snap.exists()) {
                    tvNickname.text = "존재하지 않는 사용자"
                    tvStatus.text = ""
                    return@addSnapshotListener
                }

                val nickname = snap.getString("nickname") ?: "(알 수 없음)"
                val status = snap.getString("statusMessage") ?: "상태메시지 없음"

                val level = (snap.getLong("level") ?: 1L).toInt()
                val badgeIdFromDb = snap.getString("badgeId").orEmpty()
                val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                    getBadgeIdByLevel(level)
                } else {
                    badgeIdFromDb
                }

                val favoriteGroupId = snap.getString("favoriteGroupId").orEmpty()
                val favoriteGroupName = if (favoriteGroupId.isBlank()) {
                    "-"
                } else {
                    groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
                }

                val photoUrl = snap.getString("photoUrl")
                    ?: snap.getString("profileImageUrl")

                tvNickname.text = nickname
                tvStatus.text = status.ifBlank { "상태메시지 없음" }
                tvLevel.text = "Lv.$level"
                tvFavorite.text = if (favoriteGroupName == "-") {
                    "최애: -"
                } else {
                    "최애\n$favoriteGroupName"
                }

                ivBadge.setImageResource(mapBadgeRes(finalBadgeId))
                tvBadgeHint.text = makeBadgeHint(finalBadgeId, level)

                if (photoUrl.isNullOrBlank()) {
                    ivProfile.setImageResource(R.drawable.person_24dp)
                } else {
                    ivProfile.load(photoUrl) {
                        crossfade(true)
                        placeholder(R.drawable.person_24dp)
                        error(R.drawable.person_24dp)
                    }
                }

                updateSameFavoriteLabel(myUid, favoriteGroupId)
            }
    }

    private fun updateSameFavoriteLabel(myUid: String?, friendFavoriteGroupId: String) {
        if (myUid.isNullOrBlank() || friendFavoriteGroupId.isBlank()) {
            tvSameFavorite.visibility = View.GONE
            return
        }

        db.collection("users")
            .document(myUid)
            .get()
            .addOnSuccessListener { mySnap ->
                val myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty()

                tvSameFavorite.visibility =
                    if (myFavoriteGroupId.isNotBlank() && myFavoriteGroupId == friendFavoriteGroupId) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
            .addOnFailureListener {
                tvSameFavorite.visibility = View.GONE
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

    override fun onDestroyView() {
        userListener?.remove()
        userListener = null
        super.onDestroyView()
    }
}