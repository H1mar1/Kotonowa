package com.example.kotonowa.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 通知設定の窓口（Step 45）。
 *
 * 「どの共有カレンダーの通知を止めているか」を、名刺（users/{uid}）の mutedCalendarIds に持つ。
 * Cloud Functions は送る前にこの一覧を見て、止めている人には送らない。
 * 招待の通知はカレンダーに入る前の話なので、ここでは止めない（いつも届く）。
 */
interface NotificationSettingRepository {

    /**
     * 通知を止めているカレンダーの id の集まりを見張る。
     * 1 つも止めていなければ空の Set が流れる。
     * Set なのは、画面で「このカレンダーは入っているか」を何度も調べるため（Step 23 の datesWithItems と同じ）。
     */
    fun observeMutedCalendarIds(uid: String): Flow<Set<String>>

    /**
     * そのカレンダーの通知を止める（muted = true）／再開する（muted = false）。
     */
    suspend fun setCalendarMuted(uid: String, calendarId: String, muted: Boolean): Result<Unit>
}
