package com.example.kotonowa.presentation.calendarlist.create

import android.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.CalendarType
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * カレンダー作成画面の頭脳。
 *
 * お手本は `presentation/calendar/edit/ScheduleEditViewModel` の「作成モード」の部分。
 * 保存すると [CalendarRepository.createCalendar] が部屋とオーナーの名簿の行をまとめて書き、
 * 一覧側（CalendarListViewModel）が購読している Flow に自動で流れる。
 * だから保存後に一覧を読み直す処理は要らない。
 */
@HiltViewModel
class CalendarCreateViewModel @Inject constructor(
    private val authRepository: AuthRepository,         // ログイン中のユーザーを知る窓口
    private val calendarRepository: CalendarRepository, // カレンダーを出し入れする窓口
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarCreateUiState())
    val uiState: StateFlow<CalendarCreateUiState> = _uiState.asStateFlow()

    // ---- 32-B-1 入力の受け取り --------------------------------------------

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onColorChange(value: String) {
        _uiState.update { it.copy(color = value) }
    }

    // ---- 32-B-2 保存 ---------------------------------------------------

    fun save() {
        val uid = authRepository.currentUser?.uid ?: return
        val state = _uiState.value

        // 門番①：通信中なら何もしない（二度押し防止）
        if (state.isSaving) return

        // 門番②：名前が空なら弾く。isSaving を立てる前に弾くので、あとで戻さなくてよい
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "名前が入っていません") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val calendar = Calendar(
                id = UUID.randomUUID().toString(),
                name = state.name,
                ownerUid = uid,
                type = CalendarType.SHARED ,
                color = state.color,
                createdAt = Instant.now(),
            )

            calendarRepository.createCalendar(calendar)
                .onSuccess {
                    _uiState.update { it.copy(isSaved = true, isSaving = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "保存できませんでした") }
                }
        }
    }
}
