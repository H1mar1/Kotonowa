package com.example.kotonowa.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * カレンダー画面。
 *
 * Phase2 前半は「今月の予定/タスクを縦に並べるだけ」の最小構成。
 * 月の升目（カレンダーらしい見た目）は後のステップで足す。
 *
 * 画面は [CalendarUiState] を見て描くだけで、
 * 取得やエラー判断は [CalendarViewModel] が行う。
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addDummyItem() }) {
                Text("＋")
            }
        },
    ) { innerPadding ->
        val message = uiState.errorMessage



        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            // 上から順に試し、最初に当てはまった 1 つだけが描かれる（§5-(64)）
            // 「まだ分からない」→「異常」→「空」→「正常」の順
            when {
                uiState.isLoading -> CircularProgressIndicator()
                message != null -> Text(message)
                uiState.items.isEmpty() -> Text("今月の予定はありません")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.items,
                        key = { item -> item.id },
                    ) { item ->
                        ScheduleItemRow(item = item)
                    }
                }
            }
        }
    }
}

/**
 * 一覧の 1 行。予定（Event）とタスク（Task）で表示を変える。
 *
 * 見た目だけを担当し、データの取得や判断は [CalendarViewModel] が済ませている。
 */
@Composable
private fun ScheduleItemRow(
    item: ScheduleItem,
    modifier: Modifier = Modifier,
) {
    val label = when (item) {
        is ScheduleItem.Event -> "予定"
        is ScheduleItem.Task ->
            if (item.isCompleted) "タスク完了"
            else "タスク未完了"
    }

    val subText = when (item) {
        is ScheduleItem.Event ->
            if (item.allDay) "${item.startAt.toDateText()} 終日"
            else "${item.startAt.toDisplayText()}～${item.endAt.toTimeText()}"

        is ScheduleItem.Task -> "期限 ${item.dueAt.toDisplayText()}"

    }


    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(subText, style = MaterialTheme.typography.bodySmall)
        }
    }
}


private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("M/d(E) HH:mm")

private fun Instant.toDisplayText(): String =
    atZone(ZoneId.systemDefault()).format(DISPLAY_FORMATTER)

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

private fun Instant.toTimeText(): String =
    atZone(ZoneId.systemDefault()).format(TIME_FORMATTER)

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d(E)")

private fun Instant.toDateText(): String =
    atZone(ZoneId.systemDefault()).format(DATE_FORMATTER)

/**
 * Preview 専用のサンプルデータ。
 *
 * エミュレータも Firestore も使わずに [ScheduleItemRow] の 4 パターンを確かめるために置く。
 * 時刻は `Instant.now()` ではなく固定値にする（実行するたびに表示が変わると比べられないため）。
 */
private val PREVIEW_ITEMS: List<ScheduleItem> = listOf(

    ScheduleItem.Event(
        id = "1",
        calendarId = "1",
        title = "テスト予定",
        description = null,
        createdBy = "1",
        reminderMinutesBefore = null,
        startAt = Instant.parse("2026-08-14T05:00:00Z"),
        updatedAt = Instant.parse("2026-08-14T05:00:00Z"),
        endAt = Instant.parse("2026-08-14T06:00:00Z"),
        allDay = false,
    ),

    ScheduleItem.Event(
        id = "2",
        calendarId = "2",
        title = "お休み",
        description = null,
        createdBy = "1",
        reminderMinutesBefore = null,
        startAt = Instant.parse("2026-08-14T05:00:00Z"),
        updatedAt = Instant.parse("2026-08-14T05:00:00Z"),
        endAt = Instant.parse("2026-08-14T06:00:00Z"),
        allDay = true,
    ),

    ScheduleItem.Task(
        id = "3",
        calendarId = "3",
        title = "レポート提出",
        description = null,
        createdBy = "3",
        reminderMinutesBefore = null,
        dueAt = Instant.parse("2026-08-14T05:00:00Z"),
        updatedAt = Instant.parse("2026-08-14T05:00:00Z"),
        isCompleted = false,
    ),

    ScheduleItem.Task(
        id = "4",
        calendarId = "4",
        title = "ゴミ出し",
        description = null,
        createdBy = "4",
        reminderMinutesBefore = null,
        dueAt = Instant.parse("2026-08-14T05:00:00Z"),
        updatedAt = Instant.parse("2026-08-14T05:00:00Z"),
        isCompleted = true,
    ),
)

@Preview(showBackground = true)
@Composable
private fun ScheduleItemRowPreview() {
    KotonowaTheme {
        Column(
            Modifier.padding(16.dp),
            Arrangement.spacedBy(8.dp),
        ) {
            PREVIEW_ITEMS.forEach { item ->
                ScheduleItemRow(item = item)
            }
        }
    }
}

