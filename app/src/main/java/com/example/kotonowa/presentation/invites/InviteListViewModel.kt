package com.example.kotonowa.presentation.invites

import androidx.collection.intIntMapOf
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
        observeInvites()
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
