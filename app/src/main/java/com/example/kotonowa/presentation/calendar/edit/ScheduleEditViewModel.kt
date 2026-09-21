package com.example.kotonowa.presentation.calendar.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.ReminderScheduler
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
    private val savedStateHandle: SavedStateHandle,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {


    private val _uiState = MutableStateFlow(ScheduleEditUiState())
    val uiState: StateFlow<ScheduleEditUiState> = _uiState.asStateFlow()
    private val calendarId = authRepository.currentUser?.uid

    private val itemId: String? = savedStateHandle["itemId"]

    private var originalItem: ScheduleItem? = null

    init {
        if (itemId != null) {
            load(itemId)
        }
    }

    private fun load(itemId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            scheduleRepository.getItem(itemId)
                .onSuccess { item ->
                    originalItem = item
                    val zone = ZoneId.systemDefault()

                    _uiState.update {
                        when (item) {
                            is ScheduleItem.Event -> it.copy(
                                isLoading = false,
                                itemType = ScheduleItemType.EVENT,
                                title = item.title,
                                description = item.description ?: "",

                                allDay = item.allDay,
                                startDate = item.startAt.atZone(zone).toLocalDate(),
                                startTime = item.startAt.atZone((zone)).toLocalTime(),
                                endDate = item.endAt.atZone(zone).toLocalDate(),
                                endTime = item.endAt.atZone(zone).toLocalTime(),
                            )

                            is ScheduleItem.Task -> it.copy(
                                isLoading = false,
                                itemType = ScheduleItemType.TASK,
                                title = item.title,
                                description = item.description ?: "",

                                dueDate = item.dueAt.atZone(zone).toLocalDate(),
                                dueTime = item.dueAt.atZone(zone).toLocalTime(),
                            )
                        }

                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "データの読み込みに失敗しました"
                        )
                    }
                }
        }
    }

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

    /**
     * 日付/時刻を選ぶダイアログを開く。
     *
     * どのダイアログを出すかは画面側が [PickerTarget] で伝えてくる。
     * ここは旗を立てるだけで、ダイアログを描くのは画面の仕事。
     */
    fun onPickerOpen(target: PickerTarget) {
        _uiState.update { it.copy(pickerTarget = target) }
    }

    /** 開いているダイアログを閉じる。選ばれた値は受け取らないので、何も変えずに旗だけ下ろす。 */
    fun onPickerDismiss() {
        _uiState.update { it.copy(pickerTarget = null) }
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

    fun onDueTimeChange(value: LocalTime) {
        _uiState.update { it.copy(dueTime = value) }
    }

    fun onDueDateChange(value: LocalDate) {
        _uiState.update { it.copy(dueDate = value) }
    }

    /**
     * リマインダーの選択メニューを開く。旗を立てるだけで、メニューを描くのは画面の仕事。
     * onPickerOpen と同じ形。
     */
    fun onReminderMenuOpen() {
        _uiState.update { it.copy(isReminderMenuOpen = true) }
    }

    /** メニューを閉じる（外側を押されたとき）。何も選ばれていないので値は変えない。 */
    fun onReminderMenuDismiss() {
        _uiState.update { it.copy(isReminderMenuOpen = false) }
    }

    /**
     * リマインダーを選ぶ。null は「通知なし」。
     * 選んだら自動では閉じないので、値の変更と同時に閉じる（grammar §3-(107)）。
     */
    fun onReminderChange(value: Int?) {
        _uiState.update {
            it.copy(
                reminderMinutesBefore = value,
                isReminderMenuOpen = false,
            )
        }
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
        val original = originalItem
        if (state.isSaving) return
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "タイトルが入っていません") }
            return
        }
        val startTime = if (state.allDay) LocalTime.MIN else state.startTime
        val endTime = if (state.allDay) LocalTime.of(23, 59) else state.endTime

        val startAt = state.startDate.toInstant(startTime)
        val endAt = state.endDate.toInstant(endTime)

        if (state.itemType == ScheduleItemType.EVENT && !(startAt < endAt)) {
            _uiState.update { it.copy(errorMessage = "終了は開始より後にしてください") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val now = Instant.now()

            val item = when (state.itemType) {
                ScheduleItemType.EVENT -> ScheduleItem.Event(
                    id = original?.id ?: UUID.randomUUID().toString(),
                    calendarId = original?.calendarId ?: id,
                    createdBy = original?.createdBy ?: id,
                    title = state.title,
                    description = state.description.ifBlank { null },
                    reminderMinutesBefore = state.reminderMinutesBefore,
                    updatedAt = now,
                    startAt = startAt,
                    endAt = endAt,
                    allDay = state.allDay,
                )

                ScheduleItemType.TASK -> ScheduleItem.Task(
                    id = original?.id ?: UUID.randomUUID().toString(),
                    calendarId = original?.calendarId ?: id,
                    title = state.title,
                    description = state.description.ifBlank { null },
                    createdBy = original?.createdBy ?: id,
                    reminderMinutesBefore = state.reminderMinutesBefore,
                    updatedAt = now,
                    dueAt = state.dueDate.toInstant(state.dueTime),
                    isCompleted = false,
                )
            }

            val result = if (original == null)
                scheduleRepository.addItem(item)
            else scheduleRepository.updateItem(item)

            result
                .onSuccess {
                    // 保存できてから予約する。失敗したのに予約すると、実体の無い通知が鳴る。
                    // 編集時は ExistingWorkPolicy.REPLACE で古い予約が置き換わり、
                    // 「通知なし」に変えた場合は schedule() の中で取り消される（grammar §8-(106)）。
                    reminderScheduler.schedule(item)
                    _uiState.update {
                        it.copy(isSaving = false, isSaved = true)
                    }
                }
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