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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.ui.theme.KotonowaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * カレンダー画面。
 *
 * Phase2 前半は「今月の予定/タスクを縦に並べるだけ」の最小構成。
 * 月の升目（カレンダーらしい見た目）は後のステップで足す。
 *
 * 画面は [CalendarUiState] を見て描くだけで、
 * 取得やエラー判断は [CalendarViewModel] が行う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("ことのわ") },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        }
                    ) {
                        Text("ログアウト")
                    }

                }

            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("＋")
            }
        },
    ) { innerPadding ->
        val message = uiState.errorMessage

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MonthHeader(
                month = uiState.currentMonth,
                onPrevious = viewModel::showPreviousMonth,
                onNext = viewModel::showNextMonth,
            )

            WeekdayHeader()

            MonthGrid(
                month = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                datesWithItems = uiState.datesWithItems,
                onDateClick = viewModel::selectDate,
            )

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
                        ScheduleItemRow(
                            item = item,
                            onClick = { onItemClick(item.id) },
                            onToggleCompleted = viewModel::toggleCompleted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1 行の見た目のうち、状態（予定 / タスク未完了 / タスク完了）で変わるものをまとめた入れ物。
 *
 * ラベルと色を別々の `when` で選ぶと同じ判定を何度も書くことになり、
 * 状態が増えたときに片方だけ直し忘れる。判定は 1 回にして、結果をここに詰める。
 */
private data class RowStyle(
    val label: String,
    val badgeColor: Color,
    val badgeContentColor: Color,
    /** タイトルの文字色。完了したタスクだけ控えめにする。 */
    val titleColor: Color,
    /** タイトルの装飾。打ち消し線を引かないときは null。 */
    val titleDecoration: TextDecoration?,
)

/**
 * 一覧の 1 行。予定（Event）とタスク（Task）で表示を変える。
 *
 * 見た目だけを担当し、データの取得や判断は [CalendarViewModel] が済ませている。
 */
@Composable
private fun ScheduleItemRow(
    item: ScheduleItem,
    onClick: () -> Unit,
    onToggleCompleted: (ScheduleItem.Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowStyle = when (item) {
        is ScheduleItem.Event ->
            RowStyle(
                label = "予定",
                badgeColor = MaterialTheme.colorScheme.primaryContainer,
                badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                titleColor = MaterialTheme.colorScheme.onSurface,
                titleDecoration = null,
            )

        is ScheduleItem.Task ->
            if (item.isCompleted) RowStyle(
                label = "タスク完了",
                badgeColor = MaterialTheme.colorScheme.surfaceVariant,
                badgeContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                titleDecoration = TextDecoration.LineThrough,
            )
            else RowStyle(
                label = "タスク未完了",
                badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                titleColor = MaterialTheme.colorScheme.onSurface,
                titleDecoration = null,
            )
    }

    val subText = when (item) {
        is ScheduleItem.Event ->
            if (item.allDay) "${item.startAt.toDateText()} 終日"
            else "${item.startAt.toDisplayText()}～${item.endAt.toTimeText()}"

        is ScheduleItem.Task -> "期限 ${item.dueAt.toDisplayText()}"

    }


    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item is ScheduleItem.Task) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggleCompleted(item) })
                }
                Surface(
                    color = rowStyle.badgeColor,
                    contentColor = rowStyle.badgeContentColor,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        rowStyle.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = rowStyle.titleColor,
                    textDecoration = rowStyle.titleDecoration,
                )
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PREVIEW_ITEMS.forEach { item ->
                ScheduleItemRow(
                    item = item,
                    onClick = {},
                    onToggleCompleted = {},
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious) {
            Text("<")
        }

        Text(
            text = "${month.year}年${month.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        TextButton(onClick = onNext) {
            Text(">")
        }
    }
}

@Composable
private fun WeekdayHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        listOf("日", "月", "火", "水", "木", "金", "土").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 月の升目。42 個（6 週 × 7 日）を作り、7 個ずつの段に配る。 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    datesWithItems: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shift = month.atDay(1).dayOfWeek.value % 7

    val cells:List<LocalDate?> = (0 until 42).map{index ->
        val day=index-shift+1
        if(day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
    Column(
        modifier=modifier.fillMaxWidth()
    ) {
        cells.chunked(7).forEach { week ->
            Row(modifier= Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date=date,
                        isSelected = date == selectedDate,
                        hasItems=date != null && date in datesWithItems,
                        onClick = { if (date != null) onDateClick(date) },
                        modifier= Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** 升目 1 個。日付が null なら空欄。 */
@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    hasItems: Boolean,
    onClick: () -> Unit,
    modifier: Modifier= Modifier,
){
    TextButton(
        onClick=onClick,
        enabled = date != null,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date?.dayOfMonth?.toString() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = if(isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if(hasItems) "⚪" else " ",
                style = MaterialTheme.typography.bodyMedium,
                color = if(hasItems) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthHeaderPreview() {
    KotonowaTheme {
        Column {
            MonthHeader(month = YearMonth.of(2026, 9), onPrevious = {}, onNext = {})
            WeekdayHeader()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthGridPreview() {
    KotonowaTheme {
        Column {
            MonthHeader(month = YearMonth.of(2026, 9), onPrevious = {}, onNext = {})
            WeekdayHeader()
            MonthGrid(
                month = YearMonth.of(2026, 9),
                selectedDate = LocalDate.of(2026, 9, 13),
                datesWithItems = setOf(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 20)),
                onDateClick = {},
            )
        }
    }
}

