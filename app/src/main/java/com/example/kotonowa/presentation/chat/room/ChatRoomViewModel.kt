package com.example.kotonowa.presentation.chat.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** 1 回の発言の長さの上限。ルール（firestore.rules の text.size() <= 1000）と同じ数にする */
private const val MAX_MESSAGE_LENGTH = 1000

/**
 * トーク画面の頭脳（Step 49-D）。① ではグループ（共有カレンダーごと）のトークだけ。
 *
 * 住所 "chat_room/{calendarId}" の calendarId を SavedStateHandle から受け取る（MemberListViewModel と同じ）。
 * 開いた瞬間に ① カレンダー名を 1 回読み ② 発言を見張り始める。
 * 送ったあとに一覧を読み直す処理は書かない（見張りが新しい発言を流してくる。Step 17-F と同じ）。
 */
@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // トーク画面は必ず calendarId 付きで開かれるので、無ければ navigate の書き間違い＝バグ（Step 18-B と同じ）
    private val calendarId: String = checkNotNull(savedStateHandle["calendarId"])

    // 自分の uid は最初に 1 回入れておく（画面が「自分の発言か」を判定するため）
    private val _uiState =
        MutableStateFlow(ChatRoomUiState(myUid = authRepository.currentUser?.uid))
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    init {
        loadTitle()
        observeMessages()
    }

    /** 上のバーに出すカレンダー名を 1 回読む（名前は開いている間は変わらないので見張らない。Step 25 と同じ考え方） */
    private fun loadTitle() {
        viewModelScope.launch {
            calendarRepository.getCalendar(calendarId)
                .onSuccess { calendar ->
                    _uiState.update { it.copy(title = calendar?.name ?: "") }
                }
            // 名前が読めなくても話はできるので、失敗は画面に出さない
        }
    }

    /** 発言を見張る。誰かが送るたびに、増えた並びが流れてくる */
    private fun observeMessages() {
        viewModelScope.launch {
            try {
                chatRepository.observeMessages(calendarId).collect { messages ->
                    // TODO(49-D-3) 誰の：状態の箱を ／ 何をする：messages を流れてきた messages に、isLoading を false に書き換える
                    //   TaskListViewModel の collect の中の 1 行と同じ形（名前が rows → messages に変わるだけ）
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
            } catch (e: CancellationException) {
                throw e   // 取り止めの合図は失敗ではない（grammar §2-(91)。Exception より上に置く）
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "読み込めませんでした"
                    )
                }
            }
        }
    }

    /** 入力欄の文字が変わった */
    fun onInputChange(text: String) {
        // TODO(49-D-4) 誰の：状態の箱を ／ 何をする：input を引数の text に書き換える（TaskListViewModel の onFilterChange と同じ形）
        _uiState.update { it.copy(input = text) }
    }

    /** 送信ボタンが押された */
    fun send() {
        val state = _uiState.value
        // 前後の空白・改行を取り除いた本文（空白だけの発言を送らせない）
        val text = state.input.trim()

        // TODO(49-D-5) 門番 2 つ：本文が空（isEmpty()）か、送っている途中（state.isSending）なら、何もせず帰る（return）
        //   「または」は ||。ScheduleEditViewModel.save() の二度押し防止と同じ考え方
        if (text.isEmpty() || state.isSending) return

        // 長すぎる発言は、ルールに断られる前にここで止める（Claude が書いた）
        if (text.length > MAX_MESSAGE_LENGTH) {
            _uiState.update { it.copy(errorMessage = "${MAX_MESSAGE_LENGTH}文字までです") }
            return
        }

        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            // TODO(49-D-6) 誰の：チャットの窓口（chatRepository）の ／ 何をする：このカレンダー（calendarId）に、本文 text を送る
            chatRepository.sendMessage(calendarId, text)
                // 送れたら入力欄を空にする（送ったのに文字が残っていると、もう一度送ってしまう）
                .onSuccess { _uiState.update { it.copy(isSending = false, input = "") } }
                // 送れなかったら、打った文字は残したまま一言だけ出す（打ち直さなくてよいように）
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = "送れませんでした"
                        )
                    }
                }
        }
    }
}
