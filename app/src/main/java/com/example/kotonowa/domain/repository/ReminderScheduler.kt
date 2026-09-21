package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.ScheduleItem

/**
 * リマインダー通知の予約まわりで「できること」の約束だけを並べたもの（お品書き）。
 *
 * ここには「どうやるか」は書かない。実際のやり方は data 層の
 * `ReminderSchedulerImpl` が WorkManager を使って実装する。
 * こうしておくと ViewModel は WorkManager の存在を知らずに済み、
 * 依存の向き（presentation → domain ← data）を保てる。
 */
interface ReminderScheduler {

    /**
     * 予定/タスクのリマインダーを予約する。
     *
     * 同じ予定の予約が既にあれば置き換える。
     * [ScheduleItem.reminderMinutesBefore] が null（通知なし）のときや、
     * 通知時刻が既に過ぎているときは、予約せずに取り消すだけ。
     */
    // ★1 予定 1 件を受け取る。返すものは無い
    fun schedule(item: ScheduleItem)

    /**
     * 予定/タスクの予約を取り消す。削除したときや、通知なしに変えたときに使う。
     */
    // ★2 どの予定の予約かを指す値。id は文字列（ScheduleItem.kt:17）
    fun cancel(itemId: String)
}
