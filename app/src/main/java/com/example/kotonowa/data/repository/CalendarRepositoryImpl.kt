package com.example.kotonowa.data.repository

import android.R
import com.example.kotonowa.domain.model.Calendar
import com.example.kotonowa.domain.model.CalendarMember
import com.example.kotonowa.domain.model.CalendarType
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.CalendarRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.sql.Date
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

        val registration = firestore.collection(COLLECTION_CALENDARS)
            // memberUids に自分の uid が入っている書類だけに絞る（§4-(113)）
            .whereArrayContains(FIELD_MEMBER_UIDS, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // 読めない 1 件は捨てる（runCatching + getOrNull。§4-(56)）
                val calendars = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toCalendar() }.getOrNull() }
                    ?: emptyList()

                trySend(calendars)
            }

        awaitClose { registration.remove() }
    }

    override fun observeMembers(calendarId: String): Flow<List<CalendarMember>> = callbackFlow {

        // 名簿は部屋の書類の中のサブコレクション。collection → document → collection と降りる。
        // 最後に .document(...) は付けない（その部屋の名簿を全部見張るため）
        val registration = firestore.collection(COLLECTION_CALENDARS)
            .document(calendarId)
            .collection(SUBCOLLECTION_MEMBERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val members = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toCalendarMember() }.getOrNull() }
                    ?: emptyList()

                trySend(members)
            }

        awaitClose { registration.remove() }
    }

    // ---- 30-D ----------------------------------------------------------

    override suspend fun addMember(calendarId: String, member: CalendarMember): Result<Unit> = try {

        // 部屋の書類の住所（createCalendar の calenderDoc と同じ作り方。ただし id は引数から）
        val calendarDoc = firestore.collection(COLLECTION_CALENDARS).document(calendarId)

        // 名簿の中の、その人の行の住所
        val memberDoc = calendarDoc.collection(SUBCOLLECTION_MEMBERS).document(member.uid)

        firestore.runBatch { batch ->
            // ① 名簿に行を書く。Firestore は Kotlin の型を知らないので toMap() で翻訳してから渡す
            batch.set(memberDoc, member.toMap())

            // ② 検索用の配列にも足す（§4-(113)）。①と②が片方だけ書かれないよう runBatch でまとめる
            batch.update(calendarDoc, FIELD_MEMBER_UIDS, FieldValue.arrayUnion(member.uid))
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 肩書きだけを書き換える。人の出入りではないので `memberUids` は触らない。
     * 1 か所しか書かないので `runBatch` も要らない。
     */
    override suspend fun updateMemberRole(
        calendarId: String,
        uid: String,
        role: MemberRole,
    ): Result<Unit> = try {

        firestore.collection(COLLECTION_CALENDARS)
            .document(calendarId)
            .collection(SUBCOLLECTION_MEMBERS)
            .document(uid)
            // 書類が無ければエラーにしたいので set ではなく update（要件定義書 §3.3 の表）。
            // enum は文字に直す（§4-(111)）
            .update("role",role.name.lowercase() )
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeMember(calendarId: String, uid: String): Result<Unit> = try {

        val calendarDoc = firestore.collection(COLLECTION_CALENDARS).document(calendarId)
        val memberDoc = calendarDoc.collection(SUBCOLLECTION_MEMBERS).document(uid)

        firestore.runBatch { batch ->
            // ① 名簿の行を消す
            batch.delete(memberDoc)

            // ② 検索用の配列からも外す（addMember の逆）
            batch.update(calendarDoc, FIELD_MEMBER_UIDS, FieldValue.arrayRemove(uid))
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ---- 変換係（このファイルの中だけで使う。§4-㊱） --------------------------

/**
 * [Calendar] を Firestore に渡せる形（Map）に詰め替える。
 *
 * `memberUids` は domain の [Calendar] が持っていないので、**引数で受け取って足す**。
 * `type` は enum なので文字に直す（§4-(111)）。
 */
private fun Calendar.toMap(memberUids: List<String>): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "ownerUid" to ownerUid,
    "type" to type.name.lowercase(),        // enum は文字に直す（§4-(111)）
    "color" to color,
    "createdAt" to Date.from(createdAt),   // Instant は直接保存できない（お手本: ScheduleItem.toMap の updatedAt）
    FIELD_MEMBER_UIDS to memberUids,
)

/** [CalendarMember] を Map に詰め替える。`role` は enum なので文字に直す（§4-(111)）。 */
private fun CalendarMember.toMap(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "role" to role.name.lowercase(),        // enum は文字に直す。Step 37 のルールが小文字で照合する
    "joinedAt" to Date.from(joinedAt),    // Instant は直接保存できない
    "invitedBy" to invitedBy,   // String? なのでそのまま（null はそのまま保存される）
)

/** Firestore の書類 1 件を [Calendar] に組み立て直す。`toUser()` と同じ形（§4-⑮、§6-㊺）。 */
private fun DocumentSnapshot.toCalendar(): Calendar = Calendar(
    // お手本は UserRepositoryImpl の toUser()。ただしこちらは「無くてよい項目」が無いので、
    // 6 つとも ?: throw で守る（欠けていたら壊れたデータ）
    id = getString("id") ?: throw IllegalStateException("idが入っていません"),
    name = getString("name") ?: throw IllegalStateException("nameが入っていません"),
    ownerUid = getString("ownerUid") ?: throw IllegalStateException("ownerUidが入っていません"),
    // 文字から enum に戻す（§4-(111)）。"personal" → "PERSONAL" → CalendarType.PERSONAL
    type = CalendarType.valueOf(
        (getString("type") ?: throw IllegalStateException("typeが入っていません")).uppercase()
    ),
    color = getString("color") ?: throw IllegalStateException("colorが入っていません"),
    createdAt = getDate("createdAt")?.toInstant()
        ?: throw IllegalStateException("createdAtが入っていません"),
)

/** Firestore の書類 1 件を [CalendarMember] に組み立て直す。 */
private fun DocumentSnapshot.toCalendarMember(): CalendarMember = CalendarMember(
    uid = getString("uid") ?: throw IllegalStateException("uidが入っていません"),
    role = MemberRole.valueOf(
        (getString("role") ?: throw IllegalStateException("roleが入っていません")).uppercase()
    ),
    joinedAt = getDate("joinedAt")?.toInstant()
        ?: throw IllegalStateException("joinedAtが入っていません"),
    // invitedBy は「無いことがある」ので ?: throw を付けない（toUser の email と同じ）
    invitedBy = getString("invitedBy"),
)
