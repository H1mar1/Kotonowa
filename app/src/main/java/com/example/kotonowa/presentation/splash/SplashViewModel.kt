package com.example.kotonowa.presentation.splash

import androidx.lifecycle.ViewModel
import com.example.kotonowa.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * スプラッシュ画面の頭脳。
 *
 * お手本は presentation/auth/signup/SignUpViewModel.kt。
 * ただし今回は待つ処理（suspend）が無いので viewModelScope.launch は使わない。
 * currentUser は関数ではなくプロパティなので、その場ですぐ答えが返る。
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {


    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Checking)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {

        if (authRepository.currentUser != null) {
            _uiState.value = SplashUiState.LoggedIn
        } else {
            _uiState.value = SplashUiState.NotLoggedIn

        }

    }
}
