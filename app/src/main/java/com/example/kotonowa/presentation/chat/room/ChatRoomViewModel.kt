package com.example.kotonowa.presentation.chat.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.CalendarRepository
import com.example.kotonowa.domain.model.ChatMessage
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

    /**
     * 吹き出しが長押しされた（Step 50）。自分の発言なら「消しますか？」のダイアログを開く。
     * 画面は自分の吹き出しにしか長押しを付けないが、頭脳でも確かめる（ルールが最後の砦なのは変わらない）。
     */
    fun onMessageLongPress(message: ChatMessage) {
        // TODO(50-C-2) 門番：この発言の送った人（message.senderUid）が、自分（_uiState.value.myUid）と違うなら、何もせず帰る
        //   49-D-7 の「==」の逆で「!=」。49-D-5 の門番と同じく if (…) return の 1 行
        if (message.senderUid != _uiState.value.myUid) return

        // TODO(50-C-3) 誰の：状態の箱を ／ 何をする：deleteTarget を引数の message に書き換える（onInputChange と同じ形）
        _uiState.update { it.copy(deleteTarget = message) }
    }

    /** ダイアログの［やめる］か、ダイアログの外が押された */
    fun dismissDelete() {
        // TODO(50-C-4) 誰の：状態の箱を ／ 何をする：deleteTarget を無し（null）に戻す（50-C-3 と同じ形。入れる値だけ違う）
        _uiState.update { it.copy(deleteTarget = null) }
    }

    /** ダイアログの［消す］が押された（Claude が骨組みを書いた。send() と同じ形） */
    fun confirmDelete() {
        val state = _uiState.value
        // 確認中の発言が無い・消している途中なら何もしない
        val target = state.deleteTarget ?: return
        if (state.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }

        viewModelScope.launch {
            // TODO(50-C-5) 誰の：チャットの窓口（chatRepository）の ／ 何をする：このカレンダー（calendarId）から、確認中の発言（target）の id の発言を消す
            //   send() の 49-D-6 と同じ形。2 つ目の引数は発言そのものではなく、その id
            chatRepository.deleteMessage(calendarId,target.id)
                // 消えた吹き出しは見張りから自然に消える（Step 18 と同じ）。ダイアログを閉じるだけ
                .onSuccess { _uiState.update { it.copy(isDeleting = false, deleteTarget = null) } }
                .onFailure {
                    _uiState.update {
                        it.copy(isDeleting = false, deleteTarget = null, errorMessage = "消せませんでした")
                    }
                }
        }
    }
}
