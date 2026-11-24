package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.ChatMessage
import com.example.kotonowa.domain.model.ChatRoomId
import com.example.kotonowa.domain.model.DirectChat
import kotlinx.coroutines.flow.Flow

/**
 * チャットの窓口（Phase 5 / Step 49）。
 *
 * ① ではグループ（共有カレンダーごとのトーク）だけだったので、引数は calendarId だった。
 * ③ で DM が増えたので、「どのトークか」の札（ChatRoomId。グループか DM か）で受け取る（Step 51-B）。
 * 送った人の uid と名前は、data 層が「今ログインしている自分」から付ける（Step 44 の updatedBy と同じ。頭脳に渡させない）。
 */
interface ChatRepository {

    /**
     * そのカレンダーのトークの発言を、古い順に見張る。
     * 誰かが送るたびに、増えた一覧が流れてくる（observeItems / observeTasks と同じ「見張り」）。
     */
    // TODO(49-A-2) 戻り値の型：発言（ChatMessage）の並びが流れてくる管（observeTasks の戻り値と同じ形。中身の型だけ違う）
    // TODO(51-B-1) 3 つの係の 1 つ目の引数を「文字の calendarId」から「どのトークかの札」に変える
    //   名前は roomId、型は 51-A で作った sealed interface の名前（下の 2 つも同じ）
    fun observeMessages(roomId: ChatRoomId): Flow<List<ChatMessage>>

    /**
     * そのカレンダーのトークに、本文 text を送る。
     * 成功したかどうかだけ分かればよい（中身は見張りの方に流れてくる。Step 17-F と同じ）。
     */
    // TODO(49-A-3) 戻り値の型：成功／失敗だけを入れた封筒（setCalendarMuted の戻り値と同じ）
    suspend fun sendMessage(roomId: ChatRoomId, text: String): Result<Unit>

    /**
     * そのカレンダーのトークから、発言 messageId を消す（Step 50。跡を残さず書類ごと）。
     * 消せるのは送った本人だけ（ルールが確かめる）。消えた発言は見張りの方から自然に消える（Step 18 と同じ）。
     */
    // TODO(50-A-1) 戻り値の型：成功／失敗だけを入れた封筒（すぐ上の sendMessage と同じ）
    suspend fun deleteMessage(roomId: ChatRoomId, messageId: String): Result<Unit>

    // ---- DM の部屋（Step 51-C） ------------------------------------------------

    /**
     * 相手（otherUid）との DM の部屋を開く。まだ無ければ作ってから開く。答えは部屋の id（chatId）。
     *
     * 部屋の id は 2 人の uid から決まるので、同じ 2 人で 2 つ目の部屋はできない（招待状の id と同じ考え方）。
     * 作るときに、自分と相手の名前を部屋に写す（相手の名前は、メンバー画面がもう知っている名簿の行の名前を渡してもらう）。
     * viaCalendarId は「どの共有カレンダーで一緒か」。ルールが作るときにだけ確かめる（要件定義書 §3.6）。
     */
    // TODO(51-C-1) 戻り値の型：部屋の id（文字）を入れた封筒。成功／失敗も分かるようにする
    //   お手本は sendMessage の Result<Unit>。中身が「何も無い（Unit）」から「文字」に変わるだけ
    suspend fun openDirectChat(otherUid: String, otherName: String, viaCalendarId: String): Result<String>

    /**
     * 自分が入っている DM の部屋の並びを見張る（トーク一覧に出す。51-H）。
     * 誰かが部屋を作ったり、最後の発言が変わったりするたびに流れてくる。
     */
    // TODO(51-C-2) 戻り値の型：DM の部屋（DirectChat）の並びが流れてくる管（すぐ上の observeMessages と同じ形。中身の型だけ違う）
    fun observeMyDirectChats(myUid: String): Flow<List<DirectChat>>
}
