package com.example.kotonowa.presentation.calendar

import com.example.kotonowa.domain.model.ScheduleItem
import java.time.LocalDate
import java.time.YearMonth

/**
 * カレンダー画面の「今の状態」をまとめて表したもの。
 *
 * 画面（Composable）はこの箱だけを見て描く。
 * 中身を書き換えるのは CalendarViewModel の仕事。
 */
data class CalendarUiState(

    val items: List<ScheduleItem> = emptyList(),

    /** 予定/タスクがある日。升目に点を打つのに使う。 */
    val datesWithItems: Set<LocalDate> = emptySet(),

    val currentMonth: YearMonth=YearMonth.now(),

    val selectedDate: LocalDate= LocalDate.now(),

    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
