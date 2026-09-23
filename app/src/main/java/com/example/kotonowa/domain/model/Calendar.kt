package com.example.kotonowa.domain.model

import java.time.Instant

/**
 * カレンダー（予定/タスクを置く「部屋」）1 つぶんの情報。
 *
 * Firestore の `calendars/{calendarId}` ドキュメントに対応する（要件定義書 §4）。
 * 部屋の「中身」（予定/タスク）はここには入らない。中身は `events` コレクションに
 * フラットに置かれていて、`ScheduleItem.calendarId` がこの部屋を指している。
 *
 * 誰が入っているかの名簿もここには入らない。名簿は [CalendarMember]。
 */
data class Calendar(
    //  id        … この部屋の ID。個人用カレンダーではそのユーザーの uid と同じ値になる
    //  name      … 画面に出す名前（例：「家族」「マイカレンダー」）
    //  ownerUid  … 作った人の uid。ロール変更や部屋の削除ができる人
    //  type      … 個人用か共有用か
    //  color     … 一覧で色分けするための色。Firestore には文字列で入れる（例："#4CAF50"）
    //  createdAt … 作られた時刻
    val id: String,
    val name: String,
    val ownerUid: String,
    val type: CalendarType,
    val color: String,
    val createdAt: Instant,
)

/**
 * カレンダーの種類。
 *
 * 一覧画面で「マイカレンダー」と「共有カレンダー」を分けて出すために使う。
 */
enum class CalendarType {
    //  ・自分専用。ID はそのユーザーの uid そのもの（要件定義書 §4）
    //  ・複数人で共有する
    PERSONAL,
    SHARED,
}
