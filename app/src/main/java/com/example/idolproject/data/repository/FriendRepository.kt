package com.example.idolproject.data.repository

import com.example.idolproject.UI.Friend.Friend
import com.example.idolproject.UI.Friend.FriendRequestIdUtil
import com.example.idolproject.UI.Friend.FriendRequestItem
import com.example.idolproject.UI.Friend.FriendRequestStatus
import com.example.idolproject.UI.Friend.NicknameKeyUtil
import com.example.idolproject.UI.Friend.SendResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FriendRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCol = db.collection("users")
    private val requestsCol = db.collection("friend_requests")

    private val groupDisplayMap = mapOf(
        "ive" to "IVE",
        "aespa" to "aespa",
        "newjeans" to "NewJeans",
        "lesserafim" to "LE SSERAFIM",
        "babymonster" to "BABYMONSTER"
    )

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun observeFriends(myUid: String): Flow<List<Friend>> = callbackFlow {
        val listener = db.collection("friends")
            .document(myUid)
            .collection("list")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val friendUids = snap?.documents
                    ?.map { it.id }
                    .orEmpty()

                trySend(friendUids)
            }

        awaitClose {
            listener.remove()
        }
    }.flatMapFriendProfiles(myUid)

    fun observePendingRequestCount(myUid: String): Flow<Int> = callbackFlow {
        val listener = requestsCol
            .whereEqualTo("receiverUid", myUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snap?.size() ?: 0)
            }

        awaitClose {
            listener.remove()
        }
    }

    fun observeFriendRequests(myUid: String): Flow<List<FriendRequestItem>> = callbackFlow {
        val listener = requestsCol
            .whereEqualTo("receiverUid", myUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val senderUids = snap?.documents.orEmpty()
                    .mapNotNull { it.getString("senderUid") }

                trySend(senderUids)
            }

        awaitClose {
            listener.remove()
        }
    }.flatMapRequestProfiles(myUid)

    suspend fun searchUserByNickname(input: String): FriendSearchResult {
        val myUid = getCurrentUserId()
            ?: return FriendSearchResult.LoginRequired

        val key = NicknameKeyUtil.normalizeNicknameKey(input)
        val querySnap = usersCol
            .whereEqualTo("nicknameKey", key)
            .limit(1)
            .get()
            .await()

        val targetDoc = querySnap.documents.firstOrNull()
            ?: return FriendSearchResult.NotFound

        val targetUid = targetDoc.id
        val mySnap = usersCol.document(myUid).get().await()

        val isAlreadyFriend = db.collection("friends")
            .document(myUid)
            .collection("list")
            .document(targetUid)
            .get()
            .await()
            .exists()

        val hasPendingRequest = requestsCol
            .document(FriendRequestIdUtil.requestId(myUid, targetUid))
            .get()
            .await()
            .let { snap ->
                snap.exists() && FriendRequestStatus.from(snap.getString("status")) == FriendRequestStatus.PENDING
            }

        val hasReceivedPendingRequest = requestsCol
            .document(FriendRequestIdUtil.requestId(targetUid, myUid))
            .get()
            .await()
            .let { snap ->
                snap.exists() && FriendRequestStatus.from(snap.getString("status")) == FriendRequestStatus.PENDING
            }

        val targetProfile = targetDoc.toFriendSearchProfile(
            myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty(),
            myUid = myUid,
            isAlreadyFriend = isAlreadyFriend,
            hasPendingRequest = hasPendingRequest,
            hasReceivedPendingRequest = hasReceivedPendingRequest
        )

        return FriendSearchResult.Found(targetProfile)
    }

    suspend fun sendFriendRequest(receiverUid: String): SendResult {
        val senderUid = getCurrentUserId()
            ?: return SendResult.Failed("로그인이 필요합니다.")

        if (senderUid == receiverUid) {
            return SendResult.Failed("본인에게는 요청할 수 없습니다.")
        }

        val a = friendDoc(senderUid, receiverUid).get().await().exists()
        val b = friendDoc(receiverUid, senderUid).get().await().exists()

        if (a || b) {
            return SendResult.AlreadyFriends
        }

        val docRef = requestDoc(senderUid, receiverUid)

        return db.runTransaction { tx ->
            val cur = tx.get(docRef)
            val now = FieldValue.serverTimestamp()

            if (cur.exists()) {
                val status = FriendRequestStatus.from(cur.getString("status"))

                when (status) {
                    FriendRequestStatus.PENDING -> {
                        return@runTransaction SendResult.AlreadyPending
                    }

                    FriendRequestStatus.ACCEPTED -> {
                        if (!(a || b)) {
                            tx.update(
                                docRef,
                                mapOf(
                                    "status" to FriendRequestStatus.PENDING.raw,
                                    "checked" to false,
                                    "updatedAt" to now
                                )
                            )
                            return@runTransaction SendResult.Sent
                        }

                        return@runTransaction SendResult.AlreadyFriends
                    }

                    FriendRequestStatus.REJECTED,
                    FriendRequestStatus.CANCELED,
                    FriendRequestStatus.UNFRIENDED -> {
                        tx.update(
                            docRef,
                            mapOf(
                                "status" to FriendRequestStatus.PENDING.raw,
                                "checked" to false,
                                "updatedAt" to now
                            )
                        )
                        return@runTransaction SendResult.Sent
                    }
                }
            } else {
                tx.set(
                    docRef,
                    mapOf(
                        "senderUid" to senderUid,
                        "receiverUid" to receiverUid,
                        "status" to FriendRequestStatus.PENDING.raw,
                        "checked" to false,
                        "createdAt" to now,
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                )

                return@runTransaction SendResult.Sent
            }
        }.await()
    }

    suspend fun acceptRequest(senderUid: String): Boolean {
        val receiverUid = getCurrentUserId() ?: return false
        val reqRef = requestDoc(senderUid, receiverUid)
        val reqSnap = reqRef.get().await()

        if (!reqSnap.exists()) return false

        val status = FriendRequestStatus.from(reqSnap.getString("status"))
        if (status != FriendRequestStatus.PENDING) return false

        reqRef.update(
            mapOf(
                "status" to FriendRequestStatus.ACCEPTED.raw,
                "checked" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val batch = db.batch()

        batch.set(
            friendDoc(receiverUid, senderUid),
            mapOf("createdAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )

        batch.set(
            friendDoc(senderUid, receiverUid),
            mapOf("createdAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )

        batch.commit().await()

        return true
    }

    suspend fun rejectRequest(senderUid: String): Boolean {
        val receiverUid = getCurrentUserId() ?: return false
        val reqRef = requestDoc(senderUid, receiverUid)
        val reqSnap = reqRef.get().await()

        if (!reqSnap.exists()) return false

        val status = FriendRequestStatus.from(reqSnap.getString("status"))
        if (status != FriendRequestStatus.PENDING) return false

        reqRef.update(
            mapOf(
                "status" to FriendRequestStatus.REJECTED.raw,
                "checked" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        return true
    }

    suspend fun markAllPendingChecked() {
        val myUid = getCurrentUserId() ?: return

        val snap = requestsCol
            .whereEqualTo("receiverUid", myUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
            .whereEqualTo("checked", false)
            .get()
            .await()

        if (snap.isEmpty) return

        val batch = db.batch()

        snap.documents.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf(
                    "checked" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
    }

    suspend fun deleteFriend(otherUid: String) {
        val myUid = getCurrentUserId()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val batch = db.batch()
        batch.delete(friendDoc(myUid, otherUid))
        batch.delete(friendDoc(otherUid, myUid))
        batch.commit().await()
    }

    private fun friendDoc(uid: String, friendUid: String) =
        db.collection("friends")
            .document(uid)
            .collection("list")
            .document(friendUid)

    private fun requestDoc(senderUid: String, receiverUid: String) =
        requestsCol.document(FriendRequestIdUtil.requestId(senderUid, receiverUid))

    private fun DocumentSnapshot.toFriend(
        myFavoriteGroupId: String
    ): Friend {
        val level = (getLong("level") ?: 1L).toInt()
        val badgeIdFromDb = getString("badgeId").orEmpty()
        val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
            getBadgeIdByLevel(level)
        } else {
            badgeIdFromDb
        }

        val favoriteGroupId = getString("favoriteGroupId").orEmpty()
        val favoriteGroupName = if (favoriteGroupId.isBlank()) {
            "-"
        } else {
            groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
        }

        return Friend(
            uid = id,
            nickname = getString("nickname") ?: "(알 수 없음)",
            statusMessage = getString("statusMessage") ?: "상태메시지 없음",
            favoriteGroupId = favoriteGroupId,
            favoriteGroupName = favoriteGroupName,
            level = level,
            badgeId = finalBadgeId,
            photoUrl = getString("photoUrl") ?: getString("profileImageUrl"),
            isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                    favoriteGroupId.isNotBlank() &&
                    myFavoriteGroupId == favoriteGroupId
        )
    }

    private fun DocumentSnapshot.toFriendRequestItem(
        myFavoriteGroupId: String
    ): FriendRequestItem {
        val level = (getLong("level") ?: 1L).toInt()
        val badgeIdFromDb = getString("badgeId").orEmpty()
        val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
            getBadgeIdByLevel(level)
        } else {
            badgeIdFromDb
        }

        val favoriteGroupId = getString("favoriteGroupId").orEmpty()
        val favoriteGroupName = if (favoriteGroupId.isBlank()) {
            "-"
        } else {
            groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
        }

        return FriendRequestItem(
            senderUid = id,
            senderNickname = getString("nickname") ?: "(알 수 없음)",
            statusMessage = getString("statusMessage") ?: "상태메시지 없음",
            level = level,
            badgeId = finalBadgeId,
            favoriteGroupId = favoriteGroupId,
            favoriteGroupName = favoriteGroupName,
            photoUrl = getString("photoUrl") ?: getString("profileImageUrl"),
            isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                    favoriteGroupId.isNotBlank() &&
                    myFavoriteGroupId == favoriteGroupId
        )
    }

    private fun DocumentSnapshot.toFriendSearchProfile(
        myFavoriteGroupId: String,
        myUid: String,
        isAlreadyFriend: Boolean,
        hasPendingRequest: Boolean,
        hasReceivedPendingRequest: Boolean
    ): FriendSearchProfile {
        val level = (getLong("level") ?: 1L).toInt()
        val badgeIdFromDb = getString("badgeId").orEmpty()
        val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
            getBadgeIdByLevel(level)
        } else {
            badgeIdFromDb
        }

        val favoriteGroupId = getString("favoriteGroupId").orEmpty()
        val favoriteGroupName = if (favoriteGroupId.isBlank()) {
            "-"
        } else {
            groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
        }

        return FriendSearchProfile(
            uid = id,
            nickname = getString("nickname") ?: "(알 수 없음)",
            statusMessage = getString("statusMessage") ?: "상태메시지 없음",
            level = level,
            badgeId = finalBadgeId,
            favoriteGroupId = favoriteGroupId,
            favoriteGroupName = favoriteGroupName,
            photoUrl = getString("photoUrl") ?: getString("profileImageUrl"),
            isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                    favoriteGroupId.isNotBlank() &&
                    myFavoriteGroupId == favoriteGroupId,
            isMe = id == myUid,
            isAlreadyFriend = isAlreadyFriend,
            hasPendingRequest = hasPendingRequest,
            hasReceivedPendingRequest = hasReceivedPendingRequest
        )
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

    private fun Flow<List<String>>.flatMapFriendProfiles(
        myUid: String
    ): Flow<List<Friend>> = callbackFlow {
        collect { friendUids ->
            if (friendUids.isEmpty()) {
                trySend(emptyList())
                return@collect
            }

            val db = FirebaseFirestore.getInstance()
            val groupDisplayMap = mapOf(
                "ive" to "IVE",
                "aespa" to "aespa",
                "newjeans" to "NewJeans",
                "lesserafim" to "LE SSERAFIM",
                "babymonster" to "BABYMONSTER"
            )

            val mySnap = db.collection("users")
                .document(myUid)
                .get()
                .await()

            val myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty()

            val friends = friendUids.mapNotNull { friendUid ->
                val userSnap = db.collection("users")
                    .document(friendUid)
                    .get()
                    .await()

                if (!userSnap.exists()) {
                    null
                } else {
                    val level = (userSnap.getLong("level") ?: 1L).toInt()
                    val badgeIdFromDb = userSnap.getString("badgeId").orEmpty()
                    val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                        getBadgeIdByLevelForFriendRepository(level)
                    } else {
                        badgeIdFromDb
                    }

                    val favoriteGroupId = userSnap.getString("favoriteGroupId").orEmpty()
                    val favoriteGroupName = if (favoriteGroupId.isBlank()) {
                        "-"
                    } else {
                        groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
                    }

                    Friend(
                        uid = friendUid,
                        nickname = userSnap.getString("nickname") ?: "(알 수 없음)",
                        statusMessage = userSnap.getString("statusMessage") ?: "상태메시지 없음",
                        favoriteGroupId = favoriteGroupId,
                        favoriteGroupName = favoriteGroupName,
                        level = level,
                        badgeId = finalBadgeId,
                        photoUrl = userSnap.getString("photoUrl") ?: userSnap.getString("profileImageUrl"),
                        isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                                favoriteGroupId.isNotBlank() &&
                                myFavoriteGroupId == favoriteGroupId
                    )
                }
            }.sortedWith(
                compareByDescending<Friend> { it.isSameFavorite }
                    .thenByDescending { it.level }
                    .thenBy { it.nickname }
            )

            trySend(friends)
        }

        awaitClose { }
    }

    private fun Flow<List<String>>.flatMapRequestProfiles(
        myUid: String
    ): Flow<List<FriendRequestItem>> = callbackFlow {
        collect { senderUids ->
            if (senderUids.isEmpty()) {
                trySend(emptyList())
                return@collect
            }

            val db = FirebaseFirestore.getInstance()
            val groupDisplayMap = mapOf(
                "ive" to "IVE",
                "aespa" to "aespa",
                "newjeans" to "NewJeans",
                "lesserafim" to "LE SSERAFIM",
                "babymonster" to "BABYMONSTER"
            )

            val mySnap = db.collection("users")
                .document(myUid)
                .get()
                .await()

            val myFavoriteGroupId = mySnap.getString("favoriteGroupId").orEmpty()

            val items = senderUids.mapNotNull { senderUid ->
                val userSnap = db.collection("users")
                    .document(senderUid)
                    .get()
                    .await()

                if (!userSnap.exists()) {
                    null
                } else {
                    val level = (userSnap.getLong("level") ?: 1L).toInt()
                    val badgeIdFromDb = userSnap.getString("badgeId").orEmpty()
                    val finalBadgeId = if (badgeIdFromDb.isBlank() || badgeIdFromDb == "default") {
                        getBadgeIdByLevelForFriendRepository(level)
                    } else {
                        badgeIdFromDb
                    }

                    val favoriteGroupId = userSnap.getString("favoriteGroupId").orEmpty()
                    val favoriteGroupName = if (favoriteGroupId.isBlank()) {
                        "-"
                    } else {
                        groupDisplayMap[favoriteGroupId] ?: favoriteGroupId
                    }

                    FriendRequestItem(
                        senderUid = senderUid,
                        senderNickname = userSnap.getString("nickname") ?: "(알 수 없음)",
                        statusMessage = userSnap.getString("statusMessage") ?: "상태메시지 없음",
                        level = level,
                        badgeId = finalBadgeId,
                        favoriteGroupId = favoriteGroupId,
                        favoriteGroupName = favoriteGroupName,
                        photoUrl = userSnap.getString("photoUrl") ?: userSnap.getString("profileImageUrl"),
                        isSameFavorite = myFavoriteGroupId.isNotBlank() &&
                                favoriteGroupId.isNotBlank() &&
                                myFavoriteGroupId == favoriteGroupId
                    )
                }
            }.sortedWith(
                compareByDescending<FriendRequestItem> { it.isSameFavorite }
                    .thenByDescending { it.level }
                    .thenBy { it.senderNickname }
            )

            trySend(items)
        }

        awaitClose { }
    }

    private fun getBadgeIdByLevelForFriendRepository(level: Int): String {
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
}

data class FriendSearchProfile(
    val uid: String,
    val nickname: String,
    val statusMessage: String,
    val level: Int,
    val badgeId: String,
    val favoriteGroupId: String,
    val favoriteGroupName: String,
    val photoUrl: String?,
    val isSameFavorite: Boolean,
    val isMe: Boolean,
    val isAlreadyFriend: Boolean,
    val hasPendingRequest: Boolean,
    val hasReceivedPendingRequest: Boolean
)

sealed interface FriendSearchResult {
    data object LoginRequired : FriendSearchResult
    data object NotFound : FriendSearchResult
    data class Found(
        val profile: FriendSearchProfile
    ) : FriendSearchResult
}