package com.example.kotonowa.presentation.calendar.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.ReminderScheduler
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 予定/タスクの詳細画面の頭脳。
 *
 * 一覧の行から navigate されるとき、行き先のルートに itemId が埋め込まれている
 * （"schedule_detail/abc123"）。その abc123 を SavedStateHandle 経由で受け取り、
 * getItem で 1 件読み込んで画面に見せる。
 *
 * 削除できたら isDeleted を立てるだけ。画面遷移（一覧へ戻る）は知らない
 * ＝ ScheduleEditViewModel が isSaved を立てるだけなのと同じ分担。
 */
@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository, // 予定/タスクを出し入れする窓口
    private val reminderScheduler: ReminderScheduler,
    private val authRepository: AuthRepository,         // 自分の uid を知る窓口（Step 38）
    private val calendarRepository: CalendarRepository, // 名簿（members）を見る窓口（Step 38）
    savedStateHandle: SavedStateHandle, // NavHost が渡してくる「宛名付きメモ」
) : ViewModel() {

    // ルート "schedule_detail/{itemId}" の {itemId} に入っていた値。
    // 詳細画面は必ず itemId 付きで開かれるので、無かったらプログラムのバグ → checkNotNull で即落とす。
    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    /** 自分の uid。マイカレンダーの id でもある（CalendarViewModel の uid と同じ）。 */
    private val uid = authRepository.currentUser?.uid

    /** 肩書きの見張り（Step 38）。CalendarViewModel の roleJob と同じ。 */
    private var roleJob: Job? = null

    /**
     * 予定 1 件の見張り（Step 48）。削除する前に止めるために覚えておく。
     * 止めないと、消えた予定を見張り続けてルールに断られる（PERMISSION_DENIED）。
     */
    // TODO(48-A-1) すぐ上の roleJob と同じ形の箱を、名前 itemJob で作る（最初は「まだ見張っていない」＝ null）
    private var itemJob: Job? = null

    /** いま肩書きを見張っているカレンダーの id。同じカレンダーなら見張り直さないために覚えておく。 */
    private var roleCalendarId: String? = null

    private val _uiState = MutableStateFlow(ScheduleDetailUiState())
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    // ViewModel が作られた直後に 1 回だけ走る。開いた瞬間に読み込みを始めたいのでここで呼ぶ。
    init {
        load()
    }

    /** itemId の 1 件を読み込んで uiState.item に入れる。 */
    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        // TODO(48-A-2) 始めた見張り（launch の戻り値）を、箱 itemJob に入れておく（observeRole の roleJob = … と同じ形）
        itemJob = viewModelScope.launch {
            try {
                scheduleRepository.observeItem(itemId)
                    .collect { item ->
                        _uiState.update { it.copy(item = item, isLoading = false) }

                        // 予定が分かったら、その予定が置いてあるカレンダーでの肩書きを調べる（Step 38）。
                        // 削除されると null が流れてくるので、null でないときだけ
                        if (item != null) {
                            // TODO(38-C-2) 誰の：この頭脳の observeRole を ／ 何をする：この予定の置き場所（calendarId）で呼ぶ
                            //   ホームの 38-A-2 との違いは、渡すのが「カレンダー」ではなく「予定の calendarId（文字）」なこと
                            observeRole(item.calendarId)
                        }
                    }
            }
            // TODO(48-A-3) 止めた（cancel）ときの合図は失敗ではないので、投げ直す受け止め口を、下の catch より「上」に足す
            //   observeRole の 1 つ目の catch（catch (e: 〇〇) { throw e }）と同じ形。必ず Exception より上（順番が命。§6-㉕）
            //   ⚠️ これが無いと、削除の前に見張りを止めた瞬間、下の catch に入って「読み込めません」が出る（Step 23 の ⚠️）
            catch (e: CancellationException) {
                throw e   // 意図して取り止めた合図（grammar §2-(91)）。Exception より上に置く
            }
            catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "読み込めません")
                }
            }
        }
    }


    /**
     * このカレンダーでの自分の肩書きを見張り、canEdit を UiState に入れる（Step 38）。
     * お手本は CalendarViewModel.observeRole（38-A）。違いは 2 つだけ。
     *  ・受け取るのがカレンダーではなく calendarId（予定が知っているのは置き場所の id だけ）
     *  ・マイカレンダーかどうかを「null か」ではなく「id が自分の uid と同じか」で見分ける
     *    （Step 33 の決まり：マイカレンダーの id ＝ uid。ルールの canEdit の左側と同じ判定）
     */
    private fun observeRole(calendarId: String) {
        // 予定は編集されるたびに流れてくるが、置き場所は変わらない（ルールで禁止。Step 33-F）。
        // 同じカレンダーなら、もう見張っているので何もしない
        if (calendarId == roleCalendarId) return
        roleCalendarId = calendarId
        roleJob?.cancel()

        val myUid = uid ?: return

        // TODO(38-C-3) マイカレンダーか。誰の：この予定の置き場所（calendarId）が ／ 何か：自分の uid（myUid）と同じか（==）
        if (calendarId == myUid) {
            _uiState.update { it.copy(canEdit = true) }
            return
        }

        roleJob = viewModelScope.launch {
            try {
                calendarRepository.observeMembers(calendarId).collect { members ->
                    // TODO(38-C-4) 名簿から自分の肩書き（38-A-3 とまったく同じ式）
                    val myRole = members.firstOrNull { member -> member.uid == myUid }?.role

                    // TODO(38-C-5) オーナー「か」編集者（38-A-4 とまったく同じ式）
                    _uiState.update { it.copy(canEdit = myRole == MemberRole.OWNER || myRole == MemberRole.EDITOR) }
                }
            } catch (e: CancellationException) {
                throw e   // 意図して取り止めた合図（grammar §2-(91)）。Exception より上に置く
            } catch (e: Exception) {
                // 名簿を読めない（外された等）。canEdit は「いいえ」のまま
            }
        }
    }

    /** itemId の 1 件を削除する。成功したら isDeleted を立てる（画面がそれを見て戻る）。 */
    fun delete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        // TODO(48-A-4) 消す前に、予定の見張りを止める。誰の：箱 itemJob の ／ 何をする：止める（まだ無いかもしれないので ?.）
        itemJob?.cancel()

        viewModelScope.launch {
            scheduleRepository.deleteItem(itemId)
                .onSuccess {
                    reminderScheduler.cancel(itemId)
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "削除できませんでした"
                        )
                    }
                    // TODO(48-A-5) 消せなかった＝予定はまだある。止めた見張りを始め直す（init で呼んでいるのと同じ関数を呼ぶ）
                    load()
                }
        }
    }

}