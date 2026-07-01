package com.example.idolproject.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MyPageRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun observeMyProfile(uid: String): Flow<MyPageUserProfile> = callbackFlow {
        val listener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snap == null || !snap.exists()) {
                    close(IllegalStateException("유저 문서가 없습니다."))
                    return@addSnapshotListener
                }

                val profile = MyPageUserProfile(
                    uid = snap.id,
                    nickname = snap.getString("nickname") ?: "닉네임 없음",
                    statusMessage = snap.getString("statusMessage")
                        ?: "상태메시지를 입력해 주세요.",
                    level = (snap.getLong("level") ?: 1L).toInt(),
                    exp = (snap.getLong("exp") ?: 0L).toInt(),
                    badgeId = snap.getString("badgeId").orEmpty(),
                    favoriteGroupId = snap.getString("favoriteGroupId").orEmpty()
                )

                trySend(profile)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun syncBadgeIdIfNeeded(
        uid: String,
        currentBadgeId: String,
        correctBadgeId: String
    ) {
        if (currentBadgeId == correctBadgeId) return

        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "badgeId" to correctBadgeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    suspend fun changeNickname(
        uid: String,
        newNickname: String,
        newKey: String
    ): NicknameChangeResult {
        val userRef = db.collection("users").document(uid)
        val newNickRef = db.collection("nicknames").document(newKey)

        return db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val oldKey = userSnap.getString("nicknameKey")?.trim().orEmpty()

            if (oldKey == newKey) {
                return@runTransaction NicknameChangeResult.SAME
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

            NicknameChangeResult.CHANGED
        }.await()
    }

    suspend fun saveFavoriteGroup(
        uid: String,
        newGroupId: String
    ): FavoriteGroupSaveResult {
        val userRef = db.collection("users").document(uid)

        return db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            val oldGroupId = userSnap.getString("favoriteGroupId").orEmpty()

            if (oldGroupId == newGroupId) {
                return@runTransaction FavoriteGroupSaveResult.SAME
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

            FavoriteGroupSaveResult.CHANGED
        }.await()
    }
}

data class MyPageUserProfile(
    val uid: String,
    val nickname: String,
    val statusMessage: String,
    val level: Int,
    val exp: Int,
    val badgeId: String,
    val favoriteGroupId: String
)

enum class NicknameChangeResult {
    SAME,
    CHANGED
}

enum class FavoriteGroupSaveResult {
    SAME,
    CHANGED
}