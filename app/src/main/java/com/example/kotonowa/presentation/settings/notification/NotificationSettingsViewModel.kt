package com.example.kotonowa.presentation.settings.notification

import android.widget.Switch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.NotificationSettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 通知設定画面の頭脳（Step 45）。
 *
 * 見張るのは 2 本：自分のカレンダー一覧（Step 31 と同じ）と、名刺の mutedCalendarIds（45-A）。
 * スイッチが押されたら名刺を書き換えるだけ。画面のスイッチが切り替わるのは、
 * 書き換えた名刺が見張りから流れてきたとき（Step 21 の完了チェックと同じ。画面はスイッチの状態を持たない）。
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    // 自分の uid（変わらないので 1 回読むだけ。Step 25 の設定画面と同じ）
    private val uid: String? = authRepository.currentUser?.uid

    init {
        observeCalendars()
        observeMuted()
    }

    /** 自分が入っている共有カレンダーを見張る。形は CalendarListViewModel（Step 31）と同じ */
    private fun observeCalendars() {
        val myUid = uid
        if (myUid == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ログイン情報が取得できませんでした"
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                calendarRepository.observeMyCalendars(myUid).collect { list ->
                    _uiState.update { it.copy(calendars = list, isLoading = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "カレンダーの読み込みに失敗しました"
                    )
                }
            }
        }
    }

    /** 名刺の「通知を止めているカレンダー」を見張る */
    private fun observeMuted() {
        val myUid = uid ?: return
        viewModelScope.launch {
            try {
                notificationSettingRepository.observeMutedCalendarIds(myUid).collect { ids ->
                    // TODO(45-B-1) 誰の：状態（_uiState）を ／ 何をする：mutedCalendarIds を、流れてきた ids に書き換える
                    //   上の observeCalendars の 1 行（calendars = list）と同じ形。項目名と箱の名前を替える
                    _uiState.update { it.copy(mutedCalendarIds = ids) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 読めなくてもスイッチが全部 ON に見えるだけなので、一覧は消さない（エラーは出さない）
            }
        }
    }

    /**
     * スイッチが押された。
     *
     * @param enabled スイッチの新しい向き。true ＝ 通知を受け取る（ON）
     */
    fun onNotificationToggle(calendarId: String, enabled: Boolean) {
        val myUid = uid ?: return
        viewModelScope.launch {
            // TODO(45-B-2) 誰の：通知設定の窓口（notificationSettingRepository）の ／ 何をする：setCalendarMuted を呼ぶ
            //   渡すもの：myUid、calendarId、muted = ？
            //   ⚠️ 向きが逆。スイッチの enabled は「受け取る」、muted は「止める」。受け取る(true)なら止めない(false)
            //   失敗しても見張りが元の向きを流すだけなので、結果は見ない（Step 21 の toggleCompleted と同じ）
            notificationSettingRepository.setCalendarMuted(myUid, calendarId, muted = !enabled)
        }
    }
}
