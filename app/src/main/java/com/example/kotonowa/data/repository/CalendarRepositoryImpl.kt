package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.CalendarMember
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.CalendarRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

/** Firestore 上の名前。打ち間違い防止のため定数にする（§5-㉓）。 */
private const val COLLECTION_CALENDARS = "calendars"
private const val SUBCOLLECTION_MEMBERS = "members"
private const val FIELD_MEMBER_UIDS = "memberUids"

/**
 * [CalendarRepository] の約束を Cloud Firestore で実際に果たすクラス。
 *
 * お手本は同じフォルダの `ScheduleRepositoryImpl`。違いは 2 つ。
 *  ① 書く場所が 2 か所ある（部屋の書類と、その中の名簿）。片方だけ書かれた状態を
 *     作らないよう `runBatch`（§4-(112)）でまとめる
 *  ② 検索用の `memberUids` 配列（§4-(113)）をここで面倒を見る。
 *     domain の `Calendar` には無い項目なので、外からは見えない
 */
@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CalendarRepository {

    // ---- 30-B ----------------------------------------------------------

    override suspend fun createCalendar(calendar: Calendar): Result<Unit> = try {
        //部屋の書類の住所
        val calenderDoc = firestore.collection(COLLECTION_CALENDARS).document(calendar.id)
        //名簿の中の、オーナーの行の住所
        val ownerDoc = calenderDoc.collection(SUBCOLLECTION_MEMBERS).document(calendar.ownerUid)

        // オーナーの名簿の行を組み立てる
        val owner = CalendarMember(
            uid = calendar.ownerUid,
            role = MemberRole.OWNER,
            joinedAt = Instant.now(),
            invitedBy = null
        )

        firestore.runBatch { batch ->
            batch.set(calenderDoc,calendar.toMap(memberUids = listOf(calendar.ownerUid)))
            batch.set(ownerDoc,owner.toMap())
        }.await()
        Result.success(Unit)
    }catch (e: Exception){
        Result.failure(e)
    }

    // ---- 30-C ----------------------------------------------------------

    override fun observeMyCalendars(uid: String): Flow<List<Calendar>> = callbackFlow {
        // TODO(Step 30-C)
        TODO()
    }

    override fun observeMembers(calendarId: String): Flow<List<CalendarMember>> = callbackFlow {
        // TODO(Step 30-C)
        TODO()
    }

    // ---- 30-D ----------------------------------------------------------

    override suspend fun addMember(calendarId: String, member: CalendarMember): Result<Unit> {
        // TODO(Step 30-D)
        TODO()
    }

    override suspend fun updateMemberRole(
        calendarId: String,
        uid: String,
        role: MemberRole,
    ): Result<Unit> {
        // TODO(Step 30-D)
        TODO()
    }

    override suspend fun removeMember(calendarId: String, uid: String): Result<Unit> {
        // TODO(Step 30-D)
        TODO()
    }
}

// ---- 変換係（このファイルの中だけで使う。§4-㊱） --------------------------

/**
 * [Calendar] を Firestore に渡せる形（Map）に詰め替える。
 *
 * `memberUids` は domain の [Calendar] が持っていないので、**引数で受け取って足す**。
 * `type` は enum なので文字に直す（§4-(111)）。
 */
private fun Calendar.toMap(memberUids: List<String>): Map<String, Any?> {
    // TODO(Step 30-B)
    TODO()
}

/** [CalendarMember] を Map に詰め替える。`role` は enum なので文字に直す（§4-(111)）。 */
private fun CalendarMember.toMap(): Map<String, Any?> {
    // TODO(Step 30-D)
    TODO()
}

/** Firestore の書類 1 件を [Calendar] に組み立て直す。`toUser()` と同じ形（§4-⑮、§6-㊺）。 */
private fun DocumentSnapshot.toCalendar(): Calendar {
    // TODO(Step 30-C)
    TODO()
}

/** Firestore の書類 1 件を [CalendarMember] に組み立て直す。 */
private fun DocumentSnapshot.toCalendarMember(): CalendarMember {
    // TODO(Step 30-C)
    TODO()
}
