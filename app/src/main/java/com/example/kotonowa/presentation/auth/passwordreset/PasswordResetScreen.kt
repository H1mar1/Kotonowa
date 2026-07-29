package com.example.kotonowa.presentation.auth.passwordreset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * パスワードリセット画面。
 *
 * お手本は presentation/auth/signup/SignUpScreen.kt。
 * ただし成功しても画面は移動しないので、LaunchedEffect は要らない。
 *
 * @param onNavigateBack 「ログイン画面に戻る」を押したときに呼ばれる呼び鈴。
 */
@Composable
fun PasswordResetScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    // TODO: collectAsStateWithLifecycle() で viewModel.uiState を受け取る
    //  お手本は SignUpScreen.kt:54

    // TODO: PasswordResetContent(...) を呼び、下の引数をすべて渡す
    //  ViewModel に頼む仕事は viewModel::関数名、
    //  画面移動の呼び鈴（onNavigateBack）は素通しで渡す
}

@Composable
private fun PasswordResetContent(
    uiState: PasswordResetUiState,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // TODO: 見出しの Text（例：「パスワードの再設定」）

        // TODO: 説明文の Text
        //  例：「登録したメールアドレスに再設定用のリンクを送ります」

        // TODO: メールアドレスの OutlinedTextField
        //  お手本は SignUpScreen.kt:100-112
        //  keyboardOptions の imeAction は、入力欄が1つだけなので Next ではなく Done

        // TODO: uiState.errorMessage が null でないときだけ、赤い文字でエラーを表示
        //  お手本は SignUpScreen.kt:166-174

        // TODO: uiState.isEmailSent が true のときだけ「メールを送信しました」を表示
        //  errorMessage の表示と同じ形の if 文でよい

        // TODO: 送信ボタン（Button）
        //  お手本は SignUpScreen.kt:178-194
        //  enabled は uiState.canSubmit、通信中は CircularProgressIndicator を出す

        // TODO: 「ログイン画面に戻る」の TextButton
        //  onClick に onNavigateBack を渡す（カッコは付けない）
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetContentPreview() {
    KotonowaTheme {
        // TODO: PasswordResetContent(...) を呼ぶ
        //  uiState には PasswordResetUiState(email = "kotonowa@example.com") など、
        //  確認したい状態を直接書く。呼び鈴はすべて {} でよい
    }
}
