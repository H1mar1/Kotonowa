package com.example.kotonowa.presentation.invites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.Invite
import com.example.kotonowa.domain.model.InviteStatus
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant

/**
 * 届いた招待の一覧画面。ViewModel 係。
 *
 * お手本は `presentation/calendarlist/CalendarListScreen`（Screen ＋ Content の 2 段構え）。
 * 承認しても画面は移動しない（2026-11-16 の設計。一覧に残る）。
 */
@Composable
fun InviteListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InviteListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        // TODO(36-E-3-1) 誰の：頭脳の ／ 何をする：「承認」「拒否」の関数をそのまま手渡す（§1-(78) の①）
        //   型が (Invite) -> Unit で一致するので { } で包まない。MemberListScreen の onRemove と同じ形
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        // TODO(39-D-1) 誰の：頭脳の ／ 何を：［確認メールを再送］［確認しました］の関数をそのまま手渡す（上の 2 行と同じ形）
        //   型は () -> Unit（引数なし）。39-C で作った 2 つの関数の名前を ____ に
        onResendEmail = viewModel::resendVerificationEmail,
        onCheckEmail = viewModel::checkEmailVerified,
        modifier = modifier,
    )
}

/**
 * 一覧の見た目。ViewModel を受け取らないので `@Preview` で確認できる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteListContent(
    uiState: InviteListUiState,
    onNavigateBack: () -> Unit,
    onAccept: (Invite) -> Unit,    // 押された招待状を添えて鳴らす
    onDecline: (Invite) -> Unit,
    onResendEmail: () -> Unit,     // ［確認メールを再送］（Step 39）
    onCheckEmail: () -> Unit,      // ［確認しました］（Step 39）
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("届いた招待") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("戻る") }
                },
            )
        },
    ) { innerPadding ->

        // 条件だけの when（§5-(64)）。お手本は CalendarListScreen とまったく同じ順番
        when {
            // Step 39：未確認なら、読み込み中やエラーより先にカードを出す（when は上から順に見る。§5-(64)）
            !uiState.isEmailVerified -> {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)) {
                    VerifyEmailCard(
                        message = uiState.actionMessage,
                        isChecking = uiState.isCheckingEmail,
                        onResendEmail = onResendEmail,
                        onCheckEmail = onCheckEmail,
                    )
                }
            }

            uiState.isLoading -> {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)) {
                    Text(uiState.errorMessage)
                }
            }

            else -> {
                LazyColumn(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)) {

                    // 返事の結果の一言。一覧の一番上に 1 つだけ（§3-(120) の item）
                    // TODO(36-E-3-2) 一度ローカルの val に受けてから null でないか確かめる（Step 18-D と同じ。スマートキャストのため）
                    //   uiState.actionMessage は別の型の中の項目なので、そのままでは if の中で String として扱えない
                    val message = uiState.actionMessage
                    if (message != null) {
                        item {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    if (uiState.invites.isEmpty()) {
                        item {
                            Text("届いている招待はありません。", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        // TODO(36-E-3-3) 行の見分け方。誰の：この招待状の ／ 何で見分ける：id（CalendarListScreen の key と同じ形）
                        items(uiState.invites, key = { invite -> invite.id }) { invite ->
                            InviteRow(
                                invite = invite,
                                // TODO(36-E-3-4) この招待状の返事を送っている途中か。
                                //   誰の：状態の「送っている途中の id」が ／ 何か：この招待状の id と同じか（==）
                                isProcessing = uiState.processingInviteId==invite.id,
                                // TODO(36-E-3-5) 誰の：この画面の呼び鈴を ／ 何をする：この招待状を添えて鳴らす（{ } で包む。§1-(78) の②）
                                onAccept = { onAccept(invite) },
                                onDecline = { onDecline(invite) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 招待状 1 枚ぶんのカード。カレンダー名・肩書き・［承認］［拒否］。
 *
 * 状態は持たない（§3-(76)）。「送っている途中か」も引数でもらう。
 */
@Composable
private fun InviteRow(
    invite: Invite,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = invite.calendarName, style = MaterialTheme.typography.titleMedium)

            Text(
                text = "${invite.role.label()}として招待されています",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(8.dp))

            // 横に 2 つ。すき間は 8dp（§3-(66) の spacedBy）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 承認は塗りつぶしのボタン、拒否は枠線だけのボタン（§3-(133)）。押してほしい方を目立たせる
                Button(
                    onClick = onAccept,
                    // TODO(36-E-3-6) 押せる条件。「送っている途中ではない」とき（MemberListScreen の招待ボタンと同じ式）
                    enabled = !isProcessing,
                ) {
                    Text(if (isProcessing) "送信中…" else "承認")
                }
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isProcessing,
                ) {
                    Text("拒否")
                }
            }
        }
    }
}

