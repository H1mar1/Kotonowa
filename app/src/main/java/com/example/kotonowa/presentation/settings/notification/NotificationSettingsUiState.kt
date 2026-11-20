package com.example.kotonowa.presentation.settings.notification

import com.example.kotonowa.domain.model.Calendar

/**
 * 通知設定画面の状態（Step 45）。
 *
 * カレンダーの一覧と「止めている id の集まり」を**別々に**持つ。どちらも Firestore から別々に流れてくるため。
 * 各行のスイッチが ON か OFF かは、画面が「この行の id は mutedCalendarIds に入っているか」で決める
 * （新しい情報を作るのではなく、今ある 2 つを照らし合わせるだけなので画面の仕事。Step 23 の dayItems と同じ考え方）。
 *
 * @property calendars 自分が入っている共有カレンダー
 * @property mutedCalendarIds 通知を止めているカレンダーの id
 * @property isLoading 開いた瞬間は一覧がまだ届いていないので true 始まり（Step 31 と同じ）
 */
data class NotificationSettingsUiState(
    val calendars: List<Calendar> = emptyList(),
    val mutedCalendarIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
