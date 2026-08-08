package com.example.kotonowa.presentation.calendar

import androidx.lifecycle.ViewModel
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * カレンダー画面の頭脳。
 *
 * 画面（Composable）は「見た目を描くこと」だけを担当し、
 * 予定/タスクの取得・エラーの判断はすべてここが行う。
 *
 * ここが [ScheduleRepository] の管（Flow）を受け取り、
 * 流れてきた一覧を [CalendarUiState] に詰め替えて画面へ渡す。
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(

    private val scheduleRepository: ScheduleRepository, //予定/タスクを出し入れする窓口
    private val authRepository: AuthRepository, //ログイン中のユーザーを知る窓口
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())

    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val calendarId = authRepository.currentUser?.uid
}
