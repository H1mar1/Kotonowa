package com.example.kotonowa.presentation.calendar

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotonowa.domain.model.MemberRole
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
    onSetting: () -> Unit,
    onCreateCalendarClick: () -> Unit,     // ▼ のメニューの「＋ 新しいカレンダー」（Step 33-H）
    onManageCalendarsClick: () -> Unit,    // ▼ のメニューの「カレンダーの管理」（Step 33-H）
    onInvitesClick: () -> Unit,            // ▼ のメニューの「届いた招待」（Step 36-D）
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                // 「家族 ▼」を押すと、その場にカレンダーのメニューを開く（Step 33-H。grammar §3-(107)）
                title = {
                    // ボタンとメニューを同じ Box に入れる。メニューは「どのボタンから出たか」を Box で知る
                    Box {
                        TextButton(onClick = viewModel::openCalendarMenu) {
                            // Step 38-B：「名前 ▼」の行の下に「閲覧のみ」を足すため、縦に積む Column で包んだ。
                            //   上の段 ＝ Row（名前と ▼ の横並び）／ 下の段 ＝ 閲覧者のときだけ「閲覧のみ」
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 名前と ▼ を別の Text に分けた（Step 36-D）。▼ にだけ赤い点を付けるため
                                    Text(
                                        text = uiState.calendarName,
                                        style = MaterialTheme.typography.titleLarge,   // バーの見出しと同じ大きさ
                                        color = MaterialTheme.colorScheme.onSurface,   // ボタンの色（primary）ではなく見出しの色
                                    )
                                    // ▼ の右上に赤い点を重ねる部品（grammar §3-(132)）
                                    BadgedBox(
                                        badge = {
                                            // TODO(36-D-3-1) 点を出す条件。誰の：状態の ／ 何か：招待の数が 0 より多いか
                                            //   Badge() は中身を書かないと「小さな赤い点」になる
                                            if (uiState.pendingInviteCount > 0) {
                                                Badge()
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = " ▼",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }

                                // TODO(38-B-1) 「閲覧のみ」を出す条件。誰の：状態の ／ 何か：書き換えて「よくない」とき
                                //   「〜ではない」は頭に ! を付ける（InviteListScreen の enabled = !isProcessing と同じ）
                                if (!uiState.canEdit) {
                                    Text(
                                        text = "閲覧のみ",
                                        style = MaterialTheme.typography.labelSmall,          // 名前より小さく
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,   // 名前より薄い色
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = uiState.isCalendarMenuOpen,
                            onDismissRequest = viewModel::closeCalendarMenu,
                        ) {
                            // 先頭はマイカレンダー（掲示板の白紙＝null）
                            DropdownMenuItem(
                                text = { Text("マイカレンダー") },
                                leadingIcon = { CheckMark(visible = uiState.selectedCalendarId == null) },
                                onClick = { viewModel.selectCalendar(null) },
                            )

                            // 共有カレンダーを 1 つずつ（§4-(114)）
                            uiState.calendars.forEach { calendar ->
                                DropdownMenuItem(
                                    text = { Text(calendar.name) },
                                    leadingIcon = { CheckMark(visible = calendar.id == uiState.selectedCalendarId) },
                                    onClick = { viewModel.selectCalendar(calendar) },
                                )
                            }

                            HorizontalDivider()   // 区切り線（§3-(123)）

                            // 届いた招待（Step 36-D）。0 件なら行ごと出さない
                            // Step 39：未確認の人は件数を読めない（0 に見える）ので、確認を促す入口として行を出す
                            // TODO(39-D-5) 行を出す条件。「招待が 0 より多い」または「確認済みではない」（|| と !）
                            if (uiState.pendingInviteCount > 0 || !uiState.isEmailVerified) {
                                DropdownMenuItem(
                                    // 確認済みなら件数、未確認なら「要メール確認」（値を返す if。§5-(68)）
                                    text = {
                                        Text(
                                            if (uiState.isEmailVerified) "届いた招待(${uiState.pendingInviteCount})"
                                            else "届いた招待（要メール確認）"
                                        )
                                    },
                                    // TODO(36-D-3-4) 下の「カレンダーの管理」と同じ形。先にメニューを閉じてから、呼び鈴 onInvitesClick を鳴らす
                                    onClick = {
                                        viewModel.closeCalendarMenu()
                                        onInvitesClick()
                                    },
                                )
                            }

                            // 別の画面へ行く 2 つ。先にメニューを閉じておかないと、戻ってきたとき開いたままになる
                            DropdownMenuItem(
                                text = { Text("＋ 新しいカレンダー") },
                                onClick = {
                                    viewModel.closeCalendarMenu()
                                    onCreateCalendarClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("カレンダーの管理") },
                                onClick = {
                                    viewModel.closeCalendarMenu()
                                    onManageCalendarsClick()
                                },
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onSetting) {
                        Text("設定⚙️")
                    }

                }

            )
        },
        floatingActionButton = {
            // TODO(38-B-2) 「＋」を出す条件。誰の：状態の ／ 何か：書き換えてよいとき（38-B-1 の逆。! は要らない）
            //   出さないときは何も描かない（Scaffold は空っぽの floatingActionButton を許す）
            if (uiState.canEdit) {
                FloatingActionButton(onClick = onAddClick) {
                    Text("＋")
                }
            }
        },
    ) { innerPadding ->
        val message = uiState.errorMessage

        val zone = ZoneId.systemDefault()
        val dayItems = uiState.items.filter { item ->
            val date = when (item) {
                is ScheduleItem.Event -> item.startAt
                is ScheduleItem.Task -> item.dueAt
            }.atZone(zone).toLocalDate()
            date == uiState.selectedDate
        }

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

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator()
                    message != null -> Text(message)
                    dayItems.isEmpty() -> Text("この日の予定はありません")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = dayItems,
                            key = { item -> item.id },
                        ) { item ->
                            ScheduleItemRow(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                onToggleCompleted = viewModel::toggleCompleted,
                                // TODO(38-B-5) 誰の：状態の ／ 何か：書き換えてよいか（38-B-2 と同じものをそのまま渡す）
                                canEdit = uiState.canEdit,
                            )
                        }
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
    // TODO(38-B-3) 完了チェックを押せるか（Step 38）。型は「はい／いいえ」。既定値は付けない
    //   （付けると呼ぶ側が渡し忘れてもビルドが通り、閲覧者でも押せるままになる）
    canEdit: Boolean,
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
                        onCheckedChange = { onToggleCompleted(item) },
                        // TODO(38-B-4) 押せる条件。誰の：この行が受け取った ／ 何か：canEdit
                        //   false だと灰色になり、押しても onCheckedChange が鳴らない（MemberListScreen の招待ボタンと同じ enabled）
                        enabled = canEdit,
                    )
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
                    canEdit = true,   // Preview は編集できる人の見た目（Claude が記入済み）
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
    val today = LocalDate.now()


    val cells: List<LocalDate?> = (0 until 42).map { index ->
        val day = index - shift + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasItems = date != null && date in datesWithItems,
                        onClick = { if (date != null) onDateClick(date) },
                        modifier = Modifier.weight(1f)
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
    isToday: Boolean,
    hasItems: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = date != null,
        // TextButton は押しやすさのために自前の上下余白（約 16dp）を持っている。
        // 升目は高さを 56dp に固定しているので、その余白ぶんだけ中身が入りきらず点が切れる。
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.height(56.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 選択中の日だけ丸く塗る（B-3）。選択していない日も透明な Surface を置いたままにして、
            // 選択した瞬間に升目の大きさが変わらないようにする。
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                // 今日は枠線の丸で常に印を付ける（grammar §3-(97)）。
                // 選択中は塗りつぶしになるので、そのときは枠を出さない（同じ色の線が埋もれるため）。
                border = if (isToday && !isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = date?.dayOfMonth?.toString() ?: "",
                        style = MaterialTheme.typography.bodyMedium,

                        fontWeight = if (isToday) FontWeight.Bold
                        else FontWeight.Normal,
                    )
                }
            }
            // 点が無い日も空白を 1 文字置いて高さを揃える（空文字だと段の高さが変わる）。
            // 空白のときは色が見えないので、色の出し分けは不要。
            Text(
                text = if (hasItems) "❤️" else " ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
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

/**
 * ▼ のメニューの ✓。選ばれていない行でも**場所だけは取る**（中身を空にする）ので、
 * ✓ の有無で行の文字の位置がずれない（Step 24 で選択中でない日にも透明な Surface を置いたのと同じ考え方）。
 */
@Composable
private fun CheckMark(visible: Boolean) {
    Text(if (visible) "✓" else "")
}
