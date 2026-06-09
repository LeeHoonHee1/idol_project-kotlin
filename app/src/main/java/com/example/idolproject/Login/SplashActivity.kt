package com.example.idolproject.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.idolproject.MainActivity
import com.example.idolproject.UserSession
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // ✅ 로그인 상태면: users/{uid} 읽어서 세션 캐싱 후 메인으로
            UserSession.syncFromFirestore(this) { ok ->
                if (!ok) {
                    // 문서가 없거나 읽기 실패 시(이상상황) 로그아웃 후 로그인으로 보내는 게 안전
                    FirebaseAuth.getInstance().signOut()
                    UserSession.clear(this)
                    Toast.makeText(this, "세션 로딩 실패. 다시 로그인 해주세요.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
        } else {
            // 로그인 안됨 → 로그인 화면으로
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}