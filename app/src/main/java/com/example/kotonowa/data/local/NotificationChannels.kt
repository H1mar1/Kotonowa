package com.example.kotonowa.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * リマインダー通知のチャンネル id。
 *
 * 通知を出すとき（Step 26-D）にも**同じ文字列**を渡す必要がある。
 * 打ち間違えてもコンパイルは通り、静かに通知が出なくなるだけなので定数にする
 * （grammar §4-(52) の「名札は文字列で伝えるしかない」と同じ罠）。
 */
const val CHANNEL_ID_REMINDER = "reminder"

/**
 * 通知チャンネル（＝通知の種類ごとの受け皿）を作る。
 *
 * Android 8.0 以降、通知はチャンネルに属していないと**エラーも出ないまま表示されない**
 * （grammar §8-(99)）。アプリ起動時に 1 回呼べばよく、同じ id で何度呼んでも増えない。
 *
 * minSdk 26 なので「チャンネルが無い古い Android」の分岐は要らない（要件定義書 §2）。
 */
fun createNotificationChannels(context: Context) {

    val channel = NotificationChannel(
        CHANNEL_ID_REMINDER,
        "リマインダー",
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    channel.description = "予定・タスクの時刻をお知らせします"

    // NotificationManager は OS が持っている「通知係」。自分では作らず借りてくる
    val manager = context.getSystemService(NotificationManager::class.java)

    // ★3 作ったチャンネルを OS に登録する命令（grammar §8-(99) の例）
    manager.createNotificationChannel(channel)
}
