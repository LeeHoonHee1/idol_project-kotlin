package com.example.idolproject.FCM

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.idolproject.MainActivity
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Context
import com.example.idolproject.Drawer.Community.GroupChatActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val PREF_CHAT_STATE = "chat_state"
    private val KEY_OPEN_ROOM_ID = "current_open_chat_room_id"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "new token = $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "message received: ${message.data}")

        val title = message.data["title"] ?: "새 메시지"
        val body = message.data["body"] ?: "채팅 메시지가 도착했습니다."
        val groupId = message.data["groupId"]
        val roomName = message.data["roomName"]

        val currentOpenRoomId = getSharedPreferences(PREF_CHAT_STATE, Context.MODE_PRIVATE)
            .getString(KEY_OPEN_ROOM_ID, null)

        if (!groupId.isNullOrBlank() && groupId == currentOpenRoomId) {
            Log.d("FCM", "현재 보고 있는 채팅방이라 푸시 생략: $groupId")
            return
        }

        showNotification(title, body, groupId, roomName)
    }

    private fun showNotification(
        title: String,
        body: String,
        groupId: String?,
        roomName: String?
    ) {
        val intent = GroupChatActivity.newIntent(
            this,
            groupId ?: return,
            roomName ?: "${groupId} 오픈채팅"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            groupId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "chat_messages")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } else {
            Log.w("FCM", "POST_NOTIFICATIONS permission not granted")
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "token saved")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "token save failed", e)
            }
    }
}