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

    /**
     * 動作確認用に、ダミーの予定を 1 件追加する。
     *
     * Step 17 で作成画面を作ったら削除する仮実装。
     */
    fun addDummyItem() {
        // TODO 8: calendarId が null なら何もせず終わる
        //   ?: return と書ける（§4-⑮ の「〜が無ければ」＋ return）。解説を参照

        // TODO 9: viewModelScope.launch { } の中で addItem を呼ぶ（§2-⑩）
        //   addItem は suspend なので「待てる場所」が要る（§2-⑨）
        //
        //   渡すのは ScheduleItem.Event（§7-㉘）。名前付き引数（§1-⑤）で全項目を埋める。
        //   ScheduleItem.kt:28-43 を見ながら、必要な項目を確認すること
        //
        //   値の決め方:
        //     id                    … UUID.randomUUID().toString()（重複しない文字列を作る）
        //     calendarId            … 上で取り出したもの
        //     title                 … "テスト予定" など好きな文言
        //     description           … null
        //     createdBy             … calendarId と同じ（＝自分の uid）
        //     reminderMinutesBefore … null
        //     updatedAt / startAt   … Instant.now()（今この瞬間）
        //     endAt                 … 1 時間後。now.plus(1, ChronoUnit.HOURS)
        //     allDay                … false
        //
        //   失敗したときは .onFailure { } で errorMessage を入れる（§4-⑳）
    }
}
