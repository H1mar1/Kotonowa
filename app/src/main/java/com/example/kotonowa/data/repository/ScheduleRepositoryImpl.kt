package com.example.kotonowa.data.repository

import androidx.compose.animation.core.snap
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.ScheduleRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /**
     * 指定した期間の予定/タスクを監視する。
     *
     * Firestore の「変わったら教える」機能はコールバック（呼び返し）方式なので、
     * [callbackFlow] で Flow の管に変換して流す（§2-㊾）。
     */
    override fun observeItems(
        calendarId: String,
        from: Instant,
        to: Instant,
    ): Flow<List<ScheduleItem>> = callbackFlow {
        
        val registration = firestore.collection(COLLECTION_EVENTS)
            .whereEqualTo("calendarId", calendarId)
            .whereGreaterThanOrEqualTo("sortAt", Date.from(from))
            .whereLessThan("sortAt",Date.from(to))
            .orderBy("sortAt")
            .addSnapshotListener { napshots, error -> }

        //
        //   ヒント: where に渡す日時は Date.from(...) にする。
        //          保存したのが Date なので、探す条件も同じ形に揃える（15-F-1 と同じ話）

        // TODO 2: awaitClose { } の中で、TODO 1 の解除券を使って見張りを外す（外す命令は remove()）
        //   これを書かないと実行時にエラーになる。画面が消えても通信し続けるのを防ぐ後片付け
    }
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
            "allDay" to allDay,
            "sortAt" to Date.from(startAt)
        )

        is ScheduleItem.Task -> mapOf(
            "type" to "task",
            "dueAt" to Date.from(dueAt),
            "isCompleted" to isCompleted,
            "sortAt" to Date.from(dueAt)
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
    val id = getString("id") ?: throw IllegalStateException("idが入っていません")
    val calendarId =
        getString("calendarId") ?: throw IllegalStateException("calendarIdが入っていません")
    val title = getString("title") ?: throw IllegalStateException("titleが入っていません")
    val description = getString("description")
    val createdBy =
        getString("createdBy") ?: throw IllegalStateException("createdByが入っていません")
    val reminderMinutesBefore = getLong("reminderMinutesBefore")?.toInt()
    val updatedAt = getDate("updatedAt")?.toInstant()
        ?: throw IllegalStateException("updatedAtが入っていません")

    return when (getString("type")) {
        "event" -> ScheduleItem.Event(
            id = id,
            calendarId = calendarId,
            title = title,
            description = description,
            createdBy = createdBy,
            reminderMinutesBefore = reminderMinutesBefore,
            updatedAt = updatedAt,
            startAt = getDate("startAt")?.toInstant()
                ?: throw IllegalStateException("startAtが入っていません"),
            endAt = getDate("endAt")?.toInstant()
                ?: throw IllegalStateException("endAtが入っていません"),
            allDay = getBoolean("allDay") ?: throw IllegalStateException("allDayが入っていません"),

            )

        "task" -> ScheduleItem.Task(
            id = id,
            calendarId = calendarId,
            title = title,
            description = description,
            createdBy = createdBy,
            reminderMinutesBefore = reminderMinutesBefore,
            updatedAt = updatedAt,
            dueAt = getDate("dueAt")?.toInstant()
                ?: throw IllegalStateException("dueAtが入っていません"),
            isCompleted = getBoolean("isCompleted")
                ?: throw IllegalStateException("isCompletedが入っていません"),
        )

        else -> throw IllegalStateException("typeが不正です")
    }
}
