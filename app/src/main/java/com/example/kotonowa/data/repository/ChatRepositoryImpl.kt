package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.ChatMessage
import com.example.kotonowa.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore 上の名前。打ち間違い防止のため定数にする（§5-㉓）。ルール（firestore.rules）と Functions も同じ綴りで読む */
private const val SUBCOLLECTION_MESSAGES = "messages"
private const val FIELD_SENDER_UID = "senderUid"
private const val FIELD_SENDER_NAME = "senderName"
private const val FIELD_TEXT = "text"
private const val FIELD_CREATED_AT = "createdAt"

/**
 * [ChatRepository] の約束を Cloud Firestore で果たすクラス（Step 49）。
 *
 * 発言の置き場所は `calendars/{calendarId}/messages/{発言id}`（要件定義書 §4）。
 * 部屋の下に置くので、ルールの「この部屋のメンバーか」（isMember）がそのまま使える。
 * 送った人の uid と名前はこの層で「今ログインしている自分」から付ける（Step 44 の toMapWithWriter と同じ考え方）。
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : ChatRepository {

    /**
     * そのカレンダーの発言の棚。部屋の書類から 1 段降りる（36-C-4 の memberDoc と同じ作り方）。
     * observeMessages と sendMessage の両方で使うので、関数にしてまとめた（Claude が書いた）。
     */
    private fun messagesOf(calendarId: String): CollectionReference =
        firestore.collection(COLLECTION_CALENDARS)
            .document(calendarId)
            .collection(SUBCOLLECTION_MESSAGES)

    override fun observeMessages(calendarId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = messagesOf(calendarId)
            // TODO(49-B-1) 送った時刻の古い順に並べてもらう（grammar §4-(147) の orderBy。何で並べるか＝上の定数のどれか）
            //   絞り込みが無く並べ替え 1 つだけなので、複合インデックスは要らない
            .orderBy(FIELD_CREATED_AT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // TODO(49-B-2) 書類を 1 件ずつ ChatMessage に組み立てる。読めない 1 件は捨てる
                //   observeTasks の tasks の 3 行と同じ形。ただし Task だけを拾う行（filterIsInstance）は要らない。組み立て係は下の toChatMessage
                val messages = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toChatMessage() }.getOrNull() }
                    ?: emptyList()

                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(calendarId: String, text: String): Result<Unit> = try {
        // 今ログインしている自分。いなければ送れない（ルールにも断られる）ので失敗にする
        val me = firebaseAuth.currentUser
            ?: throw IllegalStateException("ログインしていないので送れません")

        // 送った人の名前：表示名が無い・空なら、メールアドレス（Step 44 の通知の「誰が」と同じ順）。
        // ルールも「通行証の名前かメール」と照合するので、ここで作った名前以外は断られる（Claude が書いた）
        val senderName = me.displayName?.takeIf { name -> name.isNotBlank() }
            ?: me.email
            ?: throw IllegalStateException("名前もメールも無いので送れません")

        // TODO(49-B-3) 保存する中身を「名札 to 値」で 4 行（mapOf の中は 1 行ずつ。Step 46 の 46-C-1 と同じ）
        //   送った人の uid … me の uid ／ 名前 … すぐ上の senderName ／ 本文 … 引数の text
        //   時刻 … FieldValue.serverTimestamp()（端末の時計ではなく、サーバーが受け取った時刻を入れてもらう。grammar §4-(147)）
        val data = mapOf(
            FIELD_SENDER_UID to me.uid,
            FIELD_SENDER_NAME to senderName,
            FIELD_TEXT to text,
            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
        )

        // 住所は Firestore に決めてもらう（add）。発言は同じ住所に書き直すことが無いので、自分で id を作らなくてよい（grammar §4-(147)）
        messagesOf(calendarId).add(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 発言を 1 件消す（Step 50）。お手本は ScheduleRepositoryImpl の deleteItem。
     * 違うのは棚だけ：あちらは events の棚、こちらはそのカレンダーの発言の棚（上の messagesOf）。
     */
    override suspend fun deleteMessage(calendarId: String, messageId: String): Result<Unit> = try {
        // TODO(50-A-2) 誰の：このカレンダーの発言の棚（messagesOf(calendarId)）の ／ どれ：messageId の書類を ／
        //   何をする：消して（delete）、終わるまで待つ（await）。deleteItem の 4 行と同じ形（1 行目だけ違う）
        messagesOf(calendarId)
            .document(messageId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// ---- 変換係（このファイルの中だけで使う。§4-㊱） --------------------------

/**
 * 発言の書類 1 件を [ChatMessage] に組み立てる。
 * お手本は InviteRepositoryImpl の toInvite（無くてよい項目は無いので ?: throw で守る）。
 */
private fun DocumentSnapshot.toChatMessage(): ChatMessage = ChatMessage(
    // 書類の住所そのもの（add で Firestore が決めた id）。中身には書いていないので getString ではなく id を読む
    id = id,
    // TODO(49-B-4) 文字の項目 3 つ。toInvite の calendarId の行と同じ形（名札は上の定数を使う）
    senderUid = getString("senderUid")
        ?: throw IllegalStateException("senderUidが入っていません"),
    senderName = getString("senderName")
        ?: throw IllegalStateException("senderNameが入っていません"),
    text = getString("text")
        ?: throw IllegalStateException("textが入っていません"),
    // 送った直後は、まだサーバーの時刻が付いておらず null になる（自分の端末にだけ先に届く）。
    // ESTIMATE（見込み）にすると、その間は「だいたいこの時刻」を入れてくれる（grammar §4-(147)。Claude が書いた）
    createdAt = getTimestamp(FIELD_CREATED_AT, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()
        ?.toInstant()
        ?: Instant.now(),
)
