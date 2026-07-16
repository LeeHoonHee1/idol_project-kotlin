package com.example.idolproject.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.firestore.FieldValue

class FcmRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    suspend fun updateCurrentFcmToken(): FcmTokenUpdateResult {
        if (auth.currentUser == null) {
            return FcmTokenUpdateResult.NotLoggedIn
        }

        return runCatching {
            val token = messaging.token.await()

            Log.d("FCM", "current token = $token")

            saveTokenToCurrentUser(token)
        }.getOrElse { throwable ->
            Log.e("FCM", "update token failed", throwable)

            FcmTokenUpdateResult.Failed(
                throwable.message ?: "FCM 토큰 업데이트에 실패했습니다."
            )
        }
    }

    suspend fun saveTokenToCurrentUser(token: String): FcmTokenUpdateResult {
        val user = auth.currentUser
            ?: return FcmTokenUpdateResult.NotLoggedIn

        return runCatching {
            db.collection("users")
                .document(user.uid)
                .update("fcmToken", token)
                .await()

            Log.d("FCM", "token saved")

            FcmTokenUpdateResult.Success
        }.getOrElse { throwable ->
            Log.e("FCM", "token save failed", throwable)

            FcmTokenUpdateResult.Failed(
                throwable.message ?: "FCM 토큰 저장에 실패했습니다."
            )
        }
    }

    suspend fun clearCurrentUserFcmToken(): FcmTokenUpdateResult {
        val user = auth.currentUser
            ?: return FcmTokenUpdateResult.NotLoggedIn

        return runCatching {
            db.collection("users")
                .document(user.uid)
                .update("fcmToken", FieldValue.delete())
                .await()

            Log.d("FCM", "token cleared")

            FcmTokenUpdateResult.Success
        }.getOrElse { throwable ->
            Log.e("FCM", "token clear failed", throwable)

            FcmTokenUpdateResult.Failed(
                throwable.message ?: "FCM 토큰 삭제에 실패했습니다."
            )
        }
    }
}

sealed interface FcmTokenUpdateResult {
    data object Success : FcmTokenUpdateResult

    data object NotLoggedIn : FcmTokenUpdateResult

    data class Failed(
        val message: String
    ) : FcmTokenUpdateResult
}