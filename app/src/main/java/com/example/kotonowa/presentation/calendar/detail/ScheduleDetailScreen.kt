package com.example.kotonowa.presentation.calendar.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 予定/タスクの詳細画面。
 *
 * ViewModel から状態を受け取り、操作（削除・戻る）を伝えるだけの役割。
 * 見た目は [ScheduleDetailContent] が描く（ViewModel を持たないので Preview で確認できる）。
 *
 * @param onNavigateBack 「戻る」を押したときに鳴らす呼び鈴
 * @param onDeleted 削除が終わったときに鳴らす呼び鈴。一覧へ戻るのに使う
 */
@Composable
fun ScheduleDetailScreen(
    onNavigateBack: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 削除できたら一覧へ戻る。ScheduleEditScreen の isSaved → onSaved() と同じ形。
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }

    ScheduleDetailContent(
        uiState = uiState,
        onDeleteClick = viewModel::delete,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

/**
 * 詳細画面の見た目。ViewModel を受け取らず、必要なものを引数でもらうだけ。
 * こうしておくと [ScheduleDetailContentEventPreview] などで見た目だけ確認できる。
 */
@Composable
private fun ScheduleDetailContent(
    uiState: ScheduleDetailUiState,
    onDeleteClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = uiState.item

    // 上から順に試し、最初に当てはまった 1 つだけが描かれる（§5-(64)）。
    // 「まだ読み込み中」→「読めなかった」→「読めた」の順。
    when {
        uiState.isLoading -> {
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        item == null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = uiState.errorMessage ?: "見つかりませんでした"
                )
                TextButton(onClick = onNavigateBack) {
                    Text("戻る")
                }

            }

        }

        else -> {
            val dateText = when (item) {
                is ScheduleItem.Event ->
                    if (item.allDay) "${item.startAt.toDisplayText()} 終日"
                    else "${item.startAt.toDisplayText()}〜${item.endAt.toDisplayText()}"

                is ScheduleItem.Task -> {
                    val base = "期限 ${item.dueAt.toDisplayText()}"
                    if (item.isCompleted) "$base(完了)" else base
                }
            }
            val description = item.description
            val message = uiState.errorMessage

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(dateText)

                if (description != null) {
                    Text(description)
                }

                if (message != null) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = onDeleteClick,
                    enabled = !uiState.isDeleting
                ) {
                    Text("削除")
                }

                TextButton(onClick = onNavigateBack, enabled = !uiState.isDeleting) {
                    Text("戻る")
                }
            }
        }
    }
}

// --- 日時を「読める文字」にする型紙 ---
private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("M/d(E) HH:mm")

private fun Instant.toDisplayText(): String =
    atZone(ZoneId.systemDefault()).format(DISPLAY_FORMATTER)

// --- Preview 専用のサンプルデータ（案B：この画面の中に持つ） ---
private val PREVIEW_EVENT = ScheduleItem.Event(
    id = "1",
    calendarId = "1",
    title = "打ち合わせ",
    description = "会議室A",
    createdBy = "1",
    reminderMinutesBefore = null,
    startAt = Instant.parse("2026-08-14T05:00:00Z"),
    updatedAt = Instant.parse("2026-08-14T05:00:00Z"),
    endAt = Instant.parse("2026-08-14T06:00:00Z"),
    allDay = false,
)

private val PREVIEW_TASK = ScheduleItem.Task(
    id = "2",
    calendarId = "2",
    title = "レポート提出",
    description = null,
    createdBy = "2",
    reminderMinutesBefore = null,
    dueAt = Instant.parse("2026-08-16T14:59:00Z"),
    updatedAt = Instant.parse("2026-08-14T05:00:00Z"),
    isCompleted = false,
)

@Preview(showBackground = true)
@Composable
private fun ScheduleDetailContentEventPreview() {
    KotonowaTheme {
        ScheduleDetailContent(
            uiState = ScheduleDetailUiState(item = PREVIEW_EVENT, isLoading = false),
            onDeleteClick = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleDetailContentTaskPreview() {
    KotonowaTheme {
        ScheduleDetailContent(
            uiState = ScheduleDetailUiState(item = PREVIEW_TASK, isLoading = false),
            onDeleteClick = {},
            onNavigateBack = {},
        )
    }
}
