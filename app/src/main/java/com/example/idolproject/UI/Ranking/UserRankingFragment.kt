package com.example.idolproject.UI.Ranking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserRankingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rankingAdapter: UserRankingAdapter

    private lateinit var imgMyProfile: ImageView
    private lateinit var tvMyNickname: TextView
    private lateinit var tvMyLevelExp: TextView
    private lateinit var tvMyRank: TextView
    private lateinit var imgMyBadge: ImageView

    private lateinit var imgRank1Profile: ImageView
    private lateinit var tvRank1Nickname: TextView
    private lateinit var tvRank1Level: TextView
    private lateinit var imgRank1UserBadge: ImageView

    private lateinit var imgRank2Profile: ImageView
    private lateinit var tvRank2Nickname: TextView
    private lateinit var tvRank2Level: TextView
    private lateinit var imgRank2UserBadge: ImageView

    private lateinit var imgRank3Profile: ImageView
    private lateinit var tvRank3Nickname: TextView
    private lateinit var tvRank3Level: TextView
    private lateinit var imgRank3UserBadge: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ranking_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        loadUserRanking()
    }

    private fun bindViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_user_ranking)

        imgMyProfile = view.findViewById(R.id.img_my_profile)
        tvMyNickname = view.findViewById(R.id.tv_my_nickname)
        tvMyLevelExp = view.findViewById(R.id.tv_my_level_exp)
        tvMyRank = view.findViewById(R.id.tv_my_rank)
        imgMyBadge = view.findViewById(R.id.img_my_badge)

        imgRank1Profile = view.findViewById(R.id.img_rank1_profile)
        tvRank1Nickname = view.findViewById(R.id.tv_rank1_nickname)
        tvRank1Level = view.findViewById(R.id.tv_rank1_level)
        imgRank1UserBadge = view.findViewById(R.id.img_rank1_user_badge)

        imgRank2Profile = view.findViewById(R.id.img_rank2_profile)
        tvRank2Nickname = view.findViewById(R.id.tv_rank2_nickname)
        tvRank2Level = view.findViewById(R.id.tv_rank2_level)
        imgRank2UserBadge = view.findViewById(R.id.img_rank2_user_badge)

        imgRank3Profile = view.findViewById(R.id.img_rank3_profile)
        tvRank3Nickname = view.findViewById(R.id.tv_rank3_nickname)
        tvRank3Level = view.findViewById(R.id.tv_rank3_level)
        imgRank3UserBadge = view.findViewById(R.id.img_rank3_user_badge)
    }

    private fun setupRecyclerView() {
        rankingAdapter = UserRankingAdapter(
            items = emptyList(),
            myUid = auth.currentUser?.uid
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = rankingAdapter
    }

    private fun loadUserRanking() {
        db.collection("users")
            .orderBy("level", Query.Direction.DESCENDING)
            .orderBy("exp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.map { doc ->
                    UserRank(
                        uid = doc.id,
                        nickname = doc.getString("nickname") ?: "이름없음",
                        level = doc.getLong("level") ?: 1L,
                        exp = doc.getLong("exp") ?: 0L,
                        badgeId = doc.getString("badgeId").orEmpty(),
                        profileImageUrl = doc.getString("photoUrl")
                            ?: doc.getString("profileImageUrl")
                            ?: "",
                        favoriteGroupId = doc.getString("favoriteGroupId").orEmpty(),
                        pointsTotal = doc.getLong("points_total") ?: 0L
                    )
                }

                updateRankingUI(users)
            }
            .addOnFailureListener { e ->
                Log.e("UserRankingFragment", "랭킹 불러오기 실패", e)
            }
    }

    private fun updateRankingUI(sortedUsers: List<UserRank>) {
        if (sortedUsers.isEmpty()) {
            bindEmptyTop3()
            bindEmptyMyCard()
            rankingAdapter.updateList(emptyList())
            return
        }

        val top3 = sortedUsers.take(3)
        val others = if (sortedUsers.size > 3) {
            sortedUsers.drop(3)
        } else {
            emptyList()
        }

        bindTop3(top3)
        bindMyCard(sortedUsers)
        rankingAdapter.updateList(others)
    }

    private fun bindTop3(top3: List<UserRank>) {
        bindTopRank(
            user = top3.getOrNull(0),
            nicknameView = tvRank1Nickname,
            levelView = tvRank1Level,
            profileView = imgRank1Profile,
            badgeView = imgRank1UserBadge,
            emptyName = "1등 대기"
        )

        bindTopRank(
            user = top3.getOrNull(1),
            nicknameView = tvRank2Nickname,
            levelView = tvRank2Level,
            profileView = imgRank2Profile,
            badgeView = imgRank2UserBadge,
            emptyName = "2등 대기"
        )

        bindTopRank(
            user = top3.getOrNull(2),
            nicknameView = tvRank3Nickname,
            levelView = tvRank3Level,
            profileView = imgRank3Profile,
            badgeView = imgRank3UserBadge,
            emptyName = "3등 대기"
        )
    }

    private fun bindTopRank(
        user: UserRank?,
        nicknameView: TextView,
        levelView: TextView,
        profileView: ImageView,
        badgeView: ImageView,
        emptyName: String
    ) {
        if (user == null) {
            nicknameView.text = emptyName
            levelView.text = "Lv.- · EXP -"
            profileView.setImageResource(R.drawable.person_24dp)
            badgeView.setImageResource(R.drawable.ic_badge_bronze)
            return
        }

        nicknameView.text = user.nickname
        levelView.text = "Lv.${user.level} · EXP ${user.exp}"

        if (user.profileImageUrl.isBlank()) {
            profileView.setImageResource(R.drawable.person_24dp)
        } else {
            profileView.load(user.profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }

        badgeView.setImageResource(
            mapBadgeRes(
                badgeId = user.badgeId,
                level = user.level
            )
        )
    }

    private fun bindMyCard(sortedUsers: List<UserRank>) {
        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank()) {
            bindEmptyMyCard()
            return
        }

        val myIndex = sortedUsers.indexOfFirst { it.uid == myUid }

        if (myIndex == -1) {
            bindEmptyMyCard()
            return
        }

        val myUser = sortedUsers[myIndex]
        val myRank = myIndex + 1

        tvMyNickname.text = myUser.nickname
        tvMyLevelExp.text = "Lv.${myUser.level} · EXP ${myUser.exp}"
        tvMyRank.text = "${myRank}위"

        if (myUser.profileImageUrl.isBlank()) {
            imgMyProfile.setImageResource(R.drawable.person_24dp)
        } else {
            imgMyProfile.load(myUser.profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.person_24dp)
                error(R.drawable.person_24dp)
            }
        }

        imgMyBadge.setImageResource(
            mapBadgeRes(
                badgeId = myUser.badgeId,
                level = myUser.level
            )
        )
    }

    private fun bindEmptyTop3() {
        bindTopRank(
            user = null,
            nicknameView = tvRank1Nickname,
            levelView = tvRank1Level,
            profileView = imgRank1Profile,
            badgeView = imgRank1UserBadge,
            emptyName = "1등 대기"
        )

        bindTopRank(
            user = null,
            nicknameView = tvRank2Nickname,
            levelView = tvRank2Level,
            profileView = imgRank2Profile,
            badgeView = imgRank2UserBadge,
            emptyName = "2등 대기"
        )

        bindTopRank(
            user = null,
            nicknameView = tvRank3Nickname,
            levelView = tvRank3Level,
            profileView = imgRank3Profile,
            badgeView = imgRank3UserBadge,
            emptyName = "3등 대기"
        )
    }

    private fun bindEmptyMyCard() {
        tvMyNickname.text = "내 랭킹 정보 없음"
        tvMyLevelExp.text = "로그인 또는 유저 정보를 확인해주세요"
        tvMyRank.text = "-위"
        imgMyProfile.setImageResource(R.drawable.person_24dp)
        imgMyBadge.setImageResource(R.drawable.ic_badge_bronze)
    }

    private fun mapBadgeRes(badgeId: String, level: Long): Int {
        val finalBadgeId = if (badgeId.isBlank() || badgeId == "default") {
            getBadgeIdByLevel(level)
        } else {
            badgeId
        }

        return when (finalBadgeId) {
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

    private fun getBadgeIdByLevel(level: Long): String {
        return when (level) {
            in 1L..4L -> "bronze"
            in 5L..9L -> "silver"
            in 10L..14L -> "gold"
            in 15L..19L -> "platinum"
            in 20L..29L -> "master"
            in 30L..39L -> "grandmaster"
            else -> "challenger"
        }
    }
}