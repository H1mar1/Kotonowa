package com.example.kotonowa.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * サインアップ画面の頭脳。
 *
 * お手本は presentation/auth/login/LoginViewModel.kt。
 * ログインとの違いは、パスワード確認欄(passwordConfirm)の入力を受け取ること。
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // 外からは書き換えられないように、内部用(_uiState)と公開用(uiState)を分けている
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onPasswordConfirmChange(value: String) {
        _uiState.update { it.copy(passwordConfirm = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signUp() {
        val current = _uiState.value
        if (!current.canSubmit) return

        // launch = 時間のかかる処理を、画面を固めずに裏で走らせる
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.signUp(current.email.trim(), current.password)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, isSignUpSuccess = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "サインアップに失敗しました",
                        )
                    }
                }
        }
    }
}
