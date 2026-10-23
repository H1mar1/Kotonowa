package com.example.kotonowa.presentation.calendarlist.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * メンバー画面の頭脳。
 *
 * 管理一覧の行から "calendar_members/abc123" の住所で開かれる（Step 34-B）。
 * その abc123 を SavedStateHandle から受け取り、そのカレンダーの名簿を見張る。
 * 名簿が流れてくるたびに、1 人ずつ名刺を引いて名前を付け、画面用の形（[MemberItem]）に詰め替える。
 *
 * お手本は `ScheduleDetailViewModel`（住所から id を受け取る）と `CalendarListViewModel`（Flow を collect する）。
 */
@HiltViewModel
class MemberListViewModel @Inject constructor(
    authRepository: AuthRepository,                     // 自分の uid を 1 回読むだけなので private val にしない（§7-(88)）
    private val calendarRepository: CalendarRepository, // 名簿の窓口（Step 30）
    private val userRepository: UserRepository,         // 名刺の窓口（Step 28 / 34-A）
    savedStateHandle: SavedStateHandle,                 // NavHost が渡してくる「宛名付きメモ」
) : ViewModel() {

    // TODO(34-C-2-1) 誰の：宛名付きメモの ／ 何をする：住所の {calendarId} の値を取り出す
    //   名札は 34-B-1 で { } の中に書いた名前と同じ綴り（文字なのでコンパイラは見てくれない）。
    //   この画面は必ず id 付きで開かれるので、無ければバグ → checkNotNull で即落とす（§4-(87)）
    private val calendarId: String = checkNotNull(savedStateHandle["calendarId"])

    // 自分の uid。「自分がオーナーか」を決めるのに使う
    private val myUid: String? = authRepository.currentUser?.uid

    private val _uiState = MutableStateFlow(MemberListUiState())
    val uiState: StateFlow<MemberListUiState> = _uiState.asStateFlow()

    init {
        observeMembers()
    }

    /** 名簿を見張り、流れてくるたびに名前を引いて uiState に入れる。 */
    private fun observeMembers() {
        viewModelScope.launch {
            try {
                // TODO(34-C-2-2) 誰の：カレンダーの窓口の ／ 何をする：この calendarId の名簿を見張る（Step 30-C で作った関数）
                calendarRepository.observeMembers(calendarId)
                    .collect { members ->

                        // 名簿の 1 行ずつを、名前付きの MemberItem に作り変える（§4-(55)）
                        val items = members.map { member ->
                            // TODO(34-C-2-3) 誰の：名刺の窓口の ／ 何をする：この人の uid で名刺を 1 枚読む（34-A）
                            //   通信に失敗したら名前無しで続けたいので、封筒から中身だけ取り出す。失敗なら null（§4-(56)）
                            val user = userRepository.getUser(member.uid).getOrNull()

                            MemberItem(
                                uid = member.uid,
                                // TODO(34-C-2-4) 表示名 → 無ければメールアドレス → それも無ければ「（名前未登録）」
                                //   ?: は何個でも繋げられる。左から順に、null でない最初のものが選ばれる（§4-(125)）
                                name = user?.displayName ?: user?.email ?: "（名前未登録）",
                                role = member.role,
                            )
                        }.sortedBy { item -> item.role } // オーナー → 編集者 → 閲覧者の順に並べる（§4-(125)）

                        _uiState.update {
                            it.copy(
                                members = items,
                                // TODO(34-C-2-5) 誰の：名簿の ／ 何をする：「uid が自分 かつ 肩書きがオーナー」の行が 1 つでもあるか確かめる
                                //   any { } の中は「はい／いいえ」の式（§4-(125)）。「かつ」は &&（§5-㉒）
                                isOwner = members.any { member -> member.uid == myUid && member.role == MemberRole.OWNER },
                                isLoading = false,
                            )
                        }
                    }
            } catch (e: CancellationException) {
                // 画面を閉じて止められた合図。失敗ではないので上へ通す（§2-(91)。Exception より上に置く）
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "メンバーを読み込めませんでした")
                }
            }
        }
    }

    /**
     * その人の肩書きを [role] に変える。画面ではオーナーにだけボタンを出す（34-D）。
     *
     * 成功したときに書くことは無い。名簿の見張り（[observeMembers]）が変化を拾い、
     * 一覧を作り直して流してくれる（Step 21 の toggleCompleted と同じ）。失敗したときだけ知らせる。
     */
    fun changeRole(uid: String, role: MemberRole) {
        viewModelScope.launch {
            // TODO(34-C-3-1) 誰の：カレンダーの窓口の ／ 何をする：このカレンダーの、この uid の人の肩書きを role に書き換える
            //   Step 30-D で作った関数。引数は「どのカレンダー・誰の・何に」の 3 つ（CalendarRepository.kt で順番を確かめる）
            calendarRepository.updateMemberRole(calendarId, uid, role)
                .onFailure {
                    // TODO(34-C-3-2) 誰の：状態の箱の ／ 何をする：errorMessage に「肩書きを変更できませんでした」を入れる
                    //   下の removeMember の onFailure と同じ形
                    _uiState.update { it.copy(errorMessage = "肩書きを変更できませんでした") }
                }
        }
    }

    /**
     * その人を名簿から外す。画面ではオーナーにだけボタンを出す（34-D）。
     * 成功したら、見張りが「1 人減った名簿」を流してくれる。
     */
    fun removeMember(uid: String) {
        viewModelScope.launch {
            // TODO(34-C-3-3) 誰の：カレンダーの窓口の ／ 何をする：このカレンダーの名簿から、この uid の人を外す
            //   Step 30-D で作った関数。引数は「どのカレンダー・誰を」の 2 つ
            calendarRepository.removeMember(calendarId, uid)
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "メンバーを外せませんでした") }
                }
        }
    }
}