/**
 * メールアドレスの確認を促すカード（Step 39）。
 * 招待は「メールの持ち主」本人だけが受け取れる。リンクを押してから［確認しました］で一覧が出る。
 * 状態は持たない（§3-(76)）。一言・通信中か・呼び鈴 2 つを引数でもらう。
 */
@Composable
private fun VerifyEmailCard(
    message: String?,
    isChecking: Boolean,
    onResendEmail: () -> Unit,
    onCheckEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "メールアドレスの確認が必要です", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "招待を受け取るには、登録したメールアドレスに届いた確認メールのリンクを押してください。",
                style = MaterialTheme.typography.bodyMedium,
            )

            // 結果の一言（「送りました」「まだ確認できていません」など）
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            // 横に 2 つ。押してほしい［確認しました］を塗りつぶし、［再送］を枠線にする（InviteRow と同じ考え方。§3-(133)）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    // TODO(39-D-2) 押されたら鳴らす呼び鈴。誰の：このカードが受け取った ／ どれ：［確認しました］の方
                    //   InviteRow の onClick = onAccept と同じく、() を付けずに渡す
                    onClick = onCheckEmail,
                    // TODO(39-D-3) 押せる条件。「通信中ではない」とき（InviteRow の enabled = !isProcessing と同じ形）
                    enabled = !isChecking,
                ) {
                    Text(if (isChecking) "確認中…" else "確認しました")
                }
                OutlinedButton(
                    onClick = onResendEmail,
                    enabled = !isChecking,
                ) {
                    Text("確認メールを再送")
                }
            }
        }
    }
}

/**
 * 肩書きを画面に出す日本語にする（拡張関数。§4-㊱）。
 * MemberListScreen にも同じものがあるが private なので、ここに置く。
 * 3 画面目で要るようになったら共通のファイルへ切り出す（Step 18-D の「案B」と同じ判断）。
 * 招待状の肩書きは editor / viewer だけだが、when は enum の全部の枝を書かないとコンパイルが通らない（§5-㊳）。
 */
private fun MemberRole.label(): String = when (this) {
    MemberRole.OWNER -> "オーナー"
    MemberRole.EDITOR -> "編集者"
    MemberRole.VIEWER -> "閲覧者"
}

// ---- Preview 用のダミー ------------------------------------------------------

private val PREVIEW_INVITES = listOf(
    Invite(
        id = "cal1_me@example.com",
        calendarId = "cal1",
        calendarName = "家族",
        invitedEmail = "me@example.com",
        role = MemberRole.EDITOR,
        status = InviteStatus.PENDING,
        invitedBy = "mom",
        createdAt = Instant.parse("2026-11-16T00:00:00Z"),
    ),
    Invite(
        id = "cal2_me@example.com",
        calendarId = "cal2",
        calendarName = "サークル",
        invitedEmail = "me@example.com",
        role = MemberRole.VIEWER,
        status = InviteStatus.PENDING,
        invitedBy = "leader",
        createdAt = Instant.parse("2026-11-17T00:00:00Z"),
    ),
)

@Preview(showBackground = true, name = "招待 2 件・1 件は送信中")
@Composable
private fun InviteListPreview() {
    KotonowaTheme {
        InviteListContent(
            uiState = InviteListUiState(
                invites = PREVIEW_INVITES,
                isLoading = false,
                processingInviteId = "cal2_me@example.com",
                actionMessage = "「部活」に参加しました",
            ),
            onNavigateBack = {},
            onAccept = {},
            onDecline = {},
            onResendEmail = {},
            onCheckEmail = {},
        )
    }
}

@Preview(showBackground = true, name = "招待なし")
@Composable
private fun InviteListEmptyPreview() {
    KotonowaTheme {
        InviteListContent(
            uiState = InviteListUiState(isLoading = false),
            onNavigateBack = {},
            onAccept = {},
            onDecline = {},
            onResendEmail = {},
            onCheckEmail = {},
        )
    }
}

@Preview(showBackground = true, name = "メール未確認（Step 39）")
@Composable
private fun InviteListUnverifiedPreview() {
    KotonowaTheme {
        InviteListContent(
            uiState = InviteListUiState(
                isLoading = false,
                isEmailVerified = false,
                actionMessage = "まだ確認できていません。メールのリンクを押してから、もう一度押してください",
            ),
            onNavigateBack = {},
            onAccept = {},
            onDecline = {},
            onResendEmail = {},
            onCheckEmail = {},
        )
    }
}
