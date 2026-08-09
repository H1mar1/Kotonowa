package com.example.kotonowa.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
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

    init {
        observeThisMonth()
    }

    /**
     * 今月の予定/タスクを監視し始める。
     */
    private fun observeThisMonth() {
        // ログインしていなければ calendarId が無く、読み込みようがない
        if (calendarId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ログイン情報が取得できませんでした"
                )
            }
            return
        }

        val zone = ZoneId.systemDefault()
        val month = YearMonth.now(zone)
        val from = month.atDay(1).atStartOfDay(zone).toInstant()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()


        viewModelScope.launch {
            try {
                scheduleRepository.observeItems(calendarId, from, to)
                    .collect { list ->
                        _uiState.update { state ->
                            state.copy(items = list, isLoading = false)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "予定の読み込みに失敗しました"
                    )
                }
            }
        }
    }
}
