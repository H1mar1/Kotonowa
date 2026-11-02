package com.example.kotonowa.domain.model

import java.time.Instant

/**
 * カレンダーへの招待状 1 枚。
 *
 * Firestore の `invites/{inviteId}` ドキュメントに対応する（要件定義書 §4）。
 * Step 35 でオーナーが書き、Step 36 で招待された人が承認/拒否する。
 */
data class Invite(
    //  id           … この招待状の ID
    //  calendarId   … どのカレンダーへの招待か
    //  calendarName … カレンダーの名前。招待された人はまだメンバーでなく、部屋の書類を読めないので写しておく
    //  invitedEmail … 誰宛てか（メールアドレス）
    //  role         … 承認したあとの肩書き（Step 27 で作った enum）
    //  status       … 今の状態（下の InviteStatus）
    //  invitedBy    … 招待した人の uid
    //  createdAt    … 招待した時刻
    val id: String,
    val calendarId: String,
    // TODO(35-A-1) 上の 2 行を参考に、残りの項目の型を埋める
    val calendarName: String,
    val invitedEmail: String,
    val role: MemberRole,
    val status: InviteStatus,
    val invitedBy: String,
    val createdAt: Instant,
)

/**
 * 招待状の状態。
 */
enum class InviteStatus {
    // TODO(35-A-2) 3 つの選択肢を並べる
    //  ・PENDING（ペンディング）… 返事待ち
    //  ・ACCEPTED（アクセプテッド）… 承認された
    //  ・DECLINED（ディクラインド）… 拒否された
    PENDING,
    ACCEPTED,
    DECLINED,
}
