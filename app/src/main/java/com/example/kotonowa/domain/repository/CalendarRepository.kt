package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.CalendarMember
import com.example.kotonowa.domain.model.MemberRole
import kotlinx.coroutines.flow.Flow

/**
 * カレンダー（部屋）とメンバー（名簿）まわりで「できること」の約束だけを並べたもの。
 *
 * お手本は同じフォルダの `ScheduleRepository`。「どうやるか」は書かず、
 * 実際のやり方は data 層の `CalendarRepositoryImpl` が Firestore で実装する（Step 30）。
 *
 * ⚠️ `memberUids`（検索用の配列）はここには一切出てこない。
 * 名簿に誰が載っているかを写しただけの重複情報なので、data 層が裏で面倒を見る
 * （`events` の `type` / `sortAt` と同じ扱い）。
 */
interface CalendarRepository {

    /**
     * 共有カレンダーを新しく作る。
     *
     * 実装（Step 30）では次の 3 つを**まとめて**行う。
     *  ① calendars/{id} の書類を作る
     *  ② calendars/{id}/members/{ownerUid} に OWNER として名簿の行を作る
     *  ③ memberUids に作った人の uid を入れる
     * 「誰がオーナーか」は引数の calendar.ownerUid から分かるので、別の引数は要らない。
     */
    suspend fun createCalendar(calendar: Calendar): Result<Unit>
    /**
     * 自分が入っているカレンダーを監視する。
     *
     */
    fun observeMyCalendars(uid: String): Flow<List<Calendar>>

    /**
     * あるカレンダーの名簿を監視する。
     *
     */
    fun observeMembers(calendarId: String): Flow<List<CalendarMember>>

    /**
     * 名簿に 1 人足す（Step 36 の招待の承認で使う）。
     *
     */
    suspend fun addMember(calendarId: String, member: CalendarMember): Result<Unit>

    /**
     * 名簿の肩書きを変える（Step 34。オーナーだけが使える）。
     *
     *  ⚠️ ここは CalendarMember を丸ごと受け取らない。
     *     変えたいのは role だけで、joinedAt や invitedBy まで上書きさせたくないため。
     */
    suspend fun updateMemberRole(calendarId: String, uid: String, role: MemberRole): Result<Unit>

    /**
     * 名簿から 1 人外す（Step 34）。
     *
     * 消すものの中身は受け取らず、id だけで足りる。
     */
    suspend fun removeMember(calendarId: String, uid: String): Result<Unit>
}
