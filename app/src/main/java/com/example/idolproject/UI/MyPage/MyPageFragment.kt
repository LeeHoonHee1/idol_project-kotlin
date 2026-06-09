package com.example.idolproject.UI.MyPage

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.idolproject.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyPageFragment : Fragment(R.layout.fragment_mypage) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var userListener: ListenerRegistration? = null

    // -----------------------------
    // View
    // -----------------------------
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var ivEditProfile: ImageButton

    private lateinit var tvFavGroupValue: TextView
    private lateinit var btnSelectFavGroup: MaterialButton

    private lateinit var tvLevelValue: TextView
    private lateinit var ivLevelBadge: ImageView
    private lateinit var progressExp: ProgressBar
    private lateinit var tvExpValue: TextView

    private lateinit var tvPostCount: TextView
    private lateinit var tvCommentCount: TextView
    private lateinit var tvLikeCount: TextView
    private lateinit var tvTotalAttendance: TextView
    private lateinit var tvStreakAttendance: TextView
    private lateinit var tvFriendBadgeSummary: TextView

    private lateinit var tvBadgeCollectionCount: TextView
    private lateinit var tvNextBadgeHint: TextView

    private lateinit var ivBadgeBronze: ImageView
    private lateinit var ivBadgeSilver: ImageView
    private lateinit var ivBadgeGold: ImageView
    private lateinit var ivBadgePlatinum: ImageView
    private lateinit var ivBadgeMaster: ImageView
    private lateinit var ivBadgeGrandmaster: ImageView
    private lateinit var ivBadgeChallenger: ImageView

    private lateinit var tvBadgeBronze: TextView
    private lateinit var tvBadgeSilver: TextView
    private lateinit var tvBadgeGold: TextView
    private lateinit var tvBadgePlatinum: TextView
    private lateinit var tvBadgeMaster: TextView
    private lateinit var tvBadgeGrandmaster: TextView
    private lateinit var tvBadgeChallenger: TextView

    private lateinit var layoutMyPosts: View
    private lateinit var layoutFriendManage: View
    private lateinit var layoutMyInquiries: View
    private lateinit var layoutNotificationSetting: View

    // -----------------------------
    // Group data
    // -----------------------------
    private val groupMap = linkedMapOf(
        "IVE" to "ive",
        "aespa" to "aespa",
        "NewJeans" to "newjeans",
        "LE SSERAFIM" to "lesserafim",
        "BABYMONSTER" to "babymonster"
    )

    private val groupDisplayMap = groupMap.entries.associate { (display, id) ->
        id to display
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupClickListeners()
        startUserProfileListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        userListener = null
    }

    // -----------------------------
    // View binding
    // -----------------------------
    private fun bindViews(view: View) {
        ivProfileImage = view.findViewById(R.id.iv_profile_image)
        tvNickname = view.findViewById(R.id.tv_nickname)
        tvStatusMessage = view.findViewById(R.id.tv_status_message)
        ivEditProfile = view.findViewById(R.id.iv_edit_profile)

        tvFavGroupValue = view.findViewById(R.id.tv_fav_group_value)
        btnSelectFavGroup = view.findViewById(R.id.btn_select_fav_group)

        tvLevelValue = view.findViewById(R.id.tv_level_value)
        ivLevelBadge = view.findViewById(R.id.iv_level_badge)
        progressExp = view.findViewById(R.id.progress_exp)
        tvExpValue = view.findViewById(R.id.tv_exp_value)

        tvPostCount = view.findViewById(R.id.tv_post_count)
        tvCommentCount = view.findViewById(R.id.tv_comment_count)
        tvLikeCount = view.findViewById(R.id.tv_like_count)
        tvTotalAttendance = view.findViewById(R.id.tv_total_attendance)
        tvStreakAttendance = view.findViewById(R.id.tv_streak_attendance)
        tvFriendBadgeSummary = view.findViewById(R.id.tv_friend_badge_summary)

        tvBadgeCollectionCount = view.findViewById(R.id.tv_badge_collection_count)
        tvNextBadgeHint = view.findViewById(R.id.tv_next_badge_hint)

        ivBadgeBronze = view.findViewById(R.id.iv_badge_bronze)
        ivBadgeSilver = view.findViewById(R.id.iv_badge_silver)
        ivBadgeGold = view.findViewById(R.id.iv_badge_gold)
        ivBadgePlatinum = view.findViewById(R.id.iv_badge_platinum)
        ivBadgeMaster = view.findViewById(R.id.iv_badge_master)
        ivBadgeGrandmaster = view.findViewById(R.id.iv_badge_grandmaster)
        ivBadgeChallenger = view.findViewById(R.id.iv_badge_challenger)

        tvBadgeBronze = view.findViewById(R.id.tv_badge_bronze)
        tvBadgeSilver = view.findViewById(R.id.tv_badge_silver)
        tvBadgeGold = view.findViewById(R.id.tv_badge_gold)
        tvBadgePlatinum = view.findViewById(R.id.tv_badge_platinum)
        tvBadgeMaster = view.findViewById(R.id.tv_badge_master)
        tvBadgeGrandmaster = view.findViewById(R.id.tv_badge_grandmaster)
        tvBadgeChallenger = view.findViewById(R.id.tv_badge_challenger)

        layoutMyPosts = view.findViewById(R.id.layout_my_posts)
        layoutFriendManage = view.findViewById(R.id.layout_friend_manage)
        layoutMyInquiries = view.findViewById(R.id.layout_my_inquiries)
        layoutNotificationSetting = view.findViewById(R.id.layout_notification_setting)
    }

    // -----------------------------
    // Click
    // -----------------------------
    private fun setupClickListeners() {
        ivEditProfile.setOnClickListener {
            showChangeNicknameDialog()
        }

        btnSelectFavGroup.setOnClickListener {
            showFavoriteGroupDialog()
        }

        ivProfileImage.setOnClickListener {
            // TODO: 프로필 이미지 변경 기능은 나중에 Storage/ImagePicker 붙일 때 구현
            toast("프로필 이미지 변경은 나중에 연결할게")
        }

        layoutMyPosts.setOnClickListener {
            toast("내가 쓴 글 화면은 나중에 연결할게")
        }

        layoutFriendManage.setOnClickListener {
            toast("친구 화면으로 연결할 예정이야")
        }

        layoutMyInquiries.setOnClickListener {
            toast("고객센터 문의 내역은 나중에 연결할게")
        }

        layoutNotificationSetting.setOnClickListener {
            toast("알림 설정은 나중에 연결할게")
        }
    }

    // -----------------------------
    // Firestore -> UI
    // -----------------------------
    private fun startUserProfileListener() {
        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            toast("로그인이 필요해")
            return
        }

        val userRef = db.collection("users").document(uid)

        userListener?.remove()
        userListener = userRef.addSnapshotListener { snap, e ->
            if (e != null) {
                Log.e("MYPAGE", "profile listener failed", e)
                toast("프로필 로드 실패: ${e.message}")
                return@addSnapshotListener
            }

            if (snap == null || !snap.exists()) {
                toast("유저 문서가 없어. 회원가입 저장 로직 확인 필요")
                return@addSnapshotListener
            }

            val nickname = snap.getString("nickname") ?: "닉네임 없음"
            val status = snap.getString("statusMessage") ?: "상태메시지를 입력해 주세요."

            val level = (snap.getLong("level") ?: 1L).toInt()
            val exp = (snap.getLong("exp") ?: 0L).toInt()

            val needExp = 100
            val percent = ((exp * 100) / needExp).coerceIn(0, 100)

            val badgeIdFromDb = snap.getString("badgeId").orEmpty()
            val badgeIdByLevel = getBadgeIdByLevel(level)

            // Firestore에 badgeId가 없거나 default면 level 기준으로 표시
            val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                badgeIdByLevel
            } else {
                badgeIdFromDb
            }

            tvNickname.text = nickname
            tvStatusMessage.text = status

            tvLevelValue.text = "Lv.$level"
            progressExp.max = needExp
            progressExp.progress = exp.coerceIn(0, needExp)
            tvExpValue.text = "EXP $exp / $needExp ($percent%)"

            ivLevelBadge.setImageResource(mapBadgeRes(finalBadgeId))

            updateBadgeCollection(level)

            // badgeId가 비어 있거나 level 기준과 다르면 Firestore도 보정
            syncBadgeIdIfNeeded(uid, badgeIdFromDb, badgeIdByLevel)

            val favoriteGroupId = snap.getString("favoriteGroupId").orEmpty()

            tvFavGroupValue.text = if (favoriteGroupId.isBlank()) {
                "선택 안 함"
            } else {
                groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
            }

            btnSelectFavGroup.text = if (favoriteGroupId.isBlank()) {
                "선택"
            } else {
                "변경"
            }

            // 아직 미연동이면 placeholder
            tvPostCount.text = "작성 글: 0개"
            tvCommentCount.text = "댓글: 0개"
            tvLikeCount.text = "좋아요: 0개"
            tvTotalAttendance.text = "총 출석: 0일"
            tvStreakAttendance.text = "연속 출석: 0일"
            tvFriendBadgeSummary.text = "친구 0명 · 대표 칭호: 없음"
        }
    }

    private fun updateBadgeCollection(level: Int) {
        val unlockedCount = getUnlockedBadgeCount(level)

        tvBadgeCollectionCount.text = "$unlockedCount / 7"

        setBadgeUnlocked(ivBadgeBronze, tvBadgeBronze, level >= 1)
        setBadgeUnlocked(ivBadgeSilver, tvBadgeSilver, level >= 5)
        setBadgeUnlocked(ivBadgeGold, tvBadgeGold, level >= 10)
        setBadgeUnlocked(ivBadgePlatinum, tvBadgePlatinum, level >= 15)
        setBadgeUnlocked(ivBadgeMaster, tvBadgeMaster, level >= 20)
        setBadgeUnlocked(ivBadgeGrandmaster, tvBadgeGrandmaster, level >= 30)
        setBadgeUnlocked(ivBadgeChallenger, tvBadgeChallenger, level >= 40)

        tvNextBadgeHint.text = getNextBadgeHint(level)
    }

    private fun setBadgeUnlocked(imageView: ImageView, textView: TextView, unlocked: Boolean) {
        imageView.alpha = if (unlocked) 1.0f else 0.25f
        textView.alpha = if (unlocked) 1.0f else 0.45f

        textView.text = if (unlocked) {
            textView.text.toString().replace(" 🔒", "")
        } else {
            val currentText = textView.text.toString().replace(" 🔒", "")
            "$currentText 🔒"
        }
    }

    private fun getUnlockedBadgeCount(level: Int): Int {
        return when {
            level >= 40 -> 7
            level >= 30 -> 6
            level >= 20 -> 5
            level >= 15 -> 4
            level >= 10 -> 3
            level >= 5 -> 2
            else -> 1
        }
    }

    private fun getNextBadgeHint(level: Int): String {
        return when {
            level < 5 -> "다음 뱃지 Silver까지 ${5 - level}레벨 남았어요"
            level < 10 -> "다음 뱃지 Gold까지 ${10 - level}레벨 남았어요"
            level < 15 -> "다음 뱃지 Platinum까지 ${15 - level}레벨 남았어요"
            level < 20 -> "다음 뱃지 Master까지 ${20 - level}레벨 남았어요"
            level < 30 -> "다음 뱃지 Grand Master까지 ${30 - level}레벨 남았어요"
            level < 40 -> "다음 뱃지 Challenger까지 ${40 - level}레벨 남았어요"
            else -> "최고 등급 Challenger 뱃지를 획득했어요 👑"
        }
    }

    private fun syncBadgeIdIfNeeded(uid: String, currentBadgeId: String, correctBadgeId: String) {
        if (currentBadgeId == correctBadgeId) return

        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "badgeId" to correctBadgeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { e ->
                Log.e("MYPAGE_BADGE", "badgeId sync failed", e)
            }
    }

    // -----------------------------
    // Badge
    // -----------------------------
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

    // -----------------------------
    // Nickname change dialog + transaction
    // -----------------------------
    private fun showChangeNicknameDialog() {
        val uid = auth.currentUser?.uid ?: run {
            toast("로그인이 필요해")
            return
        }

        val input = EditText(requireContext()).apply {
            setText(tvNickname.text?.toString().orEmpty())
            setSelection(text.length)
            hint = "새 닉네임"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("닉네임 변경")
            .setView(input)
            .setNegativeButton("취소", null)
            .setPositiveButton("저장") { _, _ ->
                val newNickname = input.text.toString().trim()
                val newKey = normalizeNicknameKey(newNickname)

                if (!isValidNickname(newNickname)) {
                    toast("닉네임은 2~12자, 공백 없이 입력해줘")
                    return@setPositiveButton
                }

                changeNicknameTransaction(uid, newNickname, newKey)
            }
            .show()
    }

    private fun changeNicknameTransaction(uid: String, newNickname: String, newKey: String) {
        val userRef = db.collection("users").document(uid)
        val newNickRef = db.collection("nicknames").document(newKey)

        db.runTransaction { tx ->
            // ---------- READ PHASE ----------
            val userSnap = tx.get(userRef)
            val oldKey = userSnap.getString("nicknameKey")?.trim().orEmpty()

            if (oldKey == newKey) {
                return@runTransaction null
            }

            val newNickSnap = tx.get(newNickRef)

            if (newNickSnap.exists()) {
                throw IllegalStateException("TAKEN")
            }

            var canDeleteOld = false
            var oldNickRef: DocumentReference? = null

            if (oldKey.isNotBlank()) {
                oldNickRef = db.collection("nicknames").document(oldKey)
                val oldSnap = tx.get(oldNickRef)
                val reservedUid = oldSnap.getString("uid")
                canDeleteOld = oldSnap.exists() && reservedUid == uid
            }

            // ---------- WRITE PHASE ----------
            tx.set(
                newNickRef,
                hashMapOf(
                    "uid" to uid,
                    "nickname" to newNickname,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            tx.update(
                userRef,
                mapOf(
                    "nickname" to newNickname,
                    "nicknameKey" to newKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (canDeleteOld) {
                tx.delete(oldNickRef!!)
            }

            null
        }.addOnSuccessListener {
            toast("닉네임 변경 완료 ✅")
        }.addOnFailureListener { e ->
            Log.e("NICK_CHANGE", "changeNicknameTransaction failed", e)

            if (e.message?.contains("TAKEN") == true) {
                toast("이미 사용 중인 닉네임이야")
            } else {
                toast("닉네임 변경 실패: ${e.message}")
            }
        }
    }

    private fun isValidNickname(nickname: String): Boolean {
        val n = nickname.trim()

        if (n.length !in 2..12) return false
        if (n.contains(" ")) return false

        return true
    }

    private fun normalizeNicknameKey(nickname: String): String {
        return nickname.trim()
            .lowercase()
            .replace("\\s+".toRegex(), "")
    }

    // -----------------------------
    // Favorite group
    // -----------------------------
    private fun showFavoriteGroupDialog() {
        val uid = auth.currentUser?.uid ?: run {
            toast("로그인이 필요해")
            return
        }

        val displayNames = groupMap.keys.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("최애 그룹 선택")
            .setItems(displayNames) { _, which ->
                val selectedDisplayName = displayNames[which]
                val selectedGroupId = groupMap[selectedDisplayName] ?: return@setItems

                saveFavoriteGroup(uid, selectedGroupId)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun saveFavoriteGroup(uid: String, newGroupId: String) {
        val userRef = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val oldGroupId = userSnap.getString("favoriteGroupId").orEmpty()

            if (oldGroupId == newGroupId) {
                return@runTransaction "same"
            }

            if (oldGroupId.isNotBlank()) {
                val oldGroupRef = db.collection("groups").document(oldGroupId)
                tx.update(oldGroupRef, "likeCount", FieldValue.increment(-1))
            }

            val newGroupRef = db.collection("groups").document(newGroupId)
            tx.update(newGroupRef, "likeCount", FieldValue.increment(1))

            tx.update(
                userRef,
                mapOf(
                    "favoriteGroupId" to newGroupId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            "changed"
        }.addOnSuccessListener { result ->
            if (result == "same") {
                toast("이미 선택한 그룹이야")
            } else {
                toast("최애 그룹이 저장됐어 ✅")
            }
        }.addOnFailureListener { e ->
            Log.e("FAV_GROUP", "saveFavoriteGroup failed", e)
            toast("최애 그룹 저장 실패: ${e.message}")
        }
    }

    // -----------------------------
    // Util
    // -----------------------------
    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}