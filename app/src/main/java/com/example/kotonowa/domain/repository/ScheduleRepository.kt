package com.example.kotonowa.domain.repository

import com.example.kotonowa.domain.model.ScheduleItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 予定/タスクまわりで「できること」の約束だけを並べたもの（お品書き）。
 *
 * ここには「どうやるか」は書かない。実際のやり方は data 層の
 * `ScheduleRepositoryImpl` が Firestore を使って実装する（Step 15）。
 * こうしておくと ViewModel は保存先が Firestore かどうかを知らずに済み、
 * テストのときは偽物の実装に差し替えられる。
 */
interface ScheduleRepository {
    suspend fun addItem(item: ScheduleItem): Result<Unit> //追加

    suspend fun updateItem(item: ScheduleItem): Result<Unit> //更新

    suspend fun deleteItem(itemId: String): Result<Unit> //削除

    suspend fun getItem(itemId: String): Result<ScheduleItem> //1件を取得

    fun observeItem(itemId: String): Flow<ScheduleItem?>

    fun observeItems(
        calendarId: String,
        from: Instant,
        to: Instant,
    ): Flow<List<ScheduleItem>> //一覧を監視

    /**
     * 1 つのカレンダーの「タスクだけ」を、期限に関係なく全部見張る（Step 47。タスク一覧画面のため）。
     *
     * 戻り値を List<ScheduleItem.Task> にしているのは、タスク一覧の画面が予定（Event）を受け取らないようにするため。
     * 型で「タスクしか来ない」と決めておけば、画面で Event の枝を書かずに済む（Step 21 の toggleCompleted(task) と同じ考え方）。
     * 並べ替えはしないで返す（並べ方は画面ごとに違うので、受け取った側で決める）。
     */
    fun observeTasks(calendarId: String): Flow<List<ScheduleItem.Task>>
}
