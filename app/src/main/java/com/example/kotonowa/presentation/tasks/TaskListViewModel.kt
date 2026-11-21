package com.example.kotonowa.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.model.ScheduleItem
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** マイカレンダーの行に出す名前（ホームの ▼ と同じ） */
private const val MY_CALENDAR_NAME = "マイカレンダー"

/**
 * タスク一覧画面の頭脳（Step 47）。
 *
 * 自分が入っている全カレンダーのタスクを 1 本の並びに束ねて、画面に渡す。
 *  ① マイカレンダー（id ＝ 自分の uid）のタスク … いつでも完了チェックを押せる
 *  ② 共有カレンダーごとのタスク ＋ その名簿 … 名簿から自分の肩書きを見て、押せるかを決める
 * カレンダーが増えたり減ったりしたら（招待の承認・外された等）、見張りを張り直す（flatMapLatest。grammar §2-(143)）。
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    // 自分の uid。マイカレンダーの id でもある（Step 33）
    private val myUid: String? = authRepository.currentUser?.uid

    init {
        observeAllTasks()
    }

    /**
     * 全カレンダーのタスクを見張る。
     *
     * 束ねる仕組み（flatMapLatest / combine）は Claude が書いた。grammar §2-(143) を参照。
     * 1 枚の共有カレンダー分の行を作るところ（rowsOfSharedCalendar）だけが穴埋め。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAllTasks() {
        val uid = myUid
        if (uid == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ログイン情報が取得できませんでした"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                calendarRepository.observeMyCalendars(uid)
                    // 入っているカレンダーの一覧が流れてくるたびに、「全カレンダー分のタスクの管」を作り直す
                    .flatMapLatest { calendars ->
                        // ① マイカレンダーの管：タスクを行に変える。いつでも押せる
                        val myRows: Flow<List<TaskRowItem>> = scheduleRepository.observeTasks(uid)
                            .map { tasks ->
                                tasks.map { task ->
                                    TaskRowItem(
                                        task,
                                        MY_CALENDAR_NAME,
                                        canEdit = true
                                    )
                                }
                            }

                        // ② 共有カレンダー 1 枚ずつの管
                        val sharedRows: List<Flow<List<TaskRowItem>>> = calendars.map { calendar ->
                            rowsOfSharedCalendar(calendar.id, calendar.name, uid)
                        }

                        // ①と②の全部の管を 1 本に束ねる。どれか 1 本でも流れてきたら、全部の最新の並びを 1 つにつなげて流す
                        combine(listOf(myRows) + sharedRows) { lists -> lists.toList().flatten() }
                    }
                    .collect { rows ->
                        _uiState.update { it.copy(rows = rows, isLoading = false) }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "タスクを読み込めませんでした"
                    )
                }
            }
        }
    }

    /**
     * 共有カレンダー 1 枚分の「行の管」を作る。
     * タスクの管と名簿の管の 2 本を combine で束ね、どちらかが変わるたびに行を作り直す
     * （オーナーが肩書きを変えたら、チェックの押せる・押せないもすぐ切り替わる。Step 38 と同じ）。
     */
    private fun rowsOfSharedCalendar(
        calendarId: String,
        calendarName: String,
        uid: String,
    ): Flow<List<TaskRowItem>> =
        combine(
            scheduleRepository.observeTasks(calendarId),
            calendarRepository.observeMembers(calendarId),
        ) { tasks, members ->
            // TODO(47-C-1) 名簿（members）から自分の行を探し、その肩書きを取り出す（38-A とまったく同じ 1 行）
            //   自分の行が無ければ null（firstOrNull ＋ ?.role）
            val myRole = members.firstOrNull { member -> member.uid == uid }?.role

            // TODO(47-C-2) 押せるか：肩書きがオーナーか編集者なら「はい」（38-A と同じ基準。「または」は ||）
            val canEdit = myRole == MemberRole.OWNER || myRole == MemberRole.EDITOR

            // TODO(47-C-3) タスク 1 つずつを、行（TaskRowItem）に変える
            //   すぐ上の observeAllTasks の ① の行と同じ形。カレンダーの名前と canEdit が引数の箱に変わるだけ
            tasks.map { task -> TaskRowItem(task, calendarName, canEdit = canEdit) }
        }

    /** 上の「未完了／完了」の切り替え */
    fun onFilterChange(filter: TaskFilter) {
        // TODO(47-C-4) 誰の：状態の箱を ／ 何をする：filter を、引数の filter に書き換える
        _uiState.update { it.copy(filter = filter) }
    }

    /**
     * 行のチェックが押された。完了 ⇄ 未完了を切り替えて保存する。
     * ホームの CalendarViewModel.toggleCompleted（Step 21）とまったく同じ。
     * 保存すると observeTasks の見張りが新しい行を流すので、画面の書き換えは書かない。
     */
    fun toggleCompleted(task: ScheduleItem.Task) {
        viewModelScope.launch {
            // TODO(47-C-5) CalendarViewModel.toggleCompleted の中身（copy → updateItem → onFailure）を写す
            val update = task.copy(
                isCompleted = !task.isCompleted,
                updatedAt = Instant.now(),
            )
            scheduleRepository.updateItem(update)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "更新できませんでした") }
                }
        }
    }
}
