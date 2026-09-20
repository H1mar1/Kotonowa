package com.example.kotonowa.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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

    private var observeJob: Job? = null

    init {
        observeMonth(_uiState.value.currentMonth)
    }
    /** 前の月へ。升目の表示を変え、その月を購読し直す。 */
    fun showPreviousMonth(){
        val month=_uiState.value.currentMonth.minusMonths(1)
        _uiState.update { it.copy(currentMonth = month) }
        observeMonth(month)
    }

    /** 次の月へ。 */
    fun showNextMonth(){
       val month=_uiState.value.currentMonth.plusMonths(1)
        _uiState.update { it.copy(currentMonth = month) }
        observeMonth(month)
    }

    /** 升目の日付を選ぶ。下の一覧を絞るだけなので、購読は張り直さない。 */
    fun selectDate(date: LocalDate){
        _uiState.update { it.copy(selectedDate = date) }
    }


    /**
     * 今月の予定/タスクを監視し始める。
     */
    private fun observeMonth(month: YearMonth) {
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

        val from = month.atDay(1).atStartOfDay(zone).toInstant()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

        observeJob?.cancel()

        _uiState.update { it.copy(isLoading = true) }

        observeJob =  viewModelScope.launch {
            try {
                scheduleRepository.observeItems(calendarId, from, to)
                    .collect { list ->
                        val dates = list.map { item ->
                            when (item) {
                                is ScheduleItem.Event -> item.startAt
                                is ScheduleItem.Task -> item.dueAt
                            }.atZone(zone).toLocalDate()
                        }.toSet()

                        _uiState.update { state ->
                            state.copy(
                                items = list,
                                datesWithItems = dates,
                                isLoading = false,
                            )
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

    fun toggleCompleted(task: ScheduleItem.Task){
        viewModelScope.launch {
            val update=task.copy(
                isCompleted = !task.isCompleted,
                updatedAt = Instant.now(),
            )
            scheduleRepository.updateItem(update)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "更新できませんでした") }
                }
        }
    }

    /** ログアウトする。画面遷移は知らない（旗も立てず、呼ばれたら消すだけ）。 */
    fun logout(){
        authRepository.logout()
    }


}
