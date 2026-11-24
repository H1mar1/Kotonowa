package com.example.kotonowa.domain.model

import java.time.Instant

/**
 * DM の部屋 1 つ（Phase 5 / Step 51）。トーク一覧の 1 行になる。
 *
 * Firestore の `directChats/{chatId}` に対応する（要件定義書 §4）。
 * 書類には 2 人分の uid と名前が入っているが、画面が知りたいのは「相手」だけなので、
 * data 層で自分ではない方を選んで otherUid / otherName に入れる（画面で毎回探さなくてよい）。
 *
 * @property id 部屋の id（2 人の uid を小さい順に `_` でつないだもの）
 * @property otherUid 相手の uid
 * @property otherName 相手の名前（部屋を作ったときに名簿の行から写したもの）
 * @property lastMessageText 最後の発言の本文（一覧の行に小さく出す）
 * @property lastMessageAt 最後の発言の時刻（一覧を新しい順に並べるのに使う）
 */
data class DirectChat(
    val id: String,
    val otherUid: String,
    val otherName: String,
    // TODO(51-A-3) 最後の発言の本文と時刻。部屋を作ったばかりでまだ誰も話していないと、どちらも無い
    //   本文は「文字、または無し」、時刻は「Instant、または無し」の型（ChatRoomUiState の errorMessage と同じ「? 付き」）
    val lastMessageText: String?,
    val lastMessageAt: Instant?,
)
