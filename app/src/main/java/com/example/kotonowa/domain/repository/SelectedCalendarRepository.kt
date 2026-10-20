package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.Calendar
import kotlinx.coroutines.flow.StateFlow

/**
 * 「今どのカレンダーを表示しているか」を貼っておく掲示板の約束（お品書き）。
 *
 * ホーム（CalendarViewModel）と作成画面（ScheduleEditViewModel）が同じ掲示板を見るので、
 * 「ホームは家族を表示しているのに、作った予定は個人に入った」というズレが起きない。
 *
 * 掲示板が白紙（null）のときは**個人カレンダー**を表す。個人カレンダーは Firestore に
 * `calendars` の書類を持たない（id ＝ 自分の uid。要件定義書 §4）ため、Calendar を作らず null で表す。
 */
interface SelectedCalendarRepository {

    // TODO(33-A-1) 今選ばれているカレンダー。白紙（個人）もありうる（grammar §2-(58)）
    val selected: StateFlow<Calendar?>

    // TODO(33-A-1) カレンダーを選ぶ。個人に戻すときは null を渡せるようにする
    fun select(calendar: Calendar?)

    /** 白紙（個人）に戻す。ログアウト時に呼び、次にログインした人へ前の人の選択を残さない。 */
    fun clear()
}
