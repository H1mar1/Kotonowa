package com.example.kotonowa.presentation.chat.room

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.ChatMessage
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 吹き出しの横の時刻（「15:04」） */
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm")

/**
 * トーク画面（Step 49-D）。ViewModel 係。見た目は [ChatRoomContent]。
 *
 * @param onNavigateBack 上のバーの「＜」が押されたら鳴らす呼び鈴（行き先は NavHost が決める）
 */
@Composable
fun ChatRoomScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatRoomContent(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::send,
        // TODO(50-D-1) 頭脳の 3 つの仕事を、見た目係にそのまま手渡す（すぐ上の 2 行と同じ形。§1-(78) の①）
        //   長押し … onMessageLongPress ／ ［やめる］… dismissDelete ／ ［消す］… confirmDelete
        onMessageLongPress = viewModel::onMessageLongPress,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatRoomContent(
    uiState: ChatRoomUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 「この発言を消しますか？」（Step 50。grammar §3-(149)）。
    // 確認中の発言（deleteTarget）があるときだけ出す。開く・閉じるは画面が持たず、頭脳の箱を見るだけ（Step 17 のピッカーと同じ）
    // TODO(50-D-3) 出す条件：状態の deleteTarget が null でないか（詳細画面の「errorMessage != null」と同じ形）
    if (uiState.deleteTarget !=null) {
        AlertDialog(
            // ダイアログの外や端末の「戻る」が押されたとき。［やめる］と同じ扱い
            onDismissRequest = onDismissDelete,
            title = { Text("この発言を消しますか？") },
            text = { Text("消した発言は元に戻せません。ほかのメンバーの画面からも消えます") },
            confirmButton = {
                // TODO(50-D-4) 押されたら：呼び鈴 onConfirmDelete を（渡すだけなので () は付けない）。消している途中は押せない
                TextButton(onClick = onConfirmDelete, enabled = !uiState.isDeleting) { Text("消す") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("やめる") }
            },
        )
    }

    // 一覧のスクロール位置を覚えておく係（grammar §3-(148)。Claude が書いた）
    val listState = rememberLazyListState()

    // 発言が増えたら、いちばん下（最新）までスクロールする。
    // 鍵（messages.size）が変わったときだけ中身が動く（grammar §3-(148)。Claude が書いた）
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("＜") } },
            )
        },
        // 入力欄は画面の下に固定。キーボードが出たら、その上に押し上げる（imePadding。grammar §3-(148)）
        bottomBar = {
            InputBar(
                input = uiState.input,
                isSending = uiState.isSending,
                errorMessage = uiState.errorMessage,
                onInputChange = onInputChange,
                onSend = onSend,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.messages.isEmpty() -> Text(
                    "まだ発言はありません。最初のひとことを送ってみましょう",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.messages, key = { message -> message.id }) { message ->
                        // TODO(49-D-7) 自分の発言か：この発言の送った人の uid が、状態の myUid と同じか（==）
                        val isMine = message.senderUid == uiState.myUid
                        // TODO(50-D-2) 長押しされたら：誰の：呼び鈴 onMessageLongPress を ／ 何をする：この発言（message）を添えて鳴らす
                        //   値を添えるので { } で包む（§1-(78) の②。49-E-6 の onClick = { onRoomClick(calendar.id) } と同じ形）
                        MessageBubble(message = message, isMine = isMine, onLongPress = { onMessageLongPress(message) })
                    }
                }
            }
        }
    }
}

/**
 * 発言 1 つ。自分の発言は右・色付き、他の人の発言は左・灰色で、上に名前を出す（LINE と同じ並べ方）。
 *
 * @param onLongPress 吹き出しが長押しされたら鳴らす呼び鈴（Step 50）。自分の発言にだけ付ける
 */
@Composable
private fun MessageBubble(message: ChatMessage, isMine: Boolean, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // TODO(49-D-8) 横の寄せ方：自分の発言なら右（Arrangement.End）、そうでなければ左（Arrangement.Start）
        //   if は「答えを返す式」として使える（grammar §5-㊳。38-B の色の出し分けと同じ形：if (条件) A else B）
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        ) {
            // 他の人の発言だけ、上に小さく名前（自分の発言に自分の名前は要らない）
            if (!isMine) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                // 吹き出しが画面いっぱいに広がらないよう、幅の上限を決める。
                // 自分の発言にだけ長押しを付ける（他の人の発言は押しても何も起きない。grammar §3-(149)。Claude が書いた）
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .then(
                        if (isMine) Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onLongPress
                        )
                        else Modifier
                    ),
            ) {
                Text(message.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Text(
                message.createdAt.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 画面の下の入力欄と［送信］ボタン。キーボードが出たらその上に乗る。
 */
@Composable
private fun InputBar(
    input: String,
    isSending: Boolean,
    errorMessage: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(8.dp),
        ) {
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    // TODO(49-D-9) 今の値と、変化の返し先（MemberListScreen の招待のメール欄と同じ形）
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("メッセージ") },
                    // 長い文は 4 行まで広がり、それ以上は欄の中でスクロール
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    // TODO(49-D-10) 押されたら：呼び鈴 onSend を（渡すだけなので () は付けない。Step 35-E の ⚠️）
                    onClick = onSend,
                    // 送っている途中・空白だけのときは押せない
                    enabled = !isSending && input.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("送信") }
            }
        }
    }
}

// ---- Preview：自分と他の人の発言が交互に ----

private val PREVIEW_MESSAGES = listOf(
    ChatMessage(
        "1",
        "other",
        "test@example.com",
        "明日の会議、場所はどこでしたっけ？",
        Instant.parse("2026-11-24T06:00:00Z")
    ),
    ChatMessage("2", "me", "tester", "3 階の会議室です！", Instant.parse("2026-11-24T06:01:00Z")),
    ChatMessage(
        "3",
        "other",
        "test@example.com",
        "ありがとうございます。資料はカレンダーの予定に貼っておきますね",
        Instant.parse("2026-11-24T06:02:00Z")
    ),
)

@Preview(showBackground = true)
@Composable
private fun ChatRoomPreview() {
    KotonowaTheme {
        ChatRoomContent(
            uiState = ChatRoomUiState(
                title = "school",
                messages = PREVIEW_MESSAGES,
                myUid = "me",
                input = "了解です",
                isLoading = false,
            ),
            onInputChange = {},
            onSend = {},
            onMessageLongPress = {},
            onDismissDelete = {},
            onConfirmDelete = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatRoomEmptyPreview() {
    KotonowaTheme {
        ChatRoomContent(
            uiState = ChatRoomUiState(title = "school", isLoading = false),
            onInputChange = {},
            onSend = {},
            onMessageLongPress = {},
            onDismissDelete = {},
            onConfirmDelete = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, name = "消す確認")
@Composable
private fun ChatRoomDeletePreview() {
    KotonowaTheme {
        ChatRoomContent(
            uiState = ChatRoomUiState(
                title = "school",
                messages = PREVIEW_MESSAGES,
                myUid = "me",
                isLoading = false,
                deleteTarget = PREVIEW_MESSAGES[1],
            ),
            onInputChange = {},
            onSend = {},
            onMessageLongPress = {},
            onDismissDelete = {},
            onConfirmDelete = {},
            onNavigateBack = {},
        )
    }
}
