package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.Invite
import kotlinx.coroutines.flow.Flow

/**
 * 招待状（Firestore の `invites` コレクション）まわりの約束（お品書き）。
 *
 * お手本は同じフォルダの `CalendarRepository`。「どうやるか」は書かず、
 * 実際のやり方は data 層の `InviteRepositoryImpl` が Firestore で実装する（35-C）。
 * 届いた招待の一覧と、承認/拒否の約束は Step 36 で足す。
 */
interface InviteRepository {

    /**
     * 招待状を 1 枚出す。
     *
     * 通信が必要なので suspend。返すものは「成功したか、失敗したか」だけ。
     */
    // TODO(35-B-1) 関数名は sendInvite。引数は招待状 1 枚（名前は invite）
    //   お手本は CalendarRepository の createCalendar。形がまったく同じ
    suspend fun sendInvite(invite: Invite): Result<Unit>

    // ---- 36-B：受け取る側 ---------------------------------------------------

    /**
     * 自分宛てで、返事待ち（PENDING）の招待状を見張り続ける。
     *
     * 承認・拒否すると PENDING ではなくなるので、一覧から自動で消える（▼ の赤い点も消える）。
     * 引数はメールアドレス。招待状には uid ではなくメールで宛先が書いてあるため（35-D-3）。
     */
    // TODO(36-B-1) 関数名は observeMyInvites。引数は email（文字）。
    //   戻り値は「招待状のリスト」が流れ続ける管。お手本は CalendarRepository の observeMyCalendars（suspend は付かない）
    fun observeMyInvites(email: String): Flow<List<Invite>>

    /**
     * 招待を承認する。実装（36-C）では次の 3 つを**まとめて**行う（runBatch。§4-(112)）。
     *  ① calendars/{calendarId}/members/{uid} に、招待状の肩書きで名簿の行を作る
     *  ② 部屋の memberUids に uid を足す
     *  ③ 招待状の status を ACCEPTED にする
     *
     * uid を別にもらうのは、招待状には宛先が「メール」でしか書いていないため。
     * 名簿の行の住所（①）と memberUids（②）には uid が要る。
     */
    // TODO(36-B-2) 関数名は acceptInvite。引数は 2 つ：招待状 1 枚（invite）と、自分の uid（uid）。
    //   通信が要り、返すのは「成功したか、失敗したか」だけ。お手本はすぐ上の sendInvite
    suspend fun acceptInvite(invite: Invite, uid: String): Result<Unit>

    /**
     * 招待を拒否する。招待状の status を DECLINED にするだけ（名簿も部屋も触らない）。
     */
    // TODO(36-B-3) 関数名は declineInvite。引数は招待状 1 枚（invite）。形は sendInvite とまったく同じ
    suspend fun declineInvite(invite: Invite): Result<Unit>
}
