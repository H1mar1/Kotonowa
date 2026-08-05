package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.ScheduleRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore 上のコレクション名。打ち間違い防止のため定数にする（§5-㉓）。 */
private const val COLLECTION_EVENTS = "events"

/**
 * `ScheduleRepository` の約束を Cloud Firestore で実際に果たすクラス。
 *
 * Firestore に触るのはこのファイルまで。ここから先（domain / presentation）へは
 * 自前の [ScheduleItem] だけを渡す。
 *
 * Firestore は Kotlin の型をそのまま保存できないので、
 * 保存するときは Map に詰め替え、読むときは [ScheduleItem] に戻す。
 * `type`（event / task）を付けるのもこの層の仕事。
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ScheduleRepository {

    override suspend fun addItem(item: ScheduleItem): Result<Unit> = try {
        firestore.collection(COLLECTION_EVENTS)
            .document(item.id)
            .set(item.toMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateItem(item: ScheduleItem): Result<Unit> = try {
        firestore.collection(COLLECTION_EVENTS)
            .document(item.id)
            .update(item.toMap())//そのドキュメントが存在している時だけ書き換える
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> = try {
        firestore.collection(COLLECTION_EVENTS)
            .document(itemId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }


    override suspend fun getItem(itemId: String): Result<ScheduleItem> = try {
        val snapshot = firestore.collection(COLLECTION_EVENTS)
            .document(itemId)
            .get()
            .await()

        if (!snapshot.exists()) {
            throw IllegalStateException("予定が見つかりません(id=$itemId)")
        }
        Result.success(snapshot.toScheduleItem())
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeItems(
        calendarId: String,
        from: Instant,
        to: Instant,
    ): Flow<List<ScheduleItem>> = TODO("Step 15-F で実装する")
}

/**
 * [ScheduleItem] を Firestore に渡せる形（Map）に詰め替える。
 *
 * Firestore は Kotlin の型をそのまま保存できないため、名札と値の組にする（§4-㉟）。
 * 日時は Instant のままでは保存できないので Date に変換する（Firestore が Timestamp にしてくれる）。
 */
private fun ScheduleItem.toMap(): Map<String, Any?> {

    val base = mapOf(
        "id" to id,
        "calendarId" to calendarId,
        "title" to title,
        "description" to description,
        "createdBy" to createdBy,
        "reminderMinutesBefore" to reminderMinutesBefore,
        "updatedAt" to Date.from(updatedAt)
    )


    val extra = when (this) {
        is ScheduleItem.Event -> mapOf(
            "type" to "event",
            "startAt" to Date.from(startAt),
            "endAt" to Date.from(endAt),
            "allDay" to allDay
        )

        is ScheduleItem.Task -> mapOf(
            "type" to "task",
            "dueAt" to Date.from(dueAt),
            "isCompleted" to isCompleted
        )

    }

    return base + extra
}

/**
 * Firestore から取ってきた 1 件分のコピー（[DocumentSnapshot]）を [ScheduleItem] に組み立て直す。
 *
 * [toMap] の裏返し。ただし Firestore から来る値は「何が入っているか分からない」状態なので、
 * 1 つずつ「文字列として取る」「日時として取る」と指定して取り出す。
 * 指定と違えば null が返るため、必須のものが欠けていたら例外を投げて壊れたデータを通さない。
 */
private fun DocumentSnapshot.toScheduleItem(): ScheduleItem {
    // TODO: ① 共通フィールドを取り出す（7 個）
    //         getString("id") / getString("calendarId") / getString("title") /
    //         getString("description") / getString("createdBy") /
    //         getLong("reminderMinutesBefore") / getDate("updatedAt")
    //         null が返りうるので、必須のものは ?: で受けて throw する（§4-⑮）
    //         description と reminderMinutesBefore は元々 null 可なのでそのままでよい

    // TODO: ② getString("type") を when で分岐する（§7-㉙）
    //         "event" -> ScheduleItem.Event(...) を組み立てて返す
    //         "task"  -> ScheduleItem.Task(...) を組み立てて返す
    //         else    -> 想定外の値なので throw

    // TODO: ③ 型を戻すのを忘れない（toMap の逆）
    //         Date -> .toInstant() で Instant に戻す
    //         Long -> .toInt() で Int に戻す（Firestore は整数を必ず Long で返すため）

    TODO("Step 15-E で実装する")
}
