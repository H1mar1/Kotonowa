package com.example.kotonowa.presentation.calendarlist.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.Invite
import com.example.kotonowa.domain.model.InviteStatus
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.InviteRepository
import com.example.kotonowa.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
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
    // TODO(35-D-2-2) 招待状の窓口（35-B / 35-C）。上の 2 行と同じ形。名前は inviteRepository
    private val inviteRepository: InviteRepository,
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

    // ---- 35-D-2：招待 -----------------------------------------------------

    /** 招待のメールアドレス欄に文字が打たれたら呼ばれる。打った文字を箱に入れるだけ。 */
    fun onInviteEmailChange(email: String) {
        // TODO(35-D-2-3) 誰の：状態の箱の ／ 何をする：inviteEmail を、打たれた email に差し替える
        //   お手本は ScheduleEditViewModel の onTitleChange
        _uiState.update { it.copy(inviteEmail = email) }
    }

    /** 招待の肩書きが選ばれたら呼ばれる。 */
    fun onInviteRoleChange(role: MemberRole) {
        // TODO(35-D-2-3) 誰の：状態の箱の ／ 何をする：inviteRole を、選ばれた role に差し替える
        _uiState.update { it.copy(inviteRole = role) }
    }

    // ---- 35-D-3：招待状を出す ----------------------------------------------

    /**
     * 入力欄のメールアドレスの人に、このカレンダーへの招待状を出す。
     *
     * ① 名刺（users）から相手を探す → ② カレンダーの名前を読む → ③ 招待状を書く、の順。
     * 前の答えを見て「先へ進むか」を決めるので、ダメなら finishInvite で一言出して return@launch で打ち切る（§(54) ガード節）。
     * お手本は CalendarCreateViewModel の save()（門番 → 旗を立てる → launch → onSuccess / onFailure）。
     */
    fun sendInvite() {
        val uid = myUid ?: return
        val state = _uiState.value

        // 門番①：オーナーでなければ何もしない（ボタンはオーナーにしか出さないが、念のため）
        if (!state.isOwner) return

        // 門番②：送っている途中なら何もしない（二度押しで招待状が 2 枚できるのを防ぐ）
        if (state.isInviting) return

        // TODO(35-D-3-1) 打った文字の、前後の空白を取り除いてから、小文字に揃える（§4-(128)）
        //   誰の：状態の inviteEmail の ／ 何をする：空白を取る → 小文字にする（2 つを . で繋ぐ）
        val email = state.inviteEmail.trim().lowercase()

        // 門番③：空なら弾く。isInviting を立てる前に弾くので、あとで戻さなくてよい
        if (email.isBlank()) {
            _uiState.update { it.copy(inviteMessage = "メールアドレスを入れてください") }
            return
        }

        // TODO(35-D-3-2) 誰の：状態の箱の ／ 何をする：isInviting を「はい」に、inviteMessage を null に（前回の一言を消す）
        //   save() の isSaving = true, errorMessage = null と同じ形
        _uiState.update { it.copy(isInviting = true, inviteMessage = null) }

        viewModelScope.launch {
            // ---- ① 相手を探す ----
            // TODO(35-D-3-3) 誰の：名刺の窓口の ／ 何をする：email で人を探す（Step 28 の関数）。答えは封筒（Result）のまま受け取る
            val userResult = userRepository.findUserByEmail(email)

            // 封筒が「失敗」＝通信できなかった（§4-(128) isFailure）
            if (userResult.isFailure) {
                finishInvite("通信に失敗しました。もう一度お試しください")
                return@launch
            }
            val user = userResult.getOrNull()

            // TODO(35-D-3-4) 通信はできたが、その人が居なかったとき（user が空っぽ）の条件
            if (user == null) {
                finishInvite("このメールアドレスの人は登録されていません")
                return@launch
            }

            // TODO(35-D-3-5) 誰の：今の名簿の行の並び（state.members）の ／ 何をする：uid が見つかった人（user.uid）と同じ行が 1 つでもあるか（§4-(125) any）
            //   自分のメールアドレスを打った場合も、自分は名簿に居るのでここで止まる
            if (state.members.any { member -> member.uid == user.uid }) {
                finishInvite("この人はすでにメンバーです")
                return@launch
            }

            // ---- ② カレンダーの名前を読む ----
            // 招待された人はまだ部屋の書類を読めないので、名前を招待状に写す（35 の設計）。
            // 失敗でも「書類が無い」でも先へ進めないので、getOrNull で 1 つにまとめて null を見る（§4-(56)）
            val calendar = calendarRepository.getCalendar(calendarId).getOrNull()
            if (calendar == null) {
                finishInvite("カレンダーを読み込めませんでした")
                return@launch
            }

            // ---- ③ 招待状を組み立てて、書く ----
            // TODO(35-D-3-6) 6 つの欄を埋める。左の名前と右の値の「綴り」と「意味」を 1 行ずつ声に出して照合する
            //   （全部 String なので、取り違えてもコンパイラは見つけられない。Step 19-D と同じ注意）
            val invite = Invite(
                id = UUID.randomUUID().toString(),  // 招待状そのものの新しい ID（§4-(63)）
                calendarId = calendarId,            // どのカレンダーへ：この画面のカレンダー
                calendarName = calendar.name,       // ② で読んだカレンダーの名前
                invitedEmail = email,               // 誰宛て：小文字に揃えたほうのメールアドレス
                role = state.inviteRole,            // 承認後の肩書き：入力欄で選ばれたもの
                status = InviteStatus.PENDING,      // 今の状態：返事待ち（InviteStatus の 1 つ）
                invitedBy = uid,                    // 招待した人：自分の uid
                createdAt = Instant.now(),
            )

            inviteRepository.sendInvite(invite)
                .onSuccess {
                    // TODO(35-D-3-7) 誰の：状態の箱の ／ 何をする：isInviting を「いいえ」、inviteEmail を空の文字、
                    //   inviteMessage を「招待を送りました」に（入力欄を空にして、次の人を打てるようにする）
                    _uiState.update { it.copy(isInviting = false, inviteEmail = "", inviteMessage = "招待を送りました") }
                }
                .onFailure {
                    finishInvite("招待を送れませんでした")
                }
        }
    }

    /** 招待を途中で終えるときの後片付け。「送信中」の旗を下ろし、一言を出す。打った文字は残す（打ち直せるように）。 */
    private fun finishInvite(message: String) {
        // TODO(35-D-3-8) 誰の：状態の箱の ／ 何をする：isInviting を「いいえ」に、inviteMessage を message に
        _uiState.update { it.copy(isInviting = false, inviteMessage = message) }
    }
}
