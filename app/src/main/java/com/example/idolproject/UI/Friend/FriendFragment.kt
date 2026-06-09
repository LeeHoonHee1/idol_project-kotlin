package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.android.material.chip.ChipGroup

class FriendFragment : Fragment(R.layout.fragment_friend) {

    private lateinit var recyclerFriends: RecyclerView
    private lateinit var tvFriendCount: TextView
    private lateinit var adapter: FriendAdapter

    private lateinit var btnSearchFriend: MaterialButton
    private lateinit var btnAddFriend: MaterialButton

    private lateinit var layoutFriendEmpty: View
    private lateinit var btnEmptyFindFriend: MaterialButton

    private lateinit var tvFriendEmptyTitle: TextView
    private lateinit var tvFriendEmptyDesc: TextView

    private lateinit var chipGroupFriendFilter: ChipGroup

    private var allFriends: List<Friend> = emptyList()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var friendsListener: ListenerRegistration? = null
    private var requestCountListener: ListenerRegistration? = null

    private var currentFriendCount: Int = 0
    private var currentPendingRequestCount: Int = 0

    private val groupDisplayMap = mapOf(
        "ive" to "IVE",
        "aespa" to "aespa",
        "newjeans" to "NewJeans",
        "lesserafim" to "LE SSERAFIM",
        "babymonster" to "BABYMONSTER"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerFriends = view.findViewById(R.id.recycler_friends)
        tvFriendCount = view.findViewById(R.id.tv_friend_count)

        btnSearchFriend = view.findViewById(R.id.btn_search_friend)
        btnAddFriend = view.findViewById(R.id.btn_add_friend)

        layoutFriendEmpty = view.findViewById(R.id.layout_friend_empty)
        btnEmptyFindFriend = view.findViewById(R.id.btn_empty_find_friend)
        tvFriendEmptyTitle = view.findViewById(R.id.tv_friend_empty_title)
        tvFriendEmptyDesc = view.findViewById(R.id.tv_friend_empty_desc)

        chipGroupFriendFilter = view.findViewById(R.id.chip_group_friend_filter)

        btnSearchFriend.setOnClickListener {
            openFriendSearch()
        }

        btnAddFriend.setOnClickListener {
            openFriendSearch()
        }

        btnEmptyFindFriend.setOnClickListener {
            openFriendSearch()
        }

        chipGroupFriendFilter.setOnCheckedStateChangeListener { _, _ ->
            applyFriendFilter()
        }

        adapter = FriendAdapter(
            onItemClick = { friend ->
                openFriendProfile(friend.uid)
            },
            onDeleteClick = { friend ->
                showDeleteConfirm(friend)
            }
        )

        recyclerFriends.layoutManager = LinearLayoutManager(requireContext())
        recyclerFriends.adapter = adapter

        startFriendsListener()
        startPendingRequestCountListener()
    }

    private fun startFriendsListener() {
        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank()) {
            toast("로그인이 필요해")
            return
        }

        friendsListener?.remove()

