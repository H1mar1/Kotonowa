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
        // HIGH にすると、音に加えて画面上部にポップアップする（grammar §8-(99)）。
        // ⚠️ 一度作られたチャンネルの重要度は、ここを変えても反映されない。
        // 開発中はアプリを入れ直し、実運用では CHANNEL_ID_REMINDER を変えて作り直す。
        NotificationManager.IMPORTANCE_HIGH,
    )
    channel.description = "予定・タスクの時刻をお知らせします"

    // NotificationManager は OS が持っている「通知係」。自分では作らず借りてくる
    val manager = context.getSystemService(NotificationManager::class.java)

    manager.createNotificationChannel(channel)
}
