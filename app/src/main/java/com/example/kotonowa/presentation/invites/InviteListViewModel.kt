package com.example.kotonowa.presentation.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.model.Invite
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.InviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 届いた招待の一覧画面の頭脳。
 *
 * お手本は `presentation/calendarlist/CalendarListViewModel`（init で 1 回見張り始めるだけ）。
 * 承認・拒否のあとに一覧を読み直す処理は**書かない**。返事をすると招待状が pending ではなくなり、
 * observeMyInvites の絞り込みから外れて、Flow が「1 枚減った束」を流してくれる（Step 17-F と同じ）。
 */
@HiltViewModel
class InviteListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val inviteRepository: InviteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteListUiState())
    val uiState: StateFlow<InviteListUiState> = _uiState.asStateFlow()

    /** 自分の uid。承認で名簿の行の住所に使う。 */
    private val uid = authRepository.currentUser?.uid

    init {
        // Step 39：確認済みの人だけ招待を見張る。未確認ならルールに読み取りを断られるので、見張らずにカードを出す
        // TODO(39-C-2) 条件。誰の：認証の窓口の ／ 何か：確認済みか（39-A で作った val。() は付けない）
        if (authRepository.isEmailVerified) {
            observeInvites()
        } else {
            _uiState.update { it.copy(isEmailVerified = false, isLoading = false) }
        }
    }

    /** ［確認メールを再送］が押された（Step 39）。 */
    fun resendVerificationEmail() {
        if (_uiState.value.isCheckingEmail) return
        _uiState.update { it.copy(isCheckingEmail = true, actionMessage = null) }

        viewModelScope.launch {
            // TODO(39-C-3) 誰の：認証の窓口の ／ 何をする：確認メールを送る（39-B-1 と同じ 1 行）
            authRepository.sendEmailVerification()
                .onSuccess {
                    _uiState.update { it.copy(actionMessage = "確認メールを送りました。メールのリンクを押してから［確認しました］を押してください") }
                }
                .onFailure {
                    _uiState.update { it.copy(actionMessage = "確認メールを送れませんでした。時間をおいて試してください") }
                }
            _uiState.update { it.copy(isCheckingEmail = false) }
        }
    }

    /** ［確認しました］が押された（Step 39）。Firebase に問い合わせ直し、確認済みなら招待を見張り始める。 */
    fun checkEmailVerified() {
        if (_uiState.value.isCheckingEmail) return
        _uiState.update { it.copy(isCheckingEmail = true, actionMessage = null) }

        viewModelScope.launch {
            // 読み直し ＋ 通行証の作り直し（39-A の refreshEmailVerified）。封筒の中身は「確認済みか」
            authRepository.refreshEmailVerified()
                .onSuccess { verified ->
                    // TODO(39-C-4) 条件。verified（受け取った「はい／いいえ」）が「はい」か。Boolean なので == true は要らない
                    if (verified) {
                        // 確認できた：カードを消して読み込み中にし、招待の見張りを始める
                        _uiState.update { it.copy(isEmailVerified = true, isLoading = true) }
                        // TODO(39-C-5) 誰の：この頭脳の ／ 何をする：招待の見張りを始める（init の if の中と同じ 1 行）
                        observeInvites()
                    } else {
                        _uiState.update { it.copy(actionMessage = "まだ確認できていません。メールのリンクを押してから、もう一度押してください") }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(actionMessage = "確認できませんでした。通信状態を確かめてください") }
                }
            _uiState.update { it.copy(isCheckingEmail = false) }
        }
    }

    private fun observeInvites() {
        // 小文字にした自分のメール（CalendarViewModel.observeInvites の 1 行目と同じ取り出し方）。
        // ただしここは「無ければ黙って return」ではなく、CalendarListViewModel のように画面にエラーを出してから終わる。
        // この画面は招待を見るための画面なので、黙っていると「招待が 0 件」と区別がつかない
        val myEmail = authRepository.currentUser?.email?.lowercase()
        if (myEmail == null) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "ログイン情報が取得できませんでした")
            }
            return
        }

        viewModelScope.launch {
            try {
                // TODO(36-E-2-2) 誰の：招待の窓口の ／ 何をする：自分宛ての招待状を見張り、流れてくるたびに
                //   invites に束を入れ、isLoading を「いいえ」にする。CalendarListViewModel の collect と同じ形
                inviteRepository.observeMyInvites(myEmail)
                    .collect { list ->
                        _uiState.update { state -> state.copy(invites = list, isLoading = false) }
                    }
            } catch (e: CancellationException) {
                throw e   // 意図して取り止めた合図（grammar §2-(91)）。Exception より上に置く
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "招待を読み込めませんでした")
                }
            }
        }
    }

    /**
     * 承認ボタンが押された。
     *
     * 門番は 2 つ（ScheduleEditViewModel.save() と同じ考え方）。
     *  ① 自分の uid が無ければ何もしない
     *  ② すでに何かの返事を送っている途中なら何もしない（二度押し防止）
     */
    fun accept(invite: Invite) {
        val myUid = uid ?: return
        if (_uiState.value.processingInviteId != null) return

        // 「この招待状の返事を送っている途中」の印を付け、前の一言を消す
        _uiState.update { it.copy(processingInviteId = invite.id, actionMessage = null) }

        viewModelScope.launch {
            // TODO(36-E-2-3) 誰の：招待の窓口の ／ 何をする：この招待状を、自分の uid で承認する（36-B で決めた引数の順）
            inviteRepository.acceptInvite(invite, myUid)
                .onSuccess {
                    // TODO(36-E-2-4) 一言を「『〇〇』に参加しました」にする。〇〇は招待状に書いてあるカレンダー名（§6-㉖）
                    _uiState.update { it.copy(actionMessage = "「${invite.calendarName}」に参加しました") }
                }
                .onFailure {
                    _uiState.update { it.copy(actionMessage = "承認できませんでした") }
                }

            // 成功しても失敗しても、送っている途中の印は外す
            _uiState.update { it.copy(processingInviteId = null) }
        }
    }

    /** 拒否ボタンが押された。形は accept とほぼ同じ（uid が要らないだけ）。 */
    fun decline(invite: Invite) {
        // TODO(36-E-2-5) 門番（二度押し防止）。accept の 2 行目と同じ
        val myUid = uid ?: return
        if (_uiState.value.processingInviteId != null) return

        _uiState.update { it.copy(processingInviteId = invite.id, actionMessage = null) }

        viewModelScope.launch {
            // TODO(36-E-2-6) 誰の：招待の窓口の ／ 何をする：この招待状を拒否する
            inviteRepository.declineInvite(invite)
                .onSuccess {
                    _uiState.update { it.copy(actionMessage = "招待をお断りしました") }
                }
                .onFailure {
                    // TODO(36-E-2-7) 一言を「お断りできませんでした」にする（上の onSuccess と同じ形）
                    _uiState.update { it.copy(actionMessage = "招待をお断りできませんでした") }
                }

            _uiState.update { it.copy(processingInviteId = null) }
        }
    }
}
