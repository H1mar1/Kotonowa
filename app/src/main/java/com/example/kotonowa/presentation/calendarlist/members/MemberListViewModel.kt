package com.example.kotonowa.presentation.calendarlist.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.InviteSendResult
import com.example.kotonowa.domain.model.MemberRole
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.ChatRepository
import com.example.kotonowa.domain.repository.InviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    // TODO(35-D-2-2) 招待状の窓口（35-B / 35-C）。上の 2 行と同じ形。名前は inviteRepository
    private val inviteRepository: InviteRepository,
    // TODO(51-G-3) チャットの窓口（Step 49 / 51-C）。上の 2 行と同じ形。名前は chatRepository、型は ChatRepository
    private val chatRepository: ChatRepository,
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
                        // Step 46：名前は名簿の行に写してあるので、名刺（users）は読まない（読めなくなる）
                        val items = members.map { member ->
                            MemberItem(
                                uid = member.uid,
                                // TODO(46-E-5) 表示名 → 無ければメールアドレス → それも無ければ「（名前未登録）」
                                //   34-C-2-4 と同じ並び。読む先が名刺（user?.）から、名簿の行（member.）に変わるだけ
                                //   member は null にならないので ?. は要らない（中の displayName / email が null になりうるだけ）
                                name = member.displayName ?: member.email ?: "（名前未登録）",
                                role = member.role,
                                // TODO(51-G-1) この行が自分か。誰の：この行の人の uid が ／ 何か：自分の uid（myUid）と同じか
                                //   すぐ下の isOwner の any { } の中の前半と同じ比べ方
                                isMe = member.uid == myUid
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
     * Step 46 から、相手探し・メンバーかの確認・招待状づくりはサーバーの関数 sendInvite が行う
     * （他人の名刺 users をアプリが読まずに済むように）。この画面は門番を通したあと、関数に頼んで結果を出すだけ。
     * お手本は CalendarCreateViewModel の save()（門番 → 旗を立てる → launch → onSuccess / onFailure）。
     */
    fun sendInvite() {
        if (myUid == null) return
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
            // Step 46：相手探し・メンバーかの確認・招待状づくりは、サーバーの関数がまとめて行う。
            //   この画面は「このカレンダーに・この人を・この肩書きで」と頼み、返ってきた結果で一言を出すだけ
            // TODO(46-D-1) 誰の：招待状の窓口の ／ 何をする：sendInvite を呼ぶ
            //   渡す 3 つ：この画面のカレンダーの id（calendarId）、小文字に揃えたメール（email）、入力欄で選ばれた肩書き（state.inviteRole）
            inviteRepository.sendInvite(calendarId, email, state.inviteRole)
                .onSuccess { result ->
                    // TODO(46-D-2) 結果の 3 つで出し分ける。else は書かない（InviteSendResult が増えたらビルドで気づけるように。Step 17 の when と同じ）
                    //   残りの 1 枝の左側：「すでにメンバー」を表す InviteSendResult の名前
                    when (result) {
                        InviteSendResult.SENT -> _uiState.update {
                            // 入力欄を空にして、次の人を打てるようにする（35-D-3-7 と同じ）
                            it.copy(
                                isInviting = false,
                                inviteEmail = "",
                                inviteMessage = "招待を送りました"
                            )
                        }

                        InviteSendResult.NOT_REGISTERED -> finishInvite("このメールアドレスの人は登録されていません")
                        InviteSendResult.ALREADY_MEMBER -> finishInvite("この人はすでにメンバーです")
                    }
                }
                .onFailure {
                    finishInvite("招待を送れませんでした")
                }
        }
    }

    // ---- 51-G：DM を開く -----------------------------------------------------

    /**
     * ［メッセージ］が押されたら呼ばれる。この人との DM の部屋を開き（無ければ作る。51-C）、開けたら合図を立てる。
     * 画面遷移はしない。画面が合図（openedChatId）を見て DM のトーク画面へ移る（Step 17 の isSaved と同じ分担）。
     * お手本はこのファイルの sendInvite（門番 → 旗を立てる → launch → onSuccess / onFailure）。
     */
    fun openDirectChat(member: MemberItem) {
        // 門番①：自分の行（ボタンは出さないが、念のため）
        if (member.isMe) return
        // 門番②：どれかの部屋を開いている途中なら何もしない（二度押しで 2 回遷移するのを防ぐ）
        if (_uiState.value.openingDmUid != null) return

        // TODO(51-G-4) 誰の：状態の箱の ／ 何をする：openingDmUid をこの行の人の uid に、errorMessage を null に（前回のエラーを消す）
        //   sendInvite の isInviting = true, inviteMessage = null と同じ形
        _uiState.update { it.copy(openingDmUid = member.uid, errorMessage = null) }

        viewModelScope.launch {
            // TODO(51-G-5) 誰の：チャットの窓口の ／ 何をする：この人との DM の部屋を開く（51-C の openDirectChat）
            //   渡す 3 つ：相手の uid（member.uid）・相手の名前（member.name）・どのカレンダーで知り合ったか（この画面の calendarId）
            //   お手本はすぐ上の inviteRepository.sendInvite(calendarId, email, state.inviteRole) の行
            chatRepository.openDirectChat(member.uid, member.name, calendarId)
                .onSuccess { chatId ->
                    // TODO(51-G-6) 誰の：状態の箱の ／ 何をする：openingDmUid を null に戻し、openedChatId に chatId を入れる
                    _uiState.update {
                        it.copy(
                            openingDmUid = null,
                            openedChatId = chatId
                        )
                    }
                }
                .onFailure {
                    // ルールに断られたとき（相手の名簿の行に名前もメールも無い、など。51-D の ⚠️）もここに来る
                    _uiState.update {
                        it.copy(
                            openingDmUid = null,
                            errorMessage = "トークを開けませんでした"
                        )
                    }
                }
        }
    }

    /** 画面が DM のトーク画面へ移ったら呼ばれる。合図を下ろす（下ろさないと、戻ってきたときにまた DM へ飛ぶ）。 */
    fun onDmOpened() {
        _uiState.update { it.copy(openedChatId = null) }
    }

    /** 招待を途中で終えるときの後片付け。「送信中」の旗を下ろし、一言を出す。打った文字は残す（打ち直せるように）。 */
    private fun finishInvite(message: String) {
        // TODO(35-D-3-8) 誰の：状態の箱の ／ 何をする：isInviting を「いいえ」に、inviteMessage を message に
        _uiState.update { it.copy(isInviting = false, inviteMessage = message) }
    }
}
