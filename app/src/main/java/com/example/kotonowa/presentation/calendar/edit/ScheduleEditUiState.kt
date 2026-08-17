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

    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,

    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1),
    val endDate: LocalDate = startDate,
    val endTime: LocalTime = startTime.plusHours(1),

    )
