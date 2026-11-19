package com.example.kotonowa.domain.repository

/**
 * プッシュ通知の「宛先」（FCM トークン）を名刺に登録・削除する係の約束（Step 41）。
 *
 * FCM トークン ＝ この端末に通知を届けるための住所。アプリを入れた端末ごとに 1 つ、Firebase が発行する。
 * サーバー（Cloud Functions）は `users/{uid}.fcmTokens` に並んだ住所へ通知を送る。
 * 1 人が複数の端末で使うこともあるので、住所は「並び（配列）」で持つ。
 *
 * 住所の取り出し方（FirebaseMessaging）は Firebase の道具なので、約束には出さない。
 * ViewModel は「登録して」「外して」と頼むだけ。
 */
interface PushTokenRepository {

    /**
     * この端末の住所を、その人の名刺に足す。すでに入っていれば何もしない（同じ住所が 2 つにならない）。
     * ホームを開くたびに呼ぶ（住所は Firebase の都合で変わることがあるため、毎回確かめる）。
     */
    // TODO(41-B-1) 通信するので suspend。成功しても返すものは無い（UserRepository.saveUser と同じ戻り値）
    suspend fun registerToken(uid: String): Result<Unit>

    /**
     * この端末の住所を、その人の名刺から外す。ログアウトの直前に呼ぶ。
     * 外さないと、ログアウトしたあとの端末に前の人宛ての通知が届いてしまう。
     */
    // TODO(41-B-2) 上と同じ戻り値
    suspend fun unregisterToken(uid: String): Result<Unit>
}
