package com.example.kotonowa.presentation.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
 * 通知設定画面（Step 45）。ViewModel 係。見た目は [NotificationSettingsContent]。
 *
 * @param onNavigateBack 「戻る」を押したときに鳴らす呼び鈴
 */
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationSettingsContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        // 型が (String, Boolean) -> Unit で一致するので、関数をそのまま渡す（§1-(78) の ①）
        onToggle = viewModel::onNotificationToggle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsContent(
    uiState: NotificationSettingsUiState,
    onNavigateBack: () -> Unit,
    onToggle: (calendarId: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("通知設定") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("戻る") }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val errorMessage = uiState.errorMessage
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                errorMessage != null -> Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "他のメンバーが予定・タスクを追加・編集したときの通知",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    if (uiState.calendars.isEmpty()) {
                        item {
                            Text(
                                "共有カレンダーがありません",
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }

                    items(uiState.calendars, key = { it.id }) { calendar ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(calendar.name, modifier = Modifier.weight(1f))

                            Switch(
                                // TODO(45-B-3) スイッチが ON か：この行の id が「止めている一覧」に入って“いない”なら ON
                                //   「入っている」は in、「入っていない」は !in（grammar §4-(93)。Step 23 の date in datesWithItems）
                                checked = calendar.id !in uiState.mutedCalendarIds,
                                // TODO(45-B-4) 押されたら：呼び鈴 onToggle を、この行の id と、スイッチが渡してくる新しい向きで鳴らす
                                //   { 受け取る名前 -> 呼び鈴(…, …) } の形（Step 21 の Checkbox の onCheckedChange と同じ。今回は向きも渡す）
                                onCheckedChange = { checked -> onToggle(calendar.id, checked) },
                            )
                        }
                        HorizontalDivider()
                    }

                    item {
                        Text(
                            "※ 招待の通知は、この設定に関係なく届きます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun previewCalendar(id: String, name: String) = Calendar(
    id = id,
    name = name,
    ownerUid = "owner",
    type = CalendarType.SHARED,
    color = "#4CAF50",
    createdAt = Instant.parse("2026-11-20T00:00:00Z"),
)

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsContentPreview() {
    KotonowaTheme {
        NotificationSettingsContent(
            uiState = NotificationSettingsUiState(
                calendars = listOf(previewCalendar("a", "school"), previewCalendar("b", "家族")),
                // 家族だけ止めている → school は ON、家族は OFF に見えるはず
                mutedCalendarIds = setOf("b"),
                isLoading = false,
            ),
            onNavigateBack = {},
            onToggle = { _, _ -> },
        )
    }
}
