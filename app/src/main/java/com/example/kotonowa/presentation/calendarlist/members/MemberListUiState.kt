package com.example.kotonowa.presentation.calendarlist.members

import com.example.kotonowa.domain.model.MemberRole

/**
 * メンバー画面に出す 1 行ぶん。「名簿の 1 行（CalendarMember）」と「名刺（User）」を合体させた形。
 *
 * 名簿には uid と肩書きしか無く、名前は名刺にある（Step 27 / 34-A）。
 * 画面が 2 つを突き合わせなくて済むよう、頭脳（[MemberListViewModel]）が合体させてから渡す。
 * joinedAt / invitedBy は今は画面に出さないので持たない。
 */
data class MemberItem(
    val uid: String,       // その人の uid。ロール変更・外すときに「誰を」の指定に使う
    val name: String,      // 画面に出す名前（名刺の表示名 → メール → 「名前未登録」の順で決める）
    val role: MemberRole,  // その人の肩書き
)

/**
 * メンバー画面の「今の状態」。お手本は同じフォルダの上の `CalendarListUiState`。
 */
data class MemberListUiState(
    // TODO(34-C-1) 名簿の行の並び。型は「MemberItem のリスト」、最初は空っぽ（CalendarListUiState の calendars と同じ形）
    val members: List<MemberItem> = emptyList(),

    // TODO(34-C-1) 自分がこのカレンダーのオーナーか。最初は「いいえ」。名簿が届いてから頭脳が決める
    val isOwner: Boolean = false,

    // TODO(34-C-1) 開いた瞬間に名簿の読み込みが始まる。最初は読み込み中か？（Step 18-B / 31 と同じ考え方）
    val isLoading: Boolean = true,

    val errorMessage: String? = null,
)
