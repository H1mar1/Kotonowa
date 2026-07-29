package com.example.kotonowa.presentation.auth.passwordreset


/**
 * パスワードリセット画面の「今の状態」をまとめて表したもの。
 *
 * お手本は presentation/auth/signup/SignUpUiState.kt。
 * パスワード欄が無いぶん、項目は少なくなる。
 */
data class PasswordResetUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEmailSent: Boolean = false,


    ) {

    val canSubmit: Boolean
        get() = email.isNotBlank() && !isLoading
}
