package com.example.idolproject.Login

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.idolproject.MainActivity
import com.example.idolproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw = etPassword.text.toString().trim()

            if (email.isEmpty() || pw.isEmpty()) {
                toast("이메일/비번 입력")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pw)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    ensureUserDoc(uid, email)
                }
                .addOnFailureListener { e ->
                    toast("로그인 실패: ${e.message}")
                }
        }
    }

    private fun ensureUserDoc(uid: String, email: String) {
        val ref = db.collection("users").document(uid)
        ref.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    goMain()
                } else {
                    val minimal = hashMapOf(
                        "nickname" to email.substringBefore("@"),
                        "role" to "user",
                        "points_total" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    ref.set(minimal)
                        .addOnSuccessListener { goMain() }
                        .addOnFailureListener { e -> toast("users 생성 실패: ${e.message}") }
                }
            }
            .addOnFailureListener { e -> toast("users 조회 실패: ${e.message}") }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}