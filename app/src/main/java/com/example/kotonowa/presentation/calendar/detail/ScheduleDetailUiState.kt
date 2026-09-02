package com.example.kotonowa.presentation.calendar.detail

import com.example.kotonowa.domain.model.ScheduleItem

/**
 * 予定/タスクの詳細画面の「今の状態」をまとめたもの。
 *
 * 画面（Composable）はこの箱だけを見て描き、
 * 中身を書き換えるのは ScheduleDetailViewModel の仕事。
 *
 * 開いた瞬間に getItem で 1 件読み込むので isLoading は true 始まり
 * （CalendarUiState は false 始まりだったのと逆）。
 * 削除は ScheduleEditUiState の isSaving / isSaved と同じ形で
 * isDeleting / isDeleted を使う。
 */
data class ScheduleDetailUiState(

    val item: ScheduleItem? = null,

    val isLoading: Boolean = true,

    val isDeleting: Boolean = false,

    val isDeleted: Boolean = false,

    val errorMessage: String? = null,


    )
