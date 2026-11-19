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
 * プッシュ通知（共有カレンダーのお知らせ）のチャンネル id（Step 42）。
 *
 * リマインダーとは別の郵便受けにする。端末の設定で「リマインダーは鳴らす、共有のお知らせは鳴らさない」
 * のように、ユーザーが種類ごとに切り替えられるようにするため。
 * AndroidManifest.xml の default_notification_channel_id にも同じ文字列を書いている。
 */
const val CHANNEL_ID_PUSH = "shared_updates"

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

    // ── Step 42：共有カレンダーのお知らせ用の郵便受け ──
    // 作り方は上のリマインダーとまったく同じ。違うのは id・名前・説明の 3 つだけ
    val pushChannel = NotificationChannel(
        // TODO(42-A-1) 何を入れる：この郵便受けの id（このファイルの上で作った、プッシュ通知用の定数）
        CHANNEL_ID_PUSH,
        "共有カレンダーのお知らせ",
        NotificationManager.IMPORTANCE_HIGH,
    )
    pushChannel.description = "招待や、他のメンバーによる予定の追加・編集をお知らせします"

    // TODO(42-A-2) 誰の：通知係（manager）の ／ 何をする：上で作ったプッシュ通知用の郵便受けを設置する（3 行上のリマインダーと同じ命令）
    manager.createNotificationChannel(pushChannel)
}
