package com.example.idolproject.UI.Friend

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendRequestsFragment : Fragment(R.layout.fragment_friend_requests) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repo = FriendRequestRepository()

    private lateinit var rv: RecyclerView
    private lateinit var adapter: FriendRequestsAdapter

    private var listener: ListenerRegistration? = null

    private val groupDisplayMap = mapOf(
        "ive" to "IVE",
        "aespa" to "aespa",
        "newjeans" to "NewJeans",
        "lesserafim" to "LE SSERAFIM",
        "babymonster" to "BABYMONSTER"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rv_requests)

        adapter = FriendRequestsAdapter(
            onAccept = { item ->
                val me = auth.currentUser?.uid ?: return@FriendRequestsAdapter

                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        repo.acceptRequest(item.senderUid, me)
                    }.onSuccess { ok ->
                        if (ok) {
                            toast("${item.senderNickname}님과 친구가 되었어")
                        } else {
                            toast("수락 실패: 요청 상태를 확인해줘")
                        }
                    }.onFailure { e ->
                        Log.e("FRIEND_REQ", "accept failed", e)
                        toast("수락 실패: ${e.message}")
                    }
                }
            },
            onReject = { item ->
                val me = auth.currentUser?.uid ?: return@FriendRequestsAdapter

                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        repo.rejectRequest(item.senderUid, me)
                    }.onSuccess { ok ->
                        if (ok) {
                            toast("친구 요청을 거절했어")
                        } else {
                            toast("거절 실패: 요청 상태를 확인해줘")
                        }
                    }.onFailure { e ->
                        Log.e("FRIEND_REQ", "reject failed", e)
                        toast("거절 실패: ${e.message}")
                    }
                }
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val me = auth.currentUser?.uid ?: return

        Log.d("FRIEND_REQ", "FriendRequestsFragment me=$me")

        markAllPendingChecked(me)
        startRequestListener(me)
    }

    private fun startRequestListener(myUid: String) {
        listener?.remove()

        listener = db.collection("friend_requests")
            .whereEqualTo("receiverUid", myUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("FRIEND_REQ", "listen failed", err)
                    toast("요청 조회 실패: ${err.message}")
                    return@addSnapshotListener
                }

                val docs = snap?.documents.orEmpty()

                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        val mySnap = db.collection("users")
                            .document(myUid)
                            .get()
                            .await()

                        val myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty()

                        docs.mapNotNull { doc ->
                            val senderUid = doc.getString("senderUid") ?: return@mapNotNull null

                            async {
                                val user = db.collection("users")
                                    .document(senderUid)
                                    .get()
                                    .await()

                                if (!user.exists()) return@async null

                                val nickname = user.getString("nickname") ?: "(알 수 없음)"
                                val statusMessage = user.getString("statusMessage") ?: "상태메시지 없음"

                                val level = (user.getLong("level") ?: 1L).toInt()
                                val badgeIdFromDb = user.getString("badgeId").orEmpty()
                                val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                                    getBadgeIdByLevel(level)
                                } else {
                                    badgeIdFromDb
                                }

                                val favoriteGroupId = user.getString("favoriteGroupId").orEmpty()
                                val favoriteGroupName = if (favoriteGroupId.isBlank()) {
                                    "-"
                                } else {
                                    groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
                                }

                                val photoUrl = user.getString("photoUrl")
                                    ?: user.getString("profileImageUrl")

                                val isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                                        favoriteGroupId.isNotBlank() &&
                                        myFavoriteGroupId == favoriteGroupId

                                FriendRequestItem(
                                    senderUid = senderUid,
                                    senderNickname = nickname,
                                    statusMessage = statusMessage,
                                    level = level,
                                    badgeId = finalBadgeId,
                                    favoriteGroupId = favoriteGroupId,
                                    favoriteGroupName = favoriteGroupName,
                                    photoUrl = photoUrl,
                                    isSameFavorite = isSameFavorite
                                )
                            }
                        }.awaitAll().filterNotNull()
                    }.onSuccess { items ->
                        adapter.submitList(
                            items.sortedWith(
                                compareByDescending<FriendRequestItem> { it.isSameFavorite }
                                    .thenByDescending { it.level }
                                    .thenBy { it.senderNickname }
                            )
                        )
                    }.onFailure { e ->
                        Log.e("FRIEND_REQ", "load request users failed", e)
                        toast("요청자 정보를 불러오지 못했어: ${e.message}")
                    }
                }
            }
    }

    private fun markAllPendingChecked(meUid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val snap = db.collection("friend_requests")
                    .whereEqualTo("receiverUid", meUid)
                    .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
                    .whereEqualTo("checked", false)
                    .get()
                    .await()

                if (snap.isEmpty) return@runCatching

                val batch = db.batch()

                for (doc in snap.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "checked" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                batch.commit().await()
            }.onFailure { e ->
                Log.e("FRIEND_REQ", "markAllPendingChecked failed", e)
            }
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

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        super.onDestroyView()
    }
}