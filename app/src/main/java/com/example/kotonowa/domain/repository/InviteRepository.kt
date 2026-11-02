package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.Invite

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
}
