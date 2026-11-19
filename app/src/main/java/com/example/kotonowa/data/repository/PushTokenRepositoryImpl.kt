package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.repository.PushTokenRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val COLLECTION_USERS = "users"
private const val FIELD_FCM_TOKENS = "fcmTokens"

/**
 * [PushTokenRepository] の Firebase 実装（Step 41）。
 *
 * 住所（FCM トークン）は FirebaseMessaging からもらい、名刺（users/{uid}）の fcmTokens に出し入れする。
 * 出し入れは「配列に 1 つ足す／1 つ抜く」専用の命令を使う（Step 30 の memberUids と同じ考え方。grammar §4-(112)）。
 */
@Singleton
class PushTokenRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
) : PushTokenRepository {

    override suspend fun registerToken(uid: String): Result<Unit> = try {
        // この端末の住所をもらう。初めてなら Firebase が発行するので、通信を待つ（.await()）
        // TODO(41-B-3) 誰の：通知の受付（messaging）の ／ 何か：token。もらい終わるまで待つ
        val token = messaging.token.await()

        // 名刺に住所を足す。arrayUnion ＝「まだ無ければ足す」（承認の memberUids と同じ。36-C）
        // set ＋ merge にしているのは、名刺がまだ無い場合でも失敗させないため（update は書類が無いとエラー。Step 19-D）
        // TODO(41-B-4) arrayUnion の ( ) に入れるもの：上でもらった住所
        firestore.collection(COLLECTION_USERS).document(uid)
            .set(mapOf(FIELD_FCM_TOKENS to FieldValue.arrayUnion(token)), SetOptions.merge())
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unregisterToken(uid: String): Result<Unit> = try {
        val token = messaging.token.await()

        // arrayRemove ＝「入っていれば抜く」（arrayUnion の逆。removeMember の memberUids と同じ。Step 30-D）
        // 名刺は必ずある（ログイン中の人）ので update でよい
        // TODO(41-B-5) 誰の：Firestore の users の、この uid の名刺の ／ 何をする：fcmTokens から住所を抜く
        //   上の registerToken の 1 行目（.collection(...).document(uid)）と同じ住所。命令は .update(項目名, 値)（36-C の declineInvite と同じ形）
        firestore.collection(COLLECTION_USERS).document(uid)
            .update(FIELD_FCM_TOKENS, FieldValue.arrayRemove(token))
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
