package com.example.kotonowa.presentation.auth.login

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
 * ログイン画面の頭脳。
 *
 * 画面（Composable）は「見た目を描くこと」だけを担当し、
 * 入力の保持・ログイン処理の呼び出し・エラーの判断はすべてここが行う。
 * ViewModel は画面の回転などで作り直されないため、入力内容が消えない。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // 外からは書き換えられないように、内部用(_uiState)と公開用(uiState)を分けている
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val current = _uiState.value
        if (!current.canSubmit) return

        // launch = 時間のかかる処理を、画面を固めずに裏で走らせる
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.login(current.email.trim(), current.password)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, isLoginSuccess = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "ログインに失敗しました",
                        )
                    }
                }
        }
    }
}
