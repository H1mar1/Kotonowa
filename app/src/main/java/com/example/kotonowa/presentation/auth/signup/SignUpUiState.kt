package com.example.kotonowa.presentation.auth.signup

/**
 * サインアップ画面の「今の状態」をまとめて表したもの。
 *
 * お手本は presentation/auth/login/LoginUiState.kt。
 * ほとんど同じだが、パスワード確認欄(passwordConfirm)が増えているのが違い。
 */
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpSuccess: Boolean = false,
) {
    /** サインアップボタンを押せる状態かどうか。 */
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && password == passwordConfirm && !isLoading
}
