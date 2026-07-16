package com.example.idolproject.Login

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.idolproject.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etNickname: EditText
    private lateinit var btnCheckNickname: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        bindViews()
        setupListeners()
        observeRegisterUiState()
        observeAuthEvent()
    }

    private fun bindViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etNickname = findViewById(R.id.etNickname)
        btnCheckNickname = findViewById(R.id.btnCheckNickname)
        btnRegister = findViewById(R.id.btnRegister)
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

        etNickname.addTextChangedListener(
            SimpleTextWatcher { nickname ->
                viewModel.onNicknameChanged(nickname)
            }
        )

        btnCheckNickname.setOnClickListener {
            viewModel.checkNickname()
        }

        btnRegister.setOnClickListener {
            viewModel.register()
        }
    }

    private fun observeRegisterUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    bindRegisterUi(uiState)
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
                            Unit
                        }

                        AuthEvent.GoRegister -> {
                            Unit
                        }

                        AuthEvent.FinishRegister -> {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun bindRegisterUi(uiState: RegisterUiState) {
        setNicknameCheckButtonState(uiState.nicknameCheckState)
        setRegisterEnabled(uiState.isRegisterButtonEnabled && !uiState.isLoading)

        btnRegister.text = if (uiState.isLoading) {
            "가입 중..."
        } else {
            "회원가입"
        }
    }

    private fun setNicknameCheckButtonState(state: NicknameCheckState) {
        when (state) {
            NicknameCheckState.IDLE -> {
                btnCheckNickname.isEnabled = true
                btnCheckNickname.text = "중복확인"
            }

            NicknameCheckState.CHECKING -> {
                btnCheckNickname.isEnabled = false
                btnCheckNickname.text = "확인중..."
            }

            NicknameCheckState.AVAILABLE -> {
                btnCheckNickname.isEnabled = false
                btnCheckNickname.text = "사용가능✅"
            }

            NicknameCheckState.TAKEN -> {
                btnCheckNickname.isEnabled = true
                btnCheckNickname.text = "다시확인"
            }
        }
    }

    private fun setRegisterEnabled(enabled: Boolean) {
        btnRegister.isEnabled = enabled
        btnRegister.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}