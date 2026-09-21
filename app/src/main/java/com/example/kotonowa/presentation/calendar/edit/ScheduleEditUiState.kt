package com.example.kotonowa.presentation.calendar.edit

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 作成画面で「予定」と「タスク」のどちらを作ろうとしているか。
 *
 * 画面上部の切り替えボタンで選ぶ。保存するときに
 * `ScheduleItem.Event` と `ScheduleItem.Task` のどちらを組み立てるかがこれで決まる。
 */
enum class ScheduleItemType {
    EVENT,
    TASK,
}

/**
 * 日付/時刻を選ぶダイアログのうち、いまどれを開いているか。
 *
 * 4 つの入力欄（開始・終了 × 日付・時刻）でダイアログを使い回すため、
 * 「開いている / 閉じている」ではなく「どれを開いているか」で持つ。
 * どれも開いていないときは null。
 */
enum class PickerTarget {
    START_DATE,
    START_TIME,
    END_DATE,
    END_TIME,
    DUE_DATE,
    DUE_TIME,
}

/**
 * 予定/タスクの作成画面の「今の状態」をまとめて表したもの。
 *
 * 画面（Composable）はこの箱だけを見て描き、
 * 中身を書き換えるのは ScheduleEditViewModel の仕事。
 * 入力中の値もここに持つので、画面が作り直されても入力が消えない。
 */
data class ScheduleEditUiState(

    /** 予定とタスクのどちらを作ろうとしているか。画面上部の切り替えで変わる。 */
    val itemType: ScheduleItemType = ScheduleItemType.EVENT,

    val title: String = "",
    val description: String = "",
    val allDay: Boolean = false,

   val isLoading: Boolean=false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,

    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1),
    val endDate: LocalDate = startDate,
    val endTime: LocalTime = startTime.plusHours(1),
    val dueDate: LocalDate = LocalDate.now(),
    val dueTime: LocalTime = LocalTime.of(23, 59),

    /**
     * 何分前に通知するか。null は「通知なし」。
     * ScheduleItem.reminderMinutesBefore にそのまま入る（Step 13 で用意した受け皿）。
     */
    // ★1 「数値、または通知なし」を表す型（§4-⑯）。既定は通知なし
    val reminderMinutesBefore: Int? = null,

    /**
     * リマインダーの選択メニューが開いているか。
     * ピッカーの開閉を pickerTarget で持っているのと同じ考え方（grammar §3-(107)）。
     */
    // ★2 開いている / 閉じている の 2 択。既定は閉じている
    val isReminderMenuOpen: Boolean = false,

    /** 開いているピッカー。null ならどれも開いていない。 */
    val pickerTarget: PickerTarget? = null,

    )
