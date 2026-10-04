package com.example.kotonowa.presentation.calendarlist

import com.example.kotonowa.domain.model.Calendar

/**
 * カレンダー一覧画面の「今の状態」をまとめて表したもの。
 *
 * 画面（Composable）はこの箱だけを見て描く。中身を書き換えるのは
 * [CalendarListViewModel] の仕事。お手本は `presentation/calendar/CalendarUiState`。
 */
data class CalendarListUiState(

    val calendars: List<Calendar> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
