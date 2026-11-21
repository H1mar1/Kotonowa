package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.ScheduleRepository
import com.google.firebase.auth.FirebaseAuth
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
 * 最後に書いた人の uid（Step 44）。Cloud Functions が「書いた本人には通知しない」ために読む。
 * セキュリティルールも「自分の uid であること」を確かめる。
 */
private const val FIELD_UPDATED_BY = "updatedBy"

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
    // Step 44：「今書いているのは誰か」を知るため（FirebaseModule が届けてくれる）
    private val firebaseAuth: FirebaseAuth,
) : ScheduleRepository {

    /**
     * 予定の中身（toMap）に「書いた人＝今ログインしている自分」を足した Map を作る（Step 44）。
     *
     * ScheduleItem には updatedBy を持たせず、この層で付ける（`type` や `sortAt` と同じ扱い）。
     * 画面の頭脳が入れ忘れることが無く、他人の uid を入れることもできない。
     *
     * クラスの中に書いた拡張関数なので、このクラスの firebaseAuth をそのまま使える（grammar §4-(140)）。
     */
    private fun ScheduleItem.toMapWithWriter(): Map<String, Any?> {
        // TODO(44-A-1) 誰の：Firebase 認証の受付（firebaseAuth）の ／ 何か：今ログインしている人（currentUser）の uid
        //   ログインしていなければ null。null なら書けない（ルールにも断られる）ので、下の ?: throw で失敗にする
        val myUid = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("ログインしていないので保存できません")

        // TODO(44-A-2) 何を返す：予定の中身（toMap()）に、名札 FIELD_UPDATED_BY と値 myUid の組を「足した」Map
        //   Map + (名札 to 値) で、1 項目増えた新しい Map ができる（grammar §4-(140)）
        return toMap() + (FIELD_UPDATED_BY to myUid)
    }

    override suspend fun addItem(item: ScheduleItem): Result<Unit> = try {
        firestore.collection(COLLECTION_EVENTS)
            .document(item.id)
            // TODO(44-A-3) 保存する中身を、書いた人つきの Map に替える（上で作った関数。item の）
            .set(item.toMapWithWriter())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateItem(item: ScheduleItem): Result<Unit> = try {
        firestore.collection(COLLECTION_EVENTS)
            .document(item.id)
            // TODO(44-A-4) こちらも書いた人つきの Map に替える（44-A-3 と同じ）
            .update(item.toMapWithWriter())//そのドキュメントが存在している時だけ書き換える
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
            .whereLessThan("sortAt", Date.from(to))
            .orderBy("sortAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toScheduleItem() }.getOrNull() }
                    ?: emptyList()

                trySend(items)
            }


        awaitClose {
            registration.remove()
        }
    }

    /**
     * 1 つのカレンダーのタスクを全部見張る（Step 47）。形は observeItems とほぼ同じ。違いは 2 つ。
     *  ① 期間（sortAt の範囲）で絞らず、代わりに type が "task" のものだけに絞る
     *  ② orderBy を付けない。「〜と等しい」の条件だけなら、Firestore は複合インデックス（Step 16-F）を作らなくても検索できる。
     *     並べ替えは受け取った側（タスク一覧の頭脳）が行う
     */
    override fun observeTasks(calendarId: String): Flow<List<ScheduleItem.Task>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_EVENTS)
            // TODO(47-A-1) 絞り込み 2 つ。どちらも「〜と等しい」（observeItems の 1 つ目と同じ命令）
            //   1 つ目：calendarId が、引数の calendarId と等しい
            //   2 つ目：type が "task" と等しい（保存するとき toMap で付けている文字。予定は "event"）
            .whereEqualTo("calendarId",calendarId)
            .whereEqualTo("type", "task")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val tasks = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toScheduleItem() }.getOrNull() }
                    // TODO(47-A-2) 翻訳した ScheduleItem の並びから、Task だけを拾う（grammar §4-(141) の filterIsInstance）
                    //   type で絞っているので全部 Task のはずだが、型（List<ScheduleItem>）はそれを知らない。ここで Task の並びに変える
                    ?.filterIsInstance<ScheduleItem.Task>()
                    ?: emptyList()

                trySend(tasks)
            }

        awaitClose { registration.remove() }
    }

    override fun observeItem(itemId: String): Flow<ScheduleItem?> = callbackFlow {
        val registration = firestore.collection(COLLECTION_EVENTS)
            .document(itemId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val item = if (snapshot != null && snapshot.exists()) {
                    runCatching { snapshot.toScheduleItem() }.getOrNull()
                } else {
                    null
                }
                trySend(item)
            }
        awaitClose { registration.remove() }
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
