package com.example.kotonowa.presentation.auth.passwordreset

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
 * パスワードリセット画面の頭脳。
 *
 * お手本は presentation/auth/signup/SignUpViewModel.kt。
 * ログインやサインアップと違い、成功しても画面は移動しない。
 * 「送信しました」と表示するだけなので、成功フラグは画面遷移には使わない。
 */
@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // TODO: 内部用の _uiState（MutableStateFlow）と、公開用の uiState（StateFlow）を用意する
    //  お手本は SignUpViewModel.kt:26-27

    fun onEmailChange(value: String) {
        // TODO: uiState の email を新しい値に更新し、errorMessage は null に戻す
        //  お手本は SignUpViewModel.kt:29-31
    }

    fun sendResetEmail() {
        // TODO: SignUpViewModel.kt:45-68（signUp）とほぼ同じ流れ
        //  1. 今の状態を取り出し、canSubmit が false なら何もせず return する
        //  2. viewModelScope.launch { } の中で、まず isLoading = true / errorMessage = null にする
        //  3. authRepository.sendPasswordResetEmail(メールアドレス) を呼ぶ
        //     メールアドレスは前後の空白を消すため .trim() を付ける
        //  4. .onSuccess { } → isLoading = false, isEmailSent = true
        //  5. .onFailure { error -> } → isLoading = false, errorMessage にエラー文を入れる
    }
}
