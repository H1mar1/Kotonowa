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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PasswordResetContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSendClick = viewModel::sendResetEmail,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
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
        Text(
            text = "パスワードの再設定",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "登録したメールアドレスに再設定用のリンクを送ります",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("メールアドレス") },
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = uiState.errorMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()

            )
        }

        if (uiState.isEmailSent) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "メールを送信しました",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))


        Button(
            onClick = onSendClick,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("送信")
            }
        }
        // TODO: 「ログイン画面に戻る」の TextButton
        //  onClick に onNavigateBack を渡す（カッコは付けない）

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ログイン画面に戻る")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetContentPreview() {
    KotonowaTheme {
        // TODO: PasswordResetContent(...) を呼ぶ
        //  uiState には PasswordResetUiState(email = "kotonowa@example.com") など、
        //  確認したい状態を直接書く。呼び鈴はすべて {} でよい

        PasswordResetContent(
            uiState = PasswordResetUiState(
                email = "kotonowa@example.com",
            ),
            onEmailChange = {},
            onSendClick = {},
            onNavigateBack = {},
        )
    }
}
