package com.example.kotonowa.presentation.chat.room

import com.example.kotonowa.domain.model.ChatMessage

/**
 * トーク画面の状態（Step 49-D）。画面はこれを見て描くだけ。
 *
 * @property title 上のバーに出す名前（グループならカレンダー名）
 * @property messages 発言の並び（古い順。下ほど新しい）
 * @property myUid 自分の uid。発言が自分のものか（右に寄せるか）を画面が判定するため
 * @property input 入力欄の今の文字（状態ホイスティング。§3-(76)）
 * @property isSending 送っている途中か（二度押し防止・送信ボタンを押せなくする）
 * @property isLoading 最初の読み込み中か（開いた瞬間に見張りが始まるので true 始まり。Step 18-B と同じ）
 * @property errorMessage 送れなかった・読めなかったときの一言
 */
data class ChatRoomUiState(
    val title: String = "",
    // TODO(49-D-1) 発言の並び。型は ChatMessage の並び、最初は空の並び（TaskListUiState の rows と同じ形）
    val messages: ____ = ____,
    val myUid: String? = null,
    // TODO(49-D-2) 入力欄の文字。最初は何も打っていない（空の文字）
    val input: String = ____,
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
