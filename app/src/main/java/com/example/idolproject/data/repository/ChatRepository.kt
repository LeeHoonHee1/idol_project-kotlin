package com.example.idolproject.data.repository

import com.example.idolproject.Drawer.Community.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCol = db.collection("users")
    private val groupChatsCol = db.collection("group_chats")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getCurrentUserNickname(): String {
        val uid = getCurrentUserId() ?: return "익명"

        val userSnap = usersCol
            .document(uid)
            .get()
            .await()

        return userSnap.getString("nickname")
            .orEmpty()
            .ifBlank { "익명" }
    }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val myUid = getCurrentUserId()

        if (roomId.isBlank()) {
            close(IllegalArgumentException("채팅방 정보가 없습니다."))
            return@callbackFlow
        }

        if (myUid == null) {
            close(IllegalStateException("로그인 정보가 없습니다."))
            return@callbackFlow
        }

        val listener = groupChatsCol
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshots?.documents
                    ?.map { doc ->
                        val senderUid = doc.getString("senderUid").orEmpty()

                        ChatMessage(
                            id = doc.getString("id").orEmpty(),
                            roomId = doc.getString("roomId").orEmpty(),
                            senderUid = senderUid,
                            senderName = doc.getString("senderName").orEmpty(),
                            message = doc.getString("message").orEmpty(),
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isMe = senderUid == myUid
                        )
                    }
                    .orEmpty()

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun sendMessage(
        roomId: String,
        roomName: String,
        text: String
    ) {
        val user = auth.currentUser ?: throw IllegalStateException("로그인 정보가 없습니다.")

        if (roomId.isBlank()) {
            throw IllegalArgumentException("채팅방 정보가 없습니다.")
        }

        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            throw IllegalArgumentException("메시지를 입력해 주세요.")
        }

        val nickname = getCurrentUserNickname()
        val now = System.currentTimeMillis()

        val messageDoc = groupChatsCol
            .document(roomId)
            .collection("messages")
            .document()

        val messageData = hashMapOf(
            "id" to messageDoc.id,
            "roomId" to roomId,
            "senderUid" to user.uid,
            "senderName" to nickname,
            "message" to trimmedText,
            "timestamp" to now
        )

        val roomData = hashMapOf(
            "groupName" to roomName,
            "roomName" to roomName,
            "lastMessage" to trimmedText,
            "lastMessageAt" to now,
            "lastSenderUid" to user.uid,
            "lastSenderName" to nickname
        )

        messageDoc.set(messageData).await()

        groupChatsCol
            .document(roomId)
            .set(roomData, SetOptions.merge())
            .await()
    }

    suspend fun markAsRead(roomId: String) {
        val user = auth.currentUser ?: return

        if (roomId.isBlank()) return

        val nickname = getCurrentUserNickname()

        val memberData = hashMapOf(
            "nickname" to nickname,
            "lastReadAt" to System.currentTimeMillis()
        )

        groupChatsCol
            .document(roomId)
            .collection("members")
            .document(user.uid)
            .set(memberData, SetOptions.merge())
            .await()
    }
}