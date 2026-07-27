package com.example.kotonowa.presentation.auth.signup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * サインアップ（新規登録）画面。
 *
 * お手本は presentation/auth/login/LoginScreen.kt。
 * 役割はログイン画面とまったく同じで、
 *   - ViewModel から状態(uiState)を受け取って
 *   - ユーザーの操作を ViewModel に伝える
 * だけ。実際の見た目は [SignUpContent] が描く。
 *
 * @param onSignUpSuccess 登録に成功したときに呼ばれる。画面遷移は NavHost 側が担当する。
 * @param onNavigateToLogin 「すでにアカウントをお持ちの方」を押したときに呼ばれる。
 */
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSignUpSuccess) {
        if (uiState.isSignUpSuccess) onSignUpSuccess()
    }

    SignUpContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSignUpClick = viewModel::signUp,
        onNavigateToLogin = onNavigateToLogin,
        modifier = modifier,
    )
}

@Composable
private fun SignUpContent(
    uiState: SignUpUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignUpClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
            text = "kotonowa",
            style = MaterialTheme.typography.headlineLarge,
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
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

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
                imeAction = ImeAction.Next,
            ),
            trailingIcon = {
                TextButton(onClick = onTogglePasswordVisibility) {
                    Text(if (uiState.isPasswordVisible) "隠す" else "表示")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.passwordConfirm,
            onValueChange = onPasswordConfirmChange,
            label = { Text("パスワード確認") },
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
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSignUpClick,
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
                Text("登録")
            }


        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ログイン画面に戻る")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpContentPreview() {
    KotonowaTheme {
        SignUpContent(
            uiState = SignUpUiState(
                email = "kotonowa@exmple.com",
                password = "password",
                passwordConfirm = "password",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSignUpClick = {},
            onPasswordConfirmChange = {},
            onNavigateToLogin = {},
        )
    }
}
