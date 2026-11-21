package com.example.kotonowa.presentation.tasks

import com.example.kotonowa.domain.model.ScheduleItem

/**
 * タスク一覧の 1 行（Step 47）。
 *
 * タスクそのものに加えて、「どのカレンダーのタスクか」と「チェックを押せるか」を一緒に持つ。
 * どちらもタスクの書類には無い情報（カレンダー名は calendars、押せるかは名簿の肩書きから決まる）なので、
 * 頭脳が束ねるときに付けておく。画面は行を見るだけで描ける。
 *
 * @property task タスク本体
 * @property calendarName 行に小さく出すカレンダーの名前（マイカレンダー／school など）
 * @property canEdit 完了チェックを押せるか（閲覧者のカレンダーなら false。Step 38 と同じ基準）
 */
data class TaskRowItem(
    val task: ScheduleItem.Task,
    val calendarName: String,
    val canEdit: Boolean,
)

/** 上の切り替え。未完了のタスクを見るか、完了したタスクを見るか */
enum class TaskFilter {
    INCOMPLETE,
    COMPLETED,
}

/**
 * タスク一覧画面の状態（Step 47）。
 *
 * rows には未完了も完了も全部入れておき、どちらを出すか・どう並べるかは画面が filter を見て決める
 * （新しい情報を作るのではなく、今ある行を絞って並べるだけなので画面の仕事。Step 23 の dayItems と同じ考え方）。
 */
data class TaskListUiState(
    // TODO(47-B-1) 4 つの箱。型と既定値を書く
    //   rows … 全カレンダーのタスクの行の並び。最初は空の並び（CalendarListUiState の calendars と同じ形）
    //   filter … 今どちらを見ているか。最初は「未完了」
    //   isLoading … 開いた瞬間は読み込み中なので「はい」始まり
    //   errorMessage … エラーの一言。最初は無し
    val rows: List<TaskRowItem> = emptyList(),
    val filter: TaskFilter = TaskFilter.INCOMPLETE,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
