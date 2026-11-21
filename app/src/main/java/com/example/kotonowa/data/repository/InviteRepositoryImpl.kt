package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.CalendarMember
import com.example.kotonowa.domain.model.Invite
import com.example.kotonowa.domain.model.InviteSendResult
import com.example.kotonowa.domain.model.InviteStatus
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.InviteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore 上の名前。打ち間違い防止のため定数にする（§5-㉓）。 */
private const val COLLECTION_INVITES = "invites"
private const val FIELD_INVITED_EMAIL = "invitedEmail"
private const val FIELD_STATUS = "status"

/**
 * [InviteRepository] の約束を Cloud Firestore で実際に果たすクラス。
 *
 * お手本は同じフォルダの `CalendarRepositoryImpl`。
 * 承認（acceptInvite）だけは 3 か所を書くので `runBatch` を使う。
 * `calendars` の名前（COLLECTION_CALENDARS など）と名簿の詰め替え係（CalendarMember.toMap）は
 * CalendarRepositoryImpl.kt のものを借りる（internal。§4-(131)）。
 */
@Singleton
class InviteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    // サーバーの関数の呼び出し口（Step 46。FirebaseModule が東京向けのものを届ける）
    private val functions: FirebaseFunctions,
    // Step 46：承認したとき、名簿の行に自分の名前とメールを写すため
    private val firebaseAuth: FirebaseAuth,
) : InviteRepository {

    /**
     * 招待を送る（Step 46）。サーバーの関数 sendInvite（functions/src/index.ts）を呼ぶ。
     *
     * 関数へは Map で渡し、関数からは Map で返ってくる（JSON と同じ形。grammar §4-(142)）。
     */
    override suspend fun sendInvite(
        calendarId: String,
        email: String,
        role: MemberRole,
    ): Result<InviteSendResult> = try {
        // 関数へ渡す 3 つ。名札は関数側（request.data.calendarId など）と同じ綴りにする
        val data = mapOf(
            "calendarId" to calendarId,
            "email" to email,
            // TODO(46-C-1) 肩書きを、関数が受け取れる小文字の英語（"editor" / "viewer"）にする
            //   保存するときと同じ変換（Step 30 の role.name.lowercase()。grammar §4-(111)）
            "role" to role.name.lowercase(),
        )

        // 関数を名前で呼び、終わるまで待つ。返ってきた中身（data）から result の文字を取り出す
        val response = functions.getHttpsCallable("sendInvite").call(data).await()
        val result = (response.getData() as? Map<*, *>)?.get("result") as? String

        // TODO(46-C-2) 関数が返した文字を、アプリの InviteSendResult に翻訳する（残り 2 行）
        //   "not_registered" → NOT_REGISTERED、"already_member" → ALREADY_MEMBER（1 行目の "sent" と同じ形）
        //   どれでもない文字（関数とアプリの食い違い）は else で例外にする（下の catch で失敗になる）
        val sendResult = when (result) {
            "sent" -> InviteSendResult.SENT
            "not_registered" -> InviteSendResult.NOT_REGISTERED
            "already_member" -> InviteSendResult.ALREADY_MEMBER
            else -> throw IllegalStateException("招待の結果が読めません: $result")
        }

        Result.success(sendResult)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---- 36-C-2：自分宛ての招待状を見張る ------------------------------------

    /**
     * 自分宛てで、返事待ちの招待状だけを見張る。
     * お手本は CalendarRepositoryImpl の observeMyCalendars。違いは絞り込みの 2 行だけ。
     */
    override fun observeMyInvites(email: String): Flow<List<Invite>> = callbackFlow {

        val registration = firestore.collection(COLLECTION_INVITES)
            // TODO(36-C-2-1) 誰の：招待状の ／ 何で絞る：宛先のメールが、引数の email と同じもの
            //   書き方は whereEqualTo(項目の名前, 値)（§4 の whereEqualTo の表）。項目の名前は上の定数を使う
            .whereEqualTo(FIELD_INVITED_EMAIL, email)
            // TODO(36-C-2-2) 誰の：招待状の ／ 何で絞る：状態が「返事待ち」のもの
            //   値は Firestore に保存されている形（小文字の文字）。enum から作る（toMap の "status" の行と同じ書き方）
            .whereEqualTo(FIELD_STATUS, InviteStatus.PENDING.name.lowercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // TODO(36-C-2-3) 書類を 1 件ずつ Invite に組み立てる。読めない 1 件は捨てる
                //   observeMyCalendars の calendars の 3 行と同じ形。組み立て係は下の toInvite
                val invites = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toInvite() }.getOrNull() }
                    ?: emptyList()

                trySend(invites)
            }

        awaitClose { registration.remove() }
    }

    // ---- 36-C-3：拒否する ----------------------------------------------------

    /**
     * 招待状の status だけを DECLINED に書き換える。
     * お手本は CalendarRepositoryImpl の updateMemberRole（1 項目だけ update）。
     */
    override suspend fun declineInvite(invite: Invite): Result<Unit> = try {
        firestore.collection(COLLECTION_INVITES)
            // TODO(36-C-3-1) 誰の：招待状の ／ 何を：id を住所にする（sendInvite と同じ）
            .document(invite.id)
            // TODO(36-C-3-2) 何をする：status の項目を「拒否」の文字に書き換える
            //   update(項目の名前, 値)。値は enum から小文字の文字を作る
            .update(FIELD_STATUS, InviteStatus.DECLINED.name.lowercase())
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---- 36-C-4：承認する ----------------------------------------------------

    /**
     * 3 つを runBatch でまとめて書く（§4-(112)）。どれか 1 つでもルールに断られると、3 つとも書かれない。
     *  ① 名簿の行を作る ② 部屋の memberUids に自分を足す ③ 招待状を ACCEPTED に
     * お手本は CalendarRepositoryImpl の addMember（①② と同じ）に、③ の 1 行を足した形。
     */
    override suspend fun acceptInvite(invite: Invite, uid: String): Result<Unit> = try {

        // ---- 住所を 3 つ用意する ----
        // TODO(36-C-4-1) 部屋の書類の住所。誰の：招待状の ／ 何：どのカレンダーへの招待か
        val calendarDoc = firestore.collection(COLLECTION_CALENDARS).document(invite.calendarId)
        // TODO(36-C-4-2) 名簿の中の、自分の行の住所（addMember の memberDoc と同じ作り方。行の名前は自分の uid）
        val memberDoc = calendarDoc.collection(SUBCOLLECTION_MEMBERS).document(uid)
        // TODO(36-C-4-3) 招待状の住所（declineInvite と同じ）
        val inviteDoc = firestore.collection(COLLECTION_INVITES).document(invite.id)

        // ---- 名簿の行を組み立てる ----
        // TODO(36-C-4-4) 4 つの欄を埋める（createCalendar の owner と同じ形）
        //   ・誰が：自分の uid ・肩書き：招待状に書いてある肩書き（OWNER と書かないこと）
        //   ・いつから：今 ・誰に招待されたか：招待状に書いてある「招待した人」
        val member = CalendarMember(
            uid = uid,
            role = invite.role,
            joinedAt = Instant.now(),
            invitedBy = invite.invitedBy,
            // Step 46：承認しているのは招待された本人（今ログインしている人）なので、自分の名前とメールを写す（46-E-3 と同じ 2 行）
            // TODO(46-E-4) 46-E-3 と同じ。こちらのクラスにも firebaseAuth を足してある
            displayName = firebaseAuth.currentUser?.displayName,
            email = firebaseAuth.currentUser?.email?.lowercase(),
        )

        firestore.runBatch { batch ->
            // TODO(36-C-4-5) ① 名簿の行を書く（addMember の ① と同じ）
            batch.set(memberDoc, member.toMap())
            // TODO(36-C-4-6) ② 部屋の memberUids に自分の uid を足す（addMember の ② と同じ）
            batch.update(calendarDoc, FIELD_MEMBER_UIDS, FieldValue.arrayUnion(uid))
            // TODO(36-C-4-7) ③ 招待状の status を「承認」の文字にする（declineInvite の update を batch で書く形）
            //   batch.update(住所, 項目の名前, 値)
            batch.update(inviteDoc, FIELD_STATUS, InviteStatus.ACCEPTED.name.lowercase())
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ---- 変換係（このファイルの中だけで使う。§4-㊱） --------------------------

// Invite.toMap（招待状 → Map）は Step 46 で削除した。招待状はサーバーの関数 sendInvite が書くため。

// ---- 36-C-1：書類 → Invite --------------------------------------------------

/**
 * Firestore の書類 1 件を [Invite] に組み立て直す。toMap の逆。
 * お手本は CalendarRepositoryImpl の toCalendar。8 つとも「無くてよい項目」は無いので、全部 ?: throw で守る。
 */
private fun DocumentSnapshot.toInvite(): Invite = Invite(
    // TODO(36-C-1-1) 文字の項目 5 つ。toCalendar の id / name の行と同じ形
    id = getString("id") ?: throw IllegalStateException("idが入っていません"),
    calendarId = getString("calendarId")
        ?: throw IllegalStateException("calendarIdが入っていません"),
    calendarName = getString("calendarName")
        ?: throw IllegalStateException("calendarNameが入っていません"),
    invitedEmail = getString("invitedEmail")
        ?: throw IllegalStateException("invitedEmailが入っていません"),
    invitedBy = getString("invitedBy") ?: throw IllegalStateException("invitedByが入っていません"),
    // TODO(36-C-1-2) enum の項目 2 つ。toCalendarMember の role と同じ形（小文字 → uppercase → valueOf）
    role = MemberRole.valueOf(
        (getString("role") ?: throw IllegalStateException("roleが入っていません")).uppercase()
    ),
    status = InviteStatus.valueOf(
        (getString("status") ?: throw IllegalStateException("statusが入っていません")).uppercase()
    ),
    // TODO(36-C-1-3) 時刻。toCalendar の createdAt と同じ形
    createdAt = getDate("createdAt")?.toInstant()
        ?: throw IllegalStateException("createdAtが入っていません"),
)
