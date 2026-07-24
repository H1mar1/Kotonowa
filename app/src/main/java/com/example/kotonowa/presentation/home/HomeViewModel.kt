package com.example.kotonowa.presentation.home

import androidx.lifecycle.ViewModel
import com.example.kotonowa.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ホーム画面の頭脳。
 *
 * 現時点ではログインが成功したことを確認するための仮実装。
 * Phase2 でカレンダーと予定/タスクの表示を担うようになる。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val userEmail: String? = authRepository.currentUser?.email

    fun logout() {
        authRepository.logout()
    }
}
