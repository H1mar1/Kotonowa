package com.example.kotonowa.presentation.calendarlist.create

/**
 * カレンダーの色の候補。画面ではこの中から 1 つ選ぶ（32-C）。
 *
 * Firestore の `color` は文字列で保存するので（`Calendar.color`）、ここも文字列で持つ。
 * 自由入力にしないのは、"#4CAF5" のような打ち間違いを最初から起こさせないため。
 */
val CALENDAR_COLORS = listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0")

/**
 * カレンダー作成画面の「今の状態」をまとめて表したもの。
 *
 * 画面（Composable）はこの箱だけを見て描く。中身を書き換えるのは
 * [CalendarCreateViewModel] の仕事。お手本は `presentation/calendar/edit/ScheduleEditUiState`。
 */
data class CalendarCreateUiState(

    // 入力中の値
    val name: String = "",
    val color: String = CALENDAR_COLORS.first(),

    // 保存の状態（ScheduleEditUiState と同じ 3 つ）
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)
