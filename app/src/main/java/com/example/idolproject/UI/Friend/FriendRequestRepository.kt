package com.example.idolproject.UI.Friend

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FriendRequestRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCol = db.collection("users")
    private val requestsCol = db.collection("friend_requests")

    suspend fun findUserUidByNicknameExact(nicknameInput: String): String? {
        val key = NicknameKeyUtil.normalizeNicknameKey(nicknameInput)
        val snap = usersCol.whereEqualTo("nicknameKey", key).limit(1).get().await()
        return snap.documents.firstOrNull()?.id
    }

    private fun friendsDoc(uid: String, friendUid: String) =
        db.collection("friends").document(uid).collection("list").document(friendUid)

    private fun requestDoc(senderUid: String, receiverUid: String) =
        requestsCol.document(FriendRequestIdUtil.requestId(senderUid, receiverUid))

    suspend fun sendRequest(senderUid: String, receiverUid: String): SendResult {
        if (senderUid == receiverUid) return SendResult.Failed("본인에게는 요청할 수 없습니다.")

        // 이미 친구인지 체크
        val a = friendsDoc(senderUid, receiverUid).get().await().exists()
        val b = friendsDoc(receiverUid, senderUid).get().await().exists()
        android.util.Log.d("FRIEND_DEBUG", "friendExists A->B=$a, B->A=$b, sender=$senderUid receiver=$receiverUid")
        if (a || b) return SendResult.AlreadyFriends

        val docRef = requestDoc(senderUid, receiverUid)

        return db.runTransaction { tx ->
            val cur = tx.get(docRef)

            android.util.Log.d(
                "FRIEND_DEBUG",
                "requestId=${docRef.id}, exists=${cur.exists()}, status=${cur.getString("status")}"
            )

            val now = FieldValue.serverTimestamp()

            if (cur.exists()) {
                val status = FriendRequestStatus.from(cur.getString("status"))
                when (status) {
                    FriendRequestStatus.PENDING -> {
                        return@runTransaction SendResult.AlreadyPending
                    }
                    FriendRequestStatus.ACCEPTED -> {
                        // ✅ accepted인데 friends가 실제로 없으면 "유령 accepted" 상태
                        // -> pending으로 복구하고 재요청 허용
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

                        // friends가 있으면 진짜 친구
                        return@runTransaction SendResult.AlreadyFriends
                    }
                    FriendRequestStatus.REJECTED,
                    FriendRequestStatus.CANCELED,
                    FriendRequestStatus.UNFRIENDED -> {
                        // ✅ 재요청 허용: pending으로 갱신
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
                    SetOptions.merge() // <- 아래 설명 보고 merge 제거해도 됨
                )
                return@runTransaction SendResult.Sent
            }
        }.await()
    }

    suspend fun cancelRequest(senderUid: String, receiverUid: String): Boolean {
        val docRef = requestDoc(senderUid, receiverUid)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val status = FriendRequestStatus.from(snap.getString("status"))
        if (status != FriendRequestStatus.PENDING) return false

        docRef.update(
            mapOf(
                "status" to FriendRequestStatus.CANCELED.raw,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return true
    }

    suspend fun acceptRequest(senderUid: String, receiverUid: String): Boolean {
        val reqRef = requestDoc(senderUid, receiverUid)
        val reqSnap = reqRef.get().await()
        if (!reqSnap.exists()) return false

        val status = FriendRequestStatus.from(reqSnap.getString("status"))
        if (status != FriendRequestStatus.PENDING) return false

        val me = receiverUid
        val other = senderUid

        // ✅ 1단계: 요청 문서만 먼저 accepted로 (Rules가 여기까지는 허용해야 함)
        reqRef.update(
            mapOf(
                "status" to FriendRequestStatus.ACCEPTED.raw,
                "checked" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // ✅ 2단계: 그 다음 friends 양방향 생성
        val batch = db.batch()
        batch.set(
            friendsDoc(me, other),
            mapOf("createdAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.set(
            friendsDoc(other, me),
            mapOf("createdAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()

        return true
    }

    suspend fun rejectRequest(senderUid: String, receiverUid: String): Boolean {
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

    fun listenUncheckedPendingCount(
        myUid: String,
        onChanged: (Int) -> Unit
    ): ListenerRegistration {
        return requestsCol
            .whereEqualTo("receiverUid", myUid)
            .whereEqualTo("status", FriendRequestStatus.PENDING.raw)
            .whereEqualTo("checked", false)
            .addSnapshotListener { snap, _ ->
                onChanged(snap?.size() ?: 0)
            }
    }
}

sealed class SendResult {
    data object Sent : SendResult()
    data object AlreadyPending : SendResult()
    data object AlreadyFriends : SendResult()
    data class Failed(val message: String) : SendResult()
}