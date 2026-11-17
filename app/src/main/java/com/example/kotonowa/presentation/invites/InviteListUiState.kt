package com.example.kotonowa.presentation.invites

import com.example.kotonowa.domain.model.Invite

/**
 * 届いた招待の一覧画面の「今の状態」。
 *
 * お手本は `presentation/calendarlist/CalendarListUiState`（一覧＋読み込み中＋エラー）。
 * それに「今どの招待状の返事を送っている途中か」と「返事の結果の一言」を足した形。
 */
data class InviteListUiState(

    // TODO(36-E-1-1) 自分宛て・返事待ちの招待状の並び。型は「Invite のリスト」、最初は空っぽ
    //   CalendarListUiState の calendars と同じ形
    val invites: List<Invite> = emptyList(),

    // TODO(36-E-1-2) 開いた瞬間に読み込みが始まる。最初は読み込み中か？（CalendarListUiState と同じ）
    val isLoading: Boolean = true,

    /** 読み込みに失敗したときの一言。これが入ると一覧の代わりに出る。 */
    val errorMessage: String? = null,

    /**
     * 返事（承認／拒否）を送っている途中の招待状の id。送っていなければ null。
     *
     * Boolean（isSaving のような「はい／いいえ」）にしないのは、**どの招待状の**ボタンを止めるかを知るため。
     * 招待が 2 枚あるとき、押した 1 枚のボタンだけを押せなくし、もう 1 枚はそのまま押せるようにする。
     */
    val processingInviteId: String? = null,

    /**
     * 返事の結果の一言（「『家族』に参加しました」「承認できませんでした」など）。無ければ null。
     * errorMessage と分けるのは、これが出ても一覧は消さないため（MemberListUiState の inviteMessage と同じ考え方）。
     */
    val actionMessage: String? = null,
)
