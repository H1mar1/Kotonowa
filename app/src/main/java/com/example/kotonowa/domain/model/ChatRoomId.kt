package com.example.kotonowa.domain.model

/**
 * 「どのトークか」（Phase 5 / Step 51）。
 *
 * グループのトークは共有カレンダーの id で、DM は DM の部屋の id で見分ける。
 * どちらも文字 1 つだが、ただの String にすると「これはカレンダーの id？ DM の id？」が型から分からず、
 * 取り違えてもビルドが通ってしまう（Step 19-D の calendarId / createdBy と同じ落とし穴）。
 * そこで仲間を 2 つだけに封をした sealed interface で包む（grammar §7-㉘。SplashUiState と同じ形）。
 * when で分けると、Group の枝では calendarId、Direct の枝では chatId が必ずある。
 */
sealed interface ChatRoomId {

    // TODO(51-A-1) グループ：どの共有カレンダーのトークか。文字の calendarId を 1 つ持つ
    //   ( ) の中は ChatMessage の 1 行と同じ「val 名前: 型」の形
    data class Group(val calendarId: String) : ChatRoomId

    // TODO(51-A-2) DM：どの DM の部屋か。文字の chatId を 1 つ持つ（51-A-1 と同じ形）
    data class Direct(val chatId: String) : ChatRoomId
}
