package com.example.kotonowa.presentation.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * 起動直後に一瞬だけ出る、ログイン済みかどうかを判定するための画面。
 *
 * 判定が終わったら呼び鈴を鳴らすだけで、自分がどこへ飛ぶかは知らない。
 * 行き先を決めるのは KotonowaNavHost。
 *
 * @param onLoggedIn ログイン済みだったときに呼ばれる呼び鈴。
 * @param onNotLoggedIn ログインしていなかったときに呼ばれる呼び鈴。
 */
@Composable
fun SplashScreen(
    onLoggedIn: () -> Unit,
    onNotLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {

        when (uiState) {
            SplashUiState.Checking -> Unit
            SplashUiState.LoggedIn -> onLoggedIn()
            SplashUiState.NotLoggedIn -> onNotLoggedIn()
        }


    }

    SplashContent(modifier = modifier)
}

@Composable
private fun SplashContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Text(
            text = "kotonowa",
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashContentPreview() {
    KotonowaTheme {
        SplashContent()
    }
}
