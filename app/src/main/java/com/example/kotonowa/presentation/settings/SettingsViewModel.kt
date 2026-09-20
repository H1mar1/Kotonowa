package com.example.kotonowa.presentation.settings

import androidx.lifecycle.ViewModel
import com.example.kotonowa.domain.model.User
import com.example.kotonowa.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 設定画面の頭脳。
 *
 * 表示するのはログイン中のユーザー情報だけで、画面が開いている間は変わらない。
 * だから StateFlow（§2-(58)）は使わず、作られたときに 1 回読むだけにしている。
 * 「変わるか変わらないか」で道具を選ぶ。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** ログイン中のユーザー。ログインしていなければ null。 */

    val user: User? = authRepository.currentUser

    /** ログアウトする。画面遷移は知らない（Step 22 と同じ分担）。 */
    fun logout() {
        authRepository.logout()
    }
}
