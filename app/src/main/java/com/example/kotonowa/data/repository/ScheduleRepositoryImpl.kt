package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.ScheduleRepository
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

    override suspend fun addItem(item: ScheduleItem): Result<Unit> {
        // TODO: ① firestore.collection(COLLECTION_EVENTS) で events コレクションを指す
        // TODO: ② .document(item.id) で「この id のドキュメント」を指す
        // TODO: ③ .set(item.toMap()) で中身を書き込み、.await() で終わるまで待つ
        // TODO: ④ 全体を try / catch で囲み、Result.success(Unit) か Result.failure(e) を返す
        //         書き方は AuthRepositoryImpl の sendPasswordResetEmail が見本
        TODO("Step 15-C で実装する")
    }

    override suspend fun updateItem(item: ScheduleItem): Result<Unit> =
        TODO("Step 15-A で実装する")

    override suspend fun deleteItem(itemId: String): Result<Unit> =
        TODO("Step 15-A で実装する")

    override suspend fun getItem(itemId: String): Result<ScheduleItem> =
        TODO("Step 15-A で実装する")

    override fun observeItems(
        calendarId: String,
        from: Instant,
        to: Instant,
    ): Flow<List<ScheduleItem>> = TODO("Step 15-A で実装する")
}

/**
 * [ScheduleItem] を Firestore に渡せる形（Map）に詰め替える。
 *
 * Firestore は Kotlin の型をそのまま保存できないため、名札と値の組にする（§4-㉟）。
 * 日時は Instant のままでは保存できないので Date に変換する（Firestore が Timestamp にしてくれる）。
 */
private fun ScheduleItem.toMap(): Map<String, Any?> {
    // TODO: ① 共通のフィールドを mapOf(...) で並べる
    //         id / calendarId / title / description / createdBy
    //         reminderMinutesBefore / updatedAt
    //         updatedAt は Date.from(updatedAt) と書いて Date に変換する

    // TODO: ② when (this) で Event と Task に分岐し、それぞれ専用のフィールドを足す（§7-㉙）
    //         Event → "type" to "event", startAt, endAt, allDay
    //         Task  → "type" to "task",  dueAt,   isCompleted
    //         type は domain のモデルが持たないので、ここで付ける

    TODO("Step 15-C で実装する")
}
