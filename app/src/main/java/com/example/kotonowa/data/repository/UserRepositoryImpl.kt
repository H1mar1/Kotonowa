package com.example.kotonowa.data.repository

import com.example.kotonowa.domain.model.User
import com.example.kotonowa.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore 上のコレクション名。打ち間違い防止のため定数にする（§5-㉓）。 */
private const val COLLECTION_USERS = "users"

/**
 * [UserRepository] の約束を Cloud Firestore で実際に果たすクラス。
 *
 * お手本は同じフォルダの `ScheduleRepositoryImpl`。形はほとんど同じで、
 *  ・Firestore に触るのはこのファイルまで
 *  ・保存するときは Map に詰め替え、読むときは [User] に戻す（`toUser`）
 *  ・`try` / `catch` で受け止めて `Result` に包んで返す（§6-㉔、§4-⑳）
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override suspend fun saveUser(user: User): Result<Unit> = try {
        // 名刺の住所。ドキュメント ID にその人の uid をそのまま使う
        // （events で item.id を使ったのと同じ考え方。要件定義書 §4 の「設計メモ」）
        val document = firestore.collection(COLLECTION_USERS).document(user.uid)

        // この uid の名刺が既にあるか 1 回読む（createdAt を上書きしないため）
        val snapshot = document.get().await()

        // 毎回書き換えてよい項目。
        // email は Step 35 の検索に使う。Firestore の検索は大文字小文字を区別するので、
        // 保存する時点で小文字に揃えておく（「あれば小文字にする」＝ ?. 。§4-㊻）
        val base = mapOf(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "email" to user.email?.lowercase(),
            "photoUrl" to user.photoUrl,
        )

        // createdAt は「最初の 1 回だけ」入れる。
        // 2 回目以降に入れると、ログインのたびに登録日が今日に書き換わってしまう（§5-(68)）。
        val extra: Map<String, Any?> = if (snapshot.exists()) {
            emptyMap()
        } else {
            mapOf("createdAt" to Date.from(Instant.now()))
        }

        // base + extra で 2 つの Map を合体させて渡す（§4-㊴）。
        // merge() は「書いた項目だけ差し替える」（§4-(109)）。Phase4 の fcmTokens を消さないため。
        document.set(base + extra, SetOptions.merge()).await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// toUser（書類 → User）は、使っていた findUserByEmail / getUser と一緒に Step 46 で削除した。
