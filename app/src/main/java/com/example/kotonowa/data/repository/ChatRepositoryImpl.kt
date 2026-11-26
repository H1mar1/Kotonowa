package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.ChatMessage
import com.example.kotonowa.domain.model.ChatRoomId
import com.example.kotonowa.domain.model.DirectChat
import com.example.kotonowa.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
private const val COLLECTION_DIRECT_CHATS = "directChats"
private const val SUBCOLLECTION_MESSAGES = "messages"
private const val FIELD_SENDER_UID = "senderUid"
private const val FIELD_SENDER_NAME = "senderName"
private const val FIELD_TEXT = "text"
private const val FIELD_CREATED_AT = "createdAt"

/** DM の部屋の書類（directChats/{chatId}）の名札（Step 51-C）。memberUids は CalendarRepositoryImpl の FIELD_MEMBER_UIDS を借りる */
private const val FIELD_MEMBER_NAMES = "memberNames"
private const val FIELD_VIA_CALENDAR_ID = "viaCalendarId"
private const val FIELD_LAST_MESSAGE_TEXT = "lastMessageText"
private const val FIELD_LAST_MESSAGE_AT = "lastMessageAt"

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
     * そのトークの発言の棚。部屋の書類から 1 段降りる（36-C-4 の memberDoc と同じ作り方）。
     * 3 つの係のすべてがここを通るので、グループか DM かを見分けるのはこの 1 か所だけ（Step 51-B）。
     *
     *   グループ … calendars/{calendarId}/messages
     *   DM       … directChats/{chatId}/messages
     */
    private fun messagesOf(roomId: ChatRoomId): CollectionReference {
        // 札の種類で、部屋の書類（1 段上）を選ぶ。when の答えをそのまま roomDoc に入れる（grammar §7-㉙）
        val roomDoc = when (roomId) {
            // TODO(51-B-2) 枝の中では roomId がその種類として扱える（スマートキャスト。§4-⑱）
            //   グループ：棚は COLLECTION_CALENDARS ／ 書類は roomId の calendarId（51-A-1 で付けた名前）
            //   DM      ：棚は上に足した COLLECTION_DIRECT_CHATS ／ 書類は roomId の chatId
            is ChatRoomId.Group -> firestore.collection(COLLECTION_CALENDARS)
                .document(roomId.calendarId)

            is ChatRoomId.Direct -> firestore.collection(COLLECTION_DIRECT_CHATS)
                .document(roomId.chatId)
        }
        return roomDoc.collection(SUBCOLLECTION_MESSAGES)
    }

    // TODO(51-B-3) 3 つの係の引数を約束（ChatRepository）と同じ「roomId: 札の型」に。中で messagesOf に渡すのも roomId
    override fun observeMessages(roomId: ChatRoomId): Flow<List<ChatMessage>> = callbackFlow {
        val registration = messagesOf(roomId)
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

    override suspend fun sendMessage(roomId: ChatRoomId, text: String): Result<Unit> = try {
        // 今ログインしている自分。いなければ送れない（ルールにも断られる）ので失敗にする
        val me = firebaseAuth.currentUser
            ?: throw IllegalStateException("ログインしていないので送れません")

        // 送った人の名前（下の chatName。DM の部屋に写す名前と同じ作り方なので、51-C で 1 か所にまとめた）
        val senderName = me.chatName()

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
        messagesOf(roomId).add(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 発言を 1 件消す（Step 50）。お手本は ScheduleRepositoryImpl の deleteItem。
     * 違うのは棚だけ：あちらは events の棚、こちらはそのカレンダーの発言の棚（上の messagesOf）。
     */
    override suspend fun deleteMessage(roomId: ChatRoomId, messageId: String): Result<Unit> = try {
        // TODO(50-A-2) 誰の：このカレンダーの発言の棚（messagesOf(calendarId)）の ／ どれ：messageId の書類を ／
        //   何をする：消して（delete）、終わるまで待つ（await）。deleteItem の 4 行と同じ形（1 行目だけ違う）
        messagesOf(roomId)
            .document(messageId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---- DM の部屋（Step 51-C） ------------------------------------------------

    override suspend fun openDirectChat(
        otherUid: String,
        otherName: String,
        viaCalendarId: String,
    ): Result<String> = try {
        val me = firebaseAuth.currentUser
            ?: throw IllegalStateException("ログインしていないので開けません")

        // 部屋の id：2 人の uid を小さい順に並べて「_」でつなぐ（grammar §4-(151)。Claude が書いた）
        //   A さんが開いても B さんが開いても、同じ id になる（順番を決めておかないと 2 部屋できる）
        val chatId = listOf(me.uid, otherUid).sorted().joinToString("_")
        val chatDoc = firestore.collection(COLLECTION_DIRECT_CHATS).document(chatId)

        // TODO(51-C-3) 誰の：部屋の書類（chatDoc）を ／ 何をする：1 回だけ読んで（get）、届くまで待つ（await）
        //   お手本は CalendarRepositoryImpl.getCalendar の .document(calendarId) の後ろの 2 行（1 行につなげてよい）
        val snapshot = chatDoc.get().await()

        // まだ無ければ作る。もうあれば何もしない（作り直すと最後の発言などが消える）
        if (!snapshot.exists()) {
            // TODO(51-C-4) 保存する中身を「名札 to 値」で 4 行（sendMessage の data と同じ形）
            //   2 人の uid   … 自分の uid（me.uid）と相手の uid（otherUid）の並び（listOf(…, …)）
            //   2 人の名前   … 「uid to 名前」の表（mapOf(…, …)。自分の名前は me.chatName()、相手は引数の otherName）
            //   どこで一緒か … 引数の viaCalendarId
            //   作った時刻   … サーバーの時刻（sendMessage の時刻と同じ）
            //   最後の発言（lastMessageText / lastMessageAt）は書かない。Functions が発言のたびに書く（51-E）
            val data = mapOf(
                FIELD_MEMBER_UIDS to listOf(me.uid, otherUid),
                FIELD_MEMBER_NAMES to mapOf(me.uid to me.chatName(), otherUid to otherName),
                FIELD_VIA_CALENDAR_ID to viaCalendarId,
                FIELD_CREATED_AT to FieldValue.serverTimestamp(),
            )
            chatDoc.set(data).await()
        }

        Result.success(chatId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeMyDirectChats(myUid: String): Flow<List<DirectChat>> = callbackFlow {
        val registration = firestore.collection(COLLECTION_DIRECT_CHATS)
            // TODO(51-C-5) memberUids に自分（myUid）が入っている部屋だけに絞る
            //   お手本は CalendarRepositoryImpl.observeMyCalendars の絞り込みの 1 行（そっくり同じ。渡す uid の名前だけ違う）
            .whereArrayContains(FIELD_MEMBER_UIDS, myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // TODO(51-C-6) 書類を 1 件ずつ DirectChat に組み立てる。読めない 1 件は捨てる
                //   observeMessages の messages の 3 行と同じ形。組み立て係は下の toDirectChat で、自分の uid（myUid）を渡す
                val chats = snapshot?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toDirectChat(myUid) }.getOrNull() }
                    ?: emptyList()

                trySend(chats)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun getDirectChat(chatId: String, myUid: String): Result<DirectChat?> = try {
        // TODO(51-F-4) 誰の：DM の部屋の棚（COLLECTION_DIRECT_CHATS）の ／ どれ：chatId の書類を ／ 何をする：1 回読んで待つ
        //   お手本は CalendarRepositoryImpl.getCalendar の snapshot の 4 行（棚の名前と id の名前だけ違う）
        val snapshot = firestore.collection(COLLECTION_DIRECT_CHATS)
            .document(chatId)
            .get()
            .await()

        // TODO(51-F-5) 書類があれば DirectChat に組み立て、無ければ null（getCalendar の Result.success(…) の 1 行と同じ形）
        //   組み立て係は toDirectChat。自分の uid（myUid）を渡す（51-C-6 と同じ）
        Result.success(if(snapshot.exists()) snapshot.toDirectChat(myUid) else null)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * チャットに出す自分の名前。表示名が無い・空なら、メールアドレス（Step 44 の通知の「誰が」と同じ順）。
 * ルールも「通行証の名前かメール」と照合するので、ここで作った名前以外は断られる（Claude が書いた）。
 * 発言の senderName と、DM の部屋の memberNames の両方で使う。
 */
private fun FirebaseUser.chatName(): String =
    displayName?.takeIf { name -> name.isNotBlank() }
        ?: email
        ?: throw IllegalStateException("名前もメールも無いので送れません")


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

/**
 * DM の部屋の書類 1 件を [DirectChat] に組み立てる（Step 51-C）。
 * 書類には 2 人分の uid と名前が入っているので、自分（myUid）ではない方を「相手」として選ぶ。
 */
private fun DocumentSnapshot.toDirectChat(myUid: String): DirectChat {
    // 2 人の uid の並び。配列には専用の取り出し口が無いので as? で受ける（grammar §4-(141)。NotificationSettingRepositoryImpl と同じ形）
    val memberUids = (get(FIELD_MEMBER_UIDS) as? List<*>)
        ?.filterIsInstance<String>()
        ?: throw IllegalStateException("memberUidsが入っていません")

    // TODO(51-C-7) 相手 ＝ 並びの中で「自分（myUid）ではない」最初の 1 人
    //   お手本は grammar §4-(110) の members.firstOrNull { member -> member.uid == myUid }（「==」が逆になる）
    //   ここでは並びの中身が uid の文字そのものなので、{ uid -> … } と名前を付けて uid と myUid を比べる
    val otherUid = memberUids.firstOrNull { uid -> uid != myUid }
        ?: throw IllegalStateException("相手が見つかりません")

    // uid → 名前 の表。表（Map）にも専用の取り出し口が無いので as? で受ける（grammar §4-(151)）
    val memberNames = get(FIELD_MEMBER_NAMES) as? Map<*, *>

    return DirectChat(
        id = id,
        otherUid = otherUid,
        // 表から相手の uid の名前を引く。無ければ（壊れた書類）メンバー画面と同じ「（名前未登録）」
        otherName = memberNames?.get(otherUid) as? String ?: "（名前未登録）",
        // TODO(51-C-8) 最後の発言の本文と時刻。部屋を作ったばかりなら無いので、?: throw で守らない（そのまま null でよい）
        //   本文 … getString(名札)（toChatMessage の text の 1 行目だけ）
        //   時刻 … getTimestamp(名札)?.toDate()?.toInstant()（toChatMessage の createdAt から ESTIMATE と最後の ?: を抜いた形）
        lastMessageText = getString(FIELD_LAST_MESSAGE_TEXT),
        lastMessageAt = getTimestamp(FIELD_LAST_MESSAGE_AT)?.toDate()?.toInstant(),
    )
}
