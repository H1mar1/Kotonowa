package com.example.kotonowa.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.impl.utils.forName
import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import com.example.kotonowa.domain.repository.SelectedCalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
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
    private val selectedCalendarRepository: SelectedCalendarRepository, // 選択中のカレンダーの掲示板（Step 33-A）
    private val calendarRepository: CalendarRepository, // ▼ のメニューに並べるカレンダーを読む窓口（Step 33-H）
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())

    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    /** ログイン中のユーザーの uid。マイカレンダー（個人）の id でもある（要件定義書 §4）。 */
    private val uid = authRepository.currentUser?.uid

    /**
     * 今表示しているカレンダーの id。掲示板が貼り替わるたびに入れ直すので `var`。
     * Step 33 までは `val calendarId = uid` の決め打ちだった。
     */
    private var calendarId: String? = uid

    private var observeJob: Job? = null

    init {
        // 掲示板を見張る。StateFlow は collect した瞬間に「今貼ってある紙」を 1 回流してくれるので、
        // 最初の observeMonth もここから始まる（grammar §2-(121)）。
        viewModelScope.launch {
            selectedCalendarRepository.selected.collect { calendar ->
                // TODO(33-C-2) 誰の：この頭脳の calendarId に ／ 何をする：貼られたカレンダーの id を入れる。
                //              白紙（null）なら自分の uid（§4-⑮ の ?:）
                calendarId = calendar?.id?:uid

                // TODO(33-C-2) 誰の：_uiState の ／ 何をする：calendarName を、貼られたカレンダーの名前に書き換える。
                //              白紙なら "マイカレンダー"（§4-⑮、§2-(58) の update）
                _uiState.update { it.copy(calendarName = calendar?.name ?: "マイカレンダー") }

                // ▼ のメニューの ✓ 用。白紙なら null のまま（selectedCalendarId は String? なので ?: は要らない）
                _uiState.update { it.copy(selectedCalendarId = calendar?.id) }

                // TODO(33-C-2) 誰の：この頭脳の observeMonth を ／ 何をする：今表示している月で呼び直す（＝購読の張り直し）
                observeMonth(_uiState.value.currentMonth)
            }
        }

        // ▼ のメニューに並べるカレンダーも見張る（Step 33-H）
        observeCalendars()
    }

    /**
     * 自分が入っている共有カレンダーを見張り、▼ のメニュー用に UiState へ入れる。
     * お手本は CalendarListViewModel.observeCalendars（Step 31）。月の移動とは関係ないので observeJob は使わない。
     */
    private fun observeCalendars() {
        val myUid = uid ?: return

        viewModelScope.launch {
            try {
                calendarRepository.observeMyCalendars(myUid).collect { list ->
                    _uiState.update { it.copy(calendars = list) }
                }
            } catch (e: CancellationException) {
                throw e   // 意図して取り止めた合図（grammar §2-(91)）。Exception より上に置く
            } catch (e: Exception) {
                // メニューに共有カレンダーが出ないだけで、ホームの予定は読める。
                // 画面全体のエラー（errorMessage）にはしない（ログアウトの瞬間の PERMISSION_DENIED もここに来る）
            }
        }
    }

    // ---- ▼ のメニュー（Step 33-H） -------------------------------------

    /** ▼ が押された。お手本は ScheduleEditViewModel.onReminderMenuOpen（Step 26）。 */
    fun openCalendarMenu() {
        _uiState.update { it.copy(isCalendarMenuOpen = true) }
    }

    /** メニューの外側や「戻る」が押された。 */
    fun closeCalendarMenu() {
        _uiState.update { it.copy(isCalendarMenuOpen = false) }
    }

    /**
     * メニューでカレンダーが選ばれた。`null` はマイカレンダー。
     * 掲示板に貼れば、上の init の collect が気づいて購読を張り直す（33-C の仕組みがそのまま動く）。
     */
    fun selectCalendar(calendar: Calendar?) {
        selectedCalendarRepository.select(calendar)
        // 選んでも自動では閉じないので、自分で閉じる（grammar §3-(107)）
        closeCalendarMenu()
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
        // TODO(33-C-3) calendarId は var になったので、一度 val に写してから使う（§4-(65) の var の項）
        val id = calendarId

        // ログインしていなければ id が無く、読み込みようがない
        if (id == null) {
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

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        observeJob =  viewModelScope.launch {
            try {
                scheduleRepository.observeItems(id, from, to)
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
            } catch (e: CancellationException) {
                // 月を送ったときの observeJob?.cancel() で飛んでくる。
                // 失敗ではなく「意図して取り止めた」合図なので、握り潰さず上へ通す（grammar §2-(91)）。
                throw e
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
}
