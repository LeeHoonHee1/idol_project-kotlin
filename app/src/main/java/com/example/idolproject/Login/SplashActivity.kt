package com.example.idolproject.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.idolproject.MainActivity
import com.example.idolproject.UserSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkLoginSession()
    }

    private fun checkLoginSession() {
        val user = auth.currentUser

        if (user == null) {
            goLogin()
            return
        }

        syncUserSession()
    }

    private fun syncUserSession() {
        lifecycleScope.launch {
            val isSuccess = UserSession.syncFromFirestore(this@SplashActivity)

            if (isSuccess) {
                goMain()
            } else {
                clearSessionAndGoLogin()
            }
        }
    }

    private fun clearSessionAndGoLogin() {
        auth.signOut()
        UserSession.clear(this)

        Toast.makeText(
            this,
            "세션 로딩 실패. 다시 로그인 해주세요.",
            Toast.LENGTH_SHORT
        ).show()

        goLogin()
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}