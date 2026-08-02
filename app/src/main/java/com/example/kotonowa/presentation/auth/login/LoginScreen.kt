package com.example.kotonowa.presentation.auth.login

import android.app.Activity
import com.example.kotonowa.R
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.ui.theme.KotonowaTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.oAuthCredential
import kotlinx.coroutines.launch

/**
 * ログイン画面。
 *
 * ViewModel から状態を受け取り、ユーザーの操作を ViewModel に伝えるだけの役割。
 * 実際の見た目は [LoginContent] が描く（ViewModel を持たないので Preview で確認できる）。
 *
 * @param onNavigateToPasswordReset 「パスワードリセット画面へ」を押したときに呼ばれる呼び鈴。
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToPasswordReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    // ログインが成功した瞬間に一度だけ画面遷移を呼ぶ
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) onLoginSuccess()
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onLoginClick = viewModel::login,
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToPasswordReset = onNavigateToPasswordReset,
        onGoogleLoginClick = {
            scope.launch {
//TODO
                val googleOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)

                    //「Googleアカウントを1つ選ばせてください」という注文書
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()

                //注文書を封筒に入れる
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()

                //資格情報の担当者を呼び出す
                val credentialManager = CredentialManager.create(context)

                //封筒を渡して、ユーザーが選ぶまで待つ
                val result = credentialManager.getCredential(
                    context = activity,
                    request = request,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onNavigateToPasswordReset: () -> Unit,
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
            text = "Kotonowa",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "ことのわ にログイン",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("メールアドレス") },
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = uiState.errorMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("パスワード") },
            singleLine = true,
            enabled = !uiState.isLoading,
            isError = uiState.errorMessage != null,
            visualTransformation = if (uiState.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                TextButton(onClick = onTogglePasswordVisibility) {
                    Text(if (uiState.isPasswordVisible) "隠す" else "表示")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onLoginClick,
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
                Text("ログイン")
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoogleLoginClick,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Googleでログイン")
        }



        TextButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("サインアップ画面へ")
        }

        TextButton(
            onClick = onNavigateToPasswordReset,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("パスワードリセット画面へ")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    KotonowaTheme {
        LoginContent(
            uiState = LoginUiState(email = "kotonowa@example.com", password = "password"),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onNavigateToSignUp = {},
            onNavigateToPasswordReset = {},
            onGoogleLoginClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentErrorPreview() {
    KotonowaTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "kotonowa@example.com",
                password = "wrong",
                errorMessage = "メールアドレスまたはパスワードが正しくありません",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onNavigateToSignUp = {},
            onNavigateToPasswordReset = {},
            onGoogleLoginClick = {},
        )
    }
}
