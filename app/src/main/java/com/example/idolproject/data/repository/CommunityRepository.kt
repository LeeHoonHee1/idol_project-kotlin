package com.example.idolproject.data.repository

import com.example.idolproject.Drawer.Community.ChatRoom
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CommunityRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCol = db.collection("users")
    private val groupChatsCol = db.collection("group_chats")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getMyFavoriteGroup(): CommunityFavoriteGroup? {
        val uid = getCurrentUserId() ?: return null

        val userSnap = usersCol
            .document(uid)
            .get()
            .await()

        val favoriteGroupIdFromList = (userSnap.get("favoriteGroupIds") as? List<*>)
            ?.filterIsInstance<String>()
            ?.firstOrNull()

        val favoriteGroupId = favoriteGroupIdFromList
            ?: userSnap.getString("favoriteGroupId")

        if (favoriteGroupId.isNullOrBlank()) {
            return null
        }

        val favoriteGroupName = userSnap.getString("favoriteGroupName")
            ?: getDisplayGroupName(favoriteGroupId)

        return CommunityFavoriteGroup(
            groupId = favoriteGroupId,
            groupName = favoriteGroupName
        )
    }

    fun observeFavoriteGroupChatRoom(
        groupId: String,
        groupName: String
    ): Flow<ChatRoom> = callbackFlow {
        val uid = getCurrentUserId()
        if (uid == null) {
            close(IllegalStateException("로그인 정보가 없습니다."))
            return@callbackFlow
        }

        val roomRef = groupChatsCol.document(groupId)
        val memberRef = roomRef.collection("members").document(uid)

        var latestRoom = ChatRoom(
            id = groupId,
            groupName = groupName.ifBlank { groupId },
            roomName = "${groupName.ifBlank { groupId }} 팬톡방",
            lastMessage = "팬들과 실시간으로 대화해 보세요.",
            unreadCount = 0,
            imageResId = R.drawable.person_24dp,
            lastMessageAt = 0L
        )

        trySend(latestRoom)

        var unreadListener: ListenerRegistration? = null

        val roomListener = roomRef.addSnapshotListener { roomSnap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (roomSnap == null) return@addSnapshotListener

            latestRoom = latestRoom.copy(
                groupName = roomSnap.getString("groupName")
                    .orEmpty()
                    .ifBlank { latestRoom.groupName },
                roomName = roomSnap.getString("roomName")
                    .orEmpty()
                    .ifBlank { latestRoom.roomName },
                lastMessage = roomSnap.getString("lastMessage")
                    .orEmpty()
                    .ifBlank { latestRoom.lastMessage },
                lastMessageAt = roomSnap.getLong("lastMessageAt") ?: latestRoom.lastMessageAt
            )

            trySend(latestRoom)
        }

        val memberListener = memberRef.addSnapshotListener { memberSnap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val lastReadAt = memberSnap?.getLong("lastReadAt") ?: 0L

            unreadListener?.remove()
            unreadListener = roomRef.collection("messages")
                .whereGreaterThan("timestamp", lastReadAt)
                .addSnapshotListener { msgSnap, msgError ->
                    if (msgError != null) {
                        close(msgError)
                        return@addSnapshotListener
                    }

                    val unreadCount = msgSnap?.documents
                        ?.count { doc ->
                            doc.getString("senderUid").orEmpty() != uid
                        }
                        ?: 0

                    latestRoom = latestRoom.copy(
                        unreadCount = unreadCount.coerceAtMost(99)
                    )

                    trySend(latestRoom)
                }
        }

        awaitClose {
            roomListener.remove()
            memberListener.remove()
            unreadListener?.remove()
        }
    }

    private fun getDisplayGroupName(groupId: String): String {
        return when (groupId) {
            "ive" -> "IVE"
            "newjeans" -> "NewJeans"
            "lesserafim" -> "LE SSERAFIM"
            "aespa" -> "aespa"
            "babymonster" -> "BABYMONSTER"
            else -> groupId
        }
    }
}

data class CommunityFavoriteGroup(
    val groupId: String,
    val groupName: String
)