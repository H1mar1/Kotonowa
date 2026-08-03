package com.example.kotonowa.domain.model

import java.time.Instant

/**
 * 予定（Event）とタスク（Task）を統一して扱うための型。
 *
 * Firestore 側は 1 つの `events` コレクションに両方を入れ、`type` フィールドで見分ける。
 * Kotlin 側では sealed class で型そのものを分けるので、`type` は持たない
 * （data 層で保存する直前に付ける）。
 *
 * 日時はすべて Instant（世界共通の時刻の一点）で持つ。
 * 表示するときだけ端末のタイムゾーンを当てて変換する。
 */
sealed class ScheduleItem {

    abstract val id: String
    abstract val calendarId: String
    abstract val title: String
    abstract val description: String?
    abstract val createdBy: String
    abstract val reminderMinutesBefore: Int?
    abstract val updatedAt: Instant

    /**
     * 予定。開始と終了の時刻を持つ。
     */
    data class Event(
        override val id: String,

        override val calendarId: String,
        override val title: String,
        override val description: String?,
        override val createdBy: String,
        override val reminderMinutesBefore: Int?,
        override val updatedAt: Instant,

        val startAt: Instant,
        val endAt: Instant,
        val allDay: Boolean,


        ) : ScheduleItem()

    /**
     * タスク。期限（1 点）と完了状態を持つ。
     */
    data class Task(
        override val id: String,

        override val calendarId: String,
        override val title: String,
        override val description: String?,
        override val createdBy: String,
        override val reminderMinutesBefore: Int?,
        override val updatedAt: Instant,

        val dueAt: Instant,
        val isCompleted: Boolean,

        ) : ScheduleItem()
}
