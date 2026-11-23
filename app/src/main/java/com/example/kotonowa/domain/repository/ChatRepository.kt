package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * チャットの窓口（Phase 5 / Step 49）。
 *
 * ① ではグループ（共有カレンダーごとのトーク）だけ。だから引数は calendarId。
 * 送った人の uid と名前は、data 層が「今ログインしている自分」から付ける（Step 44 の updatedBy と同じ。頭脳に渡させない）。
 */
interface ChatRepository {

    /**
     * そのカレンダーのトークの発言を、古い順に見張る。
     * 誰かが送るたびに、増えた一覧が流れてくる（observeItems / observeTasks と同じ「見張り」）。
     */
    // TODO(49-A-2) 戻り値の型：発言（ChatMessage）の並びが流れてくる管（observeTasks の戻り値と同じ形。中身の型だけ違う）
    fun observeMessages(calendarId: String): Flow<List<ChatMessage>>

    /**
     * そのカレンダーのトークに、本文 text を送る。
     * 成功したかどうかだけ分かればよい（中身は見張りの方に流れてくる。Step 17-F と同じ）。
     */
    // TODO(49-A-3) 戻り値の型：成功／失敗だけを入れた封筒（setCalendarMuted の戻り値と同じ）
    suspend fun sendMessage(calendarId: String, text: String): Result<Unit>
}
