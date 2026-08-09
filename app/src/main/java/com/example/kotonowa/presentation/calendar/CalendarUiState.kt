package com.example.kotonowa.presentation.calendar

import com.example.kotonowa.domain.model.ScheduleItem

/**
 * カレンダー画面の「今の状態」をまとめて表したもの。
 *
 * 画面（Composable）はこの箱だけを見て描く。
 * 中身を書き換えるのは CalendarViewModel の仕事。
 */
data class CalendarUiState(

    val items: List<ScheduleItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
