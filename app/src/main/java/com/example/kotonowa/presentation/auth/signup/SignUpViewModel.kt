package com.example.kotonowa.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.UserRepository
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
    private val userRepository: UserRepository,
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
                .onSuccess {user ->
                    userRepository.saveUser(user)

                    // Step 39-B：確認メールを送る。届いたリンクを押すと、招待を受け取れるようになる。
                    //   失敗しても登録自体は成功しているので、結果は見ない（saveUser と同じ考え方。Step 28）。
                    //   招待一覧の［確認メールを再送］からいつでも送り直せる。
                    //   ⚠️ isSignUpSuccess = true より「前」に呼ぶ（画面が消えると viewModelScope が打ち切られる。Step 28 の ⚠️）
                    // TODO(39-B-1) 誰の：認証の窓口の ／ 何をする：確認メールを送る（39-A で作った関数。引数なし）
                    authRepository.sendEmailVerification()

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
