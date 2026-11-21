package com.example.kotonowa.domain.model

import java.time.Instant

/**
 * カレンダーの名簿の 1 行。「誰が・どの肩書きで・いつから入っているか」。
 *
 * Firestore の `calendars/{calendarId}/members/{uid}` ドキュメントに対応する。
 * ドキュメント ID にその人の uid を使うので、セキュリティルールから
 * `members/$(request.auth.uid)` と一発で引ける（Step 37 でそうする）。
 *
 * 表示名やメールアドレスはここには入れない。それは `users` コレクションが持つ
 * （Step 28）。名前を変えたときに名簿を全部書き直さずに済むため。
 */
data class CalendarMember(
    //  uid       … その人の uid
    //  role      … その人の肩書き
    //  joinedAt  … 入った時刻
    //  invitedBy … 招待した人の uid。オーナー自身は誰にも招待されていないので「無い」ことがある
    val uid: String,
    val role: MemberRole,
    val joinedAt: Instant,
    val invitedBy: String?,
    // Step 46：名簿の行に、その人の名前とメールを写しておく（メンバー画面に出すため）。
    //   他人の名刺（users）は読めなくなるので、名簿だけで名前を出せるようにする。
    //   Step 46 より前に作られた行には無いので null になりうる。既定値 null は、作る側で省略してよい印（§7-(57)）
    val displayName: String? = null,
    val email: String? = null,
)
