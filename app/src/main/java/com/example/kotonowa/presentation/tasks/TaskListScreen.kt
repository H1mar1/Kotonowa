package com.example.kotonowa.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.presentation.calendar.ScheduleItemRow
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant

/**
 * タスク一覧画面（Step 47）。ViewModel 係。見た目は [TaskListContent]。
 *
 * @param onItemClick 行が押されたら、そのタスクの id で鳴らす呼び鈴（詳細画面へ。ホームの onItemClick と同じ）
 * @param bottomBar 画面の下に置く部品（47-E で「カレンダー／タスク」のタブを渡す）
 */
@Composable
fun TaskListScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TaskListContent(
        uiState = uiState,
        onFilterChange = viewModel::onFilterChange,
        onToggleCompleted = viewModel::toggleCompleted,
        onItemClick = onItemClick,
        bottomBar = bottomBar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListContent(
    uiState: TaskListUiState,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleCompleted: (ScheduleItem.Task) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    // ---- 行を絞って並べる（画面の仕事。Step 23 の dayItems と同じ考え方） ----
    // 今の時刻。これより前の期限の未完了タスクが「期限切れ」
    val now = Instant.now()

    // 未完了を期限の近い順に（§4-(125) sortedBy）
    val incompleteRows = uiState.rows
        .filter { row -> !row.task.isCompleted }
        .sortedBy { row -> row.task.dueAt }

    // TODO(47-D-1) 未完了のうち、期限が今より前のもの／今以降のものに分ける（Instant も < >= で比べられる。grammar §4-(144)）
    //   どちらも incompleteRows から filter { row -> 条件 } で絞る。条件の左は row.task.dueAt、右は now
    val overdueRows = incompleteRows.filter { row -> ____ }
    val upcomingRows = incompleteRows.filter { row -> ____ }

    // TODO(47-D-2) 完了したものを、期限の新しい順に（新しい＝大きい順。sortedBy の逆向きの sortedByDescending。grammar §4-(144)）
    //   すぐ上の incompleteRows と同じ形。filter の条件から ! を取り、並べ替えの道具を替える
    val completedRows = ____

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("タスク") }) },
        bottomBar = bottomBar,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---- 未完了／完了の切り替え（Step 17 の予定／タスクの切り替えと同じ部品） ----
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    // TODO(47-D-3) 選ばれているか：状態の filter が「未完了」と同じか（ScheduleEditScreen の itemType の行と同じ形）
                    selected = ____,
                    // TODO(47-D-4) 押されたら：呼び鈴 onFilterChange を「未完了」で鳴らす
                    onClick = { ____ },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("未完了") }
                SegmentedButton(
                    // TODO(47-D-5) 47-D-3 / 47-D-4 の「完了」版
                    selected = ____,
                    onClick = { ____ },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("完了") }
            }

            // ---- 一覧（読み込み中・エラーのときは出し分け。カレンダー画面の下半分と同じ形） ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val errorMessage = uiState.errorMessage
                when {
                    uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    errorMessage != null -> Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.filter == TaskFilter.INCOMPLETE) {
                            if (incompleteRows.isEmpty()) {
                                item { EmptyMessage("未完了のタスクはありません") }
                            }
                            // 期限切れは見出しを赤に（error 色）
                            if (overdueRows.isNotEmpty()) {
                                item { SectionHeader("期限切れ", MaterialTheme.colorScheme.error) }
                                items(overdueRows, key = { row -> row.task.id }) { row ->
                                    TaskRow(row, onToggleCompleted, onItemClick)
                                }
                            }
                            if (upcomingRows.isNotEmpty()) {
                                item { SectionHeader("これから", MaterialTheme.colorScheme.onSurfaceVariant) }
                                items(upcomingRows, key = { row -> row.task.id }) { row ->
                                    TaskRow(row, onToggleCompleted, onItemClick)
                                }
                            }
                        } else {
                            if (completedRows.isEmpty()) {
                                item { EmptyMessage("完了したタスクはありません") }
                            }
                            items(completedRows, key = { row -> row.task.id }) { row ->
                                TaskRow(row, onToggleCompleted, onItemClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1 行。ホームの一覧と同じ部品（ScheduleItemRow）に、カレンダー名と押せるかを渡すだけ。
 */
@Composable
private fun TaskRow(
    row: TaskRowItem,
    onToggleCompleted: (ScheduleItem.Task) -> Unit,
    onItemClick: (String) -> Unit,
) {
    ScheduleItemRow(
        // TODO(47-D-6) 4 つを渡す（左の名前と右の値を 1 行ずつ声に出して照合する）
        //   item … この行のタスク（row の task）
        //   onClick … 押されたら、この行のタスクの id で onItemClick を鳴らす（ホームの { onItemClick(item.id) } と同じ形）
        //   canEdit … この行のカレンダーで押せるか（row の canEdit）
        //   calendarName … この行のカレンダー名（row の calendarName）
        item = ____,
        onClick = { ____ },
        onToggleCompleted = onToggleCompleted,
        canEdit = ____,
        calendarName = ____,
    )
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

// ---- Preview：期限切れ 1 件・これから 1 件（閲覧者のカレンダー）・完了 1 件 ----

private fun previewTask(id: String, title: String, due: String, completed: Boolean) = ScheduleItem.Task(
    id = id,
    calendarId = "c",
    title = title,
    description = null,
    createdBy = "u",
    reminderMinutesBefore = null,
    updatedAt = Instant.parse("2026-11-20T00:00:00Z"),
    dueAt = Instant.parse(due),
    isCompleted = completed,
)

private val PREVIEW_ROWS = listOf(
    TaskRowItem(previewTask("1", "レポート提出", "2020-11-20T14:59:00Z", false), "school", canEdit = true),
    TaskRowItem(previewTask("2", "買い物", "2099-11-23T09:00:00Z", false), "マイカレンダー", canEdit = true),
    TaskRowItem(previewTask("3", "掃除当番", "2099-11-30T09:00:00Z", false), "家族", canEdit = false),
    TaskRowItem(previewTask("4", "申込書", "2026-11-18T09:00:00Z", true), "school", canEdit = true),
)

@Preview(showBackground = true)
@Composable
private fun TaskListIncompletePreview() {
    KotonowaTheme {
        TaskListContent(
            uiState = TaskListUiState(rows = PREVIEW_ROWS, isLoading = false),
            onFilterChange = {},
            onToggleCompleted = {},
            onItemClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListCompletedPreview() {
    KotonowaTheme {
        TaskListContent(
            uiState = TaskListUiState(rows = PREVIEW_ROWS, filter = TaskFilter.COMPLETED, isLoading = false),
            onFilterChange = {},
            onToggleCompleted = {},
            onItemClick = {},
        )
    }
}
