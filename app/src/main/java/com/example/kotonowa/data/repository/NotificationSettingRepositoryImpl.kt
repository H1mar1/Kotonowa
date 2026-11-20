package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.repository.NotificationSettingRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val COLLECTION_USERS = "users"

/** 通知を止めたカレンダーの id の一覧。Cloud Functions（functions/src/index.ts）も同じ名札で読む */
private const val FIELD_MUTED_CALENDAR_IDS = "mutedCalendarIds"

/**
 * [NotificationSettingRepository] の Firebase 実装（Step 45）。
 *
 * 出し入れは fcmTokens（Step 41）と同じく arrayUnion / arrayRemove。
 */
@Singleton
class NotificationSettingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : NotificationSettingRepository {

    // 名刺 1 枚を見張る。形は ScheduleRepositoryImpl.observeItem（Step 20）と同じ
    override fun observeMutedCalendarIds(uid: String): Flow<Set<String>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_USERS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // 配列は「何が入っているか分からない List」として届く。
                //   as? List<*> … List なら受け取る、違えば（項目が無いときも）null（grammar §4-(141)）
                //   filterIsInstance<String>() … 中から文字だけを拾う
                //   ?: emptySet() … 項目が無ければ空
                val ids = (snapshot?.get(FIELD_MUTED_CALENDAR_IDS) as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toSet()
                    ?: emptySet()
                trySend(ids)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setCalendarMuted(
        uid: String,
        calendarId: String,
        muted: Boolean,
    ): Result<Unit> = try {
        // TODO(45-A-1) 何を作る：止めるなら「一覧に calendarId を足す」、再開するなら「一覧から calendarId を抜く」命令
        //   if (条件) A else B の形で、答えを change に入れる（grammar §5-(68)。if も値を返す）
        //   足す命令は FieldValue.arrayUnion(…)、抜く命令は FieldValue.arrayRemove(…)（PushTokenRepositoryImpl と同じ）
        val change = if (muted){
            FieldValue.arrayUnion(calendarId)
        }
        else{
            FieldValue.arrayRemove(calendarId)
        }

        // 名刺に書く。set ＋ merge なのは、一覧の項目がまだ無くても失敗させないため（registerToken と同じ）
        // TODO(45-A-2) mapOf の中：名札 FIELD_MUTED_CALENDAR_IDS と、上で作った change の組
        firestore.collection(COLLECTION_USERS).document(uid)
            .set(mapOf(FIELD_MUTED_CALENDAR_IDS to change), SetOptions.merge())
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
