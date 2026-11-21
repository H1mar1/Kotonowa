package com.example.kotonowa.domain.model

/**
 * 招待を送った結果（Step 46）。
 *
 * サーバーの関数 sendInvite が 3 つのどれかを返す。どれも「通信は成功した」場合の答えで、
 * 通信の失敗やオーナーでない場合などは Result の失敗（例外）で届く。
 */
enum class InviteSendResult {
    /** 招待状を書いた（相手に通知も届く） */
    SENT,

    /** そのメールアドレスの人は登録していない */
    NOT_REGISTERED,

    /** その人はすでにメンバー */
    ALREADY_MEMBER,
}
