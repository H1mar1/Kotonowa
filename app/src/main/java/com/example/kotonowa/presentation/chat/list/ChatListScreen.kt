package com.example.kotonowa.presentation.chat.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.CalendarType
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant

/**
 * トーク一覧（Step 49-E）。ViewModel 係。見た目は [ChatListContent]。
 *
 * 下のタブの 3 つ目「チャット」の行き先（49-F）。タブの画面なので「戻る」は置かない（タスク画面と同じ）。
 *
 * @param onRoomClick 行が押されたら、そのカレンダーの id を添えて鳴らす呼び鈴（NavHost がトーク画面へ行く）
 * @param bottomBar 画面の下に置く部品（49-F で「カレンダー／タスク／チャット」のタブを渡す）
 */
@Composable
fun ChatListScreen(
    onRoomClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatListContent(
        uiState = uiState,
        // TODO(49-E-4) 誰の：この画面の呼び鈴を ／ 何をする：見た目係にそのまま手渡す
        //   型が同じ (String) -> Unit なので { } で包まない（§1-(78) の①。34-B-2 と同じ）
        onRoomClick = onRoomClick,
        bottomBar = bottomBar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListContent(
    uiState: ChatListUiState,
    onRoomClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("チャット") }) },
        bottomBar = bottomBar,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 条件だけの when（§5-(64)）。上から順に試して、最初に当たった枝だけ描く
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                uiState.errorMessage != null -> Text(
                    uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )

                // TODO(49-E-5) 空っぽの案内を出す条件：トークの行（uiState.rooms）が空か（49-D の「まだ発言はありません」の枝と同じ形）
                uiState.rooms.isEmpty() -> Text(
                    "共有カレンダーに入ると、ここにそのカレンダーのトークが出ます",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.rooms, key = { calendar -> calendar.id }) { calendar ->
                        // TODO(49-E-6) 押されたら：誰の：呼び鈴 onRoomClick を ／ 何をする：このカレンダーの id を添えて鳴らす
                        //   値を添えるので { } で包む（§1-(78) の②。カレンダー一覧の 34-B-4 と同じ形）
                        RoomRow(calendar = calendar, onClick = { onRoomClick(calendar.id) })
                    }
                }
            }
        }
    }
}

/**
 * トーク一覧の 1 行。左にカレンダーの色の丸、右に名前。
 * ④ で未読の赤い点、新しい発言のひとことを足す予定。
 */
@Composable
private fun RoomRow(
    calendar: Calendar,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,   // 押せる版の Card（Step 18-A と同じ）
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            // カレンダーの色の丸（作成画面の色選びと同じ変換。grammar §3-(115)）
            Surface(
                shape = CircleShape,
                color = Color(calendar.color.toColorInt()),
                modifier = Modifier.size(16.dp),
            ) {}
            Text(calendar.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ---- Preview ----

private val PREVIEW_ROOMS = listOf(
    Calendar("1", "school", "me", CalendarType.SHARED, "#4CAF50", Instant.parse("2026-09-01T00:00:00Z")),
    Calendar("2", "家族", "other", CalendarType.SHARED, "#2196F3", Instant.parse("2026-09-10T00:00:00Z")),
)

@Preview(showBackground = true, name = "トークあり")
@Composable
private fun ChatListPreview() {
    KotonowaTheme {
        ChatListContent(
            uiState = ChatListUiState(rooms = PREVIEW_ROOMS, isLoading = false),
            onRoomClick = {},
        )
    }
}

@Preview(showBackground = true, name = "空っぽ")
@Composable
private fun ChatListEmptyPreview() {
    KotonowaTheme {
        ChatListContent(
            uiState = ChatListUiState(isLoading = false),
            onRoomClick = {},
        )
    }
}
