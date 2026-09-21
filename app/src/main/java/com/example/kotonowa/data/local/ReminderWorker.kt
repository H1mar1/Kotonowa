package com.example.kotonowa.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** 予約するときに渡す値の名札。打ち間違い防止のため定数にする（grammar §4-(52)）。 */
const val KEY_TITLE = "title"
const val KEY_MESSAGE = "message"
const val KEY_NOTIFICATION_ID = "notificationId"

/**
 * 予約した時刻に OS から呼ばれ、通知を 1 つ出す（grammar §8-(103)）。
 *
 * Worker は OS が作るクラスなので、コンストラクタに自由な引数を足せない。
 * 通知に出す文言は予約時に渡し、ここでは [inputData] から名札で取り出す。
 *
 * @HiltWorker / @AssistedInject は「OS が作るときに Hilt が割り込む」仕掛け（grammar §8-(104)）。
 * いまは Repository を使わないので Hilt から配る材料は無いが、
 * 26-G で予定を読み直す必要が出たときにここへ足せる形にしてある。
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        // ★1 予約時に渡された値を名札で取り出す。無ければ予約の作り方が間違っている＝
        //     やり直しても直らないので、Result.retry() ではなく「失敗」で終える
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val message = inputData.getString(KEY_MESSAGE) ?: ""
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 0)

        // Android 13 以降は許可が無いと通知を出せない（grammar §8-(100)）。
        // 出せないだけでやり直す意味は無いので、ここも失敗として終える。
        val granted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.failure()

        val notification = NotificationCompat.Builder(
            applicationContext,
            // ★2 どの郵便受けに入れるか。NotificationChannels.kt で定義した定数
            CHANNEL_ID_REMINDER,
        )
            // ★3 これを忘れると、エラーも出ないまま通知が表示されない（grammar §8-(105)）
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            // ★4 太字の見出しに出す文字。上で取り出した箱
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        // ★5 実際に出す命令（grammar §8-(105)）
        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)

        // ★6 ここまで来たら成功
        return Result.success()
    }
}
