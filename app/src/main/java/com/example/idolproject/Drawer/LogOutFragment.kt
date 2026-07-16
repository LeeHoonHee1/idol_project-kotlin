package com.example.idolproject.Drawer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.idolproject.Login.LoginActivity
import com.example.idolproject.data.repository.AuthRepository
import com.example.idolproject.data.repository.FcmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LogOutFragment : Fragment() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var fcmRepository: FcmRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(
            requireContext(),
            "로그아웃 실행",
            Toast.LENGTH_SHORT
        ).show()

        clearFcmTokenAndLogout()
    }

    private fun clearFcmTokenAndLogout() {
        lifecycleScope.launch {
            fcmRepository.clearCurrentUserFcmToken()

            authRepository.logout()
            moveToLogin()
        }
    }

    private fun moveToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        startActivity(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // onCreate에서 이미 이동 처리
    }
}