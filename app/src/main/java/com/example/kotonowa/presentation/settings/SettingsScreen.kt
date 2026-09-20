package com.example.kotonowa.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.kotonowa.domain.model.User
import com.example.kotonowa.ui.theme.KotonowaTheme

/**
 * 設定画面。
 *
 * ViewModel から情報を受け取り、操作（ログアウト・戻る）を伝えるだけの役割。
 * 見た目は [SettingsContent] が描く（ViewModel を持たないので Preview で確認できる）。
 *
 * @param onNavigateBack 「戻る」を押したときに鳴らす呼び鈴
 * @param onLoggedOut ログアウトが済んだときに鳴らす呼び鈴。ログイン画面へ戻るのに使う
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsContent(
        user = viewModel.user,
        onNavigateBack = onNavigateBack,
        onLogoutClick = {
            viewModel.logout()
            onLoggedOut()
        },
        modifier = modifier,
    )
}

/**
 * 設定画面の見た目。ViewModel を受け取らず、必要なものを引数でもらうだけ。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    user: User?,
    onNavigateBack: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {

            // --- プロフィール ---
            Text("プロフィール", style = MaterialTheme.typography.titleMedium)

            Text(user?.displayName ?: "（表示名なし）")

            // ★5 メールアドレス。★4 と同じ形で、見る先を email に変えるだけ
            Text(user?.email ?: "メールアドレス不明")

            // --- 通知設定（準備中） ---
            // ★6 押せない状態にする。中身が無いので、押して空振りさせない（Step 21 の enabled と同じ）
            TextButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("通知設定", modifier = Modifier.weight(1f))
                Text("準備中")
            }

            // --- ログアウト ---
            // ★7 押されたら引数で受け取った呼び鈴を鳴らすだけ（行き先は NavHost が決める）
            TextButton(onClick = onLogoutClick) {
                Text("ログアウト")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    KotonowaTheme {
        SettingsContent(
            user = User(
                uid = "1",
                email = "you@example.com",
                displayName = "ことのわ太郎",
                photoUrl = null,
            ),
            onNavigateBack = {},
            onLogoutClick = {},
        )
    }
}
