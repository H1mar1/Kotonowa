package com.example.kotonowa.presentation.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * トーク一覧の頭脳（Step 49-E）。
 *
 * お手本は `CalendarListViewModel`（Step 31）。自分が入っているカレンダーを見張るだけで、ほぼ同じ形。
 * observeMyCalendars は `memberUids` に自分がいる書類を返すので、出てくるのは**共有カレンダーだけ**
 * （マイカレンダーには `calendars` の書類が無い。Step 33）。マイカレンダーにトークが無い仕様（§3.6）とちょうど合う。
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        observeRooms()
    }

    /** 自分が入っている共有カレンダーを見張る。招待を承認したり外されたりすると、行が自動で増減する */
    private fun observeRooms() {
        val uid = authRepository.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "ログイン情報が取得できませんでした") }
            return
        }

        viewModelScope.launch {
            try {
                // TODO(49-E-2) 誰の：カレンダーの窓口（calendarRepository）の ／ 何をする：自分（uid）が入っているカレンダーを見張る
                //   CalendarListViewModel の observeCalendars と同じ呼び出し
                calendarRepository.observeMyCalendars(uid).collect { calendars ->
                    // TODO(49-E-3) 誰の：状態の箱を ／ 何をする：rooms を流れてきた calendars に、isLoading を false に書き換える（49-D-3 と同じ形）
                    _uiState.update { it.copy(rooms = calendars, isLoading = false) }
                }
            } catch (e: CancellationException) {
                throw e   // 取り止めの合図は失敗ではない（grammar §2-(91)。Exception より上に置く）
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "トークを読み込めませんでした") }
            }
        }
    }
}
