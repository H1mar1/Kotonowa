package com.example.kotonowa.presentation.calendar.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

/**
 * 予定/タスクの作成画面の頭脳。
 *
 * 画面（Composable）は入力欄を描いて「変わったよ」と伝えるだけで、
 * 入力の保持・保存・エラー判断はすべてここが行う。
 *
 * 保存すると [ScheduleRepository] 経由で Firestore に書き込まれ、
 * 一覧側（CalendarViewModel）が購読している Flow に自動で流れる。
 * つまり保存後に一覧を読み直す処理は要らない。
 */
@HiltViewModel
class ScheduleEditViewModel @Inject constructor(

    private val scheduleRepository: ScheduleRepository, // 予定/タスクを出し入れする窓口
    private val authRepository: AuthRepository, // ログイン中のユーザーを知る窓口
) : ViewModel() {


    private val _uiState = MutableStateFlow(ScheduleEditUiState())
    val uiState: StateFlow<ScheduleEditUiState> = _uiState.asStateFlow()

    private val calendarId = authRepository.currentUser?.uid
    fun onItemTypeChange(value: ScheduleItemType) {
        _uiState.update {
            it.copy(
                itemType = value,
                allDay = it.allDay && value == ScheduleItemType.EVENT
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onAllDayChange(value: Boolean) {
        _uiState.update { it.copy(allDay = value) }
    }

    fun onStartDateChange(value: LocalDate) {
        _uiState.update { it.copy(startDate = value) }
    }

    fun onStartTimeChange(value: LocalTime) {
        _uiState.update { it.copy(startTime = value) }
    }

    fun onEndDateChange(value: LocalDate) {
        _uiState.update { it.copy(endDate = value) }
    }

    fun onEndTimeChange(value: LocalTime) {
        _uiState.update { it.copy(endTime = value) }
    }


    /**
     * 入力された内容を 1 件保存する。
     *
     * 保存できたら [ScheduleEditUiState.isSaved] を立てる。
     * 画面はそれを見て前の画面に戻る。一覧は Flow 経由で勝手に更新されるので、
     * ここから一覧に「増えたよ」と伝える必要はない。
     */
    fun save() {

        val id = calendarId ?: return
        val state = _uiState.value
        if (state.isSaving) return
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "タイトルが入っていません") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val now = Instant.now()

            val startTime = if (state.allDay) LocalTime.MIN else state.startTime
            val endTime = if (state.allDay) LocalTime.of(23, 59) else state.endTime
            val item = when (state.itemType) {
                ScheduleItemType.EVENT -> ScheduleItem.Event(
                    id = UUID.randomUUID().toString(),
                    calendarId = id,
                    title = state.title,
                    description = state.description.ifBlank { null },
                    createdBy = id,
                    reminderMinutesBefore = null,
                    updatedAt = now,
                    startAt = state.startDate.toInstant(startTime),
                    endAt = state.endDate.toInstant(endTime),
                    allDay = state.allDay,
                )

                ScheduleItemType.TASK -> ScheduleItem.Task(
                    id = UUID.randomUUID().toString(),
                    calendarId = id,
                    title = state.title,
                    description = state.description.ifBlank { null },
                    createdBy = id,
                    reminderMinutesBefore = null,
                    updatedAt = now,
                    dueAt = state.startDate.toInstant(state.startTime),
                    isCompleted = false,
                )
            }

            scheduleRepository.addItem(item)
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaved = false,
                            errorMessage = "保存できていません"
                        )
                    }
                }
        }
    }
}

/** 画面で選んだ「日付」と「時刻」を、端末のタイムゾーンで 1 つの [Instant] にする。 */
private fun LocalDate.toInstant(time: LocalTime): Instant =
    atTime(time).atZone(ZoneId.systemDefault()).toInstant()