        friendsListener = db.collection("friends")
            .document(myUid)
            .collection("list")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("FRIEND_LIST", "friends listener failed", err)
                    toast("친구 목록을 불러오지 못했어")
                    return@addSnapshotListener
                }

                val friendUids = snap?.documents
                    ?.map { it.id }
                    .orEmpty()

                loadFriendProfiles(myUid, friendUids)
            }
    }

    private fun startPendingRequestCountListener() {
        val myUid = auth.currentUser?.uid

        if (myUid.isNullOrBlank()) {
            return
        }

        requestCountListener?.remove()

        requestCountListener = db.collection("friend_requests")
            .whereEqualTo("receiverUid", myUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("FRIEND_COUNT", "pending request count listener failed", err)
                    return@addSnapshotListener
                }

                currentPendingRequestCount = snap?.size() ?: 0
                updateFriendSummaryText()
            }
    }

    private fun updateFriendSummaryText() {
        tvFriendCount.text = if (currentPendingRequestCount > 0) {
            "${currentFriendCount}명 · 요청 ${currentPendingRequestCount}개"
        } else {
            "${currentFriendCount}명"
        }
    }

    private fun applyFriendFilter() {
        val filteredList = when (chipGroupFriendFilter.checkedChipId) {
            R.id.chip_friend_same_favorite -> {
                allFriends
                    .filter { it.isSameFavorite }
                    .sortedWith(
                        compareByDescending<Friend> { it.level }
                            .thenBy { it.nickname }
                    )
            }

            R.id.chip_friend_level_desc -> {
                allFriends
                    .sortedWith(
                        compareByDescending<Friend> { it.level }
                            .thenBy { it.nickname }
                    )
            }

            else -> {
                allFriends
                    .sortedWith(
                        compareByDescending<Friend> { it.isSameFavorite }
                            .thenByDescending { it.level }
                            .thenBy { it.nickname }
                    )
            }
        }

        adapter.submitList(filteredList)
        updateEmptyState(filteredList.size)
    }

    private fun loadFriendProfiles(myUid: String, friendUids: List<String>) {
        if (friendUids.isEmpty()) {
            allFriends = emptyList()
            adapter.submitList(emptyList())
            currentFriendCount = 0
            updateFriendSummaryText()
            updateEmptyState(0)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val mySnap = db.collection("users")
                    .document(myUid)
                    .get()
                    .await()

                val myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty()

                val friends = mutableListOf<Friend>()

                for (friendUid in friendUids) {
                    val userSnap = db.collection("users")
                        .document(friendUid)
                        .get()
                        .await()

                    if (!userSnap.exists()) continue

                    val nickname = userSnap.getString("nickname") ?: "(알 수 없음)"
                    val statusMessage = userSnap.getString("statusMessage") ?: "상태메시지 없음"

                    val level = (userSnap.getLong("level") ?: 1L).toInt()
                    val badgeIdFromDb = userSnap.getString("badgeId").orEmpty()
                    val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                        getBadgeIdByLevel(level)
                    } else {
                        badgeIdFromDb
                    }

                    val favoriteGroupId = userSnap.getString("favoriteGroupId").orEmpty()
                    val favoriteGroupName = if (favoriteGroupId.isBlank()) {
                        "-"
                    } else {
                        groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
                    }

                    val photoUrl = userSnap.getString("photoUrl")

                    val isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                            favoriteGroupId.isNotBlank() &&
                            myFavoriteGroupId == favoriteGroupId

                    friends.add(
                        Friend(
                            uid = friendUid,
                            nickname = nickname,
                            statusMessage = statusMessage,
                            favoriteGroupId = favoriteGroupId,
                            favoriteGroupName = favoriteGroupName,
                            level = level,
                            badgeId = finalBadgeId,
                            photoUrl = photoUrl,
                            isSameFavorite = isSameFavorite
                        )
                    )
                }

                friends.sortedWith(
                    compareByDescending<Friend> { it.isSameFavorite }
                        .thenByDescending { it.level }
                        .thenBy { it.nickname }
                )
            }.onSuccess { friends ->
                allFriends = friends
                currentFriendCount = friends.size
                updateFriendSummaryText()
                applyFriendFilter()
            }.onFailure { e ->
                Log.e("FRIEND_LIST", "loadFriendProfiles failed", e)
                toast("친구 정보를 불러오지 못했어: ${e.message}")

                allFriends = emptyList()
                adapter.submitList(emptyList())
                currentFriendCount = 0
                updateFriendSummaryText()
                updateEmptyState(0)
            }
        }
    }

    private fun showDeleteConfirm(friend: Friend) {
        AlertDialog.Builder(requireContext())
            .setTitle("친구 삭제")
            .setMessage("${friend.nickname}님을 친구에서 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                val me = auth.currentUser?.uid ?: return@setPositiveButton

                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        deleteFriendBoth(me, friend.uid)
                    }.onSuccess {
                        toast("친구를 삭제했어")
                    }.onFailure { e ->
                        Log.e("FRIEND_DEL", "delete failed", e)
                        toast("삭제 실패: ${e.message}")
                    }
                }
            }
            .show()
    }

    /**
     * friends/{me}/list/{other} + friends/{other}/list/{me} 양방향 삭제
     */
    private suspend fun deleteFriendBoth(meUid: String, otherUid: String) {
        val myFriendRef = db.collection("friends")
            .document(meUid)
            .collection("list")
            .document(otherUid)

        val otherFriendRef = db.collection("friends")
            .document(otherUid)
            .collection("list")
            .document(meUid)

        val batch = db.batch()

        batch.delete(myFriendRef)
        batch.delete(otherFriendRef)

        batch.commit().await()
    }

    private fun openFriendSearch() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendSearchFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openFriendProfile(friendUid: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendProfileFragment.newInstance(friendUid))
            .addToBackStack(null)
            .commit()
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

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        friendsListener?.remove()
        friendsListener = null

        requestCountListener?.remove()
        requestCountListener = null

        super.onDestroyView()
    }

    private fun openFriendRequests() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendRequestsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun updateEmptyState(displayCount: Int) {
        if (displayCount == 0) {
            recyclerFriends.visibility = View.GONE
            layoutFriendEmpty.visibility = View.VISIBLE

            when (chipGroupFriendFilter.checkedChipId) {
                R.id.chip_friend_same_favorite -> {
                    if (allFriends.isEmpty()) {
                        tvFriendEmptyTitle.text = "아직 친구가 없어요"
                        tvFriendEmptyDesc.text = "닉네임으로 팬 친구를 찾아보세요"
                        btnEmptyFindFriend.visibility = View.VISIBLE
                    } else {
                        tvFriendEmptyTitle.text = "같은 최애 친구가 아직 없어요"
                        tvFriendEmptyDesc.text = "다른 필터를 선택하거나 새 친구를 찾아보세요"
                        btnEmptyFindFriend.visibility = View.VISIBLE
                    }
                }

                R.id.chip_friend_level_desc -> {
                    if (allFriends.isEmpty()) {
                        tvFriendEmptyTitle.text = "아직 친구가 없어요"
                        tvFriendEmptyDesc.text = "닉네임으로 팬 친구를 찾아보세요"
                        btnEmptyFindFriend.visibility = View.VISIBLE
                    } else {
                        tvFriendEmptyTitle.text = "표시할 친구가 없어요"
                        tvFriendEmptyDesc.text = "전체 필터로 다시 확인해보세요"
                        btnEmptyFindFriend.visibility = View.GONE
                    }
                }

                else -> {
                    tvFriendEmptyTitle.text = "아직 친구가 없어요"
                    tvFriendEmptyDesc.text = "닉네임으로 팬 친구를 찾아보세요"
                    btnEmptyFindFriend.visibility = View.VISIBLE
                }
            }
        } else {
            recyclerFriends.visibility = View.VISIBLE
            layoutFriendEmpty.visibility = View.GONE
        }
    }
}