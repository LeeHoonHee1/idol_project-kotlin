package com.example.idolproject.Login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RegisterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var lastCheckedKey: String? = null
    private var isNicknameAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etNickname = findViewById<EditText>(R.id.etNickname)
        val btnCheckNickname = findViewById<Button>(R.id.btnCheckNickname)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        // ✅ 가입 버튼은 기본 비활성화
        btnRegister.isEnabled = false
        btnRegister.alpha = 0.5f

        setNicknameCheckButtonState(btnCheckNickname, State.IDLE)

        // 닉네임 변경 → 중복확인 상태 초기화 + 가입 버튼 비활성화
        etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isNicknameAvailable = false
                lastCheckedKey = null
                setNicknameCheckButtonState(btnCheckNickname, State.IDLE)
                setRegisterEnabled(btnRegister, false)
            }
        })

        btnCheckNickname.setOnClickListener {
            val nickname = etNickname.text.toString().trim()
            val key = normalizeNicknameKey(nickname)

            if (!isValidNickname(nickname)) {
                toast("닉네임은 2~12자, 공백 없이 입력해줘")
                return@setOnClickListener
            }

            setNicknameCheckButtonState(btnCheckNickname, State.CHECKING)
            setRegisterEnabled(btnRegister, false)

            db.collection("nicknames").document(key).get()
                .addOnSuccessListener { snap ->
                    lastCheckedKey = key
                    if (snap.exists()) {
                        isNicknameAvailable = false
                        setNicknameCheckButtonState(btnCheckNickname, State.TAKEN)
                        setRegisterEnabled(btnRegister, false)
                        toast("이미 사용 중인 닉네임이야")
                    } else {
                        isNicknameAvailable = true
                        setNicknameCheckButtonState(btnCheckNickname, State.AVAILABLE)
                        // ✅ 사용가능이면 가입 버튼 활성화
                        setRegisterEnabled(btnRegister, true)
                        toast("사용 가능한 닉네임이야 ✅")
                    }
                }
                .addOnFailureListener { e ->
                    isNicknameAvailable = false
                    lastCheckedKey = null
                    setNicknameCheckButtonState(btnCheckNickname, State.IDLE)
                    setRegisterEnabled(btnRegister, false)
                    toast("중복확인 실패: ${e.message}")
                }
        }

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw = etPassword.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val key = normalizeNicknameKey(nickname)

            if (email.isEmpty() || pw.isEmpty() || nickname.isEmpty()) {
                toast("모든 항목 입력")
                return@setOnClickListener
            }
            if (!isValidNickname(nickname)) {
                toast("닉네임은 2~12자, 공백 없이 입력해줘")
                return@setOnClickListener
            }
            if (lastCheckedKey != key || !isNicknameAvailable) {
                toast("닉네임 중복확인을 먼저 해줘")
                setRegisterEnabled(btnRegister, false)
                return@setOnClickListener
            }

            setRegisterEnabled(btnRegister, false)

            auth.createUserWithEmailAndPassword(email, pw)
                .addOnSuccessListener { result ->
                    val createdUser = result.user
                    val uid = createdUser?.uid

                    if (uid.isNullOrBlank()) {
                        toast("회원가입 실패: uid 없음")
                        setRegisterEnabled(btnRegister, true)
                        return@addOnSuccessListener
                    }

                    reserveNicknameAndCreateUser(
                        uid = uid,
                        nickname = nickname,
                        key = key,
                        onSuccess = {
                            toast("회원가입 완료")
                            finish()
                        },
                        onFailure = { msg ->
                            // ✅ Firestore 트랜잭션 실패 시: Auth 계정 삭제(유령 계정 방지)
                            createdUser.delete()
                                .addOnCompleteListener {
                                    // 삭제 성공/실패와 무관하게 로그아웃 처리
                                    auth.signOut()
                                    toast(msg)
                                    // 실패 후 다시 시도할 수 있게 버튼 상태 복구
                                    setRegisterEnabled(btnRegister, isNicknameAvailable && lastCheckedKey == key)
                                }
                        }
                    )
                }
                .addOnFailureListener { e ->
                    toast("회원가입 실패: ${e.message}")
                    setRegisterEnabled(btnRegister, isNicknameAvailable && lastCheckedKey == key)
                }
        }
    }

    private fun reserveNicknameAndCreateUser(
        uid: String,
        nickname: String,
        key: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val nickRef = db.collection("nicknames").document(key)
        val userRef = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val nickSnap = tx.get(nickRef)
            if (nickSnap.exists()) throw IllegalStateException("TAKEN")

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
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            null
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            if (e.message?.contains("TAKEN") == true) {
                onFailure("방금 누가 선점했어. 다른 닉네임으로 해줘!")
            } else {
                onFailure("가입 처리 실패: ${e.message}")
            }
        }
    }

    // --- UI helpers ---
    private enum class State { IDLE, CHECKING, AVAILABLE, TAKEN }

    private fun setNicknameCheckButtonState(btn: Button, state: State) {
        when (state) {
            State.IDLE -> {
                btn.isEnabled = true
                btn.text = "중복확인"
            }
            State.CHECKING -> {
                btn.isEnabled = false
                btn.text = "확인중..."
            }
            State.AVAILABLE -> {
                btn.isEnabled = false
                btn.text = "사용가능✅"
            }
            State.TAKEN -> {
                btn.isEnabled = true
                btn.text = "다시확인"
            }
        }
    }

    private fun setRegisterEnabled(btn: Button, enabled: Boolean) {
        btn.isEnabled = enabled
        btn.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun isValidNickname(nickname: String): Boolean {
        val n = nickname.trim()
        if (n.length !in 2..12) return false
        if (n.contains(" ")) return false
        return true
    }

    private fun normalizeNicknameKey(nickname: String): String {
        return nickname.trim()
            .lowercase()
            .replace("\\s+".toRegex(), "")
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}