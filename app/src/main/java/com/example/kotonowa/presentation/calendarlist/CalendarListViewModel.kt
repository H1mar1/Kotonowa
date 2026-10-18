package com.example.kotonowa.presentation.calendarlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.SelectedCalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * カレンダー一覧画面の頭脳。
 *
 * お手本は `presentation/calendar/CalendarViewModel`。ただし月の移動が無いので、
 * 購読を張り直す必要がなく `observeJob` は持たない（init で 1 回始めるだけ）。
 */
@HiltViewModel
class CalendarListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
    private val selectedCalendarRepository: SelectedCalendarRepository, // 選択中のカレンダーの掲示板（Step 33-A）
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarListUiState())
    val uiState: StateFlow<CalendarListUiState> = _uiState.asStateFlow()

    init {
        observeCalendars()
    }

    private fun observeCalendars() {

        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "ログイン情報が取得できませんでした")
            }
            return
        }

        viewModelScope.launch {
            try {
                calendarRepository.observeMyCalendars(uid)
                    .collect { list ->
                        _uiState.update { state ->
                            state.copy(
                                calendars = list,
                                isLoading = false,
                            )
                        }

                    }
            } catch (e: CancellationException) {
                // 意図して取り止めた合図なので握り潰さず上へ通す（grammar §2-(91)）。
                // Exception より上に置くこと（順番が命。§6-㉕）
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "カレンダーの読み込みに失敗しました",
                    )
                }
            }
        }
    }

    /**
     * 行がタップされたカレンダーを掲示板に貼る。`null` は「マイカレンダー（個人）」。
     * 画面遷移は知らない（呼び鈴を鳴らすのは画面の仕事。Step 22 / 25 の logout と同じ分担）。
     */
    fun selectCalendar(calendar: Calendar?) {
        // TODO(33-B-1) 掲示板に貼る（33-A で作った select を呼ぶ
        selectedCalendarRepository.select(calendar)
    }
}
