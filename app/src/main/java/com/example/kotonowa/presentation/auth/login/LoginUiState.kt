package com.example.kotonowa.presentation.auth.login

/**
 * ログイン画面の「今の状態」をまとめて表したもの。
 *
 * 入力中の文字・読み込み中かどうか・エラー文言などを1つの箱に入れておくと、
 * 画面はこの箱を見るだけで描画でき、状態の食い違いが起きにくくなる。
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
) {
    /** ログインボタンを押せる状態かどうか。 */
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}
