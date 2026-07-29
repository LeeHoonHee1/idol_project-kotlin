package com.example.idolproject.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.firestore.SetOptions
import com.example.idolproject.domain.policy.NicknamePolicy

class AuthRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthResult {
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Failed("이메일/비밀번호를 입력해 주세요.")
        }

        return runCatching {
            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()

            val uid = result.user?.uid
                ?: return AuthResult.Failed("로그인 정보를 확인하지 못했습니다.")

            ensureUserDoc(
                uid = uid,
                email = email
            )

            AuthResult.Success
        }.getOrElse { throwable ->
            AuthResult.Failed(
                throwable.message ?: "로그인에 실패했습니다."
            )
        }
    }

    private suspend fun ensureUserDoc(
        uid: String,
        email: String
    ) {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()

        if (snap.exists()) return

        val minimalUser = hashMapOf(
            "nickname" to email.substringBefore("@"),
            "role" to "user",
            "points_total" to 0L,
            "level" to 1L,
            "exp" to 0L,
            "badgeId" to "default",
            "photoUrl" to null,
            "statusMessage" to null,
            "favoriteGroupId" to null,
            "favoriteGroupIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        userRef.set(minimalUser).await()
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun checkNicknameAvailable(nickname: String): NicknameCheckResult {
        val key = NicknamePolicy.normalizeNicknameKey(nickname)

        if (!NicknamePolicy.isValidNickname(nickname)) {
            return NicknameCheckResult.Invalid(NicknamePolicy.getInvalidMessage())
        }

        return runCatching {
            val snap = db.collection("nicknames")
                .document(key)
                .get()
                .await()

            if (snap.exists()) {
                NicknameCheckResult.Taken
            } else {
                NicknameCheckResult.Available(key)
            }
        }.getOrElse { throwable ->
            NicknameCheckResult.Failed(
                throwable.message ?: "닉네임 중복확인에 실패했습니다."
            )
        }
    }

    suspend fun register(
        email: String,
        password: String,
        nickname: String,
        checkedNicknameKey: String?,
        isNicknameAvailable: Boolean
    ): RegisterResult {
        val key = NicknamePolicy.normalizeNicknameKey(nickname)

        if (email.isBlank() || password.isBlank() || nickname.isBlank()) {
            return RegisterResult.Failed("모든 항목을 입력해 주세요.")
        }

        if (!NicknamePolicy.isValidNickname(nickname)) {
            return RegisterResult.Failed(NicknamePolicy.getInvalidMessage())
        }

        if (checkedNicknameKey != key || !isNicknameAvailable) {
            return RegisterResult.Failed("닉네임 중복확인을 먼저 해줘")
        }

        return runCatching {
            val authResult = auth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val createdUser = authResult.user
                ?: return RegisterResult.Failed("회원가입 실패: uid 없음")

            try {
                reserveNicknameAndCreateUser(
                    uid = createdUser.uid,
                    nickname = nickname,
                    key = key
                )

                RegisterResult.Success
            } catch (e: Exception) {
                runCatching {
                    createdUser.delete().await()
                }

                auth.signOut()

                if (e.message?.contains("TAKEN") == true) {
                    RegisterResult.Failed("방금 누가 선점했어. 다른 닉네임으로 해줘!")
                } else {
                    RegisterResult.Failed("가입 처리 실패: ${e.message}")
                }
            }
        }.getOrElse { throwable ->
            RegisterResult.Failed(
                throwable.message ?: "회원가입에 실패했습니다."
            )
        }
    }

    private suspend fun reserveNicknameAndCreateUser(
        uid: String,
        nickname: String,
        key: String
    ) {
        val nickRef = db.collection("nicknames").document(key)
        val userRef = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val nickSnap = tx.get(nickRef)
            if (nickSnap.exists()) {
                throw IllegalStateException("TAKEN")
            }

            tx.set(
                nickRef,
                hashMapOf(
                    "uid" to uid,
                    "nickname" to nickname,
                    "createdAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            tx.set(
                userRef,
                hashMapOf(
                    "nickname" to nickname,
                    "nicknameKey" to key,
                    "role" to "user",
                    "points_total" to 0L,
                    "level" to 1L,
                    "exp" to 0L,
                    "badgeId" to "default",
                    "photoUrl" to null,
                    "statusMessage" to null,
                    "favoriteGroupId" to null,
                    "favoriteGroupIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            null
        }.await()
    }
}

sealed interface AuthResult {
    data object Success : AuthResult

    data class Failed(
        val message: String
    ) : AuthResult
}

sealed interface NicknameCheckResult {
    data class Available(
        val key: String
    ) : NicknameCheckResult

    data object Taken : NicknameCheckResult

    data class Invalid(
        val message: String
    ) : NicknameCheckResult

    data class Failed(
        val message: String
    ) : NicknameCheckResult
}

sealed interface RegisterResult {
    data object Success : RegisterResult

    data class Failed(
        val message: String
    ) : RegisterResult
}