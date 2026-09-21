package com.example.kotonowa.data.local

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `ReminderScheduler` の約束を WorkManager で実際に果たすクラス（grammar §8-(106)）。
 *
 * WorkManager に触れるのはこのファイルまで。domain / presentation へは漏らさない。
 *
 * ⚠️ WorkManager は「だいたいこの頃」で動く仕組みで、時刻ぴったりは保証されない
 * （端末の省電力状態によっては数分遅れる）。分単位の正確さが要るようになったら
 * AlarmManager の setExactAndAllowWhileIdle に寄せる。
 */
@Singleton
class ReminderSchedulerImpl @Inject constructor(
    // @ApplicationContext ＝ 画面より長生きする「アプリ全体の Context」を配ってもらう印
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    override fun schedule(item: ScheduleItem) {

        // 通知なし（null）なら、予約せずに古い予約を消して終わり
        val minutesBefore = item.reminderMinutesBefore ?: return cancel(item.id)

        // 予定は開始、タスクは期限を基準にする（grammar §5-㊳ の値を返す when）
        val baseAt = when (item) {
            is ScheduleItem.Event -> item.startAt
            is ScheduleItem.Task -> item.dueAt
        }

        // 「○分前」を引いて、通知を出したい時刻を求める（grammar §4-(61) の plus/minus）
        val notifyAt = baseAt.minus(minutesBefore.toLong(), ChronoUnit.MINUTES)

        // ★5 setInitialDelay は「時刻」ではなく「あとどれくらい」なので、今との差を出す
        //     （grammar §8-(106) の⚠️）
        val delayMillis = Duration.between(Instant.now(), notifyAt).toMillis()

        // 通知時刻が既に過ぎているなら予約しない（過去の通知は鳴らせない）
        if (delayMillis <= 0) return cancel(item.id)

        // Worker に渡す材料。単純な値しか渡せないので、予定そのものは渡さない
        val data = workDataOf(
            KEY_TITLE to item.title,
            KEY_MESSAGE to "まもなく始まります",
            // 通知 id は Int。予定ごとに別の通知にしたいので id から数値を作る
            KEY_NOTIFICATION_ID to item.id.hashCode(),
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            // ★6 何ミリ秒後に動かすか（grammar §8-(106)）
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            // 予定ごとに決まった名前。編集時に置き換え、削除時に取り消すために要る
            workName(item.id),
            // ★7 同じ名前の予約が既にあったら「古い方を捨てて置き換える」方針
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun cancel(itemId: String) {
        // ★8 その名前の予約を取り消す命令（grammar §8-(106) の表）
        WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
    }
}

/** 予約の名前。予定 1 件につき 1 つに決まるようにする。 */
private fun workName(itemId: String) = "reminder_$itemId"
