package com.example.kotonowa.data.remote

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kotonowa.MainActivity
import com.example.kotonowa.data.local.CHANNEL_ID_PUSH
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.PushTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cloud Functions（Step 43 / 44）が送る中身の名札。送る側の TypeScript と同じ文字列にする
 * （打ち間違えても静かに読めないだけなので定数にする。grammar §4-(52)）。
 */
const val KEY_PUSH_TITLE = "title"
const val KEY_PUSH_BODY = "body"

/** Step 50：チャットの通知だけに載っている「どのカレンダーのトークか」の名札（Functions の KEY_PUSH_CHAT_CALENDAR_ID と同じ綴り） */
const val KEY_PUSH_CHAT_CALENDAR_ID = "chatCalendarId"

/** Step 50：通知 → MainActivity へ渡す依頼書（Intent）に付ける、トーク画面のカレンダー id の名札（grammar §8-(150)） */
const val EXTRA_CHAT_CALENDAR_ID = "com.example.kotonowa.CHAT_CALENDAR_ID"

/**
 * プッシュ通知を受け取る係（Step 42。grammar §8-(136)）。
 *
 * Firebase から呼ばれる入口は 2 つ。
 * - [onNewToken] … この端末の住所（FCM トークン）が新しくなったとき
 * - [onMessageReceived] … 通知が届いたとき
 *
 * Worker と同じく OS（と Firebase）が作るクラスなので、コンストラクタに材料を足せない。
 * そこで @AndroidEntryPoint ＋ @Inject lateinit var で、Hilt に「作られたあとで」材料を配ってもらう（grammar §8-(137)）。
 */
@AndroidEntryPoint
class KotonowaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var pushTokenRepository: PushTokenRepository

    // この係専用の「裏で走らせる場所」。ViewModel ではないので viewModelScope が無く、自分で作る。
    // 係が片付けられるとき（onDestroy）に止める（grammar §8-(138)）
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 住所が新しくなったら、名刺の住所も更新する。
     * 住所は「アプリを入れ直した」「Firebase の都合」などで変わることがある。
     * ホームを開いたときにも登録しているが（Step 41-C）、ホームを開かないまま住所が変わった場合の取りこぼしを防ぐ。
     */
    override fun onNewToken(token: String) {
        // ログインしていなければ名刺が無い（書き込む権限も無い）ので何もしない。次にホームを開いたとき登録される
        val myUid = authRepository.currentUser?.uid ?: return

        scope.launch {
            // TODO(42-B-1) 誰の：宛先の登録係の ／ 何をする：自分（myUid）の名刺に、この端末の住所を登録する（41-C-1 と同じ 1 行）
            pushTokenRepository.registerToken(myUid)
        }
    }

    /**
     * 通知が届いたら、見出しと本文を取り出して画面に出す。
     *
     * 届く中身は 2 種類ある。
     * - message.data … 自分たちで決めた名札つきの中身（Step 43 以降の Functions はこちらで送る）
     * - message.notification … Firebase コンソールの「テスト送信」が使う形（42-D の確認で使う）
     * どちらから来ても出せるよう、data を先に見て、無ければ notification を見る。
     */
    override fun onMessageReceived(message: RemoteMessage) {
        // 本文：data の body → 無ければ notification の body → それも無ければ空文字
        //   message.data は Map（grammar §4-㉟）。[名札] で取り出すと、無いときは null
        val body = message.data[KEY_PUSH_BODY] ?: message.notification?.body ?: ""

        // TODO(42-B-2) 見出しも上の本文と同じ形で取り出す。ただし最後は「無ければ通知を出さずに終わる」
        //   使う名札は KEY_PUSH_TITLE、notification 側は title。最後の「それも無ければ」は "" ではなく return
        //   （見出しの無い通知は出さない。ReminderWorker の ★1 と同じ考え方）
        val title = message.data[KEY_PUSH_TITLE] ?: message.notification?.title ?: return

        // Step 50：チャットの通知なら、どのカレンダーのトークか。予定・招待の通知には載っていないので null
        // TODO(50-F-1) message.data から、名札 KEY_PUSH_CHAT_CALENDAR_ID の値を取り出す（すぐ上の body の行の最初の部分と同じ形。
        //   無ければ null のままでよいので、?: は付けない）
        val chatCalendarId = message.data[KEY_PUSH_CHAT_CALENDAR_ID]

        showNotification(title, body, chatCalendarId)
    }

    /**
     * @param chatCalendarId チャットの通知なら、そのカレンダーの id（Step 50）。押されたらそのトーク画面を開く。null ならアプリを開くだけ
     */
    private fun showNotification(title: String, body: String, chatCalendarId: String?) {
        // Android 13 以降は許可が無いと出せない（grammar §8-(100)。ReminderWorker と同じ）
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        // 通知 id は「今の時刻」から作る。届いた通知が前の通知を上書きせず、並んで出る（grammar §8-(105) の ⚠️）
        val notificationId = System.currentTimeMillis().toInt()

        // 通知をタップしたらアプリを開く（grammar §8-(139)）
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Step 50：チャットの通知なら、依頼書に「このカレンダーのトークを開いて」を書き添える（grammar §8-(150)）
            if (chatCalendarId != null) {
                // TODO(50-F-2) 依頼書に、名札 EXTRA_CHAT_CALENDAR_ID で chatCalendarId を書き添える（putExtra(名札, 値)。§8-(150)）
                putExtra(EXTRA_CHAT_CALENDAR_ID, chatCalendarId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            // 通知ごとに違う番号にする（Step 50。Claude が書いた）。同じ 0 のままだと、2 つ目の通知の依頼書が
            // 1 つ目と「同じもの」と見なされ、書き添えた id が古いまま（別のトークが開く）になる（grammar §8-(150)）
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(
            this,
            // TODO(42-B-3) どの郵便受けに入れるか：42-A で作ったプッシュ通知用の id（ReminderWorker の ★2 はリマインダー用）
            CHANNEL_ID_PUSH,
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(notificationId, notification)
    }

    override fun onDestroy() {
        // 係が片付けられるとき、裏で走っている作業も止める
        scope.cancel()
        super.onDestroy()
    }
}
