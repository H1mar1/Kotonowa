package com.example.kotonowa.presentation.calendarlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.CalendarType
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant

/**
 * カレンダー一覧画面。ViewModel 係。
 *
 * お手本は `presentation/settings/SettingsScreen`（Screen ＋ Content の 2 段構え）。
 * 画面は `navController` を受け取らず、呼び鈴（`() -> Unit`）だけを持つ。
 *
 * @param onCalendarClick 行が押されたときに鳴らす呼び鈴。押されたカレンダーの id を添える。
 *   NavHost がその id を住所に載せてメンバー画面へ行く（Step 34-B）。
 */
@Composable
fun CalendarListScreen(
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    onCalendarClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddClick = onAddClick,
        // TODO(34-B-2) 誰の：この画面の呼び鈴を ／ 何をする：見た目係にそのまま手渡す
        //   型が同じ (String) -> Unit なので { } で包まなくてよい（§1-(78) の①。onAddClick と同じ）
        onCalendarClick = onCalendarClick,
        modifier = modifier,
    )
}

/**
 * 一覧の見た目。ViewModel を受け取らないので `@Preview` で確認できる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarListContent(
    uiState: CalendarListUiState,
    onNavigateBack: () -> Unit,
    onAddClick: () -> Unit,
    // TODO(34-B-3) 呼び鈴の型。添えるのはカレンダーの id（文字）。Calendar そのものではない
    onCalendarClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("カレンダー") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("戻る") }
                },
                actions = {
                    TextButton(onClick = onAddClick) { Text("＋") }
                },
            )
        },
    ) { innerPadding ->

        // 条件だけの when（§5-(64)）。上から順に試して、最初に当たった枝だけ描く。
        // 空っぽの案内は LazyColumn の中にある（Step 33-B）。
        // ⚠️ 枝の順番が命。errorMessage の枝を先に置くと、読み込み中に何も出なくなる
        when {
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (uiState.calendars.isEmpty()) {
                        item {
                            Text(
                                "共有カレンダーはまだありません。＋ から作成してください。",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        // 一覧の 1 件ずつを CalendarRow で描く（§3-(66)）
                        items(uiState.calendars, key = { calendar -> calendar.id }) { calendar ->
                            // TODO(34-B-4) 誰の：この行の ／ 何をする：呼び鈴を「このカレンダーの id」を添えて鳴らす
                            CalendarRow(calendar = calendar, onClick = { onCalendarClick(calendar.id) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * 一覧の 1 行。お手本は `CalendarScreen` の `ScheduleItemRow`。
 */
@Composable
private fun CalendarRow(
    calendar: Calendar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,   // 押せる版の Card（Step 18-A の ScheduleItemRow と同じ）
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = when (calendar.type) {
                    CalendarType.PERSONAL -> "個人"
                    CalendarType.SHARED -> "共有"
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )

            Text(
                text = calendar.name,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

// ---- Preview 用のダミー（この画面の中だけで使う。Step 18-D の「案B」と同じ判断） ----

private val PREVIEW_CALENDARS = listOf(
    Calendar(
        id = "1",
        name = "サークル",
        ownerUid = "me",
        type = CalendarType.SHARED,
        color = "#4CAF50",
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
    ),
    Calendar(
        id = "2",
        name = "家族",
        ownerUid = "me",
        type = CalendarType.SHARED,
        color = "#2196F3",
        createdAt = Instant.parse("2026-09-10T00:00:00Z"),
    ),
)

@Preview(showBackground = true, name = "一覧あり")
@Composable
private fun CalendarListContentPreview() {
    KotonowaTheme {
        CalendarListContent(
            uiState = CalendarListUiState(calendars = PREVIEW_CALENDARS, isLoading = false),
            onNavigateBack = {},
            onAddClick = {},
            onCalendarClick = {},
        )
    }
}

@Preview(showBackground = true, name = "空っぽ")
@Composable
private fun CalendarListEmptyPreview() {
    KotonowaTheme {
        CalendarListContent(
            uiState = CalendarListUiState(isLoading = false),
            onNavigateBack = {},
            onAddClick = {},
            onCalendarClick = {},
        )
    }
}
