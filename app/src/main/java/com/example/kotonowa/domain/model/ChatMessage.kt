package com.example.kotonowa.domain.model

import java.time.Instant

/**
 * チャットの発言 1 つ（Phase 5 / Step 49）。
 *
 * Firestore の `calendars/{calendarId}/messages/{messageId}` に対応する（要件定義書 §3.6・§4）。
 * ③ の DM でも同じ形を使う（置き場所が `directChats/{chatId}/messages` に変わるだけ）。
 */
data class ChatMessage(
    //  id         … この発言の ID（書類の住所）
    //  senderUid  … 送った人の uid。「自分の発言か」（右寄せにする・② で消せる）の判定に使う
    //  senderName … 送った人の名前。送った時点の名前を写しておく（名簿の行と同じ考え方。他人の名刺は読めない）
    //  text       … 本文
    //  createdAt  … 送った時刻。並び順と、吹き出しの横の「15:04」に使う
    val id: String,
    // TODO(49-A-1) 上の説明を見て、残り 4 つの型を埋める（uid・名前・本文は文字、時刻は Invite の createdAt と同じ型）
    val senderUid: String,
    val senderName: String,
    val text: String,
    val createdAt: Instant,
)
