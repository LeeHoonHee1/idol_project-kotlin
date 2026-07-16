package com.example.idolproject.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.idolproject.MainActivity
import com.example.idolproject.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        bindViews()
        setupListeners()
        observeLoginUiState()
        observeAuthEvent()
    }

    private fun bindViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoRegister = findViewById(R.id.tvGoRegister)
    }

    private fun setupListeners() {
        etEmail.addTextChangedListener(
            SimpleTextWatcher { email ->
                viewModel.onEmailChanged(email)
            }
        )

        etPassword.addTextChangedListener(
            SimpleTextWatcher { password ->
                viewModel.onPasswordChanged(password)
            }
        )

        btnLogin.setOnClickListener {
            viewModel.login()
        }

        tvGoRegister.setOnClickListener {
            viewModel.goRegister()
        }
    }

    private fun observeLoginUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindLoginUi(uiState)
                }
            }
        }
    }

    private fun observeAuthEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is AuthEvent.ShowToast -> {
                            toast(event.message)
                        }

                        AuthEvent.GoMain -> {
                            goMain()
                        }

                        AuthEvent.GoRegister -> {
                            goRegister()
                        }

                        AuthEvent.FinishRegister -> {
                            Unit
                        }
                    }
                }
            }
        }
    }

    private fun bindLoginUi(uiState: LoginUiState) {
        btnLogin.isEnabled = uiState.isLoginButtonEnabled

        btnLogin.text = if (uiState.isLoading) {
            "로그인 중..."
        } else {
            "로그인"
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}